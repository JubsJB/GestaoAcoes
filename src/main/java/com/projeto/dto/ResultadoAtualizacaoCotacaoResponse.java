package com.projeto.dto;

import com.projeto.entities.Mercado;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado público de uma Ação do lote. FALHA exige erro individual; as outras classificações retornam erro nulo.")
public record ResultadoAtualizacaoCotacaoResponse(
        @Schema(description = "Identificador da Ação", requiredMode = Schema.RequiredMode.REQUIRED) Long acaoId,
        @Schema(description = "Ticker persistido da Ação", requiredMode = Schema.RequiredMode.REQUIRED) String ticker,
        @Schema(description = "Mercado da Ação e moeda do preço: BRASIL/BRL ou EUA/USD", requiredMode = Schema.RequiredMode.REQUIRED) Mercado mercado,
        @Schema(description = "ATUALIZADA: observação aceita e confirmada nesta execução; SEM_NOVA_COTACAO: sem substituição de preço/referência (nova obtenção da mesma observação pode avançar somente cotacaoObtidaEm); FALHA: falha isolada desta Ação", requiredMode = Schema.RequiredMode.REQUIRED) Resultado resultado,
        @Schema(description = "Motivo público: REFERENCIA_INICIAL_ESTABELECIDA ou REFERENCIA_POSTERIOR em ATUALIZADA; MESMA_OBSERVACAO, REFERENCIA_ANTERIOR, REUSO ou COOLDOWN_SUCESSO em SEM_NOVA_COTACAO; código público da falha ou COOLDOWN_FALHA em FALHA. Códigos incluem CONFLITO_NA_MESMA_REFERENCIA, TIPO_REFERENCIA_INCOMPATIVEL, REFERENCIA_TEMPORAL_INVALIDA, erros externos e de aplicação. Não é uma enumeração fechada de códigos de erro", requiredMode = Schema.RequiredMode.REQUIRED) String motivo,
        @Schema(description = "Snapshot da cotação correspondente ao resultado", requiredMode = Schema.RequiredMode.REQUIRED) EstadoCotacaoResponse cotacao,
        @Schema(description = "Erro desta Ação somente em FALHA; nulo nos demais resultados", nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) ErroAtualizacaoCotacaoResponse erro) {
    public enum Resultado { ATUALIZADA, SEM_NOVA_COTACAO, FALHA }
}
