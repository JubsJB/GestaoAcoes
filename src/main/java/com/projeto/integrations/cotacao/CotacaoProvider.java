package com.projeto.integrations.cotacao;

import com.projeto.entities.Mercado;
import java.time.Clock;

public interface CotacaoProvider {

    Mercado mercado();

    CotacaoData consultar(String ticker);

    default CotacaoData consultarAtualizacao(String ticker, String nomeEmpresa, String moeda) {
        return consultar(ticker);
    }

    /** Consulta real; somente o cadastro admite referência ausente com preço válido. */
    default CotacaoCandidata consultarCandidataCadastro(String ticker, Clock clock) {
        return CotacaoCandidata.obtidaDe(consultar(ticker), ticker, mercado(), true, clock);
    }

    /** Não consulta nem modifica estado persistido; comparação e aceitação são posteriores. */
    default CotacaoCandidata consultarCandidataAtualizacao(
            String ticker, String nomeEmpresa, String moeda, Clock clock) {
        return CotacaoCandidata.obtidaDe(consultarAtualizacao(ticker, nomeEmpresa, moeda),
                ticker, mercado(), false, clock);
    }
}
