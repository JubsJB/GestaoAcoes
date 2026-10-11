package com.projeto.integrations.cotacao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;

/** Resposta interna do adapter; campos incompletos ainda exigem validação da aplicação. */
public record CotacaoData(
        String ticker,
        String nomeEmpresa,
        String moeda,
        BigDecimal cotacao,
        OffsetDateTime dataHoraCotacao,
        boolean tickerAlteradoExplicitamente,
        LocalDate referenciaData,
        boolean referenciaInvalida
) {
    public CotacaoData(String ticker, String nomeEmpresa, String moeda, BigDecimal cotacao,
                      OffsetDateTime dataHoraCotacao, boolean tickerAlteradoExplicitamente) {
        this(ticker, nomeEmpresa, moeda, cotacao, dataHoraCotacao,
                tickerAlteradoExplicitamente, null, false);
    }
}
