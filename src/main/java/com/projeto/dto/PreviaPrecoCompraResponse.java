package com.projeto.dto;

import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Fechamento histórico exato e informativo para uma COMPRA; o POST consulta novamente o provider")
public record PreviaPrecoCompraResponse(
        @Schema(example = "PETR4") String ticker,
        @Schema(example = "BRASIL") Mercado mercado,
        @Schema(example = "BRL") Moeda moeda,
        @Schema(example = "2026-08-20") LocalDate dataCotacao,
        @Schema(example = "42.300000") BigDecimal precoUnitario
) {
}
