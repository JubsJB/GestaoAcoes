package com.projeto.autenticacao;

import liquibase.*;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Contratos físicos: somente H2 em memória construído aqui, sem configuração externa. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutenticacaoMigrationTest {
    Connection connection;
    Liquibase liquibase;
    static final String INSTANTE = "TIMESTAMP WITH TIME ZONE '2026-10-30 08:00:00+00'";
    static final String MAIS_15 = "TIMESTAMP WITH TIME ZONE '2026-10-30 08:15:00+00'";
    static final String MAIS_4H = "TIMESTAMP WITH TIME ZONE '2026-10-30 12:00:00+00'";
    @BeforeAll void migrar() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:auth_" + UUID.randomUUID(), "sa", "");
        liquibase = new Liquibase("db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(),
                DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)));
        liquibase.update(new Contexts(), new LabelExpression());
    }
    @BeforeEach void transacao() throws Exception { connection.setAutoCommit(false); }
    @AfterEach void desfazerFixtures() throws Exception { connection.rollback(); connection.setAutoCommit(true); }
    @AfterAll void fechar() throws Exception { if (liquibase != null) liquibase.close(); else if (connection != null) connection.close(); }

    @Test void persisteUsuarioSemSenhaComPerfilEIdentidadeUnica() throws Exception {
        long id = usuario("aluno");
        try (var statement=connection.createStatement(); var row=statement.executeQuery("SELECT * FROM usuario WHERE id="+id)) {
            assertTrue(row.next()); assertEquals("USER",row.getString("papel"));
            assertNull(row.getString("senha_hash")); assertFalse(row.getBoolean("credencial_definida"));
            assertTrue(row.getBoolean("ativo")); assertEquals(0,row.getLong("versao_seguranca"));
        }
    }
    @Test void persisteSessaoComChaveDeUsuarioEPrazosUtc() throws Exception {
        long id=usuario("sessao"); UUID sid=UUID.randomUUID(); sessao(sid,id);
        try(var s=connection.createStatement();var row=s.executeQuery("SELECT * FROM sessao_autenticacao")) {
            assertTrue(row.next()); assertEquals(sid,row.getObject("sid",UUID.class));
            assertEquals(OffsetDateTime.parse("2026-10-30T12:00:00Z").toInstant(),
                    row.getObject("expira_em_absoluto",OffsetDateTime.class).toInstant());
            assertEquals(id,row.getLong("usuario_id")); assertNull(row.getObject("revogada_em"));
        }
    }
    @Test void criaEstruturasAuxiliaresSemSemearCredenciais() throws Exception {
        for(String table:List.of("CONTROLE_SEGURANCA","AUDITORIA_USUARIO","AUTORIZACAO_REDEFINICAO_SENHA","CONTADOR_ABUSO")) {
            try(var rows=connection.getMetaData().getTables(null,schemaFisico(),nomeFisico(table),null)){assertTrue(rows.next(),table);}
        }
        assertEquals(0,numero("SELECT COUNT(*) FROM usuario"));
        assertEquals(1,numero("SELECT COUNT(*) FROM controle_seguranca WHERE id=1"));
        assertThrows(SQLException.class,()->sql("INSERT INTO controle_seguranca(id) VALUES(2)"));
        assertThrows(SQLException.class,()->sql("INSERT INTO controle_seguranca(id) VALUES(1)"));
    }
    @ParameterizedTest @ValueSource(strings={"ab","ALUNO","a b","abc\n","ábc",""})
    void rejeitaUsernameNaoCanonicoMesmoPorSql(String username) { assertThrows(SQLException.class,()->usuario(username)); }
    @Test void rejeitaUsernameDuplicadoOuLongo() throws Exception {
        usuario("duplicado"); assertThrows(SQLException.class,()->usuario("duplicado"));
        assertThrows(SQLException.class,()->usuario("x".repeat(101)));
    }
    @ParameterizedTest @ValueSource(strings={"papel='ROOT'","papel=NULL","ativo=NULL","versao_seguranca=-1","versao=-1","atualizada_em=TIMESTAMP WITH TIME ZONE '2026-10-29 08:00:00+00'","credencial_definida=true","senha_hash='texto-puro'"})
    void rejeitaEstadoInconsistenteDeUsuario(String alteracao) throws Exception {
        long id=usuario("invalido"); assertThrows(SQLException.class,()->sql("UPDATE usuario SET "+alteracao+" WHERE id="+id));
    }
    @Test void aceitaHashEstruturalBCryptSemPersistirSenhaOriginal() throws Exception {
        long id=usuario("hashvalido");String hash="$2b$10$"+"A".repeat(53);
        try(var p=connection.prepareStatement("UPDATE usuario SET senha_hash=?,credencial_definida=true WHERE id=?")) {
            p.setString(1,hash);p.setLong(2,id);assertEquals(1,p.executeUpdate());
        }
    }
    @ParameterizedTest @ValueSource(strings={"ultima_atividade_em=expira_em_absoluto","ultima_atividade_em=TIMESTAMP WITH TIME ZONE '2026-10-29 08:00:00+00'","versao_emitida=-1","versao=-1","revogada_em=criada_em","motivo_revogacao='LOGOUT'"})
    void rejeitaSessaoInconsistente(String alteracao) throws Exception {
        sessao(UUID.randomUUID(),usuario("prazos"));assertThrows(SQLException.class,()->sql("UPDATE sessao_autenticacao SET "+alteracao));
    }
    @Test void exigeReferenciasEUnicidadeDaSessaoSemCascadeDeUsuario() throws Exception {
        assertThrows(SQLException.class,()->sessao(UUID.randomUUID(),999999));
        long id=usuario("referencias");UUID sid=UUID.randomUUID();sessao(sid,id);
        assertThrows(SQLException.class,()->sessao(sid,id));
        assertThrows(SQLException.class,()->sql("DELETE FROM usuario WHERE id="+id));
    }
    @Test void persisteSomenteVerificadorDaAutorizacao() throws Exception {
        long id=usuario("titular"),autor=usuario("administrador");autorizacao(id,autor);
        assertEquals(5,numero("SELECT tentativas_restantes FROM autorizacao_redefinicao_senha"));
        try(var rows=connection.getMetaData().getColumns(null,schemaFisico(),nomeFisico("AUTORIZACAO_REDEFINICAO_SENHA"),"%")) {
            Set<String> colunas=new HashSet<>();while(rows.next())colunas.add(rows.getString("COLUMN_NAME").toUpperCase(Locale.ROOT));
            assertTrue(colunas.contains("DIGEST"));assertFalse(colunas.contains("CODIGO"));assertFalse(colunas.contains("SENHA"));
        }
        assertThrows(SQLException.class,()->sql("DELETE FROM usuario WHERE id="+autor));
    }
    @ParameterizedTest @ValueSource(strings={"tentativas_restantes=-1","tentativas_restantes=6","versao=-1","digest='codigo-aberto'","motivo='OUTRO'","expira_em=criada_em","consumida_em=expira_em","consumida_em=criada_em,invalidada_em=criada_em"})
    void rejeitaAutorizacaoInconsistente(String alteracao) throws Exception {
        long id=usuario("codigo");autorizacao(id,id);assertThrows(SQLException.class,()->sql("UPDATE autorizacao_redefinicao_senha SET "+alteracao));
    }
    @Test void persisteAuditoriaMinimaComReferenciasSemCamposDeSegredo() throws Exception {
        long id=usuario("auditado");sql("INSERT INTO auditoria_usuario(ator_id,alvo_id,acao,resultado,registrada_em) VALUES("+id+","+id+",'CADASTRO','SUCESSO',"+INSTANTE+")");
        assertEquals(1,numero("SELECT COUNT(*) FROM auditoria_usuario"));
        assertThrows(SQLException.class,()->sql("DELETE FROM usuario WHERE id="+id));
        assertThrows(SQLException.class,()->sql("UPDATE auditoria_usuario SET acao='OUTRO'"));
        assertThrows(SQLException.class,()->sql("UPDATE auditoria_usuario SET resultado='OUTRO'"));
    }
    @Test void contadorAbusoTemJanelaUnicaENaoAceitaEstadoNegativo() throws Exception {
        String insert="INSERT INTO contador_abuso(tipo,chave_hash,inicio_janela,expira_em) VALUES('LOGIN_IP','"+"a".repeat(64)+"',"+INSTANTE+","+MAIS_15+")";
        sql(insert); assertEquals(0,numero("SELECT tentativas FROM contador_abuso"));
        assertThrows(SQLException.class,()->sql(insert));
        for(String change:List.of("tipo='OUTRO'","chave_hash='ip-aberto'","tentativas=-1","versao=-1","expira_em=inicio_janela"))
            assertThrows(SQLException.class,()->sql("UPDATE contador_abuso SET "+change));
    }
    @Test void migrationEhIncrementalIdempotenteERollbackNaoApagaAuditoria() throws Exception {
        connection.setAutoCommit(true);
        assertEquals(1,numero("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE id='009-create-autenticacao'"));
        liquibase.update(new Contexts(),new LabelExpression());
        assertEquals(1,numero("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE id='009-create-autenticacao'"));
        assertThrows(LiquibaseException.class,()->liquibase.rollback(1,new Contexts(),new LabelExpression()));
        assertEquals(1,numero("SELECT COUNT(*) FROM controle_seguranca"));
        assertEquals(1,numero("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE id='009-create-autenticacao'"));
    }
    String schemaFisico() { return "PUBLIC"; }
    String nomeFisico(String nome) { return nome; }
    long usuario(String username) throws Exception {
        try(var p=connection.prepareStatement("INSERT INTO usuario(username,papel,criada_em,atualizada_em) VALUES(?,'USER',"+INSTANTE+","+INSTANTE+")",Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1,username);p.executeUpdate();try(var keys=p.getGeneratedKeys()){assertTrue(keys.next());return keys.getLong(1);}
        }
    }
    void sessao(UUID sid,long usuario) throws Exception {
        try(var p=connection.prepareStatement("INSERT INTO sessao_autenticacao(sid,usuario_id,versao_emitida,criada_em,ultima_atividade_em,expira_em_absoluto,versao) VALUES(?,?,0,"+INSTANTE+","+INSTANTE+","+MAIS_4H+",0)")) {
            p.setObject(1,sid);p.setLong(2,usuario);p.executeUpdate();
        }
    }
    void autorizacao(long usuario,long autor) throws Exception {
        sql("INSERT INTO autorizacao_redefinicao_senha(id,usuario_id,autorizada_por_id,digest,motivo,criada_em,expira_em) VALUES('"+UUID.randomUUID()+"',"+usuario+","+autor+",'"+"a".repeat(64)+"','INICIAL',"+INSTANTE+","+MAIS_15+")");
    }
    void sql(String text) throws Exception { try(var s=connection.createStatement()){s.executeUpdate(text);} }
    int numero(String query) throws Exception { try(var s=connection.createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getInt(1);} }
}
