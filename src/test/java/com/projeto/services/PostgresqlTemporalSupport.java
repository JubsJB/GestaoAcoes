package com.projeto.services;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in exclusivo de testes; nunca usa configuração/credenciais de desenvolvimento. */
final class PostgresqlTemporalSupport {
    private PostgresqlTemporalSupport() { }

    static void configurar(DynamicPropertyRegistry registry) {
        String url = System.getProperty("temporal.test.postgresql.url", "");
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/postgres")
                || !"codex_etapa_d".equals(System.getProperty("temporal.test.postgresql.user"))) {
            throw new IllegalStateException("Requer cluster temporário loopback e usuário codex_etapa_d explícitos");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "codex_etapa_d");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10");
    }

    static void conferirBanco(JdbcTemplate jdbc) {
        assertEquals("codex_etapa_d", jdbc.queryForObject("select current_user", String.class));
        assertEquals(16, jdbc.queryForObject("select current_setting('server_version_num')::integer / 10000", Integer.class));
    }

    static void esperarBloqueio(JdbcTemplate jdbc, int bloqueadora, int bloqueada) {
        long limite = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < limite) {
            Boolean bloqueio = jdbc.queryForObject("select exists (select 1 from pg_stat_activity "
                    + "where pid = ? and ? = any(pg_blocking_pids(pid)) and wait_event_type = 'Lock' "
                    + "and lower(query) like '%for%')", Boolean.class, bloqueada, bloqueadora);
            if (Boolean.TRUE.equals(bloqueio)) {
                System.out.println("POSTGRESQL_LOCK blocker=" + bloqueadora + " blocked=" + bloqueada
                        + " " + jdbc.queryForMap("select wait_event_type,wait_event,query from pg_stat_activity where pid=?", bloqueada));
                return;
            }
            Thread.onSpinWait();
        }
        fail("PostgreSQL deve comprovar sessão aguardando lock da primeira transação");
    }
}
