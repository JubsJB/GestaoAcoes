package com.projeto.dto;

import com.projeto.entities.Moeda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record PatrimonioMoedaResponse(
        @Schema(description = "Moeda agregada sem conversão", allowableValues = {"BRL", "USD"})
        Moeda moeda,
        @Schema(description = "Patrimônio atual na moeda indicada")
        BigDecimal patrimonioAtual
) {
}
