package com.projeto.repositories;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Executa o mesmo changelog em H2 e, quando configurado, em schema PostgreSQL isolado. */
class ReferenciaTemporalMigrationTest {
    @ParameterizedTest
    @ValueSource(strings = {"h2", "postgresql"})
    void migraLegadoSemFabricarTemporalidadeERollbackPreservaDados(String banco) throws Exception {
        noSchemaIsolado(banco, (connection, liquibase) -> {
            liquibase.update(6, new Contexts(), new LabelExpression());
            connection.setAutoCommit(true);
            executar(connection, "INSERT INTO acao(id,ticker,nome_empresa,mercado,moeda,cotacao_atual,data_hora_cotacao) "
                    + "VALUES(1,'AAPL','Apple','EUA','USD',250.123456,TIMESTAMP WITH TIME ZONE '2026-10-02 15:00:00+00')");
            executar(connection, "INSERT INTO historico_cotacao(id,acao_id,cotacao,data_hora_cotacao) "
                    + "VALUES(1,1,250.123456,TIMESTAMP WITH TIME ZONE '2026-10-02 15:00:00+00')");
            liquibase.update(new Contexts(), new LabelExpression());
            connection.setAutoCommit(true);
            for (String tabela : new String[]{"acao", "historico_cotacao"}) {
                try (var statement = connection.createStatement();
                     ResultSet row = statement.executeQuery("SELECT * FROM " + tabela + " WHERE id=1")) {
                    assertTrue(row.next());
                    assertEquals("DESCONHECIDA", row.getString("referencia_tipo"));
                    assertNull(row.getObject("referencia_instante"));
                    assertNull(row.getObject("referencia_data"));
                    assertNull(row.getObject("cotacao_obtida_em"));
                    assertEquals("250.123456", row.getBigDecimal(tabela.equals("acao") ? "cotacao_atual" : "cotacao").toPlainString());
                    assertEquals(java.time.Instant.parse("2026-10-02T15:00:00Z"),
                            row.getObject("data_hora_cotacao", java.time.OffsetDateTime.class).toInstant());
                }
            }
            liquibase.rollback(1, new Contexts(), new LabelExpression());
            connection.setAutoCommit(true);
            assertEquals(1, contar(connection, "acao"));
            assertEquals(1, contar(connection, "historico_cotacao"));
            assertThrows(SQLException.class, () -> executar(connection,
                    "INSERT INTO historico_cotacao(id,acao_id,cotacao,data_hora_cotacao) "
                            + "SELECT 2,acao_id,cotacao,data_hora_cotacao FROM historico_cotacao WHERE id=1"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "postgresql"})
    void checksRejeitamCombinacoesInvalidasENaoAceitamTipoNuloOuInvalido(String banco) throws Exception {
        noSchemaIsolado(banco, (connection, liquibase) -> {
            liquibase.update(new Contexts(), new LabelExpression());
            connection.setAutoCommit(true);
            inserirAcao(connection, 1, "DESCONHECIDA", "NULL", "NULL");
            for (String tipo : new String[]{"DESCONHECIDA", "DATA", "INSTANTE"}) {
                for (String instante : new String[]{"NULL", instante()}) {
                    for (String data : new String[]{"NULL", data()}) {
                        boolean valida = switch (tipo) {
                            case "INSTANTE" -> !instante.equals("NULL") && data.equals("NULL");
                            case "DATA" -> !data.equals("NULL") && instante.equals("NULL");
                            default -> instante.equals("NULL") && data.equals("NULL");
                        };
                        if (valida) continue;
                        assertThrows(SQLException.class, () -> inserirAcao(connection, 2, tipo, instante, data));
                        assertThrows(SQLException.class, () -> inserirHistorico(connection, 2, 1, tipo, instante, data));
                    }
                }
            }
            for (String valor : new String[]{"NULL", "'INVALIDA'"}) {
                for (String tabela : new String[]{"acao", "historico_cotacao"}) {
                    assertThrows(SQLException.class, () -> executar(connection,
                            tabela.equals("acao")
                                    ? "INSERT INTO acao(id,ticker,nome_empresa,mercado,moeda,cotacao_atual,data_hora_cotacao,referencia_tipo) VALUES(2,'BAD','Empresa','EUA','USD',10,CURRENT_TIMESTAMP," + valor + ")"
                                    : "INSERT INTO historico_cotacao(id,acao_id,cotacao,data_hora_cotacao,referencia_tipo) VALUES(2,1,10,CURRENT_TIMESTAMP," + valor + ")"));
                }
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "postgresql"})
    void unicidadeConhecidaPermiteNulosEDistingueDataDeInstante(String banco) throws Exception {
        noSchemaIsolado(banco, (connection, liquibase) -> {
            liquibase.update(new Contexts(), new LabelExpression());
            connection.setAutoCommit(true);
            inserirAcao(connection, 1, "DESCONHECIDA", "NULL", "NULL");
            inserirAcao(connection, 2, "DATA", "NULL", data());
            inserirAcao(connection, 3, "INSTANTE", instante(), "NULL");
            inserirHistorico(connection, 1, 1, "DATA", "NULL", data());
            inserirHistorico(connection, 2, 1, "INSTANTE", instante(), "NULL");
            inserirHistorico(connection, 3, 1, "DESCONHECIDA", "NULL", "NULL");
            inserirHistorico(connection, 4, 1, "DESCONHECIDA", "NULL", "NULL");
            inserirHistorico(connection, 5, 2, "DATA", "NULL", data());
            inserirHistorico(connection, 6, 2, "INSTANTE", instante(), "NULL");
            inserirHistorico(connection, 7, 1, "DATA", "NULL", "DATE '2026-10-03'");
            inserirHistorico(connection, 8, 1, "INSTANTE", "TIMESTAMP WITH TIME ZONE '2026-10-02 16:00:00+00'", "NULL");
            assertThrows(SQLException.class, () -> inserirHistorico(connection, 9, 1, "DATA", "NULL", data()));
            assertThrows(SQLException.class, () -> inserirHistorico(connection, 9, 1, "INSTANTE",
                    "TIMESTAMP WITH TIME ZONE '2026-10-02 12:00:00-03'", "NULL"));
            assertEquals(8, contar(connection, "historico_cotacao"));
            // O mesmo legado é permitido: não inventar referência para obter unicidade.
            assertThrows(LiquibaseException.class,
                    () -> liquibase.rollback(1, new Contexts(), new LabelExpression()));
            if (!connection.getAutoCommit()) connection.rollback();
            connection.setAutoCommit(true);
            assertEquals(8, contar(connection, "historico_cotacao"));
            try (var statement = connection.createStatement();
                 var row = statement.executeQuery("SELECT referencia_tipo FROM historico_cotacao WHERE id=1")) {
                assertTrue(row.next());
                assertEquals("DATA", row.getString(1));
            }
        });
    }

    private void noSchemaIsolado(String banco, Verificacao verificacao) throws Exception {
        String url = banco.equals("h2") ? "jdbc:h2:mem:" + UUID.randomUUID()
                : System.getProperty("temporal.test.postgresql.url");
        assumeTrue(url != null && !url.isBlank(), "PostgreSQL requer temporal.test.postgresql.url de ambiente isolado");
        String schema = "quote_temporal_" + UUID.randomUUID().toString().replace("-", "");
        if (banco.equals("h2")) schema = schema.toUpperCase(java.util.Locale.ROOT);
        try (Connection connection = DriverManager.getConnection(url,
                banco.equals("h2") ? "sa" : System.getProperty("temporal.test.postgresql.user", "postgres"),
                System.getProperty("temporal.test.postgresql.password", ""))) {
            executar(connection, "CREATE SCHEMA \"" + schema + "\"");
            connection.setSchema(schema);
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schema);
            Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(), database);
            try {
                verificacao.executar(connection, liquibase);
            } finally {
                if (!connection.getAutoCommit()) connection.rollback();
                connection.setAutoCommit(true);
                executar(connection, "DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }

    private void inserirAcao(Connection c, long id, String tipo, String instante, String data) throws SQLException {
        executar(c, "INSERT INTO acao(id,ticker,nome_empresa,mercado,moeda,cotacao_atual,data_hora_cotacao,referencia_tipo,referencia_instante,referencia_data) "
                + "VALUES(" + id + ",'TEST" + id + "','Empresa','EUA','USD',250.123456,CURRENT_TIMESTAMP,'" + tipo + "'," + instante + "," + data + ")");
    }

    private void inserirHistorico(Connection c, long id, long acaoId, String tipo, String instante, String data) throws SQLException {
        executar(c, "INSERT INTO historico_cotacao(id,acao_id,cotacao,data_hora_cotacao,referencia_tipo,referencia_instante,referencia_data) "
                + "VALUES(" + id + "," + acaoId + ",250.123456,TIMESTAMP WITH TIME ZONE '2026-10-02 15:00:00+00','" + tipo + "'," + instante + "," + data + ")");
    }

    private void executar(Connection c, String sql) throws SQLException {
        try (var statement = c.createStatement()) { statement.execute(sql); }
    }

    private int contar(Connection c, String tabela) throws SQLException {
        try (var statement = c.createStatement(); var row = statement.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            row.next();
            return row.getInt(1);
        }
    }

    private String instante() { return "TIMESTAMP WITH TIME ZONE '2026-10-02 15:00:00+00'"; }
    private String data() { return "DATE '2026-10-02'"; }

    @FunctionalInterface
    private interface Verificacao { void executar(Connection connection, Liquibase liquibase) throws Exception; }
}
