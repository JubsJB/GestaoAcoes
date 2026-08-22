package com.projeto.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarteiraUpdateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void trimsOnlyEdgesAndPreservesInternalSpacesAccentsAndCase() {
        CarteiraUpdateRequest request = new CarteiraUpdateRequest("  Carteira  Ágil Principal  ");

        assertEquals("Carteira  Ágil Principal", request.getNome());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsExactly255CharactersAfterTrim() {
        String name = "Á".repeat(255);
        CarteiraUpdateRequest request = new CarteiraUpdateRequest("  " + name + "  ");

        assertEquals(name, request.getNome());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsNullEmptyBlankAndNameAboveMaximum() {
        List<String> invalidNames = java.util.Arrays.asList(null, "", "   ", " " + "a".repeat(256) + " ");

        for (String invalidName : invalidNames) {
            CarteiraUpdateRequest request = new CarteiraUpdateRequest(invalidName);
            assertTrue(validator.validate(request).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("nome")));
        }
    }

    @Test
    void rejectsAnyPropertyOtherThanName() {
        CarteiraUpdateRequest request = new CarteiraUpdateRequest("Carteira Principal");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> request.rejectUnknownProperty("id", 10L)
        );

        assertEquals("Campo não permitido na atualização de carteira: id", exception.getMessage());
    }
}
