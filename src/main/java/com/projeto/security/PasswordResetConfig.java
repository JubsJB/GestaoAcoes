package com.projeto.security;

import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/** Segredo de runtime; não expor em binding, mensagens, record ou toString. */
public final class PasswordResetConfig {
    private final SecretKey key;
    public PasswordResetConfig(String segredo, SecretKey jwt) {
        byte[] bytes;
        try { bytes=Base64.getDecoder().decode(segredo); }
        catch (IllegalArgumentException | NullPointerException ex) { throw invalido(); }
        if(bytes.length<32 || MessageDigest.isEqual(bytes,jwt.getEncoded())) throw invalido();
        key=new SecretKeySpec(bytes,"HmacSHA256");
    }
    private static IllegalStateException invalido() {
        return new IllegalStateException("AUTH_RESET_SECRET exige chave Base64 própria de pelo menos 32 bytes, distinta da JWT");
    }
    public SecretKey key() { return key; }
}
