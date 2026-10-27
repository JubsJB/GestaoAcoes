package com.projeto.custodia.postgresql;

import java.util.Map;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/** Não usa Boot, ConfigData, scan, serviços ou providers. Nenhum contexto é iniciado aqui.
 * O initializer explícito registra o único datasource após conferir o manifesto.
 * Esta etapa não fornece implementação real de Inspecao ou Conector. */
public final class CustodiaPostgresConfiguracao {
    private CustodiaPostgresConfiguracao() { }

    @TestConfiguration(proxyBeanMethods = false)
    public static class SomenteDatasource {
        /** Preparação manual: não é bean. Só inicializar após comprovar aplicação completa de 008.
         * O futuro teste chama afterPropertiesSet/destroy e encerra a fábrica antes da limpeza. */
        public LocalContainerEntityManagerFactoryBean prepararHibernate(CustodiaPostgresDataSource ds) {
            var vendor = new HibernateJpaVendorAdapter(); vendor.setDatabase(Database.POSTGRESQL); vendor.setGenerateDdl(false);
            var factory = new LocalContainerEntityManagerFactoryBean(); factory.setDataSource(ds);
            factory.setPackagesToScan("com.projeto.entities"); factory.setJpaVendorAdapter(vendor);
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
            return factory;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import(SomenteDatasource.class)
    public static class Completa {
        @Bean public SpringLiquibase liquibase(CustodiaPostgresDataSource ds, ResourceLoader recursos) {
            var liquibase = new SpringLiquibase(); liquibase.setDataSource(ds); liquibase.setResourceLoader(recursos);
            liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml"); return liquibase;
        }
        @Bean @DependsOn("liquibase")
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(CustodiaPostgresDataSource ds) {
            return new SomenteDatasource().prepararHibernate(ds);
        }
    }
}
