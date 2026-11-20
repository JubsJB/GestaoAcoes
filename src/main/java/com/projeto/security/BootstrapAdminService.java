package com.projeto.security;

import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;

/** Fronteira operacional local; não é cadastro administrativo nem endpoint HTTP. */
@Service
public class BootstrapAdminService {
    private final UsuarioRepository usuarios;
    private final ControleSegurancaRepository controle;
    private final AuditoriaUsuarioRepository auditoria;
    private final PasswordEncoder encoder;
    private final Clock clock;
    public BootstrapAdminService(UsuarioRepository usuarios, ControleSegurancaRepository controle,
                                 AuditoriaUsuarioRepository auditoria, PasswordEncoder encoder, Clock clock) {
        this.usuarios=usuarios; this.controle=controle; this.auditoria=auditoria; this.encoder=encoder; this.clock=clock;
    }
    @Transactional
    public boolean executar(String username, String senha) {
        String canonico = CredenciaisService.username(username);
        CredenciaisService.validarSenha(senha);
        controle.findByIdForUpdate((short) 1).orElseThrow(() -> new IllegalStateException("Controle de segurança ausente"));
        if (usuarios.count() != 0) return false;
        var agora = OffsetDateTime.now(clock);
        var usuario = new Usuario(canonico, Papel.ADMIN, agora);
        usuario.definirCredencialInicial(encoder.encode(senha), agora);
        usuarios.saveAndFlush(usuario);
        auditoria.saveAndFlush(new AuditoriaUsuario(null, usuario, "BOOTSTRAP", "SUCESSO", agora));
        return true;
    }
}
