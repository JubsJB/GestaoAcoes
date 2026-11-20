package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2Test;
import com.projeto.entities.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import liquibase.integration.spring.SpringLiquibase;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 2: fixtures sintéticas, transação independente por caso e descarte sem DML de remoção. */
@CustodiaH2Test
class CustodiaPersistenceH2Test {
    private static final LocalDate DIA = LocalDate.of(2001, 2, 3);
    private static final OffsetDateTime REGISTRO = OffsetDateTime.parse("2026-10-14T12:34:56.123456-03:00");
    private static final String FETCH = "select m from MovimentacaoCustodia m "
            + "join fetch m.carteira join fetch m.acao join fetch m.corretoraDestino "
            + "left join fetch m.corretoraOrigem where m.id = :id";

    @Autowired private EntityManagerFactory factory;
    @Autowired private DataSource dataSource;
    @Autowired private SpringLiquibase liquibase;
    @Autowired private ConfigurableApplicationContext context;

    @BeforeEach
    void confirmaInfraestruturaProtegida() throws Exception {
        assertTrue(getClass().isAnnotationPresent(CustodiaH2Test.class));
        assertEquals(1, context.getBeansOfType(DataSource.class).size());
        assertSame(dataSource, liquibase.getDataSource());
        assertSame(dataSource, ((EntityManagerFactoryInfo) factory).getDataSource());
        assertEquals("validate", factory.getProperties().get("hibernate.hbm2ddl.auto"));
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
    }

    @ParameterizedTest
    @CsvSource({
            "BRASIL, TRANSFERENCIA, 100.000000, 7",
            "BRASIL, ATRIBUICAO, 9999999999999.000000, 0",
            "EUA, TRANSFERENCIA, 0.000001, 1",
            "EUA, TRANSFERENCIA, 123.123456, 2",
            "EUA, ATRIBUICAO, 9999999999999.999999, 2147483647",
            "EUA, ATRIBUICAO, 0.5, 0"
    })
    void recuperaMovimentacaoExataEAuditoriaImutavel(Mercado mercado, NaturezaMovimentacaoCustodia natureza,
            String quantidade, int ancora) {
        emTransacao(em -> {
            Fixture f = fixture(em, mercado);
            // Ambos os componentes têm 512 dígitos; consecutivos, portanto coprimos.
            BigInteger n = BigInteger.TEN.pow(511);
            var posicao = PosicaoRacionalCustodia.criar(n, n.add(BigInteger.ONE));
            var original = movimentacao(f, natureza, quantidade, ancora, posicao, REGISTRO);
            em.persist(original);
            em.flush();
            Long id = original.getId();
            assertNotNull(id);
            assertTrue(id > 0);
            em.clear();
            assertFalse(em.contains(original));
            var recuperada = recuperar(em, id);
            assertNotSame(original, recuperada);
            assertEquals(id, recuperada.getId());
            assertEquals(natureza, recuperada.getNatureza());
            assertEquals(new BigDecimal(quantidade).setScale(6), recuperada.getQuantidade());
            assertEquals(DIA, recuperada.getDataEfetiva());
            assertEquals(REGISTRO.toInstant(), recuperada.getDataRegistro().toInstant());
            assertEquals(ancora, recuperada.getOrdemFinanceiraAncora());
            assertEquals(posicao, recuperada.getPosicaoRacional());
            assertEquals(f.carteira().getId(), recuperada.getCarteiraId());
            assertEquals(f.carteira().getNome(), recuperada.getCarteira().getNome());
            assertEquals(f.acao().getId(), recuperada.getAcaoId());
            assertEquals(f.acao().getTicker(), recuperada.getAcao().getTicker());
            assertEquals(mercado, recuperada.getMercado());
            assertEquals(f.destino().getId(), recuperada.getCorretoraDestinoId());
            assertEquals(f.destino().getCnpj(), recuperada.getCorretoraDestino().getCnpj());
            if (natureza == NaturezaMovimentacaoCustodia.TRANSFERENCIA) {
                assertEquals(f.origem().getId(), recuperada.getCorretoraOrigemId());
                assertEquals(f.origem().getCnpj(), recuperada.getCorretoraOrigem().getCnpj());
                assertNotEquals(recuperada.getCorretoraOrigemId(), recuperada.getCorretoraDestinoId());
            } else {
                assertNull(recuperada.getCorretoraOrigem());
                assertNull(recuperada.getCorretoraOrigemId());
            }
            Object[] antes = linhaMovimentacao(em, id);
            assertEquals(n.toString(), antes[0]);
            assertEquals(n.add(BigInteger.ONE).toString(), antes[1]);
            associacoesInicializadas(recuperada);
            assertDoesNotThrow(recuperada::validarEstruturaHistorica);
            semSetters(MovimentacaoCustodia.class);
            em.flush();
            em.clear();
            assertNotSame(recuperada, recuperar(em, id));
            assertArrayEquals(antes, linhaMovimentacao(em, id));
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"  Motivo sintético preservado  "})
    void recuperaEstornoSemDuplicarDadosDaOriginal(String motivo) {
        emTransacao(em -> {
            Fixture f = fixture(em, Mercado.BRASIL);
            var original = movimentacao(f, NaturezaMovimentacaoCustodia.TRANSFERENCIA, "100", 9,
                    PosicaoRacionalCustodia.deTexto("1", "3"), REGISTRO);
            em.persist(original);
            var registroEstorno = REGISTRO.plusDays(2).withOffsetSameInstant(ZoneOffset.ofHours(5));
            var estorno = new EstornoMovimentacaoCustodia(null, original, motivo, relogio(registroEstorno));
            em.persist(estorno);
            em.flush();
            Long id = estorno.getId();
            Long originalId = original.getId();
            Object[] antes = linhaMovimentacao(em, originalId);
            em.clear();
            assertFalse(em.contains(estorno));
            assertFalse(em.contains(original));
            var recuperado = em.find(EstornoMovimentacaoCustodia.class, id);
            assertNotSame(estorno, recuperado);
            assertNotNull(recuperado);
            assertEquals(id, recuperado.getId());
            assertEquals(originalId, recuperado.getMovimentacaoOriginalId());
            assertEquals(motivo, recuperado.getMotivo());
            assertEquals(registroEstorno.toInstant(), recuperado.getDataRegistro().toInstant());
            var originalRecuperada = recuperar(em, originalId);
            Hibernate.initialize(recuperado.getMovimentacaoOriginal());
            assertNotSame(original, originalRecuperada);
            associacoesInicializadas(originalRecuperada);
            assertDoesNotThrow(originalRecuperada::validarEstruturaHistorica);
            assertDoesNotThrow(recuperado::validarEstruturaHistorica);
            assertEquals(DIA, recuperado.getDataEfetiva());
            assertEquals(9, recuperado.getOrdemFinanceiraAncora());
            assertEquals(PosicaoRacionalCustodia.deTexto("1", "3"), recuperado.getPosicaoRacional());
            assertEquals(List.of("ID", "MOVIMENTACAO_ORIGINAL_ID", "DATA_REGISTRO", "MOTIVO"),
                    em.createNativeQuery("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE "
                            + "TABLE_SCHEMA='PUBLIC' AND TABLE_NAME='ESTORNO_MOVIMENTACAO_CUSTODIA' "
                            + "ORDER BY ORDINAL_POSITION", String.class).getResultList());
            semSetters(EstornoMovimentacaoCustodia.class);
            em.flush();
            em.clear();
            var novamente = em.find(EstornoMovimentacaoCustodia.class, id);
            assertNotSame(recuperado, novamente);
            assertEquals(motivo, novamente.getMotivo());
            assertEquals(registroEstorno.toInstant(), novamente.getDataRegistro().toInstant());
            assertArrayEquals(antes, linhaMovimentacao(em, originalId));
        });
    }

    @ParameterizedTest
    @CsvSource({"123456000,123456000", "123456789,123457000"})
    void medePrecisaoTemporalDeMovimentacaoEEstorno(int nanosEntrada, int nanosArmazenados) {
        emTransacao(em -> {
            Fixture f = fixture(em, Mercado.EUA);
            var entrada = REGISTRO.withNano(nanosEntrada).withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5, 30));
            var original = movimentacao(f, NaturezaMovimentacaoCustodia.ATRIBUICAO, "0.123456", 0,
                    PosicaoRacionalCustodia.deTexto("1", "2"), entrada);
            em.persist(original);
            var estorno = new EstornoMovimentacaoCustodia(null, original, null, relogio(entrada.plusDays(1)));
            em.persist(estorno);
            em.flush();
            em.clear();
            var movimentoLido = recuperar(em, original.getId());
            var estornoLido = em.find(EstornoMovimentacaoCustodia.class, estorno.getId());
            assertNotSame(original, movimentoLido);
            assertNotSame(estorno, estornoLido);
            verificaTempo(em, "movimentacao_custodia", entrada, movimentoLido.getDataRegistro(), nanosArmazenados);
            verificaTempo(em, "estorno_movimentacao_custodia", entrada.plusDays(1),
                    estornoLido.getDataRegistro(), nanosArmazenados);
            associacoesInicializadas(movimentoLido);
            Hibernate.initialize(estornoLido.getMovimentacaoOriginal());
            assertDoesNotThrow(movimentoLido::validarEstruturaHistorica);
            assertDoesNotThrow(estornoLido::validarEstruturaHistorica);
        });
    }

    @ParameterizedTest
    @CsvSource({"2,4,canônica", "1,1,estritamente entre 0 e 1", "3,2,estritamente entre 0 e 1"})
    void hidratacaoRejeitaFracaoInvalidaSemCorrigirJdbc(String numerador, String denominador, String mensagem) {
        emTransacao(em -> {
            Fixture f = fixture(em, Mercado.EUA);
            em.flush();
            long id = em.unwrap(Session.class).doReturningWork(connection -> {
                try (var insert = connection.prepareStatement("INSERT INTO movimentacao_custodia "
                        + "(carteira_id,acao_id,natureza,corretora_destino_id,quantidade,data_efetiva,data_registro,"
                        + "ordem_financeira_ancora,posicao_numerador,posicao_denominador) VALUES (?,?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insert.setLong(1, f.carteira().getId());
                    insert.setLong(2, f.acao().getId());
                    insert.setString(3, "ATRIBUICAO");
                    insert.setLong(4, f.destino().getId());
                    insert.setBigDecimal(5, new BigDecimal("1.000000"));
                    insert.setObject(6, DIA);
                    insert.setObject(7, REGISTRO);
                    insert.setInt(8, 0);
                    insert.setString(9, numerador);
                    insert.setString(10, denominador);
                    assertEquals(1, insert.executeUpdate());
                    try (var keys = insert.getGeneratedKeys()) {
                        assertTrue(keys.next());
                        return keys.getLong(1);
                    }
                }
            });
            em.clear();
            RuntimeException erro = assertThrows(RuntimeException.class,
                    () -> em.find(MovimentacaoCustodia.class, id));
            Throwable causa = erro;
            while (causa.getCause() != null && causa.getCause() != causa) causa = causa.getCause();
            assertInstanceOf(IllegalArgumentException.class, causa);
            assertTrue(causa.getMessage().contains(mensagem), causa.toString());
            // Após falha de hidratação, somente leitura JDBC da mesma conexão; sem flush adicional.
            em.unwrap(Session.class).doWork(connection -> {
                try (var select = connection.prepareStatement("SELECT posicao_numerador,posicao_denominador "
                        + "FROM movimentacao_custodia WHERE id=?")) {
                    select.setLong(1, id);
                    try (var rs = select.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(numerador, rs.getString(1));
                        assertEquals(denominador, rs.getString(2));
                    }
                }
            });
        });
    }

    private void verificaTempo(EntityManager em, String tabela, OffsetDateTime entrada,
            OffsetDateTime lido, int nanos) {
        int precisao = ((Number) em.createNativeQuery("SELECT DATETIME_PRECISION FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_NAME=:t AND COLUMN_NAME='DATA_REGISTRO'")
                .setParameter("t", tabela.toUpperCase(java.util.Locale.ROOT)).getSingleResult()).intValue();
        assertEquals(6, precisao);
        assertEquals(entrada.withNano(nanos).toInstant(), lido.toInstant());
        assertEquals(nanos, lido.getNano());
        // Offset é observado separadamente; não se exige igualdade textual com a entrada.
        assertEquals(entrada.withNano(nanos).withOffsetSameInstant(lido.getOffset()), lido);
        System.out.printf("TEMPORAL %s entrada=%s recuperado=%s precisao=%d offsetEntrada=%s offsetRecuperado=%s%n",
                tabela, entrada, lido, precisao, entrada.getOffset(), lido.getOffset());
    }

    private MovimentacaoCustodia recuperar(EntityManager em, Long id) {
        return em.createQuery(FETCH, MovimentacaoCustodia.class).setParameter("id", id).getSingleResult();
    }

    private Object[] linhaMovimentacao(EntityManager em, Long id) {
        return (Object[]) em.createNativeQuery("SELECT posicao_numerador,posicao_denominador,quantidade,"
                + "data_efetiva,data_registro,ordem_financeira_ancora,carteira_id,acao_id,natureza,"
                + "corretora_origem_id,corretora_destino_id FROM movimentacao_custodia WHERE id=:id")
                .setParameter("id", id).getSingleResult();
    }

    private void associacoesInicializadas(MovimentacaoCustodia m) {
        assertTrue(Hibernate.isInitialized(m.getCarteira()));
        assertTrue(Hibernate.isInitialized(m.getAcao()));
        assertTrue(Hibernate.isInitialized(m.getCorretoraDestino()));
        if (m.getCorretoraOrigem() != null) assertTrue(Hibernate.isInitialized(m.getCorretoraOrigem()));
    }

    private void semSetters(Class<?> tipo) {
        assertTrue(Arrays.stream(tipo.getMethods()).noneMatch(m -> m.getName().startsWith("set")));
    }

    private MovimentacaoCustodia movimentacao(Fixture f, NaturezaMovimentacaoCustodia natureza, String quantidade,
            int ancora, PosicaoRacionalCustodia posicao, OffsetDateTime registro) {
        return new MovimentacaoCustodia(null, f.carteira(), f.acao(), natureza,
                natureza == NaturezaMovimentacaoCustodia.TRANSFERENCIA ? f.origem() : null,
                f.destino(), new BigDecimal(quantidade), DIA, ancora, posicao, relogio(registro));
    }

    private static Clock relogio(OffsetDateTime registro) {
        return Clock.fixed(registro.toInstant(), registro.getOffset());
    }

    private Fixture fixture(EntityManager em, Mercado mercado) {
        var carteira = new Carteira("Carteira sintética", REGISTRO);
        var acao = new Acao("SINT1", "Empresa sintética", mercado,
                mercado == Mercado.BRASIL ? Moeda.BRL : Moeda.USD, BigDecimal.TEN, REGISTRO);
        var origem = corretora("11111111000111", "Origem sintética");
        var destino = corretora("22222222000122", "Destino sintético");
        em.persist(carteira);
        em.persist(acao);
        em.persist(origem);
        em.persist(destino);
        em.flush();
        return new Fixture(carteira, acao, origem, destino);
    }

    private Corretora corretora(String cnpj, String nome) {
        return new Corretora(cnpj, nome, null, null, null, "00000000", "Rua sintética", "1", null,
                "Bairro sintético", "Cidade sintética", "SP", "ATIVA", REGISTRO);
    }

    private void emTransacao(Consumer<EntityManager> cenario) {
        try (EntityManager em = factory.createEntityManager()) {
            em.getTransaction().begin();
            try {
                cenario.accept(em);
            } finally {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
            }
        }
    }

    private record Fixture(Carteira carteira, Acao acao, Corretora origem, Corretora destino) { }
}
