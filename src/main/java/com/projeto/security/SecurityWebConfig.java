package com.projeto.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.http.HttpStatus;
import java.time.Clock;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnWebApplication
public class SecurityWebConfig {
    @Bean JwtService jwtService(AuthRuntimeConfig config, Clock clock) { return new JwtService(config, clock); }
    @Bean SecurityErrors securityErrors(JsonMapper mapper, Clock clock) { return new SecurityErrors(mapper, clock); }
    @Bean org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        var config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOrigins(java.util.List.of());
        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    @Bean CookieCsrfTokenRepository csrfTokenRepository(AuthRuntimeConfig config) {
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax").secure(!config.httpLocal()));
        return repository;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, CookieCsrfTokenRepository csrf,
                                                  JwtService jwt, SessaoService sessoes, SecurityErrors errors, AuthRuntimeConfig config,
                                                  RedefinicaoRateLimiter limites,JsonMapper mapper) throws Exception {
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable()).formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable())
                .csrf(c -> c.spa().csrfTokenRepository(csrf))
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .addFilterBefore(new TransportSecurityFilter(config,errors), CsrfFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> errors.write(req,res,HttpStatus.UNAUTHORIZED,
                                "AUTENTICACAO_NECESSARIA", "Autenticação necessária."))
                        .accessDeniedHandler((req,res,ex) -> errors.write(req,res,HttpStatus.FORBIDDEN,
                                ex instanceof CsrfException ? "CSRF_INVALIDO" : "ACESSO_NEGADO", "Acesso negado.")))
                .addFilterAfter(new RedefinicaoRateLimitFilter(limites,errors,mapper),CsrfFilter.class)
                .addFilterAfter(new CookieAuthenticationFilter(jwt, sessoes, errors), RedefinicaoRateLimitFilter.class)
                .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.GET, "/auth/csrf", "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/redefinicoes/confirmar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/logout", "/auth/atividade").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/carteiras", "/carteiras/{id}", "/carteiras/{id}/operacoes", "/carteiras/{id}/operacoes/sugestao-preco-venda",
                                "/carteiras/{id}/posicoes", "/carteiras/{id}/resultados-realizados", "/carteiras/{id}/patrimonio",
                                "/carteiras/{id}/resumo", "/carteiras/{id}/evolucao-patrimonial", "/carteiras/{id}/posicoes/distribuicao-corretoras",
                                "/carteiras/{id}/movimentacoes-custodia", "/carteiras/{id}/movimentacoes-custodia/{movimentacaoId}",
                                "/acoes", "/acoes/{id}", "/acoes/por-ticker", "/corretoras", "/corretoras/{id}", "/corretoras/por-cnpj",
                                "/operacoes", "/operacoes/{id}", "/operacoes/previa-compra").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/carteiras", "/acoes", "/corretoras", "/operacoes",
                                "/carteiras/{id}/snapshots", "/carteiras/{id}/atualizacoes-cotacoes",
                                "/carteiras/{id}/movimentacoes-custodia", "/carteiras/{id}/movimentacoes-custodia/{movimentacaoId}/estornos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/carteiras/{id}", "/acoes/{id}/cotacao").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/carteiras/{id}").hasRole("ADMIN")
                        .requestMatchers("/usuarios", "/usuarios/**").hasRole("ADMIN")
                        .anyRequest().denyAll());
        return http.build();
    }
}
