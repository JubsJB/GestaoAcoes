package com.projeto.security;

import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.*;
import com.projeto.services.exceptions.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service @ConditionalOnWebApplication
public class RedefinicaoSenhaService {
    private final UsuarioRepository usuarios;
    private final AutorizacaoRedefinicaoSenhaRepository autorizacoes;
    private final ControleSegurancaRepository controle;
    private final AuditoriaUsuarioRepository auditoria;
    private final CodigosRedefinicao codigos;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final TransactionTemplate comandos;
    public RedefinicaoSenhaService(UsuarioRepository usuarios,AutorizacaoRedefinicaoSenhaRepository autorizacoes,
            ControleSegurancaRepository controle,AuditoriaUsuarioRepository auditoria,CodigosRedefinicao codigos,
            PasswordEncoder encoder,Clock clock,PlatformTransactionManager manager) {
        this.usuarios=usuarios;this.autorizacoes=autorizacoes;this.controle=controle;this.auditoria=auditoria;
        this.codigos=codigos;this.encoder=encoder;this.clock=clock;
        comandos=new TransactionTemplate(manager);comandos.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        comandos.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    public void confirmar(String username,String codigo,String senha,String confirmacao) {
        try {CredenciaisService.validarSenha(senha);}
        catch(ApiException ex) {throw senhaInvalida();}
        if(!senha.equals(confirmacao)) throw senhaInvalida();
        String canonico;
        try {canonico=Usuario.normalizarUsername(username);} catch(IllegalArgumentException | NullPointerException ex) {throw invalida();}
        try {
            var erro=comandos.execute(status -> {
                controle.findByIdForUpdate((short)1).orElseThrow(IllegalStateException::new);
                var usuario=usuarios.findByUsernameForUpdate(canonico).orElse(null);
                var pendentes=usuario==null?java.util.List.<AutorizacaoRedefinicaoSenha>of():autorizacoes.findPendentesForUpdate(usuario.getId());
                var autorizacao=pendentes.stream().max(java.util.Comparator.comparing(AutorizacaoRedefinicaoSenha::getCriadaEm).thenComparing(AutorizacaoRedefinicaoSenha::getId)).orElse(null);
                boolean confere=codigos.confere(usuario==null?0:usuario.getId(),autorizacao==null?new UUID(0,0):autorizacao.getId(),codigo,
                        autorizacao==null?"0".repeat(64):autorizacao.getDigest());
                var agora=OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
                if(usuario==null || !usuario.isAtivo() || autorizacao==null || autorizacao.getTentativasRestantes()<=0 || !agora.isBefore(autorizacao.getExpiraEm()))
                    return invalida();
                if(!confere) {autorizacao.registrarErro(agora);autorizacoes.flush();return invalida();}
                var hash=encoder.encode(senha);
                agora=OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
                if(!agora.isBefore(autorizacao.getExpiraEm())) return invalida();
                usuario.redefinirCredencial(hash,agora);autorizacao.consumir(agora);
                usuarios.flush();autorizacoes.flush();
                auditoria.saveAndFlush(new AuditoriaUsuario(usuario,usuario,"REDEFINICAO_SENHA","SUCESSO",agora));
                return null;
            });
            if(erro!=null) throw erro;
        } catch(org.springframework.dao.DataAccessException | TransactionException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"SEGURANCA_INDISPONIVEL","Não foi possível concluir a definição de senha.");
        }
    }
    private static ApiException invalida(){return new ApiException(HttpStatus.BAD_REQUEST,"REDEFINICAO_INVALIDA","Não foi possível confirmar a definição de senha.");}
    private static ApiException senhaInvalida(){return new ApiException(HttpStatus.BAD_REQUEST,"SENHA_INVALIDA","Senha e confirmação devem atender à política e ser iguais.");}
}
