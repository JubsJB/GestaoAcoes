package com.projeto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotCarteiraMoedaTest {

    @Test
    void validatesRequiredPositiveExactNumericState() throws Exception {
        Carteira carteira = new Carteira("Principal", OffsetDateTime.now());
        SnapshotCarteira snapshot = new SnapshotCarteira(carteira, OffsetDateTime.now());
        SnapshotCarteiraMoeda componente = new SnapshotCarteiraMoeda(
                snapshot,
                Moeda.BRL,
                new BigDecimal("99999999999999999999999999.123456789012")
        );

        assertEquals(new BigDecimal("99999999999999999999999999.123456789012"), componente.getPatrimonioAtual());
        Column column = SnapshotCarteiraMoeda.class.getDeclaredField("patrimonioAtual")
                .getAnnotation(Column.class);
        assertEquals(38, column.precision());
        assertEquals(12, column.scale());
        assertThrows(IllegalArgumentException.class, () -> new SnapshotCarteiraMoeda(null, Moeda.BRL, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotCarteiraMoeda(snapshot, null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotCarteiraMoeda(snapshot, Moeda.BRL, null));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotCarteiraMoeda(snapshot, Moeda.BRL, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotCarteiraMoeda(snapshot, Moeda.BRL, BigDecimal.ONE.negate()));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotCarteiraMoeda(snapshot, Moeda.BRL, new BigDecimal("1.0000000000001")));

        ManyToOne association = SnapshotCarteiraMoeda.class.getDeclaredField("snapshotCarteira")
                .getAnnotation(ManyToOne.class);
        assertEquals(jakarta.persistence.FetchType.LAZY, association.fetch());
        assertEquals(0, association.cascade().length);
    }
}
