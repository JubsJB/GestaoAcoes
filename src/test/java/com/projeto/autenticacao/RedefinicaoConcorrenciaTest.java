package com.projeto.autenticacao;

import com.projeto.entities.autenticacao.Papel;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RedefinicaoConcorrenciaTest extends RedefinicaoHttpSupport {
    @Test void loginConcorrenteNaoMantemSessaoAntigaValidaAposReset() throws Exception {
        var alvo=conta(Papel.USER);var codigo=codigo(login(admin),alvo,0);var senha=java.util.UUID.randomUUID().toString();
        var cookie=new java.util.concurrent.atomic.AtomicReference<jakarta.servlet.http.Cookie>();
        var resultados=paralelo(()-> {
            var r=postJson("/auth/login",mapper.writeValueAsString(java.util.Map.of("username",alvo.nome(),"senha",alvo.senha()))).andReturn().getResponse();
            cookie.set(r.getCookie("GESTAOACOES_AUTH"));return r.getStatus();
        },()->confirmar(alvo.nome(),codigo,senha,senha).andReturn().getResponse().getStatus());
        assertTrue(resultados.get(0)==200 || resultados.get(0)==401);assertEquals(204,resultados.get(1));
        if(cookie.get()!=null) http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/auth/me").secure(true).cookie(cookie.get()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        login(new Conta(alvo.id(),alvo.nome(),senha));
    }
    @Test void limiteDeIdentidadeTambemEhAtomicoEmRequestsConcorrentes() throws Exception {
        String nome="ausente."+java.util.UUID.randomUUID();
        for(int i=0;i<9;i++) confirmar(nome,"incorreto","curta","curta");
        var resultados=paralelo(()->confirmar(nome,"incorreto","curta","curta").andReturn().getResponse().getStatus(),()->confirmar(nome,"incorreto","curta","curta").andReturn().getResponse().getStatus());
        assertEquals(java.util.Set.of(400,429),new java.util.HashSet<>(resultados));
        assertEquals(10,jdbc().queryForObject("select tentativas from contador_abuso where tipo='REDEFINICAO_IDENTIDADE_IP'",Integer.class));
    }
    @Test void doisConsumosConcorrentesTrocamSenhaUmaUnicaVez() throws Exception {
        var alvo=conta(Papel.USER);var codigo=codigo(login(admin),alvo,0);var a=java.util.UUID.randomUUID().toString();var b=java.util.UUID.randomUUID().toString();
        var resultados=paralelo(()->confirmar(alvo.nome(),codigo,a,a).andReturn().getResponse().getStatus(),()->confirmar(alvo.nome(),codigo,b,b).andReturn().getResponse().getStatus());
        assertEquals(java.util.Set.of(204,400),new java.util.HashSet<>(resultados));
        login(new Conta(alvo.id(),alvo.nome(),resultados.get(0)==204?a:b));
        assertEquals(1,usuarios.findById(alvo.id()).orElseThrow().getVersaoSeguranca());
        assertEquals(1,jdbc().queryForObject("select count(*) from auditoria_usuario where acao='REDEFINICAO_SENHA' and resultado='SUCESSO'",Integer.class));
    }
    @Test void emissaoVersusConsumoRespeitaVersaoEOrdemDeCommit() throws Exception {
        var alvo=conta(Papel.USER);var auth=login(admin);var codigo=codigo(auth,alvo,0);var senha=java.util.UUID.randomUUID().toString();
        var resultados=paralelo(()->emitir(auth,alvo.id(),1).andReturn().getResponse().getStatus(),()->confirmar(alvo.nome(),codigo,senha,senha).andReturn().getResponse().getStatus());
        assertTrue(resultados.equals(java.util.List.of(200,400)) || resultados.equals(java.util.List.of(409,204)),resultados.toString());
        login(resultados.get(1)==204?new Conta(alvo.id(),alvo.nome(),senha):alvo);
    }
    @Test void resetVersusBloqueioNaoProduzSenhaNovaEmContaJaBloqueada() throws Exception {
        var alvo=conta(Papel.USER);var auth=login(admin);var codigo=codigo(auth,alvo,0);var senha=java.util.UUID.randomUUID().toString();
        var resultados=paralelo(()->situacao(auth,alvo.id(),false,1).andReturn().getResponse().getStatus(),()->confirmar(alvo.nome(),codigo,senha,senha).andReturn().getResponse().getStatus());
        assertTrue(resultados.equals(java.util.List.of(200,400)) || resultados.equals(java.util.List.of(409,204)),resultados.toString());
        assertEquals(resultados.get(0)!=200,usuarios.findById(alvo.id()).orElseThrow().isAtivo());
    }
    @Test void quintoErroVersusCodigoValidoNuncaReviveAutorizacao() throws Exception {
        var alvo=conta(Papel.USER);var codigo=codigo(login(admin),alvo,0);var senha=java.util.UUID.randomUUID().toString();
        for(int i=0;i<4;i++) confirmar(alvo.nome(),"incorreto",senha,senha).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        var resultados=paralelo(()->confirmar(alvo.nome(),"incorreto",senha,senha).andReturn().getResponse().getStatus(),()->confirmar(alvo.nome(),codigo,senha,senha).andReturn().getResponse().getStatus());
        assertEquals(400,resultados.get(0));assertTrue(resultados.get(1)==204 || resultados.get(1)==400);
        var registro=autorizacoes.findAll().get(0);assertNotEquals(registro.getConsumidaEm()!=null,registro.getInvalidadaEm()!=null);
        assertEquals(resultados.get(1)==204?1:0,usuarios.findById(alvo.id()).orElseThrow().getVersaoSeguranca());
    }
    @Test void cadastrosConcorrentesNormalizadosCriamSoUmaContaEAutorizacao() throws Exception {
        var auth=login(admin);String nome="concorrente."+java.util.UUID.randomUUID();
        var a=mapper.writeValueAsString(java.util.Map.of("username",nome,"papel","USER"));
        var b=mapper.writeValueAsString(java.util.Map.of("username"," "+nome.toUpperCase(java.util.Locale.ROOT)+" ","papel","ADMIN"));
        var resultados=paralelo(()->postJson("/usuarios",a,auth).andReturn().getResponse().getStatus(),()->postJson("/usuarios",b,auth).andReturn().getResponse().getStatus());
        assertEquals(java.util.Set.of(201,409),new java.util.HashSet<>(resultados));assertEquals(2,usuarios.count());assertEquals(1,autorizacoes.count());
    }
    @Test void consumoReavaliaExpiracaoDepoisDeEsperarUsuario() throws Exception {
        var alvo=conta(Papel.USER);var codigo=codigo(login(admin),alvo,0);var senha=java.util.UUID.randomUUID().toString();
        var pool=Executors.newSingleThreadExecutor();
        try(var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("select id from usuario where id=? for update")){statement.setLong(1,alvo.id());statement.executeQuery().close();}
            var request=pool.submit(()->confirmar(alvo.nome(),codigo,senha,senha).andReturn().getResponse().getStatus());
            esperarBloqueio();quando(inicio.plusSeconds(900));connection.commit();
            assertEquals(400,request.get(15,TimeUnit.SECONDS));assertNull(autorizacoes.findAll().get(0).getConsumidaEm());
            assertEquals(0,usuarios.findById(alvo.id()).orElseThrow().getVersaoSeguranca());login(alvo);
        } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
    }
    java.util.List<Integer> paralelo(Callable<Integer> a,Callable<Integer> b) throws Exception {
        var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
        try {
            var primeira=pool.submit(()->{inicio.await();return a.call();});var segunda=pool.submit(()->{inicio.await();return b.call();});inicio.countDown();
            return java.util.List.of(primeira.get(20,TimeUnit.SECONDS),segunda.get(20,TimeUnit.SECONDS));
        } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
    }
    @Test void emissaoRevalidaAtorAposEsperarAutorizacaoPendente() throws Exception {
        var alvo=conta(Papel.USER);var auth=login(admin);codigo(auth,alvo,0);
        var id=autorizacoes.findAll().get(0).getId();var pool=Executors.newSingleThreadExecutor();
        try(var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("select id from autorizacao_redefinicao_senha where id=? for update")) {
                statement.setObject(1,id);statement.executeQuery().close();
            }
            var request=pool.submit(()->emitir(auth,alvo.id(),1).andReturn().getResponse().getStatus());
            esperarBloqueio();quando(inicio.plusSeconds(601));connection.commit();
            assertEquals(401,request.get(15,TimeUnit.SECONDS));
            assertEquals(1,autorizacoes.count());assertNull(autorizacoes.findById(id).orElseThrow().getInvalidadaEm());
            assertEquals(1,usuarios.findById(alvo.id()).orElseThrow().getVersao());
        } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
    }
    void esperarBloqueio() {
        long limite=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(jdbc().queryForObject("select count(*) from information_schema.sessions where blocker_id is not null",Integer.class)==0 && System.nanoTime()<limite)
            java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        assertTrue(jdbc().queryForObject("select count(*) from information_schema.sessions where blocker_id is not null",Integer.class)>0);
    }
}
