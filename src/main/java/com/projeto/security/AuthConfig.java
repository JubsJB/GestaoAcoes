package com.projeto.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import java.time.Duration;
import java.util.Arrays;

@Configuration(proxyBeanMethods = false)
@Profile("!bootstrap-admin")
public class AuthConfig {
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
    PasswordResetConfig passwordResetConfig(Environment env, AuthRuntimeConfig jwt) {
        return new PasswordResetConfig(env.getProperty("auth.reset.secret",env.getProperty("AUTH_RESET_SECRET")),jwt.key());
    }
    @Bean AuthRuntimeConfig authRuntimeConfig(Environment env) {
        boolean local = Boolean.parseBoolean(env.getProperty("auth.allow-local-http", "false"));
        if (local && (env.getActiveProfiles().length == 0 || Arrays.stream(env.getActiveProfiles()).anyMatch(p -> !p.equals("dev") && !p.equals("test"))))
            throw new IllegalStateException("HTTP local exige profile dev ou test explícito");
        Duration duracao;
        try { duracao = Duration.parse(env.getProperty("auth.absolute-duration", "PT4H")); }
        catch (RuntimeException ex) { throw new IllegalStateException("Duração absoluta inválida"); }
        return new AuthRuntimeConfig(env.getProperty("auth.jwt.secret", env.getProperty("AUTH_JWT_SECRET")), duracao, local);
    }
}
