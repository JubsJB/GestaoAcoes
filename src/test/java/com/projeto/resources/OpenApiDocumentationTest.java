package com.projeto.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.GestaoacoesApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete", "options", "head", "trace"
    );

    private static final Map<String, Set<String>> EXPECTED_OPERATIONS = Map.ofEntries(
            Map.entry("/corretoras", Set.of("get", "post")),
            Map.entry("/corretoras/{id}", Set.of("get")),
            Map.entry("/corretoras/por-cnpj", Set.of("get")),
            Map.entry("/acoes", Set.of("get", "post")),
            Map.entry("/acoes/{id}", Set.of("get")),
            Map.entry("/acoes/por-ticker", Set.of("get")),
            Map.entry("/acoes/{id}/cotacao", Set.of("patch")),
            Map.entry("/carteiras", Set.of("get", "post")),
            Map.entry("/carteiras/{id}", Set.of("get", "patch", "delete")),
            Map.entry("/carteiras/{carteiraId}/operacoes", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/posicoes", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/resultados-realizados", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/patrimonio", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/resumo", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/snapshots", Set.of("post")),
            Map.entry("/carteiras/{carteiraId}/evolucao-patrimonial", Set.of("get")),
            Map.entry("/operacoes", Set.of("get", "post")),
            Map.entry("/operacoes/{id}", Set.of("get"))
    );

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exposesParseableOpenApiJsonWithExpectedMetadataAndTags() throws Exception {
        JsonNode document = openApiDocument();

        assertThat(document.path("openapi").asText()).startsWith("3.");
        assertThat(document.at("/info/title").asText())
                .isEqualTo("Sistema de Gestão e Controle de Carteira de Investimentos API");
        assertThat(document.at("/info/description").asText())
                .isEqualTo("API REST para gerenciamento de corretoras, ações, carteiras, operações e indicadores de uma carteira de investimentos.");
        assertThat(document.at("/info/version").asText()).isEqualTo("0.0.1-SNAPSHOT");

        Set<String> tags = new HashSet<>();
        document.path("tags").forEach(tag -> tags.add(tag.path("name").asText()));
        assertThat(tags).containsExactlyInAnyOrder(
                "Corretoras", "Ações", "Carteiras", "Operações", "Indicadores da Carteira"
        );
    }

    @Test
    void documentsExactlyEighteenFunctionalPathsAndTwentyFourOperations() throws Exception {
        JsonNode paths = openApiDocument().path("paths");
        Map<String, Set<String>> functionalOperations = collectFunctionalOperations(paths);

        assertThat(functionalOperations).isEqualTo(EXPECTED_OPERATIONS);
        assertThat(functionalOperations).hasSize(18);
        assertThat(functionalOperations.values().stream().mapToInt(Set::size).sum()).isEqualTo(24);
    }

    @Test
    void documentsRepresentativeParametersRequestBodiesAndResponses() throws Exception {
        JsonNode paths = openApiDocument().path("paths");

        assertParameter(paths, "/corretoras/por-cnpj", "get", "cnpj", "query");
        assertParameter(paths, "/acoes/por-ticker", "get", "ticker", "query");
        assertParameter(paths, "/acoes/por-ticker", "get", "mercado", "query");
        assertParameter(paths, "/carteiras/{carteiraId}/posicoes", "get", "carteiraId", "path");
        assertParameter(paths, "/operacoes/{id}", "get", "id", "path");

        assertThat(paths.path("/corretoras").path("post").path("requestBody").path("content")
                .path("application/json").path("schema").path("$ref").asText())
                .endsWith("/CorretoraCreateRequest");
        assertThat(paths.path("/acoes").path("post").path("requestBody").path("content")
                .path("application/json").path("schema").path("$ref").asText())
                .endsWith("/AcaoCreateRequest");
        assertThat(paths.path("/carteiras").path("post").path("requestBody").path("content")
                .path("application/json").path("schema").path("$ref").asText())
                .endsWith("/CarteiraCreateRequest");
        assertThat(paths.path("/operacoes").path("post").path("requestBody").path("content")
                .path("application/json").path("schema").path("$ref").asText())
                .endsWith("/OperacaoCreateRequest");

        assertThat(paths.path("/corretoras").path("post").path("responses").has("201")).isTrue();
        assertThat(paths.path("/acoes/{id}/cotacao").path("patch").path("responses").has("504")).isTrue();
        assertThat(paths.path("/carteiras/{id}").path("delete").path("responses").has("204")).isTrue();
        assertThat(paths.path("/operacoes").path("post").path("responses").has("422")).isTrue();
    }

    @Test
    void exposesReusablePublicSchemasIncludingStandardError() throws Exception {
        JsonNode schemas = openApiDocument().at("/components/schemas");

        assertThat(schemas.has("StandardError")).isTrue();
        assertThat(schemas.path("StandardError").path("properties").fieldNames())
                .toIterable()
                .contains("timeStamp", "status", "error", "message", "path", "code", "details");
        assertThat(schemas.has("CorretoraResponse")).isTrue();
        assertThat(schemas.has("AcaoResponse")).isTrue();
        assertThat(schemas.has("OperacaoResponse")).isTrue();
        assertThat(schemas.has("EvolucaoPatrimonialResponse")).isTrue();
    }

    @Test
    void documentsDiscriminatedOperationContractAndHistoricalErrors() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode schemas = document.at("/components/schemas");
        JsonNode base = schemas.path("OperacaoCreateRequest");
        assertThat(base.path("oneOf").isArray()).isTrue();
        assertThat(base.path("oneOf").size()).isEqualTo(2);
        assertThat(base.at("/discriminator/propertyName").asText()).isEqualTo("tipo");

        JsonNode purchase = schemas.path("OperacaoCompraCreateRequest");
        JsonNode sale = schemas.path("OperacaoVendaCreateRequest");
        assertThat(purchase.toString()).doesNotContain("precoUnitario", "ordemNoDia");
        assertThat(sale.toString()).contains("precoUnitario").doesNotContain("ordemNoDia");
        assertThat(sale.path("required").toString()).contains("precoUnitario");
        assertThat(purchase.path("additionalProperties").asBoolean(true)).isFalse();
        assertThat(sale.path("additionalProperties").asBoolean(true)).isFalse();
        assertThat(purchase.path("description").asText()).contains("fechamento histórico", "COMPRA");
        assertThat(sale.path("description").asText())
                .contains("informado pelo cliente", "nenhum provider histórico")
                .doesNotContain("consulta o fechamento histórico");
        assertThat(sale.at("/allOf/1/properties/precoUnitario/description").asText())
                .contains("informado", "VENDA");

        JsonNode response = schemas.path("OperacaoResponse").path("properties");
        assertThat(response.has("precoUnitario")).isTrue();
        assertThat(response.has("ordemNoDia")).isTrue();
        assertThat(response.has("valorTotal")).isTrue();
        JsonNode responses = document.at("/paths/~1operacoes/post/responses");
        for (String status : List.of("404", "422", "429", "502", "503", "504")) {
            assertThat(responses.has(status)).isTrue();
        }
        assertThat(document.at("/paths/~1operacoes/post/description").asText()).contains("COMPRA");
    }

    @Test
    void doesNotDeclareFictitiousSecurityVersioningOrSecrets() throws Exception {
        JsonNode document = openApiDocument();
        String serialized = objectMapper.writeValueAsString(document).toLowerCase();

        assertThat(document.at("/components/securitySchemes").isMissingNode()).isTrue();
        assertThat(document.path("security").isMissingNode()).isTrue();
        assertThat(document.path("paths").fieldNames()).toIterable().noneMatch(path -> path.startsWith("/v1"));
        assertThat(serialized)
                .doesNotContain("brapi_api_key")
                .doesNotContain("alpha_vantage_api_key")
                .doesNotContain("spring_datasource_password")
                .doesNotContain("datasource.password")
                .doesNotContain("sqlstate")
                .doesNotContain("stacktrace");
    }

    @Test
    void exposesOpenApiYamlAtDefaultPath() throws Exception {
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("openapi:")));
    }

    @Test
    void exposesSwaggerUiEntryAndEffectiveResourceWithoutInspectingHtml() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/swagger-ui/index.html"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    private JsonNode openApiDocument() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json);
    }

    private Map<String, Set<String>> collectFunctionalOperations(JsonNode paths) {
        var operations = new java.util.LinkedHashMap<String, Set<String>>();
        paths.fields().forEachRemaining(path -> {
            Set<String> methods = new HashSet<>();
            path.getValue().fieldNames().forEachRemaining(method -> {
                if (HTTP_METHODS.contains(method)) {
                    methods.add(method);
                }
            });
            operations.put(path.getKey(), Set.copyOf(methods));
        });
        return Map.copyOf(operations);
    }

    private void assertParameter(JsonNode paths, String path, String method, String name, String location) {
        Iterator<JsonNode> parameters = paths.path(path).path(method).path("parameters").elements();
        boolean found = false;
        while (parameters.hasNext()) {
            JsonNode parameter = parameters.next();
            if (name.equals(parameter.path("name").asText()) && location.equals(parameter.path("in").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).as("parameter %s in %s %s", name, method, path).isTrue();
    }
}
