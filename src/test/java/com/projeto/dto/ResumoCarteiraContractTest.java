package com.projeto.dto;

import com.projeto.entities.Moeda;
import com.projeto.mappers.ResumoCarteiraMapper;
import com.projeto.services.AgregadorPosicoesPorMoeda.TotaisPorMoeda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResumoCarteiraContractTest {

    @Test
    void exposesOnlyApprovedFields() {
        assertEquals(Set.of("carteiraId", "resumos"), components(ResumoCarteiraResponse.class));
        assertEquals(
                Set.of("moeda", "custoTotalPosicoes", "patrimonioAtual",
                        "resultadoNaoRealizadoTotal", "rentabilidadePercentual"),
                components(ResumoMoedaResponse.class)
        );
    }

    @Test
    void mapperOnlyProjectsReadyValuesAndReturnsImmutableList() {
        ResumoCarteiraMapper mapper = new ResumoCarteiraMapper();
        ResumoMoedaResponse brl = mapper.toMoedaResponse(new TotaisPorMoeda(
                Moeda.BRL,
                new BigDecimal("10000.000000000000"),
                new BigDecimal("12500.000000000000"),
                new BigDecimal("2500.000000000000")
        ), new BigDecimal("25.000000"));
        ResumoCarteiraResponse response = mapper.toResponse(1L, List.of(brl));

        assertEquals(1L, response.carteiraId());
        assertEquals(brl, response.resumos().get(0));
        assertThrows(UnsupportedOperationException.class, () -> response.resumos().clear());
    }

    private Set<String> components(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());
    }
}
