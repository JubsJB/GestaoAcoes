package com.projeto.autenticacao;

@AutenticacaoPostgresReal
class AutenticacaoPostgresAdministracaoRealTest extends AdministracaoUsuariosHttpTest {
    @org.springframework.beans.factory.annotation.Autowired com.projeto.security.BootstrapAdminService bootstrap;
    @org.springframework.beans.factory.annotation.Autowired jakarta.persistence.EntityManagerFactory emf;
    @org.junit.jupiter.api.BeforeEach @Override void preparar() {
        var instante=java.time.Instant.parse("2030-01-01T10:00:00Z");
        org.mockito.Mockito.doReturn(instante).when(clock).instant();org.mockito.Mockito.doReturn(instante.toEpochMilli()).when(clock).millis();
        super.preparar();
    }
    @Override void esperarBloqueios(int quantidade) { AutenticacaoPostgresExecucao.esperarBloqueios(dataSource,quantidade); }
    @Override void instalarFalhaAuditoria() { AutenticacaoPostgresExecucao.instalarFalha(dataSource,"auditoria_usuario","insert","falha_auditoria"); }
    @Override void removerFalhaAuditoria() { AutenticacaoPostgresExecucao.removerFalha(dataSource,"auditoria_usuario","falha_auditoria"); }
    @org.junit.jupiter.api.Test void bootstrapConcorrenteUsaSingletonReal()throws Exception {
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        for(String tabela:java.util.List.of("auditoria_usuario","autorizacao_redefinicao_senha","sessao_autenticacao","contador_abuso","usuario"))jdbc.update("delete from "+tabela);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try(var c=dataSource.getConnection()) {
            c.setAutoCommit(false);try(var s=c.createStatement()){s.executeQuery("select id from controle_seguranca where id=1 for update").close();}
            try {
                var a=pool.submit(()->bootstrap.executar("bootstrap.a",java.util.UUID.randomUUID().toString()));
                var b=pool.submit(()->bootstrap.executar("bootstrap.b",java.util.UUID.randomUUID().toString()));
                esperarBloqueios(2);c.commit();
                org.junit.jupiter.api.Assertions.assertNotEquals(a.get(15,java.util.concurrent.TimeUnit.SECONDS),b.get(15,java.util.concurrent.TimeUnit.SECONDS));
                org.junit.jupiter.api.Assertions.assertEquals(1,usuarios.countByPapelAndAtivoTrueAndCredencialDefinidaTrue(com.projeto.entities.autenticacao.Papel.ADMIN));
                org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("select count(*) from auditoria_usuario where acao='BOOTSTRAP'",Integer.class));
            } finally {c.rollback();}
        } finally {pool.shutdownNow();org.junit.jupiter.api.Assertions.assertTrue(pool.awaitTermination(15,java.util.concurrent.TimeUnit.SECONDS));}
        // Corrupção da fixture não pode recriar o singleton ou criar outra conta silenciosamente.
        jdbc.update("delete from controle_seguranca where id=1");
        try {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->bootstrap.executar("bootstrap.ausente",java.util.UUID.randomUUID().toString()));
            org.junit.jupiter.api.Assertions.assertEquals(1,usuarios.count());
            org.junit.jupiter.api.Assertions.assertEquals(0,jdbc.queryForObject("select count(*) from controle_seguranca",Integer.class));
        } finally {jdbc.update("insert into controle_seguranca(id) values(1)");}
    }
    @org.junit.jupiter.api.Test void jpaRejeitaCommitComVersaoObsoleta() {
        var alvo=conta(com.projeto.entities.autenticacao.Papel.USER);
        var a=emf.createEntityManager();var b=emf.createEntityManager();
        try {
            a.getTransaction().begin();b.getTransaction().begin();
            a.find(com.projeto.entities.autenticacao.Usuario.class,alvo.id()).setAtivo(false);
            b.find(com.projeto.entities.autenticacao.Usuario.class,alvo.id()).setAtivo(false);
            a.getTransaction().commit();
            var erro=org.junit.jupiter.api.Assertions.assertThrows(jakarta.persistence.RollbackException.class,()->b.getTransaction().commit());
            Throwable causa=erro;while(causa!=null && !(causa instanceof jakarta.persistence.OptimisticLockException))causa=causa.getCause();
            org.junit.jupiter.api.Assertions.assertInstanceOf(jakarta.persistence.OptimisticLockException.class,causa);
            org.junit.jupiter.api.Assertions.assertEquals(1,usuarios.findById(alvo.id()).orElseThrow().getVersao());
        } finally {if(a.getTransaction().isActive())a.getTransaction().rollback();if(b.getTransaction().isActive())b.getTransaction().rollback();a.close();b.close();}
    }
}
