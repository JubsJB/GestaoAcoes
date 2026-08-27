package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.HistoricoCotacao;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.HistoricoCotacaoRepository;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class AcaoCotacaoPersistenceService {

    private final AcaoRepository repository;
    private final HistoricoCotacaoRepository historicoRepository;

    public AcaoCotacaoPersistenceService(
            AcaoRepository repository,
            HistoricoCotacaoRepository historicoRepository
    ) {
        this.repository = repository;
        this.historicoRepository = historicoRepository;
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
        Acao persisted = repository.saveAndFlush(acao);
        historicoRepository.saveAndFlush(new HistoricoCotacao(
                persisted,
                persisted.getCotacaoAtual(),
                persisted.getDataHoraCotacao()
        ));
        return persisted;
    }
}
