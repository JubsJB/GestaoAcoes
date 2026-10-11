package com.projeto.resources;

import com.projeto.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfSystemProperty(named = "temporal.test.postgresql.enabled", matches = "true")
class ReferenciaDataJpaPostgresqlTest extends ReferenciaDataJpaDiagnosticoTest {
    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        String url = System.getProperty("temporal.test.postgresql.url", "");
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/postgres")
                || !"codex_etapa_d".equals(System.getProperty("temporal.test.postgresql.user"))) {
            throw new IllegalStateException("Requer PostgreSQL temporário loopback e usuário de teste explícitos");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "codex_etapa_d");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeEach
    void conferirBancoIsolado() {
        assertEquals("codex_etapa_d", jdbc.queryForObject("select current_user", String.class));
        assertEquals("PostgreSQL", jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>)
                c -> c.getMetaData().getDatabaseProductName()));
    }

    @ParameterizedTest
    @MethodSource("civilDates")
    void leitorNovoPreservaRegistrosDoBindingAnterior(String text) throws Exception {
        LocalDate original = LocalDate.parse(text);
        LocalDate expected = original.getYear() == 1582 && original.getMonthValue() == 10
                && original.getDayOfMonth() >= 5 && original.getDayOfMonth() <= 14
                ? original.plusDays(10) : original;
        OffsetDateTime obtained = OffsetDateTime.parse("2026-10-10T00:00:00Z");
        var a = acoes.saveAndFlush(new Acao("P" + UUID.randomUUID().toString().substring(0, 20), "Anterior",
                Mercado.EUA, Moeda.USD, BigDecimal.TEN, obtained, ReferenciaTipoCotacao.DATA, null, original, obtained));
        var h = historicos.saveAndFlush(new HistoricoCotacao(a, BigDecimal.TEN, obtained,
                ReferenciaTipoCotacao.DATA, null, original, obtained));
        Long aid = a.getId(), hid = h.getId();
        for (String table : new String[]{"acao", "historico_cotacao"}) {
            Long id = table.equals("acao") ? aid : hid;
            // Reproduz exatamente a escrita anterior do DateJdbcType, nos dois campos.
            jdbc.update(c -> {
                var s = c.prepareStatement("update " + table + " set referencia_data = ? where id = ?");
                s.setDate(1, java.sql.Date.valueOf(original)); s.setLong(2, id); return s;
            });
            jdbc.query("select referencia_data, cast(referencia_data as varchar) from " + table + " where id = ?", r -> {
                assertEquals(expected, r.getDate(1).toLocalDate());
                assertEquals(expected, r.getObject(1, LocalDate.class));
                assertEquals(expected.toString(), r.getString(2));
            }, id);
        }
        em.clear();
        assertEquals(expected, acoes.findById(aid).orElseThrow().getReferenciaData());
        assertEquals(expected, historicos.findById(hid).orElseThrow().getReferenciaData());
        mvc.perform(get("/acoes/{id}", aid)).andExpect(status().isOk())
                .andExpect(jsonPath("$.referenciaMercadoCotacao.data").value(expected.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000-01-01", "-0001-01-01", "-0001-12-31", "-0100-03-01"})
    void dominioAnteriorBcNaoPodeSerReinterpretado(String text) throws Exception {
        LocalDate original = LocalDate.parse(text);
        LocalDate expectedLegacy = LocalDate.of(1 - original.getYear(), original.getMonthValue(), original.getDayOfMonth());
        OffsetDateTime obtained = OffsetDateTime.parse("2026-10-10T00:00:00Z");
        var a = acoes.saveAndFlush(new Acao("B" + UUID.randomUUID().toString().substring(0, 20), "Legado BC",
                Mercado.EUA, Moeda.USD, BigDecimal.TEN, obtained, ReferenciaTipoCotacao.DATA, null, original, obtained));
        var h = historicos.saveAndFlush(new HistoricoCotacao(a, BigDecimal.TEN, obtained,
                ReferenciaTipoCotacao.DATA, null, original, obtained));
        Long aid = a.getId(), hid = h.getId();
        for (String table : new String[]{"acao", "historico_cotacao"}) {
            Long id = table.equals("acao") ? aid : hid;
            jdbc.update(c -> {
                var s = c.prepareStatement("update " + table + " set referencia_data = ? where id = ?");
                s.setDate(1, java.sql.Date.valueOf(original)); s.setLong(2, id); return s;
            });
            jdbc.query("select referencia_data, cast(referencia_data as varchar) from " + table + " where id = ?", r -> {
                assertEquals(expectedLegacy, r.getDate(1).toLocalDate());
                assertEquals(original, r.getObject(1, LocalDate.class));
                System.out.printf("POSTGRESQL_BC_COMPAT table=%s input=%s sql=%s previous=%s direct=%s%n",
                        table, original, r.getString(2), expectedLegacy, original);
            }, id);
        }
        em.clear();
        assertEquals(expectedLegacy, acoes.findById(aid).orElseThrow().getReferenciaData());
        assertEquals(expectedLegacy, historicos.findById(hid).orElseThrow().getReferenciaData());
        mvc.perform(get("/acoes/{id}", aid)).andExpect(status().isOk())
                .andExpect(jsonPath("$.referenciaMercadoCotacao.data").value(expectedLegacy.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000-02-29", "-0004-02-29", "-0400-02-29"})
    void preservaErrosBcAnterioresSemFabricarReferencia(String text) throws Exception {
        LocalDate original = LocalDate.parse(text);
        OffsetDateTime obtained = OffsetDateTime.parse("2026-10-10T00:00:00Z");
        var a = acoes.saveAndFlush(new Acao("E" + UUID.randomUUID().toString().substring(0, 20), "BC",
                Mercado.EUA, Moeda.USD, BigDecimal.TEN, obtained, ReferenciaTipoCotacao.DATA, null, original, obtained));
        var h = historicos.saveAndFlush(new HistoricoCotacao(a, BigDecimal.TEN, obtained,
                ReferenciaTipoCotacao.DATA, null, original, obtained));
        Long aid = a.getId(), hid = h.getId();
        for (String table : new String[]{"acao", "historico_cotacao"}) {
            Long id = table.equals("acao") ? aid : hid;
            jdbc.update(c -> {
                var s = c.prepareStatement("update " + table + " set referencia_data = ? where id = ?");
                s.setDate(1, java.sql.Date.valueOf(original)); s.setLong(2, id); return s;
            });
        }
        String previous = jdbc.queryForObject("select referencia_data, cast(referencia_data as varchar) from acao where id = ?", (r, row) -> {
            assertTrue(r.getString(2).endsWith(" BC"));
            return assertThrows(DateTimeException.class, () -> r.getDate(1).toLocalDate()).getMessage();
        }, aid);
        em.clear();
        assertLegacyError(previous, () -> acoes.findById(aid));
        em.clear();
        assertLegacyError(previous, () -> historicos.findById(hid));
        em.clear();
        assertLegacyError(previous, () -> mvc.perform(get("/acoes/{id}", aid)));
    }

    private void assertLegacyError(String expected, org.junit.jupiter.api.function.Executable operation) {
        Throwable error = assertThrows(Exception.class, operation);
        while (error.getCause() != null) error = error.getCause();
        assertInstanceOf(DateTimeException.class, error);
        assertEquals(expected, error.getMessage());
    }
}
