package com.projeto.validation;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TickerNormalizer {

    public String normalizeAndValidate(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw invalidTicker();
        }

        String normalized = ticker.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 30) {
            throw invalidTicker();
        }
        return normalized;
    }

    private ApiException invalidTicker() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.TICKER_INVALIDO,
                "Ticker inválido"
        );
    }
}
