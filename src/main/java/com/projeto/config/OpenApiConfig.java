package com.projeto.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import java.util.Set;
import java.util.Map;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer segurancaPorOperacao() {
        return document -> document.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            Set<String> businessResponses = Set.copyOf(operation.getResponses().keySet());
            boolean publicOperation = (method == PathItem.HttpMethod.GET && path.equals("/auth/csrf"))
                    || (method == PathItem.HttpMethod.POST && (path.equals("/auth/login") || path.equals("/auth/redefinicoes/confirmar")));
            operation.setSecurity(publicOperation ? List.of() : List.of(new SecurityRequirement().addList("cookieAuth")));
            if (!publicOperation) {
                boolean adminOnly = path.startsWith("/usuarios") || (method != PathItem.HttpMethod.GET && !path.startsWith("/auth/"));
                operation.addExtension("x-roles", adminOnly ? List.of("ADMIN") : List.of("ADMIN", "USER"));
            }
            if (method != PathItem.HttpMethod.GET && method != PathItem.HttpMethod.HEAD && method != PathItem.HttpMethod.OPTIONS) {
                operation.addParametersItem(new Parameter().in("header").name("X-XSRF-TOKEN").required(true)
                        .schema(new StringSchema()).description("Obter GET /auth/csrf; enviar o valor do cookie XSRF-TOKEN neste header, inclusive em login e confirmação pública. Login/logout invalidam CSRF anterior: obter novamente antes da próxima escrita. Ausente/inválido: 403 CSRF_INVALIDO, podendo preceder 401. Não repetir escrita automaticamente."));
            }
            ApiResponses responses = operation.getResponses();
            if (!publicOperation) {
                responses.putIfAbsent("401", erro("Autenticação necessária: AUTENTICACAO_NECESSARIA ou SESSAO_EXPIRADA; cookie ausente/inválido, revogado ou prazo atingido."));
                responses.putIfAbsent("403", erro("ACESSO_NEGADO por papel insuficiente; em mutações, CSRF_INVALIDO pode preceder autenticação."));
                responses.putIfAbsent("503", erro("SEGURANCA_INDISPONIVEL: acesso negado por indisponibilidade, sem expor detalhes internos."));
            }
            if (path.startsWith("/auth/") || path.startsWith("/usuarios")) {
                if (method != PathItem.HttpMethod.GET) responses.putIfAbsent("400", erro("Validação inválida, sem revelar existência/estado de conta no fluxo público."));
                if (!path.equals("/auth/csrf")) responses.putIfAbsent("503", erro("SEGURANCA_INDISPONIVEL; resultado não confirmado, sem repetição automática de comando."));
                if (method == PathItem.HttpMethod.POST && (path.equals("/auth/login") || path.equals("/auth/redefinicoes/confirmar"))) {
                    responses.putIfAbsent("403", erro("CSRF_INVALIDO: proteção obrigatória mesmo sem autenticação."));
                    responses.putIfAbsent("429", erro("Limite de quinze minutos excedido; aguardar Retry-After. Login: cinco falhas username+IP/cinquenta por IP; confirmação: dez requisições username+IP/cinquenta por IP.")
                            .addHeaderObject("Retry-After", new Header().description("Segundos até nova tentativa permitida").schema(new IntegerSchema().minimum(java.math.BigDecimal.ONE))));
                }
                if (path.equals("/auth/login")) responses.putIfAbsent("401", erro("AUTENTICACAO_NECESSARIA: credenciais inválidas, sem enumerar contas."));
                if (path.equals("/auth/redefinicoes/confirmar")) responses.put("400", erro("REDEFINICAO_INVALIDA para identidade/código incorreto, expirado, revogado ou usado, sem distinguir a causa; SENHA_INVALIDA para política/confirmação."));
                if (path.startsWith("/usuarios")) {
                    if (!path.equals("/usuarios")) responses.putIfAbsent("404", erro("Usuário não encontrado."));
                    if (method != PathItem.HttpMethod.GET) responses.putIfAbsent("409", erro(path.endsWith("/perfil") || path.endsWith("/situacao")
                            ? "VERSAO_USUARIO_OBSOLETA ou ULTIMO_ADMIN_ATIVO; nenhuma mudança efetivada."
                            : "USUARIO_DUPLICADO no cadastro; CONFLITO_VERSAO ou USUARIO_BLOQUEADO na emissão."));
                }
                boolean noContent = path.equals("/auth/csrf") || path.equals("/auth/logout") || path.equals("/auth/redefinicoes/confirmar");
                if (noContent) {
                    responses.remove("200"); responses.put("204", new ApiResponse().description("Concluído sem corpo; Cache-Control: no-store."));
                }
                if (path.equals("/usuarios") && method == PathItem.HttpMethod.POST) {
                    ApiResponse created = responses.remove("200");
                    if (created == null) created = responses.get("201");
                    responses.put("201", created.description("Conta sem senha e autorização INICIAL exibida uma única vez; Cache-Control: no-store.")
                            .addHeaderObject("Location", new Header().description("URI relativa /usuarios/{id}").schema(new StringSchema())));
                }
                responses.forEach((status, response) -> {
                    if (status.startsWith("2")) response.addHeaderObject("Cache-Control", new Header().schema(new StringSchema()).description("no-store"));
                });
                if (path.equals("/auth/login")) responses.get("200").addHeaderObject("Set-Cookie", new Header().schema(new StringSchema())
                        .description("GESTAOACOES_AUTH gerenciado pelo navegador: HttpOnly; SameSite=Lax; Path=/; Secure em produção; sem Domain. JWT nunca no JSON. CSRF anterior invalidado: obter GET /auth/csrf novamente."));
                if (path.equals("/auth/login")) responses.keySet().retainAll(Set.of("200", "400", "401", "403", "429", "503"));
                else if (path.equals("/auth/redefinicoes/confirmar")) responses.keySet().retainAll(Set.of("204", "400", "403", "429", "503"));
                else if (path.equals("/auth/csrf")) responses.keySet().retainAll(Set.of("204", "400"));
                else if (path.startsWith("/auth/")) responses.keySet().retainAll(Set.of("200", "204", "400", "401", "403", "503"));
                else {
                    responses.keySet().retainAll(Set.of("200", "201", "400", "401", "403", "404", "409", "503"));
                    if (method == PathItem.HttpMethod.GET) responses.remove("409");
                    if (method == PathItem.HttpMethod.GET && path.equals("/usuarios")) responses.remove("404");
                }
            }
            responses.forEach((status, response) -> {
                // Não substituir exemplos/erros financeiros preexistentes; enriquecer só segurança adicionada.
                if ((status.startsWith("4") || status.startsWith("5"))
                        && (path.startsWith("/auth/") || path.startsWith("/usuarios") || !businessResponses.contains(status))) {
                    int value = Integer.parseInt(status);
                    String code = switch (value) {
                        case 400 -> path.equals("/auth/redefinicoes/confirmar") ? "REDEFINICAO_INVALIDA" : path.equals("/auth/csrf") ? "HTTPS_OBRIGATORIO" : "REQUEST_INVALIDO";
                        case 401 -> "AUTENTICACAO_NECESSARIA";
                        case 403 -> publicOperation ? "CSRF_INVALIDO" : "ACESSO_NEGADO";
                        case 404 -> "USUARIO_NAO_ENCONTRADO";
                        case 409 -> path.equals("/usuarios") ? "USUARIO_DUPLICADO" : path.endsWith("/redefinicoes") ? "CONFLITO_VERSAO" : "ULTIMO_ADMIN_ATIVO";
                        case 429 -> path.equals("/auth/login") ? "LIMITE_LOGIN_EXCEDIDO" : "LIMITE_REDEFINICAO_EXCEDIDO";
                        case 503 -> "SEGURANCA_INDISPONIVEL";
                        default -> "REQUEST_INVALIDO";
                    };
                    response.setContent(new Content().addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/StandardError"))
                            .example(Map.of("timeStamp", 0L, "status", value, "error", org.springframework.http.HttpStatus.valueOf(value).getReasonPhrase(),
                                    "message", "Requisição recusada.", "path", path, "code", code, "details", Map.of()))));
                }
            });
        }));
    }

    private static ApiResponse erro(String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/StandardError"))));
    }

    @Bean
    public OpenApiCustomizer credenciaisSomenteNosContratosPermitidos() {
        return document -> {
            var schemas = document.getComponents().getSchemas();
            for (String name : List.of("LoginRequest", "ConfirmacaoRequest")) {
                Schema<?> schema = schemas.get(name);
                if (schema != null && schema.getProperties() != null) {
                    Schema<?> senha = schema.getProperties().get("senha");
                    senha.setWriteOnly(true); senha.setFormat("password");
                    senha.setMinLength(8);
                    senha.setDescription("No mínimo oito caracteres e no máximo 72 bytes UTF-8, sem trim ou truncamento; limite em bytes não equivale a maxLength de caracteres.");
                    if (name.equals("ConfirmacaoRequest")) {
                        Schema<?> confirmation = schema.getProperties().get("confirmacao");
                        confirmation.setWriteOnly(true); confirmation.setFormat("password"); confirmation.setDescription("Deve coincidir exatamente com senha; não é retornada.");
                        Schema<?> code = schema.getProperties().get("codigo");
                        code.setWriteOnly(true); code.setDescription("Código de uso único recebido do ADMIN; aceita caixa, espaços e hífens. Expira em quinze minutos, rejeitado na igualdade; cinco erros o invalidam. Nunca retornado por confirmação.");
                    }
                }
            }
            Schema<?> authorization = schemas.get("Autorizacao");
            if (authorization != null && authorization.getProperties() != null) {
                Schema<?> code = authorization.getProperties().get("codigo");
                code.setReadOnly(true); code.setDescription("Somente resposta administrativa de emissão, com no-store. Base32 RFC 4648 sem padding, 128 bits; exibir uma única vez. Não persistir/logar ou recuperar por consulta. Banco guarda somente HMAC-SHA-256 vinculado à autorização/usuário.");
            }
        };
    }

    @Bean
    public OpenApiCustomizer erroIndividualCotacaoAnulavel() {
        return document -> {
            var item = document.getComponents().getSchemas().get("ResultadoAtualizacaoCotacaoResponse");
            if (item != null && item.getProperties() != null) {
                // Em 3.1, $ref com type:null não admite objeto nem null: representar a união explicitamente.
                var erro = new ComposedSchema();
                erro.setDescription("Erro desta Ação somente em FALHA; nulo nos demais resultados");
                erro.addAnyOfItem(new Schema<>().$ref("#/components/schemas/ErroAtualizacaoCotacaoResponse"));
                var nulo = new JsonSchema();
                nulo.setTypes(java.util.Set.of("null"));
                erro.addAnyOfItem(nulo);
                item.getProperties().put("erro", erro);
            }
        };
    }

    @Bean
    public OpenAPI gestaoAcoesOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("cookieAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("GESTAOACOES_AUTH")
                        .description("JWT gerenciado pelo navegador em cookie HttpOnly, SameSite=Lax, Path=/, Secure em produção, sem Domain. Não usar Authorization/Bearer ou copiar o JWT para JavaScript. Login separado; até dez minutos ociosos e quatro horas absolutas, sem refresh.")))
                .info(new Info()
                        .title("Sistema de Gestão e Controle de Carteira de Investimentos API")
                        .description("API REST para gerenciamento de corretoras, ações, carteiras, operações e indicadores de uma carteira de investimentos.")
                        .version("0.0.1-SNAPSHOT"))
                .addTagsItem(new Tag().name("Corretoras").description("Cadastro e consultas de corretoras"))
                .addTagsItem(new Tag().name("Ações").description("Cadastro, consultas e atualização da cotação de ações"))
                .addTagsItem(new Tag().name("Carteiras").description("Criação, consulta, atualização e exclusão de carteiras"))
                .addTagsItem(new Tag().name("Operações").description("Registro e consulta de compras e vendas"))
                .addTagsItem(new Tag().name("Indicadores da Carteira").description("Posições e indicadores financeiros atuais e históricos"));
    }
}
