package com.projeto.security;

import com.projeto.entities.autenticacao.Papel;
import com.projeto.entities.autenticacao.Usuario;
import com.projeto.repositories.autenticacao.UsuarioRepository;
import com.projeto.services.exceptions.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class CredenciaisService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String dummy;
    public CredenciaisService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.dummy = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Identidade autenticar(String username, String senha) {
        validarSenha(senha);
        String canonico = username(username);
        Usuario usuario = usuarios.findByUsernameForUpdate(canonico).orElse(null);
        boolean elegivel = usuario != null && usuario.isAtivo() && usuario.isCredencialDefinida();
        boolean confere = encoder.matches(senha, elegivel ? usuario.getSenhaHash() : dummy);
        if (!confere || !elegivel) throw invalida();
        return new Identidade(usuario.getId(), usuario.getUsername(), usuario.getPapel(), usuario.getVersaoSeguranca());
    }

    static String username(String username) {
        try { return Usuario.normalizarUsername(username); }
        catch (IllegalArgumentException | NullPointerException ex) { throw formato(); }
    }
    static void validarSenha(String senha) {
        if (senha == null || senha.codePointCount(0, senha.length()) < 8
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) throw formato();
    }
    static ApiException formato() {
        return new ApiException(HttpStatus.BAD_REQUEST, "CREDENCIAIS_FORMATO_INVALIDO", "Formato de credenciais inválido.");
    }
    static ApiException invalida() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTENTICACAO_NECESSARIA", "Credenciais inválidas.");
    }
    public record Identidade(Long id, String username, Papel papel, long versaoSeguranca) { }
}
