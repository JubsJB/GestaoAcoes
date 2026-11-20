package com.projeto.security;

import com.projeto.services.exceptions.ApiException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.util.List;

/** Instanciado somente na cadeia de segurança; não registrar como filtro servlet extra. */
final class CookieAuthenticationFilter extends OncePerRequestFilter {
    static final String COOKIE = "GESTAOACOES_AUTH";
    private final JwtService jwt;
    private final SessaoService sessoes;
    private final SecurityErrors errors;
    CookieAuthenticationFilter(JwtService jwt, SessaoService sessoes, SecurityErrors errors) { this.jwt=jwt; this.sessoes=sessoes; this.errors=errors; }
    static String cookie(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() != null) for (Cookie c : request.getCookies()) if (COOKIE.equals(c.getName())) {
            if (token != null) throw CredenciaisService.invalida();
            token = c.getValue();
        }
        return token;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath().isEmpty() ? request.getRequestURI().substring(request.getContextPath().length()) : request.getServletPath();
        return ("GET".equals(request.getMethod()) && (path.equals("/auth/csrf") || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui"))) || ("POST".equals(request.getMethod()) && (path.equals("/auth/login") || path.equals("/auth/redefinicoes/confirmar")));
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            String token = cookie(request);
            if (token != null) {
                var claims = jwt.validar(token);
                var principal = sessoes.autenticar(claims);
                var context = SecurityContextHolder.createEmptyContext();
                var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_"+principal.papel())));
                // Metadados verificados no servidor para revalidar o ator em serviços administrativos.
                // Não contém o JWT bruto e não é obtido do corpo de comandos.
                authentication.setDetails(claims);
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }
        } catch (ApiException ex) { errors.write(request, response, ex); return; }
        catch (org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException ex) {
            errors.write(request, response, HttpStatus.SERVICE_UNAVAILABLE, "SEGURANCA_INDISPONIVEL", "Não foi possível verificar a sessão."); return;
        }
        chain.doFilter(request, response);
    }
}
