package com.projeto.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrecoOperacaoResponseContractTest {

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void serializesPurchasePreviewWithExactCivilDateAndDecimal() throws Exception {
        String serialized = json.writeValueAsString(new PreviaPrecoCompraResponse(
                "PETR4", Mercado.BRASIL, Moeda.BRL, LocalDate.of(2026, 8, 20),
                new BigDecimal("42.300000")
        ));
        JsonNode value = json.readTree(serialized);

        assertEquals("PETR4", value.path("ticker").asText());
        assertEquals("BRASIL", value.path("mercado").asText());
        assertEquals("BRL", value.path("moeda").asText());
        org.junit.jupiter.api.Assertions.assertTrue(serialized.contains("\"dataCotacao\":\"2026-08-20\""));
        org.junit.jupiter.api.Assertions.assertTrue(serialized.contains("\"precoUnitario\":42.300000"));
    }

    @Test
    void serializesSuggestionWithExactPriceOrExplicitNull() throws Exception {
        String serialized = json.writeValueAsString(
                new SugestaoPrecoVendaResponse(new BigDecimal("1234567890123.123456"))
        );
        JsonNode present = json.readTree(serialized);
        JsonNode absent = json.readTree(json.writeValueAsString(new SugestaoPrecoVendaResponse(null)));

        org.junit.jupiter.api.Assertions.assertTrue(
                serialized.contains("\"precoUnitarioSugerido\":1234567890123.123456")
        );
        assertTrue(absent.has("precoUnitarioSugerido"));
        assertTrue(absent.path("precoUnitarioSugerido").isNull());
    }
}
