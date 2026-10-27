package com.projeto.services;

import com.projeto.dto.RegistroCustodiaResponse;
import com.projeto.dto.DetalheCustodiaResponse;
import com.projeto.entities.*;
import com.projeto.mappers.CustodiaMapper;
import com.projeto.repositories.*;
import com.projeto.services.exceptions.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ConsultaMovimentacaoCustodiaService {
    private static final Comparator<RegistroCustodiaResponse> ORDEM=Comparator
            .comparing((RegistroCustodiaResponse r)->r.dataRegistro().toInstant(),Comparator.reverseOrder())
            .thenComparingInt(r->r.tipoRegistro()==RegistroCustodiaResponse.TipoRegistro.MOVIMENTACAO?0:1)
            .thenComparing(RegistroCustodiaResponse::id,Comparator.reverseOrder());
    private final CarteiraRepository carteiras;
    private final MovimentacaoCustodiaRepository movimentos;
    private final EstornoMovimentacaoCustodiaRepository estornos;
    private final CustodiaMapper mapper;
    public ConsultaMovimentacaoCustodiaService(CarteiraRepository carteiras,
            MovimentacaoCustodiaRepository movimentos, EstornoMovimentacaoCustodiaRepository estornos,
            CustodiaMapper mapper) {
        this.carteiras=carteiras; this.movimentos=movimentos; this.estornos=estornos; this.mapper=mapper;
    }
    @Transactional(readOnly=true, isolation=Isolation.SERIALIZABLE, propagation=Propagation.REQUIRES_NEW)
    public List<RegistroCustodiaResponse> historico(Long carteiraId, String natureza, String situacao) {
        var filtroNatureza=filtro(RegistroCustodiaResponse.Natureza.class,natureza,"natureza");
        var filtroSituacao=filtro(SituacaoMovimentacaoCustodia.class,situacao,"situacao");
        conferirCarteira(carteiraId);
        var originais=movimentos.listarPorCarteira(carteiraId);
        var vinculos=vinculos(carteiraId,originais,estornos.listarPorCarteira(carteiraId));
        var linhas=new ArrayList<RegistroCustodiaResponse>();
        for(var m:originais){
            var e=vinculos.get(m.getId());
            linhas.add(mapper.original(m,e==null?VinculoEstornoConsultado.ausenciaVerificada(m.getId())
                    :VinculoEstornoConsultado.vinculoPresente(m.getId(),e)));
        }
        for(var e:vinculos.values())linhas.add(mapper.estorno(e));
        return linhas.stream().filter(r->filtroNatureza==null || r.natureza()==filtroNatureza)
                .filter(r->r.tipoRegistro()==RegistroCustodiaResponse.TipoRegistro.ESTORNO
                        ?filtroNatureza==RegistroCustodiaResponse.Natureza.ESTORNO || filtroSituacao==null
                        :filtroSituacao==null || r.situacao()==filtroSituacao)
                .sorted(ORDEM).toList();
    }
    @Transactional(readOnly=true, isolation=Isolation.SERIALIZABLE, propagation=Propagation.REQUIRES_NEW)
    public DetalheCustodiaResponse detalhe(Long carteiraId,Long movimentacaoId) {
        id(carteiraId,"carteiraId");id(movimentacaoId,"movimentacaoId");
        conferirCarteira(carteiraId);
        var m=movimentos.buscarPorCarteiraEId(carteiraId,movimentacaoId)
                .orElseThrow(()->new ObjectNotFoundException("Movimentação não encontrada"));
        var encontrados=vinculos(carteiraId,List.of(m),
                estornos.listarPorCarteiraEOriginais(carteiraId,List.of(movimentacaoId)));
        var e=encontrados.get(movimentacaoId);
        var vinculo=e==null?VinculoEstornoConsultado.ausenciaVerificada(movimentacaoId)
                :VinculoEstornoConsultado.vinculoPresente(movimentacaoId,e);
        return new DetalheCustodiaResponse(mapper.original(m,vinculo),e==null?null:mapper.estorno(e));
    }
    private <E extends Enum<E>> E filtro(Class<E> tipo,String valor,String campo) {
        if(valor==null)return null;
        try{return Enum.valueOf(tipo,valor);}catch(IllegalArgumentException e){throw invalida(campo);}
    }
    private void conferirCarteira(Long id) {
        id(id,"carteiraId");
        if(!carteiras.existsById(id))throw new ObjectNotFoundException("Carteira não encontrada");
    }
    private void id(Long valor,String campo){if(valor==null || valor<=0)throw invalida(campo);}
    private ApiException invalida(String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST,ErrorCodes.REQUEST_INVALIDO,
                "Parâmetro de consulta inválido",Map.of(campo,"Valor inválido"));
    }
    private Map<Long,EstornoMovimentacaoCustodia> vinculos(Long carteiraId,
            List<MovimentacaoCustodia> originais,List<EstornoMovimentacaoCustodia> consultados) {
        Set<Long> ids=new HashSet<>();
        for(var m:originais){
            if(!carteiraId.equals(m.getCarteiraId()) || !ids.add(m.getId()))
                throw new IllegalStateException("Histórico de custódia inconsistente");
        }
        Map<Long,EstornoMovimentacaoCustodia> resultado=new HashMap<>();
        for(var e:consultados){
            if(!ids.contains(e.getMovimentacaoOriginalId())
                    || !carteiraId.equals(e.getMovimentacaoOriginal().getCarteiraId())
                    || resultado.putIfAbsent(e.getMovimentacaoOriginalId(),e)!=null)
                throw new IllegalStateException("Vínculos de custódia inconsistentes");
        }
        return resultado;
    }
}
