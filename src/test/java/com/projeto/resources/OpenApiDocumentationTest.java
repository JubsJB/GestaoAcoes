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
            Map.entry("/carteiras/{carteiraId}/operacoes/sugestao-preco-venda", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/posicoes", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/posicoes/distribuicao-corretoras", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/atualizacoes-cotacoes", Set.of("post")),
            Map.entry("/carteiras/{carteiraId}/resultados-realizados", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/patrimonio", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/resumo", Set.of("get")),
            Map.entry("/carteiras/{carteiraId}/snapshots", Set.of("post")),
            Map.entry("/carteiras/{carteiraId}/evolucao-patrimonial", Set.of("get")),
            Map.entry("/operacoes", Set.of("get", "post")),
            Map.entry("/operacoes/previa-compra", Set.of("get")),
            Map.entry("/operacoes/{id}", Set.of("get"))
    );

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsBrokerDistributionWithRequiredFieldsAndFiveExamples() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode operation = document.path("paths")
                .path("/carteiras/{carteiraId}/posicoes/distribuicao-corretoras").path("get");
        assertThat(operation.has("requestBody")).isFalse();
        assertThat(operation.path("responses").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("200", "400", "404", "409", "500");
        assertThat(operation.at("/responses/200/content/application~1json/examples").size()).isEqualTo(5);
        JsonNode schemas = document.at("/components/schemas");
        for (String name : List.of("DistribuicaoCarteiraResponse", "PosicaoDistribuicaoResponse",
                "QuantidadeCorretoraResponse", "DiagnosticoCorretoraResponse", "PrimeiroDeficitResponse",
                "DiagnosticoReconciliacaoResponse")) {
            JsonNode schema = schemas.path(name);
            assertThat(schema.has("properties")).as(name).isTrue();
            Set<String> required = new HashSet<>();
            schema.path("required").forEach(field -> required.add(field.asText()));
            Set<String> properties = new HashSet<>();
            schema.path("properties").fieldNames().forEachRemaining(properties::add);
            assertThat(required).as(name).isEqualTo(properties);
        }
        assertThat(schemas.at("/PosicaoDistribuicaoResponse/properties/quantidadeConsolidada/type").asText())
                .isEqualTo("number");
        assertThat(schemas.at("/PrimeiroDeficitResponse/properties/dataOperacao/format").asText()).isEqualTo("date");
        assertThat(schemas.path("DiagnosticoReconciliacaoResponse").path("properties").has("corretoraId")).isFalse();
    }

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
    void documentsImplementedFunctionalPathsAndOperations() throws Exception {
        JsonNode paths = openApiDocument().path("paths");
        Map<String, Set<String>> functionalOperations = collectFunctionalOperations(paths);

        assertThat(functionalOperations).isEqualTo(EXPECTED_OPERATIONS);
        assertThat(functionalOperations).hasSize(EXPECTED_OPERATIONS.size());
        assertThat(functionalOperations.values().stream().mapToInt(Set::size).sum())
                .isEqualTo(EXPECTED_OPERATIONS.values().stream().mapToInt(Set::size).sum());
        assertThat(paths.path("/carteiras/{carteiraId}/atualizacoes-cotacoes").path("post").has("requestBody"))
                .isFalse();
    }

    @Test
    void documentsRepresentativeParametersRequestBodiesAndResponses() throws Exception {
        JsonNode paths = openApiDocument().path("paths");

        assertParameter(paths, "/corretoras/por-cnpj", "get", "cnpj", "query");
        assertParameter(paths, "/acoes/por-ticker", "get", "ticker", "query");
        assertParameter(paths, "/acoes/por-ticker", "get", "mercado", "query");
        assertParameter(paths, "/carteiras/{carteiraId}/posicoes", "get", "carteiraId", "path");
        assertParameter(paths, "/operacoes/{id}", "get", "id", "path");
        assertParameter(paths, "/operacoes/previa-compra", "get", "ticker", "query");
        assertParameter(paths, "/operacoes/previa-compra", "get", "mercado", "query");
        assertParameter(paths, "/operacoes/previa-compra", "get", "dataOperacao", "query");
        assertParameter(paths, "/carteiras/{carteiraId}/operacoes/sugestao-preco-venda", "get", "carteiraId", "path");

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
        assertThat(paths.path("/operacoes").path("post").path("responses").has("422")).isFalse();
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
        assertThat(schemas.has("PreviaPrecoCompraResponse")).isTrue();
        assertThat(schemas.has("SugestaoPrecoVendaResponse")).isTrue();
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
        assertThat(purchase.toString()).contains("precoUnitario").doesNotContain("ordemNoDia");
        assertThat(sale.toString()).contains("precoUnitario").doesNotContain("ordemNoDia");
        assertThat(sale.path("required").toString()).contains("precoUnitario");
        assertThat(purchase.path("additionalProperties").asBoolean(true)).isFalse();
        assertThat(sale.path("additionalProperties").asBoolean(true)).isFalse();
        assertThat(purchase.path("description").asText()).contains("informado pelo cliente", "COMPRA");
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
        for (String status : List.of("400", "404", "409")) {
            assertThat(responses.has(status)).isTrue();
        }
        assertThat(document.at("/paths/~1operacoes/post/description").asText()).contains("COMPRA");
    }

    @Test
    void documentsPurchasePreviewAndSaleSuggestionSemanticsAndResponses() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode preview = document.at("/paths/~1operacoes~1previa-compra/get");
        assertThat(preview.path("description").asText())
                .contains("fechamento histórico bruto", "data exata", "Consulta independente")
                .contains("exige precoUnitario manual");
        for (String description : List.of(
                preview.path("description").asText(),
                document.at("/components/schemas/PreviaPrecoCompraResponse/description").asText())) {
            assertThat(description)
                    .contains("informativa", "sugestão inicial editável", "sem efeitos colaterais")
                    .containsIgnoringCase("não vinculante")
                    .contains("preço final é informado pelo cliente", "independente da prévia", "não exige seu sucesso")
                    .contains("não consulta", "provider histórico")
                    .doesNotContain("consulta novamente o provider", "somente leitura");
        }
        for (String status : List.of("200", "400", "404", "422", "429", "502", "503", "504")) {
            assertThat(preview.path("responses").has(status)).isTrue();
        }
        assertThat(preview.at("/responses/200/content/*~1*/schema").toString())
                .contains("PreviaPrecoCompraResponse");

        JsonNode suggestion = document.at("/paths/~1carteiras~1{carteiraId}~1operacoes~1sugestao-preco-venda/get");
        assertThat(suggestion.path("description").asText())
                .contains("última COMPRA", "null", "livremente editável", "não representa preço médio", "recomendação financeira");
        assertThat(suggestion.path("responses").has("200")).isTrue();
        assertThat(suggestion.path("responses").has("400")).isTrue();
        assertThat(suggestion.path("responses").has("404")).isTrue();
        assertThat(suggestion.at("/responses/200/content/*~1*/schema").toString())
                .contains("SugestaoPrecoVendaResponse");
        assertThat(document.at("/components/schemas/SugestaoPrecoVendaResponse/properties/precoUnitarioSugerido/type")
                .toString()).contains("number", "null");
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
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/openapi-b2.json"), json);
        return objectMapper.readTree(json);
    }

    @Test
    void documentsBatchWithoutBodyAndWithOnlyApplicableGlobalResponses() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode operation = document.path("paths").path("/carteiras/{carteiraId}/atualizacoes-cotacoes").path("post");
        assertThat(operation.has("requestBody")).isFalse();
        assertThat(operation.path("parameters").size()).isEqualTo(1);
        JsonNode parameter = operation.path("parameters").get(0);
        assertThat(parameter.path("name").asText()).isEqualTo("carteiraId");
        assertThat(parameter.path("in").asText()).isEqualTo("path");
        assertThat(parameter.path("required").asBoolean()).isTrue();
        assertThat(parameter.at("/schema/type").asText()).isEqualTo("integer");
        assertThat(parameter.at("/schema/format").asText()).isEqualTo("int64");
        assertThat(operation.path("description").asText()).contains("{}", "400 REQUEST_INVALIDO", "sucesso parcial", "COOLDOWN_SUCESSO", "outra instância");
        assertThat(operation.path("responses").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("200", "400", "404", "409", "422", "500");
        assertThat(operation.at("/responses/200/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/AtualizacaoCotacoesCarteiraResponse");
        for (String code : List.of("400", "404", "409", "422", "500")) {
            assertThat(operation.path("responses").path(code).at("/content/application~1json/schema/$ref").asText())
                    .isEqualTo("#/components/schemas/StandardError");
        }
        assertThat(operation.at("/responses/409/description").asText()).contains("HISTORICO_OPERACOES_INCONSISTENTE");
        assertThat(operation.at("/responses/422/description").asText()).contains("CALCULO_POSICAO_FORA_DA_PRECISAO");
        JsonNode error = operation.at("/responses/500/content/application~1json/example");
        assertThat(error.path("status").asInt()).isEqualTo(500);
        assertThat(error.path("code").asText()).isEqualTo("FALHA_ATUALIZACAO_COTACOES");
        assertThat(error.path("message").asText()).isEqualTo("Não foi possível concluir o lote. Atualizações já confirmadas podem ter sido preservadas.");
        assertThat(error.path("details").size()).isZero();
    }

    @Test
    void documentsCompleteBatchSchemasEnumsCountersAndSafeNullableError() throws Exception {
        JsonNode schemas = openApiDocument().at("/components/schemas");
        Map<String, List<String>> fields = Map.of(
                "AtualizacaoCotacoesCarteiraResponse", List.of("carteiraId", "resumo", "resultados"),
                "ResumoAtualizacaoCotacoesResponse", List.of("atualizadas", "semNovaCotacao", "falhas"),
                "ResultadoAtualizacaoCotacaoResponse", List.of("acaoId", "ticker", "mercado", "resultado", "motivo", "cotacao", "erro"),
                "EstadoCotacaoResponse", List.of("cotacaoAtual", "referenciaMercadoCotacao", "cotacaoObtidaEm", "dataHoraCotacao"),
                "ReferenciaMercadoCotacaoResponse", List.of("tipo", "instante", "data"),
                "ErroAtualizacaoCotacaoResponse", List.of("code", "message"));
        for (var entry : fields.entrySet()) {
            JsonNode schema = schemas.path(entry.getKey());
            assertThat(schema.path("properties").fieldNames()).toIterable().containsExactlyInAnyOrderElementsOf(entry.getValue());
            assertThat(schema.path("required")).extracting(JsonNode::asText).containsExactlyInAnyOrderElementsOf(entry.getValue());
            schema.path("properties").forEach(property -> assertThat(property.path("description").asText()).isNotBlank());
        }
        JsonNode batch = schemas.path("AtualizacaoCotacoesCarteiraResponse").path("properties");
        assertThat(batch.at("/carteiraId/type").asText()).isEqualTo("integer");
        assertThat(batch.at("/resumo/$ref").asText()).endsWith("/ResumoAtualizacaoCotacoesResponse");
        assertThat(batch.at("/resultados/type").asText()).isEqualTo("array");
        assertThat(batch.at("/resultados/items/$ref").asText()).endsWith("/ResultadoAtualizacaoCotacaoResponse");
        schemas.path("ResumoAtualizacaoCotacoesResponse").path("properties").forEach(counter -> {
            assertThat(counter.path("type").asText()).isEqualTo("integer");
            assertThat(counter.path("format").asText()).isEqualTo("int64");
            assertThat(counter.path("minimum").asInt(-1)).isZero();
        });
        JsonNode item = schemas.path("ResultadoAtualizacaoCotacaoResponse").path("properties");
        assertThat(item.at("/resultado/enum")).extracting(JsonNode::asText)
                .containsExactlyInAnyOrder("ATUALIZADA", "SEM_NOVA_COTACAO", "FALHA");
        assertThat(item.at("/motivo/description").asText()).contains("REFERENCIA_INICIAL_ESTABELECIDA", "REFERENCIA_POSTERIOR", "MESMA_OBSERVACAO", "REFERENCIA_ANTERIOR", "REUSO", "COOLDOWN_SUCESSO", "COOLDOWN_FALHA");
        assertThat(item.path("motivo").has("enum")).isFalse();
        assertThat(item.at("/cotacao/$ref").asText()).endsWith("/EstadoCotacaoResponse");
        JsonNode errorAlternatives = item.at("/erro/anyOf");
        assertThat(errorAlternatives.size()).isEqualTo(2);
        assertThat(errorAlternatives.get(0).path("$ref").asText()).endsWith("/ErroAtualizacaoCotacaoResponse");
        assertThat(errorAlternatives.get(1).path("type").asText()).isEqualTo("null");
        assertThat(item.path("erro").has("$ref")).isFalse();
        JsonNode reference = schemas.path("ReferenciaMercadoCotacaoResponse");
        assertThat(reference.at("/properties/tipo/enum")).extracting(JsonNode::asText)
                .containsExactlyInAnyOrder("DATA", "INSTANTE", "DESCONHECIDA");
        assertNullableString(reference.at("/properties/data"), "date");
        assertNullableString(reference.at("/properties/instante"), "date-time");
        assertThat(reference.path("description").asText()).contains("DATA preenche somente data", "INSTANTE somente instante", "DESCONHECIDA");
    }

    @Test
    void preservesActionPositionFieldsAndDocumentsSharedTemporalSemanticsAndPatch() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode schemas = document.at("/components/schemas");
        Map<String, List<String>> legacy = Map.of(
                "AcaoResponse", List.of("id", "ticker", "nomeEmpresa", "mercado", "moeda", "cotacaoAtual", "dataHoraCotacao"),
                "PosicaoResponse", List.of("acaoId", "ticker", "nomeEmpresa", "mercado", "moeda", "quantidadeAtual", "precoMedio", "custoPosicao", "cotacaoAtual", "dataHoraCotacao", "valorAtualPosicao", "resultadoNaoRealizado", "rentabilidadePercentual"));
        for (var entry : legacy.entrySet()) {
            assertThat(schemas.path(entry.getKey()).path("properties").fieldNames()).toIterable()
                    .containsAll(entry.getValue()).hasSize(entry.getValue().size() + 2);
        }
        for (String name : List.of("AcaoResponse", "PosicaoResponse", "EstadoCotacaoResponse")) {
            JsonNode properties = schemas.path(name).path("properties");
            assertThat(properties.at("/cotacaoAtual/type").asText()).isEqualTo("number");
            assertThat(properties.path("cotacaoAtual").has("format")).isFalse();
            assertThat(properties.at("/dataHoraCotacao/format").asText()).isEqualTo("date-time");
            assertThat(properties.at("/dataHoraCotacao/description").asText()).contains("legado", "compatibilidade");
            assertThat(properties.at("/referenciaMercadoCotacao/$ref").asText()).endsWith("/ReferenciaMercadoCotacaoResponse");
            assertNullableString(properties.path("cotacaoObtidaEm"), "date-time");
            assertThat(properties.at("/cotacaoObtidaEm/description").asText()).contains("nulo no legado", "Não é referência de mercado", "monotonicidade global");
        }
        JsonNode patch = document.path("paths").path("/acoes/{id}/cotacao").path("patch");
        assertThat(patch.has("requestBody")).isFalse();
        assertThat(patch.path("description").asText()).contains("DESCONHECIDA", "DATA e INSTANTE são incomparáveis", "Sucesso HTTP não garante nova cotação");
        assertThat(patch.at("/responses/422/description").asText()).contains("CONFLITO_NA_MESMA_REFERENCIA", "TIPO_REFERENCIA_INCOMPATIVEL", "REFERENCIA_TEMPORAL_INVALIDA");
        for (String code : List.of("200", "400", "404", "409", "422", "429", "502", "503", "504")) {
            assertThat(patch.path("responses").has(code)).isTrue();
        }
        assertThat(patch.at("/responses/200/content/*~1*/schema/$ref").asText()).endsWith("/AcaoResponse");
        assertThat(schemas.fieldNames()).toIterable().doesNotContain("ResultadoAtualizacaoCotacao", "SnapshotCotacao", "CotacaoCandidata", "Decisao", "Acao", "HistoricoCotacao", "ApiException", "Throwable", "EstadoCotacao");
    }

    @Test
    void batchExamplesConformToGeneratedSchemasAndTemporalShapes() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode schemas = document.at("/components/schemas");
        JsonNode examples = document.path("paths").path("/carteiras/{carteiraId}/atualizacoes-cotacoes")
                .at("/post/responses/200/content/application~1json/examples");
        assertThat(examples.fieldNames()).toIterable().containsExactlyInAnyOrder("atualizada", "semNovaCotacao", "parcial");
        for (JsonNode example : examples) {
            JsonNode value = example.path("value");
            assertExample(value, schemas.path("AtualizacaoCotacoesCarteiraResponse"), schemas);
            Map<String, Long> counts = new java.util.HashMap<>(Map.of("ATUALIZADA", 0L, "SEM_NOVA_COTACAO", 0L, "FALHA", 0L));
            long previousId = -1;
            for (JsonNode item : value.path("resultados")) {
                String result = item.path("resultado").asText();
                counts.compute(result, (key, count) -> count + 1);
                assertThat(item.path("acaoId").asLong()).isGreaterThan(previousId);
                previousId = item.path("acaoId").asLong();
                assertThat(item.path("erro").isNull()).isEqualTo(!result.equals("FALHA"));
                JsonNode reference = item.at("/cotacao/referenciaMercadoCotacao");
                String type = reference.path("tipo").asText();
                assertThat(reference.path("data").isNull()).isEqualTo(!type.equals("DATA"));
                assertThat(reference.path("instante").isNull()).isEqualTo(!type.equals("INSTANTE"));
            }
            assertThat(value.at("/resumo/atualizadas").asLong()).isEqualTo(counts.get("ATUALIZADA"));
            assertThat(value.at("/resumo/semNovaCotacao").asLong()).isEqualTo(counts.get("SEM_NOVA_COTACAO"));
            assertThat(value.at("/resumo/falhas").asLong()).isEqualTo(counts.get("FALHA"));
        }
    }

    private void assertNullableString(JsonNode property, String format) {
        assertThat(property.path("type")).extracting(JsonNode::asText).containsExactlyInAnyOrder("string", "null");
        assertThat(property.path("format").asText()).isEqualTo(format);
    }

    /** Valida as formas usadas nestes exemplos contra o documento gerado, incluindo $ref e null. */
    private void assertExample(JsonNode value, JsonNode schema, JsonNode schemas) {
        if (schema.has("$ref")) {
            assertExample(value, schemas.path(schema.path("$ref").asText().substring("#/components/schemas/".length())), schemas);
            return;
        }
        if (schema.has("anyOf")) {
            JsonNode alternative = schema.path("anyOf").get(value.isNull() ? 1 : 0);
            assertExample(value, alternative, schemas);
            return;
        }
        Set<String> types = new HashSet<>();
        if (schema.path("type").isArray()) schema.path("type").forEach(type -> types.add(type.asText()));
        else types.add(schema.path("type").asText());
        if (value.isNull()) { assertThat(types).contains("null"); return; }
        if (value.isObject()) {
            assertThat(types).contains("object");
            for (JsonNode required : schema.path("required")) assertThat(value.has(required.asText())).isTrue();
            value.fields().forEachRemaining(field -> {
                assertThat(schema.path("properties").has(field.getKey())).isTrue();
                assertExample(field.getValue(), schema.path("properties").path(field.getKey()), schemas);
            });
        } else if (value.isArray()) {
            assertThat(types).contains("array");
            value.forEach(item -> assertExample(item, schema.path("items"), schemas));
        } else if (value.isNumber()) {
            assertThat(types).contains(value.isIntegralNumber() && types.contains("integer") ? "integer" : "number");
        } else {
            assertThat(types).contains("string");
            if (schema.path("format").asText().equals("date")) java.time.LocalDate.parse(value.asText());
            if (schema.path("format").asText().equals("date-time")) java.time.OffsetDateTime.parse(value.asText());
            if (schema.has("enum")) assertThat(schema.path("enum")).extracting(JsonNode::asText).contains(value.asText());
        }
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
