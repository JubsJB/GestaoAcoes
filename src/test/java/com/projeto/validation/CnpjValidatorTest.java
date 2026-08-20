package com.projeto.validation;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CnpjValidatorTest {

    private final CnpjValidator validator = new CnpjValidator();

    @Test
    void acceptsMaskedAndUnmaskedCnpjAndNormalizesIt() {
        assertEquals("11222333000181", validator.normalizeAndValidate("11.222.333/0001-81"));
        assertEquals("11222333000181", validator.normalizeAndValidate("11222333000181"));
    }

    @Test
    void rejectsInvalidCheckDigitsAndInvalidFormat() {
        ApiException invalidDigits = assertThrows(
                ApiException.class,
                () -> validator.normalizeAndValidate("11.222.333/0001-82")
        );
        ApiException invalidFormat = assertThrows(
                ApiException.class,
                () -> validator.normalizeAndValidate("11 222 333 0001 81")
        );

        assertEquals(ErrorCodes.CNPJ_INVALIDO, invalidDigits.getCode());
        assertEquals(ErrorCodes.CNPJ_INVALIDO, invalidFormat.getCode());
    }

    @Test
    void rejectsRepeatedDigits() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> validator.normalizeAndValidate("11.111.111/1111-11")
        );

        assertEquals(ErrorCodes.CNPJ_INVALIDO, exception.getCode());
    }
}
