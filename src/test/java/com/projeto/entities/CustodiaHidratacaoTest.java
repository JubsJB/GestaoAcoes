package com.projeto.entities;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import static com.projeto.entities.MovimentacaoCustodiaTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustodiaHidratacaoTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"), ZoneOffset.UTC);

    static Stream<Arguments> camposInvalidos() {
        return Stream.of(
                Arguments.of("id", null), Arguments.of("id", 0L),
                Arguments.of("carteira", null), Arguments.of("acao", null),
                Arguments.of("natureza", null), Arguments.of("corretoraOrigem", null),
                Arguments.of("corretoraDestino", null), Arguments.of("quantidade", null),
                Arguments.of("quantidade", BigDecimal.ZERO), Arguments.of("quantidade", BigDecimal.ONE.negate()),
                Arguments.of("quantidade", new BigDecimal("0.0000001")),
                Arguments.of("quantidade", new BigDecimal("10000000000000")),
                Arguments.of("quantidade", new BigDecimal(BigInteger.ONE, Integer.MIN_VALUE)),
                Arguments.of("dataEfetiva", null), Arguments.of("dataRegistro", null),
                Arguments.of("ordemFinanceiraAncora", null), Arguments.of("ordemFinanceiraAncora", -1),
                Arguments.of("posicaoNumerador", null), Arguments.of("posicaoNumerador", "0"),
                Arguments.of("posicaoNumerador", "1".repeat(513)),
                Arguments.of("posicaoNumerador", "01"), Arguments.of("posicaoDenominador", "0"),
                Arguments.of("posicaoDenominador", "1"), Arguments.of("posicaoNumerador", "2"));
    }

    @ParameterizedTest
    @MethodSource("camposInvalidos")
    void callbackRejeitaEstruturaLocalInvalida(String campo, Object valor) {
        var m = original(10L);
        definirCampo(m, campo, valor);
        assertThrows(IllegalArgumentException.class, () -> simularPostLoad(m));
    }

    @Test
    void rejeitaFracaoNaoReduzidaSemCorrigirCampos() {
        var m = original(10L);
        definirCampo(m, "posicaoNumerador", "2");
        definirCampo(m, "posicaoDenominador", "4");
        assertThrowsExactly(IllegalArgumentException.class, () -> simularPostLoad(m));
        assertThrowsExactly(IllegalArgumentException.class, m::getPosicaoRacional);
    }

    @Test
    void historicoPreservaDatasOffsetENanosSemRelogioOuNormalizacao() {
        var m = original(10L);
        var diaFuturo = LocalDate.of(2050, 1, 2);
        var registro = OffsetDateTime.parse("2001-01-01T10:11:12.123456789-03:00");
        var quantidade = new BigDecimal("10.000");
        definirCampo(m, "dataEfetiva", diaFuturo);
        definirCampo(m, "dataRegistro", registro);
        definirCampo(m, "quantidade", quantidade);
        simularPostLoad(m);
        assertDoesNotThrow(m::validarEstruturaHistorica);
        assertSame(registro, m.getDataRegistro());
        assertSame(diaFuturo, m.getDataEfetiva());
        assertSame(quantidade, m.getQuantidade());
        assertEquals(3, m.getQuantidade().scale());
        assertThrows(IllegalArgumentException.class, () -> criar(10L, Mercado.BRASIL,
                NaturezaMovimentacaoCustodia.TRANSFERENCIA, 3L, 4L, BigDecimal.TEN, diaFuturo));
    }

    @Test
    void integralidadeDependeDeAcaoCarregadaSemConsultaNoCallback() {
        var m = criar(10L, Mercado.EUA, NaturezaMovimentacaoCustodia.TRANSFERENCIA,
                3L, 4L, new BigDecimal("0.000001"), DIA);
        var acao = m.getAcao();
        clearInvocations(acao);
        simularPostLoad(m);
        verifyNoInteractions(acao);
        assertDoesNotThrow(m::validarEstruturaHistorica);
        when(acao.getMercado()).thenReturn(Mercado.BRASIL);
        assertThrows(IllegalArgumentException.class, m::validarEstruturaHistorica);
        when(acao.getMercado()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, m::validarEstruturaHistorica);
    }

    @Test
    void proxyNaoInicializadoNaoEhAcessadoPeloCallbackNemInicializadoPelaValidacao() {
        var m = original(10L);
        var proxy = mock(Acao.class, withSettings().extraInterfaces(HibernateProxy.class));
        when(((HibernateProxy) proxy).asHibernateProxy()).thenReturn((HibernateProxy) proxy);
        var initializer = mock(LazyInitializer.class);
        when(((HibernateProxy) proxy).getHibernateLazyInitializer()).thenReturn(initializer);
        when(initializer.isUninitialized()).thenReturn(true);
        definirCampo(m, "acao", proxy);
        clearInvocations(proxy, initializer);
        assertDoesNotThrow(() -> simularPostLoad(m));
        verifyNoInteractions(proxy, initializer);
        assertThrows(IllegalStateException.class, m::validarEstruturaHistorica);
        verify(proxy, never()).getMercado();
        verify(proxy, never()).getId();
        verify(initializer, never()).initialize();
    }

    @Test
    void direcaoEIdsSaoValidadosForaDoCallback() {
        var m = original(10L);
        when(m.getCorretoraDestino().getId()).thenReturn(3L);
        assertDoesNotThrow(() -> simularPostLoad(m));
        assertThrows(IllegalArgumentException.class, m::validarEstruturaHistorica);
        var atribuicao = criar(10L, Mercado.EUA, NaturezaMovimentacaoCustodia.ATRIBUICAO,
                null, 4L, BigDecimal.ONE, DIA);
        assertDoesNotThrow(atribuicao::validarEstruturaHistorica);
        definirCampo(atribuicao, "corretoraOrigem", m.getCorretoraOrigem());
        assertThrows(IllegalArgumentException.class, () -> simularPostLoad(atribuicao));
    }

    static Stream<Arguments> estornosInvalidos() {
        return Stream.of(Arguments.of("id", null), Arguments.of("id", 0L),
                Arguments.of("movimentacaoOriginal", null), Arguments.of("dataRegistro", null),
                Arguments.of("motivo", "a".repeat(1001)));
    }

    @ParameterizedTest
    @MethodSource("estornosInvalidos")
    void callbackDeEstornoRejeitaEstruturaLocalInvalida(String campo, Object valor) {
        var estorno = new EstornoMovimentacaoCustodia(20L, original(10L), null, CLOCK);
        definirCampo(estorno, campo, valor);
        assertThrows(IllegalArgumentException.class, () -> simularPostLoad(estorno));
    }

    @Test
    void estornoHistoricoPreservaOriginalEValoresSemNormalizar() {
        var m = original(10L);
        var estorno = new EstornoMovimentacaoCustodia(20L, m, "Justificativa", CLOCK);
        var registro = OffsetDateTime.parse("2002-01-01T10:11:12.123456789+05:00");
        definirCampo(estorno, "dataRegistro", registro);
        definirCampo(estorno, "motivo", "  Justificativa  ");
        simularPostLoad(estorno);
        assertDoesNotThrow(estorno::validarEstruturaHistorica);
        assertSame(registro, estorno.getDataRegistro());
        assertEquals("  Justificativa  ", estorno.getMotivo());
        assertSame(m, estorno.getMovimentacaoOriginal());
        assertEquals(m.getPosicaoRacional(), estorno.getPosicaoRacional());
        assertEquals(MovimentacaoCustodiaTest.CLOCK.instant(), m.getDataRegistro().toInstant());
    }

    @Test
    void callbackDoEstornoNaoInicializaOriginalLazy() {
        var estorno = new EstornoMovimentacaoCustodia(20L, original(10L), null, CLOCK);
        var proxy = mock(MovimentacaoCustodia.class, withSettings().extraInterfaces(HibernateProxy.class));
        when(((HibernateProxy) proxy).asHibernateProxy()).thenReturn((HibernateProxy) proxy);
        var initializer = mock(LazyInitializer.class);
        when(((HibernateProxy) proxy).getHibernateLazyInitializer()).thenReturn(initializer);
        when(initializer.isUninitialized()).thenReturn(true);
        definirCampo(estorno, "movimentacaoOriginal", proxy);
        clearInvocations(proxy, initializer);
        assertDoesNotThrow(() -> simularPostLoad(estorno));
        verifyNoInteractions(proxy, initializer);
        assertThrows(IllegalStateException.class, estorno::validarEstruturaHistorica);
        verify(proxy, never()).validarEstruturaHistorica();
        verify(initializer, never()).initialize();
    }

    @Test
    void candidataMesmoComIdNaoComprovaVinculoHistorico() {
        var m = original(10L);
        var candidata = new EstornoMovimentacaoCustodia(20L, m, null, CLOCK);
        assertDoesNotThrow(candidata::validarEstruturaHistorica);
        assertThrows(IllegalArgumentException.class, () -> m.getSituacao(null));
        simularPostLoad(candidata); // Validação local não fornece resultado de consulta.
        assertThrows(IllegalArgumentException.class, () -> m.getSituacao(null));
    }
}
