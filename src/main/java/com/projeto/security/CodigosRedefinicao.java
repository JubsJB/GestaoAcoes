package com.projeto.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

/** Código em memória somente durante emissão/verificação. Nunca serializar este componente. */
@Component @ConditionalOnWebApplication
public class CodigosRedefinicao {
    private static final String BASE32="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private final SecureRandom random=new SecureRandom();
    private final PasswordResetConfig config;
    public CodigosRedefinicao(PasswordResetConfig config) {this.config=config;}
    public String gerar() {
        byte[] bytes=new byte[16];random.nextBytes(bytes);
        var texto=new StringBuilder(26);int buffer=0,bits=0;
        for(byte b:bytes) {
            buffer=(buffer<<8)|(b&255);bits+=8;
            while(bits>=5) {bits-=5;texto.append(BASE32.charAt((buffer>>>bits)&31));}
        }
        if(bits>0) texto.append(BASE32.charAt((buffer<<(5-bits))&31));
        return texto.toString();
    }
    public String digest(long usuario,UUID autorizacao,String codigo) {
        try {
            var mac=Mac.getInstance("HmacSHA256");mac.init(config.key());
            mac.update(ByteBuffer.allocate(24).putLong(usuario).putLong(autorizacao.getMostSignificantBits()).putLong(autorizacao.getLeastSignificantBits()).array());
            return HexFormat.of().formatHex(mac.doFinal(normalizar(codigo).getBytes(StandardCharsets.US_ASCII)));
        } catch(GeneralSecurityException ex) {throw new IllegalStateException("Verificação de código indisponível");}
    }
    public boolean confere(long usuario,UUID autorizacao,String codigo,String digest) {
        var calculado=HexFormat.of().parseHex(digest(usuario,autorizacao,codigo));
        return MessageDigest.isEqual(calculado,HexFormat.of().parseHex(digest)) && normalizar(codigo).matches("[A-Z2-7]{26}");
    }
    private static String normalizar(String codigo) {
        return codigo==null?"":codigo.replace(" ","").replace("-","").toUpperCase(Locale.ROOT);
    }
}
