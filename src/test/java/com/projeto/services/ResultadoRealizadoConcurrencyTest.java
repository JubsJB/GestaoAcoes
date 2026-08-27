package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.dto.OperacaoCreateRequest;
import com.projeto.dto.ResultadoRealizadoResponse;
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
class ResultadoRealizadoConcurrencyTest {

    @Autowired
    private ResultadoRealizadoService resultadoRealizadoService;

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
    void concurrentSaleRegistrationReturnsEitherCompletePreviousOrCompleteNewSnapshot() throws Exception {
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
        operacaoService.cadastrar(request(carteira.getId(), TipoOperacao.COMPRA, "100", "10", 1));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<List<ResultadoRealizadoResponse>> query = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return resultadoRealizadoService.listarPorCarteira(carteira.getId());
            });
            Future<?> registration = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return operacaoService.cadastrar(request(
                        carteira.getId(), TipoOperacao.VENDA, "40", "15", 2
                ));
            });

            start.countDown();
            List<ResultadoRealizadoResponse> concurrentSnapshot = query.get(10, TimeUnit.SECONDS);
            registration.get(10, TimeUnit.SECONDS);

            assertTrue(concurrentSnapshot.isEmpty() || concurrentSnapshot.size() == 1);
            if (!concurrentSnapshot.isEmpty()) {
                assertEquals(
                        new BigDecimal("200.000000000000"),
                        concurrentSnapshot.get(0).resultadoRealizado()
                );
            }

            List<ResultadoRealizadoResponse> committed =
                    resultadoRealizadoService.listarPorCarteira(carteira.getId());
            assertEquals(1, committed.size());
            assertEquals(new BigDecimal("200.000000000000"), committed.get(0).resultadoRealizado());
            assertEquals(2, operacaoRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    private OperacaoCreateRequest request(
            Long carteiraId,
            TipoOperacao tipo,
            String quantidade,
            String preco,
            int ordem
    ) {
        return new OperacaoCreateRequest(
                carteiraId,
                "PETR4",
                Mercado.BRASIL,
                null,
                tipo,
                new BigDecimal(quantidade),
                new BigDecimal(preco),
                LocalDate.of(2026, 8, 10),
                ordem
        );
    }
}
