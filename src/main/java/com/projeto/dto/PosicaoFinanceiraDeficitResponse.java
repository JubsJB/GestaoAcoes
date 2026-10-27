package com.projeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties={"ordemNoDia"})
public record PosicaoFinanceiraDeficitResponse(@Schema(minimum="1") Integer ordemNoDia) { }
