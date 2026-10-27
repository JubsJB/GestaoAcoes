package com.projeto.repositories;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.*;
import java.time.*;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Diagnóstico em tabelas temporárias; não modifica o mapeamento de produção. */
class ReferenciaDataJdbcDiagnosticoTest {
    static final String[] DATES = {"0001-01-01", "0004-02-29", "0100-03-01", "0400-02-29",
            "1500-03-01", "1582-10-04", "1582-10-15", "1600-02-29", "1900-03-01",
            "2000-02-29", "2026-10-02", "9999-12-31"};

    static Stream<Arguments> cases() {
        return Stream.of("h2", "postgresql").flatMap(db -> Stream.of("UTC", "America/Sao_Paulo",
                "Pacific/Kiritimati").flatMap(zone -> Stream.of(DATES).map(date -> Arguments.of(db, zone, date))));
    }

    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("cases")
    void comparaBindingAnteriorDiretoELeituraExistente(String db, String zone, String text) throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            String url = db.equals("h2") ? "jdbc:h2:mem:probe_" + UUID.randomUUID()
                    : System.getProperty("temporal.test.postgresql.url", "");
            if (db.equals("postgresql")) {
                assumeTrue(!url.isEmpty(), "Requer PostgreSQL temporário explicitamente configurado");
                assertTrue(url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/postgres"));
                assertEquals("codex_etapa_d", System.getProperty("temporal.test.postgresql.user"));
            }
            try (Connection c = DriverManager.getConnection(url, db.equals("h2") ? "sa" : "codex_etapa_d", "")) {
                LocalDate original = LocalDate.parse(text);
                try (Statement s = c.createStatement()) {
                    // Controle experimental: evita cache do fuso inicial do H2 ao variar TimeZone na mesma JVM.
                    if (db.equals("h2")) s.execute("set time zone '" + zone + "'");
                    s.execute("create temporary table temporal_date_probe (kind varchar(12), civil_date date)");
                }
                try (PreparedStatement s = c.prepareStatement("insert into temporal_date_probe values (?, ?)")) {
                    s.setString(1, "anterior"); s.setDate(2, java.sql.Date.valueOf(original)); s.executeUpdate();
                    s.setString(1, "direto"); s.setObject(2, original, Types.DATE); s.executeUpdate();
                }
                try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(
                        "select kind, civil_date, cast(civil_date as varchar) from temporal_date_probe order by kind")) {
                    while (r.next()) {
                        String kind = r.getString(1), sql = r.getString(3);
                        LocalDate direct = r.getObject(2, LocalDate.class);
                        String old;
                        try { old = r.getDate(2).toLocalDate().toString(); }
                        catch (DateTimeException e) { old = "ERROR:" + e.getMessage(); }
                        System.out.printf("JDBC_PROBE db=%s driver=%s zone=%s input=%s binding=%s sql=%s oldRead=%s directRead=%s reinterpretation=%s%n",
                                db, c.getMetaData().getDriverVersion(), zone, original, kind, sql, old, direct, !old.equals(direct.toString()));
                        assertEquals(direct.toString(), sql);
                        if (kind.equals("direto")) assertEquals(original, direct, "Binding civil direto deve preservar a data");
                        if (db.equals("postgresql")) assertEquals(original, direct, "PostgreSQL deve preservar escrita anterior");
                        if (db.equals("h2") && zone.equals("America/Sao_Paulo") && text.equals("0001-01-01") && kind.equals("anterior")) {
                            assertEquals("0000-12-29", sql);
                            assertEquals("0001-12-31", old);
                            assertNotEquals(old, direct.toString(), "Trocar leitor reinterpreta registro antigo");
                        }
                    }
                }
                java.sql.Date legacy = java.sql.Date.valueOf(original);
                var calendar = new java.util.GregorianCalendar(TimeZone.getTimeZone(zone));
                calendar.setTime(legacy);
                System.out.printf("CALENDAR_PROBE zone=%s input=%s sqlDateText=%s epochMillis=%s hybridEra=%s hybridYear=%s proleptic=%s%n",
                        zone, original, legacy, legacy.getTime(), calendar.get(java.util.Calendar.ERA),
                        calendar.get(java.util.Calendar.YEAR), Instant.ofEpochMilli(legacy.getTime()).atZone(ZoneId.of(zone)).toLocalDate());
            }
        } finally { TimeZone.setDefault(previous); }
    }
}
