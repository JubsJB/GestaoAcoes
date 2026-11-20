package com.projeto.services;

import com.projeto.dto.UsuarioResponse;
import com.projeto.entities.autenticacao.Papel;
import com.projeto.repositories.autenticacao.UsuarioRepository;
import com.projeto.security.JwtService;
import com.projeto.security.SessaoService;
import com.projeto.services.exceptions.ApiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.projeto.entities.autenticacao.AuditoriaUsuario;
import com.projeto.repositories.autenticacao.ControleSegurancaRepository;
import com.projeto.repositories.autenticacao.AuditoriaUsuarioRepository;
import com.projeto.repositories.autenticacao.AutorizacaoRedefinicaoSenhaRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@ConditionalOnWebApplication
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioAdministracaoService {
    private final UsuarioRepository usuarios;
    private final SessaoService sessoes;
    private final ControleSegurancaRepository controle;
    private final AuditoriaUsuarioRepository auditoria;
    private final Clock clock;
    private final AutorizacaoRedefinicaoSenhaRepository autorizacoes;
    private final TransactionTemplate comandos;
    private final com.projeto.security.CodigosRedefinicao codigos;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    public UsuarioAdministracaoService(UsuarioRepository usuarios, SessaoService sessoes, ControleSegurancaRepository controle,
            AuditoriaUsuarioRepository auditoria, Clock clock, PlatformTransactionManager manager,AutorizacaoRedefinicaoSenhaRepository autorizacoes,
            com.projeto.security.CodigosRedefinicao codigos) {
        this.usuarios=usuarios; this.sessoes=sessoes;
        this.controle=controle; this.auditoria=auditoria; this.clock=clock;
        this.autorizacoes=autorizacoes;
        this.codigos=codigos;
        comandos=new TransactionTemplate(manager);
        comandos.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        comandos.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    @Transactional(readOnly=true)
    public List<UsuarioResponse> listar() {
        revalidarAtor();
        return usuarios.findAll(Sort.by("id")).stream().map(UsuarioResponse::from).toList();
    }
    @Transactional(readOnly=true)
    public UsuarioResponse buscar(Long id) {
        revalidarAtor();
        return UsuarioResponse.from(usuarios.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND,"USUARIO_NAO_ENCONTRADO","Usuário não encontrado.")));
    }
    public UsuarioResponse alterarPerfil(Long id,Papel papel,Long versaoEsperada) {
        if(id==null || papel==null || versaoEsperada==null || versaoEsperada<0)
            throw new ApiException(HttpStatus.BAD_REQUEST,"REQUEST_INVALIDO","Comando administrativo inválido.");
        return alterar(id,versaoEsperada,papel,null);
    }
    public UsuarioResponse alterarSituacao(Long id,Boolean ativo,Long versaoEsperada) {
        if(id==null || ativo==null || versaoEsperada==null || versaoEsperada<0)
            throw new ApiException(HttpStatus.BAD_REQUEST,"REQUEST_INVALIDO","Comando administrativo inválido.");
        return alterar(id,versaoEsperada,null,ativo);
    }
    private UsuarioResponse alterar(Long id,Long versaoEsperada,Papel papel,Boolean ativo) {
        try {
            return executarAlteracao(id, versaoEsperada, papel, ativo);
        } catch (org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SEGURANCA_INDISPONIVEL",
                    "Não foi possível concluir o comando administrativo.");
        }
    }
    private UsuarioResponse executarAlteracao(Long id,Long versaoEsperada,Papel papel,Boolean ativo) {
        var resultado = comandos.execute(status -> {
            controle.findByIdForUpdate((short)1).orElseThrow(() -> new IllegalStateException("Controle de segurança ausente"));
            var usuario = usuarios.findByIdForUpdate(id);
            // A sessão pode expirar ou ser invalidada durante qualquer espera por locks.
            var ator=revalidarAtor();
            var alvo=usuario.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"USUARIO_NAO_ENCONTRADO","Usuário não encontrado."));
            String acao = papel != null ? "ALTERACAO_PERFIL" : ativo ? "HABILITACAO" : "BLOQUEIO";
            var agora=OffsetDateTime.now(clock);
            if(alvo.getVersao()!=versaoEsperada)
                return new Resultado(null, new Recusa(ator.id(), id, acao, agora,
                        new ApiException(HttpStatus.CONFLICT,"VERSAO_USUARIO_OBSOLETA","A conta foi alterada. Consulte o estado atual.")));
            if(papel!=null ? alvo.getPapel()==papel : alvo.isAtivo()==ativo) return new Resultado(UsuarioResponse.from(alvo), null);
            boolean removeAdmin=papel!=null ? papel!=Papel.ADMIN : !ativo;
            if(removeAdmin && alvo.getPapel()==Papel.ADMIN && alvo.isAtivo() && alvo.isCredencialDefinida()
                    && usuarios.countByPapelAndAtivoTrueAndCredencialDefinidaTrue(Papel.ADMIN)<=1)
                return new Resultado(null, new Recusa(ator.id(), id, acao, agora,
                        new ApiException(HttpStatus.CONFLICT,"ULTIMO_ADMIN_ATIVO","O último administrador operacional deve ser preservado.")));
            if(papel!=null) { alvo.alterarPerfil(papel,agora); }
            else {
                alvo.alterarSituacao(ativo,agora);
                if(!ativo) autorizacoes.findPendentesForUpdate(id).forEach(a->a.invalidar(agora));
            }
            usuarios.flush();
            auditoria.saveAndFlush(new AuditoriaUsuario(usuarios.getReferenceById(ator.id()),alvo,acao,"SUCESSO",agora));
            return new Resultado(UsuarioResponse.from(alvo), null);
        });
        if (resultado.recusa() != null) {
            var recusa = resultado.recusa();
            // Iniciar somente após liberar os locks originais: as FKs da auditoria
            // não devem esperar pela própria transação suspensa no PostgreSQL.
            registrarRecusa(recusa);
        }
        return resultado.usuario();
    }
    private record Resultado(UsuarioResponse usuario, Recusa recusa) { }
    private record ResultadoEmissao(com.projeto.dto.EmissaoAutorizacaoResponse resposta,Recusa recusa) { }
    public com.projeto.dto.EmissaoAutorizacaoResponse cadastrar(String username,Papel papel) {
        String canonico;
        try {canonico=com.projeto.entities.autenticacao.Usuario.normalizarUsername(username);}
        catch(IllegalArgumentException | NullPointerException ex) {throw new ApiException(HttpStatus.BAD_REQUEST,"REQUEST_INVALIDO","Username inválido.");}
        if(papel==null) throw new ApiException(HttpStatus.BAD_REQUEST,"REQUEST_INVALIDO","Perfil obrigatório.");
        try {
            var resultado=comandos.execute(status -> {
                controle.findByIdForUpdate((short)1).orElseThrow(IllegalStateException::new);
                var ator=revalidarAtor();
                var existente=usuarios.findByUsername(canonico);
                if(existente.isPresent()) return new ResultadoEmissao(null,new Recusa(ator.id(),existente.get().getId(),"CADASTRO",OffsetDateTime.now(clock),
                        new ApiException(HttpStatus.CONFLICT,"USUARIO_DUPLICADO","Username indisponível.")));
                var usuario=usuarios.saveAndFlush(new com.projeto.entities.autenticacao.Usuario(canonico,papel,OffsetDateTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));
                // Hibernate mantém lock WRITE na entidade inserida e pode ignorar FORCE_INCREMENT.
                // Recarregar somente esta conta, ainda na mesma transação, antes de emitir.
                entityManager.detach(usuario);
                var persistido=usuarios.findByIdForUpdate(usuario.getId()).orElseThrow(IllegalStateException::new);
                return new ResultadoEmissao(emitirAutorizacao(persistido,ator.id(),"CADASTRO"),null);
            });
            if(resultado.recusa()!=null) registrarRecusa(resultado.recusa());
            return resultado.resposta();
        } catch(org.springframework.dao.DataIntegrityViolationException ex) {
            if(String.valueOf(ex.getMostSpecificCause().getMessage()).toUpperCase(java.util.Locale.ROOT).contains("UK_USUARIO_USERNAME"))
                throw new ApiException(HttpStatus.CONFLICT,"USUARIO_DUPLICADO","Username indisponível.");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"SEGURANCA_INDISPONIVEL","Não foi possível cadastrar a conta.");
        } catch(org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"SEGURANCA_INDISPONIVEL","Não foi possível cadastrar a conta.");
        }
    }
    private void registrarRecusa(Recusa recusa) {
        comandos.executeWithoutResult(status -> auditoria.saveAndFlush(new AuditoriaUsuario(
                usuarios.getReferenceById(recusa.ator()),usuarios.getReferenceById(recusa.alvo()),recusa.acao(),"RECUSADO",recusa.instante())));
        throw recusa.erro();
    }
    public com.projeto.dto.EmissaoAutorizacaoResponse autorizarRedefinicao(Long id,Long versaoEsperada) {
        if(id==null || versaoEsperada==null || versaoEsperada<0)
            throw new ApiException(HttpStatus.BAD_REQUEST,"REQUEST_INVALIDO","Comando administrativo inválido.");
        try {
            var resultado=comandos.execute(status -> {
                controle.findByIdForUpdate((short)1).orElseThrow(IllegalStateException::new);
                var usuario=usuarios.findByIdForUpdate(id);
                var ator=revalidarAtor();
                var alvo=usuario.orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"USUARIO_NAO_ENCONTRADO","Usuário não encontrado."));
                ApiException erro=alvo.getVersao()!=versaoEsperada?new ApiException(HttpStatus.CONFLICT,"CONFLITO_VERSAO","Consulte a versão atual da conta."):
                        !alvo.isAtivo()?new ApiException(HttpStatus.CONFLICT,"USUARIO_BLOQUEADO","Conta bloqueada."):null;
                if(erro!=null) return new ResultadoEmissao(null,new Recusa(ator.id(),id,"AUTORIZACAO_REDEFINICAO",OffsetDateTime.now(clock),erro));
                return new ResultadoEmissao(emitirAutorizacao(alvo,ator.id(),"AUTORIZACAO_REDEFINICAO"),null);
            });
            if(resultado.recusa()!=null) registrarRecusa(resultado.recusa());
            return resultado.resposta();
        } catch(org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"SEGURANCA_INDISPONIVEL","Não foi possível emitir autorização.");
        }
    }
    private com.projeto.dto.EmissaoAutorizacaoResponse emitirAutorizacao(com.projeto.entities.autenticacao.Usuario alvo,Long ator,String acao) {
        var pendentes=autorizacoes.findPendentesForUpdate(alvo.getId());
        revalidarAtor(); // Inclusive depois de esperar os locks das autorizações.
        var agora=OffsetDateTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        pendentes.forEach(a->a.invalidar(agora));
        var id=java.util.UUID.randomUUID();var codigo=codigos.gerar();
        var motivo=alvo.isCredencialDefinida()?com.projeto.entities.autenticacao.MotivoRedefinicao.REDEFINICAO:com.projeto.entities.autenticacao.MotivoRedefinicao.INICIAL;
        var autorizacao=new com.projeto.entities.autenticacao.AutorizacaoRedefinicaoSenha(id,alvo,usuarios.getReferenceById(ator),
                codigos.digest(alvo.getId(),id,codigo),motivo,agora,agora.plusMinutes(15),5,null,null);
        autorizacoes.saveAndFlush(autorizacao);
        entityManager.lock(alvo,jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        auditoria.saveAndFlush(new AuditoriaUsuario(usuarios.getReferenceById(ator),alvo,acao,"SUCESSO",agora));
        return new com.projeto.dto.EmissaoAutorizacaoResponse(UsuarioResponse.from(alvo),
                new com.projeto.dto.EmissaoAutorizacaoResponse.Autorizacao(codigo,motivo,autorizacao.getExpiraEm()));
    }
    private record Recusa(Long ator, Long alvo, String acao, OffsetDateTime instante, ApiException erro) { }
    private SessaoService.Principal revalidarAtor() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth==null || !(auth.getPrincipal() instanceof SessaoService.Principal)
                || !(auth.getDetails() instanceof JwtService.Claims claims))
            throw new ApiException(HttpStatus.UNAUTHORIZED,"AUTENTICACAO_NECESSARIA","Autenticação necessária.");
        var ator=sessoes.autenticar(claims);
        if (ator.papel()!=Papel.ADMIN)
            throw new ApiException(HttpStatus.FORBIDDEN,"ACESSO_NEGADO","Acesso negado.");
        return ator;
    }
}
