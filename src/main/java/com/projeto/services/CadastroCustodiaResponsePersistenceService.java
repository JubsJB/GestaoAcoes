package com.projeto.services;

import com.projeto.dto.RegistroCustodiaResponse;
import com.projeto.dto.DetalheCustodiaResponse;
import com.projeto.entities.MovimentacaoCustodia;
import com.projeto.entities.VinculoEstornoConsultado;
import com.projeto.mappers.CustodiaMapper;
import com.projeto.repositories.EstornoMovimentacaoCustodiaRepository;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Monta a resposta sob o mesmo lock da escrita, antes da confirmação do proxy. */
@Service
@Transactional(propagation=Propagation.REQUIRED,isolation=Isolation.READ_COMMITTED,readOnly=false,rollbackFor=Exception.class)
public class CadastroCustodiaResponsePersistenceService {
    private final TransferenciaCustodiaService transferencias;
    private final AtribuicaoCustodiaService atribuicoes;
    private final EstornoCustodiaService reversoes;
    private final EstornoMovimentacaoCustodiaRepository estornos;
    private final CustodiaMapper mapper;
    @PersistenceContext private EntityManager entityManager;
    public CadastroCustodiaResponsePersistenceService(TransferenciaCustodiaService transferencias,
            AtribuicaoCustodiaService atribuicoes,EstornoCustodiaService reversoes,EstornoMovimentacaoCustodiaRepository estornos,CustodiaMapper mapper){
        this.transferencias=transferencias;this.atribuicoes=atribuicoes;this.estornos=estornos;this.mapper=mapper;
        this.reversoes=reversoes;
    }
    public RegistroCustodiaResponse transferir(TransferenciaCustodiaCommand entrada){
        return resposta(transferencias.transferir(entrada));
    }
    public RegistroCustodiaResponse atribuir(AtribuicaoCustodiaCommand entrada){
        return resposta(atribuicoes.atribuir(entrada));
    }
    public DetalheCustodiaResponse estornar(EstornoCustodiaCommand entrada){
        var criado=reversoes.estornar(entrada);
        entityManager.refresh(criado);
        var original=criado.getMovimentacaoOriginal();
        entityManager.refresh(original);
        var vinculos=estornos.listarPorCarteiraEOriginais(original.getCarteiraId(),List.of(original.getId()));
        if(vinculos.size()!=1 || !criado.getId().equals(vinculos.get(0).getId())){
            throw new IllegalStateException("Consulta de vínculo não corresponde ao estorno gravado");
        }
        // Vínculo consultado na própria transação; a resposta só sai do proxy após sua confirmação.
        var consultado=vinculos.get(0);
        return new DetalheCustodiaResponse(mapper.original(original,
                VinculoEstornoConsultado.vinculoPresente(original.getId(),consultado)),mapper.estorno(consultado));
    }
    private RegistroCustodiaResponse resposta(MovimentacaoCustodia original){
        // Releitura da própria escrita antes do commit: reflete também a precisão física do timestamp.
        entityManager.refresh(original);
        // Consulta completa de vínculo, ainda sob o lock: não presume ATIVA por persist/flush.
        var vinculos=estornos.listarPorCarteiraEOriginais(original.getCarteiraId(),List.of(original.getId()));
        if(!vinculos.isEmpty())throw new IllegalStateException("Original recém-criada com vínculo inesperado");
        return mapper.original(original,VinculoEstornoConsultado.ausenciaVerificada(original.getId()));
    }
}
