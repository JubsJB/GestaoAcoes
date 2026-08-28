package com.projeto.repositories;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Carteira;
import com.projeto.entities.Moeda;
import com.projeto.entities.SnapshotCarteira;
import com.projeto.entities.SnapshotCarteiraMoeda;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.transaction.TransactionDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
@Transactional
class SnapshotCarteiraRepositoryTest {

    @Autowired CarteiraRepository carteiraRepository;
    @Autowired SnapshotCarteiraRepository snapshotRepository;
    @Autowired SnapshotCarteiraMoedaRepository componenteRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void persistsParentWithoutChildrenAndAllowsSameDayAndSameTimestampAcrossPortfolios() {
        Carteira primeira = carteiraRepository.saveAndFlush(carteira("Primeira"));
        Carteira segunda = carteiraRepository.saveAndFlush(carteira("Segunda"));
        OffsetDateTime instante = OffsetDateTime.parse("2026-08-27T15:00:00Z");

        snapshotRepository.saveAndFlush(new SnapshotCarteira(primeira, instante));
        snapshotRepository.saveAndFlush(new SnapshotCarteira(primeira, instante.plusHours(1)));
        snapshotRepository.saveAndFlush(new SnapshotCarteira(segunda, instante));

        assertEquals(3, snapshotRepository.count());
        assertEquals(0, componenteRepository.count());
        assertTrue(snapshotRepository.existsByCarteiraId(primeira.getId()));
    }

    @Test
    void rejectsDuplicatePortfolioTimestampAndDuplicateCurrency() {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira("Principal"));
        OffsetDateTime instante = OffsetDateTime.parse("2026-08-27T15:00:00Z");
        snapshotRepository.saveAndFlush(new SnapshotCarteira(carteira, instante));
        assertThrows(DataIntegrityViolationException.class, () ->
                snapshotRepository.saveAndFlush(new SnapshotCarteira(carteira, instante)));

        // A transação fica marcada após a violação; a unique monetária é coberta em teste separado.
    }

    @Test
    void rejectsDuplicateCurrencyAndDoesNotCascadeDeleteHistory() {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira("Principal"));
        SnapshotCarteira snapshot = snapshotRepository.saveAndFlush(new SnapshotCarteira(
                carteira,
                OffsetDateTime.parse("2026-08-27T16:00:00Z")
        ));
        componenteRepository.saveAndFlush(new SnapshotCarteiraMoeda(
                snapshot, Moeda.BRL, new BigDecimal("10.000000000000")
        ));
        assertThrows(DataIntegrityViolationException.class, () -> componenteRepository.saveAndFlush(
                new SnapshotCarteiraMoeda(snapshot, Moeda.BRL, new BigDecimal("20.000000000000"))
        ));
        assertFalse(snapshotRepository.findById(snapshot.getId()).isEmpty());
    }

    @Test
    void schemaHasApprovedNumericChecksForeignKeysAndNoDeleteCascade() {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira("Checks"));
        SnapshotCarteira snapshot = snapshotRepository.saveAndFlush(new SnapshotCarteira(
                carteira, OffsetDateTime.parse("2026-08-27T17:00:00Z")));
        Integer precision = jdbcTemplate.queryForObject("""
                SELECT NUMERIC_PRECISION FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'SNAPSHOT_CARTEIRA_MOEDA' AND COLUMN_NAME = 'PATRIMONIO_ATUAL'
                """, Integer.class);
        Integer scale = jdbcTemplate.queryForObject("""
                SELECT NUMERIC_SCALE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'SNAPSHOT_CARTEIRA_MOEDA' AND COLUMN_NAME = 'PATRIMONIO_ATUAL'
                """, Integer.class);
        List<String> deleteRules = jdbcTemplate.queryForList("""
                SELECT DELETE_RULE FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
                WHERE CONSTRAINT_NAME IN ('FK_SNAPSHOT_CARTEIRA_CARTEIRA', 'FK_SNAPSHOT_CARTEIRA_MOEDA_SNAPSHOT')
                """, String.class);

        assertEquals(38, precision);
        assertEquals(12, scale);
        assertEquals(2, deleteRules.size());
        assertTrue(deleteRules.stream().noneMatch("CASCADE"::equals));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO snapshot_carteira_moeda(snapshot_carteira_id, moeda, patrimonio_atual) VALUES (?, 'EUR', 1)",
                snapshot.getId()));
    }

    @Test
    void transactionRollbackRemovesParentAndEveryComponent() {
        long parentCount = snapshotRepository.count();
        long childCount = componenteRepository.count();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            Carteira carteira = carteiraRepository.saveAndFlush(carteira("Rollback"));
            SnapshotCarteira snapshot = snapshotRepository.saveAndFlush(new SnapshotCarteira(
                    carteira, OffsetDateTime.parse("2026-08-27T18:00:00Z")));
            componenteRepository.saveAndFlush(new SnapshotCarteiraMoeda(
                    snapshot, Moeda.BRL, new BigDecimal("1.000000000000")));
            throw new IllegalStateException("force rollback");
        }));

        assertEquals(parentCount, snapshotRepository.count());
        assertEquals(childCount, componenteRepository.count());
    }

    private Carteira carteira(String nome) {
        return new Carteira(nome, OffsetDateTime.parse("2026-01-01T00:00:00Z"));
    }
}
