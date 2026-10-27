package com.projeto.entities;

import java.math.BigInteger;

/** Fração imutável e exata; não contém âncora financeira, data, replay ou persistência. */
public final class PosicaoRacionalCustodia implements Comparable<PosicaoRacionalCustodia> {
    public static final int MAXIMO_DIGITOS = 512;
    private static final BigInteger LIMITE_EXCLUSIVO = BigInteger.TEN.pow(MAXIMO_DIGITOS);

    public static final PosicaoRacionalCustodia INICIO =
            new PosicaoRacionalCustodia(BigInteger.ZERO, BigInteger.ONE);
    public static final PosicaoRacionalCustodia FIM =
            new PosicaoRacionalCustodia(BigInteger.ONE, BigInteger.ONE);

    private final BigInteger numerador;
    private final BigInteger denominador;

    private PosicaoRacionalCustodia(BigInteger numerador, BigInteger denominador) {
        if (numerador == null || denominador == null || denominador.signum() <= 0) {
            throw new IllegalArgumentException("Numerador e denominador positivo são obrigatórios");
        }
        if (numerador.signum() < 0 || numerador.compareTo(denominador) > 0) {
            throw new IllegalArgumentException("Fração deve pertencer ao intervalo de 0 a 1");
        }
        BigInteger divisor = numerador.gcd(denominador);
        BigInteger n = numerador.divide(divisor);
        BigInteger d = denominador.divide(divisor);
        // Não converter entradas excedentes para texto nem limitar os operandos antes do gcd.
        if (n.compareTo(LIMITE_EXCLUSIVO) >= 0 || d.compareTo(LIMITE_EXCLUSIVO) >= 0) {
            throw new LimitePosicaoCustodiaException();
        }
        this.numerador = n;
        this.denominador = d;
    }

    /** Cria apenas posições de custódia no intervalo aberto; fronteiras são constantes separadas. */
    public static PosicaoRacionalCustodia criar(BigInteger numerador, BigInteger denominador) {
        PosicaoRacionalCustodia posicao = new PosicaoRacionalCustodia(numerador, denominador);
        posicao.exigirPersistivel();
        return posicao;
    }

    /** Aceita inteiros ASCII sem sinal/zeros iniciais e reduz frações equivalentes. */
    public static PosicaoRacionalCustodia deTexto(String numerador, String denominador) {
        return criar(inteiroAscii(numerador), inteiroAscii(denominador));
    }

    /** Reconstrói dados armazenados sem reduzir ou corrigir uma representação inválida. */
    public static PosicaoRacionalCustodia reconstruirPersistida(String numerador, String denominador) {
        // Conferir a largura antes do parsing limita o trabalho aos dois VARCHAR(512).
        if (numerador == null || denominador == null
                || numerador.length() > MAXIMO_DIGITOS || denominador.length() > MAXIMO_DIGITOS) {
            throw new IllegalArgumentException("Componentes persistidos devem possuir até 512 dígitos");
        }
        BigInteger n = inteiroAscii(numerador);
        BigInteger d = inteiroAscii(denominador);
        if (d.signum() <= 0 || n.signum() <= 0 || n.compareTo(d) >= 0) {
            throw new IllegalArgumentException("Posição persistida deve estar estritamente entre 0 e 1");
        }
        if (!n.gcd(d).equals(BigInteger.ONE)) {
            throw new IllegalArgumentException("Posição persistida deve estar reduzida à forma canônica");
        }
        // As pré-condições garantem que criar não altera os componentes nem excede a capacidade.
        return criar(n, d);
    }

    private static BigInteger inteiroAscii(String texto) {
        if (texto == null || texto.isEmpty() || (texto.length() > 1 && texto.charAt(0) == '0')) {
            throw new IllegalArgumentException("Componente deve ser decimal ASCII sem zeros iniciais");
        }
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Componente deve conter somente dígitos ASCII");
            }
        }
        return new BigInteger(texto);
    }

    /** Medianta reduzida entre limites estritamente ordenados, inclusive INICIO/FIM. */
    public PosicaoRacionalCustodia entre(PosicaoRacionalCustodia superior) {
        if (superior == null || compareTo(superior) >= 0) {
            throw new IllegalArgumentException("Limites devem ser distintos e crescentes");
        }
        return criar(numerador.add(superior.numerador), denominador.add(superior.denominador));
    }

    public boolean isPersistivel() {
        return numerador.signum() > 0 && numerador.compareTo(denominador) < 0;
    }

    public void exigirPersistivel() {
        if (!isPersistivel()) {
            throw new IllegalArgumentException("Fronteiras não são posições persistíveis de custódia");
        }
    }

    @Override
    public int compareTo(PosicaoRacionalCustodia outra) {
        if (outra == null) throw new NullPointerException("Posição de comparação é obrigatória");
        return numerador.multiply(outra.denominador).compareTo(outra.numerador.multiply(denominador));
    }

    @Override
    public boolean equals(Object objeto) {
        return objeto instanceof PosicaoRacionalCustodia outra
                && numerador.equals(outra.numerador) && denominador.equals(outra.denominador);
    }

    @Override
    public int hashCode() {
        return 31 * numerador.hashCode() + denominador.hashCode();
    }

    public BigInteger getNumerador() { return numerador; }
    public BigInteger getDenominador() { return denominador; }
    public String getNumeradorTexto() { return numerador.toString(); }
    public String getDenominadorTexto() { return denominador.toString(); }

    @Override
    public String toString() {
        return getNumeradorTexto() + "/" + getDenominadorTexto();
    }
}
