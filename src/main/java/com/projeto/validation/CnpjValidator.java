package com.projeto.validation;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CnpjValidator {

    private static final String MASCARADO = "\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}";
    private static final String SOMENTE_DIGITOS = "\\d{14}";

    public String normalizeAndValidate(String value) {
        if (value == null || value.isBlank()) {
            throw invalidCnpj();
        }

        String cnpj = value.trim();
        if (!cnpj.matches(MASCARADO) && !cnpj.matches(SOMENTE_DIGITOS)) {
            throw invalidCnpj();
        }

        String normalized = cnpj.replaceAll("\\D", "");
        if (allDigitsEqual(normalized)
                || calculateDigit(normalized, 12) != Character.digit(normalized.charAt(12), 10)
                || calculateDigit(normalized, 13) != Character.digit(normalized.charAt(13), 10)) {
            throw invalidCnpj();
        }

        return normalized;
    }

    private int calculateDigit(String cnpj, int length) {
        int weight = length - 7;
        int sum = 0;

        for (int index = 0; index < length; index++) {
            sum += Character.digit(cnpj.charAt(index), 10) * weight;
            weight--;
            if (weight == 1) {
                weight = 9;
            }
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private boolean allDigitsEqual(String value) {
        return value.chars().allMatch(character -> character == value.charAt(0));
    }

    private ApiException invalidCnpj() {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.CNPJ_INVALIDO, "CNPJ inválido");
    }
}
