package com.projeto.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.env.Environment;

/** Fronteira de validação do deployment operacional. */
@Configuration(proxyBeanMethods = false)
@Profile("operacional")
public class OperationalRuntimeConfig {
    @Bean static BeanFactoryPostProcessor validarRuntimeOperacional(Environment env) {
        return factory -> {
            String ip = env.getProperty("app.trusted-proxy-ip", "");
            if (!ip.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")
                    || java.util.Arrays.stream(ip.split("\\.")).anyMatch(v -> Integer.parseInt(v) > 255)
                    || !(ip + "/32").equals(env.getProperty("server.tomcat.remoteip.internal-proxies")))
                throw new IllegalStateException("Proxy operacional deve ser um único IPv4 explicitamente identificado");
            if (java.util.Arrays.stream(env.getActiveProfiles()).anyMatch(p -> !p.equals("operacional") && !p.equals("bootstrap-admin"))
                    || !"native".equals(env.getProperty("server.forward-headers-strategy"))
                    || !env.getProperty("server.tomcat.remoteip.trusted-proxies", "").isBlank()
                    || !"false".equals(env.getProperty("auth.allow-local-http")))
                throw new IllegalStateException("Perfil operacional exige HTTPS e encaminhamento nativo estrito");
            if (!"https://gestaoacoes.localhost:8443".equals(env.getProperty("app.public-origin")))
                throw new IllegalStateException("Origem operacional incompatível com o ingresso homologado");
            var headers = java.util.Map.of("remote-ip-header", "X-Forwarded-For", "protocol-header", "X-Forwarded-Proto",
                    "host-header", "X-Forwarded-Host", "port-header", "X-Forwarded-Port");
            headers.forEach((property, expected) -> {
                if (!expected.equals(env.getProperty("server.tomcat.remoteip." + property)))
                    throw new IllegalStateException("Header confiável incompatível com Nginx operacional");
            });
            for (String property : java.util.List.of("spring.datasource.jndi-name", "spring.datasource.type",
                    "spring.datasource.hikari.jdbc-url", "spring.datasource.hikari.data-source-class-name",
                    "spring.datasource.hikari.username", "spring.datasource.hikari.password"))
                if (!env.getProperty(property, "").isBlank())
                    throw new IllegalStateException("Redirecionamento de datasource operacional recusado");
            String banco = env.getProperty("op.db.name", ""), usuario = env.getProperty("op.db.app-user", "");
            if (!banco.matches("[a-z][a-z0-9_]{2,50}") || !usuario.matches("[a-z][a-z0-9_]{2,50}")
                    || !("jdbc:postgresql://postgres:5432/"+banco).equals(env.getProperty("spring.datasource.url"))
                    || !usuario.equals(env.getProperty("spring.datasource.username"))
                    || env.getProperty("spring.datasource.password", "").isBlank())
                throw new IllegalStateException("Datasource operacional explícito e isolado obrigatório");
            if (!java.util.Arrays.asList(env.getActiveProfiles()).contains("bootstrap-admin")) {
                var jwt = new AuthRuntimeConfig(env.getProperty("auth.jwt.secret", env.getProperty("AUTH_JWT_SECRET")),
                        java.time.Duration.parse(env.getProperty("auth.absolute-duration", "PT4H")), false);
                new PasswordResetConfig(env.getProperty("auth.reset.secret", env.getProperty("AUTH_RESET_SECRET")), jwt.key());
            }
        };
    }
    @Bean org.springdoc.core.customizers.OpenApiCustomizer origemPublicaOpenApi() {
        return api -> api.setServers(java.util.List.of(new io.swagger.v3.oas.models.servers.Server().url("/api")));
    }
}
