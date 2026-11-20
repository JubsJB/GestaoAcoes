package com.projeto.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/** Snapshot da observação retornada, sem garantia de cotação em tempo real. */
@Schema(description = "Snapshot da cotação retornada ao concluir o item; não garante tempo real nem snapshot global do lote. Em COOLDOWN_SUCESSO representa a tentativa concluída, podendo diferir do estado persistido mais recente após atualização por outra instância.")
public record EstadoCotacaoResponse(
        @Schema(description = "Preço na moeda do mercado, decimal exato NUMERIC(19,6), sem arredondamento", example = "32.123456", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal cotacaoAtual,
        @Schema(description = "Referência real de mercado, incluindo DESCONHECIDA no legado", requiredMode = Schema.RequiredMode.REQUIRED)
        ReferenciaMercadoCotacaoResponse referenciaMercadoCotacao,
        @Schema(description = "Instante UTC de obtenção válida da observação do provider, quando conhecido; nulo no legado. Não é referência de mercado nem possui monotonicidade global entre observações", type = "string", format = "date-time", nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime cotacaoObtidaEm,
        @Schema(description = "Campo legado de compatibilidade: instante de mercado para INSTANTE e obtenção para DATA nas observações aceitas; avanço isolado de obtenção preserva o legado. Não decide ordenação, identidade, reuso ou cooldown", type = "string", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime dataHoraCotacao) { }
