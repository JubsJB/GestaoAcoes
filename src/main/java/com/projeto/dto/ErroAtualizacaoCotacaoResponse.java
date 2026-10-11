package com.projeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Somente código e mensagem públicos; nenhuma representação de exceção. */
@Schema(description = "Erro isolado da Ação indicada pelo item, somente código e mensagem públicos seguros; não define o status HTTP global do lote.")
public record ErroAtualizacaoCotacaoResponse(
        @Schema(description = "Código público técnico da falha desta Ação", requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(description = "Mensagem pública segura da falha desta Ação", requiredMode = Schema.RequiredMode.REQUIRED) String message) { }
