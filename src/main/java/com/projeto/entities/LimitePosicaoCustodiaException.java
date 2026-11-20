package com.projeto.entities;

/** Falha de capacidade matemática; sua tradução HTTP pertence à futura integração. */
public final class LimitePosicaoCustodiaException extends IllegalArgumentException {
    public LimitePosicaoCustodiaException() {
        super("Posição de custódia excede 512 dígitos por componente após redução");
    }
}
