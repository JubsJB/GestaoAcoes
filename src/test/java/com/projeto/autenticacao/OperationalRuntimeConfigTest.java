package com.projeto.autenticacao;

import com.projeto.security.OperationalRuntimeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;

class OperationalRuntimeConfigTest {
    private ApplicationContextRunner valido() {
        return new ApplicationContextRunner().withUserConfiguration(OperationalRuntimeConfig.class)
                .withPropertyValues("spring.profiles.active=operacional", "app.trusted-proxy-ip=172.30.240.10",
                        "server.tomcat.remoteip.internal-proxies=172.30.240.10/32",
                        "server.forward-headers-strategy=native", "auth.allow-local-http=false",
                        "server.tomcat.remoteip.remote-ip-header=X-Forwarded-For",
                        "server.tomcat.remoteip.protocol-header=X-Forwarded-Proto",
                        "server.tomcat.remoteip.host-header=X-Forwarded-Host",
                        "server.tomcat.remoteip.port-header=X-Forwarded-Port",
                        "app.public-origin=https://gestaoacoes.localhost:8443",
                        "op.db.name=homologacao", "op.db.app-user=aplicacao",
                        "spring.datasource.url=jdbc:postgresql://postgres:5432/homologacao",
                        "spring.datasource.username=aplicacao", "spring.datasource.password=fixture",
                        "auth.jwt.secret="+AuthRuntimeConfigTest.chave(),
                        "auth.reset.secret="+AuthRuntimeConfigTest.chave());
    }
    @Test void naoPermiteRedirecionarDatasourceOuConfiarEmHeaderNaoSobrescrito() {
        valido().withPropertyValues("server.tomcat.remoteip.protocol-header=X-Transporte-Cliente")
                .run(context -> assertNotNull(context.getStartupFailure()));
        valido().withPropertyValues("spring.datasource.jndi-name=java:comp/env/dev")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
    @Test void validaAntesDoDatasourceSemPublicarValoresRejeitados() {
        valido().run(context -> assertNull(context.getStartupFailure()));
        for (String property : new String[]{"auth.jwt.secret=valor-privado",
                "auth.reset.secret=valor-privado", "spring.profiles.active=operacional,dev",
                "auth.allow-local-http=true", "server.forward-headers-strategy=framework",
                "server.tomcat.remoteip.trusted-proxies=10.0.0.0/8",
                "app.public-origin=http://gestaoacoes.localhost:8443",
                "spring.datasource.url=jdbc:postgresql://desenvolvimento:5432/real"}) {
            valido().withPropertyValues(property).run(context -> {
                assertNotNull(context.getStartupFailure(), property.split("=")[0]);
                for (Throwable e=context.getStartupFailure(); e!=null; e=e.getCause())
                    assertFalse(String.valueOf(e.getMessage()).contains("valor-privado"));
            });
        }
    }
    @Test void recusaProxyGenericoAntesDeInicializarInfraestrutura() {
        new ApplicationContextRunner().withUserConfiguration(OperationalRuntimeConfig.class)
                .withPropertyValues("spring.profiles.active=operacional",
                        "app.trusted-proxy-ip=172.30.240.10",
                        "server.tomcat.remoteip.internal-proxies=172.16.0.0/12")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
