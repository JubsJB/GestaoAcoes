package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.dto.OperacaoCreateRequest;
import com.projeto.dto.SnapshotCarteiraResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.TipoOperacao;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.HistoricoCotacaoRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.repositories.SnapshotCarteiraMoedaRepository;
import com.projeto.repositories.SnapshotCarteiraRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
class SnapshotCarteiraConcurrencyTest {

    @Autowired SnapshotCarteiraService snapshotService;
    @Autowired OperacaoService operacaoService;
    @Autowired SnapshotCarteiraRepository snapshotRepository;
    @Autowired SnapshotCarteiraMoedaRepository componenteRepository;
    @Autowired OperacaoRepository operacaoRepository;
    @Autowired CarteiraRepository carteiraRepository;
    @Autowired HistoricoCotacaoRepository historicoRepository;
    @Autowired AcaoRepository acaoRepository;
    @Autowired PlatformTransactionManager transactionManager;
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
        when(clock.withZone(any())).thenReturn(clock);
        when(clock.instant()).thenReturn(Instant.parse("2026-08-27T15:00:00Z"));
    }

    @Test
    void concurrentOperationProducesCompletePreviousOrNewSnapshot() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira());
        acaoRepository.saveAndFlush(acao("PETR4", "35.500000"));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<SnapshotCarteiraResponse> snapshot = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return snapshotService.criar(carteira.getId());
            });
            Future<?> operation = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return operacaoService.cadastrar(new OperacaoCreateRequest(
                        carteira.getId(), "PETR4", Mercado.BRASIL, null, TipoOperacao.COMPRA,
                        new BigDecimal("100"), new BigDecimal("32"), LocalDate.of(2026, 8, 10), 1));
            });
            start.countDown();
            SnapshotCarteiraResponse response = snapshot.get(10, TimeUnit.SECONDS);
            operation.get(10, TimeUnit.SECONDS);

            assertTrue(response.patrimonios().isEmpty()
                    || response.patrimonios().equals(List.of(
                    new com.projeto.dto.SnapshotCarteiraMoedaResponse(
                            Moeda.BRL, new BigDecimal("3550.000000000000")))));
            assertEquals(1, snapshotRepository.count());
            assertEquals(response.patrimonios().size(), componenteRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentQuoteChangeProducesOnlyCompleteOldOrNewValueWithoutHistoryDependency() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira());
        Acao acao = acaoRepository.saveAndFlush(acao("VALE3", "10.000000"));
        operacaoService.cadastrar(new OperacaoCreateRequest(carteira.getId(), "VALE3", Mercado.BRASIL,
                null, TipoOperacao.COMPRA, BigDecimal.ONE, BigDecimal.ONE,
                LocalDate.of(2026, 8, 10), 1));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<SnapshotCarteiraResponse> snapshot = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return snapshotService.criar(carteira.getId());
            });
            Future<?> quote = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    Acao managed = acaoRepository.findById(acao.getId()).orElseThrow();
                    managed.atualizarCotacao(new BigDecimal("20.000000"),
                            OffsetDateTime.parse("2026-08-27T14:00:00Z"));
                    acaoRepository.saveAndFlush(managed);
                });
                return null;
            });
            start.countDown();
            BigDecimal value = snapshot.get(10, TimeUnit.SECONDS).patrimonios().get(0).patrimonioAtual();
            quote.get(10, TimeUnit.SECONDS);

            assertTrue(value.equals(new BigDecimal("10.000000000000"))
                    || value.equals(new BigDecimal("20.000000000000")));
            assertEquals(0, historicoRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void exactConcurrentTimestampCollisionKeepsOneAtomicSnapshot() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = executor.submit(() -> createCapturingConflict(carteira.getId(), start));
            Future<Object> second = executor.submit(() -> createCapturingConflict(carteira.getId(), start));
            start.countDown();
            List<Object> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertEquals(1, results.stream().filter(SnapshotCarteiraResponse.class::isInstance).count());
            assertEquals(1, results.stream().filter(item -> item instanceof ApiException exception
                    && ErrorCodes.SNAPSHOT_CARTEIRA_DUPLICADO.equals(exception.getCode())).count());
            assertEquals(1, snapshotRepository.count());
            assertEquals(0, componenteRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    private Object createCapturingConflict(Long carteiraId, CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try {
            return snapshotService.criar(carteiraId);
        } catch (ApiException exception) {
            return exception;
        }
    }

    private Carteira carteira() {
        return new Carteira("Concorrente", OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private Acao acao(String ticker, String cotacao) {
        return new Acao(ticker, "Empresa", Mercado.BRASIL, Moeda.BRL, new BigDecimal(cotacao),
                OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }
}
