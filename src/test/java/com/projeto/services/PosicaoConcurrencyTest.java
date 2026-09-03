package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.dto.OperacaoCreateRequest;
import com.projeto.TestOperacaoRequests;
import com.projeto.dto.PosicaoResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.TipoOperacao;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.HistoricoCotacaoRepository;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.CorretoraRepository;
import com.projeto.repositories.OperacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
class PosicaoConcurrencyTest {

    @Autowired
    private PosicaoService posicaoService;

    @Autowired
    private OperacaoService operacaoService;

    @Autowired
    private OperacaoRepository operacaoRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private AcaoRepository acaoRepository;

    @Autowired
    private HistoricoCotacaoRepository historicoCotacaoRepository;

    @Autowired
    private CorretoraRepository corretoraRepository;

    @BeforeEach
    void cleanDatabase() {
        operacaoRepository.deleteAll();
        carteiraRepository.deleteAll();
        historicoCotacaoRepository.deleteAll();
        acaoRepository.deleteAll();
        corretoraRepository.deleteAll();
    }

    @Test
    void concurrentRegistrationProducesEitherTheCompletePreviousOrCompleteNewSnapshot() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(new Carteira(
                "Carteira concorrente",
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        ));
        acaoRepository.saveAndFlush(new Acao(
                "PETR4",
                "Petrobras",
                Mercado.BRASIL,
                Moeda.BRL,
                new BigDecimal("99.123456"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        ));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<List<PosicaoResponse>> query = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return posicaoService.listarPorCarteira(carteira.getId());
            });
            Future<?> registration = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return operacaoService.cadastrar(TestOperacaoRequests.request(
                        carteira.getId(),
                        "PETR4",
                        Mercado.BRASIL,
                        null,
                        TipoOperacao.COMPRA,
                        new BigDecimal("100"),
                        new BigDecimal("10"),
                        LocalDate.of(2026, 8, 10)
                ));
            });

            start.countDown();
            List<PosicaoResponse> concurrentSnapshot = query.get(10, TimeUnit.SECONDS);
            registration.get(10, TimeUnit.SECONDS);

            assertTrue(concurrentSnapshot.isEmpty() || concurrentSnapshot.size() == 1);
            if (!concurrentSnapshot.isEmpty()) {
                assertEquals(new BigDecimal("100.000000"), concurrentSnapshot.get(0).quantidadeAtual());
                assertEquals(new BigDecimal("10.000000000000"), concurrentSnapshot.get(0).precoMedio());
                assertEquals(new BigDecimal("1000.000000000000"), concurrentSnapshot.get(0).custoPosicao());
            }

            List<PosicaoResponse> committedSnapshot = posicaoService.listarPorCarteira(carteira.getId());
            assertEquals(1, committedSnapshot.size());
            assertEquals(new BigDecimal("100.000000"), committedSnapshot.get(0).quantidadeAtual());
            assertEquals(1, operacaoRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }
}
