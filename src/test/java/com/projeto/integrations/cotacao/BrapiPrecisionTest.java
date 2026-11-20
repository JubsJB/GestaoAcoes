package com.projeto.integrations.cotacao;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrapiPrecisionTest {
    private MockRestServiceServer server;
    private BrapiAdapter adapter;
    private Clock clock;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://brapi.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BrapiAdapter(builder.build(), "test-placeholder");
        clock = spy(Clock.fixed(Instant.parse("2026-10-03T15:00:00Z"), ZoneOffset.UTC));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890123.123456", "9999999999999.999999", "32.123456", "10.500000", "1", "1.230000e2"})
    void numeroJsonRepresentavelPreservadoExatamente(String decimal) {
        // Mesmo RestClient/conversores padrão de ExternalApiConfig; somente o transporte é simulado.
        responder(decimal);
        assertEquals(new BigDecimal(decimal), adapter.consultar("PETR4").cotacao());
        server.verify();
        server.reset();
        responder(decimal);
        CotacaoCandidata candidata = adapter.consultarCandidataAtualizacao("PETR4", "Empresa", "BRL", clock);
        assertEquals(new BigDecimal(decimal).setScale(6), candidata.cotacao());
        assertEquals(Instant.parse("2026-10-02T15:00:00Z"), candidata.referenciaInstante().toInstant());
        assertEquals(ZoneOffset.UTC, candidata.referenciaInstante().getOffset());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"34.1234560000000001", "1.1234567", "10000000000000", "10000000000000.000001"})
    void numeroJsonForaDaEscalaNuncaViraPrecoValido(String decimal) {
        responder(decimal);
        ApiException erro = assertThrows(ApiException.class,
                () -> adapter.consultarCandidataAtualizacao("PETR4", "Empresa", "BRL", clock));
        assertEquals(ErrorCodes.COTACAO_FORA_DA_PRECISAO, erro.getCode());
        assertEquals(422, erro.getStatus().value());
        verify(clock, never()).instant();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890123.123456", "9999999999999.999999", "10.500000", "1.230000e2"})
    void textoDecimalJaSuportadoPreservaEscalaEValor(String decimal) {
        responder("\"" + decimal + "\"");
        assertEquals(new BigDecimal(decimal), adapter.consultar("PETR4").cotacao());
        server.verify();
        server.reset();
        responder("\"" + decimal + "\"");
        assertEquals(new BigDecimal(decimal).setScale(6),
                adapter.consultarCandidataCadastro("PETR4", clock).cotacao());
        server.verify();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0|COTACAO_INDISPONIVEL", "-1.000001|COTACAO_INDISPONIVEL",
            "null|COTACAO_INDISPONIVEL", "true|COTACAO_INDISPONIVEL",
            "{}|COTACAO_INDISPONIVEL", "[32.12]|COTACAO_INDISPONIVEL",
            "\"nao-numero\"|COTACAO_INDISPONIVEL", "\"NaN\"|COTACAO_INDISPONIVEL",
            "\"Infinity\"|COTACAO_INDISPONIVEL", "\"-Infinity\"|COTACAO_INDISPONIVEL",
            "\"34.1234560000000001\"|COTACAO_FORA_DA_PRECISAO",
            "\"10000000000000\"|COTACAO_FORA_DA_PRECISAO"})
    void entradaInvalidaNaoProduzCandidataNemObtencao(String json, String codigo) {
        responder(json);
        ApiException erro = assertThrows(ApiException.class,
                () -> adapter.consultarCandidataCadastro("PETR4", clock));
        assertEquals(codigo, erro.getCode());
        assertEquals(422, erro.getStatus().value());
        verify(clock, never()).instant();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity", "{", "01"})
    void jsonNaoPadraoOuMalformadoPreservaErroExterno(String json) {
        responder(json);
        ApiException erro = assertThrows(ApiException.class,
                () -> adapter.consultarCandidataCadastro("PETR4", clock));
        assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA, erro.getCode());
        assertEquals(502, erro.getStatus().value());
        verify(clock, never()).instant();
        server.verify();
    }

    private void responder(String precoJson) {
        server.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess("{\"results\":[{\"symbol\":\"PETR4\",\"longName\":\"Empresa\","
                        + "\"currency\":\"BRL\",\"regularMarketPrice\":" + precoJson
                        + ",\"regularMarketTime\":\"2026-10-02T12:00:00-03:00\"}]}", MediaType.APPLICATION_JSON));
    }
}
