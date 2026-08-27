package com.projeto.dto;

import com.projeto.entities.Moeda;

import java.math.BigDecimal;

public record PatrimonioMoedaResponse(
        Moeda moeda,
        BigDecimal patrimonioAtual
) {
}
