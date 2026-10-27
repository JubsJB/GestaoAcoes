package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2Test;
import jakarta.persistence.EntityManagerFactory;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 1: somente metadados; nenhuma escrita de domínio, replay ou rollback. */
@CustodiaH2Test
class CustodiaMigrationH2Test {
    @Autowired private DataSource dataSource;
    @Autowired private SpringLiquibase liquibase;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private ConfigurableApplicationContext context;

    // Um único cenário interrompe as verificações na primeira falha.
    @Test
    void validaMigrationCompletaEmH2Descartavel() throws Exception {
        assertEquals(1, context.getBeansOfType(DataSource.class).size());
        assertSame(dataSource, liquibase.getDataSource());
        assertSame(dataSource, ((EntityManagerFactoryInfo) entityManagerFactory).getDataSource());
        assertTrue(entityManagerFactory.isOpen()); // O refresh só termina após hbm2ddl validate.
        assertEquals("validate", entityManagerFactory.getProperties().get("hibernate.hbm2ddl.auto"));
        assertArrayEquals(new String[]{"custodia-h2"}, context.getEnvironment().getActiveProfiles());
        assertEquals("org.h2.Driver", ReflectionTestUtils.getField(dataSource, "driver").getClass().getName());
        String url = (String) ReflectionTestUtils.getField(dataSource, "url");
        String prefix = "jdbc:h2:mem:custodia_";
        assertNotNull(url);
        assertTrue(url.startsWith(prefix));
        assertEquals(prefix + UUID.fromString(url.substring(prefix.length())), url);
        try (var connection = dataSource.getConnection()) {
            assertEquals("H2", connection.getMetaData().getDatabaseProductName());
            assertEquals(url, connection.getMetaData().getURL());
        }
        for (String name : context.getBeanDefinitionNames()) {
            Class<?> type = context.getType(name);
            if (type != null) {
                assertFalse(type.getName().startsWith("com.projeto.services."), name);
                assertFalse(type.getName().startsWith("com.projeto.controllers."), name);
                assertFalse(type.getName().startsWith("com.projeto.integrations."), name);
                assertFalse(type.getName().contains("ServletWebServer"), name);
            }
        }

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> ids = List.of("001-create-corretora", "002-create-acao", "003-create-carteira",
                "004-create-operacao", "005-create-historico-cotacao", "006-create-snapshot-carteira",
                "007-add-referencia-temporal-cotacao", "008-create-movimentacoes-custodia",
                "008-add-integridade-movimentacoes-custodia");
        assertEquals(ids, jdbc.queryForList("SELECT ID FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED", String.class));
        assertEquals(9, jdbc.queryForObject("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE EXECTYPE = 'EXECUTED' "
                + "AND AUTHOR = 'codex' AND MD5SUM IS NOT NULL", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(DISTINCT DEPLOYMENT_ID) FROM DATABASECHANGELOG", Integer.class));
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), jdbc.queryForList(
                "SELECT ORDEREXECUTED FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM DATABASECHANGELOG WHERE "
                + "FILENAME = 'db/changelog/changes/008-create-movimentacoes-custodia.yaml'", Integer.class));

        colunas(jdbc, "MOVIMENTACAO_CUSTODIA", Map.ofEntries(
                Map.entry("ID", "BIGINT|NO|YES"), Map.entry("CARTEIRA_ID", "BIGINT|NO|NO"),
                Map.entry("ACAO_ID", "BIGINT|NO|NO"), Map.entry("NATUREZA", "CHARACTER VARYING(20)|NO|NO"),
                Map.entry("CORRETORA_ORIGEM_ID", "BIGINT|YES|NO"), Map.entry("CORRETORA_DESTINO_ID", "BIGINT|NO|NO"),
                Map.entry("QUANTIDADE", "NUMERIC(19,6)|NO|NO"), Map.entry("DATA_EFETIVA", "DATE|NO|NO"),
                Map.entry("DATA_REGISTRO", "TIMESTAMP WITH TIME ZONE|NO|NO"),
                Map.entry("ORDEM_FINANCEIRA_ANCORA", "INTEGER|NO|NO"),
                Map.entry("POSICAO_NUMERADOR", "CHARACTER VARYING(512)|NO|NO"),
                Map.entry("POSICAO_DENOMINADOR", "CHARACTER VARYING(512)|NO|NO")));
        colunas(jdbc, "ESTORNO_MOVIMENTACAO_CUSTODIA", Map.of(
                "ID", "BIGINT|NO|YES", "MOVIMENTACAO_ORIGINAL_ID", "BIGINT|NO|NO",
                "DATA_REGISTRO", "TIMESTAMP WITH TIME ZONE|NO|NO", "MOTIVO", "CHARACTER VARYING(1000)|YES|NO"));

        assertEquals(Set.of("FK_MOVIMENTACAO_CUSTODIA_CARTEIRA", "FK_MOVIMENTACAO_CUSTODIA_ACAO",
                "FK_MOVIMENTACAO_CUSTODIA_CORRETORA_ORIGEM", "FK_MOVIMENTACAO_CUSTODIA_CORRETORA_DESTINO",
                "FK_ESTORNO_MOVIMENTACAO_CUSTODIA_ORIGINAL"), constraints(jdbc, "FOREIGN KEY"));
        Map<String, String> destinos = Map.of("FK_MOVIMENTACAO_CUSTODIA_CARTEIRA", "CARTEIRA_ID|CARTEIRA",
                "FK_MOVIMENTACAO_CUSTODIA_ACAO", "ACAO_ID|ACAO",
                "FK_MOVIMENTACAO_CUSTODIA_CORRETORA_ORIGEM", "CORRETORA_ORIGEM_ID|CORRETORA",
                "FK_MOVIMENTACAO_CUSTODIA_CORRETORA_DESTINO", "CORRETORA_DESTINO_ID|CORRETORA",
                "FK_ESTORNO_MOVIMENTACAO_CUSTODIA_ORIGINAL", "MOVIMENTACAO_ORIGINAL_ID|MOVIMENTACAO_CUSTODIA");
        Map<String, String> encontradas = new TreeMap<>();
        jdbc.query("SELECT R.CONSTRAINT_NAME, K.COLUMN_NAME, T.TABLE_NAME, R.DELETE_RULE, R.UPDATE_RULE "
                + "FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS R "
                + "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE K ON K.CONSTRAINT_SCHEMA=R.CONSTRAINT_SCHEMA "
                + "AND K.CONSTRAINT_NAME=R.CONSTRAINT_NAME "
                + "JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS T ON T.CONSTRAINT_SCHEMA=R.UNIQUE_CONSTRAINT_SCHEMA "
                + "AND T.CONSTRAINT_NAME=R.UNIQUE_CONSTRAINT_NAME WHERE R.CONSTRAINT_SCHEMA='PUBLIC' "
                + "AND K.TABLE_NAME IN ('MOVIMENTACAO_CUSTODIA','ESTORNO_MOVIMENTACAO_CUSTODIA')", rs -> {
            assertEquals("RESTRICT", rs.getString("DELETE_RULE"));
            assertTrue(Set.of("RESTRICT", "NO ACTION").contains(rs.getString("UPDATE_RULE")));
            encontradas.put(rs.getString("CONSTRAINT_NAME"), rs.getString("COLUMN_NAME") + "|" + rs.getString("TABLE_NAME"));
        });
        assertEquals(destinos, encontradas);
        assertEquals(Set.of("UK_MOVIMENTACAO_CUSTODIA_POSICAO", "UK_ESTORNO_MOVIMENTACAO_CUSTODIA_ORIGINAL"),
                constraints(jdbc, "UNIQUE"));
        assertEquals(List.of("CARTEIRA_ID", "ACAO_ID", "DATA_EFETIVA", "ORDEM_FINANCEIRA_ANCORA",
                "POSICAO_NUMERADOR", "POSICAO_DENOMINADOR"), chave(jdbc, "UK_MOVIMENTACAO_CUSTODIA_POSICAO"));
        assertEquals(List.of("MOVIMENTACAO_ORIGINAL_ID"), chave(jdbc, "UK_ESTORNO_MOVIMENTACAO_CUSTODIA_ORIGINAL"));
        assertEquals(Set.of("CK_MOVIMENTACAO_CUSTODIA_NATUREZA", "CK_MOVIMENTACAO_CUSTODIA_QUANTIDADE_POSITIVA",
                "CK_MOVIMENTACAO_CUSTODIA_ANCORA_NAO_NEGATIVA", "CK_MOVIMENTACAO_CUSTODIA_DIRECAO",
                "CK_MOVIMENTACAO_CUSTODIA_NUMERADOR_FORMATO", "CK_MOVIMENTACAO_CUSTODIA_DENOMINADOR_FORMATO"),
                constraints(jdbc, "CHECK"));
        assertEquals(List.of("ID"), chave(jdbc, "PK_MOVIMENTACAO_CUSTODIA"));
        assertEquals(List.of("ID"), chave(jdbc, "PK_ESTORNO_MOVIMENTACAO_CUSTODIA"));
        indice(jdbc, "IDX_MOVIMENTACAO_CUSTODIA_CARTEIRA_REGISTRO_ID", List.of("CARTEIRA_ID", "DATA_REGISTRO", "ID"));
        indice(jdbc, "IDX_MOVIMENTACAO_CUSTODIA_CARTEIRA_ACAO_CRONOLOGIA",
                List.of("CARTEIRA_ID", "ACAO_ID", "DATA_EFETIVA", "ORDEM_FINANCEIRA_ANCORA"));

        // Schema financeiro vigente após 007: nenhuma coluna pode desaparecer com 008.
        Map<String, List<String>> legado = Map.of(
                "CORRETORA", List.of("ID", "CNPJ", "RAZAO_SOCIAL", "NOME_FANTASIA", "EMAIL", "TELEFONE", "CEP",
                        "LOGRADOURO", "NUMERO", "COMPLEMENTO", "BAIRRO", "CIDADE", "UF", "SITUACAO_CADASTRAL",
                        "VALIDADA_MERCADO_FINANCEIRO", "DATA_CADASTRO"),
                "ACAO", List.of("ID", "TICKER", "NOME_EMPRESA", "MERCADO", "MOEDA", "COTACAO_ATUAL", "DATA_HORA_COTACAO",
                        "REFERENCIA_TIPO", "REFERENCIA_INSTANTE", "REFERENCIA_DATA", "COTACAO_OBTIDA_EM"),
                "CARTEIRA", List.of("ID", "NOME", "DATA_CRIACAO"),
                "OPERACAO", List.of("ID", "CARTEIRA_ID", "ACAO_ID", "CORRETORA_ID", "TIPO", "QUANTIDADE",
                        "PRECO_UNITARIO", "DATA_OPERACAO", "ORDEM_NO_DIA", "VALOR_TOTAL"),
                "HISTORICO_COTACAO", List.of("ID", "ACAO_ID", "COTACAO", "DATA_HORA_COTACAO", "REFERENCIA_TIPO",
                        "REFERENCIA_INSTANTE", "REFERENCIA_DATA", "COTACAO_OBTIDA_EM"),
                "SNAPSHOT_CARTEIRA", List.of("ID", "CARTEIRA_ID", "DATA_HORA_SNAPSHOT"),
                "SNAPSHOT_CARTEIRA_MOEDA", List.of("ID", "SNAPSHOT_CARTEIRA_ID", "MOEDA", "PATRIMONIO_ATUAL"));
        for (var tabela : legado.entrySet()) {
            assertEquals(tabela.getValue(), jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_NAME=? ORDER BY ORDINAL_POSITION", String.class, tabela.getKey()));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela.getKey(), Integer.class));
        }
        colunas(jdbc, "OPERACAO", Map.ofEntries(Map.entry("ID", "BIGINT|NO|YES"),
                Map.entry("CARTEIRA_ID", "BIGINT|NO|NO"), Map.entry("ACAO_ID", "BIGINT|NO|NO"),
                Map.entry("CORRETORA_ID", "BIGINT|YES|NO"), Map.entry("TIPO", "CHARACTER VARYING(10)|NO|NO"),
                Map.entry("QUANTIDADE", "NUMERIC(19,6)|NO|NO"), Map.entry("PRECO_UNITARIO", "NUMERIC(19,6)|NO|NO"),
                Map.entry("DATA_OPERACAO", "DATE|NO|NO"), Map.entry("ORDEM_NO_DIA", "INTEGER|NO|NO"),
                Map.entry("VALOR_TOTAL", "NUMERIC(38,12)|NO|NO")));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM MOVIMENTACAO_CUSTODIA", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ESTORNO_MOVIMENTACAO_CUSTODIA", Integer.class));
    }

    private void colunas(JdbcTemplate jdbc, String tabela, Map<String, String> esperado) {
        Map<String, String> encontrado = new TreeMap<>();
        jdbc.query("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_NAME=?", rs -> {
            String tipo = rs.getString("DATA_TYPE");
            if (tipo.equals("CHARACTER VARYING")) tipo += "(" + rs.getInt("CHARACTER_MAXIMUM_LENGTH") + ")";
            if (tipo.equals("NUMERIC")) tipo += "(" + rs.getInt("NUMERIC_PRECISION") + "," + rs.getInt("NUMERIC_SCALE") + ")";
            encontrado.put(rs.getString("COLUMN_NAME"), tipo + "|" + rs.getString("IS_NULLABLE") + "|" + rs.getString("IS_IDENTITY"));
        }, tabela);
        assertEquals(esperado, encontrado, tabela);
    }

    private Set<String> constraints(JdbcTemplate jdbc, String tipo) {
        return Set.copyOf(jdbc.queryForList("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                + "WHERE CONSTRAINT_SCHEMA='PUBLIC' AND TABLE_NAME IN ('MOVIMENTACAO_CUSTODIA',"
                + "'ESTORNO_MOVIMENTACAO_CUSTODIA') AND CONSTRAINT_TYPE=?", String.class, tipo));
    }

    private List<String> chave(JdbcTemplate jdbc, String nome) {
        return jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE "
                + "WHERE CONSTRAINT_SCHEMA='PUBLIC' AND CONSTRAINT_NAME=? ORDER BY ORDINAL_POSITION", String.class, nome);
    }

    private void indice(JdbcTemplate jdbc, String nome, List<String> colunas) {
        assertEquals(colunas, jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS "
                + "WHERE INDEX_SCHEMA='PUBLIC' AND TABLE_NAME='MOVIMENTACAO_CUSTODIA' AND INDEX_NAME=? "
                + "ORDER BY ORDINAL_POSITION", String.class, nome));
    }
}
