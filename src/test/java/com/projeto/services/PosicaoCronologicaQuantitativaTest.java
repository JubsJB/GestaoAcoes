package com.projeto.services;

import com.projeto.entities.PosicaoRacionalCustodia;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PosicaoCronologicaQuantitativaTest {
    private static final LocalDate DATA = LocalDate.of(2026, 1, 1);
    private static PosicaoRacionalCustodia fracao(long n, long d) {
        return PosicaoRacionalCustodia.criar(BigInteger.valueOf(n), BigInteger.valueOf(d));
    }
    @Test void ordenaDataAncoraDominioEFracao() {
        var anterior = PosicaoCronologicaQuantitativa.financeira(DATA.minusDays(1), 20);
        var zero = PosicaoCronologicaQuantitativa.custodia(DATA, 0, fracao(1, 2));
        var f1 = PosicaoCronologicaQuantitativa.financeira(DATA, 1);
        var c1 = PosicaoCronologicaQuantitativa.custodia(DATA, 1, fracao(2, 3));
        var c2 = PosicaoCronologicaQuantitativa.custodia(DATA, 1, fracao(10, 11));
        var f2 = PosicaoCronologicaQuantitativa.financeira(DATA, 2);
        var esperada = List.of(anterior, zero, f1, c1, c2, f2);
        assertEquals(esperada, List.of(f2, c2, anterior, f1, c1, zero).stream().sorted().toList());
    }
    @Test void produtosMaioresQue512DigitosSaoComparadosExatamente() {
        var limite = BigInteger.TEN.pow(512);
        var maior = PosicaoRacionalCustodia.criar(limite.subtract(BigInteger.TWO), limite.subtract(BigInteger.ONE));
        var menor = PosicaoRacionalCustodia.criar(limite.subtract(BigInteger.valueOf(3)), limite.subtract(BigInteger.TWO));
        assertTrue(PosicaoCronologicaQuantitativa.custodia(DATA, 1, menor)
                .compareTo(PosicaoCronologicaQuantitativa.custodia(DATA, 1, maior)) < 0);
    }
    @Test void rejeitaFronteirasEAncoraInvalida() {
        assertThrows(IllegalArgumentException.class, () -> PosicaoCronologicaQuantitativa.financeira(DATA, 0));
        assertThrows(IllegalArgumentException.class, () -> PosicaoCronologicaQuantitativa.custodia(DATA, -1, fracao(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> PosicaoCronologicaQuantitativa.custodia(DATA, 0, null));
        assertThrows(IllegalArgumentException.class, () -> PosicaoCronologicaQuantitativa.custodia(DATA, 0, PosicaoRacionalCustodia.INICIO));
        assertThrows(IllegalArgumentException.class, () -> PosicaoCronologicaQuantitativa.custodia(DATA, 0, PosicaoRacionalCustodia.FIM));
        assertThrows(IllegalArgumentException.class, () -> PosicaoCronologicaQuantitativa.financeira(null, 1));
    }
    @Test void posicoesEquivalentesPossuemIgualdadeEHash() {
        var a = PosicaoCronologicaQuantitativa.custodia(DATA, 1, fracao(1, 2));
        var b = PosicaoCronologicaQuantitativa.custodia(DATA, 1, fracao(2, 4));
        assertEquals(0, a.compareTo(b));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
    @Test void operacaoPosteriorNaoMoveCustodiaDeAncoraZero() {
        var c = PosicaoCronologicaQuantitativa.custodia(DATA, 0, fracao(1, 2));
        assertTrue(c.compareTo(PosicaoCronologicaQuantitativa.financeira(DATA, 1)) < 0);
        assertEquals(0, c.ancoraFinanceira());
        assertEquals(fracao(1, 2), c.fracao().paraPersistivel());
    }
}
