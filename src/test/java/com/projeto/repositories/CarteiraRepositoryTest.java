package com.projeto.repositories;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Carteira;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
@Transactional
class CarteiraRepositoryTest {

    private static final OffsetDateTime CREATION_DATE =
            OffsetDateTime.of(2026, 8, 21, 14, 30, 0, 0, ZoneOffset.UTC);

    @Autowired
    private CarteiraRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibaseAndHibernatePersistPortfolioWithExpectedSchemaAndUtcTimestamp() {
        Carteira saved = repository.saveAndFlush(new Carteira("Carteira Principal", CREATION_DATE));

        Carteira found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Carteira Principal", found.getNome());
        assertEquals(CREATION_DATE.toInstant(), found.getDataCriacao().toInstant());

        Integer changelogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID = '003-create-carteira'",
                Integer.class
        );
        assertEquals(1, changelogCount);

        Integer nameLength = jdbcTemplate.queryForObject("""
                SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'CARTEIRA' AND COLUMN_NAME = 'NOME'
                """, Integer.class);
        assertEquals(255, nameLength);

        assertEquals("NO", nullableFor("NOME"));
        assertEquals("NO", nullableFor("DATA_CRIACAO"));
    }

    @Test
    void allowsDuplicateNamesWithIndependentIds() {
        Carteira first = repository.saveAndFlush(new Carteira("Carteira Principal", CREATION_DATE));
        Carteira second = repository.saveAndFlush(new Carteira("Carteira Principal", CREATION_DATE));

        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void accepts255CharactersAndRejectsLongerName() {
        Carteira accepted = repository.saveAndFlush(new Carteira("a".repeat(255), CREATION_DATE));
        assertEquals(255, accepted.getNome().length());

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new Carteira("a".repeat(256), CREATION_DATE))
        );
    }

    @Test
    void rejectsNullName() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new Carteira(null, CREATION_DATE))
        );
    }

    @Test
    void rejectsNullCreationDate() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new Carteira("Carteira Principal", null))
        );
    }

    private String nullableFor(String columnName) {
        return jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'CARTEIRA' AND COLUMN_NAME = ?
                """, String.class, columnName);
    }
}
