package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.dto.AcaoCreateRequest;
import com.projeto.dto.AcaoResponse;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.services.AcaoService;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    void listsActionsWithCompletePersistedDataInServiceOrder() throws Exception {
        when(service.listar()).thenReturn(List.of(
                response(
                        1L,
                        "PETR4",
                        "Petróleo Brasileiro S.A.",
                        Mercado.BRASIL,
                        Moeda.BRL,
                        new BigDecimal("32.123456"),
                        OffsetDateTime.parse("2026-08-19T18:45:00Z")
                ),
                response(
                        2L,
                        "AAPL",
                        "Apple Inc.",
                        Mercado.EUA,
                        Moeda.USD,
                        new BigDecimal("224.410000"),
                        OffsetDateTime.parse("2026-08-20T15:30:00Z")
                )
        ));

        mockMvc.perform(get("/acoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[0].nomeEmpresa").value("Petróleo Brasileiro S.A."))
                .andExpect(jsonPath("$[0].mercado").value("BRASIL"))
                .andExpect(jsonPath("$[0].moeda").value("BRL"))
                .andExpect(jsonPath("$[0].cotacaoAtual").value(32.123456))
                .andExpect(jsonPath("$[0].dataHoraCotacao").value("2026-08-19T18:45:00Z"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].ticker").value("AAPL"))
                .andExpect(jsonPath("$[1].nomeEmpresa").value("Apple Inc."))
                .andExpect(jsonPath("$[1].mercado").value("EUA"))
                .andExpect(jsonPath("$[1].moeda").value("USD"))
                .andExpect(jsonPath("$[1].cotacaoAtual").value(224.410000))
                .andExpect(jsonPath("$[1].dataHoraCotacao").value("2026-08-20T15:30:00Z"));

        verify(service).listar();
    }

    @Test
    void returnsEmptyArrayWhenNoActionIsPersisted() throws Exception {
        when(service.listar()).thenReturn(List.of());

        mockMvc.perform(get("/acoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(service).listar();
    }

    @Test
    void returnsPersistedActionByIdWithCompleteResponse() throws Exception {
        when(service.buscarPorId(7L)).thenReturn(response(
                7L,
                "MSFT",
                "Microsoft Corporation",
                Mercado.EUA,
                Moeda.USD,
                new BigDecimal("501.250000"),
                OffsetDateTime.parse("2026-08-20T15:30:00Z")
        ));

        mockMvc.perform(get("/acoes/{id}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.ticker").value("MSFT"))
                .andExpect(jsonPath("$.nomeEmpresa").value("Microsoft Corporation"))
                .andExpect(jsonPath("$.mercado").value("EUA"))
                .andExpect(jsonPath("$.moeda").value("USD"))
                .andExpect(jsonPath("$.cotacaoAtual").value(501.250000))
                .andExpect(jsonPath("$.dataHoraCotacao").value("2026-08-20T15:30:00Z"));

        verify(service).buscarPorId(7L);
    }

    @Test
    void returnsStandardNotFoundErrorForMissingActionId() throws Exception {
        when(service.buscarPorId(999999L)).thenThrow(
                new ObjectNotFoundException("Ação não encontrada para o id: 999999")
        );

        mockMvc.perform(get("/acoes/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ação não encontrada para o id: 999999"))
                .andExpect(jsonPath("$.path").value("/acoes/999999"))
                .andExpect(jsonPath("$.code").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.details").isEmpty());

        verify(service).buscarPorId(999999L);
    }

    @Test
    void patchSemBodyAtualizaBrasilERetornaDtoCompletoSemLocation() throws Exception {
        when(service.atualizarCotacao(1L)).thenReturn(response(1L, "PETR4", Mercado.BRASIL, Moeda.BRL));

        mockMvc.perform(patch("/acoes/{id}/cotacao", 1L))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.nomeEmpresa").value("Empresa"))
                .andExpect(jsonPath("$.mercado").value("BRASIL"))
                .andExpect(jsonPath("$.moeda").value("BRL"))
                .andExpect(jsonPath("$.cotacaoAtual").value(100.123456))
                .andExpect(jsonPath("$.dataHoraCotacao").value("2026-08-20T15:30:00Z"));

        verify(service).atualizarCotacao(1L);
    }

    @Test
    void patchComBodyVazioAtualizaEua() throws Exception {
        when(service.atualizarCotacao(2L)).thenReturn(response(2L, "AAPL", Mercado.EUA, Moeda.USD));

        mockMvc.perform(patch("/acoes/{id}/cotacao", 2L).content(""))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.mercado").value("EUA"))
                .andExpect(jsonPath("$.moeda").value("USD"));

        verify(service).atualizarCotacao(2L);
    }

    @Test
    void patchRejeitaQualquerBodyNaoVazioSemChamarService() throws Exception {
        for (String body : List.of("{\"cotacaoAtual\":1}", "{invalido", "texto")) {
            mockMvc.perform(patch("/acoes/{id}/cotacao", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCodes.REQUEST_INVALIDO));
        }

        verifyNoInteractions(service);
    }

    @Test
    void patchPropagaNotFoundEConflitoCanonicoPadronizados() throws Exception {
        when(service.atualizarCotacao(404L)).thenThrow(
                new ObjectNotFoundException("Ação não encontrada para o id: 404"));
        mockMvc.perform(patch("/acoes/{id}/cotacao", 404L))
                .andExpect(status().isNotFound());

        when(service.atualizarCotacao(1L)).thenThrow(new ApiException(
                HttpStatus.CONFLICT,
                ErrorCodes.TICKER_CANONICO_DIVERGENTE,
                "Ticker divergente",
                Map.of(
                        "tickerPersistido", "PETR4",
                        "tickerCanonicoRetornado", "NEW3",
                        "acaoId", 1L,
                        "cotacaoPreservada", true
                )
        ));
        mockMvc.perform(patch("/acoes/{id}/cotacao", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.TICKER_CANONICO_DIVERGENTE))
                .andExpect(jsonPath("$.details.tickerPersistido").value("PETR4"))
                .andExpect(jsonPath("$.details.cotacaoPreservada").value(true));
    }

    @Test
    void patchPreservaDetailsDeFalhaExterna() throws Exception {
        when(service.atualizarCotacao(1L)).thenThrow(new ApiException(
                HttpStatus.GATEWAY_TIMEOUT,
                ErrorCodes.SERVICO_EXTERNO_TIMEOUT,
                "Timeout",
                Map.of(
                        "acaoId", 1L,
                        "cotacaoPreservada", true,
                        "ultimaCotacaoValida", new BigDecimal("30.000000"),
                        "dataHoraUltimaCotacao", "2026-08-19T15:30:00Z"
                )
        ));

        mockMvc.perform(patch("/acoes/{id}/cotacao", 1L))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value(ErrorCodes.SERVICO_EXTERNO_TIMEOUT))
                .andExpect(jsonPath("$.details.acaoId").value(1))
                .andExpect(jsonPath("$.details.cotacaoPreservada").value(true))
                .andExpect(jsonPath("$.details.ultimaCotacaoValida").value(30.000000));
    }

    private AcaoResponse response(Long id, String ticker, Mercado market, Moeda currency) {
        return response(
                id,
                ticker,
                "Empresa",
                market,
                currency,
                new BigDecimal("100.123456"),
                OffsetDateTime.parse("2026-08-20T15:30:00Z")
        );
    }

    private AcaoResponse response(
            Long id,
            String ticker,
            String companyName,
            Mercado market,
            Moeda currency,
            BigDecimal quote,
            OffsetDateTime timestamp
    ) {
        return new AcaoResponse(
                id,
                ticker,
                companyName,
                market,
                currency,
                quote,
                timestamp
        );
    }
}
