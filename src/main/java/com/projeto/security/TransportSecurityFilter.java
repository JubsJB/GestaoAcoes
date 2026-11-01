package com.projeto.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import java.io.IOException;

final class TransportSecurityFilter extends OncePerRequestFilter {
    private final AuthRuntimeConfig config;
    private final SecurityErrors errors;
    TransportSecurityFilter(AuthRuntimeConfig config, SecurityErrors errors) { this.config=config; this.errors=errors; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        boolean loopback = "127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
        if (!request.isSecure() && !(config.httpLocal() && loopback)) {
            errors.write(request,response,HttpStatus.BAD_REQUEST,"HTTPS_OBRIGATORIO","HTTPS é obrigatório."); return;
        }
        // Não interpretar X-Forwarded-* fornecido diretamente pelo cliente.
        chain.doFilter(request,response);
    }
}
