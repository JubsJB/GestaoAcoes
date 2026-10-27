package com.projeto.custodia.postgresql;

import java.sql.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Somente JDBC simulado: nenhuma conexão PostgreSQL ou Docker. */
class CustodiaPostgresIntegridade19Test {
    static final String NATUREZA="ck_movimentacao_custodia_natureza";
    static final String DIRECAO="ck_movimentacao_custodia_direcao";
    static PSQLException sql(String estado,String constraint) {
        return new PSQLException(new ServerErrorMessage("SERROR\0C"+estado+"\0Mpassword=segredo jdbc:postgresql://segredo\0n"+constraint+"\0\0"));
    }
    static void rejeicao(String esperado,SQLException falha) throws Throwable {
        var ds=mock(CustodiaPostgresDataSource.class);var c=mock(Connection.class);var s=mock(Statement.class);
        when(ds.getConnection()).thenReturn(c);when(c.createStatement()).thenReturn(s);when(s.executeUpdate(anyString())).thenReturn(2);
        try { CustodiaPostgresIntegridade19.rejeitar(ds,"23514",esperado,ignorada->{if(falha!=null)throw falha;}); }
        finally {verify(c).rollback();verify(c).close();}
    }
    @Test void naturezaInvalidaAceitaDirecaoQuandoAmbasAsConstraintsSaoVioladas() {
        assertDoesNotThrow(()->rejeicao(NATUREZA,sql("23514",DIRECAO)));
    }
    @Test void naturezaInvalidaContinuaAceitandoSuaConstraint() {
        assertDoesNotThrow(()->rejeicao(NATUREZA,sql("23514",NATUREZA)));
    }
    @ParameterizedTest @ValueSource(strings={"23505","23503","XXXXX"})
    void naturezaInvalidaExigeSqlState23514(String estado) {
        assertThrows(AssertionError.class,()->rejeicao(NATUREZA,sql(estado,DIRECAO)));
    }
    @ParameterizedTest @ValueSource(strings={"desconhecida","ck_movimentacao_custodia_natureza_extra","ck_movimentacao_custodia_quantidade_positiva"})
    void naturezaInvalidaRejeitaQualquerOutraConstraint(String constraint) {
        assertThrows(AssertionError.class,()->rejeicao(NATUREZA,sql("23514",constraint)));
    }
    @Test void ausenciaDeExcecaoSqlReprova() {
        assertThrows(AssertionError.class,()->rejeicao(NATUREZA,null));
    }
    @Test void direcaoNaoAceitaConstraintDeNatureza() {
        assertThrows(AssertionError.class,()->rejeicao(DIRECAO,sql("23514",NATUREZA)));
    }
    @ParameterizedTest @ValueSource(strings={"quantidade_positiva","ancora_nao_negativa","direcao","numerador_formato","denominador_formato"})
    void outrosChecksMantemIdentificacaoEspecifica(String nome) {
        String constraint="ck_movimentacao_custodia_"+nome;
        assertThrows(AssertionError.class,()->rejeicao(constraint,sql("23514",NATUREZA)));
        assertDoesNotThrow(()->rejeicao(constraint,sql("23514",constraint)));
    }
    @Test void mensagemBrutaNaoSubstituiIdentificadorEstruturado() {
        assertThrows(AssertionError.class,()->rejeicao(NATUREZA,new SQLException("constraint "+NATUREZA,"23514")));
    }
    @Test void falhaPorConstraintDesconhecidaNaoCarregaTextoExterno() {
        var erro=assertThrows(AssertionError.class,()->rejeicao(NATUREZA,sql("23514","password_segredo")));
        assertTrue(erro.toString().contains("categoria=IDENTIFICADOR_DIVERGENTE"));
        assertTrue(erro.toString().contains("constraint=NA"));
        assertFalse(erro.toString().contains("segredo"));assertNull(erro.getCause());assertEquals(0,erro.getSuppressed().length);
    }
}
