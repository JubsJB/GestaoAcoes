package com.projeto.repositories;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Carteira;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    void findsAllPortfoliosByAscendingIdAndReturnsEmptyListWithoutRecords() {
        Sort ascendingId = Sort.by(Sort.Direction.ASC, "id");
        assertTrue(repository.findAll(ascendingId).isEmpty());

        Carteira first = repository.saveAndFlush(new Carteira(
                "Carteira Ágil",
                OffsetDateTime.parse("2026-08-19T09:15:00Z")
        ));
        Carteira second = repository.saveAndFlush(new Carteira(
                "carteira Principal",
                OffsetDateTime.parse("2026-08-20T10:30:00Z")
        ));

        List<Carteira> found = repository.findAll(ascendingId);

        assertEquals(List.of(first.getId(), second.getId()), found.stream().map(Carteira::getId).toList());
    }

    @Test
    void findsPortfolioByIdAndPreservesPersistedNameAndCreationDate() {
        String persistedName = "  Carteira Ágil sem normalização  ";
        OffsetDateTime persistedDate = OffsetDateTime.parse("2026-08-18T08:05:00Z");
        Carteira saved = repository.saveAndFlush(new Carteira(persistedName, persistedDate));

        Carteira found = repository.findById(saved.getId()).orElseThrow();

        assertEquals(persistedName, found.getNome());
        assertEquals(persistedDate, found.getDataCriacao());
        assertTrue(repository.findById(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void updatesOnlyNameAndPreservesIdAndCreationDate() {
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-08-17T07:45:00Z");
        Carteira saved = repository.saveAndFlush(new Carteira("Carteira Original", creationDate));
        Long originalId = saved.getId();

        saved.atualizarNome("Carteira Atualizada");
        repository.saveAndFlush(saved);

        Carteira found = repository.findById(originalId).orElseThrow();
        assertEquals(originalId, found.getId());
        assertEquals("Carteira Atualizada", found.getNome());
        assertEquals(creationDate, found.getDataCriacao());
        assertEquals(1, repository.count());
    }

    @Test
    void allowsDuplicateNamesAfterUpdate() {
        Carteira first = repository.saveAndFlush(new Carteira("Carteira Principal", CREATION_DATE));
        Carteira second = repository.saveAndFlush(new Carteira("Carteira Secundária", CREATION_DATE));

        second.atualizarNome("Carteira Principal");
        repository.saveAndFlush(second);

        List<Carteira> found = repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        assertEquals(List.of(first.getId(), second.getId()), found.stream().map(Carteira::getId).toList());
        assertEquals(List.of("Carteira Principal", "Carteira Principal"),
                found.stream().map(Carteira::getNome).toList());
    }

    @Test
    void physicallyDeletesOnlySelectedPortfolioAndPreservesOthers() {
        OffsetDateTime deletedDate = OffsetDateTime.parse("2026-08-16T06:25:00Z");
        OffsetDateTime preservedDate = OffsetDateTime.parse("2026-08-15T05:15:00Z");
        Carteira deleted = repository.saveAndFlush(new Carteira("Carteira Excluída", deletedDate));
        Carteira preserved = repository.saveAndFlush(new Carteira("Carteira Preservada", preservedDate));

        repository.delete(deleted);
        repository.flush();

        assertTrue(repository.findById(deleted.getId()).isEmpty());
        Carteira unchanged = repository.findById(preserved.getId()).orElseThrow();
        assertEquals("Carteira Preservada", unchanged.getNome());
        assertEquals(preservedDate, unchanged.getDataCriacao());
        assertEquals(1, repository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackDeletionAndPreservesAllPortfoliosWhenTransactionFails() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long[] ids = transaction.execute(status -> {
            Carteira first = repository.saveAndFlush(new Carteira("Carteira Um", CREATION_DATE));
            Carteira second = repository.saveAndFlush(new Carteira("Carteira Dois", CREATION_DATE));
            return new Long[]{first.getId(), second.getId()};
        });

        try {
            assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
                Carteira target = repository.findById(ids[0]).orElseThrow();
                repository.delete(target);
                repository.flush();
                throw new IllegalStateException("Falha simulada antes da conclusão da transação");
            }));

            assertTrue(repository.existsById(ids[0]));
            assertTrue(repository.existsById(ids[1]));
            assertEquals(2, repository.count());
        } finally {
            repository.deleteAll();
        }
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
