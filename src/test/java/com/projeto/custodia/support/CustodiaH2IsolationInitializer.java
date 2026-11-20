package com.projeto.custodia.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.Locale;
import java.util.Set;

/**
 * Falha antes do refresh e da criação de qualquer bean de persistência.
 * Examina todas as fontes, inclusive valores mascarados por uma fonte de maior prioridade.
 * Normaliza nomes de variáveis de ambiente, propriedades JVM e fontes de argumentos.
 * Este contexto não usa SpringApplication nem carrega application*.properties.
 */
public final class CustodiaH2IsolationInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    static final String PROFILE = "custodia-h2";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        validar(context.getEnvironment());
        if (context.getBeanFactory() instanceof DefaultListableBeanFactory factory) {
            factory.setAllowBeanDefinitionOverriding(false);
        } else {
            throw new IllegalStateException("BeanFactory incompatível com isolamento de custódia");
        }
    }

    static void validar(ConfigurableEnvironment environment) {
        // Leitura de metadado da biblioteca, sem driver.connect ou inicialização de banco.
        // Reflexão evita aceitar uma versão diferente em runtime por constante Java inlined.
        try {
            if (!"2.5.250".equals(org.h2.engine.Constants.class.getField("VERSION").get(null))) {
                throw new IllegalStateException("Testes de custódia exigem H2 2.5.250");
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Não foi possível verificar a versão H2", exception);
        }
        for (String profile : environment.getActiveProfiles()) {
            if (!PROFILE.equals(profile)) {
                throw new IllegalStateException("Perfil incompatível com isolamento de custódia: " + profile);
            }
        }
        for (String profile : environment.getDefaultProfiles()) {
            if (!Set.of("default", PROFILE).contains(profile)) {
                throw new IllegalStateException("Perfil padrão incompatível com isolamento de custódia");
            }
        }
        for (var source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    String key = name.replaceAll("[._-]", "").toLowerCase(Locale.ROOT);
                    if (key.startsWith("springdatasource") || key.startsWith("springliquibase")
                            || key.startsWith("springjpa") || key.startsWith("springsqlinit")
                            || key.startsWith("hibernateconnection") || key.startsWith("jakartapersistencejdbc")
                            || key.equals("springapplicationjson") || key.startsWith("springprofiles")) {
                        // Nunca incluir o valor: URLs e credenciais externas podem conter segredos.
                        throw new IllegalStateException("Configuração externa proibida no contexto H2 de custódia: " + name);
                    }
                }
            } else {
                throw new IllegalStateException("Fonte de propriedades não enumerável no contexto H2 de custódia: "
                        + source.getName());
            }
        }
    }
}
