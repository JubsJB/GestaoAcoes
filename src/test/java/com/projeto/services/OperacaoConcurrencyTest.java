package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.dto.OperacaoCreateRequest;
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
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ObjectNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
class OperacaoConcurrencyTest {

    @Autowired
    private OperacaoService operacaoService;

    @Autowired
    private CarteiraService carteiraService;

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
    void pessimisticPortfolioLockPreventsConcurrentSalesFromOversellingTogether() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira concorrente"));
        acaoRepository.saveAndFlush(action());
        operacaoService.cadastrar(request(carteira.getId(), TipoOperacao.COMPRA, "100", 1));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(
                    () -> registerAfter(start, request(carteira.getId(), TipoOperacao.VENDA, "80", 2))
            );
            Future<String> second = executor.submit(
                    () -> registerAfter(start, request(carteira.getId(), TipoOperacao.VENDA, "80", 3))
            );
            start.countDown();

            List<String> outcomes = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );

            assertEquals(1, outcomes.stream().filter("CREATED"::equals).count());
            assertEquals(1, outcomes.stream().filter("POSICAO_INSUFICIENTE"::equals).count());
            assertEquals(2, operacaoRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void operationCreationAndPortfolioDeletionEndInOneOfTwoConsistentLockedStates() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira em disputa"));
        acaoRepository.saveAndFlush(action());

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> creation = executor.submit(() -> {
                start.await();
                try {
                    operacaoService.cadastrar(request(carteira.getId(), TipoOperacao.COMPRA, "1", 1));
                    return "CREATED";
                } catch (ObjectNotFoundException exception) {
                    return "PORTFOLIO_NOT_FOUND";
                }
            });
            Future<String> deletion = executor.submit(() -> {
                start.await();
                try {
                    carteiraService.excluir(carteira.getId());
                    return "DELETED";
                } catch (ApiException exception) {
                    return exception.getCode();
                }
            });
            start.countDown();

            String creationOutcome = creation.get(10, TimeUnit.SECONDS);
            String deletionOutcome = deletion.get(10, TimeUnit.SECONDS);

            boolean creationWon = creationOutcome.equals("CREATED")
                    && deletionOutcome.equals("CARTEIRA_POSSUI_OPERACOES");
            boolean deletionWon = creationOutcome.equals("PORTFOLIO_NOT_FOUND")
                    && deletionOutcome.equals("DELETED");
            assertTrue(creationWon || deletionWon);

            if (creationWon) {
                assertTrue(carteiraRepository.existsById(carteira.getId()));
                assertTrue(operacaoRepository.existsByCarteiraId(carteira.getId()));
            } else {
                assertFalse(carteiraRepository.existsById(carteira.getId()));
                assertFalse(operacaoRepository.existsByCarteiraId(carteira.getId()));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private String registerAfter(CountDownLatch start, OperacaoCreateRequest request) throws InterruptedException {
        start.await();
        try {
            operacaoService.cadastrar(request);
            return "CREATED";
        } catch (ApiException exception) {
            return exception.getCode();
        }
    }

    private OperacaoCreateRequest request(Long portfolioId, TipoOperacao type, String quantity, int order) {
        return new OperacaoCreateRequest(
                portfolioId,
                "PETR4",
                Mercado.BRASIL,
                null,
                type,
                new BigDecimal(quantity),
                new BigDecimal("10"),
                LocalDate.of(2026, 8, 10),
                order
        );
    }

    private Carteira portfolio(String name) {
        return new Carteira(name, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private Acao action() {
        return new Acao(
                "PETR4",
                "Petrobras",
                Mercado.BRASIL,
                Moeda.BRL,
                new BigDecimal("32.000000"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
    }
}
