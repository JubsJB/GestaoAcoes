package com.projeto.dto;

import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PosicaoResponse(
        Long acaoId,
        String ticker,
        String nomeEmpresa,
        Mercado mercado,
        Moeda moeda,
        BigDecimal quantidadeAtual,
        BigDecimal precoMedio,
        BigDecimal custoPosicao,
        BigDecimal cotacaoAtual,
        OffsetDateTime dataHoraCotacao,
        BigDecimal valorAtualPosicao,
        BigDecimal resultadoNaoRealizado
) {
}
