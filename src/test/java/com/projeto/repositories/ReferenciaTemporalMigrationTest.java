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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Executa o mesmo changelog em H2 e, quando configurado, em schema PostgreSQL isolado. */
class ReferenciaTemporalMigrationTest {
    private static final List<String> MIGRATIONS_TEMPORAIS = List.of(
            "001-create-corretora", "002-create-acao", "003-create-carteira",
            "004-create-operacao", "005-create-historico-cotacao", "006-create-snapshot-carteira",
            "007-add-referencia-temporal-cotacao");
    private static final String UNICIDADE_ANTIGA = "uk_historico_cotacao_acao_data_hora";

    @ParameterizedTest
    @ValueSource(strings = {"h2", "postgresql"})
    void migraLegadoSemFabricarTemporalidadeERollbackPreservaDados(String banco) throws Exception {
        noSchemaIsolado(banco, (connection, liquibase) -> {
            atualizarAte(connection, liquibase, "006-create-snapshot-carteira");
            executar(connection, "INSERT INTO acao(id,ticker,nome_empresa,mercado,moeda,cotacao_atual,data_hora_cotacao) "
                    + "VALUES(1,'AAPL','Apple','EUA','USD',250.123456,TIMESTAMP WITH TIME ZONE '2026-10-02 15:00:00+00')");
            executar(connection, "INSERT INTO historico_cotacao(id,acao_id,cotacao,data_hora_cotacao) "
                    + "VALUES(1,1,250.123456,TIMESTAMP WITH TIME ZONE '2026-10-02 15:00:00+00')");
            var dadosLegados = dadosTemporais(connection);
            var colunasLegadas = colunasTemporais(connection);
            atualizarAte(connection, liquibase, "007-add-referencia-temporal-cotacao");
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
            confirmarHistorico(connection, "007-add-referencia-temporal-cotacao");
            liquibase.rollback(1, new Contexts(), new LabelExpression());
            connection.setAutoCommit(true);
            confirmarHistorico(connection, "006-create-snapshot-carteira");
            assertEquals(colunasLegadas, colunasTemporais(connection));
            assertEquals(dadosLegados, dadosTemporais(connection));
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
            atualizarAte(connection, liquibase, "007-add-referencia-temporal-cotacao");
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
            atualizarAte(connection, liquibase, "007-add-referencia-temporal-cotacao");
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
            var dadosAntes = dadosTemporais(connection);
            var colunasAntes = colunasTemporais(connection);
            confirmarHistorico(connection, "007-add-referencia-temporal-cotacao");
            LiquibaseException erro = assertThrows(LiquibaseException.class,
                    () -> liquibase.rollback(1, new Contexts(), new LabelExpression()));
            SQLException violacao = null;
            for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
                if (causa instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                    violacao = sql;
                    break;
                }
            }
            assertNotNull(violacao, "Rollback deve falhar por unicidade (SQLState 23505)");
            String comando = erro.getMessage().toUpperCase(Locale.ROOT);
            assertTrue(comando.contains("ADD CONSTRAINT"), comando);
            assertTrue(comando.contains(UNICIDADE_ANTIGA.toUpperCase(Locale.ROOT)), comando);
            System.out.println("ROLLBACK_TEMPORAL_REJEITADO sqlState=" + violacao.getSQLState()
                    + " constraint=" + UNICIDADE_ANTIGA);
            if (!connection.getAutoCommit()) connection.rollback();
            connection.setAutoCommit(true);
            confirmarHistorico(connection, "007-add-referencia-temporal-cotacao");
            assertEquals(colunasAntes, colunasTemporais(connection));
            assertEquals(dadosAntes, dadosTemporais(connection));
            assertEquals(8, contar(connection, "historico_cotacao"));
            try (var statement = connection.createStatement();
                 var row = statement.executeQuery("SELECT referencia_tipo FROM historico_cotacao WHERE id=1")) {
                assertTrue(row.next());
                assertEquals("DATA", row.getString(1));
            }
        });
    }

    private List<String> identidadesAte(String migration) {
        int limite = MIGRATIONS_TEMPORAIS.indexOf(migration);
        assertTrue(limite >= 0, "Migration fora do escopo temporal: " + migration);
        return MIGRATIONS_TEMPORAIS.subList(0, limite + 1).stream()
                .map(id -> "db/changelog/changes/" + id + ".yaml::" + id + "::codex").toList();
    }

    private void atualizarAte(Connection connection, Liquibase liquibase, String migration) throws Exception {
        var esperadas = identidadesAte(migration);
        var declaradas = liquibase.getDatabaseChangeLog().getChangeSets().stream()
                .map(cs -> cs.getFilePath() + "::" + cs.getId() + "::" + cs.getAuthor()).toList();
        assertTrue(declaradas.size() >= esperadas.size());
        assertEquals(esperadas, declaradas.subList(0, esperadas.size()));
        var aplicadas = historico(connection);
        assertTrue(aplicadas.size() <= esperadas.size());
        assertEquals(esperadas.subList(0, aplicadas.size()), aplicadas);
        int pendentes = esperadas.size() - aplicadas.size();
        if (pendentes > 0) liquibase.update(pendentes, new Contexts(), new LabelExpression());
        connection.setAutoCommit(true);
        confirmarHistorico(connection, migration);
    }

    private void confirmarHistorico(Connection connection, String migration) throws SQLException {
        var aplicadas = historico(connection);
        // Igualdade da sequência completa comprova a última identidade e a ausência de 008.
        assertEquals(identidadesAte(migration), aplicadas);
        System.out.println("HISTORICO_TEMPORAL aplicado=" + aplicadas + " custodia008=false");
    }

    private List<String> historico(Connection connection) throws SQLException {
        try (var tabelas = connection.getMetaData().getTables(null, connection.getSchema(), "%", new String[]{"TABLE"})) {
            boolean existe = false;
            while (tabelas.next()) existe |= "DATABASECHANGELOG".equalsIgnoreCase(tabelas.getString("TABLE_NAME"));
            if (!existe) return List.of();
        }
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT FILENAME, ID, AUTHOR FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED")) {
            List<String> identidades = new ArrayList<>();
            while (rows.next()) identidades.add(rows.getString(1) + "::" + rows.getString(2) + "::" + rows.getString(3));
            return identidades;
        }
    }

    private Map<String, List<String>> colunasTemporais(Connection connection) throws SQLException {
        return Map.of("acao", colunas(connection, "acao"),
                "historico_cotacao", colunas(connection, "historico_cotacao"));
    }

    private List<String> colunas(Connection connection, String tabela) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT * FROM " + tabela + " WHERE 1=0")) {
            var metadata = rows.getMetaData();
            List<String> colunas = new ArrayList<>();
            for (int i = 1; i <= metadata.getColumnCount(); i++) colunas.add(metadata.getColumnName(i).toLowerCase(Locale.ROOT));
            return colunas;
        }
    }

    private Map<String, List<List<String>>> dadosTemporais(Connection connection) throws SQLException {
        return Map.of("acao", dados(connection, "acao"),
                "historico_cotacao", dados(connection, "historico_cotacao"));
    }

    private List<List<String>> dados(Connection connection, String tabela) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT * FROM " + tabela + " ORDER BY id")) {
            List<List<String>> dados = new ArrayList<>();
            while (rows.next()) {
                List<String> linha = new ArrayList<>();
                for (int i = 1; i <= rows.getMetaData().getColumnCount(); i++) linha.add(rows.getString(i));
                dados.add(linha);
            }
            return dados;
        }
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
