package com.projeto.services;

import com.projeto.entities.Mercado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Clock;
import java.math.RoundingMode;
import com.projeto.validation.TickerNormalizer;

/** A origem sem corretora, a posição e a auditoria são determinadas pelo servidor. */
public record AtribuicaoCustodiaCommand(Long carteiraId, String ticker, Mercado mercado,
        Long corretoraDestinoId, BigDecimal quantidade, LocalDate dataEfetiva) {
    AtribuicaoCustodiaCommand validada(TickerNormalizer tickers, Clock clock) {
        String normalizado = ValidacaoEntradaCustodia.validar(carteiraId, ticker, mercado,
                corretoraDestinoId, quantidade, dataEfetiva, tickers, clock);
        return new AtribuicaoCustodiaCommand(carteiraId, normalizado, mercado, corretoraDestinoId,
                quantidade.setScale(6, RoundingMode.UNNECESSARY), dataEfetiva);
    }
}
