package com.projeto.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Reuses the complete H2 matrix against an explicitly isolated disposable cluster. */
@EnabledIfSystemProperty(named = "broker.test.postgresql.enabled", matches = "true")
class DistribuicaoPosicaoCorretoraPostgresqlTest extends DistribuicaoPosicaoCorretoraServiceIntegrationTest {
    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        String url = System.getProperty("broker.test.postgresql.url", "");
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:(?!5432/)[0-9]+/postgres")) {
            throw new IllegalStateException("Requires a disposable loopback cluster outside port 5432");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> "broker_disposable_test");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeEach
    void confirmarDestino() {
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(s -> {
            var connection = em.unwrap(org.hibernate.Session.class);
            connection.doWork(c -> {
                assertEquals(System.getProperty("broker.test.postgresql.url"), c.getMetaData().getURL());
                assertEquals("PostgreSQL", c.getMetaData().getDatabaseProductName());
                assertEquals("broker_disposable_test", c.getMetaData().getUserName());
            });
        });
    }
}
