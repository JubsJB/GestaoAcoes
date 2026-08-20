package com.projeto.services;

import com.projeto.entities.Corretora;
import com.projeto.repositories.CorretoraRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorretoraPersistenceService {

    private final CorretoraRepository repository;

    public CorretoraPersistenceService(CorretoraRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public void ensureCnpjAvailable(String cnpj) {
        if (repository.existsByCnpj(cnpj)) {
            throw duplicateCnpj();
        }
    }

    @Transactional
    public Corretora saveUnique(Corretora corretora) {
        if (repository.existsByCnpj(corretora.getCnpj())) {
            throw duplicateCnpj();
        }

        try {
            return repository.saveAndFlush(corretora);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateCnpj();
        }
    }

    private ApiException duplicateCnpj() {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCodes.CORRETORA_DUPLICADA,
                "Já existe uma corretora cadastrada com este CNPJ"
        );
    }
}
