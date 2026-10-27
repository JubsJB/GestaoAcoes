package com.projeto.services;

import com.projeto.entities.LimitePosicaoCustodiaException;
import com.projeto.entities.PosicaoRacionalCustodia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PosicaoRacionalTransitoriaTest {
    private static final BigInteger UM = BigInteger.ONE;
    private static final BigInteger D = BigInteger.TEN.pow(512).subtract(UM);
    private PosicaoRacionalTransitoria f(long n, long d) {
        return PosicaoRacionalTransitoria.criar(BigInteger.valueOf(n), BigInteger.valueOf(d));
    }
    @Test void conversaoPersistivelParaTransitoriaPreservaComponentes() {
        var persistivel = PosicaoRacionalCustodia.criar(UM, D);
        var transitoria = PosicaoRacionalTransitoria.dePersistivel(persistivel);
        assertEquals(persistivel.getNumerador(), transitoria.getNumerador());
        assertEquals(persistivel.getDenominador(), transitoria.getDenominador());
        assertEquals(persistivel, transitoria.paraPersistivel());
    }
    @Test void conversaoDentroDoLimiteEhExplicitaEExata() {
        var transitoria = f(2, 3);
        assertEquals(PosicaoRacionalCustodia.deTexto("2", "3"), transitoria.paraPersistivel());
        assertFalse(PosicaoRacionalCustodia.class.isAssignableFrom(PosicaoRacionalTransitoria.class));
    }
    @ParameterizedTest @ValueSource(ints = {513, 1024})
    void componentesGrandesSaoSimulaveisMasNaoPersistiveis(int digitos) {
        var denominador = BigInteger.TEN.pow(digitos - 1);
        var transitoria = PosicaoRacionalTransitoria.criar(UM, denominador);
        assertEquals(digitos, transitoria.getDenominador().toString().length());
        assertThrows(LimitePosicaoCustodiaException.class, transitoria::paraPersistivel);
    }
    @Test void numeradorGrandeTambemEhRejeitadoNaConversao() {
        var n = BigInteger.TEN.pow(512);
        var transitoria = PosicaoRacionalTransitoria.criar(n, n.add(UM));
        assertThrows(LimitePosicaoCustodiaException.class, transitoria::paraPersistivel);
    }
    @Test void mediantaDoIntervaloEsgotadoPossui513Digitos() {
        var superior = PosicaoRacionalTransitoria.dePersistivel(PosicaoRacionalCustodia.criar(UM, D));
        var candidata = PosicaoRacionalTransitoria.INICIO.entre(superior);
        assertEquals(UM, candidata.getNumerador());
        assertEquals(BigInteger.TEN.pow(512), candidata.getDenominador());
        assertEquals(513, candidata.getDenominador().toString().length());
        assertTrue(PosicaoRacionalTransitoria.INICIO.compareTo(candidata) < 0);
        assertTrue(candidata.compareTo(superior) < 0);
        assertThrows(LimitePosicaoCustodiaException.class, candidata::paraPersistivel);
        assertEquals(D, superior.getDenominador());
    }
    @Test void ordenaFracoesGrandesDistintasSemArredondamento() {
        var d = BigInteger.TEN.pow(1024);
        var menor = PosicaoRacionalTransitoria.criar(UM, d.add(UM));
        var maior = PosicaoRacionalTransitoria.criar(UM, d);
        assertTrue(menor.compareTo(maior) < 0);
        assertTrue(maior.compareTo(menor) > 0);
        assertEquals(List.of(menor, maior), List.of(maior, menor).stream().sorted().toList());
    }
    @Test void canonicalizaAntesDeAvaliarConversaoPersistivel() {
        var fator = BigInteger.TEN.pow(700);
        var transitoria = PosicaoRacionalTransitoria.criar(fator, fator.multiply(BigInteger.TWO));
        assertEquals(UM, transitoria.getNumerador());
        assertEquals(BigInteger.TWO, transitoria.getDenominador());
        assertEquals(PosicaoRacionalCustodia.deTexto("1", "2"), transitoria.paraPersistivel());
    }
    @Test void igualdadeHashEOrdenacaoSaoCanonicos() {
        var a = f(2, 3); var b = f(20, 30);
        assertEquals(a, b); assertEquals(a.hashCode(), b.hashCode());
        assertEquals(0, a.compareTo(b)); assertEquals(1, new HashSet<>(List.of(a, b)).size());
        assertNotEquals(a, null); assertNotEquals(a, "2/3");
        assertEquals("2/3", a.toString());
    }
    @ParameterizedTest @CsvSource({"0,1", "1,1", "2,1", "-1,2", "1,0", "1,-2"})
    void rejeitaPosicoesEfetivasInvalidas(long n, long d) {
        assertThrows(IllegalArgumentException.class, () -> f(n, d));
    }
    @Test void rejeitaComponentesEConversaoAusentes() {
        assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalTransitoria.criar(null, BigInteger.TWO));
        assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalTransitoria.criar(UM, null));
        assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalTransitoria.dePersistivel(null));
    }
    @Test void fronteirasSaoSentinelasENuncaPosicoesEfetivasOuPersistidas() {
        var inicio = PosicaoRacionalTransitoria.dePersistivel(PosicaoRacionalCustodia.INICIO);
        var fim = PosicaoRacionalTransitoria.dePersistivel(PosicaoRacionalCustodia.FIM);
        assertEquals(PosicaoRacionalTransitoria.INICIO, inicio);
        assertEquals(PosicaoRacionalTransitoria.FIM, fim);
        assertEquals(f(1, 2), inicio.entre(fim));
        for (var limite : List.of(inicio, fim)) {
            assertThrows(IllegalArgumentException.class, limite::exigirEfetiva);
            assertThrows(IllegalArgumentException.class, limite::paraPersistivel);
            assertThrows(IllegalArgumentException.class, () -> PosicaoCronologicaQuantitativa.custodiaTransitoria(LocalDate.of(2026, 1, 1), 0, limite));
        }
    }
    @Test void mediantaExigeLimitesOrdenados() {
        assertThrows(IllegalArgumentException.class, () -> f(1, 2).entre(f(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> f(2, 3).entre(f(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> f(1, 2).entre(null));
    }
    @Test void fabricaTransitoriaNaoConfundeAusenciaComEventoFinanceiro() {
        assertThrows(IllegalArgumentException.class, () -> PosicaoCronologicaQuantitativa.custodiaTransitoria(LocalDate.of(2026, 1, 1), 0, null));
        assertTrue(PosicaoCronologicaQuantitativa.financeira(LocalDate.of(2026, 1, 1), 1).financeira());
    }
    @Test void mesmaPosicaoPelasDuasFabricasTemIgualdadeHashEComparacao() {
        var data = LocalDate.of(2026, 1, 1);
        var persistivel = PosicaoCronologicaQuantitativa.custodia(data, 1, PosicaoRacionalCustodia.deTexto("2", "3"));
        var transitoria = PosicaoCronologicaQuantitativa.custodiaTransitoria(data, 1, f(20, 30));
        assertEquals(persistivel, transitoria); assertEquals(persistivel.hashCode(), transitoria.hashCode());
        assertEquals(0, persistivel.compareTo(transitoria));
    }
    @Test void posicaoDe513DigitosOrdenaEntreLimiteEPosicaoExistente() {
        var data = LocalDate.of(2026, 1, 1);
        var superior = PosicaoRacionalCustodia.criar(UM, D);
        var medianta = PosicaoRacionalTransitoria.INICIO.entre(PosicaoRacionalTransitoria.dePersistivel(superior));
        var existente = PosicaoCronologicaQuantitativa.custodia(data, 0, superior);
        var candidata = PosicaoCronologicaQuantitativa.custodiaTransitoria(data, 0, medianta);
        var financeira = PosicaoCronologicaQuantitativa.financeira(data, 1);
        assertEquals(List.of(candidata, existente, financeira), List.of(financeira, existente, candidata).stream().sorted().toList());
    }
    @Test void tipoTransitorioEhImutavelESemMapeamentoDeInfraestrutura() {
        assertTrue(java.lang.reflect.Modifier.isFinal(PosicaoRacionalTransitoria.class.getModifiers()));
        assertEquals(0, PosicaoRacionalTransitoria.class.getAnnotations().length);
        for (var campo : PosicaoRacionalTransitoria.class.getDeclaredFields()) {
            assertTrue(java.lang.reflect.Modifier.isFinal(campo.getModifiers()));
        }
        var menor = f(1, 3); var maior = f(2, 3); var entre = menor.entre(maior);
        assertEquals("1/3", menor.toString()); assertEquals("2/3", maior.toString());
        assertEquals("1/2", entre.toString());
    }
}
