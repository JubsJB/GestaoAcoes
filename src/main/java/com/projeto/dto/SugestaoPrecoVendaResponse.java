package com.projeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Sugestão editável baseada na última COMPRA cronologicamente aplicável; não é preço médio nem recomendação financeira")
public record SugestaoPrecoVendaResponse(
        @Schema(description = "Preço da última COMPRA aplicável, ou null quando não existir", example = "42.300000", nullable = true)
        BigDecimal precoUnitarioSugerido
) {
}
