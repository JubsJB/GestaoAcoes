package com.projeto.security;

import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/** Não usar record/toString ou binding que inclua o valor rejeitado nas mensagens. */
public final class AuthRuntimeConfig {
    private final SecretKey key;
    private final Duration duracao;
    private final boolean httpLocal;

    public AuthRuntimeConfig(String segredoBase64, Duration duracao, boolean httpLocal) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(segredoBase64); }
        catch (IllegalArgumentException | NullPointerException ex) { throw segredoInvalido(); }
        if (bytes.length < 32) throw segredoInvalido();
        if (duracao == null || duracao.isZero() || duracao.isNegative() || duracao.compareTo(Duration.ofHours(4)) > 0)
            throw new IllegalStateException("Duração absoluta deve ser positiva e no máximo quatro horas");
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
        this.duracao = duracao;
        this.httpLocal = httpLocal;
    }

    private static IllegalStateException segredoInvalido() {
        return new IllegalStateException("AUTH_JWT_SECRET deve conter chave Base64 aleatória de pelo menos 32 bytes");
    }
    public SecretKey key() { return key; }
    public Duration duracao() { return duracao; }
    public boolean httpLocal() { return httpLocal; }
}
