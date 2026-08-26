package com.projeto.dto;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.mappers.ResultadoRealizadoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultadoRealizadoContractTest {

    @Test
    void exposesExactlyTheSixApprovedFieldsAndMapperOnlyProjectsValues() {
        Set<String> fields = Arrays.stream(ResultadoRealizadoResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "acaoId",
                "ticker",
                "nomeEmpresa",
                "mercado",
                "moeda",
                "resultadoRealizado"
        ), fields);

        Acao acao = new Acao(
                "PETR4",
                "Petróleo Brasileiro S.A.",
                Mercado.BRASIL,
                Moeda.BRL,
                new BigDecimal("35.500000"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
        ReflectionTestUtils.setField(acao, "id", 7L);
        BigDecimal realizado = new BigDecimal("40.000000000000");

        ResultadoRealizadoResponse response = new ResultadoRealizadoMapper().toResponse(acao, realizado);

        assertEquals(7L, response.acaoId());
        assertEquals("PETR4", response.ticker());
        assertEquals("Petróleo Brasileiro S.A.", response.nomeEmpresa());
        assertEquals(Mercado.BRASIL, response.mercado());
        assertEquals(Moeda.BRL, response.moeda());
        assertEquals(realizado, response.resultadoRealizado());
        assertEquals(12, response.resultadoRealizado().scale());
    }
}
