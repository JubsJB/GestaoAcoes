package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.HistoricoCotacao;
import com.projeto.entities.Mercado;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.HistoricoCotacaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AcaoPersistenceService {

    private final AcaoRepository repository;
    private final HistoricoCotacaoRepository historicoRepository;

    public AcaoPersistenceService(
            AcaoRepository repository,
            HistoricoCotacaoRepository historicoRepository
    ) {
        this.repository = repository;
        this.historicoRepository = historicoRepository;
    }

    @Transactional(readOnly = true)
    public void ensureAvailable(String ticker, Mercado mercado) {
        if (repository.existsByTickerAndMercado(ticker, mercado)) {
            throw duplicate(ticker, mercado);
        }
    }

    @Transactional
    public Acao saveUnique(Acao acao) {
        if (repository.existsByTickerAndMercado(acao.getTicker(), acao.getMercado())) {
            throw duplicate(acao.getTicker(), acao.getMercado());
        }

        Acao persisted;
        try {
            persisted = repository.saveAndFlush(acao);
        } catch (DataIntegrityViolationException exception) {
            throw duplicate(acao.getTicker(), acao.getMercado());
        }

        historicoRepository.saveAndFlush(new HistoricoCotacao(
                persisted,
                persisted.getCotacaoAtual(),
                persisted.getDataHoraCotacao()
        ));
        return persisted;
    }

    private ApiException duplicate(String ticker, Mercado mercado) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("ticker", ticker);
        details.put("mercado", mercado.name());
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCodes.ACAO_DUPLICADA,
                "Já existe uma ação cadastrada com este ticker e mercado",
                details
        );
    }
}
