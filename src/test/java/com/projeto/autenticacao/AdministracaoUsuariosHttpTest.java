package com.projeto.autenticacao;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=GestaoacoesApplication.class,properties={"auth.test.administracao=true","spring.datasource.hikari.connection-init-sql=SET LOCK_TIMEOUT 10000"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdministracaoUsuariosHttpTest {
    @Autowired MockMvc http;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired DataSource dataSource;
    @Autowired AutorizacaoRedefinicaoSenhaRepository autorizacoes;
    @Autowired com.projeto.services.UsuarioAdministracaoService servico;
    @Autowired com.projeto.security.JwtService jwt;
    @Autowired com.projeto.security.SessaoService sessoes;
    @Autowired org.springframework.transaction.PlatformTransactionManager transacoes;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean(name="applicationClock") Clock clock;
    Conta admin;
    @BeforeEach void preparar() {
        var jdbc=new JdbcTemplate(dataSource);
        for(String tabela:new String[]{"auditoria_usuario","autorizacao_redefinicao_senha","sessao_autenticacao","contador_abuso","usuario"})
            jdbc.update("delete from "+tabela);
        admin=conta(Papel.ADMIN);
    }
    @Test void adminConsultaEListaDtoSeguroComVersaoOtimista() throws Exception {
        var alvo=conta(Papel.USER); var cookie=login(admin);
        var resposta=http.perform(get("/usuarios/{id}",alvo.id()).secure(true).cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(alvo.nome()))
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$.versao").value(0)).andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.versaoSeguranca").doesNotExist()).andReturn().getResponse();
        assertFalse(resposta.getContentAsString().contains(alvo.senha()));
        http.perform(get("/usuarios").secure(true).cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
    @Test void ultimoAdministradorNaoPodeSeRebaixar() throws Exception {
        var auth=login(admin);
        alterarPerfil(auth,admin.id(),"USER",0).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ULTIMO_ADMIN_ATIVO"));
        http.perform(get("/usuarios/{id}",admin.id()).secure(true).cookie(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN")).andExpect(jsonPath("$.versao").value(0));
    }
    @Test void perfilSemMudancaMantemVersaoSessaoEAuditoria() throws Exception {
        var auth=login(admin);
        alterarPerfil(auth,admin.id(),"ADMIN",0).andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(0));
        assertEquals(0,new JdbcTemplate(dataSource).queryForObject("select count(*) from auditoria_usuario",Integer.class));
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isOk());
    }
    @Test void versaoObsoletaEhRejeitadaAntesDoComandoSemMudanca() throws Exception {
        alterarPerfil(login(admin),admin.id(),"ADMIN",9).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_USUARIO_OBSOLETA"));
    }
    @Test void alteracaoEfetivaDePerfilRetornaNovaVersaoEInvalidaSessaoAntiga() throws Exception {
        var alvo=conta(Papel.USER); var antigo=login(alvo); var auth=login(admin);
        alterarPerfil(auth,alvo.id(),"ADMIN",0).andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN")).andExpect(jsonPath("$.versao").value(1));
        http.perform(get("/auth/me").secure(true).cookie(antigo)).andExpect(status().isUnauthorized());
        http.perform(get("/usuarios").secure(true).cookie(login(alvo))).andExpect(status().isOk());
        var jdbc=new JdbcTemplate(dataSource);
        assertEquals(1,jdbc.queryForObject("select count(*) from auditoria_usuario where ator_id=? and alvo_id=? and acao='ALTERACAO_PERFIL' and resultado='SUCESSO'",Integer.class,admin.id(),alvo.id()));
    }
    @Test void ultimoAdminNaoPodeSeBloquearNemContarAdminSemCredencialComoSubstituto() throws Exception {
        usuarios.saveAndFlush(new Usuario("pendente."+UUID.randomUUID(),Papel.ADMIN,OffsetDateTime.now(ZoneOffset.UTC)));
        var auth=login(admin);
        alterarSituacao(auth,admin.id(),false,0).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ULTIMO_ADMIN_ATIVO"));
        alterarPerfil(auth,admin.id(),"USER",0).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ULTIMO_ADMIN_ATIVO"));
    }
    @Test void bloqueioInvalidaSessoesECodigoEHabilitacaoNaoRestauraEstadoAntigo() throws Exception {
        var alvo=conta(Papel.USER); var a=login(alvo); var b=login(alvo); var auth=login(admin);
        var instante=OffsetDateTime.now(ZoneOffset.UTC);
        var codigo=autorizacoes.saveAndFlush(new AutorizacaoRedefinicaoSenha(UUID.randomUUID(),usuarios.getReferenceById(alvo.id()),
                usuarios.getReferenceById(admin.id()),"a".repeat(64),MotivoRedefinicao.INICIAL,instante,instante.plusMinutes(15),5,null,null));
        alterarSituacao(auth,alvo.id(),false,0).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.versao").value(1));
        for(var antigo:new Cookie[]{a,b}) http.perform(get("/auth/me").secure(true).cookie(antigo)).andExpect(status().isUnauthorized());
        var invalidado=autorizacoes.findById(codigo.getId()).orElseThrow().getInvalidadaEm(); assertNotNull(invalidado);
        alterarSituacao(auth,alvo.id(),true,1).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.versao").value(2));
        for(var antigo:new Cookie[]{a,b}) http.perform(get("/auth/me").secure(true).cookie(antigo)).andExpect(status().isUnauthorized());
        assertEquals(invalidado,autorizacoes.findById(codigo.getId()).orElseThrow().getInvalidadaEm());
        http.perform(get("/auth/me").secure(true).cookie(login(alvo))).andExpect(status().isOk());
    }
    @Test void situacaoSemMudancaNaoAtualizaVersoesNemAuditoria() throws Exception {
        var auth=login(admin); var alvo=conta(Papel.USER);
        alterarSituacao(auth,alvo.id(),true,0).andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(0));
        assertEquals(0,new JdbcTemplate(dataSource).queryForObject("select count(*) from auditoria_usuario",Integer.class));
        alterarSituacao(auth,alvo.id(),false,0).andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1));
        alterarSituacao(auth,alvo.id(),false,1).andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1));
        assertEquals(1,new JdbcTemplate(dataSource).queryForObject("select count(*) from auditoria_usuario",Integer.class));
    }
    @Test void recusasAuditaveisPersistemSemAlterarUsuario() throws Exception {
        var auth=login(admin);
        alterarPerfil(auth,admin.id(),"USER",0).andExpect(status().isConflict());
        alterarPerfil(auth,admin.id(),"ADMIN",99).andExpect(status().isConflict());
        var jdbc=new JdbcTemplate(dataSource);
        assertEquals(2,jdbc.queryForObject("select count(*) from auditoria_usuario where ator_id=? and alvo_id=? and resultado='RECUSADO'",Integer.class,admin.id(),admin.id()));
        assertEquals(0,jdbc.queryForObject("select count(*) from auditoria_usuario where resultado='SUCESSO'",Integer.class));
        http.perform(get("/usuarios/{id}",admin.id()).secure(true).cookie(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(0)).andExpect(jsonPath("$.papel").value("ADMIN"));
    }
    @Test void recusaAuditadaSobreviveAoRollbackDoChamador() throws Exception {
        var auth=login(admin);
        new org.springframework.transaction.support.TransactionTemplate(transacoes).executeWithoutResult(status -> {
            como(auth,()-> {
                var erro=assertThrows(com.projeto.services.exceptions.ApiException.class,()->servico.alterarSituacao(admin.id(),false,0L));
                assertEquals("ULTIMO_ADMIN_ATIVO",erro.getCode());
            });
            status.setRollbackOnly();
        });
        assertEquals(1,new JdbcTemplate(dataSource).queryForObject("select count(*) from auditoria_usuario where resultado='RECUSADO'",Integer.class));
        assertTrue(usuarios.findById(admin.id()).orElseThrow().isAtivo());
        assertEquals(0,usuarios.findById(admin.id()).orElseThrow().getVersao());
    }
    @Test void revalidaAtorDepoisDeEsperarLockDoAlvo() throws Exception {
        var alvo=conta(Papel.USER); var auth=login(admin);
        var pool=java.util.concurrent.Executors.newSingleThreadExecutor();
        try(var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("select id from usuario where id=? for update")) {
                statement.setLong(1,alvo.id()); statement.executeQuery().close();
            }
            var request=pool.submit(()->alterarPerfil(auth,alvo.id(),"ADMIN",0).andReturn().getResponse().getStatus());
            esperarBloqueio();
            var futuro=clock.instant().plusSeconds(601);
            org.mockito.Mockito.doReturn(futuro).when(clock).instant();
            org.mockito.Mockito.doReturn(futuro.toEpochMilli()).when(clock).millis();
            connection.commit();
            assertEquals(401,request.get(15,java.util.concurrent.TimeUnit.SECONDS));
            http.perform(get("/usuarios/{id}",alvo.id()).secure(true).cookie(login(admin))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.papel").value("USER")).andExpect(jsonPath("$.versao").value(0));
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(15,java.util.concurrent.TimeUnit.SECONDS)); }
    }
    @Test void endpointsExigemAdminECsrfSemAlterarEstado() throws Exception {
        var alvo=conta(Papel.USER); var user=login(alvo); var auth=login(admin);
        for(String caminho:new String[]{"/usuarios","/usuarios/"+alvo.id()}) {
            http.perform(get(caminho).secure(true)).andExpect(status().isUnauthorized());
            http.perform(get(caminho).secure(true).cookie(user)).andExpect(status().isForbidden());
        }
        for(String parte:new String[]{"perfil","situacao"}) {
            String corpo=parte.equals("perfil")?"{\"papel\":\"ADMIN\",\"versaoEsperada\":0}":"{\"ativo\":false,\"versaoEsperada\":0}";
            String caminho="/usuarios/"+alvo.id()+"/"+parte;
            http.perform(patch(caminho).secure(true).cookie(auth).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
            var csrf=csrf();
            http.perform(patch(caminho).secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN","invalido").contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
            http.perform(patch(caminho).secure(true).cookie(user,csrf).header("X-XSRF-TOKEN",csrf.getValue()).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
        }
        assertEquals(0,usuarios.findById(alvo.id()).orElseThrow().getVersao());
    }
    @Test void validaContratosENaoAceitaAtorOuSegredosNoCorpo() throws Exception {
        var auth=login(admin);
        for(String corpo:new String[]{"{}","{\"papel\":\"USER\"}","{\"papel\":\"ROOT\",\"versaoEsperada\":0}",
                "{\"papel\":\"USER\",\"versaoEsperada\":-1}","{\"papel\":\"USER\",\"versaoEsperada\":0,\"atorId\":1}",
                "{\"papel\":\"USER\",\"versaoEsperada\":0,\"senha\":\"proibida\"}"}) {
            var csrf=csrf();
            http.perform(patch("/usuarios/{id}/perfil",admin.id()).secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isBadRequest());
        }
        var csrf=csrf();
        http.perform(patch("/usuarios/{id}/situacao",admin.id()).secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"versaoEsperada\":0}")).andExpect(status().isBadRequest());
        http.perform(get("/usuarios/999999999").secure(true).cookie(auth)).andExpect(status().isNotFound());
        alterarPerfil(auth,999999999L,"USER",0).andExpect(status().isNotFound());
        http.perform(post("/usuarios").secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())).andExpect(status().isBadRequest());
        http.perform(delete("/usuarios/{id}",admin.id()).secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())).andExpect(status().isMethodNotAllowed());
    }
    @Test void servicoPublicoExigeContextoAutenticadoEAdmin() throws Exception {
        assertThrows(org.springframework.security.core.AuthenticationException.class,()->servico.listar());
        var alvo=conta(Papel.USER);
        como(login(alvo),()-> {
            assertThrows(org.springframework.security.access.AccessDeniedException.class,()->servico.listar());
            assertThrows(org.springframework.security.access.AccessDeniedException.class,()->servico.buscar(admin.id()));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,()->servico.alterarPerfil(admin.id(),Papel.USER,0L));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,()->servico.alterarSituacao(admin.id(),false,0L));
        });
        como(login(admin),()-> {
            assertEquals(2,servico.listar().size());
            assertEquals(admin.id(),servico.buscar(admin.id()).id());
            assertThrows(com.projeto.services.exceptions.ApiException.class,()->servico.alterarPerfil(null,Papel.USER,0L));
            assertThrows(com.projeto.services.exceptions.ApiException.class,()->servico.alterarSituacao(alvo.id(),null,0L));
        });
    }
    @Test void papelAdminSemIdentidadeVerificadaNaoDefineAtor() {
        var auth=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated("admin",null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            var erro=assertThrows(com.projeto.services.exceptions.ApiException.class,()->servico.listar());
            assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED,erro.getStatus());
        } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    }
    @Test void servicoRejeitaComandosInvalidosSemDependerDaValidacaoHttp() throws Exception {
        var alvo=conta(Papel.USER);
        como(login(admin),()-> {
            var comandos=java.util.List.<Runnable>of(
                    ()->servico.alterarPerfil(alvo.id(),null,0L),
                    ()->servico.alterarPerfil(alvo.id(),Papel.ADMIN,null),
                    ()->servico.alterarPerfil(alvo.id(),Papel.ADMIN,-1L),
                    ()->servico.alterarSituacao(null,false,0L),
                    ()->servico.alterarSituacao(alvo.id(),false,null),
                    ()->servico.alterarSituacao(alvo.id(),false,-1L));
            for(var comando:comandos) {
                var erro=assertThrows(com.projeto.services.exceptions.ApiException.class,comando::run);
                assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST,erro.getStatus());
            }
        });
        var usuario=usuarios.findById(alvo.id()).orElseThrow(); assertTrue(usuario.isAtivo()); assertEquals(Papel.USER,usuario.getPapel());
        assertEquals(0,usuario.getVersao());
        assertEquals(0,new JdbcTemplate(dataSource).queryForObject("select count(*) from auditoria_usuario",Integer.class));
    }
    @Test void servicoRejeitaContextoSemMetadadosEAuthorityContraditoria() throws Exception {
        como(login(admin),()-> {
            ((org.springframework.security.authentication.AbstractAuthenticationToken)org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()).setDetails(null);
            var erro=assertThrows(com.projeto.services.exceptions.ApiException.class,()->servico.listar());
            assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED,erro.getStatus());
        });
        var user=conta(Papel.USER);
        como(login(user),()-> {
            var contexto=org.springframework.security.core.context.SecurityContextHolder.getContext(); var anterior=contexto.getAuthentication();
            var contraditorio=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(anterior.getPrincipal(),null,
                    java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
            contraditorio.setDetails(anterior.getDetails()); contexto.setAuthentication(contraditorio);
            var erro=assertThrows(com.projeto.services.exceptions.ApiException.class,()->servico.listar());
            assertEquals(org.springframework.http.HttpStatus.FORBIDDEN,erro.getStatus());
        });
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
    void autoAlteracoesConcorrentesPreservamUmAdminOperacional(boolean bloquear) throws Exception {
        var outro=conta(Papel.ADMIN); var a=login(admin); var b=login(outro);
        var inicio=new java.util.concurrent.CountDownLatch(1);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var primeira=pool.submit(()->{inicio.await(); return (bloquear?alterarSituacao(a,admin.id(),false,0):alterarPerfil(a,admin.id(),"USER",0)).andReturn().getResponse().getStatus();});
            var segunda=pool.submit(()->{inicio.await(); return (bloquear?alterarSituacao(b,outro.id(),false,0):alterarPerfil(b,outro.id(),"USER",0)).andReturn().getResponse().getStatus();});
            inicio.countDown();
            assertEquals(java.util.Set.of(200,409),java.util.Set.of(primeira.get(15,java.util.concurrent.TimeUnit.SECONDS),segunda.get(15,java.util.concurrent.TimeUnit.SECONDS)));
            assertEquals(1,usuarios.countByPapelAndAtivoTrueAndCredencialDefinidaTrue(Papel.ADMIN));
        } finally {pool.shutdownNow(); assertTrue(pool.awaitTermination(15,java.util.concurrent.TimeUnit.SECONDS));}
    }
    @Test void comandosConcorrentesNaoSobrescrevemVersaoNemConfundemNoOp() throws Exception {
        var outro=conta(Papel.ADMIN); var alvo=conta(Papel.USER); var a=login(admin); var b=login(outro);
        var inicio=new java.util.concurrent.CountDownLatch(1); var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var primeira=pool.submit(()->{inicio.await(); return alterarPerfil(a,alvo.id(),"ADMIN",0).andReturn().getResponse().getStatus();});
            var segunda=pool.submit(()->{inicio.await(); return alterarPerfil(b,alvo.id(),"ADMIN",0).andReturn().getResponse().getStatus();});
            inicio.countDown();
            assertEquals(java.util.Set.of(200,409),java.util.Set.of(primeira.get(15,java.util.concurrent.TimeUnit.SECONDS),segunda.get(15,java.util.concurrent.TimeUnit.SECONDS)));
            assertEquals(1,usuarios.findById(alvo.id()).orElseThrow().getVersao());
            var jdbc=new JdbcTemplate(dataSource);
            assertEquals(1,jdbc.queryForObject("select count(*) from auditoria_usuario where resultado='SUCESSO'",Integer.class));
            assertEquals(1,jdbc.queryForObject("select count(*) from auditoria_usuario where resultado='RECUSADO'",Integer.class));
        } finally {pool.shutdownNow(); assertTrue(pool.awaitTermination(15,java.util.concurrent.TimeUnit.SECONDS));}
    }
    @Test void revalidaAtorBloqueadoEnquantoEsperaSingleton() throws Exception {
        var outro=conta(Papel.ADMIN); var alvo=conta(Papel.USER); var auth=login(admin);
        var pool=java.util.concurrent.Executors.newSingleThreadExecutor();
        try(var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.createStatement().executeQuery("select id from controle_seguranca for update").close();
            var request=pool.submit(()->alterarPerfil(auth,alvo.id(),"ADMIN",0).andReturn().getResponse().getStatus());
            esperarBloqueio();
            try(var statement=connection.prepareStatement("update usuario set ativo=false, versao_seguranca=versao_seguranca+1 where id=?")) {
                statement.setLong(1,admin.id()); statement.executeUpdate();
            }
            connection.commit(); assertEquals(401,request.get(15,java.util.concurrent.TimeUnit.SECONDS));
            http.perform(get("/usuarios/{id}",alvo.id()).secure(true).cookie(login(outro))).andExpect(status().isOk()).andExpect(jsonPath("$.papel").value("USER"));
        } finally {pool.shutdownNow(); assertTrue(pool.awaitTermination(15,java.util.concurrent.TimeUnit.SECONDS));}
    }
    @Test void autoRebaixamentoPermitidoComOutroAdminInvalidaCookieAntigo() throws Exception {
        conta(Papel.ADMIN); var auth=login(admin);
        alterarPerfil(auth,admin.id(),"USER",0).andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1));
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized());
        var novo=login(admin);
        http.perform(get("/usuarios").secure(true).cookie(novo)).andExpect(status().isForbidden());
        http.perform(get("/carteiras").secure(true).cookie(novo)).andExpect(status().isOk());
    }
    void como(Cookie cookie, Runnable teste) {
        var claims=jwt.validar(cookie.getValue()); var principal=sessoes.autenticar(claims);
        var auth=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal,null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+principal.papel())));
        auth.setDetails(claims);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {teste.run();} finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    }
    @Test void falhaNaAuditoriaReverteBloqueioVersaoEInvalidacaoDoCodigo() throws Exception {
        var alvo=conta(Papel.USER); var antigo=login(alvo); var auth=login(admin);
        var instante=OffsetDateTime.now(clock);
        var codigo=autorizacoes.saveAndFlush(new AutorizacaoRedefinicaoSenha(UUID.randomUUID(),usuarios.getReferenceById(alvo.id()),
                usuarios.getReferenceById(admin.id()),"b".repeat(64),MotivoRedefinicao.INICIAL,instante,instante.plusMinutes(15),5,null,null));
        var jdbc=new JdbcTemplate(dataSource);
        instalarFalhaAuditoria();
        try {
            var resposta=alterarSituacao(auth,alvo.id(),false,0).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("SEGURANCA_INDISPONIVEL")).andReturn().getResponse().getContentAsString();
            assertFalse(resposta.contains("insert into")); assertFalse(resposta.contains("SQLException"));
        } finally {removerFalhaAuditoria();}
        var usuario=usuarios.findById(alvo.id()).orElseThrow();
        assertTrue(usuario.isAtivo()); assertEquals(0,usuario.getVersao()); assertEquals(0,usuario.getVersaoSeguranca());
        assertNull(autorizacoes.findById(codigo.getId()).orElseThrow().getInvalidadaEm());
        assertEquals(0,jdbc.queryForObject("select count(*) from auditoria_usuario",Integer.class));
        http.perform(get("/auth/me").secure(true).cookie(antigo)).andExpect(status().isOk());
    }
    void instalarFalhaAuditoria() { new JdbcTemplate(dataSource).execute("create trigger falha_auditoria before insert on auditoria_usuario for each row call 'com.projeto.autenticacao.AdministracaoUsuariosHttpTest$FalhaAuditoria'"); }
    void removerFalhaAuditoria() { new JdbcTemplate(dataSource).execute("drop trigger falha_auditoria"); }
    public static class FalhaAuditoria implements org.h2.api.Trigger {
        @Override public void fire(java.sql.Connection connection,Object[] anterior,Object[] novo) throws java.sql.SQLException {
            throw new java.sql.SQLException("Falha isolada de persistência para verificar atomicidade", "45000");
        }
    }
    @Test void bloqueioNaoReescreveAutorizacoesConsumidasOuJaInvalidadas() throws Exception {
        var alvo=conta(Papel.USER); var auth=login(admin); var agora=OffsetDateTime.now(clock).minusMinutes(20);
        var consumida=autorizacoes.saveAndFlush(new AutorizacaoRedefinicaoSenha(UUID.randomUUID(),usuarios.getReferenceById(alvo.id()),usuarios.getReferenceById(admin.id()),
                "c".repeat(64),MotivoRedefinicao.INICIAL,agora,agora.plusMinutes(15),5,agora.plusMinutes(1),null));
        var invalidada=autorizacoes.saveAndFlush(new AutorizacaoRedefinicaoSenha(UUID.randomUUID(),usuarios.getReferenceById(alvo.id()),usuarios.getReferenceById(admin.id()),
                "d".repeat(64),MotivoRedefinicao.INICIAL,agora,agora.plusMinutes(15),5,null,agora.plusMinutes(1)));
        var expirada=autorizacoes.saveAndFlush(new AutorizacaoRedefinicaoSenha(UUID.randomUUID(),usuarios.getReferenceById(alvo.id()),usuarios.getReferenceById(admin.id()),
                "e".repeat(64),MotivoRedefinicao.INICIAL,agora,agora.plusMinutes(15),5,null,null));
        var invalidacaoOriginal=autorizacoes.findById(invalidada.getId()).orElseThrow().getInvalidadaEm();
        alterarSituacao(auth,alvo.id(),false,0).andExpect(status().isOk());
        assertNull(autorizacoes.findById(consumida.getId()).orElseThrow().getInvalidadaEm());
        assertEquals(invalidacaoOriginal,autorizacoes.findById(invalidada.getId()).orElseThrow().getInvalidadaEm());
        assertNotNull(autorizacoes.findById(expirada.getId()).orElseThrow().getInvalidadaEm());
    }
    @Test void loginEsperandoBloqueioNaoEmiteSessaoAutorizadaAposCommit() throws Exception {
        var alvo=conta(Papel.USER); var antigo=login(alvo); var auth=login(admin); var csrf=csrf();
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try(var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("select id from usuario where id=? for update")) {
                statement.setLong(1,alvo.id()); statement.executeQuery().close();
            }
            var bloqueio=pool.submit(()->alterarSituacao(auth,alvo.id(),false,0).andReturn().getResponse().getStatus());
            esperarBloqueio();
            var entrada=pool.submit(()->http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+alvo.nome()+"\",\"senha\":\""+alvo.senha()+"\"}"))
                    .andReturn().getResponse());
            esperarBloqueios(2); connection.commit();
            assertEquals(200,bloqueio.get(15,java.util.concurrent.TimeUnit.SECONDS));
            var resposta=entrada.get(15,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(401,resposta.getStatus()); assertNull(resposta.getCookie("GESTAOACOES_AUTH"));
            http.perform(get("/auth/me").secure(true).cookie(antigo)).andExpect(status().isUnauthorized());
        } finally {pool.shutdownNow(); assertTrue(pool.awaitTermination(15,java.util.concurrent.TimeUnit.SECONDS));}
    }
    void esperarBloqueio() {
        esperarBloqueios(1);
    }
    void esperarBloqueios(int quantidade) {
        var jdbc=new JdbcTemplate(dataSource); long limite=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while(jdbc.queryForObject("select count(*) from information_schema.sessions where blocker_id is not null",Integer.class)<quantidade && System.nanoTime()<limite)
            java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(10));
        assertTrue(jdbc.queryForObject("select count(*) from information_schema.sessions where blocker_id is not null",Integer.class)>=quantidade);
    }
    org.springframework.test.web.servlet.ResultActions alterarSituacao(Cookie auth,Long id,boolean ativo,long versao) throws Exception {
        var csrf=csrf();
        return http.perform(patch("/usuarios/{id}/situacao",id).secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":"+ativo+",\"versaoEsperada\":"+versao+"}"));
    }
    org.springframework.test.web.servlet.ResultActions alterarPerfil(Cookie auth,Long id,String papel,long versao) throws Exception {
        var csrf=csrf();
        return http.perform(patch("/usuarios/{id}/perfil",id).secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"papel\":\""+papel+"\",\"versaoEsperada\":"+versao+"}"));
    }
    Conta conta(Papel papel) {
        String nome="teste."+UUID.randomUUID(), senha=UUID.randomUUID().toString();
        var u=new Usuario(nome,papel,OffsetDateTime.now(clock));
        u.definirCredencialInicial(encoder.encode(senha),u.getCriadaEm()); usuarios.saveAndFlush(u);
        return new Conta(u.getId(),nome,senha);
    }
    Cookie csrf() throws Exception {
        return http.perform(get("/auth/csrf").secure(true)).andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
    Cookie login(Conta conta) throws Exception {
        var csrf=csrf();
        return http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+conta.nome()+"\",\"senha\":\""+conta.senha()+"\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("GESTAOACOES_AUTH");
    }
    record Conta(Long id,String nome,String senha) { @Override public String toString() {return "Conta[fixture protegida]";} }
}
