package com.projeto.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PosicaoRacionalCustodiaTest {
    private static PosicaoRacionalCustodia fracao(long n, long d) {
        return PosicaoRacionalCustodia.criar(BigInteger.valueOf(n), BigInteger.valueOf(d));
    }

    @Test
    void reduzEquivalentesComIgualdadeHashETextoCanonicos() {
        var metade = fracao(1, 2);
        var equivalente = fracao(50, 100);
        assertEquals(metade, equivalente);
        assertEquals(metade.hashCode(), equivalente.hashCode());
        assertEquals(0, metade.compareTo(equivalente));
        assertEquals(1, new HashSet<>(List.of(metade, equivalente)).size());
        assertEquals(BigInteger.ONE, equivalente.getNumerador());
        assertEquals(BigInteger.TWO, equivalente.getDenominador());
        assertEquals("1/2", equivalente.toString());
        assertNotEquals(metade, fracao(2, 3));
        assertNotEquals(metade, null);
        assertNotEquals(metade, "1/2");
    }

    @ParameterizedTest
    @CsvSource({"-1,2", "1,0", "1,-2", "-1,-2", "0,1", "1,1", "2,1", "2,2"})
    void rejeitaComponentesEIntervaloInvalidos(long n, long d) {
        assertThrows(IllegalArgumentException.class, () -> fracao(n, d));
    }

    @Test
    void rejeitaComponentesAusentes() {
        assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalCustodia.criar(null, BigInteger.TWO));
        assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalCustodia.criar(BigInteger.ONE, null));
    }

    @Test
    void fronteirasSaoCanonicasOrdenaveisENaoPersistiveis() {
        var inicio = PosicaoRacionalCustodia.INICIO;
        var fim = PosicaoRacionalCustodia.FIM;
        assertEquals("0/1", inicio.toString());
        assertEquals("1/1", fim.toString());
        assertFalse(inicio.isPersistivel());
        assertFalse(fim.isPersistivel());
        assertThrows(IllegalArgumentException.class, inicio::exigirPersistivel);
        assertThrows(IllegalArgumentException.class, fim::exigirPersistivel);
        assertEquals(fracao(1, 2), inicio.entre(fim));
        assertTrue(inicio.compareTo(fracao(1, 2)) < 0);
        assertTrue(fim.compareTo(fracao(1, 2)) > 0);
        assertDoesNotThrow(fracao(1, 2)::exigirPersistivel);
    }

    @Test
    void comparaExatamenteSemOrdemTextualMesmoComProdutosAcimaDe512Digitos() {
        assertTrue(fracao(2, 3).compareTo(fracao(10, 11)) < 0);
        BigInteger limite = BigInteger.TEN.pow(512);
        var a = PosicaoRacionalCustodia.criar(limite.subtract(BigInteger.TWO), limite.subtract(BigInteger.ONE));
        var b = PosicaoRacionalCustodia.criar(limite.subtract(BigInteger.valueOf(3)), limite.subtract(BigInteger.TWO));
        assertTrue(a.compareTo(b) > 0);
        assertTrue(b.compareTo(a) < 0);
        assertThrows(NullPointerException.class, () -> a.compareTo(null));
    }

    @Test
    void mediantaInsereEntreVizinhasEReduzSemModificarLimites() {
        var esquerda = fracao(1, 2);
        var direita = fracao(2, 3);
        var meio = esquerda.entre(direita);
        assertEquals(fracao(3, 5), meio);
        assertTrue(esquerda.compareTo(meio) < 0);
        assertTrue(meio.compareTo(direita) < 0);
        assertEquals("1/2", esquerda.toString());
        assertEquals("2/3", direita.toString());
        assertEquals(fracao(1, 2), fracao(1, 3).entre(fracao(2, 3)));
        assertEquals(fracao(1, 3), PosicaoRacionalCustodia.INICIO.entre(esquerda));
        assertThrows(IllegalArgumentException.class, () -> direita.entre(esquerda));
        assertThrows(IllegalArgumentException.class, () -> esquerda.entre(fracao(2, 4)));
        assertThrows(IllegalArgumentException.class, () -> esquerda.entre(null));
    }

    @Test
    void insercoesRepetidasPreservamTodasAsPosicoesGeradas() {
        var esquerda = fracao(1, 2);
        var direita = fracao(2, 3);
        var geradas = new java.util.ArrayList<PosicaoRacionalCustodia>();
        var textos = new java.util.ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            var nova = esquerda.entre(direita);
            assertTrue(esquerda.compareTo(nova) < 0);
            assertTrue(nova.compareTo(direita) < 0);
            geradas.add(nova);
            textos.add(nova.toString());
            direita = nova;
        }
        for (int i = 0; i < geradas.size(); i++) assertEquals(textos.get(i), geradas.get(i).toString());
        assertEquals("1/2", esquerda.toString());
    }

    @Test
    void limiteEhAvaliadoAposReducaoInclusiveParaOperandosGrandes() {
        BigInteger dez512 = BigInteger.TEN.pow(512);
        var maxima = PosicaoRacionalCustodia.criar(dez512.subtract(BigInteger.TWO), dez512.subtract(BigInteger.ONE));
        assertEquals(512, maxima.getNumeradorTexto().length());
        assertEquals(512, maxima.getDenominadorTexto().length());
        assertThrowsExactly(LimitePosicaoCustodiaException.class,
                () -> PosicaoRacionalCustodia.criar(BigInteger.ONE, dez512));
        assertThrowsExactly(LimitePosicaoCustodiaException.class,
                () -> PosicaoRacionalCustodia.criar(dez512, dez512.add(BigInteger.ONE)));
        assertEquals(fracao(1, 2), PosicaoRacionalCustodia.criar(dez512, dez512.multiply(BigInteger.TWO)));
        BigInteger fator = BigInteger.TEN.pow(4096);
        assertEquals(maxima, PosicaoRacionalCustodia.criar(maxima.getNumerador().multiply(fator),
                maxima.getDenominador().multiply(fator)));
    }

    @Test
    void falhaDeMediantaNoLimitePreservaPosicoesSemEscolherOutra() {
        BigInteger dez512 = BigInteger.TEN.pow(512);
        var esquerda = PosicaoRacionalCustodia.criar(dez512.subtract(BigInteger.TWO), dez512.subtract(BigInteger.ONE));
        String antes = esquerda.toString();
        assertThrowsExactly(LimitePosicaoCustodiaException.class, () -> esquerda.entre(PosicaoRacionalCustodia.FIM));
        assertEquals(antes, esquerda.toString());
        assertEquals("1/1", PosicaoRacionalCustodia.FIM.toString());
    }

    @Test
    void crescimentoAdversarialTerminaComErroExplicitoSemMemoriaIlimitada() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            var esquerda = PosicaoRacionalCustodia.INICIO;
            var direita = PosicaoRacionalCustodia.FIM;
            int aceitas = 0;
            while (aceitas < 3000) {
                String antesEsquerda = esquerda.toString();
                String antesDireita = direita.toString();
                try {
                    var nova = esquerda.entre(direita);
                    assertTrue(nova.isPersistivel());
                    if ((aceitas & 1) == 0) esquerda = nova; else direita = nova;
                    aceitas++;
                } catch (LimitePosicaoCustodiaException esperada) {
                    assertEquals(2449, aceitas);
                    assertEquals(antesEsquerda, esquerda.toString());
                    assertEquals(antesDireita, direita.toString());
                    return;
                }
            }
            fail("Sequência adversarial deveria exceder o limite de 512 dígitos");
        });
    }

    @Test
    void reconstrucaoTextualEhExataAsciiEReduzEquivalentes() {
        BigInteger dez512 = BigInteger.TEN.pow(512);
        var original = PosicaoRacionalCustodia.criar(dez512.subtract(BigInteger.TWO), dez512.subtract(BigInteger.ONE));
        assertEquals(original, PosicaoRacionalCustodia.deTexto(original.getNumeradorTexto(), original.getDenominadorTexto()));
        assertTrue(original.getNumeradorTexto().matches("[1-9][0-9]*"));
        assertTrue(original.getDenominadorTexto().matches("[1-9][0-9]*"));
        assertEquals(fracao(1, 2), PosicaoRacionalCustodia.deTexto("2", "4"));
        assertEquals(fracao(1, 2), PosicaoRacionalCustodia.deTexto(dez512.toString(), dez512.multiply(BigInteger.TWO).toString()));
        assertThrowsExactly(LimitePosicaoCustodiaException.class,
                () -> PosicaoRacionalCustodia.deTexto("1", dez512.toString()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "01", "+1", "-1", " 1", "1 ", "1.0", "1E2", "١", "１", "1\n"})
    void rejeitaTextoNaoCanonicoAscii(String componente) {
        assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalCustodia.deTexto(componente, "2"));
        assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalCustodia.deTexto("1", componente));
    }

    @Test
    void textoNaoPermitePersistirFronteirasEDenominadorZero() {
        assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalCustodia.deTexto("0", "1"));
        assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalCustodia.deTexto("1", "1"));
        assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalCustodia.deTexto("1", "0"));
    }

    @Test
    void entradasExtremasUsamOperandosLimitadosNoTesteESaoRejeitadas() {
        BigInteger grande = BigInteger.ONE.shiftLeft(100000);
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertThrowsExactly(LimitePosicaoCustodiaException.class,
                    () -> PosicaoRacionalCustodia.criar(grande, grande.add(BigInteger.ONE)));
            assertThrows(IllegalArgumentException.class, () -> PosicaoRacionalCustodia.criar(grande, BigInteger.ONE));
        });
    }

    @ParameterizedTest
    @CsvSource({"1,2", "3,5", "2,3", "10,11"})
    void reconstrucaoPersistidaPreservaComponentesCanonicos(String n, String d) {
        var posicao = PosicaoRacionalCustodia.reconstruirPersistida(n, d);
        assertEquals(n, posicao.getNumeradorTexto());
        assertEquals(d, posicao.getDenominadorTexto());
        assertEquals(PosicaoRacionalCustodia.deTexto(n, d), posicao);
        assertTrue(posicao.isPersistivel());
    }

    @ParameterizedTest
    @CsvSource({"2,4", "50,100", "0,1", "1,1", "2,2", "2,1", "1,0", "1,-2", "-1,2"})
    void rejeitaPosicaoPersistidaInvalidaSemConfundirComCapacidade(String n, String d) {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> PosicaoRacionalCustodia.reconstruirPersistida(n, d));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "01", "+1", "-1", " 1", "1 ", "1.0", "1E2", "١", "１", "1\n", "\t1"})
    void rejeitaTextoPersistidoNaoCanonicoEmAmbosComponentes(String componente) {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> PosicaoRacionalCustodia.reconstruirPersistida(componente, "2"));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> PosicaoRacionalCustodia.reconstruirPersistida("1", componente));
    }

    @Test
    void reconstrucaoRejeitaLarguraExcedenteAntesDeParsingMesmoSeRedutivel() {
        BigInteger dez512 = BigInteger.TEN.pow(512);
        String n = dez512.toString();
        String d = dez512.multiply(BigInteger.TWO).toString();
        assertThrowsExactly(IllegalArgumentException.class,
                () -> PosicaoRacionalCustodia.reconstruirPersistida(n, "2"));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> PosicaoRacionalCustodia.reconstruirPersistida("1", n));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> PosicaoRacionalCustodia.reconstruirPersistida(n, d));
        assertEquals(fracao(1, 2), PosicaoRacionalCustodia.deTexto(n, d));
        assertEquals(fracao(1, 2), PosicaoRacionalCustodia.criar(dez512, dez512.multiply(BigInteger.TWO)));
    }

    @Test
    void roundtripPersistidoNoLimitePreservaValorIgualdadeEHash() {
        BigInteger dez512 = BigInteger.TEN.pow(512);
        var original = PosicaoRacionalCustodia.criar(dez512.subtract(BigInteger.TWO), dez512.subtract(BigInteger.ONE));
        var reconstruida = PosicaoRacionalCustodia.reconstruirPersistida(
                original.getNumeradorTexto(), original.getDenominadorTexto());
        assertEquals(original, reconstruida);
        assertEquals(original.hashCode(), reconstruida.hashCode());
        assertEquals(0, original.compareTo(reconstruida));
        assertEquals(original.toString(), reconstruida.toString());
        assertEquals(512, reconstruida.getNumeradorTexto().length());
        assertEquals(512, reconstruida.getDenominadorTexto().length());
        assertEquals(fracao(1, 2), PosicaoRacionalCustodia.deTexto("2", "4"));
    }
}
