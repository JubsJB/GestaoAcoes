package com.projeto.services;

import com.projeto.entities.MovimentacaoCustodia;
import com.projeto.validation.TickerNormalizer;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class TransferenciaCustodiaService {
    private final TransferenciaCustodiaPersistenceService persistence;
    private final TickerNormalizer tickers;
    private final Clock clock;
    public TransferenciaCustodiaService(TransferenciaCustodiaPersistenceService persistence,TickerNormalizer tickers,Clock clock) {
        this.persistence=persistence;this.tickers=tickers;this.clock=clock;
    }
    public MovimentacaoCustodia transferir(TransferenciaCustodiaCommand command) {
        if (command == null) {
            throw TransferenciaCustodiaCommand.invalida("request", "Transferência obrigatória");
        }
        var entrada = command.validada(tickers,clock);
        try { return persistence.persistir(entrada); }
        catch (RuntimeException falha) { throw FalhasEscritaOperacao.traduzir(falha); }
    }
}
