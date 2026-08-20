package com.projeto.validation;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CepValidatorTest {

    private final CepValidator validator = new CepValidator();

    @Test
    void acceptsMaskedAndUnmaskedCepAndNormalizesIt() {
        assertEquals("01001000", validator.normalizeAndValidate("01001-000"));
        assertEquals("01001000", validator.normalizeAndValidate("01001000"));
    }

    @Test
    void rejectsInvalidCepFormat() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> validator.normalizeAndValidate("01001 000")
        );

        assertEquals(ErrorCodes.CEP_INVALIDO, exception.getCode());
    }
}
