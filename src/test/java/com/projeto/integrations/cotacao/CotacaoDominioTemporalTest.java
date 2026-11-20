package com.projeto.integrations.cotacao;

import com.projeto.entities.Moeda;
import com.projeto.entities.ReferenciaTipoCotacao;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;

class CotacaoDominioTemporalTest {
    private CotacaoCandidata candidata(LocalDate date, OffsetDateTime instant, OffsetDateTime obtained) {
        return new CotacaoCandidata("AAPL", "Apple", Moeda.USD, BigDecimal.TEN,
                date == null ? ReferenciaTipoCotacao.INSTANTE : ReferenciaTipoCotacao.DATA, instant, date, obtained);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001-01-01", "9999-12-31", "2000-02-29", "2024-02-29"})
    void dataCivilSemPisoOuLimiteFuturo(String value) {
        assertEquals(LocalDate.parse(value), candidata(LocalDate.parse(value), null,
                OffsetDateTime.parse("1960-01-01T00:00:00Z")).referenciaData());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 10000, 999999999})
    void dataForaDoDominio(int year) {
        ApiException error = assertThrows(ApiException.class, () -> candidata(LocalDate.of(year, 1, 1), null,
                OffsetDateTime.parse("2026-10-10T00:00:00Z")));
        assertEquals(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA, error.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1970-01-01T00:00:00Z", "1969-12-31T23:00:00-01:00", "1970-01-01T00:00:30+00:00:30"})
    void pisoInclusivoPorInstanteReal(String value) {
        assertEquals(OffsetDateTime.parse("1970-01-01T00:00:00Z"), candidata(null, OffsetDateTime.parse(value),
                OffsetDateTime.parse("1970-01-01T00:00:00Z")).referenciaInstante());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1969-12-31T23:59:59.999999Z", "2026-10-10T00:00:00.000001Z", "2026-10-09T00:00:00.123456001Z"})
    void rejeitaSemToleranciaOuPerda(String value) {
        assertEquals(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA, assertThrows(ApiException.class,
                () -> candidata(null, OffsetDateTime.parse(value), OffsetDateTime.parse("2026-10-10T00:00:00Z"))).getCode());
    }

    @Test
    void igualdadeExataComOffsetESegundos() {
        var obtained = OffsetDateTime.parse("2026-10-10T00:00:00.123456Z");
        var instant = OffsetDateTime.parse("2026-10-10T00:00:30.123456000+00:00:30");
        assertEquals(obtained, candidata(null, instant, obtained).referenciaInstante());
    }
}
