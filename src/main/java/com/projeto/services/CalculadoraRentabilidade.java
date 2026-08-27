package com.projeto.services;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CalculadoraRentabilidade {

    public static final int ESCALA_INTERMEDIARIA = 24;
    public static final int ESCALA_SAIDA = 6;
    public static final int PRECISAO_MAXIMA = 38;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    private static final BigDecimal CEM = new BigDecimal("100");

    public BigDecimal calcularPercentual(BigDecimal resultado, BigDecimal custo) {
        if (resultado == null || custo == null || custo.signum() <= 0) {
            throw new ArithmeticException("Resultado e custo positivo são obrigatórios");
        }

        BigDecimal razao = resultado.divide(custo, ESCALA_INTERMEDIARIA, ARREDONDAMENTO);
        BigDecimal rentabilidade = razao.multiply(CEM).setScale(ESCALA_SAIDA, ARREDONDAMENTO);
        if (rentabilidade.precision() > PRECISAO_MAXIMA) {
            throw new ArithmeticException("Rentabilidade percentual excede a precisão aprovada");
        }
        return rentabilidade;
    }
}
