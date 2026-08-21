package com.projeto.integrations.cotacao;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AlphaVantageAdapterTest {

    private static final String KEY = "test-placeholder";

    private MockRestServiceServer server;
    private AlphaVantageAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://alpha.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new AlphaVantageAdapter(builder.build(), KEY);
    }

    @Test
    void usesExactUsSearchNameThenLatestQuoteWithoutOverview() {
        expectSearch("AAPL", """
                {"bestMatches":[{
                  "1. symbol":"AAPL","2. name":"Apple Inc.",
                  "4. region":"United States","8. currency":"USD"
                }]}
                """);
        expectQuote("AAPL", """
                {"Global Quote":{"01. symbol":"AAPL","05. price":"224.4100","07. latest trading day":"2026-08-19"}}
                """);

        CotacaoData result = adapter.consultar("AAPL");

        assertEquals("Apple Inc.", result.nomeEmpresa());
        assertEquals("224.4100", result.cotacao().toPlainString());
        assertEquals("USD", result.moeda());
        assertNull(result.dataHoraCotacao());
        server.verify();
    }

    @Test
    void callsOverviewOnlyWhenExactSearchHasNoUsableName() {
        expectSearch("MSFT", """
                {"bestMatches":[{
                  "1. symbol":"MSFT","2. name":" ",
                  "4. region":"USA","8. currency":"USD"
                }]}
                """);
        server.expect(requestTo("http://alpha.test/query?function=OVERVIEW&symbol=MSFT&apikey=" + KEY))
                .andRespond(withSuccess("{\"Name\":\"Microsoft Corporation\"}", MediaType.APPLICATION_JSON));
        expectQuote("MSFT", """
                {"Global Quote":{"01. symbol":"MSFT","05. price":"510.20"}}
                """);

        CotacaoData result = adapter.consultar("MSFT");

        assertEquals("Microsoft Corporation", result.nomeEmpresa());
        server.verify();
    }

    @Test
    void approximateWrongRegionAndWrongCurrencyStopBeforeFurtherCalls() {
        assertSearchDoesNotConfirm("AAPL", "AAPL34", "United States", "USD");
        assertSearchDoesNotConfirm("AAPL", "AAPL", "Brazil", "USD");
    }

    @Test
    void exactUsResultWithWrongCurrencyIsRejectedAsInvalidExternalResponse() {
        expectSearch("AAPL", exactSearchWithCurrency("AAPL", "Apple Inc.", "BRL"));

        assertCode(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA, () -> adapter.consultar("AAPL"));
        server.verify();
    }

    @Test
    void rejectsMissingOverviewNameAndEmptyQuote() {
        expectSearch("AAPL", """
                {"bestMatches":[{
                  "1. symbol":"AAPL","2. name":"",
                  "4. region":"United States","8. currency":"USD"
                }]}
                """);
        server.expect(requestTo("http://alpha.test/query?function=OVERVIEW&symbol=AAPL&apikey=" + KEY))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertCode(ErrorCodes.DADOS_EXTERNOS_INCOMPLETOS, () -> adapter.consultar("AAPL"));
        server.verify();
        server.reset();

        expectSearch("AAPL", exactSearch("AAPL", "Apple Inc."));
        expectQuote("AAPL", "{\"Global Quote\":{}}");
        assertCode(ErrorCodes.COTACAO_INDISPONIVEL, () -> adapter.consultar("AAPL"));
        server.verify();
    }

    @Test
    void differentiatesLimitTickerErrorAndInvalidInformationPayloads() {
        expectSearch("AAPL", "{\"Note\":\"API call frequency exceeded\"}");
        assertCode(ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, () -> adapter.consultar("AAPL"));
        server.verify();
        server.reset();

        expectSearch("AAPL", "{\"Error Message\":\"invalid symbol\"}");
        assertCode(ErrorCodes.TICKER_INEXISTENTE, () -> adapter.consultar("AAPL"));
        server.verify();
        server.reset();

        expectSearch("AAPL", "{\"Information\":\"unexpected provider message\"}");
        assertCode(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA, () -> adapter.consultar("AAPL"));
        server.verify();
    }

    @Test
    void mapsHttpUnavailableTimeoutAndMissingConfigurationWithoutLeakingKey() {
        server.expect(requestTo("http://alpha.test/query?function=SYMBOL_SEARCH&keywords=AAPL&apikey=" + KEY))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertCode(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL, () -> adapter.consultar("AAPL"));
        server.verify();

        AlphaVantageAdapter missing = new AlphaVantageAdapter(RestClient.create(), "");
        ApiException missingError = assertThrows(ApiException.class, () -> missing.consultar("AAPL"));
        assertEquals(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL, missingError.getCode());

        RestClient timeoutClient = RestClient.builder()
                .requestFactory((uri, method) -> {
                    throw new HttpTimeoutException("timeout");
                })
                .build();
        AlphaVantageAdapter timeout = new AlphaVantageAdapter(timeoutClient, KEY);
        ApiException timeoutError = assertThrows(ApiException.class, () -> timeout.consultar("AAPL"));
        assertEquals(ErrorCodes.SERVICO_EXTERNO_TIMEOUT, timeoutError.getCode());
        assertEquals(504, timeoutError.getStatus().value());
    }

    private void assertSearchDoesNotConfirm(String requested, String returned, String region, String currency) {
        expectSearch(requested, """
                {"bestMatches":[{
                  "1. symbol":"%s","2. name":"Empresa",
                  "4. region":"%s","8. currency":"%s"
                }]}
                """.formatted(returned, region, currency));
        assertCode(ErrorCodes.TICKER_INEXISTENTE, () -> adapter.consultar(requested));
        server.verify();
        server.reset();
    }

    private String exactSearch(String ticker, String name) {
        return exactSearchWithCurrency(ticker, name, "USD");
    }

    private String exactSearchWithCurrency(String ticker, String name, String currency) {
        return """
                {"bestMatches":[{
                  "1. symbol":"%s","2. name":"%s",
                  "4. region":"United States","8. currency":"%s"
                }]}
                """.formatted(ticker, name, currency);
    }

    private void expectSearch(String ticker, String body) {
        server.expect(requestTo("http://alpha.test/query?function=SYMBOL_SEARCH&keywords=" + ticker + "&apikey=" + KEY))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectQuote(String ticker, String body) {
        server.expect(requestTo("http://alpha.test/query?function=GLOBAL_QUOTE&symbol=" + ticker + "&apikey=" + KEY))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void assertCode(String expectedCode, Runnable invocation) {
        ApiException exception = assertThrows(ApiException.class, invocation::run);
        assertEquals(expectedCode, exception.getCode());
    }
}
