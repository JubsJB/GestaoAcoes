package com.projeto.config;

import io.swagger.v3.oas.models.OpenAPI;
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
