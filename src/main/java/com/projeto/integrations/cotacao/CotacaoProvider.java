package com.projeto.integrations.cotacao;

import com.projeto.entities.Mercado;

public interface CotacaoProvider {

    Mercado mercado();

    CotacaoData consultar(String ticker);

    default CotacaoData consultarAtualizacao(String ticker, String nomeEmpresa, String moeda) {
        return consultar(ticker);
    }
}
