package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraPosicaoTest {

    private final CalculadoraPosicao calculadora = new CalculadoraPosicao();
    private Carteira carteira;
    private Acao brasileira;
    private Acao americana;
    private long nextOperationId;

    @BeforeEach
    void setUp() {
        carteira = portfolio(1L, "Carteira");
        brasileira = action(2L, "PETR4", Mercado.BRASIL, Moeda.BRL);
        americana = action(3L, "AAPL", Mercado.EUA, Moeda.USD);
        nextOperationId = 100L;
    }

    @Test
    void calculatesSingleAndRepeatedPurchasesAtTheSamePrice() {
        CalculadoraPosicao.ResultadoReplay single = calculadora.reproduzir(List.of(
                operation(brasileira, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1)
        ));
        assertPosition(single, "100.000000", "10.000000000000", "1000.000000000000");

        CalculadoraPosicao.ResultadoReplay repeated = calculadora.reproduzir(List.of(
                operation(brasileira, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                operation(brasileira, TipoOperacao.COMPRA, "50", "10", "2026-08-02", 1)
        ));
        assertPosition(repeated, "150.000000", "10.000000000000", "1500.000000000000");
    }

    @Test
    void calculatesExactAndPeriodicWeightedAveragesWithApprovedRounding() {
        CalculadoraPosicao.ResultadoReplay exact = calculadora.reproduzir(List.of(
                operation(brasileira, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                operation(brasileira, TipoOperacao.COMPRA, "100", "20", "2026-08-02", 1)
        ));
        assertPosition(exact, "200.000000", "15.000000000000", "3000.000000000000");

        CalculadoraPosicao.ResultadoReplay periodic = calculadora.reproduzir(List.of(
                operation(brasileira, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                operation(brasileira, TipoOperacao.COMPRA, "50", "11", "2026-08-02", 1)
        ));
        assertPosition(periodic, "150.000000", "10.333333333333", "1550.000000000000");
        assertEquals(24, CalculadoraPosicao.ESCALA_INTERMEDIARIA);
        assertEquals(RoundingMode.HALF_EVEN, CalculadoraPosicao.ARREDONDAMENTO);
    }

    @Test
    void partialSalePreservesAverageAndReducesCostRegardlessOfSalePrice() {
        for (String salePrice : List.of("5", "10", "25")) {
            CalculadoraPosicao.ResultadoReplay result = calculadora.reproduzir(List.of(
                    operation(brasileira, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                    operation(brasileira, TipoOperacao.VENDA, "40", salePrice, "2026-08-02", 1)
            ));

            assertPosition(result, "60.000000", "10.000000000000", "600.000000000000");
        }
    }

    @Test
    void totalSaleZerosStateAndLaterPurchaseStartsIndependentCycle() {
        CalculadoraPosicao.ResultadoReplay closed = calculadora.reproduzir(List.of(
                operation(brasileira, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                operation(brasileira, TipoOperacao.VENDA, "100", "12", "2026-08-02", 1)
        ));
        assertPosition(closed, "0.000000", "0.000000000000", "0.000000000000");

        CalculadoraPosicao.ResultadoReplay newCycle = calculadora.reproduzir(List.of(
                operation(brasileira, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                operation(brasileira, TipoOperacao.VENDA, "100", "12", "2026-08-02", 1),
                operation(brasileira, TipoOperacao.COMPRA, "50", "20", "2026-08-03", 1)
        ));
        assertPosition(newCycle, "50.000000", "20.000000000000", "1000.000000000000");
    }

    @Test
    void purchaseAfterPartialSaleUsesRemainingCostForNewWeightedAverage() {
        CalculadoraPosicao.ResultadoReplay result = calculadora.reproduzir(List.of(
                operation(brasileira, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1),
                operation(brasileira, TipoOperacao.VENDA, "40", "15", "2026-08-02", 1),
                operation(brasileira, TipoOperacao.COMPRA, "40", "20", "2026-08-03", 1)
        ));

        assertPosition(result, "100.000000", "14.000000000000", "1400.000000000000");
    }

    @Test
    void preservesBrazilianIntegerAndAmericanFractionalQuantityRules() {
        CalculadoraPosicao.ResultadoReplay brazil = calculadora.reproduzir(List.of(
                operation(brasileira, TipoOperacao.COMPRA, "10", "1", "2026-08-01", 1)
        ));
        assertPosition(brazil, "10.000000", "1.000000000000", "10.000000000000");

        CalculadoraPosicao.ResultadoReplay usa = calculadora.reproduzir(List.of(
                operation(americana, TipoOperacao.COMPRA, "0.123456", "20", "2026-08-01", 1),
                operation(americana, TipoOperacao.VENDA, "0.023456", "30", "2026-08-02", 1)
        ));
        assertPosition(usa, "0.100000", "20.000000000000", "2.000000000000");

        CalculadoraPosicao.ResultadoReplay invalidBrazil = calculadora.reproduzir(List.of(
                operation(brasileira, TipoOperacao.COMPRA, "0.500000", "1", "2026-08-01", 1)
        ));
        assertFalse(invalidBrazil.valido());
        assertEquals(
                CalculadoraPosicao.TipoFalha.HISTORICO_INCONSISTENTE,
                invalidBrazil.falha().tipo()
        );
    }

    @Test
    void usesDateAndDailyOrderAndNeverIdAsFinancialOrder() {
        Operacao first = operation(
                brasileira, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1
        );
        Operacao second = operation(
                brasileira, TipoOperacao.COMPRA, "25", "20", "2026-08-01", 2
        );
        ReflectionTestUtils.setField(first, "id", 999L);
        ReflectionTestUtils.setField(second, "id", 1L);

        assertPosition(
                calculadora.reproduzir(List.of(first, second)),
                "125.000000",
                "12.000000000000",
                "1500.000000000000"
        );

        CalculadoraPosicao.ResultadoReplay unordered = calculadora.reproduzir(List.of(second, first));
        assertFalse(unordered.valido());
        assertTrue(unordered.falha().motivo().contains("dataOperacao"));
    }

    @Test
    void rejectsNegativeChronologicalBalanceWithoutMutatingOperations() {
        Operacao sale = operation(
                brasileira, TipoOperacao.VENDA, "11", "20", "2026-08-02", 1
        );
        BigDecimal originalQuantity = sale.getQuantidade();
        CalculadoraPosicao.ResultadoReplay result = calculadora.reproduzir(List.of(
                operation(brasileira, TipoOperacao.COMPRA, "10", "10", "2026-08-01", 1),
                sale
        ));

        assertFalse(result.valido());
        assertEquals(
                CalculadoraPosicao.TipoFalha.HISTORICO_INCONSISTENTE,
                result.falha().tipo()
        );
        assertEquals(new BigDecimal("10.000000"), result.falha().quantidadeDisponivel());
        assertEquals(new BigDecimal("11.000000"), result.falha().quantidadeSolicitada());
        assertEquals(originalQuantity, sale.getQuantidade());
    }

    @Test
    void rejectsOutputThatExceedsApprovedCostPrecision() {
        String maximumOperand = "9999999999999.999999";
        CalculadoraPosicao.ResultadoReplay result = calculadora.reproduzir(List.of(
                operation(americana, TipoOperacao.COMPRA, maximumOperand, maximumOperand, "2026-08-01", 1),
                operation(americana, TipoOperacao.COMPRA, maximumOperand, maximumOperand, "2026-08-02", 1)
        ));

        assertFalse(result.valido());
        assertEquals(
                CalculadoraPosicao.TipoFalha.CALCULO_FORA_DA_PRECISAO,
                result.falha().tipo()
        );
    }

    @Test
    void quantitativeValidationReusesBalanceRuleWithoutApplyingOutputPrecision() {
        String maximumOperand = "9999999999999.999999";
        CalculadoraPosicao.ResultadoReplay result = calculadora.validarQuantidade(List.of(
                operation(americana, TipoOperacao.COMPRA, maximumOperand, maximumOperand, "2026-08-01", 1),
                operation(americana, TipoOperacao.COMPRA, maximumOperand, maximumOperand, "2026-08-02", 1)
        ));

        assertTrue(result.valido());
        assertEquals(new BigDecimal("19999999999999.999998"), result.posicao().quantidadeAtual());
    }

    @Test
    void calculatesExactCurrentValueForIntegerAndFractionalQuantities() {
        assertEquals(
                new BigDecimal("3550.000000000000"),
                calculadora.calcularValorAtual(
                        new BigDecimal("100.000000"),
                        new BigDecimal("35.500000")
                )
        );
        assertEquals(
                new BigDecimal("112.205000000000"),
                calculadora.calcularValorAtual(
                        new BigDecimal("0.500000"),
                        new BigDecimal("224.410000")
                )
        );
        assertEquals(12, calculadora.calcularValorAtual(BigDecimal.ONE, BigDecimal.ONE).scale());
        assertEquals(38, CalculadoraPosicao.PRECISAO_VALOR_ATUAL);
    }

    @Test
    void rejectsCurrentValueOutsideApprovedPrecisionWithoutRounding() {
        assertThrows(ArithmeticException.class, () -> calculadora.calcularValorAtual(
                new BigDecimal("99999999999999.999999"),
                new BigDecimal("9999999999999.999999")
        ));
    }

    @Test
    void calculatesPositiveNegativeAndZeroUnrealizedResultsAtScaleTwelve() {
        assertEquals(
                new BigDecimal("350.000000000000"),
                calculadora.calcularResultadoNaoRealizado(
                        new BigDecimal("3550.000000000000"),
                        new BigDecimal("3200.000000000000")
                )
        );
        assertEquals(
                new BigDecimal("-200.000000000000"),
                calculadora.calcularResultadoNaoRealizado(
                        new BigDecimal("3000.000000000000"),
                        new BigDecimal("3200.000000000000")
                )
        );
        BigDecimal zero = calculadora.calcularResultadoNaoRealizado(
                new BigDecimal("3200.000000000000"),
                new BigDecimal("3200.000000000000")
        );
        assertEquals(new BigDecimal("0.000000000000"), zero);
        assertEquals(12, zero.scale());
        assertEquals(38, CalculadoraPosicao.PRECISAO_RESULTADO_NAO_REALIZADO);
    }

    @Test
    void calculatesUnrealizedResultOnlyFromCurrentValueAndConsolidatedCost() {
        BigDecimal result = calculadora.calcularResultadoNaoRealizado(
                new BigDecimal("900.000000000000"),
                new BigDecimal("600.000000000000")
        );

        assertEquals(new BigDecimal("300.000000000000"), result);
    }

    @Test
    void rejectsUnrealizedResultThatRequiresRoundingOrExceedsPrecision() {
        assertThrows(ArithmeticException.class, () -> calculadora.calcularResultadoNaoRealizado(
                new BigDecimal("1.0000000000001"),
                BigDecimal.ZERO
        ));
        assertThrows(ArithmeticException.class, () -> calculadora.calcularResultadoNaoRealizado(
                new BigDecimal("999999999999999999999999999.000000000000"),
                new BigDecimal("-1.000000000000")
        ));
        assertThrows(ArithmeticException.class, () ->
                calculadora.calcularResultadoNaoRealizado(null, BigDecimal.ZERO));
    }

    @Test
    void calculatesPositiveNegativeAndZeroProfitabilityAsPercentageAtScaleSix() {
        assertEquals(
                new BigDecimal("10.937500"),
                calculadora.calcularRentabilidadePercentual(
                        new BigDecimal("350.000000000000"),
                        new BigDecimal("3200.000000000000")
                )
        );
        assertEquals(
                new BigDecimal("-6.250000"),
                calculadora.calcularRentabilidadePercentual(
                        new BigDecimal("-200.000000000000"),
                        new BigDecimal("3200.000000000000")
                )
        );
        BigDecimal zero = calculadora.calcularRentabilidadePercentual(
                BigDecimal.ZERO,
                new BigDecimal("3200.000000000000")
        );
        assertEquals(new BigDecimal("0.000000"), zero);
        assertEquals(6, zero.scale());
        assertEquals(6, CalculadoraPosicao.ESCALA_RENTABILIDADE_PERCENTUAL);
        assertEquals(38, CalculadoraPosicao.PRECISAO_RENTABILIDADE_PERCENTUAL);
    }

    @Test
    void usesScaleTwentyFourHalfEvenBeforeMultiplyingByOneHundred() {
        assertEquals(
                new BigDecimal("50.000000"),
                calculadora.calcularRentabilidadePercentual(
                        new BigDecimal("300.000000000000"),
                        new BigDecimal("600.000000000000")
                )
        );
        assertEquals(
                new BigDecimal("33.333333"),
                calculadora.calcularRentabilidadePercentual(BigDecimal.ONE, new BigDecimal("3"))
        );
        assertEquals(RoundingMode.HALF_EVEN, CalculadoraPosicao.ARREDONDAMENTO);
        assertEquals(24, CalculadoraPosicao.ESCALA_INTERMEDIARIA);
    }

    @Test
    void acceptsProfitabilityAboveOneHundredAndRejectsInvalidCostOrPrecisionOverflow() {
        assertEquals(
                new BigDecimal("250.000000"),
                calculadora.calcularRentabilidadePercentual(new BigDecimal("250"), new BigDecimal("100"))
        );
        assertThrows(ArithmeticException.class, () ->
                calculadora.calcularRentabilidadePercentual(BigDecimal.ONE, BigDecimal.ZERO));
        assertThrows(ArithmeticException.class, () ->
                calculadora.calcularRentabilidadePercentual(BigDecimal.ONE, new BigDecimal("-1")));
        assertThrows(ArithmeticException.class, () ->
                calculadora.calcularRentabilidadePercentual(
                        new BigDecimal("99999999999999999999999999.000000000000"),
                        new BigDecimal("0.000000000001")
                ));
    }

    private void assertPosition(
            CalculadoraPosicao.ResultadoReplay result,
            String quantity,
            String average,
            String cost
    ) {
        assertTrue(result.valido(), () -> result.falha() == null ? "" : result.falha().motivo());
        assertEquals(new BigDecimal(quantity), result.posicao().quantidadeAtual());
        assertEquals(new BigDecimal(average), result.posicao().precoMedio());
        assertEquals(new BigDecimal(cost), result.posicao().custoPosicao());
    }

    private Operacao operation(
            Acao action,
            TipoOperacao type,
            String quantity,
            String price,
            String date,
            int order
    ) {
        BigDecimal normalizedQuantity = new BigDecimal(quantity).setScale(6);
        BigDecimal normalizedPrice = new BigDecimal(price).setScale(6);
        Operacao operation = new Operacao(
                carteira,
                action,
                null,
                type,
                normalizedQuantity,
                normalizedPrice,
                LocalDate.parse(date),
                order,
                normalizedQuantity.multiply(normalizedPrice).setScale(12)
        );
        ReflectionTestUtils.setField(operation, "id", nextOperationId++);
        return operation;
    }

    private Carteira portfolio(Long id, String name) {
        Carteira value = new Carteira(name, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private Acao action(Long id, String ticker, Mercado market, Moeda currency) {
        Acao value = new Acao(
                ticker,
                "Empresa " + ticker,
                market,
                currency,
                new BigDecimal("999.999999"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
