package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(requiredProperties = {"codigo"})
public record DiagnosticoReconciliacaoResponse(@Schema(allowableValues = "DIVERGENCIA_QUANTIDADES") String codigo) {
}
