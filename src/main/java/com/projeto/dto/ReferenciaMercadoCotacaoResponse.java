package com.projeto.dto;

import com.projeto.entities.ReferenciaTipoCotacao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Referência de mercado; DATA não representa um horário artificial. */
@Schema(description = "Referência de mercado tipada: DATA preenche somente data; INSTANTE somente instante; DESCONHECIDA mantém ambos nulos. Não fabrica referência para legado.")
public record ReferenciaMercadoCotacaoResponse(
        @Schema(description = "Precisão conhecida da referência de mercado", requiredMode = Schema.RequiredMode.REQUIRED)
        ReferenciaTipoCotacao tipo,
        @Schema(description = "Instante de mercado UTC, somente para INSTANTE; nulo para DATA e DESCONHECIDA", type = "string", format = "date-time", nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime instante,
        @Schema(description = "Data civil de mercado, somente para DATA, sem hora ou offset; nula para INSTANTE e DESCONHECIDA", type = "string", format = "date", nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate data) { }
