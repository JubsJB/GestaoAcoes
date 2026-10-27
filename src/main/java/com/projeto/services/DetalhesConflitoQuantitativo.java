package com.projeto.services;

import java.util.*;
import static com.projeto.services.EventoQuantitativo.*;

/** Apresentação pura, sem consultas ou dados de exceções SQL. */
final class DetalhesConflitoQuantitativo {
    private DetalhesConflitoQuantitativo() { }
    static Map<String, Object> montar(Long carteiraId, Long acaoId, Grupo origem,
            java.math.BigDecimal quantidadeSolicitada, String motivo,
            ValidadorHistoricoQuantitativo.Conflito causa, List<EventoQuantitativo> eventos) {
        Map<String,Object> details=new LinkedHashMap<>();
        details.put("carteiraId", carteiraId);
        details.put("acaoId", acaoId);
        details.put("origem", grupo(origem));
        if (quantidadeSolicitada != null) {
            details.put("quantidadeSolicitada", quantidadeSolicitada);
        }
        details.put("evento", null);
        details.put("motivo", motivo);
        if(causa!=null) {
            var evidencia=causa.evidencia();details.put("saldoAntes",evidencia.saldoAntes());details.put("saldoDepois",evidencia.saldoDepois());
            details.put("deficitAntes",evidencia.deficitAntes());details.put("deficitDepois",evidencia.deficitDepois());
            if(evidencia.saldoDisponivel()!=null)details.put("quantidadeDisponivel",evidencia.saldoDisponivel());
            if(causa.grupo()!=null)details.put("grupo",grupo(causa.grupo()));
            if(causa.identidade().idPersistido()!=null) {
                var evento=eventos.stream().filter(e->e.identidade().equals(causa.identidade())).findFirst().orElseThrow();
                var p=evento.posicao();
                Map<String,Object> posicao=p.financeira()?Map.of("ordemNoDia",p.ancoraFinanceira())
                        :Map.of("ordemFinanceiraAncora",p.ancoraFinanceira(),"numerador",p.fracao().getNumerador().toString(),"denominador",p.fracao().getDenominador().toString());
                details.put("evento",Map.of("dominio",evento.identidade().dominio().name(),"id",evento.identidade().idPersistido(),
                        "natureza",evento.natureza().name(),"dataEfetiva",p.dataEfetiva(),"posicaoCronologica",posicao));
            }
        }
        return details;
    }

    static Map<String,Object> grupo(Grupo grupo) {
        Map<String,Object> detalhe=new LinkedHashMap<>();detalhe.put("corretoraId",grupo.corretoraId());
        detalhe.put("categoria",grupo.corretoraId()==null?"SEM_CORRETORA_INFORMADA":"CORRETORA");
        return Collections.unmodifiableMap(detalhe);
    }
}
