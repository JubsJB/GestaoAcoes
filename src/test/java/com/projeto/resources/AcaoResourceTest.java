package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.dto.AcaoCreateRequest;
import com.projeto.dto.AcaoResponse;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.services.AcaoService;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcaoResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcaoService service;

    @Test
    void createsBrazilianActionWithCompleteDtoAndStableLocation() throws Exception {
        when(service.cadastrar(argThat(request ->
                "petr4".equals(request.getTicker()) && request.getMercado() == Mercado.BRASIL
        ))).thenReturn(response(1L, "PETR4", Mercado.BRASIL, Moeda.BRL));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"petr4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.matchesPattern(".*/acoes/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.nomeEmpresa").value("Empresa"))
                .andExpect(jsonPath("$.mercado").value("BRASIL"))
                .andExpect(jsonPath("$.moeda").value("BRL"))
                .andExpect(jsonPath("$.cotacaoAtual").value(100.123456))
                .andExpect(jsonPath("$.dataHoraCotacao").value("2026-08-20T15:30:00Z"));
    }

    @Test
    void createsAmericanActionWithUsd() throws Exception {
        when(service.cadastrar(argThat(request -> request.getMercado() == Mercado.EUA)))
                .thenReturn(response(2L, "AAPL", Mercado.EUA, Moeda.USD));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"AAPL\",\"mercado\":\"EUA\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.matchesPattern(".*/acoes/2")))
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.moeda").value("USD"));
    }

    @Test
    void rejectsMissingInvalidMarketAndClientSuppliedExternalFields() throws Exception {
        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.REQUEST_INVALIDO));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"PETR4\",\"mercado\":\"EUROPA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.REQUEST_INVALIDO));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\",\"cotacaoAtual\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.REQUEST_INVALIDO));

        verifyNoInteractions(service);
    }

    @Test
    void returnsStandardizedBusinessErrorsWithCodeAndDetails() throws Exception {
        when(service.cadastrar(argThat(request -> "DUPL3".equals(request.getTicker()))))
                .thenThrow(new ApiException(
                        HttpStatus.CONFLICT,
                        ErrorCodes.ACAO_DUPLICADA,
                        "Já existe uma ação cadastrada com este ticker e mercado",
                        Map.of("ticker", "DUPL3", "mercado", "BRASIL")
                ));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"DUPL3\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ACAO_DUPLICADA))
                .andExpect(jsonPath("$.details.ticker").value("DUPL3"))
                .andExpect(jsonPath("$.details.mercado").value("BRASIL"));
    }

    @Test
    void doesNotExposeOutOfScopeGetEndpoints() throws Exception {
        mockMvc.perform(get("/acoes"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/acoes/1"))
                .andExpect(status().isNotFound());
    }

    private AcaoResponse response(Long id, String ticker, Mercado market, Moeda currency) {
        return new AcaoResponse(
                id,
                ticker,
                "Empresa",
                market,
                currency,
                new BigDecimal("100.123456"),
                OffsetDateTime.parse("2026-08-20T15:30:00Z")
        );
    }
}
