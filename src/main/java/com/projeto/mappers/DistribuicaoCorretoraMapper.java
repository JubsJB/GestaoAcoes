package com.projeto.mappers;

import com.projeto.dto.*;
import com.projeto.services.CalculadoraDistribuicaoCorretora;
import org.springframework.stereotype.Component;

@Component
public class DistribuicaoCorretoraMapper {
    public DistribuicaoCarteiraResponse toResponse(CalculadoraDistribuicaoCorretora.Distribuicao source) {
        return new DistribuicaoCarteiraResponse(source.carteiraId(), source.posicoes().stream().map(p ->
                new PosicaoDistribuicaoResponse(p.acaoId(), p.ticker(), p.mercado(), p.moeda(),
                        p.quantidadeConsolidada(), p.quantidadeAtribuidaTotal(), p.reconciliada(),
                        p.diagnosticoReconciliacao() == null ? null :
                                new DiagnosticoReconciliacaoResponse(p.diagnosticoReconciliacao().codigo()),
                        p.corretoras().stream().map(c -> new QuantidadeCorretoraResponse(
                                c.corretoraId(), c.nome(), c.quantidadeLiquida())).toList(),
                        p.diagnosticosCorretoras().stream().map(d -> new DiagnosticoCorretoraResponse(
                                d.corretoraId(), d.nome(), d.quantidadeFinal(), d.saldoNegativoAtual(),
                                d.possuiDeficitHistorico(), d.primeiroDeficit() == null ? null :
                                        new PrimeiroDeficitResponse(d.primeiroDeficit().operacaoId(),
                                                d.primeiroDeficit().dataOperacao(), d.primeiroDeficit().ordemNoDia(),
                                                d.primeiroDeficit().saldoAposOperacao()))).toList())).toList());
    }
}
