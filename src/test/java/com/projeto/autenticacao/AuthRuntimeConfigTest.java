package com.projeto.autenticacao;

import com.projeto.security.AuthRuntimeConfig;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Base64;
import java.security.SecureRandom;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuthRuntimeConfigTest {
    @Test
    void recusaAusenciaDeSegredoSemPublicarValor() {
        var erro = assertThrows(IllegalStateException.class,
                () -> new AuthRuntimeConfig(null, Duration.ofHours(4), false));
        assertEquals("AUTH_JWT_SECRET deve conter chave Base64 aleatória de pelo menos 32 bytes", erro.getMessage());
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "segredo-invalido", "c2VncmVkby1jdXJ0bw=="})
    void rejeitaChaveFracaSemIncluirValor(String segredo) {
        var erro = assertThrows(IllegalStateException.class,
                () -> new AuthRuntimeConfig(segredo, Duration.ofHours(4), false));
        assertEquals("AUTH_JWT_SECRET deve conter chave Base64 aleatória de pelo menos 32 bytes", erro.getMessage());
        assertNull(erro.getCause());
    }
    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 14401})
    void rejeitaDuracaoForaDoContrato(long segundos) {
        assertThrows(IllegalStateException.class, () -> new AuthRuntimeConfig(chave(), Duration.ofSeconds(segundos), false));
    }
    @Test void aceitaChaveRuntimeEDuracaoPositiva() {
        var config = new AuthRuntimeConfig(chave(), Duration.ofHours(4), false);
        assertEquals(32, config.key().getEncoded().length);
        assertEquals(Duration.ofHours(4), config.duracao());
        assertFalse(config.httpLocal());
        assertThrows(IllegalStateException.class, () -> new AuthRuntimeConfig(chave(), null, false));
    }
    static String chave() {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    @Test void contextoFalhaFechadoSemSegredoRuntime() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(com.projeto.security.AuthConfig.class)
                .withPropertyValues("auth.jwt.secret=").run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("AUTH_JWT_SECRET"));
                });
    }
    @Test void segredoRejeitadoNaoApareceNaCadeiaDeErrosDoContexto() {
        String segredo = "segredo-runtime-invalido-"+java.util.UUID.randomUUID();
        new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(com.projeto.security.AuthConfig.class)
                .withPropertyValues("auth.jwt.secret="+segredo).run(context -> {
                    assertNotNull(context.getStartupFailure());
                    for (Throwable ex=context.getStartupFailure(); ex != null; ex=ex.getCause())
                        assertFalse(String.valueOf(ex.getMessage()).contains(segredo));
                });
    }
    @Test void configuracaoLocalExigeSomenteProfilesLocaisExplicitos() {
        for (String profile : new String[]{"", "prod", "prod,dev"}) {
            new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(com.projeto.security.AuthConfig.class)
                    .withPropertyValues("auth.jwt.secret="+chave(),"auth.allow-local-http=true","spring.profiles.active="+profile)
                    .run(context -> assertNotNull(context.getStartupFailure()));
        }
        new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(com.projeto.security.AuthConfig.class)
                .withPropertyValues("auth.jwt.secret="+chave(),"auth.allow-local-http=true","spring.profiles.active=dev")
                .run(context -> { assertNull(context.getStartupFailure()); assertTrue(context.getBean(AuthRuntimeConfig.class).httpLocal()); });
    }
    @Test void duracaoRuntimeMalFormadaNaoPublicaValorRejeitado() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(com.projeto.security.AuthConfig.class)
                .withPropertyValues("auth.jwt.secret="+chave(),"auth.absolute-duration=valor-invalido")
                .run(context -> { assertNotNull(context.getStartupFailure()); assertFalse(context.getStartupFailure().getMessage().contains("valor-invalido")); });
    }
}
