package com.projeto.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class EstornoMovimentacaoCustodiaTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"), ZoneOffset.UTC);

    @ParameterizedTest
    @EnumSource(NaturezaMovimentacaoCustodia.class)
    void herdaCronologiaDeTransferenciaEAtribuicaoSemInverter(NaturezaMovimentacaoCustodia natureza) {
        var posicao = PosicaoRacionalCustodia.deTexto("2", "3");
        Long origem = natureza == NaturezaMovimentacaoCustodia.TRANSFERENCIA ? 3L : null;
        var original = MovimentacaoCustodiaTest.nova(10L, 1L, 2L, Mercado.BRASIL,
                natureza, origem, 4L, BigDecimal.TEN, MovimentacaoCustodiaTest.DIA,
                7, posicao, MovimentacaoCustodiaTest.CLOCK);
        var estorno = new EstornoMovimentacaoCustodia(20L, original, null, CLOCK);
        assertSame(original, estorno.getMovimentacaoOriginal());
        assertEquals(posicao, estorno.getPosicaoRacional());
        assertEquals(7, estorno.getOrdemFinanceiraAncora());
        assertEquals(original.getDataEfetiva(), estorno.getDataEfetiva());
        assertEquals(CLOCK.instant(), estorno.getDataRegistro().toInstant());
        assertEquals(MovimentacaoCustodiaTest.CLOCK.instant(), original.getDataRegistro().toInstant());
        assertEquals(origem, original.getCorretoraOrigemId());
        assertEquals(4L, original.getCorretoraDestinoId());
        assertEquals(natureza, original.getNatureza());
        assertEquals(new BigDecimal("10.000000"), original.getQuantidade());
    }

    @Test
    void preservaOriginalEDerivaSituacaoSemDuplicarDataEfetiva() {
        var original = MovimentacaoCustodiaTest.original(10L);
        var estorno = new EstornoMovimentacaoCustodia(10L, original, "  Correção justificada  ", CLOCK);
        assertSame(original, estorno.getMovimentacaoOriginal());
        assertEquals(10L, estorno.getId()); // Identidades de domínios diferentes podem coincidir.
        assertEquals(original.getId(), estorno.getMovimentacaoOriginalId());
        assertEquals(original.getDataEfetiva(), estorno.getDataEfetiva());
        assertEquals(CLOCK.instant(), estorno.getDataRegistro().toInstant());
        assertEquals(MovimentacaoCustodiaTest.CLOCK.instant(), original.getDataRegistro().toInstant());
        assertEquals("  Correção justificada  ", estorno.getMotivo());
        assertEquals(SituacaoMovimentacaoCustodia.ESTORNADA, original.getSituacao(VinculoEstornoConsultado.vinculoPresente(10L, estorno)));
        assertEquals(SituacaoMovimentacaoCustodia.ATIVA, original.getSituacao(VinculoEstornoConsultado.ausenciaVerificada(original.getId())));
        assertEquals(3L, original.getCorretoraOrigemId());
        assertEquals(4L, original.getCorretoraDestinoId());
    }

    @Test
    void aceitaMotivoAusenteOuBrancoENormalizaSomenteBranco() {
        var original = MovimentacaoCustodiaTest.original(10L);
        for (String motivo : new String[]{null, "", " \t\n "}) {
            assertNull(new EstornoMovimentacaoCustodia(null, original, motivo, CLOCK).getMotivo());
        }
        assertEquals("a".repeat(1000), new EstornoMovimentacaoCustodia(null, original,
                "a".repeat(1000), CLOCK).getMotivo());
        assertThrows(IllegalArgumentException.class, () -> new EstornoMovimentacaoCustodia(null,
                original, "a".repeat(1001), CLOCK));
    }

    @Test
    void rejeitaOriginalRelogioOuIdentidadeInvalidos() {
        var original = MovimentacaoCustodiaTest.original(10L);
        assertThrows(IllegalArgumentException.class, () -> new EstornoMovimentacaoCustodia(null, null, null, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new EstornoMovimentacaoCustodia(null, original, null, null));
        assertThrows(IllegalArgumentException.class, () -> new EstornoMovimentacaoCustodia(0L, original, null, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new EstornoMovimentacaoCustodia(-1L, original, null, CLOCK));
    }

    @Test
    void rejeitaVinculoDeOutraOriginalEAceitaMesmaIdentidadeEmOutraInstancia() {
        var original = MovimentacaoCustodiaTest.original(10L);
        var estorno = new EstornoMovimentacaoCustodia(20L, original, null, CLOCK);
        assertThrows(IllegalArgumentException.class, () -> MovimentacaoCustodiaTest.original(11L).getSituacao(VinculoEstornoConsultado.vinculoPresente(10L, estorno)));
        assertEquals(SituacaoMovimentacaoCustodia.ESTORNADA,
                MovimentacaoCustodiaTest.original(10L).getSituacao(VinculoEstornoConsultado.vinculoPresente(10L, estorno)));
        var semId = MovimentacaoCustodiaTest.original(null);
        var vinculoSemId = new EstornoMovimentacaoCustodia(null, semId, null, CLOCK);
        assertThrows(IllegalArgumentException.class, () -> semId.getSituacao(
                VinculoEstornoConsultado.vinculoPresente(10L, vinculoSemId)));
        assertThrows(IllegalArgumentException.class, () -> VinculoEstornoConsultado.ausenciaVerificada(null));
    }
}
