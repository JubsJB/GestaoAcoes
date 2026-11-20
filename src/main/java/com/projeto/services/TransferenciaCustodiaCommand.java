package com.projeto.services;

import com.projeto.entities.Mercado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Clock;
import java.math.RoundingMode;
import com.projeto.services.exceptions.ApiException;
import com.projeto.validation.TickerNormalizer;

/** Entrada interna: posição e auditoria pertencem exclusivamente ao servidor. */
public record TransferenciaCustodiaCommand(Long carteiraId, String ticker, Mercado mercado,
        Long corretoraOrigemId, Long corretoraDestinoId, BigDecimal quantidade, LocalDate dataEfetiva) {
    TransferenciaCustodiaCommand validada(TickerNormalizer tickers,Clock clock) {
        id(carteiraId,"carteiraId");id(corretoraOrigemId,"corretoraOrigemId");id(corretoraDestinoId,"corretoraDestinoId");
        if(corretoraOrigemId.equals(corretoraDestinoId))throw invalida("corretoraDestinoId","Origem e destino devem ser distintos");
        String normalizado = ValidacaoEntradaCustodia.validar(carteiraId, ticker, mercado,
                corretoraDestinoId, quantidade, dataEfetiva, tickers, clock);
        return new TransferenciaCustodiaCommand(carteiraId,normalizado,mercado,corretoraOrigemId,corretoraDestinoId,
                quantidade.setScale(6,RoundingMode.UNNECESSARY),dataEfetiva);
    }
    private static void id(Long valor,String campo) { ValidacaoEntradaCustodia.id(valor, campo); }
    static ApiException invalida(String campo,String mensagem) {
        return ValidacaoEntradaCustodia.invalida(campo, mensagem);
    }
}
