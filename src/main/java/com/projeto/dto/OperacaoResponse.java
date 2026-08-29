package com.projeto.dto;

import com.projeto.entities.Mercado;
import com.projeto.entities.TipoOperacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OperacaoResponse(
        Long id,
        Long carteiraId,
        String ticker,
        Mercado mercado,
        Long corretoraId,
        @Schema(description = "Tipo da operação", allowableValues = {"COMPRA", "VENDA"})
        TipoOperacao tipo,
        @Schema(description = "Quantidade negociada", example = "100.000000")
        BigDecimal quantidade,
        @Schema(description = "Preço unitário registrado", example = "32.150000")
        BigDecimal precoUnitario,
        @Schema(description = "Data efetiva da operação", format = "date")
        LocalDate dataOperacao,
        Integer ordemNoDia,
        @Schema(description = "Quantidade multiplicada pelo preço unitário", example = "3215.000000000000")
        BigDecimal valorTotal
) {
}
