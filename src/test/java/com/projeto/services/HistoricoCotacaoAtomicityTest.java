package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Acao;
import com.projeto.entities.HistoricoCotacao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.HistoricoCotacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HistoricoCotacaoAtomicityTest {

    @Autowired
    private AcaoRepository acaoRepository;

    @MockitoSpyBean
    private HistoricoCotacaoRepository historicoRepository;

    @Autowired
    private AcaoPersistenceService acaoPersistenceService;

    @Autowired
    private AcaoCotacaoPersistenceService cotacaoPersistenceService;

    @Test
    void falhaDoHistoricoReverteCadastroIntegralmente() {
        Acao candidata = acao("ROLL3", OffsetDateTime.parse("2026-08-20T10:00:00Z"));
        doThrow(new DataIntegrityViolationException("history"))
                .when(historicoRepository).saveAndFlush(any(HistoricoCotacao.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> acaoPersistenceService.saveUnique(candidata));

        reset(historicoRepository);
        assertFalse(acaoRepository.existsByTickerAndMercado("ROLL3", Mercado.BRASIL));
    }

    @Test
    void falhaDoHistoricoReverteAtualizacaoIntegralmente() {
        OffsetDateTime atual = OffsetDateTime.parse("2026-08-20T10:00:00Z");
        OffsetDateTime posterior = OffsetDateTime.parse("2026-08-20T11:00:00Z");
        Acao salva = acaoRepository.saveAndFlush(acao("ROLL4", atual));
        doThrow(new DataIntegrityViolationException("history"))
                .when(historicoRepository).saveAndFlush(any(HistoricoCotacao.class));

        assertThrows(DataIntegrityViolationException.class, () -> cotacaoPersistenceService.atualizarSePosterior(
                salva.getId(), new BigDecimal("40.000000"), posterior));

        reset(historicoRepository);
        Acao preservada = acaoRepository.findById(salva.getId()).orElseThrow();
        assertEquals(new BigDecimal("30.000000"), preservada.getCotacaoAtual());
        assertEquals(atual.toInstant(), preservada.getDataHoraCotacao().toInstant());
    }

    private Acao acao(String ticker, OffsetDateTime timestamp) {
        return new Acao(
                ticker, "Empresa", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("30.000000"), timestamp
        );
    }
}
