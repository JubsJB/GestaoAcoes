package com.projeto.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcaoTest {

    @Test
    void aplicaObservacaoAceitaPreservandoCadastroENormalizandoUtc() {
        Acao acao = acao();

        aplicar(acao,
                new BigDecimal("35.000000"),
                OffsetDateTime.parse("2026-08-20T12:30:00-03:00")
        );

        assertEquals("PETR4", acao.getTicker());
        assertEquals("Empresa", acao.getNomeEmpresa());
        assertEquals(Mercado.BRASIL, acao.getMercado());
        assertEquals(Moeda.BRL, acao.getMoeda());
        assertEquals(new BigDecimal("35.000000"), acao.getCotacaoAtual());
        assertEquals(OffsetDateTime.parse("2026-08-20T15:30:00Z"), acao.getDataHoraCotacao());
    }

    @Test
    void aplicaObservacaoAceitaComPrecoIgual() {
        Acao acao = acao();

        aplicar(acao,
                new BigDecimal("30.000000"),
                OffsetDateTime.parse("2026-08-20T15:30:00Z")
        );

        assertEquals(new BigDecimal("30.000000"), acao.getCotacaoAtual());
        assertEquals(OffsetDateTime.parse("2026-08-20T15:30:00Z"), acao.getDataHoraCotacao());
    }

    @Test
    void rejeitaObservacaoInvalidaSemMutarEstado() {
        Acao acao = acao();

        assertThrows(IllegalArgumentException.class, () -> aplicar(acao, null, posterior()));
        assertThrows(IllegalArgumentException.class, () -> aplicar(acao, BigDecimal.ZERO, posterior()));
        assertThrows(IllegalArgumentException.class, () -> aplicar(acao, BigDecimal.ONE.negate(), posterior()));
        assertThrows(IllegalArgumentException.class, () -> aplicar(acao, BigDecimal.TEN, null));
        assertEquals(new BigDecimal("30.000000"), acao.getCotacaoAtual());
        assertEquals(ReferenciaTipoCotacao.DESCONHECIDA, acao.getReferenciaTipo());
    }

    private Acao acao() {
        return new Acao(
                "PETR4", "Empresa", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("30.000000"), OffsetDateTime.parse("2026-08-19T15:30:00Z")
        );
    }

    private OffsetDateTime posterior() {
        return OffsetDateTime.parse("2026-08-20T15:30:00Z");
    }
    private void aplicar(Acao acao, BigDecimal preco, OffsetDateTime instante) {
        acao.substituirObservacao(preco, ReferenciaTipoCotacao.INSTANTE, instante, null, instante);
    }

}
