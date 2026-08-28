package com.projeto.entities;

import jakarta.persistence.ManyToOne;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotCarteiraTest {

    @Test
    void validatesRequiredStateAndNormalizesTimestampToUtc() {
        Carteira carteira = new Carteira("Principal", OffsetDateTime.now(ZoneOffset.UTC));
        SnapshotCarteira snapshot = new SnapshotCarteira(
                carteira,
                OffsetDateTime.parse("2026-08-27T12:00:00-03:00")
        );

        assertEquals(carteira, snapshot.getCarteira());
        assertEquals(OffsetDateTime.parse("2026-08-27T15:00:00Z"), snapshot.getDataHoraSnapshot());
        assertThrows(IllegalArgumentException.class, () -> new SnapshotCarteira(null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotCarteira(carteira, null));
    }

    @Test
    void associationIsLazyWithoutCascadeAndModelsHaveNoCollections() throws Exception {
        ManyToOne association = SnapshotCarteira.class.getDeclaredField("carteira")
                .getAnnotation(ManyToOne.class);
        assertEquals(jakarta.persistence.FetchType.LAZY, association.fetch());
        assertEquals(0, association.cascade().length);
        assertFalse(java.util.Arrays.stream(Carteira.class.getDeclaredFields())
                .anyMatch(field -> java.util.Collection.class.isAssignableFrom(field.getType())));
        assertFalse(java.util.Arrays.stream(SnapshotCarteira.class.getDeclaredFields())
                .anyMatch(field -> java.util.Collection.class.isAssignableFrom(field.getType())));
    }
}
