package com.projeto.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HistoricoCotacaoTest {

    private final Acao acao = new Acao(
            "PETR4", "Empresa", Mercado.BRASIL, Moeda.BRL,
            new BigDecimal("30.000000"), OffsetDateTime.parse("2026-08-20T15:30:00Z")
    );

    @Test
    void aceitaSomenteCotacaoPositivaExataNaPrecisaoAprovada() {
        HistoricoCotacao historico = new HistoricoCotacao(
                acao, new BigDecimal("9999999999999.999999"), OffsetDateTime.parse("2026-08-20T15:30:00Z")
        );

        assertEquals(new BigDecimal("9999999999999.999999"), historico.getCotacao());
        assertThrows(IllegalArgumentException.class, () -> new HistoricoCotacao(
                acao, BigDecimal.ZERO, OffsetDateTime.parse("2026-08-20T15:30:00Z")));
        assertThrows(IllegalArgumentException.class, () -> new HistoricoCotacao(
                acao, BigDecimal.ONE.negate(), OffsetDateTime.parse("2026-08-20T15:30:00Z")));
        assertThrows(IllegalArgumentException.class, () -> new HistoricoCotacao(
                acao, new BigDecimal("1.1234567"), OffsetDateTime.parse("2026-08-20T15:30:00Z")));
        assertThrows(IllegalArgumentException.class, () -> new HistoricoCotacao(
                acao, new BigDecimal("10000000000000.000000"), OffsetDateTime.parse("2026-08-20T15:30:00Z")));
    }

    @Test
    void exigeSomenteOsQuatroCamposAprovados() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-20T15:30:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> new HistoricoCotacao(null, BigDecimal.ONE, timestamp));
        assertThrows(IllegalArgumentException.class,
                () -> new HistoricoCotacao(acao, null, timestamp));
        assertThrows(IllegalArgumentException.class,
                () -> new HistoricoCotacao(acao, BigDecimal.ONE, null));
    }
}
