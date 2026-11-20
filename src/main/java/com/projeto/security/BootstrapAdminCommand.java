package com.projeto.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Executar com o profile bootstrap-admin; credenciais somente no ambiente runtime. */
@Component
@Profile("bootstrap-admin")
public class BootstrapAdminCommand implements ApplicationRunner {
    private final BootstrapAdminService bootstrap;
    private final Environment environment;
    private final ConfigurableApplicationContext context;
    public BootstrapAdminCommand(BootstrapAdminService bootstrap, Environment environment, ConfigurableApplicationContext context) {
        if (!"none".equalsIgnoreCase(environment.getProperty("spring.main.web-application-type")))
            throw new IllegalStateException("Bootstrap exige execução local sem servidor HTTP");
        this.bootstrap=bootstrap; this.environment=environment; this.context=context;
    }
    @Override public void run(ApplicationArguments args) {
        try {
            boolean criado = bootstrap.executar(environment.getProperty("AUTH_BOOTSTRAP_USERNAME"),
                    environment.getProperty("AUTH_BOOTSTRAP_PASSWORD"));
            org.slf4j.LoggerFactory.getLogger(getClass()).info(criado
                    ? "Bootstrap concluído; remover profile e segredos de bootstrap do ambiente."
                    : "Bootstrap recusado: banco já inicializado; nenhuma credencial foi alterada.");
        } finally { context.close(); }
    }
}
