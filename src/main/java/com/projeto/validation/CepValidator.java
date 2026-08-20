package com.projeto.validation;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CepValidator {

    private static final String MASCARADO = "\\d{5}-\\d{3}";
    private static final String SOMENTE_DIGITOS = "\\d{8}";

    public String normalizeAndValidate(String value) {
        if (value == null || value.isBlank()) {
            throw invalidCep();
        }

        String cep = value.trim();
        if (!cep.matches(MASCARADO) && !cep.matches(SOMENTE_DIGITOS)) {
            throw invalidCep();
        }

        return cep.replace("-", "");
    }

    private ApiException invalidCep() {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.CEP_INVALIDO, "CEP inválido");
    }
}
