package com.projeto.services;

import com.projeto.dto.PosicaoResponse;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.services.AgregadorPosicoesPorMoeda.FalhaAgregacaoException;
import com.projeto.services.AgregadorPosicoesPorMoeda.Indicador;
import com.projeto.services.AgregadorPosicoesPorMoeda.TotaisPorMoeda;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgregadorPosicoesPorMoedaTest {

    private final AgregadorPosicoesPorMoeda agregador = new AgregadorPosicoesPorMoeda();

    @Test
    void aggregatesBrlAndUsdIndependentlyInCurrencyOrder() {
        List<TotaisPorMoeda> totais = agregador.agregar(List.of(
                posicao(1L, Mercado.EUA, Moeda.USD, "100.000000000000",
                        "112.205000000000", "12.205000000000"),
                posicao(2L, Mercado.BRASIL, Moeda.BRL, "600.000000000000",
                        "700.000000000000", "100.000000000000"),
                posicao(3L, Mercado.BRASIL, Moeda.BRL, "3200.000000000000",
                        "3550.000000000000", "350.000000000000")
        ));

        assertEquals(2, totais.size());
        assertEquals(Moeda.BRL, totais.get(0).moeda());
        assertEquals(new BigDecimal("3800.000000000000"), totais.get(0).custoTotalPosicoes());
        assertEquals(new BigDecimal("4250.000000000000"), totais.get(0).patrimonioAtual());
        assertEquals(new BigDecimal("450.000000000000"),
                totais.get(0).resultadoNaoRealizadoTotal());
        assertEquals(Moeda.USD, totais.get(1).moeda());
        assertEquals(new BigDecimal("112.205000000000"), totais.get(1).patrimonioAtual());
    }

    @Test
    void preservesPositiveNegativeAndZeroUnrealizedResultsWithoutDerivingThem() {
        List<TotaisPorMoeda> totais = agregador.agregar(List.of(
                posicao(1L, Mercado.BRASIL, Moeda.BRL, "10", "15", "5"),
                posicao(2L, Mercado.BRASIL, Moeda.BRL, "20", "17", "-3"),
                posicao(3L, Mercado.BRASIL, Moeda.BRL, "30", "28", "-2")
        ));

        TotaisPorMoeda total = totais.get(0);
        assertEquals(new BigDecimal("0.000000000000"), total.resultadoNaoRealizadoTotal());
        assertEquals(total.resultadoNaoRealizadoTotal(),
                total.patrimonioAtual().subtract(total.custoTotalPosicoes()));
        assertEquals(12, total.resultadoNaoRealizadoTotal().scale());
    }

    @Test
    void preservesNegativeAndZeroAccumulatedResults() {
        List<TotaisPorMoeda> totais = agregador.agregar(List.of(
                posicao(1L, Mercado.BRASIL, Moeda.BRL, "20", "15", "-5"),
                posicao(2L, Mercado.EUA, Moeda.USD, "10", "10", "0")
        ));

        assertEquals(new BigDecimal("-5.000000000000"),
                totais.get(0).resultadoNaoRealizadoTotal());
        assertEquals(new BigDecimal("0.000000000000"),
                totais.get(1).resultadoNaoRealizadoTotal());
    }

    @Test
    void normalizesOnlyAfterExactAddition() {
        TotaisPorMoeda total = agregador.agregar(List.of(
                posicao(1L, Mercado.BRASIL, Moeda.BRL, "0.1", "0.2", "0.1"),
                posicao(2L, Mercado.BRASIL, Moeda.BRL, "0.2", "0.3", "0.1")
        )).get(0);

        assertEquals(new BigDecimal("0.300000000000"), total.custoTotalPosicoes());
        assertEquals(new BigDecimal("0.500000000000"), total.patrimonioAtual());
        assertEquals(new BigDecimal("0.200000000000"), total.resultadoNaoRealizadoTotal());
    }

    @Test
    void identifiesEachAccumulatorThatCannotBeRepresented() {
        assertFailure("1.0000000000001", "2", "1", Indicador.CUSTO_TOTAL_POSICOES);
        assertFailure("1", "2.0000000000001", "1", Indicador.PATRIMONIO_ATUAL);
        assertFailure("1", "2", "1.0000000000001", Indicador.RESULTADO_NAO_REALIZADO_TOTAL);
    }

    @Test
    void rejectsPrecisionAboveThirtyEightWithoutReturningPartialCurrencies() {
        FalhaAgregacaoException exception = assertThrows(
                FalhaAgregacaoException.class,
                () -> agregador.agregar(List.of(
                        posicao(1L, Mercado.BRASIL, Moeda.BRL,
                                "99999999999999999999999999.999999999999", "1", "1"),
                        posicao(2L, Mercado.BRASIL, Moeda.BRL,
                                "0.000000000001", "1", "1"),
                        posicao(3L, Mercado.EUA, Moeda.USD, "1", "1", "0")
                ))
        );

        assertEquals(Moeda.BRL, exception.moeda());
        assertEquals(Indicador.CUSTO_TOTAL_POSICOES, exception.indicador());
    }

    @Test
    void isPureAndHasNoTransactionalOrInfrastructureDependencies() {
        assertFalse(AgregadorPosicoesPorMoeda.class.isAnnotationPresent(Transactional.class));
        assertTrue(Arrays.stream(AgregadorPosicoesPorMoeda.class.getDeclaredFields())
                .allMatch(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers())));
        assertEquals(List.of(), agregador.agregar(List.of()));
    }

    private void assertFailure(String cost, String current, String unrealized, Indicador indicador) {
        FalhaAgregacaoException exception = assertThrows(
                FalhaAgregacaoException.class,
                () -> agregador.agregar(List.of(posicao(
                        1L, Mercado.BRASIL, Moeda.BRL, cost, current, unrealized
                )))
        );
        assertEquals(indicador, exception.indicador());
        assertEquals(Moeda.BRL, exception.moeda());
    }

    private PosicaoResponse posicao(
            Long id,
            Mercado mercado,
            Moeda moeda,
            String custo,
            String atual,
            String resultado
    ) {
        return new PosicaoResponse(
                id, "T" + id, "Empresa " + id, mercado, moeda,
                new BigDecimal("1.000000"), new BigDecimal("10.000000000000"),
                new BigDecimal(custo), new BigDecimal("20.000000"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z"), new BigDecimal(atual),
                new BigDecimal(resultado), new BigDecimal("10.000000")
        );
    }
}
