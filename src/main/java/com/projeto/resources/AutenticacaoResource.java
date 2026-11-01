package com.projeto.resources;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.ResponseEntity;
import com.projeto.security.*;
import com.projeto.services.exceptions.ApiException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.*;
import java.time.*;
import java.util.Arrays;

@RestController
@ConditionalOnWebApplication
public class AutenticacaoResource {
    private final RedefinicaoSenhaService redefinicoes;
    @PostMapping("/auth/redefinicoes/confirmar")
    public ResponseEntity<Void> confirmar(@RequestBody ConfirmacaoRequest request) {
        redefinicoes.confirmar(request.username(),request.codigo(),request.senha(),request.confirmacao());
        return ResponseEntity.noContent().header("Cache-Control","no-store").build();
    }
    public record ConfirmacaoRequest(String username,String codigo,String senha,String confirmacao) {
        @Override public String toString(){return "ConfirmacaoRequest[credenciais protegidas]";}
        @com.fasterxml.jackson.annotation.JsonAnySetter public void rejeitarCampo(String campo,Object valor){throw new IllegalArgumentException("Campo não permitido.");}
    }
    private final SessaoService sessoes;
    private final AuthRuntimeConfig config;
    private final CookieCsrfTokenRepository csrf;
    private final Clock clock;
    private final JwtService jwt;
    public AutenticacaoResource(SessaoService sessoes, AuthRuntimeConfig config, CookieCsrfTokenRepository csrf, Clock clock, JwtService jwt, RedefinicaoSenhaService redefinicoes) {
        this.sessoes=sessoes; this.config=config; this.csrf=csrf; this.clock=clock; this.jwt=jwt;
        this.redefinicoes=redefinicoes;
    }
    @GetMapping("/auth/csrf")
    public ResponseEntity<Void> csrf(@io.swagger.v3.oas.annotations.Parameter(hidden = true) CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }
    @PostMapping("/auth/login")
    public ResponseEntity<SessaoResponse> login(@RequestBody LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
        try {
            String anterior = null;
            if (http.getCookies() != null) anterior = Arrays.stream(http.getCookies()).filter(c -> c.getName().equals("GESTAOACOES_AUTH"))
                    .map(Cookie::getValue).findFirst().orElse(null);
            var login = sessoes.login(request.username(), request.senha(), anterior, http.getRemoteAddr());
            csrf.saveToken(null, http, response);
            var cookie = ResponseCookie.from("GESTAOACOES_AUTH", login.token()).httpOnly(true).secure(!config.httpLocal())
                    .sameSite("Lax").path("/").maxAge(Math.max(0, Duration.between(clock.instant(), login.principal().expiraEm().toInstant()).getSeconds())).build();
            return ResponseEntity.ok().header("Cache-Control", "no-store").header("Set-Cookie", cookie.toString()).body(SessaoResponse.from(login.principal()));
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.TOO_MANY_REQUESTS) response.setHeader("Retry-After",ex.getDetails().get("retryAfter").toString());
            throw ex;
        } catch (org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SEGURANCA_INDISPONIVEL", "Não foi possível autenticar.");
        }
    }
    @GetMapping("/auth/me")
    public ResponseEntity<SessaoResponse> me(@AuthenticationPrincipal SessaoService.Principal principal) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(SessaoResponse.from(principal));
    }
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@io.swagger.v3.oas.annotations.Parameter(hidden = true) @CookieValue("GESTAOACOES_AUTH") String token, HttpServletRequest request, HttpServletResponse response) {
        try { sessoes.logout(jwt.validar(token)); }
        catch (org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SEGURANCA_INDISPONIVEL", "Não foi possível encerrar a sessão.");
        }
        csrf.saveToken(null,request,response);
        return ResponseEntity.noContent().header("Cache-Control", "no-store").header("Set-Cookie",
                ResponseCookie.from("GESTAOACOES_AUTH", "").httpOnly(true).secure(!config.httpLocal()).sameSite("Lax").path("/").maxAge(0).build().toString()).build();
    }
    @PostMapping("/auth/atividade")
    public ResponseEntity<SessaoResponse> atividade(@io.swagger.v3.oas.annotations.Parameter(hidden = true) @CookieValue("GESTAOACOES_AUTH") String token) {
        try {
            return ResponseEntity.ok().header("Cache-Control","no-store").body(SessaoResponse.from(sessoes.atividade(jwt.validar(token))));
        } catch (org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"SEGURANCA_INDISPONIVEL","Não foi possível registrar atividade.");
        }
    }
    public record LoginRequest(String username, String senha) {
        @Override public String toString() { return "LoginRequest[credenciais protegidas]"; }
    }
    public record SessaoResponse(Long id, String username, com.projeto.entities.autenticacao.Papel papel,
                                 OffsetDateTime horarioServidor, OffsetDateTime ultimaAtividadeEm, OffsetDateTime inativaEm, OffsetDateTime expiraEm) {
        static SessaoResponse from(SessaoService.Principal p) {
            return new SessaoResponse(p.id(),p.username(),p.papel(),p.horarioServidor(),p.ultimaAtividadeEm(),p.ultimaAtividadeEm().plusMinutes(10),p.expiraEm());
        }
    }
}
