package com.projeto.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class ReferenciaTemporalCotacaoTest {
    private static final OffsetDateTime LEGADO = OffsetDateTime.parse("2026-10-02T12:00:00-03:00");
    private static final LocalDate DATA = LocalDate.of(2026, 10, 2);
    private static final BigDecimal PRECO = new BigDecimal("250.123456");

    @Test
    void construtoresLegadosNaoInferemReferenciaOuObtencao() {
        Acao acao = new Acao("AAPL", "Apple", Mercado.EUA, Moeda.USD, PRECO, LEGADO);
        HistoricoCotacao historico = new HistoricoCotacao(acao, PRECO, LEGADO);
        assertEquals(ReferenciaTipoCotacao.DESCONHECIDA, acao.getReferenciaTipo());
        assertEquals(ReferenciaTipoCotacao.DESCONHECIDA, historico.getReferenciaTipo());
        assertNull(acao.getReferenciaInstante());
        assertNull(acao.getReferenciaData());
        assertNull(acao.getCotacaoObtidaEm());
        assertNull(historico.getReferenciaInstante());
        assertNull(historico.getReferenciaData());
        assertNull(historico.getCotacaoObtidaEm());
        assertEquals(PRECO, acao.getCotacaoAtual());
        assertEquals(LEGADO, acao.getDataHoraCotacao());
        assertEquals(PRECO, historico.getCotacao());
        assertEquals(LEGADO, historico.getDataHoraCotacao());
    }

    @ParameterizedTest
    @EnumSource(ReferenciaTipoCotacao.class)
    void preservaPrecisaoENormalizaSomenteInstantesConhecidos(ReferenciaTipoCotacao tipo) {
        OffsetDateTime instante = tipo == ReferenciaTipoCotacao.INSTANTE ? LEGADO : null;
        LocalDate data = tipo == ReferenciaTipoCotacao.DATA ? DATA : null;
        Acao acao = acao(tipo, instante, data, LEGADO.plusMinutes(1));
        HistoricoCotacao historico = new HistoricoCotacao(
                acao, PRECO, LEGADO, tipo, instante, data, LEGADO.plusMinutes(1));
        assertEquals(tipo, acao.getReferenciaTipo());
        assertEquals(tipo, historico.getReferenciaTipo());
        assertEquals(data, acao.getReferenciaData());
        assertEquals(data, historico.getReferenciaData());
        assertEquals(instante == null ? null : instante.withOffsetSameInstant(ZoneOffset.UTC),
                acao.getReferenciaInstante());
        assertEquals(acao.getReferenciaInstante(), historico.getReferenciaInstante());
        assertEquals(ZoneOffset.UTC, acao.getCotacaoObtidaEm().getOffset());
        assertEquals(acao.getCotacaoObtidaEm(), historico.getCotacaoObtidaEm());
        assertEquals(LEGADO, acao.getDataHoraCotacao());
    }

    @Test
    void rejeitaCombinacoesInvalidasNasDuasEntidades() {
        for (ReferenciaTipoCotacao tipo : ReferenciaTipoCotacao.values()) {
            for (OffsetDateTime instante : new OffsetDateTime[]{null, LEGADO}) {
                for (LocalDate data : new LocalDate[]{null, DATA}) {
                    boolean valida = switch (tipo) {
                        case INSTANTE -> instante != null && data == null;
                        case DATA -> data != null && instante == null;
                        case DESCONHECIDA -> instante == null && data == null;
                    };
                    if (valida) continue;
                    assertThrows(IllegalArgumentException.class, () -> acao(tipo, instante, data, null));
                    assertThrows(IllegalArgumentException.class, () -> new HistoricoCotacao(
                            acao(ReferenciaTipoCotacao.DESCONHECIDA, null, null, null),
                            PRECO, LEGADO, tipo, instante, data, null));
                }
            }
        }
        assertThrows(IllegalArgumentException.class, () -> acao(null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new HistoricoCotacao(
                acao(ReferenciaTipoCotacao.DESCONHECIDA, null, null, null), PRECO, LEGADO,
                null, null, null, null));
    }

    private Acao acao(ReferenciaTipoCotacao tipo, OffsetDateTime instante,
                      LocalDate data, OffsetDateTime obtidaEm) {
        return new Acao("AAPL", "Apple", Mercado.EUA, Moeda.USD, PRECO, LEGADO,
                tipo, instante, data, obtidaEm);
    }
}
