package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.repositories.AcaoRepository;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class AcaoCotacaoPersistenceService {

    private final AcaoRepository repository;

    public AcaoCotacaoPersistenceService(AcaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Acao atualizarSePosterior(
            Long id,
            BigDecimal cotacaoCandidata,
            OffsetDateTime dataHoraCandidata
    ) {
        Acao acao = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Ação não encontrada para o id: " + id
                ));

        if (!dataHoraCandidata.isAfter(acao.getDataHoraCotacao())) {
            return acao;
        }

        acao.atualizarCotacao(cotacaoCandidata, dataHoraCandidata);
        return repository.saveAndFlush(acao);
    }
}
