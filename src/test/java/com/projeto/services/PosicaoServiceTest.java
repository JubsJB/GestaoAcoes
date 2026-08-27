package com.projeto.services;

import com.projeto.dto.PosicaoResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.mappers.PosicaoMapper;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosicaoServiceTest {

    @Mock
    private CarteiraRepository carteiraRepository;

    @Mock
    private OperacaoRepository operacaoRepository;

    private Carteira carteira;
    private PosicaoService service;
    private long nextOperationId;

    @BeforeEach
    void setUp() {
        carteira = portfolio(1L, "Carteira");
        service = new PosicaoService(
                carteiraRepository,
                operacaoRepository,
                new CalculadoraPosicao(),
                new CalculadoraRentabilidade(),
                new PosicaoMapper()
        );
        nextOperationId = 100L;
    }

    @Test
    void rejectsMissingPortfolioBeforeReadingOperations() {
        when(carteiraRepository.findById(404L)).thenReturn(Optional.empty());

        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> service.listarPorCarteira(404L)
        );

        assertEquals("Carteira não encontrada para o id: 404", exception.getMessage());
        verify(operacaoRepository, never())
                .findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(404L);
    }

    @Test
    void returnsEmptyForPortfolioWithoutOperationsOrOnlyClosedPositions() {
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of());
        assertTrue(service.listarPorCarteira(1L).isEmpty());

        Acao action = action(2L, "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL);
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(
                        operation(carteira, action, TipoOperacao.COMPRA, "10", "5", "2026-08-01", 1),
                        operation(carteira, action, TipoOperacao.VENDA, "10", "8", "2026-08-02", 1)
                ));
        assertTrue(service.listarPorCarteira(1L).isEmpty());
    }

    @Test
    void groupsByActionAndSortsOnlyTheFinalPresentation() {
        Acao petr4 = action(30L, "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL);
        Acao vale3 = action(20L, "VALE3", "Vale", Mercado.BRASIL, Moeda.BRL);
        Acao aapl = action(10L, "AAPL", "Apple", Mercado.EUA, Moeda.USD);
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(
                        operation(carteira, aapl, TipoOperacao.COMPRA, "0.500000", "200", "2026-08-01", 1),
                        operation(carteira, vale3, TipoOperacao.COMPRA, "10", "60", "2026-08-01", 1),
                        operation(carteira, petr4, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                        operation(carteira, petr4, TipoOperacao.VENDA, "40", "99", "2026-08-02", 1),
                        operation(carteira, petr4, TipoOperacao.COMPRA, "40", "20", "2026-08-03", 1)
                ));

        List<PosicaoResponse> responses = service.listarPorCarteira(1L);

        assertEquals(List.of("PETR4", "VALE3", "AAPL"),
                responses.stream().map(PosicaoResponse::ticker).toList());
        PosicaoResponse petrobras = responses.get(0);
        assertEquals(30L, petrobras.acaoId());
        assertEquals("Petrobras", petrobras.nomeEmpresa());
        assertEquals(Mercado.BRASIL, petrobras.mercado());
        assertEquals(Moeda.BRL, petrobras.moeda());
        assertEquals(new BigDecimal("100.000000"), petrobras.quantidadeAtual());
        assertEquals(new BigDecimal("14.000000000000"), petrobras.precoMedio());
        assertEquals(new BigDecimal("1400.000000000000"), petrobras.custoPosicao());
        assertEquals(new BigDecimal("999.999999"), petrobras.cotacaoAtual());
        assertEquals(OffsetDateTime.parse("2026-08-01T10:00:00Z"), petrobras.dataHoraCotacao());
        assertEquals(new BigDecimal("99999.999900000000"), petrobras.valorAtualPosicao());
        assertEquals(new BigDecimal("98599.999900000000"), petrobras.resultadoNaoRealizado());
        assertEquals(new BigDecimal("7042.857136"), petrobras.rentabilidadePercentual());
        assertEquals(new BigDecimal("0.500000"), responses.get(2).quantidadeAtual());
        assertEquals(new BigDecimal("499.999999500000"), responses.get(2).valorAtualPosicao());
        assertEquals(new BigDecimal("399.999999500000"), responses.get(2).resultadoNaoRealizado());
        assertEquals(new BigDecimal("400.000000"), responses.get(2).rentabilidadePercentual());
    }

    @Test
    void isolatesTheRequestedPortfolioAndDoesNotWriteAnything() {
        Acao action = action(2L, "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL);
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(
                        operation(carteira, action, TipoOperacao.COMPRA, "10", "5", "2026-08-01", 1)
                ));

        List<PosicaoResponse> result = service.listarPorCarteira(1L);

        assertEquals(1, result.size());
        verify(carteiraRepository).findById(1L);
        verify(operacaoRepository)
                .findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L);
        verify(carteiraRepository, never()).findByIdForUpdate(1L);
        verify(carteiraRepository, never()).save(any(Carteira.class));
        verify(operacaoRepository, never()).save(any(Operacao.class));
        verify(operacaoRepository, never()).delete(any(Operacao.class));
        verifyNoMoreInteractions(carteiraRepository, operacaoRepository);
    }

    @Test
    void translatesInconsistentHistoryToConflictWithoutPartialResponse() {
        Acao valid = action(2L, "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL);
        Acao invalid = action(3L, "VALE3", "Vale", Mercado.BRASIL, Moeda.BRL);
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(
                        operation(carteira, valid, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                        operation(carteira, invalid, TipoOperacao.VENDA, "1", "20", "2026-08-02", 1)
                ));

        ApiException exception = assertThrows(ApiException.class, () -> service.listarPorCarteira(1L));

        assertEquals(409, exception.getStatus().value());
        assertEquals(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE, exception.getCode());
        assertEquals(new BigDecimal("0"), exception.getDetails().get("quantidadeDisponivel"));
        verify(operacaoRepository, never()).save(any(Operacao.class));
    }

    @Test
    void translatesUnrepresentableCalculationToUnprocessableEntity() {
        Acao action = action(2L, "AAPL", "Apple", Mercado.EUA, Moeda.USD);
        String maximum = "9999999999999.999999";
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(
                        operation(carteira, action, TipoOperacao.COMPRA, maximum, maximum, "2026-08-01", 1),
                        operation(carteira, action, TipoOperacao.COMPRA, maximum, maximum, "2026-08-02", 1)
                ));

        ApiException exception = assertThrows(ApiException.class, () -> service.listarPorCarteira(1L));

        assertEquals(422, exception.getStatus().value());
        assertEquals(ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO, exception.getCode());
        assertFalse(exception.getDetails().isEmpty());
    }

    @Test
    void currentQuoteChangesOnlyCurrentValueAndPreservesAccountingReplayAndTimestamp() {
        OffsetDateTime quoteTimestamp = OffsetDateTime.parse("2026-08-20T15:30:00-03:00");
        Acao action = action(2L, "AAPL", "Apple", Mercado.EUA, Moeda.USD);
        action.atualizarCotacao(new BigDecimal("200.000000"), quoteTimestamp);
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(
                        operation(carteira, action, TipoOperacao.COMPRA, "0.500000", "100", "2026-08-01", 1)
                ));

        PosicaoResponse first = service.listarPorCarteira(1L).get(0);
        action.atualizarCotacao(new BigDecimal("250.000000"), quoteTimestamp.plusHours(1));
        PosicaoResponse second = service.listarPorCarteira(1L).get(0);

        assertEquals(first.quantidadeAtual(), second.quantidadeAtual());
        assertEquals(first.precoMedio(), second.precoMedio());
        assertEquals(first.custoPosicao(), second.custoPosicao());
        assertEquals(new BigDecimal("100.000000000000"), first.valorAtualPosicao());
        assertEquals(new BigDecimal("125.000000000000"), second.valorAtualPosicao());
        assertEquals(new BigDecimal("50.000000000000"), first.resultadoNaoRealizado());
        assertEquals(new BigDecimal("75.000000000000"), second.resultadoNaoRealizado());
        assertEquals(new BigDecimal("100.000000"), first.rentabilidadePercentual());
        assertEquals(new BigDecimal("150.000000"), second.rentabilidadePercentual());
        assertEquals(action.getDataHoraCotacao(), second.dataHoraCotacao());
    }

    @Test
    void partialSaleAndNewCycleUseOnlyTheRemainingOpenPosition() {
        Acao partial = action(2L, "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL);
        partial.atualizarCotacao(
                new BigDecimal("15.000000"),
                OffsetDateTime.parse("2026-08-20T15:30:00Z")
        );
        Acao newCycle = action(3L, "VALE3", "Vale", Mercado.BRASIL, Moeda.BRL);
        newCycle.atualizarCotacao(
                new BigDecimal("25.000000"),
                OffsetDateTime.parse("2026-08-20T15:30:00Z")
        );
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(
                        operation(carteira, partial, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                        operation(carteira, newCycle, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                        operation(carteira, partial, TipoOperacao.VENDA, "40", "99", "2026-08-02", 1),
                        operation(carteira, newCycle, TipoOperacao.VENDA, "100", "99", "2026-08-02", 1),
                        operation(carteira, newCycle, TipoOperacao.COMPRA, "50", "20", "2026-08-03", 1)
                ));

        List<PosicaoResponse> result = service.listarPorCarteira(1L);

        assertEquals(2, result.size());
        PosicaoResponse partialResponse = result.get(0);
        assertEquals(new BigDecimal("60.000000"), partialResponse.quantidadeAtual());
        assertEquals(new BigDecimal("10.000000000000"), partialResponse.precoMedio());
        assertEquals(new BigDecimal("600.000000000000"), partialResponse.custoPosicao());
        assertEquals(new BigDecimal("900.000000000000"), partialResponse.valorAtualPosicao());
        assertEquals(new BigDecimal("300.000000000000"), partialResponse.resultadoNaoRealizado());
        assertEquals(new BigDecimal("50.000000"), partialResponse.rentabilidadePercentual());

        PosicaoResponse newCycleResponse = result.get(1);
        assertEquals(new BigDecimal("50.000000"), newCycleResponse.quantidadeAtual());
        assertEquals(new BigDecimal("20.000000000000"), newCycleResponse.precoMedio());
        assertEquals(new BigDecimal("1000.000000000000"), newCycleResponse.custoPosicao());
        assertEquals(new BigDecimal("1250.000000000000"), newCycleResponse.valorAtualPosicao());
        assertEquals(new BigDecimal("250.000000000000"), newCycleResponse.resultadoNaoRealizado());
        assertEquals(new BigDecimal("25.000000"), newCycleResponse.rentabilidadePercentual());
    }

    @Test
    void rejectsOpenPositionWithNonPositiveCostAsInconsistentWithoutPartialResponse() {
        Acao action = action(2L, "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL);
        Operacao operation = operation(carteira, action, TipoOperacao.COMPRA, "10", "5", "2026-08-01", 1);
        CalculadoraPosicao calculator = mock(CalculadoraPosicao.class);
        CalculadoraRentabilidade profitabilityCalculator = mock(CalculadoraRentabilidade.class);
        when(calculator.reproduzir(any())).thenReturn(new CalculadoraPosicao.ResultadoReplay(
                new CalculadoraPosicao.PosicaoCalculada(
                        new BigDecimal("10.000000"),
                        new BigDecimal("5.000000000000"),
                        BigDecimal.ZERO.setScale(12)
                ),
                null
        ));
        PosicaoService inconsistentService = new PosicaoService(
                carteiraRepository,
                operacaoRepository,
                calculator,
                profitabilityCalculator,
                new PosicaoMapper()
        );
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(operation));

        ApiException exception = assertThrows(ApiException.class, () ->
                inconsistentService.listarPorCarteira(1L));

        assertEquals(409, exception.getStatus().value());
        assertEquals(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE, exception.getCode());
        assertEquals(BigDecimal.ZERO.setScale(12), exception.getDetails().get("custoPosicao"));
        verify(profitabilityCalculator, never()).calcularPercentual(any(), any());
        verify(operacaoRepository, never()).save(any(Operacao.class));
    }

    @Test
    void rejectsOpenPositionWithNegativeCostAsInconsistent() {
        Acao action = action(2L, "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL);
        Operacao operation = operation(carteira, action, TipoOperacao.COMPRA, "10", "5", "2026-08-01", 1);
        CalculadoraPosicao calculator = mock(CalculadoraPosicao.class);
        CalculadoraRentabilidade profitabilityCalculator = mock(CalculadoraRentabilidade.class);
        BigDecimal negativeCost = new BigDecimal("-1.000000000000");
        when(calculator.reproduzir(any())).thenReturn(new CalculadoraPosicao.ResultadoReplay(
                new CalculadoraPosicao.PosicaoCalculada(
                        new BigDecimal("10.000000"),
                        new BigDecimal("5.000000000000"),
                        negativeCost
                ),
                null
        ));
        PosicaoService inconsistentService = new PosicaoService(
                carteiraRepository,
                operacaoRepository,
                calculator,
                profitabilityCalculator,
                new PosicaoMapper()
        );
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(operation));

        ApiException exception = assertThrows(ApiException.class, () ->
                inconsistentService.listarPorCarteira(1L));

        assertEquals(409, exception.getStatus().value());
        assertEquals(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE, exception.getCode());
        assertEquals(negativeCost, exception.getDetails().get("custoPosicao"));
        verify(profitabilityCalculator, never()).calcularPercentual(any(), any());
    }

    @Test
    void usesRepeatableReadReadOnlyTransactionWithoutWriteLock() throws Exception {
        Transactional annotation = PosicaoService.class
                .getMethod("listarPorCarteira", Long.class)
                .getAnnotation(Transactional.class);

        assertTrue(annotation.readOnly());
        assertEquals(Isolation.REPEATABLE_READ, annotation.isolation());
        assertEquals(
                List.of(
                        "carteiraRepository",
                        "operacaoRepository",
                        "calculadora",
                        "calculadoraRentabilidade",
                        "mapper"
                ),
                java.util.Arrays.stream(PosicaoService.class.getDeclaredFields())
                        .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                        .map(field -> field.getName())
                        .toList()
        );
    }

    private Operacao operation(
            Carteira portfolio,
            Acao action,
            TipoOperacao type,
            String quantity,
            String price,
            String date,
            int order
    ) {
        BigDecimal normalizedQuantity = new BigDecimal(quantity).setScale(6);
        BigDecimal normalizedPrice = new BigDecimal(price).setScale(6);
        Operacao value = new Operacao(
                portfolio,
                action,
                null,
                type,
                normalizedQuantity,
                normalizedPrice,
                LocalDate.parse(date),
                order,
                normalizedQuantity.multiply(normalizedPrice).setScale(12)
        );
        ReflectionTestUtils.setField(value, "id", nextOperationId++);
        return value;
    }

    private Carteira portfolio(Long id, String name) {
        Carteira value = new Carteira(name, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private Acao action(Long id, String ticker, String name, Mercado market, Moeda currency) {
        Acao value = new Acao(
                ticker,
                name,
                market,
                currency,
                new BigDecimal("999.999999"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
