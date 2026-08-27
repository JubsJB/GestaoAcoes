package com.projeto.dto;

import com.projeto.entities.Moeda;

import java.math.BigDecimal;

public record ResumoMoedaResponse(
        Moeda moeda,
        BigDecimal custoTotalPosicoes,
        BigDecimal patrimonioAtual,
        BigDecimal resultadoNaoRealizadoTotal
) {
}
