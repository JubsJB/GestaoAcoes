package com.projeto.dto;

import com.projeto.entities.Moeda;
import com.projeto.mappers.PatrimonioMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatrimonioContractTest {

    @Test
    void exposesOnlyApprovedFields() {
        assertEquals(
                Set.of("carteiraId", "patrimonios"),
                components(PatrimonioResponse.class)
        );
        assertEquals(
                Set.of("moeda", "patrimonioAtual"),
                components(PatrimonioMoedaResponse.class)
        );
    }

    @Test
    void mapperOnlyProjectsReadyValuesAndReturnsImmutableList() {
        PatrimonioMapper mapper = new PatrimonioMapper();
        PatrimonioMoedaResponse brl = mapper.toMoedaResponse(
                Moeda.BRL,
                new BigDecimal("12500.000000000000")
        );
        PatrimonioResponse response = mapper.toResponse(1L, List.of(brl));

        assertEquals(1L, response.carteiraId());
        assertEquals(Moeda.BRL, response.patrimonios().get(0).moeda());
        assertEquals(new BigDecimal("12500.000000000000"),
                response.patrimonios().get(0).patrimonioAtual());
        assertEquals(12, response.patrimonios().get(0).patrimonioAtual().scale());
        assertThrows(UnsupportedOperationException.class, () -> response.patrimonios().clear());
    }

    private Set<String> components(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());
    }
}
