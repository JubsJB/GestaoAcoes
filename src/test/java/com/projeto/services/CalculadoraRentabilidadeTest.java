package com.projeto.services;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraRentabilidadeTest {

    private final CalculadoraRentabilidade calculadora = new CalculadoraRentabilidade();

    @Test
    void calculatesGainLossZeroAndGainAboveOneHundred() {
        assertEquals(new BigDecimal("10.937500"),
                calculadora.calcularPercentual(new BigDecimal("350"), new BigDecimal("3200")));
        assertEquals(new BigDecimal("-15.000000"),
                calculadora.calcularPercentual(new BigDecimal("-1500"), new BigDecimal("10000")));
        assertEquals(new BigDecimal("0.000000"),
                calculadora.calcularPercentual(BigDecimal.ZERO, new BigDecimal("3200")));
        assertEquals(new BigDecimal("150.000000"),
                calculadora.calcularPercentual(new BigDecimal("1500"), new BigDecimal("1000")));
    }

    @Test
    void centralizesApprovedDivisionAndNormalizationPolicy() {
        assertEquals(new BigDecimal("10.000000"),
                calculadora.calcularPercentual(new BigDecimal("400"), new BigDecimal("4000")));
        assertEquals(new BigDecimal("13.333333"),
                calculadora.calcularPercentual(new BigDecimal("400"), new BigDecimal("3000")));
        assertEquals(24, CalculadoraRentabilidade.ESCALA_INTERMEDIARIA);
        assertEquals(6, CalculadoraRentabilidade.ESCALA_SAIDA);
        assertEquals(38, CalculadoraRentabilidade.PRECISAO_MAXIMA);
        assertEquals(RoundingMode.HALF_EVEN, CalculadoraRentabilidade.ARREDONDAMENTO);
    }

    @Test
    void rejectsMissingOrNonPositiveCostAndPrecisionOverflow() {
        assertThrows(ArithmeticException.class,
                () -> calculadora.calcularPercentual(null, BigDecimal.ONE));
        assertThrows(ArithmeticException.class,
                () -> calculadora.calcularPercentual(BigDecimal.ONE, null));
        assertThrows(ArithmeticException.class,
                () -> calculadora.calcularPercentual(BigDecimal.ONE, BigDecimal.ZERO));
        assertThrows(ArithmeticException.class,
                () -> calculadora.calcularPercentual(BigDecimal.ONE, BigDecimal.ONE.negate()));
        assertThrows(ArithmeticException.class, () -> calculadora.calcularPercentual(
                new BigDecimal("99999999999999999999999999.000000000000"),
                new BigDecimal("0.000000000001")));
    }

    @Test
    void remainsPureAndInfrastructureFree() {
        assertTrue(CalculadoraRentabilidade.class.isAnnotationPresent(Component.class));
        assertTrue(Arrays.stream(CalculadoraRentabilidade.class.getDeclaredFields())
                .allMatch(field -> Modifier.isStatic(field.getModifiers())
                        && Modifier.isFinal(field.getModifiers())));
        assertTrue(Arrays.stream(CalculadoraRentabilidade.class.getDeclaredMethods())
                .noneMatch(method -> method.isAnnotationPresent(
                        org.springframework.transaction.annotation.Transactional.class)));
    }
}
