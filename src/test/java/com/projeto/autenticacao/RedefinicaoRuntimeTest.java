package com.projeto.autenticacao;

import com.projeto.security.AuthConfig;
import java.util.Base64;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class RedefinicaoRuntimeTest {
    @Test void webRuntimeRecusaChaveHmacAusente() {
        new WebApplicationContextRunner().withUserConfiguration(AuthConfig.class)
                .withPropertyValues("auth.jwt.secret="+chave()).run(context -> assertThat(context).hasFailed());
    }
    static String chave() {
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getEncoder().encodeToString(bytes);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"", "invalido", "YQ=="})
    void recusaChaveHmacInseguraSemExporValor(String segredo) {
        var jwt=new com.projeto.security.AuthRuntimeConfig(chave(),java.time.Duration.ofHours(4),false);
        var erro=org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                ()->new com.projeto.security.PasswordResetConfig(segredo,jwt.key()));
        org.junit.jupiter.api.Assertions.assertNull(erro.getCause());
    }
    @Test void recusaReusoDaChaveJwt() {
        var segredo=chave(); var jwt=new com.projeto.security.AuthRuntimeConfig(segredo,java.time.Duration.ofHours(4),false);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->new com.projeto.security.PasswordResetConfig(segredo,jwt.key()));
    }
    @Test void aceitaConfiguracaoWebComChavesIndependentes() {
        new WebApplicationContextRunner().withUserConfiguration(AuthConfig.class)
                .withPropertyValues("auth.jwt.secret="+chave(),"auth.reset.secret="+chave())
                .run(context->assertThat(context).hasNotFailed().hasSingleBean(com.projeto.security.PasswordResetConfig.class));
    }
}
