package com.projeto.services;

import com.projeto.entities.*;
import com.projeto.repositories.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Leitura integral na transação protegida do chamador; não adquire locks ou abre transações. */
@Component
public class HistoricoQuantitativoCustodia {
    private final OperacaoRepository operacoes;
    private final MovimentacaoCustodiaRepository movimentos;
    private final EstornoMovimentacaoCustodiaRepository estornos;
    public HistoricoQuantitativoCustodia(OperacaoRepository operacoes, MovimentacaoCustodiaRepository movimentos,
            EstornoMovimentacaoCustodiaRepository estornos) {
        this.operacoes=operacoes; this.movimentos=movimentos; this.estornos=estornos;
    }
    public record Carregado(List<Operacao> financeiros, List<EventoQuantitativo> eventos) {
        public Carregado { financeiros=List.copyOf(financeiros); eventos=List.copyOf(eventos); }
    }
    Carregado carregar(Long carteiraId, Long acaoId) {
        // Sem corte por data: a candidata também deve preservar os eventos posteriores.
        var financeiros=operacoes.listarParaReplayQuantitativo(carteiraId,acaoId);
        var originais=movimentos.listarPorCarteiraEAcao(carteiraId,acaoId);
        var ids=originais.stream().map(MovimentacaoCustodia::getId).toList();
        var consultados=ids.isEmpty()?List.<EstornoMovimentacaoCustodia>of():estornos.listarPorCarteiraEOriginais(carteiraId,ids);
        Map<Long,EstornoMovimentacaoCustodia> porOriginal=new HashMap<>();
        for(var estorno:consultados) {
            if(porOriginal.putIfAbsent(estorno.getMovimentacaoOriginalId(),estorno)!=null)
                throw new IllegalStateException("Vínculos históricos duplicados");
        }
        var vinculos=new ArrayList<VinculoEstornoConsultado>();
        for(var original:originais) {
            var estorno=porOriginal.remove(original.getId());
            vinculos.add(estorno==null?VinculoEstornoConsultado.ausenciaVerificada(original.getId())
                    :VinculoEstornoConsultado.vinculoPresente(original.getId(),estorno));
        }
        if(!porOriginal.isEmpty())throw new IllegalStateException("Vínculos históricos fora do contexto");
        var fontes=new AdaptadorEventosQuantitativos().adaptar(carteiraId,acaoId,financeiros,originais,vinculos);
        return new Carregado(financeiros, new NormalizadorEventosQuantitativos().normalizar(carteiraId,acaoId,fontes.financeiros(),fontes.custodias()));
    }
}
