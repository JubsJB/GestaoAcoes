package com.projeto.services;

import com.projeto.dto.OperacaoCreateRequest;
import com.projeto.dto.OperacaoResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Corretora;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.mappers.OperacaoMapper;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.CorretoraRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.validation.TickerNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperacaoServiceTest {

    private static final Clock DEFAULT_CLOCK = Clock.fixed(
            Instant.parse("2026-08-23T15:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private OperacaoRepository operacaoRepository;

    @Mock
    private CarteiraRepository carteiraRepository;

    @Mock
    private AcaoRepository acaoRepository;

    @Mock
    private CorretoraRepository corretoraRepository;

    private Carteira carteira;
    private Acao brazilianAction;
    private Acao americanAction;
    private OperacaoService service;

    @BeforeEach
    void setUp() {
        carteira = portfolio(1L, "Carteira Principal");
        brazilianAction = action(2L, "PETR4", Mercado.BRASIL, Moeda.BRL, "99.123456");
        americanAction = action(3L, "AAPL", Mercado.EUA, Moeda.USD, "224.410000");
        service = service(DEFAULT_CLOCK);

        lenient().when(carteiraRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(carteira));
        lenient().when(acaoRepository.findByTickerAndMercado("PETR4", Mercado.BRASIL))
                .thenReturn(Optional.of(brazilianAction));
        lenient().when(acaoRepository.findByTickerAndMercado("AAPL", Mercado.EUA))
                .thenReturn(Optional.of(americanAction));
        lenient().when(operacaoRepository
                        .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(anyLong(), anyLong()))
                .thenReturn(List.of());
        lenient().when(operacaoRepository.saveAndFlush(any(Operacao.class))).thenAnswer(invocation -> {
            Operacao operation = invocation.getArgument(0);
            ReflectionTestUtils.setField(operation, "id", 10L);
            return operation;
        });
    }

    @Test
    void listsPersistedOperationsWithApprovedSortAndExactMappedValues() {
        Sort approvedOrder = Sort.by(
                Sort.Order.asc("dataOperacao"),
                Sort.Order.asc("ordemNoDia"),
                Sort.Order.asc("id")
        );
        Operacao first = operation(TipoOperacao.COMPRA, "100", LocalDate.of(2026, 8, 1), 5);
        Operacao second = operation(TipoOperacao.VENDA, "20", LocalDate.of(2026, 8, 10), 1);
        ReflectionTestUtils.setField(first, "id", 11L);
        ReflectionTestUtils.setField(second, "id", 12L);
        when(operacaoRepository.findAll(approvedOrder)).thenReturn(List.of(first, second));

        List<OperacaoResponse> responses = service.listar();

        assertEquals(List.of(11L, 12L), responses.stream().map(OperacaoResponse::id).toList());
        assertEquals(TipoOperacao.COMPRA, responses.get(0).tipo());
        assertEquals(TipoOperacao.VENDA, responses.get(1).tipo());
        assertEquals(new BigDecimal("100.000000"), responses.get(0).quantidade());
        assertEquals(new BigDecimal("10.000000"), responses.get(0).precoUnitario());
        assertEquals(new BigDecimal("1000.000000000000"), responses.get(0).valorTotal());
        assertNull(responses.get(0).corretoraId());
        verify(operacaoRepository).findAll(approvedOrder);
        verify(operacaoRepository, never()).saveAndFlush(any(Operacao.class));
        verify(operacaoRepository, never())
                .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(anyLong(), anyLong());
    }

    @Test
    void findsPersistedOperationByIdAndRejectsMissingIdWithoutWrites() {
        Operacao persisted = operation(TipoOperacao.COMPRA, "2", LocalDate.of(2026, 8, 2), 1);
        ReflectionTestUtils.setField(persisted, "id", 10L);
        when(operacaoRepository.findById(10L)).thenReturn(Optional.of(persisted));
        when(operacaoRepository.findById(404L)).thenReturn(Optional.empty());

        OperacaoResponse response = service.buscarPorId(10L);
        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> service.buscarPorId(404L)
        );

        assertEquals(10L, response.id());
        assertEquals(1L, response.carteiraId());
        assertEquals("PETR4", response.ticker());
        assertEquals("Operação não encontrada para o id: 404", exception.getMessage());
        verify(operacaoRepository, never()).saveAndFlush(any(Operacao.class));
    }

    @Test
    void listsOnlyValidatedPortfolioHistoryAndReturnsEmptyOrNotFoundAsRequired() {
        Operacao first = operation(TipoOperacao.COMPRA, "100", LocalDate.of(2026, 8, 1), 1);
        Operacao second = operation(TipoOperacao.VENDA, "10", LocalDate.of(2026, 8, 10), 2);
        ReflectionTestUtils.setField(first, "id", 20L);
        ReflectionTestUtils.setField(second, "id", 21L);
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(first, second));

        List<OperacaoResponse> history = service.listarPorCarteira(1L);

        assertEquals(List.of(20L, 21L), history.stream().map(OperacaoResponse::id).toList());
        assertTrue(history.stream().allMatch(item -> item.carteiraId().equals(1L)));

        Carteira empty = portfolio(2L, "Carteira vazia");
        when(carteiraRepository.findById(2L)).thenReturn(Optional.of(empty));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(2L))
                .thenReturn(List.of());
        assertTrue(service.listarPorCarteira(2L).isEmpty());

        when(carteiraRepository.findById(404L)).thenReturn(Optional.empty());
        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> service.listarPorCarteira(404L)
        );
        assertEquals("Carteira não encontrada para o id: 404", exception.getMessage());
        verify(operacaoRepository, never())
                .findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(404L);
        verify(carteiraRepository, never()).findByIdForUpdate(1L);
        verify(operacaoRepository, never()).saveAndFlush(any(Operacao.class));
    }

    @Test
    void queryMethodsDoNotUseClockReplayLocksNormalizationOrPersistence() {
        Clock queryClock = mock(Clock.class);
        OperacaoService queryService = service(queryClock);
        Operacao persisted = operation(TipoOperacao.COMPRA, "1", LocalDate.of(2026, 8, 1), 1);
        ReflectionTestUtils.setField(persisted, "id", 30L);
        Sort approvedOrder = Sort.by(
                Sort.Order.asc("dataOperacao"),
                Sort.Order.asc("ordemNoDia"),
                Sort.Order.asc("id")
        );
        when(operacaoRepository.findAll(approvedOrder)).thenReturn(List.of(persisted));
        when(operacaoRepository.findById(30L)).thenReturn(Optional.of(persisted));
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(persisted));

        queryService.listar();
        queryService.buscarPorId(30L);
        queryService.listarPorCarteira(1L);

        verifyNoInteractions(queryClock);
        verify(carteiraRepository, never()).findByIdForUpdate(anyLong());
        verify(operacaoRepository, never())
                .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(anyLong(), anyLong());
        verify(operacaoRepository, never()).save(any(Operacao.class));
        verify(operacaoRepository, never()).saveAndFlush(any(Operacao.class));
        verify(operacaoRepository, never()).delete(any(Operacao.class));
        verifyNoInteractions(acaoRepository, corretoraRepository);
    }

    @Test
    void registersBrazilianPurchaseWithNormalizedTickerRealPriceAndExactTotal() {
        OperacaoResponse response = service.cadastrar(request(
                "  petr4  ",
                Mercado.BRASIL,
                TipoOperacao.COMPRA,
                "100",
                "32.47",
                LocalDate.of(2026, 8, 10),
                1,
                null
        ));

        ArgumentCaptor<Operacao> captor = ArgumentCaptor.forClass(Operacao.class);
        verify(operacaoRepository).saveAndFlush(captor.capture());
        Operacao saved = captor.getValue();
        assertEquals(carteira, saved.getCarteira());
        assertEquals(brazilianAction, saved.getAcao());
        assertNull(saved.getCorretora());
        assertEquals(new BigDecimal("100.000000"), saved.getQuantidade());
        assertEquals(new BigDecimal("32.470000"), saved.getPrecoUnitario());
        assertEquals(new BigDecimal("3247.000000000000"), saved.getValorTotal());
        assertEquals("PETR4", response.ticker());
        assertEquals(new BigDecimal("32.470000"), response.precoUnitario());
        assertEquals(new BigDecimal("3247.000000000000"), response.valorTotal());
        assertEquals(new BigDecimal("99.123456"), brazilianAction.getCotacaoAtual());
        assertEquals("Carteira Principal", carteira.getNome());
        verify(acaoRepository).findByTickerAndMercado("PETR4", Mercado.BRASIL);
        verifyNoInteractions(corretoraRepository);
    }

    @Test
    void associatesOptionalBrokerAndRejectsMissingBroker() {
        Corretora broker = broker(3L);
        when(corretoraRepository.findById(3L)).thenReturn(Optional.of(broker));

        OperacaoResponse response = service.cadastrar(request(
                "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                "1", "10", LocalDate.of(2026, 8, 10), 1, 3L
        ));
        assertEquals(3L, response.corretoraId());

        when(corretoraRepository.findById(404L)).thenReturn(Optional.empty());
        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> service.cadastrar(request(
                        "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                        "1", "10", LocalDate.of(2026, 8, 10), 2, 404L
                ))
        );
        assertEquals("Corretora não encontrada para o id: 404", exception.getMessage());
    }

    @Test
    void rejectsMissingPortfolioOrActionWithoutCreatingAnything() {
        when(carteiraRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());
        ObjectNotFoundException missingPortfolio = assertThrows(
                ObjectNotFoundException.class,
                () -> service.cadastrar(request(
                        "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                        "1", "10", LocalDate.of(2026, 8, 10), 1, null, 404L
                ))
        );
        assertTrue(missingPortfolio.getMessage().contains("404"));

        when(acaoRepository.findByTickerAndMercado("VALE3", Mercado.BRASIL)).thenReturn(Optional.empty());
        ObjectNotFoundException missingAction = assertThrows(
                ObjectNotFoundException.class,
                () -> service.cadastrar(request(
                        " vale3 ", Mercado.BRASIL, TipoOperacao.COMPRA,
                        "1", "10", LocalDate.of(2026, 8, 10), 1, null
                ))
        );
        assertTrue(missingAction.getMessage().contains("VALE3"));
        verify(operacaoRepository, never()).saveAndFlush(any(Operacao.class));
    }

    @Test
    void appliesBrazilianIntegerQuantityRuleWithoutRejectingDecimalZeroRepresentation() {
        OperacaoResponse accepted = service.cadastrar(request(
                "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                "100.000000", "1", LocalDate.of(2026, 8, 10), 1, null
        ));
        assertEquals(new BigDecimal("100.000000"), accepted.quantidade());

        for (String invalid : List.of("0.5", "10.25")) {
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> service.cadastrar(request(
                            "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                            invalid, "1", LocalDate.of(2026, 8, 10), 2, null
                    ))
            );
            assertInvalidField(exception, "quantidade");
        }
    }

    @Test
    void rejectsNonPositiveAndOutOfPrecisionBrazilianQuantities() {
        for (String invalid : List.of("0", "-1", "10000000000000")) {
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> service.cadastrar(request(
                            "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                            invalid, "1", LocalDate.of(2026, 8, 10), 1, null
                    ))
            );
            assertInvalidField(exception, "quantidade");
        }
    }

    @Test
    void acceptsAmericanIntegerAndFractionAndRejectsScaleOrPrecisionOverflow() {
        OperacaoResponse integer = service.cadastrar(request(
                "AAPL", Mercado.EUA, TipoOperacao.COMPRA,
                "2", "10", LocalDate.of(2026, 8, 10), 1, null
        ));
        OperacaoResponse fraction = service.cadastrar(request(
                "AAPL", Mercado.EUA, TipoOperacao.COMPRA,
                "0.123456", "10", LocalDate.of(2026, 8, 10), 2, null
        ));
        assertEquals(new BigDecimal("2.000000"), integer.quantidade());
        assertEquals(new BigDecimal("0.123456"), fraction.quantidade());

        for (String invalid : List.of("0.1234567", "0", "-0.1", "10000000000000")) {
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> service.cadastrar(request(
                            "AAPL", Mercado.EUA, TipoOperacao.COMPRA,
                            invalid, "10", LocalDate.of(2026, 8, 10), 3, null
                    ))
            );
            assertInvalidField(exception, "quantidade");
        }
    }

    @Test
    void validatesUnitPriceAndNeverUsesCurrentQuoteInTotal() {
        OperacaoResponse response = service.cadastrar(request(
                "AAPL", Mercado.EUA, TipoOperacao.COMPRA,
                "0.500000", "32.123456", LocalDate.of(2026, 8, 10), 1, null
        ));
        assertEquals(new BigDecimal("32.123456"), response.precoUnitario());
        assertEquals(new BigDecimal("16.061728000000"), response.valorTotal());
        assertEquals(new BigDecimal("224.410000"), americanAction.getCotacaoAtual());

        for (String invalid : List.of("0", "-1", "1.1234567", "10000000000000")) {
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> service.cadastrar(request(
                            "AAPL", Mercado.EUA, TipoOperacao.COMPRA,
                            "1", invalid, LocalDate.of(2026, 8, 10), 2, null
                    ))
            );
            assertInvalidField(exception, "precoUnitario");
        }
    }

    @Test
    void appliesMarketCivilDatesInsteadOfUtcDate() {
        OperacaoService boundaryService = service(Clock.fixed(
                Instant.parse("2026-08-24T03:30:00Z"),
                ZoneOffset.UTC
        ));

        OperacaoResponse brazilToday = boundaryService.cadastrar(request(
                "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                "1", "10", LocalDate.of(2026, 8, 24), 1, null
        ));
        assertEquals(LocalDate.of(2026, 8, 24), brazilToday.dataOperacao());

        ApiException brazilFuture = assertThrows(
                ApiException.class,
                () -> boundaryService.cadastrar(request(
                        "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                        "1", "10", LocalDate.of(2026, 8, 25), 2, null
                ))
        );
        assertInvalidField(brazilFuture, "dataOperacao");

        ApiException usaFuture = assertThrows(
                ApiException.class,
                () -> boundaryService.cadastrar(request(
                        "AAPL", Mercado.EUA, TipoOperacao.COMPRA,
                        "1", "10", LocalDate.of(2026, 8, 24), 1, null
                ))
        );
        assertInvalidField(usaFuture, "dataOperacao");

        OperacaoResponse usaToday = boundaryService.cadastrar(request(
                "AAPL", Mercado.EUA, TipoOperacao.COMPRA,
                "1", "10", LocalDate.of(2026, 8, 23), 2, null
        ));
        assertEquals(LocalDate.of(2026, 8, 23), usaToday.dataOperacao());
    }

    @Test
    void rejectsNonPositiveOrDuplicateDailyOrder() {
        for (int invalid : List.of(0, -1)) {
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> service.cadastrar(request(
                            "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                            "1", "10", LocalDate.of(2026, 8, 10), invalid, null
                    ))
            );
            assertInvalidField(exception, "ordemNoDia");
        }

        when(operacaoRepository.existsByCarteiraIdAndAcaoIdAndDataOperacaoAndOrdemNoDia(
                1L, 2L, LocalDate.of(2026, 8, 10), 1
        )).thenReturn(true);
        ApiException duplicate = assertThrows(
                ApiException.class,
                () -> service.cadastrar(request(
                        "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                        "1", "10", LocalDate.of(2026, 8, 10), 1, null
                ))
        );
        assertEquals(409, duplicate.getStatus().value());
        assertEquals(ErrorCodes.ORDEM_OPERACAO_DUPLICADA, duplicate.getCode());
    }

    @Test
    void permitsSaleWithinOrExactlyAtAvailablePositionAfterMultipleOperations() {
        when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(1L, 2L))
                .thenReturn(List.of(
                        operation(TipoOperacao.COMPRA, "60", LocalDate.of(2026, 8, 1), 1),
                        operation(TipoOperacao.COMPRA, "40", LocalDate.of(2026, 8, 2), 1),
                        operation(TipoOperacao.VENDA, "20", LocalDate.of(2026, 8, 3), 1)
                ));

        OperacaoResponse partial = service.cadastrar(request(
                "PETR4", Mercado.BRASIL, TipoOperacao.VENDA,
                "50", "40", LocalDate.of(2026, 8, 4), 1, null
        ));
        assertEquals(TipoOperacao.VENDA, partial.tipo());

        OperacaoResponse exact = service.cadastrar(request(
                "PETR4", Mercado.BRASIL, TipoOperacao.VENDA,
                "80", "40", LocalDate.of(2026, 8, 4), 2, null
        ));
        assertEquals(new BigDecimal("80.000000"), exact.quantidade());
    }

    @Test
    void rejectsSaleAbovePositionAndPreservesExistingHistory() {
        Operacao existing = operation(TipoOperacao.COMPRA, "100", LocalDate.of(2026, 8, 1), 1);
        when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(1L, 2L))
                .thenReturn(List.of(existing));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cadastrar(request(
                        "PETR4", Mercado.BRASIL, TipoOperacao.VENDA,
                        "101", "40", LocalDate.of(2026, 8, 2), 1, null
                ))
        );

        assertEquals(409, exception.getStatus().value());
        assertEquals(ErrorCodes.POSICAO_INSUFICIENTE, exception.getCode());
        assertEquals(new BigDecimal("100.000000"), exception.getDetails().get("quantidadeDisponivel"));
        verify(operacaoRepository, never()).saveAndFlush(any(Operacao.class));
        assertEquals(new BigDecimal("100.000000"), existing.getQuantidade());
    }

    @Test
    void replaysRetroactiveCandidateByDateAndDailyOrderAndRejectsInvalidLaterSale() {
        when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(1L, 2L))
                .thenReturn(List.of(
                        operation(TipoOperacao.COMPRA, "100", LocalDate.of(2026, 8, 1), 1),
                        operation(TipoOperacao.VENDA, "80", LocalDate.of(2026, 8, 10), 1)
                ));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cadastrar(request(
                        "PETR4", Mercado.BRASIL, TipoOperacao.VENDA,
                        "30", "40", LocalDate.of(2026, 8, 5), 1, null
                ))
        );
        assertEquals(ErrorCodes.POSICAO_INSUFICIENTE, exception.getCode());
        assertEquals(new BigDecimal("70.000000"), exception.getDetails().get("quantidadeDisponivel"));
        verify(operacaoRepository, never()).saveAndFlush(any(Operacao.class));
    }

    @Test
    void acceptsValidRetroactivePurchaseAndUsesOnlySamePortfolioAndActionHistory() {
        when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(1L, 2L))
                .thenReturn(List.of(
                        operation(TipoOperacao.COMPRA, "20", LocalDate.of(2026, 8, 10), 1),
                        operation(TipoOperacao.VENDA, "20", LocalDate.of(2026, 8, 10), 2)
                ));

        OperacaoResponse response = service.cadastrar(request(
                "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                "10", "20", LocalDate.of(2026, 8, 5), 1, null
        ));

        assertEquals(TipoOperacao.COMPRA, response.tipo());
        verify(operacaoRepository)
                .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(1L, 2L);
    }

    @Test
    void doesNotUseBalancesFromAnotherPortfolioOrAction() {
        Carteira otherPortfolio = portfolio(99L, "Outra Carteira");
        Acao otherAction = action(98L, "VALE3", Mercado.BRASIL, Moeda.BRL, "50.000000");
        Operacao otherPortfolioPurchase = new Operacao(
                otherPortfolio,
                brazilianAction,
                null,
                TipoOperacao.COMPRA,
                new BigDecimal("100.000000"),
                new BigDecimal("10.000000"),
                LocalDate.of(2026, 8, 1),
                1,
                new BigDecimal("1000.000000000000")
        );
        Operacao otherActionPurchase = new Operacao(
                carteira,
                otherAction,
                null,
                TipoOperacao.COMPRA,
                new BigDecimal("100.000000"),
                new BigDecimal("10.000000"),
                LocalDate.of(2026, 8, 1),
                1,
                new BigDecimal("1000.000000000000")
        );
        lenient().when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(99L, 2L))
                .thenReturn(List.of(otherPortfolioPurchase));
        lenient().when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(1L, 98L))
                .thenReturn(List.of(otherActionPurchase));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cadastrar(request(
                        "PETR4", Mercado.BRASIL, TipoOperacao.VENDA,
                        "1", "10", LocalDate.of(2026, 8, 2), 1, null
                ))
        );

        assertEquals(ErrorCodes.POSICAO_INSUFICIENTE, exception.getCode());
        verify(operacaoRepository)
                .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(1L, 2L);
        verify(operacaoRepository, never())
                .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(99L, 2L);
        verify(operacaoRepository, never())
                .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(1L, 98L);
    }

    @Test
    void translatesKnownDatabaseOrderConstraintAndPropagatesOtherIntegrityFailures() {
        when(operacaoRepository.saveAndFlush(any(Operacao.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Violation of uk_operacao_carteira_acao_data_ordem"
                ));
        ApiException duplicate = assertThrows(
                ApiException.class,
                () -> service.cadastrar(request(
                        "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                        "1", "10", LocalDate.of(2026, 8, 10), 1, null
                ))
        );
        assertEquals(ErrorCodes.ORDEM_OPERACAO_DUPLICADA, duplicate.getCode());

        when(operacaoRepository.saveAndFlush(any(Operacao.class)))
                .thenThrow(new DataIntegrityViolationException("different constraint"));
        assertThrows(
                DataIntegrityViolationException.class,
                () -> service.cadastrar(request(
                        "PETR4", Mercado.BRASIL, TipoOperacao.COMPRA,
                        "1", "10", LocalDate.of(2026, 8, 10), 2, null
                ))
        );
    }

    private OperacaoService service(Clock clock) {
        return new OperacaoService(
                operacaoRepository,
                carteiraRepository,
                acaoRepository,
                corretoraRepository,
                new TickerNormalizer(),
                new OperacaoMapper(),
                clock,
                new CalculadoraPosicao()
        );
    }

    private OperacaoCreateRequest request(
            String ticker,
            Mercado market,
            TipoOperacao type,
            String quantity,
            String price,
            LocalDate date,
            Integer order,
            Long brokerId
    ) {
        return request(ticker, market, type, quantity, price, date, order, brokerId, 1L);
    }

    private OperacaoCreateRequest request(
            String ticker,
            Mercado market,
            TipoOperacao type,
            String quantity,
            String price,
            LocalDate date,
            Integer order,
            Long brokerId,
            Long portfolioId
    ) {
        return new OperacaoCreateRequest(
                portfolioId,
                ticker,
                market,
                brokerId,
                type,
                new BigDecimal(quantity),
                new BigDecimal(price),
                date,
                order
        );
    }

    private Operacao operation(TipoOperacao type, String quantity, LocalDate date, Integer order) {
        BigDecimal normalizedQuantity = new BigDecimal(quantity).setScale(6);
        return new Operacao(
                carteira,
                brazilianAction,
                null,
                type,
                normalizedQuantity,
                new BigDecimal("10.000000"),
                date,
                order,
                normalizedQuantity.multiply(new BigDecimal("10.000000")).setScale(12)
        );
    }

    private Carteira portfolio(Long id, String name) {
        Carteira value = new Carteira(name, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private Acao action(Long id, String ticker, Mercado market, Moeda currency, String quote) {
        Acao value = new Acao(
                ticker,
                "Empresa",
                market,
                currency,
                new BigDecimal(quote),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private Corretora broker(Long id) {
        Corretora value = new Corretora(
                "11222333000181",
                "Corretora S.A.",
                null,
                null,
                null,
                "01001000",
                "Praça da Sé",
                null,
                null,
                "Sé",
                "São Paulo",
                "SP",
                "ATIVA",
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private void assertInvalidField(ApiException exception, String field) {
        assertEquals(400, exception.getStatus().value());
        assertEquals(ErrorCodes.REQUEST_INVALIDO, exception.getCode());
        assertTrue(exception.getDetails().containsKey(field));
    }
}
