package com.projeto.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MovimentacaoCustodiaTest {
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T03:30:00Z"), ZoneOffset.UTC);
    static final PosicaoRacionalCustodia POSICAO = PosicaoRacionalCustodia.deTexto("1", "2");
    static final LocalDate DIA = LocalDate.of(2026, 10, 11);

    static void definirCampo(Object objeto, String campo, Object valor) {
        try {
            var field = objeto.getClass().getDeclaredField(campo);
            field.setAccessible(true);
            field.set(objeto, valor);
        } catch (ReflectiveOperationException erro) {
            throw new AssertionError(erro);
        }
    }

    static void simularPostLoad(Object objeto) {
        String metodo = objeto instanceof MovimentacaoCustodia
                ? "validarCamposHistoricosLocais" : "aposCarregamento";
        try {
            var callback = objeto.getClass().getDeclaredMethod(metodo);
            callback.setAccessible(true);
            callback.invoke(objeto);
        } catch (java.lang.reflect.InvocationTargetException erro) {
            if (erro.getCause() instanceof RuntimeException causa) throw causa;
            throw new AssertionError(erro.getCause());
        } catch (ReflectiveOperationException erro) {
            throw new AssertionError(erro);
        }
    }

    static MovimentacaoCustodia nova(Long id, Long carteiraId, Long acaoId, Mercado mercado,
            NaturezaMovimentacaoCustodia natureza, Long origemId, Long destinoId,
            BigDecimal quantidade, LocalDate dia, Integer ancora, PosicaoRacionalCustodia posicao, Clock clock) {
        Carteira carteira = null;
        if (carteiraId != null) {
            carteira = mock(Carteira.class);
            when(carteira.getId()).thenReturn(carteiraId);
        }
        Acao acao = null;
        if (acaoId != null) {
            acao = mock(Acao.class);
            when(acao.getId()).thenReturn(acaoId);
            when(acao.getMercado()).thenReturn(mercado);
        }
        return new MovimentacaoCustodia(id, carteira, acao, natureza, corretora(origemId),
                corretora(destinoId), quantidade, dia, ancora, posicao, clock);
    }

    private static Corretora corretora(Long id) {
        if (id == null) return null;
        Corretora corretora = mock(Corretora.class);
        when(corretora.getId()).thenReturn(id);
        return corretora;
    }

    static MovimentacaoCustodia criar(Long id, Mercado mercado, NaturezaMovimentacaoCustodia natureza,
            Long origem, Long destino, BigDecimal quantidade, LocalDate dia) {
        return nova(id, 1L, 2L, mercado, natureza, origem, destino, quantidade, dia, 0, POSICAO, CLOCK);
    }

    static MovimentacaoCustodia original(Long id) {
        return criar(id, Mercado.BRASIL, NaturezaMovimentacaoCustodia.TRANSFERENCIA,
                3L, 4L, new BigDecimal("100.000000"), DIA);
    }

    @Test
    void transferenciaPreservaIdentidadeDirecaoQuantidadeEDatas() {
        var m = original(10L);
        assertAll(() -> assertEquals(10L, m.getId()),
                () -> assertEquals(1L, m.getCarteiraId()), () -> assertEquals(2L, m.getAcaoId()),
                () -> assertEquals(Mercado.BRASIL, m.getMercado()),
                () -> assertEquals(NaturezaMovimentacaoCustodia.TRANSFERENCIA, m.getNatureza()),
                () -> assertEquals(3L, m.getCorretoraOrigemId()),
                () -> assertEquals(4L, m.getCorretoraDestinoId()),
                () -> assertEquals(new BigDecimal("100.000000"), m.getQuantidade()),
                () -> assertEquals(DIA, m.getDataEfetiva()),
                () -> assertEquals(CLOCK.instant(), m.getDataRegistro().toInstant()),
                () -> assertEquals(SituacaoMovimentacaoCustodia.ATIVA, m.getSituacao(VinculoEstornoConsultado.ausenciaVerificada(m.getId()))));
        assertArrayEquals(new TipoOperacao[]{TipoOperacao.COMPRA, TipoOperacao.VENDA}, TipoOperacao.values());
        assertEquals(2, NaturezaMovimentacaoCustodia.values().length);
    }

    @Test
    void atribuicaoMantemGrupoNuloEQuantidadeEuaExataSemGerarId() {
        var m = criar(null, Mercado.EUA, NaturezaMovimentacaoCustodia.ATRIBUICAO,
                null, 4L, new BigDecimal("0.000001"), DIA);
        assertNull(m.getId());
        assertNull(m.getCorretoraOrigemId());
        assertEquals(new BigDecimal("0.000001"), m.getQuantidade());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.0000001", "1.0000000", "10000000000000", "9999999999999.000001"})
    void rejeitaQuantidadeInvalidaBrasil(String quantidade) {
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, new BigDecimal(quantidade), DIA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.0000001", "10000000000000"})
    void rejeitaQuantidadeInvalidaEua(String quantidade) {
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.EUA,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, new BigDecimal(quantidade), DIA));
    }

    @Test
    void aceitaLimitesExatosEInteiroComZerosDecimais() {
        assertEquals(new BigDecimal("9999999999999.999999"), criar(10L, Mercado.EUA,
                NaturezaMovimentacaoCustodia.ATRIBUICAO, null, 4L,
                new BigDecimal("9999999999999.999999"), DIA).getQuantidade());
        assertEquals(new BigDecimal("9999999999999.000000"), criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.ATRIBUICAO, null, 4L,
                new BigDecimal("9999999999999.000000"), DIA).getQuantidade());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1000000, -13})
    void rejeitaEscalaNegativaExcessivaAntesDeExpandir(int escala) {
        BigDecimal quantidade = new BigDecimal(BigInteger.ONE, escala);
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            for (Mercado mercado : Mercado.values()) {
                var erro = assertThrowsExactly(IllegalArgumentException.class,
                        () -> criar(10L, mercado, NaturezaMovimentacaoCustodia.TRANSFERENCIA,
                                3L, 4L, quantidade, DIA));
                assertEquals("Quantidade excede NUMERIC(19,6)", erro.getMessage());
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
            "BRASIL, 1E+3, 1000.000000",
            "BRASIL, 1E+12, 1000000000000.000000",
            "BRASIL, 9.999999999999E+12, 9999999999999.000000",
            "EUA, 1E-6, 0.000001",
            "EUA, 9.999999999999999999E+12, 9999999999999.999999"
    })
    void preservaNotacaoCientificaValidaELimiteDePrecisao(Mercado mercado, String entrada, String esperada) {
        var m = criar(10L, mercado, NaturezaMovimentacaoCustodia.TRANSFERENCIA,
                3L, 4L, new BigDecimal(entrada), DIA);
        assertEquals(new BigDecimal(esperada), m.getQuantidade());
        assertEquals(6, m.getQuantidade().scale());
        assertTrue(m.getQuantidade().precision() <= 19);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1E+13", "1.0000000000000000000E+13", "1E-7", "9.9999999999999999999E+12"})
    void rejeitaNotacaoCientificaForaDosLimitesSemArredondar(String entrada) {
        for (Mercado mercado : Mercado.values()) {
            assertThrowsExactly(IllegalArgumentException.class, () -> criar(10L, mercado,
                    NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, new BigDecimal(entrada), DIA));
        }
    }

    @Test
    void notacaoCientificaFracionariaContinuaInvalidaBrasil() {
        assertThrowsExactly(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, new BigDecimal("1E-6"), DIA));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejeitaReferenciasObrigatoriasInvalidas(Long id) {
        assertThrows(IllegalArgumentException.class, () -> nova(10L, id, 2L,
                Mercado.BRASIL, NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, BigDecimal.ONE, DIA, 0, POSICAO, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> nova(10L, 1L, id,
                Mercado.BRASIL, NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, BigDecimal.ONE, DIA, 0, POSICAO, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, id, 4L, BigDecimal.ONE, DIA));
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.ATRIBUICAO, null, id, BigDecimal.ONE, DIA));
    }

    @Test
    void rejeitaDirecoesIncompativeisComNatureza() {
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 3L, BigDecimal.ONE, DIA));
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.ATRIBUICAO, 3L, 4L, BigDecimal.ONE, DIA));
    }

    @Test
    void rejeitaAusenciasEIdentidadeInvalida() {
        assertThrows(IllegalArgumentException.class, () -> original(0L));
        assertThrows(IllegalArgumentException.class, () -> original(-1L));
        assertThrows(IllegalArgumentException.class, () -> criar(10L, null,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, BigDecimal.ONE, DIA));
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                null, 3L, 4L, BigDecimal.ONE, DIA));
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, null, DIA));
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, BigDecimal.ONE, null));
        assertThrows(IllegalArgumentException.class, () -> nova(10L, 1L, 2L,
                Mercado.BRASIL, NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, BigDecimal.ONE, DIA, 0, POSICAO, null));
    }

    @Test
    void validaDiaCorrenteNaZonaDoMercadoEPermiteRetroatividade() {
        LocalDate diaUtc = LocalDate.of(2026, 10, 12);
        assertEquals(diaUtc, criar(10L, Mercado.BRASIL, NaturezaMovimentacaoCustodia.TRANSFERENCIA,
                3L, 4L, BigDecimal.ONE, diaUtc).getDataEfetiva());
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.EUA,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, BigDecimal.ONE, diaUtc));
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, BigDecimal.ONE, diaUtc.plusDays(1)));
        assertEquals(DIA, original(10L).getDataEfetiva());
    }

    @Test
    void preservaPosicaoEAncoraRecebidasSemSelecionarOuAlterar() {
        var posicao = PosicaoRacionalCustodia.reconstruirPersistida("3", "5");
        var m = nova(10L, 1L, 2L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, BigDecimal.ONE,
                DIA, Integer.MAX_VALUE, posicao, CLOCK);
        assertEquals(posicao, m.getPosicaoRacional());
        assertEquals(Integer.MAX_VALUE, m.getOrdemFinanceiraAncora());
        assertEquals(0, original(10L).getOrdemFinanceiraAncora());
        posicao.entre(PosicaoRacionalCustodia.FIM);
        assertEquals("3/5", m.getPosicaoRacional().toString());
        assertEquals(DIA, m.getDataEfetiva());
        assertEquals(CLOCK.instant(), m.getDataRegistro().toInstant());
    }

    @Test
    void rejeitaPosicaoAusenteEFronteiras() {
        for (PosicaoRacionalCustodia posicao : new PosicaoRacionalCustodia[]{
                null, PosicaoRacionalCustodia.INICIO, PosicaoRacionalCustodia.FIM}) {
            assertThrowsExactly(IllegalArgumentException.class, () -> nova(
                    10L, 1L, 2L, Mercado.BRASIL, NaturezaMovimentacaoCustodia.ATRIBUICAO,
                    null, 4L, BigDecimal.ONE, DIA, 0, posicao, CLOCK));
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void rejeitaAncoraAusenteOuNegativa(Integer ancora) {
        assertThrowsExactly(IllegalArgumentException.class, () -> nova(
                10L, 1L, 2L, Mercado.BRASIL, NaturezaMovimentacaoCustodia.ATRIBUICAO,
                null, 4L, BigDecimal.ONE, DIA, ancora, POSICAO, CLOCK));
    }
}
