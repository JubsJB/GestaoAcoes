package com.projeto.temporalpoc;

import com.projeto.entities.*;
import com.projeto.mappers.AcaoMapper;
import com.projeto.resources.AcaoResource;
import com.projeto.services.AcaoService;
import liquibase.*;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.jdbc.*;
import org.hibernate.type.internal.BasicTypeImpl;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.nullValue;

/** Opt-in; usa apenas bancos efêmeros e SessionFactories com os dois campos-alvo reais. */
@EnabledIfSystemProperty(named = "temporal.poc.enabled", matches = "true")
@Execution(ExecutionMode.SAME_THREAD)
class ReferenciaDataBcPrototypeTest {
    enum Mode { OLD, A_BC_COMPAT, B_H2_ONLY, C_DIRECT }
    record Pair(long action, long history) { }
    record Read(LocalDate value, String error) { }

    static Stream<Arguments> settings() {
        return Stream.of("h2", "postgresql").flatMap(db -> Stream.of("UTC", "America/Los_Angeles",
                "Pacific/Kiritimati").map(zone -> Arguments.of(db, zone)));
    }

    static List<LocalDate> dates() {
        var result = new LinkedHashSet<LocalDate>();
        Stream.of("0001-01-01", "0004-02-29", "0100-03-01", "0400-02-29", "1500-03-01",
                "1582-10-04", "1582-10-15", "1600-02-29", "1900-03-01", "2000-02-29",
                "2026-10-02", "9999-12-31").map(LocalDate::parse).forEach(result::add);
        IntStream.rangeClosed(1, 31).mapToObj(day -> LocalDate.of(1582, 10, day)).forEach(result::add);
        return new ArrayList<>(result);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("settings")
    void comparaAlternativasSemModificarProducao(String db, String zone) throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            try (Context context = new Context(db, zone)) {
                for (LocalDate date : dates()) {
                    Pair fresh = write(context.factories.get(Mode.A_BC_COMPAT), date);
                    assertReadsAndApi(context, Mode.A_BC_COMPAT, fresh, new Read(date, null));
                    assertSql(context, fresh, date);
                    Read crossReading = read(context.factories.get(Mode.OLD), fresh.action(), Acao.class);
                    assertReadsAndApi(context, Mode.OLD, fresh, crossReading);
                    Pair old = write(context.factories.get(Mode.OLD), date);
                    Read oldReading = read(context.factories.get(Mode.OLD), old.action(), Acao.class);
                    Read proposed = read(context.factories.get(Mode.A_BC_COMPAT), old.action(), Acao.class);
                    if (db.equals("postgresql")) {
                        assertEquals(oldReading, proposed, "Preservar leitura de AD escrito pelo caminho anterior");
                        assertReadsAndApi(context, Mode.OLD, old, oldReading);
                        assertReadsAndApi(context, Mode.A_BC_COMPAT, old, oldReading);
                    }
                    Pair b = write(context.factories.get(Mode.B_H2_ONLY), date);
                    Read bReading = read(context.factories.get(Mode.B_H2_ONLY), b.action(), Acao.class);
                    Pair c = write(context.factories.get(Mode.C_DIRECT), date);
                    assertReadsAndApi(context, Mode.C_DIRECT, c, new Read(date, null));
                    System.out.printf("POC_CIVIL db=%s zone=%s input=%s oldSql=%s oldRead=%s Aold=%s Anew=%s B=%s Cold=%s oldReadsNew=%s%n",
                            db, zone, date, sql(context, old.action()), oldReading, proposed, date, bReading,
                            read(context.factories.get(Mode.C_DIRECT), old.action(), Acao.class),
                            read(context.factories.get(Mode.OLD), fresh.action(), Acao.class));
                }
                if (db.equals("postgresql")) bcAndAmbiguity(context, zone);
                Pair absent = context.factories.get(Mode.A_BC_COMPAT).fromTransaction(s -> {
                    var obtained = OffsetDateTime.parse("2026-10-10T00:00:00Z");
                    var a = new Acao("N" + UUID.randomUUID().toString().substring(0, 20), "Sem referencia",
                            Mercado.EUA, Moeda.USD, BigDecimal.TEN, obtained);
                    s.persist(a); var h = new HistoricoCotacao(a, BigDecimal.TEN, obtained); s.persist(h); s.flush();
                    return new Pair(a.getId(), h.getId());
                });
                assertReadsAndApi(context, Mode.A_BC_COMPAT, absent, new Read(null, null));
                System.out.printf("POC_SUMMARY db=%s zone=%s civilDates=%s apiChecks=%s noProductionChanges=true%n",
                        db, zone, dates().size(), context.apiChecks);
            }
        } finally { TimeZone.setDefault(previous); }
    }

    static void bcAndAmbiguity(Context c, String zone) throws Exception {
        for (String text : new String[]{"0000-01-01", "0000-02-29", "-0001-01-01", "-0001-12-31",
                "-0004-02-29", "-0100-03-01", "-0400-02-29", "+10000-01-01"}) {
            LocalDate input = LocalDate.parse(text);
            Pair old = write(c.factories.get(Mode.OLD), input);
            Read before = read(c.factories.get(Mode.OLD), old.action(), Acao.class);
            Read after = read(c.factories.get(Mode.A_BC_COMPAT), old.action(), Acao.class);
            assertEquals(before, after, "Comparar valor OU erro antigo, não apenas converter o ano BC");
            assertReadsAndApi(c, Mode.OLD, old, before);
            assertReadsAndApi(c, Mode.A_BC_COMPAT, old, before);
            assertReadsAndApi(c, Mode.C_DIRECT, old, read(c.factories.get(Mode.C_DIRECT), old.action(), Acao.class));
            System.out.printf("POC_LEGACY zone=%s input=%s sql=%s old=%s A=%s C=%s%n", zone, input,
                    sql(c, old.action()), before, after, read(c.factories.get(Mode.C_DIRECT), old.action(), Acao.class));
        }

        Pair fromOld = write(c.factories.get(Mode.OLD), LocalDate.of(1582, 10, 10));
        Pair fromNew = write(c.factories.get(Mode.A_BC_COMPAT), LocalDate.of(1582, 10, 20));
        assertEquals(sql(c, fromOld.action()), sql(c, fromNew.action()));
        System.out.printf("POC_COLLISION zone=%s inputOld=1582-10-10 inputNew=1582-10-20 sameSql=%s%n", zone, sql(c, fromOld.action()));

        // A mesma linha AD 10/10 pode existir via SQL explícito anterior ou pela escrita civil nova.
        Pair ambiguous = write(c.factories.get(Mode.OLD), LocalDate.of(2026, 10, 2));
        for (String table : new String[]{"acao", "historico_cotacao"}) {
            try (var s = c.connection.prepareStatement("update " + table + " set referencia_data = DATE '1582-10-10' where id=?")) {
                s.setLong(1, table.equals("acao") ? ambiguous.action() : ambiguous.history()); s.executeUpdate();
            }
        }
        Pair civil = write(c.factories.get(Mode.A_BC_COMPAT), LocalDate.of(1582, 10, 10));
        assertEquals(sql(c, ambiguous.action()), sql(c, civil.action()));
        Read old = read(c.factories.get(Mode.OLD), ambiguous.action(), Acao.class);
        Read proposed = read(c.factories.get(Mode.A_BC_COMPAT), ambiguous.action(), Acao.class);
        assertNotEquals(old, proposed, "O valor SQL isolado não certifica a origem para AD no intervalo de reforma");
        assertReadsAndApi(c, Mode.OLD, ambiguous, old);
        assertReadsAndApi(c, Mode.A_BC_COMPAT, ambiguous, proposed);
        // Alternativa com versão: apenas simulação do seletor em memória, não coluna/migration.
        var origin = Map.of(ambiguous.action(), Mode.OLD, civil.action(), Mode.C_DIRECT);
        AcaoService taggedService = mock(AcaoService.class);
        when(taggedService.buscarPorId(anyLong())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            return c.factories.get(origin.get(id)).fromTransaction(s -> new AcaoMapper().toResponse(s.find(Acao.class, id)));
        });
        var taggedApi = MockMvcBuilders.standaloneSetup(new AcaoResource(taggedService)).build();
        taggedApi.perform(get("/acoes/{id}", ambiguous.action())).andExpect(status().isOk())
                .andExpect(jsonPath("$.referenciaMercadoCotacao.data").value(old.value().toString()));
        taggedApi.perform(get("/acoes/{id}", civil.action())).andExpect(status().isOk())
                .andExpect(jsonPath("$.referenciaMercadoCotacao.data").value("1582-10-10"));
        assertEquals(old, read(c.factories.get(origin.get(ambiguous.action())), ambiguous.history(), HistoricoCotacao.class));
        assertEquals(new Read(LocalDate.of(1582, 10, 10), null), read(c.factories.get(origin.get(civil.action())), civil.history(), HistoricoCotacao.class));
        System.out.printf("POC_VERSIONED_SIMULATION zone=%s sameSql=1582-10-10 LEGACY=%s CIVIL=1582-10-10 persistedMetadata=false%n", zone, old.value());
        System.out.printf("POC_AMBIGUITY zone=%s sql=%s old=%s A=%s needsOriginForUniversalGuarantee=true%n",
                zone, sql(c, ambiguous.action()), old, proposed);
    }

    static Pair write(SessionFactory factory, LocalDate date) {
        return factory.fromTransaction(s -> {
            var obtained = OffsetDateTime.parse("2026-10-10T00:00:00Z");
            var a = new Acao("P" + UUID.randomUUID().toString().substring(0, 20), "POC", Mercado.EUA, Moeda.USD,
                    BigDecimal.TEN, obtained, ReferenciaTipoCotacao.DATA, null, date, obtained);
            s.persist(a);
            var h = new HistoricoCotacao(a, BigDecimal.TEN, obtained, ReferenciaTipoCotacao.DATA, null, date, obtained);
            s.persist(h); s.flush(); return new Pair(a.getId(), h.getId());
        });
    }

    static Read read(SessionFactory factory, long id, Class<?> entity) {
        try {
            LocalDate value = factory.fromTransaction(s -> entity == Acao.class
                    ? s.find(Acao.class, id).getReferenciaData() : s.find(HistoricoCotacao.class, id).getReferenciaData());
            return new Read(value, null);
        } catch (RuntimeException e) { return new Read(null, root(e)); }
    }

    static String root(Throwable e) {
        while (e.getCause() != null) e = e.getCause();
        return e.getClass().getName() + ":" + e.getMessage();
    }

    static void assertReadsAndApi(Context context, Mode mode, Pair pair, Read expected) throws Exception {
        context.apiChecks++;
        SessionFactory factory = context.factories.get(mode);
        assertEquals(expected, read(factory, pair.action(), Acao.class));
        assertEquals(expected, read(factory, pair.history(), HistoricoCotacao.class));
        try {
            var result = context.apis.get(mode).perform(get("/acoes/{id}", pair.action()));
            assertNull(expected.error(), "Erro antigo não pode virar referência silenciosa");
            result.andExpect(status().isOk());
            if (expected.value() == null) {
                result.andExpect(jsonPath("$.referenciaMercadoCotacao.tipo").value("DESCONHECIDA"))
                        .andExpect(jsonPath("$.referenciaMercadoCotacao.data").value(nullValue()));
            } else result.andExpect(jsonPath("$.referenciaMercadoCotacao.data").value(expected.value().toString()));
        } catch (jakarta.servlet.ServletException e) {
            assertNotNull(expected.error()); assertEquals(expected.error(), root(e));
        }
    }

    static String sql(Context c, long id) throws SQLException {
        try (var s = c.connection.prepareStatement("select cast(referencia_data as varchar) from acao where id=?")) {
            s.setLong(1, id); try (var r = s.executeQuery()) { assertTrue(r.next()); return r.getString(1); }
        }
    }

    static void assertSql(Context c, Pair pair, LocalDate expected) throws SQLException {
        for (String table : new String[]{"acao", "historico_cotacao"}) {
            try (var s = c.connection.prepareStatement("select referencia_data from " + table + " where id=?")) {
                s.setLong(1, table.equals("acao") ? pair.action() : pair.history());
                try (var r = s.executeQuery()) {
                    assertTrue(r.next()); assertEquals(Types.DATE, r.getMetaData().getColumnType(1));
                    assertEquals(expected, r.getObject(1, LocalDate.class));
                }
            }
        }
    }

    static final class Context implements AutoCloseable {
        final Connection connection;
        final EnumMap<Mode, SessionFactory> factories = new EnumMap<>(Mode.class);
        final EnumMap<Mode, MockMvc> apis = new EnumMap<>(Mode.class);
        final String db, schema;
        int apiChecks;

        Context(String db, String zone) throws Exception {
            this.db = db;
            schema = "poc_" + UUID.randomUUID().toString().replace("-", "");
            String base = db.equals("h2") ? "jdbc:h2:mem:" + schema : System.getProperty("temporal.test.postgresql.url", "");
            if (db.equals("postgresql")) {
                assertTrue(base.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/postgres"));
                assertEquals("codex_etapa_d", System.getProperty("temporal.test.postgresql.user"));
            }
            String user = db.equals("h2") ? "sa" : "codex_etapa_d";
            connection = DriverManager.getConnection(base, user, "");
            if (db.equals("postgresql")) {
                try (var s = connection.createStatement(); var r = s.executeQuery("show data_directory")) {
                    assertTrue(r.next()); assertTrue(r.getString(1).replace('\\', '/').contains("/target/jdbc-bc-poc-pg"));
                }
                connection.createStatement().execute("create schema " + schema);
                connection.setSchema(schema);
            } else connection.createStatement().execute("set time zone '" + zone + "'");
            var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            if (db.equals("postgresql")) database.setDefaultSchemaName(schema);
            var liquibase = new Liquibase("db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
            // As fixtures SQL precisam estar visíveis às conexões independentes das quatro fábricas.
            connection.setAutoCommit(true);
            String url = db.equals("h2") ? base + ";INIT=SET TIME ZONE '" + zone + "'"
                    : base + "?currentSchema=" + schema + "&options=" + URLEncoder.encode("-c TimeZone=" + zone, StandardCharsets.UTF_8);
            for (Mode mode : Mode.values()) {
                JdbcType jdbc = switch (mode) {
                    case OLD -> DateJdbcType.INSTANCE;
                    case A_BC_COMPAT -> new BcCompatibilityJdbcType(db.equals("postgresql"));
                    case B_H2_ONLY -> db.equals("h2") ? LocalDateJdbcType.INSTANCE : DateJdbcType.INSTANCE;
                    case C_DIRECT -> LocalDateJdbcType.INSTANCE;
                };
                var registry = new StandardServiceRegistryBuilder().applySetting("hibernate.connection.url", url)
                        .applySetting("hibernate.connection.username", user).applySetting("hibernate.connection.password", "")
                        .applySetting("hibernate.connection.pool_size", 2).applySetting("hibernate.hbm2ddl.auto", "validate")
                        .applySetting("hibernate.default_schema", db.equals("postgresql") ? schema : "PUBLIC").build();
                var builder = new MetadataSources(registry).addAnnotatedClass(Acao.class).addAnnotatedClass(HistoricoCotacao.class).getMetadataBuilder();
                builder.applyTypes((contributions, services) -> {
                    contributions.getTypeConfiguration().getJdbcTypeRegistry().addDescriptor(Types.DATE, jdbc);
                    contributions.contributeType(new BasicTypeImpl<>(LocalDateJavaType.INSTANCE, jdbc), LocalDate.class.getName(), "LocalDate");
                });
                var metadata = builder.build();
                assertEquals(2, metadata.getEntityBindings().size(), "Nenhuma outra entidade participa da substituição de teste");
                var dateFields = metadata.getEntityBindings().stream().flatMap(entity -> entity.getProperties().stream()
                        .filter(p -> p.getType().getReturnedClass() == LocalDate.class)
                        .map(p -> entity.getClassName() + "#" + p.getName())).collect(Collectors.toSet());
                assertEquals(Set.of(Acao.class.getName() + "#referenciaData", HistoricoCotacao.class.getName() + "#referenciaData"),
                        dateFields, "Não substituir qualquer outro LocalDate, mesmo nesta fábrica de prova");
                for (Class<?> entity : new Class<?>[]{Acao.class, HistoricoCotacao.class}) {
                    BasicType<?> type = (BasicType<?>) metadata.getEntityBinding(entity.getName()).getProperty("referenciaData").getType();
                    assertSame(jdbc, type.getJdbcType(), "Substituir somente os dois LocalDate mapeados nesta fábrica isolada");
                }
                SessionFactory factory = metadata.buildSessionFactory(); factories.put(mode, factory);
                AcaoService service = mock(AcaoService.class);
                when(service.buscarPorId(anyLong())).thenAnswer(i -> factory.fromTransaction(s -> new AcaoMapper().toResponse(s.find(Acao.class, (Long) i.getArgument(0)))));
                apis.put(mode, MockMvcBuilders.standaloneSetup(new AcaoResource(service)).build());
            }
        }

        @Override public void close() throws Exception {
            factories.values().forEach(SessionFactory::close);
            if (db.equals("postgresql")) connection.createStatement().execute("drop schema " + schema + " cascade");
            connection.close();
        }
    }
}
