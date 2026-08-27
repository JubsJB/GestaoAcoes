package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.dto.OperacaoCreateRequest;
import com.projeto.dto.ResumoCarteiraResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.TipoOperacao;
import com.projeto.repositories.AcaoRepository;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
class ResumoCarteiraConcurrencyTest {

    @Autowired
    private ResumoCarteiraService resumoCarteiraService;

    @Autowired
    private OperacaoService operacaoService;

    @Autowired
    private OperacaoRepository operacaoRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private AcaoRepository acaoRepository;

    @Autowired
    private CorretoraRepository corretoraRepository;

    @BeforeEach
    void cleanDatabase() {
        operacaoRepository.deleteAll();
        carteiraRepository.deleteAll();
        acaoRepository.deleteAll();
        corretoraRepository.deleteAll();
    }

    @Test
    void concurrentRegistrationReturnsCompletePreviousOrNewSummarySnapshot() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(new Carteira(
                "Carteira concorrente",
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        ));
        acaoRepository.saveAndFlush(new Acao(
                "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("35.500000"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        ));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ResumoCarteiraResponse> query = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return resumoCarteiraService.consultar(carteira.getId());
            });
            Future<?> registration = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return operacaoService.cadastrar(new OperacaoCreateRequest(
                        carteira.getId(), "PETR4", Mercado.BRASIL, null,
                        TipoOperacao.COMPRA, new BigDecimal("100"),
                        new BigDecimal("32"), LocalDate.of(2026, 8, 10), 1
                ));
            });

            start.countDown();
            ResumoCarteiraResponse snapshot = query.get(10, TimeUnit.SECONDS);
            registration.get(10, TimeUnit.SECONDS);

            assertTrue(snapshot.resumos().isEmpty() || snapshot.resumos().size() == 1);
            if (!snapshot.resumos().isEmpty()) {
                assertEquals(new BigDecimal("3200.000000000000"),
                        snapshot.resumos().get(0).custoTotalPosicoes());
                assertEquals(new BigDecimal("3550.000000000000"),
                        snapshot.resumos().get(0).patrimonioAtual());
                assertEquals(new BigDecimal("350.000000000000"),
                        snapshot.resumos().get(0).resultadoNaoRealizadoTotal());
                assertEquals(new BigDecimal("10.937500"),
                        snapshot.resumos().get(0).rentabilidadePercentual());
            }

            ResumoCarteiraResponse committed = resumoCarteiraService.consultar(carteira.getId());
            assertEquals(new BigDecimal("3550.000000000000"),
                    committed.resumos().get(0).patrimonioAtual());
            assertEquals(1, operacaoRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }
}
