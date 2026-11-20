package com.projeto.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import java.io.IOException;

final class TransportSecurityFilter extends OncePerRequestFilter {
    private final AuthRuntimeConfig config;
    private final SecurityErrors errors;
    static final String APRESENTACAO_HOST = "localhost:8081";
    static final String APRESENTACAO_ORIGEM = "http://" + APRESENTACAO_HOST;
    private final String proxyApresentacao;

    TransportSecurityFilter(AuthRuntimeConfig config, SecurityErrors errors, String proxyApresentacao) {
        this.config = config;
        this.errors = errors;
        this.proxyApresentacao = proxyApresentacao;
    }

    private boolean apresentacao(HttpServletRequest request) {
        if (request.isSecure() || !proxyApresentacao.equals(request.getRemoteAddr())
                || request.getHeader("X-Forwarded-For") != null || request.getHeader("Forwarded") != null
                || !java.util.Collections.list(request.getHeaders("X-Forwarded-Proto")).equals(java.util.List.of("http"))
                || !java.util.Collections.list(request.getHeaders("X-Forwarded-Host")).equals(java.util.List.of("localhost"))
                || !java.util.Collections.list(request.getHeaders("X-Forwarded-Port")).equals(java.util.List.of("8081")))
            return false;
        var hosts = java.util.Collections.list(request.getHeaders("Host"));
        var origins = java.util.Collections.list(request.getHeaders("Origin"));
        return hosts.equals(java.util.List.of(APRESENTACAO_HOST))
                && (origins.isEmpty() || origins.equals(java.util.List.of(APRESENTACAO_ORIGEM)));
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        boolean loopback = "127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
        boolean transportePermitido = proxyApresentacao.isEmpty()
                ? request.isSecure() || (config.httpLocal() && loopback)
                : apresentacao(request);
        if (!transportePermitido) {
            errors.write(request,response,HttpStatus.BAD_REQUEST,"HTTPS_OBRIGATORIO","HTTPS é obrigatório."); return;
        }
        // Não interpretar X-Forwarded-* fornecido diretamente pelo cliente.
        chain.doFilter(request,response);
    }
}
