package com.projeto.mappers;

import com.projeto.dto.CarteiraResponse;
import com.projeto.entities.Carteira;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarteiraMapperTest {

    @Test
    void mapsIdNameAndCreationDateExactly() {
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-08-21T14:30:00Z");
        Carteira carteira = new Carteira("Carteira Principal", creationDate);
        ReflectionTestUtils.setField(carteira, "id", 42L);

        CarteiraResponse response = new CarteiraMapper().toResponse(carteira);

        assertEquals(42L, response.id());
        assertEquals("Carteira Principal", response.nome());
        assertEquals(creationDate, response.dataCriacao());
    }
}
