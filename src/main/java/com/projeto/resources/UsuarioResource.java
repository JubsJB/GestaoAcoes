package com.projeto.resources;

import com.projeto.dto.UsuarioResponse;
import com.projeto.services.UsuarioAdministracaoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.projeto.entities.autenticacao.Papel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/** D6: toda a superfície administrativa, inclusive consultas, é exclusiva de ADMIN. */
@RestController
@RequestMapping("/usuarios")
@ConditionalOnWebApplication
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioResource {
    private final UsuarioAdministracaoService usuarios;
    public UsuarioResource(UsuarioAdministracaoService usuarios) { this.usuarios=usuarios; }
    @PostMapping
    public ResponseEntity<com.projeto.dto.EmissaoAutorizacaoResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        var resposta=usuarios.cadastrar(request.username(),request.papel());
        return ResponseEntity.created(java.net.URI.create("/usuarios/"+resposta.usuario().id())).header("Cache-Control","no-store").body(resposta);
    }
    public record CadastroRequest(@jakarta.validation.constraints.NotBlank String username,@NotNull Papel papel) {
        @JsonAnySetter public void rejeitarCampo(String campo,Object valor){throw new IllegalArgumentException("Campo não permitido no comando administrativo.");}
    }
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(usuarios.listar());
    }
    @PostMapping("/{id}/redefinicoes")
    public ResponseEntity<com.projeto.dto.EmissaoAutorizacaoResponse> autorizarRedefinicao(@PathVariable Long id,@Valid @RequestBody AutorizacaoRequest request) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(usuarios.autorizarRedefinicao(id,request.versaoEsperada()));
    }
    public record AutorizacaoRequest(@NotNull @PositiveOrZero Long versaoEsperada) {
        @JsonAnySetter public void rejeitarCampo(String campo,Object valor){throw new IllegalArgumentException("Campo não permitido no comando administrativo.");}
    }
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(usuarios.buscar(id));
    }
    @PatchMapping("/{id}/perfil")
    public ResponseEntity<UsuarioResponse> alterarPerfil(@PathVariable Long id,@Valid @RequestBody PerfilRequest request) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(usuarios.alterarPerfil(id,request.papel(),request.versaoEsperada()));
    }
    public record PerfilRequest(@NotNull Papel papel,@NotNull @PositiveOrZero Long versaoEsperada) {
        @JsonAnySetter public void rejeitarCampo(String campo,Object valor) {
            throw new IllegalArgumentException("Campo não permitido no comando administrativo.");
        }
    }
    @PatchMapping("/{id}/situacao")
    public ResponseEntity<UsuarioResponse> alterarSituacao(@PathVariable Long id,@Valid @RequestBody SituacaoRequest request) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(usuarios.alterarSituacao(id,request.ativo(),request.versaoEsperada()));
    }
    public record SituacaoRequest(@NotNull Boolean ativo,@NotNull @PositiveOrZero Long versaoEsperada) {
        @JsonAnySetter public void rejeitarCampo(String campo,Object valor) {
            throw new IllegalArgumentException("Campo não permitido no comando administrativo.");
        }
    }
}
