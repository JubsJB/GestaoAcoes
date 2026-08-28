package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.dto.EvolucaoPatrimonialResponse;
import com.projeto.entities.Carteira;
import com.projeto.entities.Moeda;
import com.projeto.entities.SnapshotCarteira;
import com.projeto.entities.SnapshotCarteiraMoeda;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.SnapshotCarteiraMoedaRepository;
import com.projeto.repositories.SnapshotCarteiraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
class EvolucaoPatrimonialConcurrencyTest {

    @Autowired EvolucaoPatrimonialService service;
    @Autowired CarteiraRepository carteiraRepository;
    @Autowired SnapshotCarteiraRepository snapshotRepository;
    @Autowired SnapshotCarteiraMoedaRepository componenteRepository;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanDatabase() {
        componenteRepository.deleteAll();
        snapshotRepository.deleteAll();
        carteiraRepository.deleteAll();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {
        cleanDatabase();
    }

    @Test
    void concurrentReadSeesSeriesBeforeOrAfterAtomicParentAndChildrenCommit() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(new Carteira(
                "Concorrente", OffsetDateTime.parse("2026-08-01T10:00:00Z")));
        CountDownLatch parentFlushed = new CountDownLatch(1);
        CountDownLatch allowChildren = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> writer = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        SnapshotCarteira snapshot = snapshotRepository.saveAndFlush(new SnapshotCarteira(
                                carteiraRepository.getReferenceById(carteira.getId()),
                                OffsetDateTime.parse("2026-08-28T12:00:00Z")));
                        parentFlushed.countDown();
                        await(allowChildren);
                        componenteRepository.saveAndFlush(new SnapshotCarteiraMoeda(
                                snapshot, Moeda.BRL, new BigDecimal("1000.000000000000")));
                    }));

            assertTrue(parentFlushed.await(5, TimeUnit.SECONDS));
            EvolucaoPatrimonialResponse duringUncommittedWrite = service.consultar(carteira.getId());
            assertTrue(duringUncommittedWrite.pontos().isEmpty());

            allowChildren.countDown();
            writer.get(10, TimeUnit.SECONDS);
            EvolucaoPatrimonialResponse afterCommit = service.consultar(carteira.getId());
            assertEquals(1, afterCommit.pontos().size());
            assertEquals(1, afterCommit.pontos().get(0).patrimonios().size());
            assertEquals(new BigDecimal("1000.000000000000"),
                    afterCommit.pontos().get(0).patrimonios().get(0).patrimonioAtual());
        } finally {
            allowChildren.countDown();
            executor.shutdownNow();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timeout aguardando continuação do teste");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
