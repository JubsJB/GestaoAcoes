package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.HistoricoCotacaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AcaoPersistenceServiceTest {

    @Mock
    private AcaoRepository repository;

    @Mock
    private HistoricoCotacaoRepository historicoRepository;

    @Test
    void rejectsKnownDuplicateBeforeSaving() {
        when(repository.existsByTickerAndMercado("AAPL", Mercado.EUA)).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> new AcaoPersistenceService(repository, historicoRepository)
                        .ensureAvailable("AAPL", Mercado.EUA)
        );

        assertDuplicate(exception);
        assertEquals("AAPL", exception.getDetails().get("ticker"));
        assertEquals("EUA", exception.getDetails().get("mercado"));
    }

    @Test
    void translatesLateUniqueViolationFromConcurrentRace() {
        Acao acao = action("PETR4", Mercado.BRASIL, Moeda.BRL);
        when(repository.existsByTickerAndMercado("PETR4", Mercado.BRASIL)).thenReturn(false);
        when(repository.saveAndFlush(acao)).thenThrow(new DataIntegrityViolationException("unique"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> new AcaoPersistenceService(repository, historicoRepository).saveUnique(acao)
        );

        assertDuplicate(exception);
    }

    @Test
    void persisteExatamenteUmHistoricoInicialComOsMesmosValores() {
        Acao acao = action("PETR4", Mercado.BRASIL, Moeda.BRL);
        when(repository.saveAndFlush(acao)).thenReturn(acao);

        new AcaoPersistenceService(repository, historicoRepository).saveUnique(acao);

        var captor = org.mockito.ArgumentCaptor.forClass(com.projeto.entities.HistoricoCotacao.class);
        verify(historicoRepository).saveAndFlush(captor.capture());
        assertEquals(acao, captor.getValue().getAcao());
        assertEquals(acao.getCotacaoAtual(), captor.getValue().getCotacao());
        assertEquals(acao.getDataHoraCotacao(), captor.getValue().getDataHoraCotacao());
        verify(repository).saveAndFlush(acao);
    }

    @Test
    void propagaFalhaDoHistoricoParaRollbackDaTransacao() {
        Acao acao = action("PETR4", Mercado.BRASIL, Moeda.BRL);
        when(repository.saveAndFlush(acao)).thenReturn(acao);
        when(historicoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("history"));

        assertThrows(DataIntegrityViolationException.class,
                () -> new AcaoPersistenceService(repository, historicoRepository).saveUnique(acao));
    }

    private Acao action(String ticker, Mercado mercado, Moeda moeda) {
        return new Acao(
                ticker,
                "Empresa",
                mercado,
                moeda,
                new BigDecimal("10.000000"),
                OffsetDateTime.of(2026, 8, 20, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }

    private void assertDuplicate(ApiException exception) {
        assertEquals(ErrorCodes.ACAO_DUPLICADA, exception.getCode());
        assertEquals(409, exception.getStatus().value());
    }
}
