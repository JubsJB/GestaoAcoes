package com.projeto.repositories;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Corretora;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
class OperacaoRepositoryTest {

    @Autowired
    private OperacaoRepository operacaoRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private AcaoRepository acaoRepository;

    @Autowired
    private CorretoraRepository corretoraRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        operacaoRepository.deleteAll();
        carteiraRepository.deleteAll();
        acaoRepository.deleteAll();
        corretoraRepository.deleteAll();
    }

    @Test
    void liquibaseAndHibernatePersistCompleteOperationWithApprovedTypesAndNullableBroker() {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        Acao acao = acaoRepository.saveAndFlush(action("AAPL", Mercado.EUA, Moeda.USD));

        Operacao saved = operacaoRepository.saveAndFlush(operation(
                carteira,
                acao,
                null,
                TipoOperacao.COMPRA,
                "0.123456",
                "32.123456",
                "3.965833383936",
                LocalDate.of(2026, 8, 10),
                1
        ));
        entityManager.clear();

        Operacao found = operacaoRepository.findById(saved.getId()).orElseThrow();
        assertEquals(carteira.getId(), found.getCarteira().getId());
        assertEquals(acao.getId(), found.getAcao().getId());
        assertNull(found.getCorretora());
        assertEquals(TipoOperacao.COMPRA, found.getTipo());
        assertEquals(new BigDecimal("0.123456"), found.getQuantidade());
        assertEquals(new BigDecimal("32.123456"), found.getPrecoUnitario());
        assertEquals(LocalDate.of(2026, 8, 10), found.getDataOperacao());
        assertEquals(1, found.getOrdemNoDia());
        assertEquals(new BigDecimal("3.965833383936"), found.getValorTotal());
        assertTrue(operacaoRepository.existsByCarteiraId(carteira.getId()));
    }

    @Test
    void persistsOptionalBrokerAndReturnsHistoryOrderedOnlyByDateAndDailyOrder() {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        Acao acao = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        Corretora corretora = corretoraRepository.saveAndFlush(broker());

        operacaoRepository.saveAndFlush(operation(
                carteira, acao, corretora, TipoOperacao.VENDA,
                "10", "12", "120", LocalDate.of(2026, 8, 10), 2
        ));
        operacaoRepository.saveAndFlush(operation(
                carteira, acao, null, TipoOperacao.COMPRA,
                "10", "10", "100", LocalDate.of(2026, 8, 1), 5
        ));
        operacaoRepository.saveAndFlush(operation(
                carteira, acao, null, TipoOperacao.COMPRA,
                "10", "11", "110", LocalDate.of(2026, 8, 10), 1
        ));

        List<Operacao> ordered = operacaoRepository
                .findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(
                        carteira.getId(), acao.getId()
                );

        assertEquals(
                List.of(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 10),
                        LocalDate.of(2026, 8, 10)
                ),
                ordered.stream().map(Operacao::getDataOperacao).toList()
        );
        assertEquals(List.of(5, 1, 2), ordered.stream().map(Operacao::getOrdemNoDia).toList());
        assertEquals(corretora.getId(), ordered.get(2).getCorretora().getId());
    }

    @Test
    void enforcesUniqueChronologicalOrder() {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        Acao acao = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        LocalDate date = LocalDate.of(2026, 8, 10);
        operacaoRepository.saveAndFlush(operation(
                carteira, acao, null, TipoOperacao.COMPRA,
                "1", "10", "10", date, 1
        ));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> operacaoRepository.saveAndFlush(operation(
                        carteira, acao, null, TipoOperacao.COMPRA,
                        "2", "10", "20", date, 1
                ))
        );
    }

    @Test
    void databaseChecksRejectInvalidTypePositiveFieldsAndInexactTotal() {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        Acao acao = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));

        assertInvalidInsert(carteira.getId(), acao.getId(), "OUTRO", "1", "10", 1, "10");
        assertInvalidInsert(carteira.getId(), acao.getId(), "COMPRA", "0", "10", 2, "0");
        assertInvalidInsert(carteira.getId(), acao.getId(), "COMPRA", "1", "0", 3, "0");
        assertInvalidInsert(carteira.getId(), acao.getId(), "COMPRA", "1", "10", 0, "10");
        assertInvalidInsert(carteira.getId(), acao.getId(), "COMPRA", "2", "10", 4, "19");
    }

    @Test
    void foreignKeysRejectUnknownRelationshipsAndNeverCascadeDeleteHistory() {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        Acao acao = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        Corretora corretora = corretoraRepository.saveAndFlush(broker());
        operacaoRepository.saveAndFlush(operation(
                carteira, acao, corretora, TipoOperacao.COMPRA,
                "1", "10", "10", LocalDate.of(2026, 8, 10), 1
        ));
        entityManager.clear();

        assertThrows(DataIntegrityViolationException.class, () -> {
            carteiraRepository.deleteById(carteira.getId());
            carteiraRepository.flush();
        });
        assertTrue(operacaoRepository.existsByCarteiraId(carteira.getId()));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO operacao(
                    carteira_id, acao_id, corretora_id, tipo, quantidade,
                    preco_unitario, data_operacao, ordem_no_dia, valor_total
                ) VALUES (?, ?, NULL, 'COMPRA', 1, 10, DATE '2026-08-11', 1, 10)
                """, Long.MAX_VALUE, acao.getId()));
    }

    @Test
    void schemaHasApprovedPrecisionConstraintsIndexesAndNoDeleteCascade() {
        assertColumn("QUANTIDADE", 19, 6);
        assertColumn("PRECO_UNITARIO", 19, 6);
        assertColumn("VALOR_TOTAL", 38, 12);

        List<String> indexes = jdbcTemplate.queryForList("""
                SELECT DISTINCT INDEX_NAME
                FROM INFORMATION_SCHEMA.INDEX_COLUMNS
                WHERE TABLE_NAME = 'OPERACAO'
                """, String.class);
        assertTrue(indexes.contains("IDX_OPERACAO_CARTEIRA_ACAO_CRONOLOGIA"));
        assertTrue(indexes.contains("IDX_OPERACAO_ACAO_ID"));
        assertTrue(indexes.contains("IDX_OPERACAO_CORRETORA_ID"));

        List<String> deleteRules = jdbcTemplate.queryForList("""
                SELECT DELETE_RULE
                FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
                WHERE CONSTRAINT_NAME LIKE 'FK_OPERACAO_%'
                """, String.class);
        assertEquals(3, deleteRules.size());
        assertTrue(deleteRules.stream().noneMatch("CASCADE"::equals));
    }

    @Test
    void transactionRollbackLeavesNoPartialOperation() {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        Acao acao = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        long before = operacaoRepository.count();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            operacaoRepository.saveAndFlush(operation(
                    carteira, acao, null, TipoOperacao.COMPRA,
                    "1", "10", "10", LocalDate.of(2026, 8, 10), 1
            ));
            throw new IllegalStateException("force rollback");
        }));

        assertEquals(before, operacaoRepository.count());
        assertFalse(operacaoRepository.existsByCarteiraId(carteira.getId()));
    }

    private void assertColumn(String column, int precision, int scale) {
        Integer actualPrecision = jdbcTemplate.queryForObject("""
                SELECT NUMERIC_PRECISION FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'OPERACAO' AND COLUMN_NAME = ?
                """, Integer.class, column);
        Integer actualScale = jdbcTemplate.queryForObject("""
                SELECT NUMERIC_SCALE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'OPERACAO' AND COLUMN_NAME = ?
                """, Integer.class, column);
        assertEquals(precision, actualPrecision);
        assertEquals(scale, actualScale);
    }

    private void assertInvalidInsert(
            Long portfolioId,
            Long actionId,
            String type,
            String quantity,
            String price,
            int order,
            String total
    ) {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO operacao(
                    carteira_id, acao_id, corretora_id, tipo, quantidade,
                    preco_unitario, data_operacao, ordem_no_dia, valor_total
                ) VALUES (?, ?, NULL, ?, ?, ?, DATE '2026-08-10', ?, ?)
                """,
                portfolioId,
                actionId,
                type,
                new BigDecimal(quantity),
                new BigDecimal(price),
                order,
                new BigDecimal(total)
        ));
    }

    private Operacao operation(
            Carteira carteira,
            Acao acao,
            Corretora corretora,
            TipoOperacao type,
            String quantity,
            String price,
            String total,
            LocalDate date,
            Integer order
    ) {
        return new Operacao(
                carteira,
                acao,
                corretora,
                type,
                new BigDecimal(quantity).setScale(6),
                new BigDecimal(price).setScale(6),
                date,
                order,
                new BigDecimal(total).setScale(12)
        );
    }

    private Carteira portfolio(String name) {
        return new Carteira(name, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private Acao action(String ticker, Mercado market, Moeda currency) {
        return new Acao(
                ticker,
                "Empresa",
                market,
                currency,
                new BigDecimal("99.123456"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
    }

    private Corretora broker() {
        return new Corretora(
                "11222333000181",
                "Corretora S.A.",
                null,
                null,
                null,
                "01001000",
                "Praça da Sé",
                null,
                null,
                "Sé",
                "São Paulo",
                "SP",
                "ATIVA",
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
    }
}
