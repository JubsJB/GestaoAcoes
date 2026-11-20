package com.projeto.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VinculoEstornoConsultadoTest {
    @Test
    void ausenciaVerificadaProduzAtivaMasInformacaoNaoFornecidaFalha() {
        var original = MovimentacaoCustodiaTest.original(10L);
        assertThrows(IllegalArgumentException.class, () -> original.getSituacao(null));
        var ausencia = VinculoEstornoConsultado.ausenciaVerificada(10L);
        assertNull(ausencia.getEstorno());
        assertEquals(SituacaoMovimentacaoCustodia.ATIVA, original.getSituacao(ausencia));
        assertThrows(IllegalArgumentException.class,
                () -> original.getSituacao(VinculoEstornoConsultado.ausenciaVerificada(11L)));
    }

    @Test
    void vinculoDeclaradoPelaConsultaProduzEstornadaIndependentementeDePostLoad() {
        // Fixtures em memória representam os resultados que o futuro fluxo deverá consultar.
        var original = MovimentacaoCustodiaTest.original(10L);
        var estorno = new EstornoMovimentacaoCustodia(20L, original, null, MovimentacaoCustodiaTest.CLOCK);
        var resultado = VinculoEstornoConsultado.vinculoPresente(10L, estorno);
        assertSame(estorno, resultado.getEstorno());
        assertEquals(SituacaoMovimentacaoCustodia.ESTORNADA, original.getSituacao(resultado));
        MovimentacaoCustodiaTest.simularPostLoad(estorno);
        assertEquals(SituacaoMovimentacaoCustodia.ESTORNADA, original.getSituacao(resultado));
        assertThrows(IllegalArgumentException.class,
                () -> MovimentacaoCustodiaTest.original(11L).getSituacao(resultado));
    }

    @Test
    void rejeitaResultadoCujoEstornoReferenciaOutraOriginal() {
        var original = MovimentacaoCustodiaTest.original(10L);
        var estorno = new EstornoMovimentacaoCustodia(20L, MovimentacaoCustodiaTest.original(11L),
                null, MovimentacaoCustodiaTest.CLOCK);
        assertThrows(IllegalArgumentException.class,
                () -> original.getSituacao(VinculoEstornoConsultado.vinculoPresente(10L, estorno)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejeitaIdentidadeDeConsultaInvalida(Long id) {
        assertThrows(IllegalArgumentException.class, () -> VinculoEstornoConsultado.ausenciaVerificada(id));
        var estorno = new EstornoMovimentacaoCustodia(20L, MovimentacaoCustodiaTest.original(10L),
                null, MovimentacaoCustodiaTest.CLOCK);
        assertThrows(IllegalArgumentException.class, () -> VinculoEstornoConsultado.vinculoPresente(id, estorno));
    }

    @Test
    void rejeitaPresencaSemVinculoOuComEstornoSemIdentidade() {
        assertThrows(IllegalArgumentException.class, () -> VinculoEstornoConsultado.vinculoPresente(10L, null));
        var original = MovimentacaoCustodiaTest.original(10L);
        var estorno = new EstornoMovimentacaoCustodia(null, original, null, MovimentacaoCustodiaTest.CLOCK);
        assertThrows(IllegalArgumentException.class,
                () -> original.getSituacao(VinculoEstornoConsultado.vinculoPresente(10L, estorno)));
    }

    @Test
    void vinculoProxyPendenteEhRejeitadoSemInicializacao() {
        var proxy = mock(EstornoMovimentacaoCustodia.class, withSettings().extraInterfaces(HibernateProxy.class));
        when(((HibernateProxy) proxy).asHibernateProxy()).thenReturn((HibernateProxy) proxy);
        var initializer = mock(LazyInitializer.class);
        when(((HibernateProxy) proxy).getHibernateLazyInitializer()).thenReturn(initializer);
        when(initializer.isUninitialized()).thenReturn(true);
        var resultado = VinculoEstornoConsultado.vinculoPresente(10L, proxy);
        assertThrows(IllegalStateException.class, () -> MovimentacaoCustodiaTest.original(10L).getSituacao(resultado));
        verify(proxy, never()).validarEstruturaHistorica();
        verify(initializer, never()).initialize();
    }
}
