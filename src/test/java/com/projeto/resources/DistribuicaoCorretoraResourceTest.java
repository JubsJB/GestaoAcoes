package com.projeto.resources;

import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.mappers.DistribuicaoCorretoraMapper;
import com.projeto.resources.exceptions.ResourceExceptionHandler;
import com.projeto.services.CalculadoraDistribuicaoCorretora.*;
import com.projeto.services.DistribuicaoPosicaoCorretoraService;
import com.projeto.services.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DistribuicaoCorretoraResourceTest {
    private static final String PATH = "/carteiras/1/posicoes/distribuicao-corretoras";
    private final DistribuicaoPosicaoCorretoraService service = mock(DistribuicaoPosicaoCorretoraService.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new DistribuicaoCorretoraResource(service,
                        new DistribuicaoCorretoraMapper()))
                .setControllerAdvice(new ResourceExceptionHandler()).build();
    }

    static Stream<String> exemplosDocumentados() throws Exception {
        var exemplos = new java.util.ArrayList<String>();
        for (String nome : List.of("reconciliada", "saldo-negativo-atual", "deficit-historico-regularizado",
                "corretora-zerada-com-deficit", "divergencia-reconciliacao")) {
            String recurso = "/contratos/distribuicao-corretoras/" + nome + ".json";
            try (var input = DistribuicaoCorretoraResourceTest.class.getResourceAsStream(recurso)) {
                assertNotNull(input, "Recurso de contrato ausente: " + recurso);
                exemplos.add(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertEquals(5, exemplos.size());
        return exemplos.stream();
    }

    @ParameterizedTest @MethodSource("exemplosDocumentados")
    void cincoExemplosPreservamSchemaNulosDiagnosticosEOrdenacao(String esperado) throws Exception {
        Distribuicao source = json.readValue(esperado, Distribuicao.class);
        when(service.consultar(1L)).thenReturn(source);
        String response = mvc.perform(get(PATH)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(json.readTree(esperado), json.readTree(response));
        verify(service).consultar(1L); verifyNoMoreInteractions(service);
        assertEquals(source, json.readValue(response, Distribuicao.class));
    }

    @Test void vazio200() throws Exception {
        when(service.consultar(1L)).thenReturn(new Distribuicao(1L, List.of()));
        mvc.perform(get(PATH)).andExpect(status().isOk())
                .andExpect(content().json("{\"carteiraId\":1,\"posicoes\":[]}"));
    }

    @Test void inexistente404Padronizado() throws Exception {
        when(service.consultar(1L)).thenThrow(new ObjectNotFoundException("Carteira nao encontrada"));
        mvc.perform(get(PATH)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(nullValue()))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.path").value(PATH));
    }

    @ParameterizedTest @ValueSource(strings = {"abc", "9223372036854775808"})
    void parametroInvalido400SemChamarServico(String id) throws Exception {
        mvc.perform(get("/carteiras/" + id + "/posicoes/distribuicao-corretoras"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));
        verifyNoInteractions(service);
    }

    @Test void replayInvalido409SemDistribuicaoParcial() throws Exception {
        when(service.consultar(1L)).thenThrow(new ApiException(HttpStatus.CONFLICT,
                ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE, "Historico inconsistente", Map.of("carteiraId", 1L)));
        mvc.perform(get(PATH)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HISTORICO_OPERACOES_INCONSISTENTE"))
                .andExpect(jsonPath("$.details.carteiraId").value(1))
                .andExpect(jsonPath("$.posicoes").doesNotExist());
    }

    @Test void falhaDeSerializacao500SeguroSemRetry() throws Exception {
        when(service.consultar(1L)).thenThrow(new CannotSerializeTransactionException("SQL SEGREDO senha TOKEN"));
        mvc.perform(get(PATH)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("FALHA_CONSULTA_DISTRIBUICAO_CORRETORAS"))
                .andExpect(jsonPath("$.message").value("Nao foi possivel consultar a distribuicao por corretora."))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(content().string(not(containsString("SEGREDO"))));
        verify(service).consultar(1L); verifyNoMoreInteractions(service);
    }

    @Test void decimalExatoAcimaDeDoubleENumeric19NaoViraString() throws Exception {
        BigDecimal q = new BigDecimal("999999999999999999999.123457");
        var p = new Posicao(1L, "AAPL", Mercado.EUA, Moeda.USD, q, q, true, null,
                List.of(new QuantidadeCorretora(null, "Sem corretora informada", q)),
                List.of(new DiagnosticoCorretora(null, "Sem corretora informada", q, false, false, null)));
        when(service.consultar(1L)).thenReturn(new Distribuicao(1L, List.of(p)));
        String response = mvc.perform(get(PATH)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("\"quantidadeConsolidada\":" + q.toPlainString()));
        assertTrue(response.contains("\"quantidadeLiquida\":" + q.toPlainString()));
        assertEquals(q, json.readValue(response, Distribuicao.class).posicoes().get(0).quantidadeConsolidada());
    }

    @Test void exemplosOpenApiCorrespondemAosCincoExemplosNormativos() throws Exception {
        var documented = exemplosDocumentados().map(json::readTree).toList();
        var published = Stream.of(DistribuicaoCorretoraExamples.EXAMPLE_1, DistribuicaoCorretoraExamples.EXAMPLE_2,
                DistribuicaoCorretoraExamples.EXAMPLE_3, DistribuicaoCorretoraExamples.EXAMPLE_4,
                DistribuicaoCorretoraExamples.EXAMPLE_5).map(json::readTree).toList();
        assertEquals(documented, published);
    }

    @Test void ordenacaoDoNucleoEhPreservadaSemOrdenarOuRecalcularNoController() throws Exception {
        var brasil = json.readValue(DistribuicaoCorretoraExamples.EXAMPLE_1, Distribuicao.class).posicoes().get(0);
        var eua = new Posicao(2L, "AAPL", Mercado.EUA, Moeda.USD, BigDecimal.ONE, BigDecimal.ONE,
                true, null, List.of(new QuantidadeCorretora(null, "Sem corretora informada", BigDecimal.ONE)),
                List.of(new DiagnosticoCorretora(null, "Sem corretora informada", BigDecimal.ONE, false, false, null)));
        var source = new Distribuicao(1L, List.of(brasil, eua));
        when(service.consultar(1L)).thenReturn(source);
        String response = mvc.perform(get(PATH)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(source, json.readValue(response, Distribuicao.class));
        mvc.perform(get(PATH)).andExpect(jsonPath("$.posicoes[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.posicoes[1].ticker").value("AAPL"))
                .andExpect(jsonPath("$.posicoes[0].corretoras[1].corretoraId").value(nullValue()));
    }
}
