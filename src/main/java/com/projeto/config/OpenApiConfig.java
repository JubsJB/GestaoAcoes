package com.projeto.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

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
