package com.projeto.dto;

import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Referência de fechamento histórico exato e informativa, sem efeitos colaterais, que pode servir como sugestão inicial editável para COMPRA. Não vinculante: o preço final é informado pelo cliente. POST /operacoes é independente da prévia, não exige seu sucesso e não consulta provider histórico.")
public record PreviaPrecoCompraResponse(
        @Schema(example = "PETR4") String ticker,
        @Schema(example = "BRASIL") Mercado mercado,
        @Schema(example = "BRL") Moeda moeda,
        @Schema(example = "2026-08-20") LocalDate dataCotacao,
        @Schema(example = "42.300000") BigDecimal precoUnitario
) {
}
