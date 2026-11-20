package com.projeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contagem por classificação; a soma dos três contadores é o número de resultados, inclusive no sucesso parcial.")
public record ResumoAtualizacaoCotacoesResponse(
        @Schema(description = "Quantidade de itens ATUALIZADA confirmados nesta execução", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        long atualizadas,
        @Schema(description = "Quantidade de itens SEM_NOVA_COTACAO, inclusive reuso e cooldown de sucesso", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        long semNovaCotacao,
        @Schema(description = "Quantidade de itens FALHA isolada, inclusive cooldown de falha", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        long falhas) { }
