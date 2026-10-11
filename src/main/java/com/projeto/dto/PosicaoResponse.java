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
        @Schema(description = "Última cotação persistida da Ação, decimal exato NUMERIC(19,6); não garante tempo real", example = "32.123456")
        BigDecimal cotacaoAtual,
        @Schema(description = "Campo legado de compatibilidade; a referência real está em referenciaMercadoCotacao", format = "date-time")
        OffsetDateTime dataHoraCotacao,
        BigDecimal valorAtualPosicao,
        BigDecimal resultadoNaoRealizado,
        @Schema(description = "Rentabilidade percentual atual da posição")
        BigDecimal rentabilidadePercentual,
        @Schema(description = "Referência de mercado tipada {tipo, instante, data}; DESCONHECIDA mantém instante e data nulos", requiredMode = Schema.RequiredMode.REQUIRED)
        ReferenciaMercadoCotacaoResponse referenciaMercadoCotacao,
        @Schema(description = "Instante UTC de obtenção válida da observação do provider, quando conhecido; nulo no legado. Não é referência de mercado nem possui monotonicidade global entre observações", type = "string", format = "date-time", nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime cotacaoObtidaEm
) {
    /** Compatibilidade de construção de posições sobre estados legados. */
    public PosicaoResponse(Long acaoId, String ticker, String nomeEmpresa, Mercado mercado, Moeda moeda,
            BigDecimal quantidadeAtual, BigDecimal precoMedio, BigDecimal custoPosicao,
            BigDecimal cotacaoAtual, OffsetDateTime dataHoraCotacao, BigDecimal valorAtualPosicao,
            BigDecimal resultadoNaoRealizado, BigDecimal rentabilidadePercentual) {
        this(acaoId, ticker, nomeEmpresa, mercado, moeda, quantidadeAtual, precoMedio, custoPosicao,
                cotacaoAtual, dataHoraCotacao, valorAtualPosicao, resultadoNaoRealizado, rentabilidadePercentual,
                new ReferenciaMercadoCotacaoResponse(com.projeto.entities.ReferenciaTipoCotacao.DESCONHECIDA,
                        null, null), null);
    }
}
