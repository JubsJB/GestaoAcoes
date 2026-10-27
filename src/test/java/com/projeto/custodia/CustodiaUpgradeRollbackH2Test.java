package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2UpgradeTest;
import com.projeto.custodia.support.CustodiaH2UpgradeTestConfiguration;
import jakarta.persistence.EntityManagerFactory;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.change.core.StopChange;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 5.1: um cenário sequencial; a primeira falha inesperada impede as etapas seguintes. */
@CustodiaH2UpgradeTest
class CustodiaUpgradeRollbackH2Test {
    private static final String MASTER = "db/changelog/db.changelog-master.yaml";
    private static final String ARQUIVO_008 = "db/changelog/changes/008-create-movimentacoes-custodia.yaml";
    private static final List<String> LEGADO = List.of("CORRETORA", "ACAO", "CARTEIRA", "OPERACAO",
            "HISTORICO_COTACAO", "SNAPSHOT_CARTEIRA", "SNAPSHOT_CARTEIRA_MOEDA");
    private static final List<String> CUSTODIA = List.of("MOVIMENTACAO_CUSTODIA", "ESTORNO_MOVIMENTACAO_CUSTODIA");
    private static final List<String> IDS = List.of("001-create-corretora", "002-create-acao", "003-create-carteira",
            "004-create-operacao", "005-create-historico-cotacao", "006-create-snapshot-carteira",
            "007-add-referencia-temporal-cotacao", "008-create-movimentacoes-custodia",
            "008-add-integridade-movimentacoes-custodia");
    @Autowired private DataSource dataSource;
    @Autowired private ConfigurableApplicationContext context;
    @Autowired private CustodiaH2UpgradeTestConfiguration configuracao;

    @Test
    void atualizaLegadoPreservandoDadosEImpedeRollbackSemReversaoParcial() throws Exception {
        confirmaIsolamentoSemInicializacaoAutomatica();
        Retrato antesRollback;
        List<List<String>> historicoCompleto;
        try (Connection c = dataSource.getConnection();
                ClassLoaderResourceAccessor recursos = new ClassLoaderResourceAccessor()) {
            confirmaConexao(c);
            assertTrue(tabelas(c).isEmpty(), "Nenhuma migration pode executar automaticamente");
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            assertEquals("h2", database.getShortName());
            database.setDefaultSchemaName("PUBLIC");
            try (Liquibase liquibase = new Liquibase(MASTER, recursos, database)) {
                assertEquals(IDS, liquibase.getDatabaseChangeLog().getChangeSets().stream().map(cs -> cs.getId()).toList());
                confirmaStopExplicito(liquibase);
                liquibase.update(7, new Contexts(), new LabelExpression());
                c.setAutoCommit(true);
                assertEquals(IDS.subList(0, 7), ids(c));
                assertEquals(Set.copyOf(LEGADO), tabelasDominio(c));
                List<List<String>> historicoLegado = historico(c);
                inserirFinanceiro(c);
                for (String tabela : LEGADO) {
                    assertEquals(tabela.equals("OPERACAO") ? 4 : 2,
                            consulta(c, "SELECT * FROM " + tabela + " ORDER BY ID").size(), tabela);
                }
                Retrato financeiroAntes = retrato(c, LEGADO);
                System.out.println("UPGRADE etapa001a007 executados=" + ids(c) + " linhasFinanceiras=16");

                assertEquals(IDS.subList(7, 9), liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression())
                        .stream().map(cs -> cs.getId()).toList());
                liquibase.update(new Contexts(), new LabelExpression());
                c.setAutoCommit(true);
                assertEquals(IDS, ids(c));
                historicoCompleto = historico(c);
                assertEquals(historicoLegado, historicoCompleto.subList(0, 7));
                assertEquals(financeiroAntes, retrato(c, LEGADO), "Upgrade não pode alterar dados ou estruturas financeiras");
                assertEquals(2, consulta(c, "SELECT ID FROM DATABASECHANGELOG WHERE FILENAME='" + ARQUIVO_008
                        + "' AND EXECTYPE='EXECUTED' AND AUTHOR='codex' AND MD5SUM IS NOT NULL").size());
                assertEquals(9, consulta(c, "SELECT ID FROM DATABASECHANGELOG WHERE EXECTYPE='EXECUTED'").size());
                assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"),
                        consulta(c, "SELECT ORDEREXECUTED FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED")
                                .stream().map(row -> row.get(0)).toList());
                confirmaEstruturas008(c);
                System.out.println("UPGRADE etapa008 novos=" + IDS.subList(7, 9) + " financeiroPreservado=true");

                List<String> todas = todasAsTabelasDominio();
                Retrato antesSegundaAtualizacao = retrato(c, todas);
                liquibase.update(new Contexts(), new LabelExpression());
                c.setAutoCommit(true);
                assertEquals(historicoCompleto, historico(c));
                assertEquals(antesSegundaAtualizacao, retrato(c, todas));
                assertTrue(liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression()).isEmpty());
                System.out.println("UPGRADE segundaAtualizacao novos=0 historicoEDadosInalterados=true");

                // Somente após comprovar os nove changeSets e o schema completo.
                var validador = configuracao.prepararValidacaoHibernate();
                try {
                    validador.afterPropertiesSet();
                    EntityManagerFactory emf = validador.getObject();
                    assertNotNull(emf);
                    assertTrue(emf.isOpen());
                    assertSame(dataSource, ((EntityManagerFactoryInfo) emf).getDataSource());
                    assertEquals("validate", emf.getProperties().get("hibernate.hbm2ddl.auto"));
                } finally {
                    validador.destroy();
                }
                assertEquals(antesSegundaAtualizacao, retrato(c, todas), "Hibernate não pode criar ou alterar estruturas/dados");
                assertEquals(historicoCompleto, historico(c));
                System.out.println("HIBERNATE validate=sucesso estruturaEDadosInalterados=true factoryDestruida=true");

                // Estado isolado equivalente para rollback: nenhum teste ou consumidor concorrente usa este banco.
                inserirCustodia(c);
                antesRollback = retrato(c, todas);
                assertEquals(1, antesRollback.dados().get("MOVIMENTACAO_CUSTODIA").size());
                assertEquals(1, antesRollback.dados().get("ESTORNO_MOVIMENTACAO_CUSTODIA").size());
                assertEquals(IDS, ids(c));
                confirmaStopExplicito(liquibase);
                // Exatamente os últimos dois changeSets são de 008; não direcionar rollback a 001–007.
                LiquibaseException erro = assertThrows(LiquibaseException.class,
                        () -> liquibase.rollback(2, new Contexts(), new LabelExpression()));
                Throwable causa = erro;
                while (causa.getCause() != null && causa.getCause() != causa) causa = causa.getCause();
                assertInstanceOf(StopChange.StopChangeException.class, causa);
                assertTrue(causa.getMessage().contains("Rollback das constraints de custódia bloqueado"), causa.toString());
                System.out.println("ROLLBACK solicitado=2 bloqueadoPor=" + causa.getClass().getName()
                        + " mensagem=" + causa.getMessage());
                // Nenhuma consulta posterior nesta conexão após a falha esperada; fechar os recursos primeiro.
            }
        }
        try (Connection verificacao = dataSource.getConnection()) {
            confirmaConexao(verificacao);
            assertEquals(antesRollback, retrato(verificacao, todasAsTabelasDominio()));
            assertEquals(historicoCompleto, historico(verificacao));
            confirmaEstruturas008(verificacao);
            assertEquals(List.of(List.of("FALSE")), consulta(verificacao,
                    "SELECT LOCKED FROM DATABASECHANGELOGLOCK ORDER BY ID"));
            System.out.println("ROLLBACK verificacaoEmNovaConexao dadosEsquemaEHistoricoPreservados=true lockLiberado=true");
        }
    }

    private void confirmaIsolamentoSemInicializacaoAutomatica() throws Exception {
        assertEquals("2.5.250", org.h2.engine.Constants.class.getField("VERSION").get(null));
        assertTrue(getClass().isAnnotationPresent(CustodiaH2UpgradeTest.class));
        assertArrayEquals(new String[]{"custodia-h2"}, context.getEnvironment().getActiveProfiles());
        assertEquals(1, context.getBeansOfType(DataSource.class).size());
        assertTrue(context.getBeansOfType(SpringLiquibase.class).isEmpty());
        assertTrue(context.getBeansOfType(EntityManagerFactory.class).isEmpty());
        assertSame(dataSource, ReflectionTestUtils.getField(configuracao, "dataSource"));
        assertEquals("org.h2.Driver", ReflectionTestUtils.getField(dataSource, "driver").getClass().getName());
        assertEquals(Map.of("user", "sa", "password", ""), ReflectionTestUtils.getField(dataSource, "credentials"));
        assertNull(ReflectionTestUtils.getField(dataSource, "keeper"), "Sem conexão durante o refresh");
        String url = (String) ReflectionTestUtils.getField(dataSource, "url");
        String prefix = "jdbc:h2:mem:custodia_";
        assertNotNull(url);
        assertTrue(url.startsWith(prefix));
        assertEquals(prefix + UUID.fromString(url.substring(prefix.length())), url);
    }

    private void confirmaConexao(Connection c) throws SQLException {
        assertEquals("H2", c.getMetaData().getDatabaseProductName());
        assertEquals(ReflectionTestUtils.getField(dataSource, "url"), c.getMetaData().getURL());
        assertFalse(c.isClosed());
    }

    private void confirmaStopExplicito(Liquibase liquibase) throws LiquibaseException {
        var novos = liquibase.getDatabaseChangeLog().getChangeSets().subList(7, 9);
        for (var changeSet : novos) {
            assertEquals(ARQUIVO_008, changeSet.getFilePath());
            assertTrue(changeSet.hasCustomRollbackChanges());
            assertEquals(1, changeSet.getRollback().getChanges().size());
            var stop = assertInstanceOf(StopChange.class, changeSet.getRollback().getChanges().get(0));
            assertTrue(stop.getMessage().contains("bloqueado"));
        }
    }

    private List<String> ids(Connection c) throws SQLException {
        return consulta(c, "SELECT ID FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED").stream().map(row -> row.get(0)).toList();
    }

    private List<List<String>> historico(Connection c) throws SQLException {
        return consulta(c, "SELECT * FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED");
    }

    private Set<String> tabelas(Connection c) throws SQLException {
        return consulta(c, "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'")
                .stream().map(row -> row.get(0)).collect(Collectors.toSet());
    }

    private Set<String> tabelasDominio(Connection c) throws SQLException {
        return tabelas(c).stream().filter(t -> !t.startsWith("DATABASECHANGELOG")).collect(Collectors.toSet());
    }

    private List<String> todasAsTabelasDominio() {
        List<String> todas = new ArrayList<>(LEGADO);
        todas.addAll(CUSTODIA);
        return List.copyOf(todas);
    }

    private void confirmaEstruturas008(Connection c) throws SQLException {
        assertEquals(Set.copyOf(todasAsTabelasDominio()), tabelasDominio(c));
        String filtro = "TABLE_SCHEMA='PUBLIC' AND TABLE_NAME IN ('MOVIMENTACAO_CUSTODIA','ESTORNO_MOVIMENTACAO_CUSTODIA')";
        Set<String> nomes = consulta(c, "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE " + filtro)
                .stream().map(row -> row.get(0)).collect(Collectors.toSet());
        assertEquals(Set.of("PK_MOVIMENTACAO_CUSTODIA", "PK_ESTORNO_MOVIMENTACAO_CUSTODIA",
                "FK_MOVIMENTACAO_CUSTODIA_CARTEIRA", "FK_MOVIMENTACAO_CUSTODIA_ACAO",
                "FK_MOVIMENTACAO_CUSTODIA_CORRETORA_ORIGEM", "FK_MOVIMENTACAO_CUSTODIA_CORRETORA_DESTINO",
                "FK_ESTORNO_MOVIMENTACAO_CUSTODIA_ORIGINAL", "UK_MOVIMENTACAO_CUSTODIA_POSICAO",
                "UK_ESTORNO_MOVIMENTACAO_CUSTODIA_ORIGINAL", "CK_MOVIMENTACAO_CUSTODIA_NATUREZA",
                "CK_MOVIMENTACAO_CUSTODIA_QUANTIDADE_POSITIVA", "CK_MOVIMENTACAO_CUSTODIA_ANCORA_NAO_NEGATIVA",
                "CK_MOVIMENTACAO_CUSTODIA_DIRECAO", "CK_MOVIMENTACAO_CUSTODIA_NUMERADOR_FORMATO",
                "CK_MOVIMENTACAO_CUSTODIA_DENOMINADOR_FORMATO"), nomes);
        assertEquals(List.of(List.of("CARTEIRA_ID"), List.of("DATA_REGISTRO"), List.of("ID")), consulta(c,
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS WHERE INDEX_SCHEMA='PUBLIC' "
                        + "AND INDEX_NAME='IDX_MOVIMENTACAO_CUSTODIA_CARTEIRA_REGISTRO_ID' ORDER BY ORDINAL_POSITION"));
        assertEquals(List.of(List.of("CARTEIRA_ID"), List.of("ACAO_ID"), List.of("DATA_EFETIVA"),
                List.of("ORDEM_FINANCEIRA_ANCORA")), consulta(c,
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS WHERE INDEX_SCHEMA='PUBLIC' "
                        + "AND INDEX_NAME='IDX_MOVIMENTACAO_CUSTODIA_CARTEIRA_ACAO_CRONOLOGIA' ORDER BY ORDINAL_POSITION"));
    }

    private Retrato retrato(Connection c, List<String> tabelas) throws SQLException {
        Map<String, List<List<String>>> dados = new LinkedHashMap<>();
        for (String tabela : tabelas) dados.put(tabela, consulta(c, "SELECT * FROM " + tabela + " ORDER BY ID"));
        String nomes = tabelas.stream().map(t -> "'" + t + "'").collect(Collectors.joining(","));
        String filtro = "TABLE_SCHEMA='PUBLIC' AND TABLE_NAME IN (" + nomes + ")";
        String constraints = "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE " + filtro;
        Map<String, List<List<String>>> estrutura = new LinkedHashMap<>();
        estrutura.put("tabelas", consulta(c, "SELECT TABLE_NAME,TABLE_TYPE,STORAGE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE "
                + filtro + " ORDER BY TABLE_NAME"));
        estrutura.put("colunas", consulta(c, "SELECT TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION,DATA_TYPE,"
                + "CHARACTER_MAXIMUM_LENGTH,NUMERIC_PRECISION,NUMERIC_SCALE,DATETIME_PRECISION,IS_NULLABLE,COLUMN_DEFAULT,"
                + "IS_IDENTITY,IDENTITY_GENERATION,IDENTITY_START,IDENTITY_INCREMENT,IDENTITY_BASE "
                + "FROM INFORMATION_SCHEMA.COLUMNS WHERE " + filtro + " ORDER BY TABLE_NAME,ORDINAL_POSITION"));
        estrutura.put("constraints", consulta(c, "SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE " + filtro
                + " ORDER BY TABLE_NAME,CONSTRAINT_NAME"));
        estrutura.put("chaves", consulta(c, "SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE " + filtro
                + " ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION"));
        estrutura.put("checks", consulta(c, "SELECT * FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='PUBLIC' "
                + "AND CONSTRAINT_NAME IN (" + constraints + ") ORDER BY CONSTRAINT_NAME"));
        estrutura.put("fks", consulta(c, "SELECT * FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='PUBLIC' "
                + "AND CONSTRAINT_NAME IN (" + constraints + ") ORDER BY CONSTRAINT_NAME"));
        estrutura.put("indices", consulta(c, "SELECT * FROM INFORMATION_SCHEMA.INDEXES WHERE " + filtro
                + " ORDER BY TABLE_NAME,INDEX_NAME"));
        estrutura.put("colunasIndices", consulta(c, "SELECT * FROM INFORMATION_SCHEMA.INDEX_COLUMNS WHERE " + filtro
                + " ORDER BY TABLE_NAME,INDEX_NAME,ORDINAL_POSITION"));
        return new Retrato(dados, estrutura);
    }

    private List<List<String>> consulta(Connection c, String sql) throws SQLException {
        List<List<String>> linhas = new ArrayList<>();
        try (var s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            int colunas = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                List<String> linha = new ArrayList<>();
                for (int i = 1; i <= colunas; i++) linha.add(rs.getString(i));
                linhas.add(Collections.unmodifiableList(linha));
            }
        }
        return List.copyOf(linhas);
    }

    private void inserirFinanceiro(Connection c) throws SQLException {
        executar(c, "INSERT INTO corretora(id,cnpj,razao_social,cep,logradouro,bairro,cidade,uf,situacao_cadastral,data_cadastro) "
                + "VALUES (101,'11111111000111','Origem sintética','00000000','Rua','Bairro','Cidade','SP','ATIVA',"
                + "TIMESTAMP WITH TIME ZONE '2020-01-01 12:00:00.123456-03:00'),"
                + "(102,'22222222000122','Destino sintético','00000000','Rua','Bairro','Cidade','SP','ATIVA',"
                + "TIMESTAMP WITH TIME ZONE '2020-01-01 12:00:00.654321+05:30')");
        executar(c, "INSERT INTO carteira(id,nome,data_criacao) VALUES "
                + "(201,'BR sintética',TIMESTAMP WITH TIME ZONE '2020-01-01 12:00:00-03:00'),"
                + "(202,'EUA sintética',TIMESTAMP WITH TIME ZONE '2020-01-01 12:00:00+00:00')");
        executar(c, "INSERT INTO acao(id,ticker,nome_empresa,mercado,moeda,cotacao_atual,data_hora_cotacao) VALUES "
                + "(301,'SINTBR','Empresa BR','BRASIL','BRL',12.123456,TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:00.123456-03:00'),"
                + "(302,'SINTUS','Empresa EUA','EUA','USD',123.654321,TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:00.654321+00:00')");
        executar(c, "INSERT INTO operacao(id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total) VALUES "
                + "(401,201,301,101,'COMPRA',100,12,DATE '2020-01-02',1,1200),"
                + "(402,201,301,NULL,'VENDA',10,15,DATE '2020-01-03',1,150),"
                + "(403,202,302,102,'COMPRA',1.123456,100,DATE '2020-01-02',1,112.3456),"
                + "(404,202,302,102,'VENDA',0.000001,120,DATE '2020-01-03',1,0.000120)");
        executar(c, "INSERT INTO historico_cotacao(id,acao_id,cotacao,data_hora_cotacao,referencia_tipo,referencia_instante,referencia_data,cotacao_obtida_em) VALUES "
                + "(501,301,12.123456,TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:00.123456-03:00','DATA',NULL,DATE '2020-01-02',"
                + "TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:01-03:00'),"
                + "(502,302,123.654321,TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:00.654321+00:00','INSTANTE',"
                + "TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:00.654321+00:00',NULL,TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:01+00:00')");
        executar(c, "INSERT INTO snapshot_carteira(id,carteira_id,data_hora_snapshot) VALUES "
                + "(601,201,TIMESTAMP WITH TIME ZONE '2020-01-04 12:00:00.123456-03:00'),"
                + "(602,202,TIMESTAMP WITH TIME ZONE '2020-01-04 12:00:00.654321+00:00')");
        executar(c, "INSERT INTO snapshot_carteira_moeda(id,snapshot_carteira_id,moeda,patrimonio_atual) VALUES "
                + "(701,601,'BRL',1091.111040000000),(702,602,'USD',138.925735459055)");
    }

    private void inserirCustodia(Connection c) throws SQLException {
        executar(c, "INSERT INTO movimentacao_custodia(id,carteira_id,acao_id,natureza,corretora_origem_id,corretora_destino_id,"
                + "quantidade,data_efetiva,data_registro,ordem_financeira_ancora,posicao_numerador,posicao_denominador) VALUES "
                + "(801,201,301,'TRANSFERENCIA',101,102,5,DATE '2020-01-02',"
                + "TIMESTAMP WITH TIME ZONE '2026-10-14 12:00:00.123456-03:00',1,'1','2')");
        executar(c, "INSERT INTO estorno_movimentacao_custodia(id,movimentacao_original_id,data_registro,motivo) VALUES "
                + "(901,801,TIMESTAMP WITH TIME ZONE '2026-10-15 12:00:00.654321+05:30','Estorno sintético preservado')");
    }

    private void executar(Connection c, String sql) throws SQLException {
        try (var s = c.createStatement()) { assertTrue(s.executeUpdate(sql) > 0); }
    }

    private record Retrato(Map<String, List<List<String>>> dados, Map<String, List<List<String>>> estrutura) { }
}
