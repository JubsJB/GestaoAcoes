package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.repositories.AcaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcaoPersistenceServiceTest {

    @Mock
    private AcaoRepository repository;

    @Test
    void rejectsKnownDuplicateBeforeSaving() {
        when(repository.existsByTickerAndMercado("AAPL", Mercado.EUA)).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> new AcaoPersistenceService(repository).ensureAvailable("AAPL", Mercado.EUA)
        );

        assertDuplicate(exception);
        assertEquals("AAPL", exception.getDetails().get("ticker"));
        assertEquals("EUA", exception.getDetails().get("mercado"));
    }

    @Test
    void translatesLateUniqueViolationFromConcurrentRace() {
        Acao acao = action("PETR4", Mercado.BRASIL, Moeda.BRL);
        when(repository.existsByTickerAndMercado("PETR4", Mercado.BRASIL)).thenReturn(false);
        when(repository.saveAndFlush(acao)).thenThrow(new DataIntegrityViolationException("unique"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> new AcaoPersistenceService(repository).saveUnique(acao)
        );

        assertDuplicate(exception);
    }

    private Acao action(String ticker, Mercado mercado, Moeda moeda) {
        return new Acao(
                ticker,
                "Empresa",
                mercado,
                moeda,
                new BigDecimal("10.000000"),
                OffsetDateTime.of(2026, 8, 20, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }

    private void assertDuplicate(ApiException exception) {
        assertEquals(ErrorCodes.ACAO_DUPLICADA, exception.getCode());
        assertEquals(409, exception.getStatus().value());
    }
}
