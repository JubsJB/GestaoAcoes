package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(requiredProperties = {"operacaoId", "dataOperacao", "ordemNoDia", "saldoAposOperacao"})
public record PrimeiroDeficitResponse(Long operacaoId, LocalDate dataOperacao, Integer ordemNoDia, BigDecimal saldoAposOperacao) {
}
