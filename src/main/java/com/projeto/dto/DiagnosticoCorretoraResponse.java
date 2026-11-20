package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(requiredProperties = {"corretoraId", "nome", "quantidadeFinal", "saldoNegativoAtual", "possuiDeficitHistorico", "primeiroDeficit"})
public record DiagnosticoCorretoraResponse(@Schema(nullable = true) Long corretoraId, String nome, BigDecimal quantidadeFinal,
        boolean saldoNegativoAtual, boolean possuiDeficitHistorico,
        @Schema(nullable = true) PrimeiroDeficitResponse primeiroDeficit) {
}
