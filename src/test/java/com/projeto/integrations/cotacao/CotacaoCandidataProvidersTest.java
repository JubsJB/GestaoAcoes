package com.projeto.integrations.cotacao;

import com.projeto.entities.ReferenciaTipoCotacao;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CotacaoCandidataProvidersTest {
    private MockRestServiceServer brapiServer;
    private MockRestServiceServer alphaServer;
    private BrapiAdapter brapi;
    private AlphaVantageAdapter alpha;
    private Clock clock;

    @BeforeEach
    void preparar() {
        RestClient.Builder b = RestClient.builder().baseUrl("http://brapi.test");
        brapiServer = MockRestServiceServer.bindTo(b).build();
        brapi = new BrapiAdapter(b.build(), "test-placeholder");
        RestClient.Builder a = RestClient.builder().baseUrl("http://alpha.test");
        alphaServer = MockRestServiceServer.bindTo(a).build();
        alpha = new AlphaVantageAdapter(a.build(), "test-placeholder", millis -> { });
        clock = spy(Clock.fixed(Instant.parse("2026-10-03T15:00:00Z"), ZoneOffset.ofHours(-3)));
    }

    @AfterEach
    void verificarChamadas() {
        brapiServer.verify();
        alphaServer.verify();
    }

    @ParameterizedTest
    @CsvSource({"1787238000,2026-08-20T15:00:00Z", "'\"2026-08-20T12:30:00-03:00\"',2026-08-20T15:30:00Z"})
    void brapiMapeiaSegundosOuOffsetParaInstanteUtc(String raw, String esperado) {
        brapiQuote("32.123456", raw);
        CotacaoCandidata c = brapi.consultarCandidataAtualizacao("PETR4", "Empresa", "BRL", clock);
        assertEquals(ReferenciaTipoCotacao.INSTANTE, c.referenciaTipo());
        assertEquals(OffsetDateTime.parse(esperado), c.referenciaInstante());
        assertNull(c.referenciaData());
        assertEquals(new BigDecimal("32.123456"), c.cotacao());
        assertObtencao(c);
    }

    @Test
    void alphaPreservaDataCivilSemHoraOuTimezoneNaAtualizacao() {
        alphaQuote("224.410001", "\"2026-10-02\"");
        CotacaoCandidata c = alpha.consultarCandidataAtualizacao("AAPL", "Apple", "USD", clock);
        assertEquals(ReferenciaTipoCotacao.DATA, c.referenciaTipo());
        assertEquals(LocalDate.of(2026, 10, 2), c.referenciaData());
        assertNull(c.referenciaInstante());
        assertEquals(new BigDecimal("224.410001"), c.cotacao());
        assertObtencao(c);
    }

    @ParameterizedTest
    @CsvSource({"123456000,true", "123456001,false", "123456789,false", "999999000,true"})
    void brapiTextoFracionarioPassaPelaPrecisaoCentralNoCadastroEAtualizacao(String fracao, boolean aceito) {
        brapiQuote("32.123456", "\"2026-08-20T12:30:00." + fracao + "-03:00\"");
        brapiQuote("32.123456", "\"2026-08-20T12:30:00." + fracao + "-03:00\"");
        for (boolean cadastro : new boolean[]{true, false}) {
            if (aceito) {
                CotacaoCandidata c = cadastro ? brapi.consultarCandidataCadastro("PETR4", clock)
                        : brapi.consultarCandidataAtualizacao("PETR4", "Empresa", "BRL", clock);
                assertEquals(OffsetDateTime.parse("2026-08-20T15:30:00." + fracao + "Z"), c.referenciaInstante());
            } else {
                assertCode(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA,
                        () -> {
                            if (cadastro) brapi.consultarCandidataCadastro("PETR4", clock);
                            else brapi.consultarCandidataAtualizacao("PETR4", "Empresa", "BRL", clock);
                        });
            }
        }
    }

    @Test
    void cadastroAlphaMantemBuscaEQuoteEMapeiaData() {
        buscaAlpha();
        alphaQuote("224.41", "\"2026-10-02\"");
        CotacaoCandidata c = alpha.consultarCandidataCadastro("AAPL", clock);
        assertEquals("Apple", c.nomeEmpresa());
        assertEquals(ReferenciaTipoCotacao.DATA, c.referenciaTipo());
        assertNull(c.referenciaInstante());
        assertObtencao(c);
    }

    @ParameterizedTest
    @ValueSource(strings = {"brapi", "alpha"})
    void ausenciaDeReferenciaSoPermiteCadastro(String provider) {
        if (provider.equals("brapi")) {
            brapiQuote("10", null);
            brapiQuote("10", null);
            assertEquals(ReferenciaTipoCotacao.DESCONHECIDA,
                    brapi.consultarCandidataCadastro("PETR4", clock).referenciaTipo());
            clearInvocations(clock);
            assertCode(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA,
                    () -> brapi.consultarCandidataAtualizacao("PETR4", "Empresa", "BRL", clock));
        } else {
            buscaAlpha();
            alphaQuote("10", null);
            alphaQuote("10", null);
            CotacaoCandidata c = alpha.consultarCandidataCadastro("AAPL", clock);
            assertEquals(ReferenciaTipoCotacao.DESCONHECIDA, c.referenciaTipo());
            assertNull(c.referenciaInstante());
            assertNull(c.referenciaData());
            assertObtencao(c);
            clearInvocations(clock);
            assertCode(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA,
                    () -> alpha.consultarCandidataAtualizacao("AAPL", "Apple", "USD", clock));
        }
        verify(clock, never()).instant();
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"sem-data\"", "\"2026-02-30\"", "\"2026-10-02\"", "1.5", "true", "{}", "\"\""})
    void brapiReferenciaPresenteInvalidaNaoViraDesconhecida(String raw) {
        brapiQuote("10", raw);
        assertCode(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA,
                () -> brapi.consultarCandidataCadastro("PETR4", clock));
        verify(clock, never()).instant();
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"sem-data\"", "\"2026-02-30\"", "\"2026-10-02T00:00:00Z\"", "\"\"", "123"})
    void alphaTradingDayInvalidoNaoViraInstanteOuDesconhecida(String raw) {
        buscaAlpha();
        alphaQuote("10", raw);
        assertCode(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA,
                () -> alpha.consultarCandidataCadastro("AAPL", clock));
        verify(clock, never()).instant();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"nao-numero\"", "\"0\"", "\"-1\"", "\"1.1234567\"", "\"10000000000000\""})
    void precoInvalidoNaoProduzCandidataNosDoisProviders(String preco) {
        brapiQuote(preco, "1787238000");
        assertThrows(ApiException.class, () -> brapi.consultarCandidataCadastro("PETR4", clock));
        alphaServer.expect(requestTo(alphaUrl("GLOBAL_QUOTE", "symbol")))
                .andRespond(withSuccess("{\"Global Quote\":{\"01. symbol\":\"AAPL\",\"05. price\":"
                        + preco + ",\"07. latest trading day\":\"2026-10-02\"}}", MediaType.APPLICATION_JSON));
        assertThrows(ApiException.class,
                () -> alpha.consultarCandidataAtualizacao("AAPL", "Apple", "USD", clock));
        verify(clock, never()).instant();
    }

    @ParameterizedTest
    @ValueSource(strings = {"brapi", "alpha"})
    void precoAusenteNaoProduzCandidata(String provider) {
        if (provider.equals("brapi")) {
            brapiQuote(null, "1787238000");
            assertCode(ErrorCodes.COTACAO_INDISPONIVEL,
                    () -> brapi.consultarCandidataCadastro("PETR4", clock));
        } else {
            alphaQuote(null, "\"2026-10-02\"");
            assertCode(ErrorCodes.COTACAO_INDISPONIVEL,
                    () -> alpha.consultarCandidataAtualizacao("AAPL", "Apple", "USD", clock));
        }
        verify(clock, never()).instant();
    }

    @ParameterizedTest
    @CsvSource({"429,LIMITE_REQUISICOES_EXCEDIDO", "503,SERVICO_EXTERNO_INDISPONIVEL", "404,TICKER_INEXISTENTE"})
    void brapiErrosExternosMantemCodigosENaoCriamObtencao(int status, String codigo) {
        brapiServer.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        assertCode(codigo, () -> brapi.consultarCandidataCadastro("PETR4", clock));
        verify(clock, never()).instant();
    }

    @ParameterizedTest
    @CsvSource({"429,LIMITE_REQUISICOES_EXCEDIDO", "503,SERVICO_EXTERNO_INDISPONIVEL"})
    void alphaErrosExternosMantemCodigosENaoCriamObtencao(int status, String codigo) {
        alphaServer.expect(requestTo(alphaUrl("GLOBAL_QUOTE", "symbol")))
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        assertCode(codigo, () -> alpha.consultarCandidataAtualizacao("AAPL", "Apple", "USD", clock));
        verify(clock, never()).instant();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"results\":[null]}", "{\"error\":true,\"message\":\"unexpected\"}"})
    void brapiPayloadInvalidoNaoProduzCandidata(String body) {
        brapiServer.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertCode(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,
                () -> brapi.consultarCandidataCadastro("PETR4", clock));
        verify(clock, never()).instant();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{ | RESPOSTA_EXTERNA_INVALIDA",
            "{\"Note\":\"API call frequency exceeded\"} | LIMITE_REQUISICOES_EXCEDIDO",
            "{\"Information\":\"unexpected\"} | RESPOSTA_EXTERNA_INVALIDA",
            "{\"Error Message\":\"invalid symbol\"} | TICKER_INEXISTENTE",
            "{\"Global Quote\":{}} | COTACAO_INDISPONIVEL"})
    void alphaPayloadInvalidoOuErroNaoProduzCandidata(String body, String codigo) {
        alphaServer.expect(requestTo(alphaUrl("GLOBAL_QUOTE", "symbol")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertCode(codigo, () -> alpha.consultarCandidataAtualizacao("AAPL", "Apple", "USD", clock));
        verify(clock, never()).instant();
    }

    @Test
    void timeoutNosDoisProvidersNaoProduzObtencao() {
        RestClient client = RestClient.builder().requestFactory((uri, method) -> {
            throw new HttpTimeoutException("timeout");
        }).build();
        assertCode(ErrorCodes.SERVICO_EXTERNO_TIMEOUT,
                () -> new BrapiAdapter(client, "test-placeholder").consultarCandidataCadastro("PETR4", clock));
        assertCode(ErrorCodes.SERVICO_EXTERNO_TIMEOUT,
                () -> new AlphaVantageAdapter(client, "test-placeholder")
                        .consultarCandidataAtualizacao("AAPL", "Apple", "USD", clock));
        verify(clock, never()).instant();
    }

    private void brapiQuote(String preco, String referencia) {
        brapiServer.expect(requestTo("http://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess("{\"results\":[{\"symbol\":\"PETR4\",\"longName\":\"Empresa\",\"currency\":\"BRL\""
                        + (preco == null ? "" : ",\"regularMarketPrice\":" + preco)
                        + (referencia == null ? "" : ",\"regularMarketTime\":" + referencia)
                        + "}]}", MediaType.APPLICATION_JSON));
    }

    private void alphaQuote(String preco, String referencia) {
        alphaServer.expect(requestTo(alphaUrl("GLOBAL_QUOTE", "symbol")))
                .andRespond(withSuccess("{\"Global Quote\":{\"01. symbol\":\"AAPL\""
                        + (preco == null ? "" : ",\"05. price\":\"" + preco + "\"")
                        + (referencia == null ? "" : ",\"07. latest trading day\":" + referencia)
                        + "}}", MediaType.APPLICATION_JSON));
    }

    private void buscaAlpha() {
        alphaServer.expect(requestTo(alphaUrl("SYMBOL_SEARCH", "keywords")))
                .andRespond(withSuccess("{\"bestMatches\":[{\"1. symbol\":\"AAPL\",\"2. name\":\"Apple\","
                        + "\"4. region\":\"United States\",\"8. currency\":\"USD\"}]}", MediaType.APPLICATION_JSON));
    }

    private String alphaUrl(String function, String parameter) {
        return "http://alpha.test/query?function=" + function + "&" + parameter + "=AAPL&apikey=test-placeholder";
    }

    private void assertObtencao(CotacaoCandidata c) {
        assertEquals(OffsetDateTime.parse("2026-10-03T15:00:00Z"), c.cotacaoObtidaEm());
        assertEquals(ZoneOffset.UTC, c.cotacaoObtidaEm().getOffset());
    }

    private void assertCode(String codigo, Runnable operacao) {
        assertEquals(codigo, assertThrows(ApiException.class, operacao::run).getCode());
    }
}
