package com.projeto.repositories;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Acao;
import com.projeto.entities.HistoricoCotacao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
@Transactional
class HistoricoCotacaoRepositoryTest {

    @Autowired
    private AcaoRepository acaoRepository;

    @Autowired
    private HistoricoCotacaoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persisteCamposAprovadosComAssociacaoLazyEOrdenacaoTemporal() {
        Acao acao = acaoRepository.saveAndFlush(acao("PETR4", Mercado.BRASIL, Moeda.BRL));
        OffsetDateTime primeiro = OffsetDateTime.parse("2026-08-20T10:00:00Z");
        OffsetDateTime segundo = OffsetDateTime.parse("2026-08-20T11:00:00Z");
        repository.saveAndFlush(new HistoricoCotacao(acao, new BigDecimal("35.500000"), segundo));
        repository.saveAndFlush(new HistoricoCotacao(acao, new BigDecimal("35.500000"), primeiro));

        entityManager.clear();
        List<HistoricoCotacao> encontrados = repository.findByAcaoIdOrderByDataHoraCotacaoAsc(acao.getId());

        assertEquals(List.of(primeiro.toInstant(), segundo.toInstant()), encontrados.stream()
                .map(item -> item.getDataHoraCotacao().toInstant()).toList());
        assertEquals(new BigDecimal("35.500000"), encontrados.get(0).getCotacao());
        PersistenceUnitUtil persistence = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertFalse(persistence.isLoaded(encontrados.get(0), "acao"));
    }

    @Test
    void unicidadeIsolaAcoesEMantemMesmoPrecoEmInstantesDiferentes() {
        Acao primeira = acaoRepository.saveAndFlush(acao("AAA3", Mercado.BRASIL, Moeda.BRL));
        Acao segunda = acaoRepository.saveAndFlush(acao("BBB3", Mercado.BRASIL, Moeda.BRL));
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-20T10:00:00Z");
        repository.saveAndFlush(new HistoricoCotacao(primeira, new BigDecimal("10.000000"), timestamp));
        repository.saveAndFlush(new HistoricoCotacao(primeira, new BigDecimal("10.000000"), timestamp.plusHours(1)));
        repository.saveAndFlush(new HistoricoCotacao(segunda, new BigDecimal("10.000000"), timestamp));

        assertEquals(2, repository.findByAcaoIdOrderByDataHoraCotacaoAsc(primeira.getId()).size());
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(
                new HistoricoCotacao(primeira, new BigDecimal("11.000000"), timestamp)));
    }

    @Test
    void schemaPossuiEscalaCheckFkEImpedeCascadeDelete() {
        Integer scale = jdbcTemplate.queryForObject("""
                SELECT NUMERIC_SCALE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'HISTORICO_COTACAO' AND COLUMN_NAME = 'COTACAO'
                """, Integer.class);
        assertEquals(6, scale);

        Acao acao = acaoRepository.saveAndFlush(acao("FKT3", Mercado.BRASIL, Moeda.BRL));
        repository.saveAndFlush(new HistoricoCotacao(
                acao, new BigDecimal("20.000000"), OffsetDateTime.parse("2026-08-20T10:00:00Z")));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("DELETE FROM acao WHERE id = ?", acao.getId()));
    }

    private Acao acao(String ticker, Mercado mercado, Moeda moeda) {
        return new Acao(
                ticker, "Empresa", mercado, moeda, new BigDecimal("10.000000"),
                OffsetDateTime.parse("2026-08-19T10:00:00Z")
        );
    }
}
