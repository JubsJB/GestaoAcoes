package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(requiredProperties={"movimentacao","estorno"})
public record DetalheCustodiaResponse(RegistroCustodiaResponse movimentacao,
        @Schema(types={"object","null"},schemaResolution=Schema.SchemaResolution.INLINE) RegistroCustodiaResponse estorno) { }
