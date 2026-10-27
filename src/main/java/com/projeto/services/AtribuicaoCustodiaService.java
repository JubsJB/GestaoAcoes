package com.projeto.services;

import com.projeto.entities.MovimentacaoCustodia;
import com.projeto.validation.TickerNormalizer;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class AtribuicaoCustodiaService {
    private final TransferenciaCustodiaPersistenceService persistence;
    private final TickerNormalizer tickers;
    private final Clock clock;

    public AtribuicaoCustodiaService(TransferenciaCustodiaPersistenceService persistence,
            TickerNormalizer tickers, Clock clock) {
        this.persistence = persistence;
        this.tickers = tickers;
        this.clock = clock;
    }

    public MovimentacaoCustodia atribuir(AtribuicaoCustodiaCommand command) {
        if (command == null) {
            throw ValidacaoEntradaCustodia.invalida("request", "Atribuição obrigatória");
        }
        var entrada = command.validada(tickers, clock);
        try { return persistence.persistirAtribuicao(entrada); }
        catch (RuntimeException falha) { throw FalhasEscritaOperacao.traduzir(falha); }
    }
}
