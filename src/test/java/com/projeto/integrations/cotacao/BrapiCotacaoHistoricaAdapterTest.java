package com.projeto.integrations.cotacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrapiCotacaoHistoricaAdapterTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 2);
    private static final long DATE_TIMESTAMP = 1_788_318_000L;
    private final ObjectMapper json = new ObjectMapper();
    private final BrapiCotacaoHistoricaAdapter adapter = new BrapiCotacaoHistoricaAdapter(
            RestClient.builder().baseUrl("http://localhost").build(), "key"
    );

    @Test
    void callsCurrentEndpointAndReadsRealContractUsingSaoPauloDateAndRawClose() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://brapi.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrapiCotacaoHistoricaAdapter http = new BrapiCotacaoHistoricaAdapter(builder.build(), "secret-placeholder");
        server.expect(requestTo("http://brapi.test/api/v2/stocks/historical?symbols=PETR4&startDate=2026-09-02&endDate=2026-09-02&interval=1d"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer secret-placeholder"))
                .andRespond(withSuccess(payload(candle(DATE_TIMESTAMP, "48.2", "999")), MediaType.APPLICATION_JSON));

        CotacaoHistoricaData value = http.consultarFechamento("PETR4", DATE);

        assertEquals(DATE, value.dataPregao());
        assertEquals(new BigDecimal("48.2"), value.close());
        server.verify();
    }

    @Test
    void locatesExactCandleWithoutUsingPreviousCandleAsFallback() throws Exception {
        String candles = candle(1_788_231_600L, "47.1", "47.1") + "," + candle(DATE_TIMESTAMP, "48.2", "99");
        CotacaoHistoricaData value = adapter.parse(node(payload(candles)), "PETR4", DATE);
        assertEquals(DATE, value.dataPregao());
        assertEquals(new BigDecimal("48.2"), value.close());
    }

    @Test
    void classifiesMissingExactCandleInsideReturnedIntervalAsUnavailable() throws Exception {
        String candles = candle(1_788_231_600L, "47", "47") + "," + candle(1_788_404_400L, "49", "49");
        assertParseCode(payload(candles), ErrorCodes.COTACAO_HISTORICA_INDISPONIVEL);
    }

    @Test
    void classifiesEmptyHistoryAsUnavailable() throws Exception {
        assertParseCode(payload(""), ErrorCodes.COTACAO_HISTORICA_INDISPONIVEL);
    }

    @Test
    void classifiesDateOutsideReturnedBoundsAsOutOfRange() throws Exception {
        assertParseCode(payload(candle(1_788_404_400L, "49", "49")), ErrorCodes.HISTORICO_COTACAO_FORA_DO_ALCANCE);
    }

    @Test
    void rejectsMissingWrappersHistoryAndDivergentSymbol() throws Exception {
        invalid("{\"results\":[{\"symbol\":\"VALE3\",\"data\":{\"historicalDataPrice\":[]}}]}");
        invalid("{\"results\":[{\"symbol\":\"PETR4\"}]}");
        invalid("{\"results\":[{\"symbol\":\"PETR4\",\"data\":{}}]}");
        invalid("{\"results\":[]}");
    }

    @Test
    void rejectsMissingInvalidOrNonNumericTimestamp() throws Exception {
        invalid(payload("{\"close\":48.2}"));
        invalid(payload("{\"date\":\"2026-09-02\",\"close\":48.2}"));
        invalid(payload("{\"date\":999999999999999999999999,\"close\":48.2}"));
    }

    @Test
    void rejectsMissingZeroNegativeOrMalformedClose() throws Exception {
        invalid(payload("{\"date\":" + DATE_TIMESTAMP + "}"));
        invalid(payload(candle(DATE_TIMESTAMP, "0", "10")));
        invalid(payload(candle(DATE_TIMESTAMP, "-1", "10")));
        invalid(payload("{\"date\":" + DATE_TIMESTAMP + ",\"close\":\"x\"}"));
    }

    @Test
    void rejectsMalformedJsonResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://brapi.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://brapi.test/api/v2/stocks/historical?symbols=PETR4&startDate=2026-09-02&endDate=2026-09-02&interval=1d"))
                .andRespond(withSuccess("{", MediaType.APPLICATION_JSON));
        assertCode(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,
                () -> new BrapiCotacaoHistoricaAdapter(builder.build(), "key").consultarFechamento("PETR4", DATE));
        server.verify();
    }

    @Test
    void classifiesHttpErrorsTransportAndMissingConfiguration() {
        assertHttp(HttpStatus.NOT_FOUND, ErrorCodes.TICKER_INEXISTENTE);
        assertHttp(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO);
        assertHttp(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL);
        assertHttp(HttpStatus.BAD_REQUEST, ErrorCodes.RESPOSTA_EXTERNA_INVALIDA);
        RestClient timeout = RestClient.builder().requestFactory((uri, method) -> { throw new HttpTimeoutException("timeout"); }).build();
        assertCode(ErrorCodes.SERVICO_EXTERNO_TIMEOUT,
                () -> new BrapiCotacaoHistoricaAdapter(timeout, "key").consultarFechamento("PETR4", DATE));
        RestClient unavailable = RestClient.builder().requestFactory((uri, method) -> { throw new java.net.ConnectException("offline"); }).build();
        assertCode(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,
                () -> new BrapiCotacaoHistoricaAdapter(unavailable, "key").consultarFechamento("PETR4", DATE));
        assertCode(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,
                () -> new BrapiCotacaoHistoricaAdapter(RestClient.create(), " ").consultarFechamento("PETR4", DATE));
    }

    @Test
    void classifiesOnlyUnequivocalProviderMessages() throws Exception {
        assertCode(ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO,
                () -> adapter.parse(uncheckedNode("{\"message\":\"Rate limit exceeded\"}"), "PETR4", DATE));
        assertCode(ErrorCodes.TICKER_INEXISTENTE,
                () -> adapter.parse(uncheckedNode("{\"message\":\"Symbol PETR4 not found\"}"), "PETR4", DATE));
        invalid("{\"message\":\"Provider error\"}");
    }

    private static String payload(String candles) {
        return "{\"results\":[{\"requestedSymbol\":\"PETR4\",\"symbol\":\"PETR4\",\"changed\":false,"
                + "\"data\":{\"usedInterval\":\"1d\",\"usedRange\":\"1mo\",\"historicalDataPrice\":[" + candles + "]}}]}";
    }

    private static String candle(long timestamp, String close, String adjustedClose) {
        return "{\"date\":" + timestamp + ",\"open\":1,\"high\":1,\"low\":1,\"close\":" + close
                + ",\"volume\":1,\"adjustedClose\":" + adjustedClose + "}";
    }

    private void assertHttp(HttpStatus status, String code) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://brapi.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://brapi.test/api/v2/stocks/historical?symbols=PETR4&startDate=2026-09-02&endDate=2026-09-02&interval=1d"))
                .andRespond(withStatus(status));
        assertCode(code, () -> new BrapiCotacaoHistoricaAdapter(builder.build(), "key").consultarFechamento("PETR4", DATE));
        server.verify();
    }

    private void assertParseCode(String body, String code) throws Exception {
        assertCode(code, () -> adapter.parse(uncheckedNode(body), "PETR4", DATE));
    }

    private void invalid(String body) throws Exception {
        assertParseCode(body, ErrorCodes.RESPOSTA_EXTERNA_INVALIDA);
    }

    private void assertCode(String code, Runnable call) {
        ApiException error = assertThrows(ApiException.class, call::run);
        assertEquals(code, error.getCode());
    }

    private JsonNode node(String body) throws Exception { return json.readTree(body); }
    private JsonNode uncheckedNode(String body) {
        try { return node(body); } catch (Exception error) { throw new AssertionError(error); }
    }
}
