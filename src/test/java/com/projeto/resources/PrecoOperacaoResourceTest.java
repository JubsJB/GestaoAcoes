package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.integrations.cotacao.CotacaoHistoricaData;
import com.projeto.integrations.cotacao.CotacaoHistoricaProvider;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrecoOperacaoResourceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OperacaoRepository operacoes;
    @Autowired private CarteiraRepository carteiras;
    @Autowired private AcaoRepository acoes;

    @MockitoSpyBean(name = "brapiHistoricoStub")
    private CotacaoHistoricaProvider brasil;

    @MockitoSpyBean(name = "alphaHistoricoStub")
    private CotacaoHistoricaProvider eua;

    @BeforeEach
    void clean() {
        operacoes.deleteAll();
        carteiras.deleteAll();
        acoes.deleteAll();
        clearInvocations(brasil, eua);
    }

    @Test
    void previewsBrazilianRawCloseWithCanonicalIdentityCurrencyAndExactDate() throws Exception {
        acoes.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        LocalDate date = LocalDate.of(2026, 8, 20);
        doReturn(new CotacaoHistoricaData("PETR4", date, new BigDecimal("42.123456")))
                .when(brasil).consultarFechamento("PETR4", date);

        mockMvc.perform(get("/operacoes/previa-compra")
                        .param("ticker", " petr4 ")
                        .param("mercado", "BRASIL")
                        .param("dataOperacao", "2026-08-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.mercado").value("BRASIL"))
                .andExpect(jsonPath("$.moeda").value("BRL"))
                .andExpect(jsonPath("$.dataCotacao").value("2026-08-20"))
                .andExpect(jsonPath("$.precoUnitario").value(42.123456));

        verify(brasil).consultarFechamento("PETR4", date);
        verify(eua, never()).consultarFechamento(anyString(), any());
        org.junit.jupiter.api.Assertions.assertEquals(0, operacoes.count());
    }

    @Test
    void previewsUsCloseUsingOnlyUsProvider() throws Exception {
        acoes.saveAndFlush(action("AAPL", Mercado.EUA, Moeda.USD));

        mockMvc.perform(get("/operacoes/previa-compra")
                        .param("ticker", "AAPL")
                        .param("mercado", "EUA")
                        .param("dataOperacao", "2026-08-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moeda").value("USD"))
                .andExpect(jsonPath("$.precoUnitario").value(32.0));

        verify(eua).consultarFechamento("AAPL", LocalDate.of(2026, 8, 20));
        verify(brasil, never()).consultarFechamento(anyString(), any());
    }

    @Test
    void previewDoesNotAuthorizePurchaseAndPostConsultsAgain() throws Exception {
        Carteira carteira = carteiras.saveAndFlush(portfolio("Carteira"));
        acoes.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        LocalDate date = LocalDate.of(2026, 8, 20);

        mockMvc.perform(get("/operacoes/previa-compra")
                        .param("ticker", "PETR4").param("mercado", "BRASIL")
                        .param("dataOperacao", "2026-08-20"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/operacoes").contentType(MediaType.APPLICATION_JSON).content("""
                {"carteiraId":%d,"ticker":"PETR4","mercado":"BRASIL","tipo":"COMPRA",
                 "quantidade":1,"dataOperacao":"2026-08-20"}
                """.formatted(carteira.getId())))
                .andExpect(status().isCreated());

        verify(brasil, times(2)).consultarFechamento("PETR4", date);

        mockMvc.perform(post("/operacoes").contentType(MediaType.APPLICATION_JSON).content("""
                {"carteiraId":%d,"ticker":"PETR4","mercado":"BRASIL","tipo":"COMPRA",
                 "quantidade":1,"precoUnitario":32,"dataOperacao":"2026-08-20"}
                """.formatted(carteira.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));
    }

    @Test
    void mapsMissingInvalidAndMissingActionWithoutCallingProvider() throws Exception {
        mockMvc.perform(get("/operacoes/previa-compra")
                        .param("ticker", "PETR4").param("mercado", "BRASIL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"))
                .andExpect(jsonPath("$.details.dataOperacao").exists());
        mockMvc.perform(get("/operacoes/previa-compra")
                        .param("ticker", "PETR4").param("mercado", "INVALIDO")
                        .param("dataOperacao", "2026-08-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));
        mockMvc.perform(get("/operacoes/previa-compra")
                        .param("ticker", "PETR4").param("mercado", "BRASIL")
                        .param("dataOperacao", "2026-08-20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/operacoes/previa-compra"));

        verify(brasil, never()).consultarFechamento(anyString(), any());
        verify(eua, never()).consultarFechamento(anyString(), any());
    }

    @Test
    void previewPreservesEveryHistoricalErrorCode() throws Exception {
        acoes.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        LocalDate date = LocalDate.of(2026, 8, 20);
        assertHistoricalError(date, HttpStatus.UNPROCESSABLE_CONTENT, ErrorCodes.COTACAO_HISTORICA_INDISPONIVEL);
        assertHistoricalError(date, HttpStatus.UNPROCESSABLE_CONTENT, ErrorCodes.HISTORICO_COTACAO_FORA_DO_ALCANCE);
        assertHistoricalError(date, HttpStatus.NOT_FOUND, ErrorCodes.TICKER_INEXISTENTE);
        assertHistoricalError(date, HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO);
        assertHistoricalError(date, HttpStatus.BAD_GATEWAY, ErrorCodes.RESPOSTA_EXTERNA_INVALIDA);
        assertHistoricalError(date, HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL);
        assertHistoricalError(date, HttpStatus.GATEWAY_TIMEOUT, ErrorCodes.SERVICO_EXTERNO_TIMEOUT);
    }

    @Test
    void suggestsLatestApplicablePurchaseAndIgnoresLaterSaleAndPurchase() throws Exception {
        Carteira carteira = carteiras.saveAndFlush(portfolio("Carteira"));
        Acao acao = acoes.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        save(carteira, acao, TipoOperacao.COMPRA, "20", LocalDate.of(2026, 8, 10), 1);
        save(carteira, acao, TipoOperacao.COMPRA, "25.123456", LocalDate.of(2026, 8, 15), 1);
        save(carteira, acao, TipoOperacao.VENDA, "99", LocalDate.of(2026, 8, 16), 1);
        save(carteira, acao, TipoOperacao.COMPRA, "30", LocalDate.of(2026, 8, 20), 1);

        mockMvc.perform(get("/carteiras/{id}/operacoes/sugestao-preco-venda", carteira.getId())
                        .param("ticker", " petr4 ").param("mercado", "BRASIL")
                        .param("dataOperacao", "2026-08-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precoUnitarioSugerido").value(25.123456));

        verify(brasil, never()).consultarFechamento(anyString(), any());
        verify(eua, never()).consultarFechamento(anyString(), any());
    }

    @Test
    void sameDateUsesHighestExistingOrderAndAbsenceIsExplicitNull() throws Exception {
        Carteira carteira = carteiras.saveAndFlush(portfolio("Carteira"));
        Acao acao = acoes.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        save(carteira, acao, TipoOperacao.COMPRA, "20", LocalDate.of(2026, 8, 20), 1);
        save(carteira, acao, TipoOperacao.COMPRA, "25", LocalDate.of(2026, 8, 20), 2);

        mockMvc.perform(get("/carteiras/{id}/operacoes/sugestao-preco-venda", carteira.getId())
                        .param("ticker", "PETR4").param("mercado", "BRASIL")
                        .param("dataOperacao", "2026-08-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precoUnitarioSugerido").value(25.0));
        mockMvc.perform(get("/carteiras/{id}/operacoes/sugestao-preco-venda", carteira.getId())
                        .param("ticker", "PETR4").param("mercado", "BRASIL")
                        .param("dataOperacao", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precoUnitarioSugerido").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void suggestionDistinguishesMissingResourcesFromNoPurchase() throws Exception {
        mockMvc.perform(get("/carteiras/999999/operacoes/sugestao-preco-venda")
                        .param("ticker", "PETR4").param("mercado", "BRASIL")
                        .param("dataOperacao", "2026-08-20"))
                .andExpect(status().isNotFound());

        Carteira carteira = carteiras.saveAndFlush(portfolio("Carteira"));
        mockMvc.perform(get("/carteiras/{id}/operacoes/sugestao-preco-venda", carteira.getId())
                        .param("ticker", "PETR4").param("mercado", "BRASIL")
                        .param("dataOperacao", "2026-08-20"))
                .andExpect(status().isNotFound());
    }

    private void assertHistoricalError(LocalDate date, HttpStatus status, String code) throws Exception {
        doThrow(new ApiException(status, code, code)).when(brasil).consultarFechamento("PETR4", date);
        mockMvc.perform(get("/operacoes/previa-compra")
                        .param("ticker", "PETR4").param("mercado", "BRASIL")
                        .param("dataOperacao", date.toString()))
                .andExpect(status().is(status.value()))
                .andExpect(jsonPath("$.code").value(code));
    }

    private void save(Carteira carteira, Acao acao, TipoOperacao tipo, String preco, LocalDate data, int ordem) {
        operacoes.saveAndFlush(new Operacao(carteira, acao, null, tipo, new BigDecimal("1.000000"),
                new BigDecimal(preco).setScale(6), data, ordem, new BigDecimal(preco).setScale(12)));
    }

    private Carteira portfolio(String nome) {
        return new Carteira(nome, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private Acao action(String ticker, Mercado mercado, Moeda moeda) {
        return new Acao(ticker, "Empresa", mercado, moeda, new BigDecimal("99.000000"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }
}
