package com.projeto.integrations.cotacao;

import com.projeto.entities.Mercado;

public interface CotacaoProvider {

    Mercado mercado();

    CotacaoData consultar(String ticker);
}
