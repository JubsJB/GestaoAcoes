package com.projeto.autenticacao;

import java.sql.*;
import java.time.*;
import java.util.*;
import liquibase.*;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

/** Mesmas asserções físicas já aprovadas, contra PostgreSQL; cada erro restaura seu savepoint. */
@ExtendWith(AutenticacaoPostgresReal.Ciclo.class)
class AutenticacaoPostgresMigrationRealTest extends AutenticacaoMigrationTest {
    AutenticacaoPostgresExecucao run;
    @org.junit.jupiter.api.BeforeAll @Override void migrar() throws Exception {
        run=AutenticacaoPostgresExecucao.nova();
        connection=run.datasource().getConnection();
        liquibase=new Liquibase("db/changelog/db.changelog-master.yaml",new ClassLoaderResourceAccessor(),
                DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)));
        liquibase.update(new Contexts(),new LabelExpression());
        assertEquals("PostgreSQL",connection.getMetaData().getDatabaseProductName());
    }
    @org.junit.jupiter.api.AfterAll @Override void fechar() throws Exception { try {super.fechar();} finally {if(run!=null)run.close();} }
    @org.junit.jupiter.api.AfterEach @Override void desfazerFixtures() throws Exception { if(!connection.getAutoCommit())connection.rollback();connection.setAutoCommit(true); }
    @Override String schemaFisico(){return "public";}
    @Override String nomeFisico(String nome){return nome.toLowerCase(Locale.ROOT);}
    @FunctionalInterface interface Sql<T> { T executar() throws Exception; }
    <T> T isolado(Sql<T> bloco) throws Exception {
        var ponto=connection.setSavepoint();
        try { var valor=bloco.executar();connection.releaseSavepoint(ponto);return valor; }
        catch(SQLException erro) {
            connection.rollback(ponto);connection.releaseSavepoint(ponto);
            String state=erro.getSQLState();
            assertTrue(Set.of("23502","23503","23505","23514","22001").contains(state),"Violação deve ser de integridade, não transação abortada ou SQL inválido");
            throw new SQLException("Violação de integridade PostgreSQL comprovada",state);
        }
    }
    @Override void sql(String text)throws Exception{isolado(()->{super.sql(text);return null;});}
    @Override long usuario(String username)throws Exception{return isolado(()->super.usuario(username));}
    @Override void sessao(UUID sid,long usuario)throws Exception{isolado(()->{super.sessao(sid,usuario);return null;});}
    @Test @Override void migrationEhIncrementalIdempotenteERollbackNaoApagaAuditoria()throws Exception {
        long id=usuario("rollback.pg");sessao(UUID.randomUUID(),id);autorizacao(id,id);
        sql("insert into auditoria_usuario(ator_id,alvo_id,acao,resultado,registrada_em) values("+id+","+id+",'CADASTRO','SUCESSO',"+INSTANTE+")");
        sql("insert into contador_abuso(tipo,chave_hash,inicio_janela,expira_em) values('LOGIN_IP','"+"a".repeat(64)+"',"+INSTANTE+","+MAIS_15+")");
        connection.commit();connection.setAutoCommit(true);
        var antes=retratoAutenticacao(connection);
        try {
            liquibase.update(new Contexts(),new LabelExpression());
            assertEquals(1,numero("select count(*) from databasechangelog where id='009-create-autenticacao'"));
            var erro=assertThrows(liquibase.exception.LiquibaseException.class,()->liquibase.rollback(1,new Contexts(),new LabelExpression()));
            Throwable raiz=erro;while(raiz.getCause()!=null)raiz=raiz.getCause();
            assertInstanceOf(liquibase.change.core.StopChange.StopChangeException.class,raiz);
            try(var c=run.datasource().getConnection()) {
                assertTrue(antes.equals(retratoAutenticacao(c)),"Rollback bloqueado deve preservar registros de todas as tabelas de autenticação");
                try(var s=c.createStatement();var r=s.executeQuery("select count(*) from databasechangelog where id='009-create-autenticacao'")){assertTrue(r.next());assertEquals(1,r.getInt(1));}
                try(var s=c.createStatement();var r=s.executeQuery("select locked from databasechangeloglock")){assertTrue(r.next());assertFalse(r.getBoolean(1));}
            }
            AutenticacaoPostgresExecucao.registrar(run.execucao.plano().execucao().toString(),"ROLLBACK_STOPCHANGE_PRESERVA_DADOS_AUDITORIA_HISTORICO","PASS");
        } finally {
            try(var c=run.datasource().getConnection();var s=c.createStatement()) {
                c.setAutoCommit(false);
                for(String tabela:List.of("auditoria_usuario","autorizacao_redefinicao_senha","sessao_autenticacao","contador_abuso","usuario"))s.executeUpdate("delete from "+tabela);
                c.commit();
            }
        }
    }
    private Map<String,List<List<String>>> retratoAutenticacao(Connection c)throws Exception {
        var snapshot=new TreeMap<String,List<List<String>>>();
        for(String tabela:List.of("usuario","sessao_autenticacao","controle_seguranca","auditoria_usuario","autorizacao_redefinicao_senha","contador_abuso")) {
            var linhas=new ArrayList<List<String>>();
            try(var s=c.createStatement();var r=s.executeQuery("select * from "+tabela+" order by "+(tabela.equals("sessao_autenticacao")?"sid":"id"))) {
                while(r.next()){var linha=new ArrayList<String>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)linha.add(r.getString(i));linhas.add(linha);}
            }
            snapshot.put(tabela,linhas);
        }
        return snapshot;
    }
    @Test void catalogoPostgresqlConfirmaConstraintsIndicesTiposEPrecisao() throws Exception {
        var tabelas=Set.of("usuario","sessao_autenticacao","controle_seguranca","auditoria_usuario","autorizacao_redefinicao_senha","contador_abuso");
        var nomes=new HashSet<String>();int fks=0,checks=0,pks=0,unicos=0;
        try(var s=connection.createStatement();var rows=s.executeQuery("select c.conname,c.contype,c.confdeltype,c.convalidated,t.relname from pg_constraint c join pg_class t on t.oid=c.conrelid join pg_namespace n on n.oid=t.relnamespace where n.nspname='public'")) {
            while(rows.next()) if(tabelas.contains(rows.getString(5))) {
                nomes.add(rows.getString(1));assertTrue(rows.getBoolean(4));
                switch(rows.getString(2)){case "f"-> {fks++;assertEquals("r",rows.getString(3));} case "c"->checks++;case "p"->pks++;case "u"->unicos++;}
            }
        }
        assertEquals(5,fks);assertEquals(6,pks);assertEquals(2,unicos);assertEquals(19,checks);
        assertTrue(nomes.containsAll(Set.of("ck_usuario_username","ck_usuario_credencial","ck_controle_seguranca_singleton","uk_usuario_username","uk_contador_abuso_janela")));
        var indices=new HashSet<String>();
        try(var s=connection.createStatement();var r=s.executeQuery("select indexname,indexdef from pg_indexes where schemaname='public'")) {
            while(r.next()) if(r.getString(1).startsWith("idx_"))indices.add(r.getString(1)+r.getString(2).substring(r.getString(2).lastIndexOf('(')));
        }
        assertTrue(indices.containsAll(Set.of("idx_sessao_usuario_expiracao(usuario_id, expira_em_absoluto)","idx_auditoria_alvo_instante(alvo_id, registrada_em)","idx_redefinicao_usuario_expiracao(usuario_id, expira_em)","idx_contador_abuso_expiracao(expira_em)")));
        try(var s=connection.createStatement();var r=s.executeQuery("select data_type,datetime_precision from information_schema.columns where table_schema='public' and table_name='sessao_autenticacao' and column_name='criada_em'")){assertTrue(r.next());assertEquals("timestamp with time zone",r.getString(1));assertEquals(6,r.getInt(2));}
        var nullable=new HashSet<String>();var defaults=new HashMap<String,String>();
        try(var s=connection.createStatement();var r=s.executeQuery("select table_name,column_name,is_nullable,column_default,data_type from information_schema.columns where table_schema='public'")) {
            while(r.next()) if(tabelas.contains(r.getString(1))) {
                String coluna=r.getString(1)+"."+r.getString(2);
                if("YES".equals(r.getString(3)))nullable.add(coluna);
                if(r.getString(4)!=null)defaults.put(coluna,r.getString(4));
                if(Set.of("versao","versao_seguranca","versao_emitida").contains(r.getString(2)))assertEquals("bigint",r.getString(5));
                if(coluna.equals("sessao_autenticacao.sid")||coluna.equals("autorizacao_redefinicao_senha.id"))assertEquals("uuid",r.getString(5));
            }
        }
        assertEquals(Set.of("usuario.senha_hash","sessao_autenticacao.revogada_em","sessao_autenticacao.motivo_revogacao","auditoria_usuario.ator_id","autorizacao_redefinicao_senha.consumida_em","autorizacao_redefinicao_senha.invalidada_em"),nullable);
        assertEquals("true",defaults.get("usuario.ativo"));assertEquals("false",defaults.get("usuario.credencial_definida"));
        assertEquals("5",defaults.get("autorizacao_redefinicao_senha.tentativas_restantes"));
        for(String coluna:List.of("usuario.versao","usuario.versao_seguranca","autorizacao_redefinicao_senha.versao","contador_abuso.versao","contador_abuso.tentativas"))assertEquals("0",defaults.get(coluna));
    }
    @Test void timestampsPreservamInstanteComFusosDistintos()throws Exception {
        long id=usuario("fuso.pg");UUID sid=UUID.randomUUID();sessao(sid,id);
        for(String zona:List.of("UTC","America/Sao_Paulo","Asia/Kolkata")) {
            try(var s=connection.createStatement()){s.execute("SET LOCAL TIME ZONE '"+zona+"'");}
            try(var s=connection.createStatement();var r=s.executeQuery("select expira_em_absoluto from sessao_autenticacao where sid='"+sid+"'")){assertTrue(r.next());assertEquals(Instant.parse("2026-10-30T12:00:00Z"),r.getObject(1,OffsetDateTime.class).toInstant());}
        }
    }
}
