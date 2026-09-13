package com.projeto.resources;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.integrations.cotacao.AlphaVantageAdapter;
import com.projeto.mappers.AcaoMapper;
import com.projeto.repositories.AcaoRepository;
import com.projeto.resources.exceptions.ResourceExceptionHandler;
import com.projeto.services.AcaoCotacaoPersistenceService;
import com.projeto.services.AcaoPersistenceService;
import com.projeto.services.AcaoService;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.validation.TickerNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcaoEuaRegistrationRequestsTest {

    private MockRestServiceServer server;
    private MockMvc mvc;
    private AcaoPersistenceService persistence;
    private AtomicInteger requests;
    private AcaoService service;
    private AcaoRepository repository;

    @BeforeEach
    void setUp() {
        requests = new AtomicInteger();
        RestClient.Builder builder = RestClient.builder().baseUrl("http://alpha.test")
                .requestInterceptor((request, body, execution) -> {
                    requests.incrementAndGet();
                    return execution.execute(request, body);
                });
        server = MockRestServiceServer.bindTo(builder).build();
        persistence = mock(AcaoPersistenceService.class);
        repository = mock(AcaoRepository.class);
        AlphaVantageAdapter adapter = new AlphaVantageAdapter(builder.build(), "test-placeholder");
        ReflectionTestUtils.setField(adapter, "registrationIntervalMs", 1L);
        service = new AcaoService(new TickerNormalizer(),
                List.of(adapter),
                persistence, mock(AcaoCotacaoPersistenceService.class), repository,
                new AcaoMapper(), Clock.systemUTC());
        mvc = MockMvcBuilders.standaloneSetup(new AcaoResource(service))
                .setControllerAdvice(new ResourceExceptionHandler()).build();
    }

    @ParameterizedTest
    @CsvSource({"NVDA,NVIDIA Corporation", "AAPL,Apple Inc."})
    void postUsesExactlyTwoRequestsAndReusesExactSearchIdentity(String ticker, String name) throws Exception {
        // Approximate result comes first: its name must never be used.
        expectSearch(ticker, """
                {"bestMatches":[
                  {"1. symbol":"%sX","2. name":"Wrong company","4. region":"United States","8. currency":"USD"},
                  {"1. symbol":"%s","2. name":"%s","4. region":"United States","8. currency":"USD"}
                ]}
                """.formatted(ticker, ticker, name));
        expectQuote(ticker, """
                {"Global Quote":{"01. symbol":"%s","05. price":"123.450001"}}
                """.formatted(ticker));
        when(persistence.saveUnique(any())).thenAnswer(invocation -> {
            Acao acao = invocation.getArgument(0);
            ReflectionTestUtils.setField(acao, "id", 1L);
            return acao;
        });

        register(" " + ticker.toLowerCase(java.util.Locale.ROOT) + " ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value(ticker))
                .andExpect(jsonPath("$.nomeEmpresa").value(name))
                .andExpect(jsonPath("$.moeda").value("USD"))
                .andExpect(jsonPath("$.cotacaoAtual").value(123.450001));
        verify(persistence).ensureAvailable(ticker, Mercado.EUA);
        verify(persistence).saveUnique(any());
        verifyNoMoreInteractions(persistence);
        verifyRequests(2);
    }

    @ParameterizedTest
    @CsvSource({
            "NVDA,United States,USD,' ',422,DADOS_EXTERNOS_INCOMPLETOS",
            "NVDAX,United States,USD,NVIDIA,404,TICKER_INEXISTENTE",
            "NVDA,Brazil,USD,NVIDIA,404,TICKER_INEXISTENTE",
            "NVDA,United States,BRL,NVIDIA,502,RESPOSTA_EXTERNA_INVALIDA"
    })
    void invalidSearchStopsAfterOneRequest(String symbol, String region, String currency,
                                         String name, int status, String code) throws Exception {
        expectSearch("NVDA", search(symbol, region, currency, name));
        assertFailure(status, code, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "", "not-a-price"})
    void invalidPriceFailsAfterTwoRequestsWithoutPersistence(String price) throws Exception {
        expectSearch("NVDA", search("NVDA", "United States", "USD", "NVIDIA Corporation"));
        expectQuote("NVDA", """
                {"Global Quote":{"01. symbol":"NVDA","05. price":"%s"}}
                """.formatted(price));
        assertFailure(422, ErrorCodes.COTACAO_INDISPONIVEL, 2);
    }

    @Test
    void differentQuoteSymbolFailsAfterTwoRequests() throws Exception {
        expectSearch("NVDA", search("NVDA", "United States", "USD", "NVIDIA Corporation"));
        expectQuote("NVDA", "{\"Global Quote\":{\"01. symbol\":\"AAPL\",\"05. price\":\"123\"}}");
        assertFailure(502, ErrorCodes.RESPOSTA_EXTERNA_INVALIDA, 2);
    }

    @ParameterizedTest
    @CsvSource({"1,http", "2,http", "1,note", "2,note", "1,information", "2,information"})
    void rateLimitAtEitherStepPreserves429WithoutRetry(int step, String kind) throws Exception {
        if (step == 2) {
            expectSearch("NVDA", search("NVDA", "United States", "USD", "NVIDIA Corporation"));
        }
        ResponseCreator response = switch (kind) {
            case "http" -> withStatus(HttpStatus.TOO_MANY_REQUESTS);
            case "note" -> withSuccess("{\"Note\":\"API call frequency exceeded\"}", MediaType.APPLICATION_JSON);
            default -> withSuccess("{\"Information\":\"Rate limit: 25 requests per day\"}", MediaType.APPLICATION_JSON);
        };
        server.expect(requestTo(url(step == 1 ? "SYMBOL_SEARCH&keywords=" : "GLOBAL_QUOTE&symbol=", "NVDA")))
                .andRespond(response);
        assertFailure(429, ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, step);
    }

    @Test
    void duplicateStopsBeforeExternalRequests() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, ErrorCodes.ACAO_DUPLICADA, "Ação duplicada"))
                .when(persistence).ensureAvailable("NVDA", Mercado.EUA);
        assertFailure(409, ErrorCodes.ACAO_DUPLICADA, 0);
    }

    @ParameterizedTest
    @CsvSource({"1,http", "2,http", "1,note", "2,note", "1,information", "2,information"})
    void newPostImmediatelySucceedsAfterProviderRecovers(int step, String kind) throws Exception {
        rateLimitAtEitherStepPreserves429WithoutRetry(step, kind);
        // Reset only the HTTP expectations, retaining the same controller/service/adapter.
        server.reset();
        expectRecoveredNvda();
        register("NVDA").andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeEmpresa").value("NVIDIA Corp"))
                .andExpect(jsonPath("$.cotacaoAtual").value(218.29));
        verifyRequests(step + 2);
        verify(persistence, times(1)).saveUnique(any());
    }

    @Test
    void refreshFailureCooldownCannotProduceRegistrationRateLimit() throws Exception {
        Acao existing = new Acao("NVDA", "NVIDIA Corp", Mercado.EUA,
                com.projeto.entities.Moeda.USD, new java.math.BigDecimal("200"),
                java.time.OffsetDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(existing, "id", 1L);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        server.expect(requestTo(url("GLOBAL_QUOTE&symbol=", "NVDA")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        ApiException first = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> service.atualizarCotacao(1L));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, first.getStatus());
        verifyRequests(1);
        server.reset();
        expectRecoveredNvda();
        // Refresh still replays its failure without HTTP, per its existing policy.
        ApiException cached = org.junit.jupiter.api.Assertions.assertThrows(ApiException.class,
                () -> service.atualizarCotacao(1L));
        assertEquals(ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, cached.getCode());
        assertEquals(1, requests.get());
        // Persistence is mocked to isolate the registration path from uniqueness.
        register("NVDA").andExpect(status().isCreated());
        verifyRequests(3);
    }

    private void expectRecoveredNvda() {
        expectSearch("NVDA", search("NVDA", "United States", "USD", "NVIDIA Corp"));
        expectQuote("NVDA", """
                {"Global Quote":{"01. symbol":"NVDA","05. price":"218.2900",
                 "07. latest trading day":"2026-09-11"}}
                """);
        when(persistence.saveUnique(any())).thenAnswer(invocation -> {
            Acao acao = invocation.getArgument(0);
            ReflectionTestUtils.setField(acao, "id", 2L);
            return acao;
        });
    }

    private ResultActions register(String ticker) throws Exception {
        return mvc.perform(post("/acoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticker\":\"" + ticker + "\",\"mercado\":\"EUA\"}"));
    }

    private void assertFailure(int status, String code, int count) throws Exception {
        register("NVDA").andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
        verify(persistence, never()).saveUnique(any());
        verifyRequests(count);
    }

    private void verifyRequests(int count) {
        server.verify(); // Expectations are once each; any additional HTTP request fails the test.
        assertEquals(count, requests.get());
    }

    private String search(String symbol, String region, String currency, String name) {
        return """
                {"bestMatches":[{"1. symbol":"%s","2. name":"%s","4. region":"%s","8. currency":"%s"}]}
                """.formatted(symbol, name, region, currency);
    }

    private String url(String functionAndParameter, String ticker) {
        return "http://alpha.test/query?function=" + functionAndParameter + ticker + "&apikey=test-placeholder";
    }

    private void expectSearch(String ticker, String body) {
        server.expect(requestTo(url("SYMBOL_SEARCH&keywords=", ticker)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectQuote(String ticker, String body) {
        server.expect(requestTo(url("GLOBAL_QUOTE&symbol=", ticker)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
