package com.projeto.services;

import com.projeto.dto.ResultadoRealizadoResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.mappers.ResultadoRealizadoMapper;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultadoRealizadoServiceTest {

    @Mock
    private CarteiraRepository carteiraRepository;

    @Mock
    private OperacaoRepository operacaoRepository;

    private ResultadoRealizadoService service;
    private Carteira carteira;

    @BeforeEach
    void setUp() {
        service = new ResultadoRealizadoService(
                carteiraRepository,
                operacaoRepository,
                new CalculadoraPosicao(),
                new ResultadoRealizadoMapper()
        );
        carteira = new Carteira("Carteira", OffsetDateTime.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(carteira, "id", 1L);
    }

    @Test
    void declaresRepeatableReadOnlyTransaction() throws Exception {
        Transactional transactional = ResultadoRealizadoService.class
                .getMethod("listarPorCarteira", Long.class)
                .getAnnotation(Transactional.class);

        assertTrue(transactional.readOnly());
        assertEquals(Isolation.REPEATABLE_READ, transactional.isolation());
    }

    @Test
    void rejectsMissingPortfolioBeforeReadingHistory() {
        when(carteiraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundException.class, () -> service.listarPorCarteira(99L));

        verify(operacaoRepository, never())
                .findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(any());
    }

    @Test
    void returnsEmptyForNoOperationsOrPurchasesOnlyWithoutWriting() {
        Acao acao = action(2L, "PETR4", Mercado.BRASIL, Moeda.BRL);
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(operation(acao, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1)));

        assertTrue(service.listarPorCarteira(1L).isEmpty());

        verify(operacaoRepository, never()).save(any(Operacao.class));
        verify(operacaoRepository, never()).saveAndFlush(any(Operacao.class));
    }

    @Test
    void accumulatesPerActionIncludesZeroAndOrdersWithoutMixingGroups() {
        Acao petr4 = action(2L, "PETR4", Mercado.BRASIL, Moeda.BRL);
        Acao vale3 = action(3L, "VALE3", Mercado.BRASIL, Moeda.BRL);
        Acao aapl = action(4L, "AAPL", Mercado.EUA, Moeda.USD);
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(
                        operation(aapl, TipoOperacao.COMPRA, "0.500000", "200", "2026-08-01", 1),
                        operation(petr4, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                        operation(vale3, TipoOperacao.COMPRA, "10", "60", "2026-08-01", 1),
                        operation(petr4, TipoOperacao.VENDA, "20", "15", "2026-08-02", 1),
                        operation(vale3, TipoOperacao.VENDA, "5", "60", "2026-08-02", 1),
                        operation(aapl, TipoOperacao.VENDA, "0.250000", "220", "2026-08-02", 1),
                        operation(petr4, TipoOperacao.VENDA, "30", "8", "2026-08-03", 1)
                ));

        List<ResultadoRealizadoResponse> responses = service.listarPorCarteira(1L);

        assertEquals(List.of("PETR4", "VALE3", "AAPL"), responses.stream().map(ResultadoRealizadoResponse::ticker).toList());
        assertEquals(new BigDecimal("40.000000000000"), responses.get(0).resultadoRealizado());
        assertEquals(new BigDecimal("0.000000000000"), responses.get(1).resultadoRealizado());
        assertEquals(new BigDecimal("5.000000000000"), responses.get(2).resultadoRealizado());
        assertEquals(Moeda.BRL, responses.get(0).moeda());
        assertEquals(Moeda.USD, responses.get(2).moeda());
    }

    @Test
    void returnsClosedAndOpenCyclesWithHistoricalAccumulation() {
        Acao closed = action(2L, "PETR4", Mercado.BRASIL, Moeda.BRL);
        Acao open = action(3L, "VALE3", Mercado.BRASIL, Moeda.BRL);
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(
                        operation(closed, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                        operation(open, TipoOperacao.COMPRA, "100", "20", "2026-08-01", 1),
                        operation(closed, TipoOperacao.VENDA, "100", "15", "2026-08-02", 1),
                        operation(open, TipoOperacao.VENDA, "30", "15", "2026-08-02", 1),
                        operation(closed, TipoOperacao.COMPRA, "50", "20", "2026-08-03", 1),
                        operation(closed, TipoOperacao.VENDA, "50", "18", "2026-08-04", 1)
                ));

        List<ResultadoRealizadoResponse> responses = service.listarPorCarteira(1L);

        assertEquals(new BigDecimal("400.000000000000"), responses.get(0).resultadoRealizado());
        assertEquals(new BigDecimal("-150.000000000000"), responses.get(1).resultadoRealizado());
    }

    @Test
    void translatesInconsistentHistoryWithoutPartialResponse() {
        Acao valid = action(2L, "PETR4", Mercado.BRASIL, Moeda.BRL);
        Acao invalid = action(3L, "VALE3", Mercado.BRASIL, Moeda.BRL);
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(
                        operation(valid, TipoOperacao.COMPRA, "10", "10", "2026-08-01", 1),
                        operation(valid, TipoOperacao.VENDA, "1", "11", "2026-08-02", 1),
                        operation(invalid, TipoOperacao.VENDA, "1", "60", "2026-08-03", 1)
                ));

        ApiException exception = assertThrows(ApiException.class, () -> service.listarPorCarteira(1L));

        assertEquals(409, exception.getStatus().value());
        assertEquals(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE, exception.getCode());
        assertFalse(exception.getDetails().isEmpty());
    }

    @Test
    void translatesPrecisionFailureToUnprocessableEntity() {
        CalculadoraPosicao calculator = org.mockito.Mockito.mock(CalculadoraPosicao.class);
        ResultadoRealizadoService precisionService = new ResultadoRealizadoService(
                carteiraRepository,
                operacaoRepository,
                calculator,
                new ResultadoRealizadoMapper()
        );
        Acao acao = action(2L, "PETR4", Mercado.BRASIL, Moeda.BRL);
        Operacao operacao = operation(acao, TipoOperacao.COMPRA, "1", "10", "2026-08-01", 1);
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteira));
        when(operacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(1L))
                .thenReturn(List.of(operacao));
        when(calculator.reproduzir(any())).thenReturn(new CalculadoraPosicao.ResultadoReplay(
                null,
                new CalculadoraPosicao.FalhaReplay(
                        CalculadoraPosicao.TipoFalha.CALCULO_FORA_DA_PRECISAO,
                        "fora da precisão",
                        operacao,
                        null,
                        null
                )
        ));

        ApiException exception = assertThrows(ApiException.class, () -> precisionService.listarPorCarteira(1L));

        assertEquals(422, exception.getStatus().value());
        assertEquals(ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO, exception.getCode());
        verify(calculator).reproduzir(any());
    }

    private Acao action(Long id, String ticker, Mercado mercado, Moeda moeda) {
        Acao acao = new Acao(
                ticker,
                ticker + " Empresa",
                mercado,
                moeda,
                new BigDecimal("99.123456"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
        ReflectionTestUtils.setField(acao, "id", id);
        return acao;
    }

    private Operacao operation(
            Acao acao,
            TipoOperacao tipo,
            String quantidade,
            String preco,
            String data,
            int ordem
    ) {
        BigDecimal quantity = new BigDecimal(quantidade).setScale(6);
        BigDecimal price = new BigDecimal(preco).setScale(6);
        Operacao operacao = new Operacao(
                carteira,
                acao,
                null,
                tipo,
                quantity,
                price,
                LocalDate.parse(data),
                ordem,
                quantity.multiply(price).setScale(12)
        );
        ReflectionTestUtils.setField(operacao, "id", (long) (data.hashCode() * 31 + ordem + acao.getId()));
        return operacao;
    }
}
