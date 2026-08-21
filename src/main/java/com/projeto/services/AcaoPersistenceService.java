package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.repositories.AcaoRepository;
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

    public AcaoPersistenceService(AcaoRepository repository) {
        this.repository = repository;
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

        try {
            return repository.saveAndFlush(acao);
        } catch (DataIntegrityViolationException exception) {
            throw duplicate(acao.getTicker(), acao.getMercado());
        }
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
