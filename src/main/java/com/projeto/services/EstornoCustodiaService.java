package com.projeto.services;

import com.projeto.entities.EstornoMovimentacaoCustodia;
import org.springframework.stereotype.Service;

@Service
public class EstornoCustodiaService {
    private final TransferenciaCustodiaPersistenceService persistence;

    public EstornoCustodiaService(TransferenciaCustodiaPersistenceService persistence) {
        this.persistence = persistence;
    }

    public EstornoMovimentacaoCustodia estornar(EstornoCustodiaCommand command) {
        if (command == null) throw ValidacaoEntradaCustodia.invalida("request", "Estorno obrigatório");
        var entrada = command.validada();
        try { return persistence.persistirEstorno(entrada); }
        catch (RuntimeException falha) { throw FalhasEscritaOperacao.traduzir(falha); }
    }
}
