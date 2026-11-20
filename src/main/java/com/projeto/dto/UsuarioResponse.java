package com.projeto.dto;

import com.projeto.entities.autenticacao.Papel;
import com.projeto.entities.autenticacao.Usuario;
import java.time.OffsetDateTime;

/** Projeção pública sem credencial, versão de segurança ou identificador de sessão. */
public record UsuarioResponse(Long id, String username, Papel papel, boolean ativo,
                              boolean credencialDefinida, OffsetDateTime criadaEm, OffsetDateTime atualizadaEm, long versao) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getPapel(), usuario.isAtivo(),
                usuario.isCredencialDefinida(), usuario.getCriadaEm(), usuario.getAtualizadaEm(), usuario.getVersao());
    }
}
