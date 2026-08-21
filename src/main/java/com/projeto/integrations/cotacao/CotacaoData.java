package com.projeto.integrations.cotacao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CotacaoData(
        String ticker,
        String nomeEmpresa,
        String moeda,
        BigDecimal cotacao,
        OffsetDateTime dataHoraCotacao,
        boolean tickerAlteradoExplicitamente
) {
}
