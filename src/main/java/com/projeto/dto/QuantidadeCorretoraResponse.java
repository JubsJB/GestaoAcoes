package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(requiredProperties = {"corretoraId", "nome", "quantidadeLiquida"})
public record QuantidadeCorretoraResponse(@Schema(nullable = true) Long corretoraId, String nome, BigDecimal quantidadeLiquida) {
}
