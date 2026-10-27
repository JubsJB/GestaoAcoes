package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2Test;
import com.projeto.entities.PosicaoRacionalCustodia;
import jakarta.persistence.EntityManagerFactory;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Constraints reais via JDBC, sem setters/reflexão em entidades e sem alterar schema. */
@CustodiaH2Test
class CustodiaIntegridadeH2Test {
    private static final OffsetDateTime REGISTRO = OffsetDateTime.parse("2026-10-14T12:00:00.123456-03:00");
    private static final String MOV = "movimentacao_custodia";
    private static final String EST = "estorno_movimentacao_custodia";
    @Autowired private DataSource dataSource;
    @Autowired private EntityManagerFactory factory;
    @Autowired private SpringLiquibase liquibase;
    @Autowired private ConfigurableApplicationContext context;

    @BeforeEach
    void confirmaIsolamento() throws Exception {
        assertEquals("2.5.250", org.h2.engine.Constants.class.getField("VERSION").get(null));
        assertEquals(1, context.getBeansOfType(DataSource.class).size());
        assertSame(dataSource, liquibase.getDataSource());
        assertSame(dataSource, ((EntityManagerFactoryInfo) factory).getDataSource());
        assertArrayEquals(new String[]{"custodia-h2"}, context.getEnvironment().getActiveProfiles());
        assertEquals("org.h2.Driver", ReflectionTestUtils.getField(dataSource, "driver").getClass().getName());
        String url = (String) ReflectionTestUtils.getField(dataSource, "url");
        String prefix = "jdbc:h2:mem:custodia_";
        assertNotNull(url);
        assertTrue(url.startsWith(prefix));
        assertEquals(prefix + UUID.fromString(url.substring(prefix.length())), url);
        try (var c = dataSource.getConnection()) {
            assertEquals("H2", c.getMetaData().getDatabaseProductName());
            assertEquals(url, c.getMetaData().getURL());
        }
        confirmaDescarte();
    }

    @AfterEach
    void confirmaDescarte() throws SQLException {
        // Conexão nova, após rollback/close do cenário; nunca reutiliza transação que falhou.
        try (var c = dataSource.getConnection()) {
            for (String tabela : List.of(MOV, EST, "carteira", "acao", "corretora")) {
                try (var s = c.createStatement(); var rs = s.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), tabela + ": fixture não deve sobreviver ao cenário");
                }
            }
        }
    }

    static Stream<Arguments> referencias() {
        return Stream.of(
                Arguments.of("carteira_id", "FK_MOVIMENTACAO_CUSTODIA_CARTEIRA"),
                Arguments.of("acao_id", "FK_MOVIMENTACAO_CUSTODIA_ACAO"),
                Arguments.of("corretora_origem_id", "FK_MOVIMENTACAO_CUSTODIA_CORRETORA_ORIGEM"),
                Arguments.of("corretora_destino_id", "FK_MOVIMENTACAO_CUSTODIA_CORRETORA_DESTINO"),
                Arguments.of("movimentacao_original_id", "FK_ESTORNO_MOVIMENTACAO_CUSTODIA_ORIGINAL"));
    }

    @ParameterizedTest
    @MethodSource("referencias")
    void foreignKeyRejeitaReferenciaInexistente(String coluna, String constraint) {
        rejeita("23506", constraint, c -> {
            Fixture f = fixture(c);
            if (coluna.equals("movimentacao_original_id")) {
                inserir(c, EST, estorno(Long.MAX_VALUE));
            } else {
                var valores = movimento(f);
                valores.put(coluna, Long.MAX_VALUE);
                inserir(c, MOV, valores);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"carteira_id", "acao_id", "corretora_destino_id", "movimentacao_original_id"})
    void referenciaObrigatoriaRejeitaNull(String coluna) {
        rejeita("23502", coluna, c -> {
            Fixture f = fixture(c);
            var valores = coluna.equals("movimentacao_original_id") ? estorno(null) : movimento(f);
            valores.put(coluna, null);
            inserir(c, coluna.equals("movimentacao_original_id") ? EST : MOV, valores);
        });
    }

    @ParameterizedTest
    @MethodSource("referencias")
    void exclusaoRestritivaPreservaReferencia(String coluna, String constraint) {
        rejeita("23503", constraint, c -> {
            Fixture f = fixture(c);
            long original = inserir(c, MOV, movimento(f));
            String tabela;
            long id;
            switch (coluna) {
                case "carteira_id" -> { tabela = "carteira"; id = f.carteira(); }
                case "acao_id" -> { tabela = "acao"; id = f.acao(); }
                case "corretora_origem_id" -> { tabela = "corretora"; id = f.origem(); }
                case "corretora_destino_id" -> { tabela = "corretora"; id = f.destino(); }
                default -> {
                    inserir(c, EST, estorno(original));
                    tabela = MOV; id = original;
                }
            }
            try (var s = c.prepareStatement("DELETE FROM " + tabela + " WHERE id=?")) {
                s.setLong(1, id);
                s.executeUpdate();
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRANSFERENCIA", "ATRIBUICAO"})
    void aceitaDirecoesValidasEOrigemNulaSomenteNaAtribuicao(String natureza) throws SQLException {
        transacao(c -> {
            Fixture f = fixture(c);
            var valores = movimento(f);
            valores.put("natureza", natureza);
            if (natureza.equals("ATRIBUICAO")) valores.put("corretora_origem_id", null);
            long id = inserir(c, MOV, valores);
            try (var s = c.prepareStatement("SELECT corretora_origem_id,corretora_destino_id FROM " + MOV + " WHERE id=?")) {
                s.setLong(1, id);
                try (var rs = s.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(natureza.equals("ATRIBUICAO") ? null : f.origem(), rs.getObject(1, Long.class));
                    assertEquals(f.destino(), rs.getLong(2));
                }
            }
        });
    }

    @Test
    void uniqueRejeitaMesmaPosicaoMesmoComQuantidadeENaturezaDiferentes() {
        rejeita("23505", "UK_MOVIMENTACAO_CUSTODIA_POSICAO", c -> {
            Fixture f = fixture(c);
            inserir(c, MOV, movimento(f));
            var duplicada = movimento(f);
            duplicada.put("natureza", "ATRIBUICAO");
            duplicada.put("corretora_origem_id", null);
            duplicada.put("quantidade", new BigDecimal("20"));
            inserir(c, MOV, duplicada);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"carteira_id", "acao_id", "data_efetiva", "ordem_financeira_ancora",
            "posicao_numerador", "posicao_denominador"})
    void uniqueAceitaChaveCompostaDistinta(String coluna) throws SQLException {
        transacao(c -> {
            Fixture f = fixture(c);
            long primeira = inserir(c, MOV, movimento(f));
            var segunda = movimento(f);
            Object distinto = switch (coluna) {
                case "carteira_id" -> carteira(c, "Outra");
                case "acao_id" -> acao(c, "SINT2");
                case "data_efetiva" -> LocalDate.of(2001, 2, 4);
                case "ordem_financeira_ancora" -> 1;
                case "posicao_numerador" -> "2";
                default -> "5";
            };
            segunda.put(coluna, distinto);
            assertNotEquals(primeira, inserir(c, MOV, segunda));
        });
    }

    @Test
    void uniqueRejeitaSegundoEstornoDaMesmaOriginal() {
        rejeita("23505", "UK_ESTORNO_MOVIMENTACAO_CUSTODIA_ORIGINAL", c -> {
            long original = inserir(c, MOV, movimento(fixture(c)));
            inserir(c, EST, estorno(original));
            inserir(c, EST, estorno(original));
        });
    }

    static Stream<Arguments> checks() {
        return Stream.of(
                Arguments.of("natureza", "DESCONHECIDA", "NATUREZA"),
                Arguments.of("quantidade", BigDecimal.ZERO, "QUANTIDADE_POSITIVA"),
                Arguments.of("quantidade", BigDecimal.ONE.negate(), "QUANTIDADE_POSITIVA"),
                Arguments.of("ordem_financeira_ancora", -1, "ANCORA_NAO_NEGATIVA"));
    }

    @ParameterizedTest
    @MethodSource("checks")
    void checkRejeitaNaturezaQuantidadeEAncoraInvalidas(String coluna, Object valor, String sufixo) {
        rejeita("23513", "CK_MOVIMENTACAO_CUSTODIA_" + sufixo, c -> {
            var valores = movimento(fixture(c));
            valores.put(coluna, valor);
            inserir(c, MOV, valores);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"transferencia-sem-origem", "transferencia-mesma-corretora", "atribuicao-com-origem"})
    void checkRejeitaDirecaoInvalida(String caso) {
        rejeita("23513", "CK_MOVIMENTACAO_CUSTODIA_DIRECAO", c -> {
            Fixture f = fixture(c);
            var valores = movimento(f);
            if (caso.equals("transferencia-sem-origem")) valores.put("corretora_origem_id", null);
            else if (caso.equals("transferencia-mesma-corretora")) valores.put("corretora_origem_id", f.destino());
            else valores.put("natureza", "ATRIBUICAO");
            inserir(c, MOV, valores);
        });
    }

    static Stream<Arguments> formatos() {
        return Stream.of("posicao_numerador", "posicao_denominador").flatMap(coluna -> Stream.of(
                "", "0", "01", "+1", "-1", "1.0", "1e2", " 1", "1 ", "1\n", "1\r", "1\t", "١", "１", "1a")
                .map(valor -> Arguments.of(coluna, valor)));
    }

    @ParameterizedTest
    @MethodSource("formatos")
    void checkRejeitaFormatosNaoAsciiZerosIniciaisESinais(String coluna, String valor) {
        rejeita("23513", "CK_MOVIMENTACAO_CUSTODIA_" + (coluna.endsWith("numerador")
                ? "NUMERADOR_FORMATO" : "DENOMINADOR_FORMATO"), c -> {
            var valores = movimento(fixture(c));
            valores.put(coluna, valor);
            inserir(c, MOV, valores);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"posicao_numerador", "posicao_denominador"})
    void varcharRejeitaMaisDe512CaracteresAntesDoCheck(String coluna) {
        rejeita("22001", coluna, c -> {
            var valores = movimento(fixture(c));
            valores.put(coluna, "1".repeat(513));
            inserir(c, MOV, valores);
        });
    }

    @Test
    void aceita512DigitosSemTruncamento() throws SQLException {
        transacao(c -> {
            var valores = movimento(fixture(c));
            String n = "1" + "0".repeat(511);
            String d = "1" + "0".repeat(510) + "1";
            valores.put("posicao_numerador", n);
            valores.put("posicao_denominador", d);
            long id = inserir(c, MOV, valores);
            try (var s = c.prepareStatement("SELECT posicao_numerador,posicao_denominador FROM " + MOV + " WHERE id=?")) {
                s.setLong(1, id);
                try (var rs = s.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(n, rs.getString(1));
                    assertEquals(d, rs.getString(2));
                }
            }
            assertDoesNotThrow(() -> PosicaoRacionalCustodia.reconstruirPersistida(n, d));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"2/4", "1/1", "3/2"})
    void bancoAceitaFormatoMasDominioRejeitaFracaoNaoCanonicaOuFronteira(String fracao) throws SQLException {
        transacao(c -> {
            var valores = movimento(fixture(c));
            String[] partes = fracao.split("/");
            valores.put("posicao_numerador", partes[0]);
            valores.put("posicao_denominador", partes[1]);
            assertTrue(inserir(c, MOV, valores) > 0);
            assertThrows(IllegalArgumentException.class,
                    () -> PosicaoRacionalCustodia.reconstruirPersistida(partes[0], partes[1]));
        });
    }

    @Test
    void uniqueTextualNaoImpedeFracoesMatematicamenteEquivalentes() throws SQLException {
        transacao(c -> {
            Fixture f = fixture(c);
            var primeira = movimento(f);
            primeira.put("posicao_denominador", "2");
            inserir(c, MOV, primeira);
            var segunda = movimento(f);
            segunda.put("posicao_numerador", "2");
            segunda.put("posicao_denominador", "4");
            assertTrue(inserir(c, MOV, segunda) > 0);
            assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalCustodia.reconstruirPersistida("2", "4"));
        });
    }

    private void rejeita(String estado, String identificador, Operacao operacao) {
        // A exceção só é examinada depois de rollback e close. Não existe SQL após a falha na mesma transação.
        SQLException erro = assertThrows(SQLException.class, () -> transacao(operacao));
        assertEquals(estado, erro.getSQLState(), erro.toString());
        assertTrue(erro.getMessage().toUpperCase(Locale.ROOT).contains(identificador.toUpperCase(Locale.ROOT)), erro.toString());
        System.out.printf("INTEGRIDADE estado=%s identificador=%s rollback=concluido%n", estado, identificador);
    }

    private void transacao(Operacao operacao) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                operacao.executar(c);
            } finally {
                c.rollback();
            }
        }
    }

    private long inserir(Connection c, String tabela, Map<String, Object> valores) throws SQLException {
        String colunas = String.join(",", valores.keySet());
        String parametros = String.join(",", java.util.Collections.nCopies(valores.size(), "?"));
        try (var s = c.prepareStatement("INSERT INTO " + tabela + " (" + colunas + ") VALUES (" + parametros + ")",
                Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            for (Object valor : valores.values()) s.setObject(i++, valor);
            assertEquals(1, s.executeUpdate());
            try (var keys = s.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    private Map<String, Object> movimento(Fixture f) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("carteira_id", f.carteira());
        valores.put("acao_id", f.acao());
        valores.put("natureza", "TRANSFERENCIA");
        valores.put("corretora_origem_id", f.origem());
        valores.put("corretora_destino_id", f.destino());
        valores.put("quantidade", new BigDecimal("10.000000"));
        valores.put("data_efetiva", LocalDate.of(2001, 2, 3));
        valores.put("data_registro", REGISTRO);
        valores.put("ordem_financeira_ancora", 0);
        valores.put("posicao_numerador", "1");
        valores.put("posicao_denominador", "3");
        return valores;
    }

    private Map<String, Object> estorno(Long original) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("movimentacao_original_id", original);
        valores.put("data_registro", REGISTRO.plusDays(1));
        valores.put("motivo", null);
        return valores;
    }

    private Fixture fixture(Connection c) throws SQLException {
        return new Fixture(carteira(c, "Sintética"), acao(c, "SINT1"), corretora(c, "11111111000111"),
                corretora(c, "22222222000122"));
    }

    private long carteira(Connection c, String nome) throws SQLException {
        return inserir(c, "carteira", Map.of("nome", nome, "data_criacao", REGISTRO));
    }

    private long acao(Connection c, String ticker) throws SQLException {
        return inserir(c, "acao", Map.of("ticker", ticker, "nome_empresa", "Empresa sintética", "mercado", "BRASIL",
                "moeda", "BRL", "cotacao_atual", BigDecimal.TEN, "data_hora_cotacao", REGISTRO));
    }

    private long corretora(Connection c, String cnpj) throws SQLException {
        return inserir(c, "corretora", Map.of("cnpj", cnpj, "razao_social", "Corretora sintética", "cep", "00000000",
                "logradouro", "Rua sintética", "bairro", "Bairro", "cidade", "Cidade", "uf", "SP",
                "situacao_cadastral", "ATIVA", "data_cadastro", REGISTRO));
    }

    @FunctionalInterface private interface Operacao { void executar(Connection c) throws SQLException; }
    private record Fixture(long carteira, long acao, long origem, long destino) { }
}
