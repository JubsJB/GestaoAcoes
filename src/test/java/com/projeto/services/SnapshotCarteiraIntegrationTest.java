package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.dto.SnapshotCarteiraResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.HistoricoCotacaoRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.repositories.SnapshotCarteiraMoedaRepository;
import com.projeto.repositories.SnapshotCarteiraRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
class SnapshotCarteiraIntegrationTest {

    @Autowired SnapshotCarteiraService service;
    @Autowired SnapshotCarteiraRepository snapshotRepository;
    @Autowired SnapshotCarteiraMoedaRepository componenteRepository;
    @Autowired OperacaoRepository operacaoRepository;
    @Autowired CarteiraRepository carteiraRepository;
    @Autowired HistoricoCotacaoRepository historicoRepository;
    @Autowired AcaoRepository acaoRepository;
    @MockitoBean Clock clock;

    @BeforeEach
    void cleanDatabase() {
        componenteRepository.deleteAll();
        snapshotRepository.deleteAll();
        operacaoRepository.deleteAll();
        carteiraRepository.deleteAll();
        historicoRepository.deleteAll();
        acaoRepository.deleteAll();
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-27T15:00:00Z"));
    }

    @Test
    void persistsExactSeparatedBrlAndUsdTotalsUsingOnlyOpenPositions() {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira("Multimoeda"));
        Acao petr4 = acaoRepository.saveAndFlush(acao("PETR4", Mercado.BRASIL, Moeda.BRL, "35.500000"));
        Acao vale3 = acaoRepository.saveAndFlush(acao("VALE3", Mercado.BRASIL, Moeda.BRL, "60.000000"));
        Acao aapl = acaoRepository.saveAndFlush(acao("AAPL", Mercado.EUA, Moeda.USD, "200.000000"));
        operacaoRepository.saveAndFlush(operacao(carteira, petr4, TipoOperacao.COMPRA, "100", "32", 1));
        operacaoRepository.saveAndFlush(operacao(carteira, petr4, TipoOperacao.VENDA, "25", "40", 2));
        operacaoRepository.saveAndFlush(operacao(carteira, vale3, TipoOperacao.COMPRA, "10", "50", 1));
        operacaoRepository.saveAndFlush(operacao(carteira, aapl, TipoOperacao.COMPRA, "0.5", "180", 1));

        SnapshotCarteiraResponse response = service.criar(carteira.getId());

        assertEquals(java.util.List.of(Moeda.BRL, Moeda.USD),
                response.patrimonios().stream().map(item -> item.moeda()).toList());
        assertEquals(new BigDecimal("3262.500000000000"), response.patrimonios().get(0).patrimonioAtual());
        assertEquals(new BigDecimal("100.000000000000"), response.patrimonios().get(1).patrimonioAtual());
        assertEquals(1, snapshotRepository.count());
        assertEquals(2, componenteRepository.count());
        assertEquals(0, historicoRepository.count());
    }

    @Test
    void closedCycleIsOmittedAndNewCycleUsesOnlyItsOpenPosition() {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira("Novo ciclo"));
        Acao acao = acaoRepository.saveAndFlush(acao("ITUB4", Mercado.BRASIL, Moeda.BRL, "30.000000"));
        operacaoRepository.saveAndFlush(operacao(carteira, acao, TipoOperacao.COMPRA, "10", "20", 1));
        operacaoRepository.saveAndFlush(operacao(carteira, acao, TipoOperacao.VENDA, "10", "25", 2));
        operacaoRepository.saveAndFlush(operacao(carteira, acao, TipoOperacao.COMPRA, "2", "28", 3));

        SnapshotCarteiraResponse response = service.criar(carteira.getId());

        assertEquals(1, response.patrimonios().size());
        assertEquals(new BigDecimal("60.000000000000"), response.patrimonios().get(0).patrimonioAtual());
    }

    @Test
    void identicalContentAtDifferentInstantsIsKeptAndExactCollisionRollsBack() {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira("Temporal"));
        Acao acao = acaoRepository.saveAndFlush(acao("MSFT", Mercado.EUA, Moeda.USD, "400.000000"));
        operacaoRepository.saveAndFlush(operacao(carteira, acao, TipoOperacao.COMPRA, "1", "350", 1));

        SnapshotCarteiraResponse first = service.criar(carteira.getId());
        when(clock.instant()).thenReturn(Instant.parse("2026-08-27T16:00:00Z"));
        SnapshotCarteiraResponse second = service.criar(carteira.getId());
        long parents = snapshotRepository.count();
        long children = componenteRepository.count();

        assertThrows(DataIntegrityViolationException.class, () -> service.criar(carteira.getId()));

        assertNotEquals(first.id(), second.id());
        assertEquals(first.patrimonios(), second.patrimonios());
        assertEquals(parents, snapshotRepository.count());
        assertEquals(children, componenteRepository.count());
    }

    private Carteira carteira(String nome) {
        return new Carteira(nome, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private Acao acao(String ticker, Mercado mercado, Moeda moeda, String cotacao) {
        return new Acao(ticker, "Empresa", mercado, moeda, new BigDecimal(cotacao),
                OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private Operacao operacao(Carteira carteira, Acao acao, TipoOperacao tipo,
                              String quantidade, String preco, int ordem) {
        BigDecimal q = new BigDecimal(quantidade).setScale(6);
        BigDecimal p = new BigDecimal(preco).setScale(6);
        return new Operacao(carteira, acao, null, tipo, q, p,
                LocalDate.of(2026, 8, 10), ordem, q.multiply(p).setScale(12));
    }
}
