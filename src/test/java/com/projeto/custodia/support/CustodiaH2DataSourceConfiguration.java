package com.projeto.custodia.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

/** Componente comum: somente o datasource protegido, sem Liquibase, JPA, scan ou auto-configuração. */
@TestConfiguration(proxyBeanMethods = false)
public class CustodiaH2DataSourceConfiguration {
    @Bean(destroyMethod = "close")
    CustodiaH2TestConfiguration.CustodiaDataSource dataSource(ConfigurableEnvironment environment) {
        CustodiaH2IsolationInitializer.validar(environment);
        // A implementação original permanece única, incluindo driver, URL, credenciais e keeper.
        return new CustodiaH2TestConfiguration.CustodiaDataSource(environment);
    }
}
