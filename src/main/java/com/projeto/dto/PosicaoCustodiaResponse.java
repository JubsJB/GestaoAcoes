package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(requiredProperties={"ordemFinanceiraAncora","numerador","denominador"},
        description="Posição de custódia; componentes racionais textuais exatos, nunca números de ponto flutuante")
public record PosicaoCustodiaResponse(@Schema(minimum="0") Integer ordemFinanceiraAncora,
        @Schema(type="string",maxLength=512) String numerador,
        @Schema(type="string",maxLength=512) String denominador) { }
