package com.projeto.integrations.cotacao;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.http.HttpTimeoutException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrapiAdapterTest {

    private MockRestServiceServer server;
    private BrapiAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://brapi.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BrapiAdapter(builder.build(), "test-placeholder");
    }

    @Test
    void mapsCompleteQuoteAndTrustworthyTimestampUsingBearerAuthentication() {
        server.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-placeholder"))
                .andRespond(withSuccess("""
                        {"results":[{
                          "symbol":"PETR4",
                          "longName":"Petróleo Brasileiro S.A.",
                          "shortName":"PETROBRAS",
                          "currency":"BRL",
                          "regularMarketPrice":32.123456,
                          "regularMarketTime":"2026-08-20T12:30:00-03:00",
                          "changed":false
                        }]}
                        """, MediaType.APPLICATION_JSON));

        CotacaoData result = adapter.consultar("PETR4");

        assertEquals("PETR4", result.ticker());
        assertEquals("Petróleo Brasileiro S.A.", result.nomeEmpresa());
        assertEquals("32.123456", result.cotacao().toPlainString());
        assertEquals(OffsetDateTime.parse("2026-08-20T12:30:00-03:00"), result.dataHoraCotacao());
        assertFalse(result.tickerAlteradoExplicitamente());
        server.verify();
    }

    @Test
    void mapsExplicitCanonicalTickerAndFallsBackToShortName() {
        server.expect(requestTo("http://brapi.test/api/quote/OLD3"))
                .andRespond(withSuccess("""
                        {"results":[{
                          "symbol":"NEW3",
                          "shortName":"EMPRESA NOVA",
                          "currency":"BRL",
                          "regularMarketPrice":10.50,
                          "regularMarketTime":1787238000,
                          "changed":true
                        }]}
                        """, MediaType.APPLICATION_JSON));

        CotacaoData result = adapter.consultar("OLD3");

        assertEquals("NEW3", result.ticker());
        assertEquals("EMPRESA NOVA", result.nomeEmpresa());
        assertTrue(result.tickerAlteradoExplicitamente());
        assertEquals("Z", result.dataHoraCotacao().getOffset().getId());
        server.verify();
    }

    @Test
    void returnsNullTimestampForUnusableValueAndExposesIncompleteFieldsToServiceValidation() {
        server.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess("""
                        {"results":[{"symbol":"PETR4","regularMarketTime":"sem-data"}]}
                        """, MediaType.APPLICATION_JSON));

        CotacaoData result = adapter.consultar("PETR4");

        assertNull(result.nomeEmpresa());
        assertNull(result.moeda());
        assertNull(result.cotacao());
        assertNull(result.dataHoraCotacao());
        server.verify();
    }

    @Test
    void exposesNonNumericQuoteAsUnavailableForBusinessValidation() {
        server.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess("""
                        {"results":[{
                          "symbol":"PETR4","longName":"Empresa","currency":"BRL",
                          "regularMarketPrice":"nao-numerica"
                        }]}
                        """, MediaType.APPLICATION_JSON));

        CotacaoData result = adapter.consultar("PETR4");

        assertNull(result.cotacao());
        server.verify();
    }

    @Test
    void mapsEmptyAndNotFoundToTickerNotFound() {
        server.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));
        assertCode(ErrorCodes.TICKER_INEXISTENTE, () -> adapter.consultar("PETR4"));
        server.verify();
        server.reset();

        server.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertCode(ErrorCodes.TICKER_INEXISTENTE, () -> adapter.consultar("PETR4"));
        server.verify();
    }

    @Test
    void mapsRateLimitUnavailableAndMalformedPayload() {
        assertHttpError(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO);
        assertHttpError(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL);

        server.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess(
                        "{\"error\":true,\"message\":\"Rate limit exceeded\"}",
                        MediaType.APPLICATION_JSON
                ));
        assertCode(ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, () -> adapter.consultar("PETR4"));
        server.verify();
        server.reset();

        server.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess("{", MediaType.APPLICATION_JSON));
        assertCode(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA, () -> adapter.consultar("PETR4"));
        server.verify();
    }

    @Test
    void validatesConfigurationLazilyAndMapsTimeoutWithoutNetwork() {
        BrapiAdapter unconfigured = new BrapiAdapter(RestClient.create(), " ");
        ApiException missing = assertThrows(ApiException.class, () -> unconfigured.consultar("PETR4"));
        assertEquals(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL, missing.getCode());
        assertFalse(missing.getMessage().contains("test-placeholder"));

        RestClient timeoutClient = RestClient.builder()
                .requestFactory((uri, method) -> {
                    throw new HttpTimeoutException("timeout");
                })
                .build();
        BrapiAdapter timeout = new BrapiAdapter(timeoutClient, "test-placeholder");
        assertCode(ErrorCodes.SERVICO_EXTERNO_TIMEOUT, () -> timeout.consultar("PETR4"));
    }

    private void assertHttpError(HttpStatus status, String expectedCode) {
        server.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withStatus(status));
        assertCode(expectedCode, () -> adapter.consultar("PETR4"));
        server.verify();
        server.reset();
    }

    private void assertCode(String expectedCode, Runnable invocation) {
        ApiException exception = assertThrows(ApiException.class, invocation::run);
        assertEquals(expectedCode, exception.getCode());
    }
}
