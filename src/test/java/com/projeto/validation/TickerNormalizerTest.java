package com.projeto.validation;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TickerNormalizerTest {

    private final TickerNormalizer normalizer = new TickerNormalizer();

    @Test
    void trimsAndUppercasesWithoutChangingPunctuationOrSuffixes() {
        assertEquals("PETR4.SA", normalizer.normalizeAndValidate("  petr4.sa  "));
        assertEquals("BRK.B", normalizer.normalizeAndValidate("brk.b"));
        assertEquals("ABC-DEF", normalizer.normalizeAndValidate("abc-def"));
    }

    @Test
    void rejectsNullBlankAndTickerLongerThanStorageContract() {
        assertInvalid(null);
        assertInvalid("   ");
        assertInvalid("A".repeat(31));
    }

    private void assertInvalid(String ticker) {
        ApiException exception = assertThrows(ApiException.class, () -> normalizer.normalizeAndValidate(ticker));
        assertEquals(ErrorCodes.TICKER_INVALIDO, exception.getCode());
        assertEquals(400, exception.getStatus().value());
    }
}
