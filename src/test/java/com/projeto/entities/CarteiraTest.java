package com.projeto.entities;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarteiraTest {

    @Test
    void updatesOnlyNameAndPreservesIdAndCreationDate() {
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-08-21T14:30:00Z");
        Carteira carteira = new Carteira("Carteira Original", creationDate);
        ReflectionTestUtils.setField(carteira, "id", 42L);

        carteira.atualizarNome("Carteira Ágil Principal");

        assertEquals(42L, carteira.getId());
        assertEquals("Carteira Ágil Principal", carteira.getNome());
        assertEquals(creationDate, carteira.getDataCriacao());
    }

    @Test
    void acceptsSameNameWithoutChangingIdentityOrCreationDate() {
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-08-20T10:15:30Z");
        Carteira carteira = new Carteira("Carteira Principal", creationDate);
        ReflectionTestUtils.setField(carteira, "id", 7L);

        carteira.atualizarNome("Carteira Principal");

        assertEquals(7L, carteira.getId());
        assertEquals("Carteira Principal", carteira.getNome());
        assertEquals(creationDate, carteira.getDataCriacao());
    }
}
