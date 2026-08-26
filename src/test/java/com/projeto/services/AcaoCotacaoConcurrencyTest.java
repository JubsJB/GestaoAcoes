package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.repositories.AcaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AcaoCotacaoConcurrencyTest {

    @Autowired
    private AcaoRepository repository;

    @Autowired
    private AcaoCotacaoPersistenceService persistenceService;

    @Test
    void maiorTimestampPrevaleceEmAtualizacoesConcorrentes() throws Exception {
        Acao saved = repository.saveAndFlush(new Acao(
                "CONC3", "Empresa", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("30.000000"), OffsetDateTime.parse("2026-08-19T15:30:00Z")
        ));
        OffsetDateTime antiga = OffsetDateTime.parse("2026-08-20T15:30:00Z");
        OffsetDateTime recente = OffsetDateTime.parse("2026-08-21T15:30:00Z");
        CountDownLatch inicio = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> primeira = executor.submit(() -> atualizarAposInicio(
                    inicio, saved.getId(), new BigDecimal("31.000000"), antiga));
            Future<?> segunda = executor.submit(() -> atualizarAposInicio(
                    inicio, saved.getId(), new BigDecimal("32.000000"), recente));

            inicio.countDown();
            primeira.get(10, TimeUnit.SECONDS);
            segunda.get(10, TimeUnit.SECONDS);
        } finally {
            assertTrue(executor.shutdownNow().isEmpty());
        }

        Acao finalState = repository.findById(saved.getId()).orElseThrow();
        assertEquals(new BigDecimal("32.000000"), finalState.getCotacaoAtual());
        assertEquals(recente.toInstant(), finalState.getDataHoraCotacao().toInstant());
    }

    private void atualizarAposInicio(
            CountDownLatch inicio,
            Long id,
            BigDecimal cotacao,
            OffsetDateTime timestamp
    ) {
        try {
            inicio.await(10, TimeUnit.SECONDS);
            persistenceService.atualizarSePosterior(id, cotacao, timestamp);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
