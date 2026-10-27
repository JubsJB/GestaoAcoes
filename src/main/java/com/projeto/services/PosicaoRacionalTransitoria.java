package com.projeto.services;

import com.projeto.entities.PosicaoRacionalCustodia;
import java.math.BigInteger;

/** Fração exata de simulação, sem limite de armazenamento e sem mapeamento de persistência.
 * A saída para o domínio persistível exige conversão explícita, sujeita ao limite original. */
public final class PosicaoRacionalTransitoria implements Comparable<PosicaoRacionalTransitoria> {
    public static final PosicaoRacionalTransitoria INICIO =
            new PosicaoRacionalTransitoria(BigInteger.ZERO, BigInteger.ONE);
    public static final PosicaoRacionalTransitoria FIM =
            new PosicaoRacionalTransitoria(BigInteger.ONE, BigInteger.ONE);

    private final BigInteger numerador;
    private final BigInteger denominador;

    private PosicaoRacionalTransitoria(BigInteger numerador, BigInteger denominador) {
        if (numerador == null || denominador == null || denominador.signum() <= 0
                || numerador.signum() < 0 || numerador.compareTo(denominador) > 0) {
            throw new IllegalArgumentException("Fração exige denominador positivo e intervalo de 0 a 1");
        }
        BigInteger divisor = numerador.gcd(denominador);
        this.numerador = numerador.divide(divisor);
        this.denominador = denominador.divide(divisor);
    }

    /** Cria posição efetiva no intervalo aberto, reduzida por gcd, sem arredondamento. */
    public static PosicaoRacionalTransitoria criar(BigInteger numerador, BigInteger denominador) {
        var posicao = new PosicaoRacionalTransitoria(numerador, denominador);
        posicao.exigirEfetiva();
        return posicao;
    }

    /** Também converte sentinelas; elas continuam impróprias como posições efetivas. */
    public static PosicaoRacionalTransitoria dePersistivel(PosicaoRacionalCustodia posicao) {
        if (posicao == null) throw new IllegalArgumentException("Posição de origem obrigatória");
        return new PosicaoRacionalTransitoria(posicao.getNumerador(), posicao.getDenominador());
    }

    /** Não contorna a validação existente: fronteiras e componentes acima de 512 são rejeitados. */
    public PosicaoRacionalCustodia paraPersistivel() {
        return PosicaoRacionalCustodia.criar(numerador, denominador);
    }

    public PosicaoRacionalTransitoria entre(PosicaoRacionalTransitoria superior) {
        if (superior == null || compareTo(superior) >= 0) {
            throw new IllegalArgumentException("Limites devem ser distintos e crescentes");
        }
        return criar(numerador.add(superior.numerador), denominador.add(superior.denominador));
    }

    public void exigirEfetiva() {
        if (numerador.signum() == 0 || numerador.compareTo(denominador) >= 0) {
            throw new IllegalArgumentException("Sentinelas não são posições efetivas de custódia");
        }
    }

    @Override
    public int compareTo(PosicaoRacionalTransitoria outra) {
        return numerador.multiply(outra.denominador).compareTo(outra.numerador.multiply(denominador));
    }

    @Override
    public boolean equals(Object objeto) {
        return objeto instanceof PosicaoRacionalTransitoria outra
                && numerador.equals(outra.numerador) && denominador.equals(outra.denominador);
    }

    @Override
    public int hashCode() { return 31 * numerador.hashCode() + denominador.hashCode(); }

    public BigInteger getNumerador() { return numerador; }
    public BigInteger getDenominador() { return denominador; }

    @Override
    public String toString() { return numerador + "/" + denominador; }
}
