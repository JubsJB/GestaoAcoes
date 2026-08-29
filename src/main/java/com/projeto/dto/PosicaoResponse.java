package com.projeto.dto;

import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PosicaoResponse(
        Long acaoId,
        String ticker,
        String nomeEmpresa,
        Mercado mercado,
        @Schema(description = "Moeda independente da posição", allowableValues = {"BRL", "USD"})
        Moeda moeda,
        @Schema(description = "Quantidade atualmente mantida")
        BigDecimal quantidadeAtual,
        @Schema(description = "Preço médio ponderado da posição")
        BigDecimal precoMedio,
        BigDecimal custoPosicao,
        BigDecimal cotacaoAtual,
        @Schema(format = "date-time")
        OffsetDateTime dataHoraCotacao,
        BigDecimal valorAtualPosicao,
        BigDecimal resultadoNaoRealizado,
        @Schema(description = "Rentabilidade percentual atual da posição")
        BigDecimal rentabilidadePercentual
) {
}
