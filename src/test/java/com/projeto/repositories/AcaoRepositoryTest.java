package com.projeto.repositories;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
@Transactional
class AcaoRepositoryTest {

    @Autowired
    private AcaoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibaseAndHibernatePersistCompleteActionWithTextEnumsNumericScaleAndUtcTimestamp() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-20T15:30:00Z");
        Acao saved = repository.saveAndFlush(action(
                "PETR4", Mercado.BRASIL, Moeda.BRL, new BigDecimal("32.123456"), timestamp
        ));

        Acao found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("PETR4", found.getTicker());
        assertEquals("Empresa", found.getNomeEmpresa());
        assertEquals(Mercado.BRASIL, found.getMercado());
        assertEquals(Moeda.BRL, found.getMoeda());
        assertEquals(new BigDecimal("32.123456"), found.getCotacaoAtual());
        assertEquals(timestamp.toInstant(), found.getDataHoraCotacao().toInstant());
        assertTrue(repository.existsByTickerAndMercado("PETR4", Mercado.BRASIL));

        Integer scale = jdbcTemplate.queryForObject("""
                SELECT NUMERIC_SCALE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'ACAO' AND COLUMN_NAME = 'COTACAO_ATUAL'
                """, Integer.class);
        assertEquals(6, scale);
    }

    @Test
    void compositeUniqueConstraintAllowsSameTickerInDifferentMarketsButRejectsSamePair() {
        repository.saveAndFlush(action(
                "ABC", Mercado.BRASIL, Moeda.BRL, new BigDecimal("10.000000"), utcNow()
        ));
        repository.saveAndFlush(action(
                "ABC", Mercado.EUA, Moeda.USD, new BigDecimal("11.000000"), utcNow()
        ));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(action(
                        "ABC", Mercado.BRASIL, Moeda.BRL, new BigDecimal("12.000000"), utcNow()
                ))
        );
    }

    @Test
    void databaseCheckRejectsWrongMarketCurrencyPair() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO acao(ticker, nome_empresa, mercado, moeda, cotacao_atual, data_hora_cotacao)
                VALUES ('AAPL', 'Apple', 'EUA', 'BRL', 10.000000, CURRENT_TIMESTAMP)
                """));
    }

    @Test
    void databaseCheckRejectsNonPositiveQuote() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO acao(ticker, nome_empresa, mercado, moeda, cotacao_atual, data_hora_cotacao)
                VALUES ('ZERO3', 'Empresa', 'BRASIL', 'BRL', 0.000000, CURRENT_TIMESTAMP)
                """));
    }

    @Test
    void listsActionsByAscendingIdWhenRowsWereInsertedOutOfOrder() {
        insertAction(200L, "MSFT", "Microsoft Corporation", "EUA", "USD", "501.250000");
        insertAction(100L, "PETR4", "Petróleo Brasileiro S.A.", "BRASIL", "BRL", "32.123456");

        List<Acao> listed = repository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        assertEquals(List.of(100L, 200L), listed.stream().map(Acao::getId).toList());
        assertEquals(List.of("PETR4", "MSFT"), listed.stream().map(Acao::getTicker).toList());
    }

    @Test
    void findsActionByIdWithoutChangingPersistedQuoteOrTimestampAndReturnsEmptyForMissingId() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-20T12:30:00-03:00");
        Acao saved = repository.saveAndFlush(action(
                "AAPL", Mercado.EUA, Moeda.USD, new BigDecimal("224.410000"), timestamp
        ));

        Acao found = repository.findById(saved.getId()).orElseThrow();

        assertEquals("AAPL", found.getTicker());
        assertEquals("Empresa", found.getNomeEmpresa());
        assertEquals(Mercado.EUA, found.getMercado());
        assertEquals(Moeda.USD, found.getMoeda());
        assertEquals(new BigDecimal("224.410000"), found.getCotacaoAtual());
        assertEquals(timestamp.toInstant(), found.getDataHoraCotacao().toInstant());
        assertTrue(repository.findById(Long.MAX_VALUE).isEmpty());
    }

    private Acao action(
            String ticker,
            Mercado market,
            Moeda currency,
            BigDecimal quote,
            OffsetDateTime timestamp
    ) {
        return new Acao(ticker, "Empresa", market, currency, quote, timestamp);
    }

    private OffsetDateTime utcNow() {
        return OffsetDateTime.of(2026, 8, 20, 15, 30, 0, 0, ZoneOffset.UTC);
    }

    private void insertAction(
            Long id,
            String ticker,
            String companyName,
            String market,
            String currency,
            String quote
    ) {
        jdbcTemplate.update("""
                INSERT INTO acao(
                    id, ticker, nome_empresa, mercado, moeda, cotacao_atual, data_hora_cotacao
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                ticker,
                companyName,
                market,
                currency,
                new BigDecimal(quote),
                utcNow()
        );
    }
}
