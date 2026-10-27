package com.projeto.custodia.support;

import com.projeto.repositories.EstornoMovimentacaoCustodiaRepository;
import com.projeto.repositories.MovimentacaoCustodiaRepository;
import com.projeto.repositories.OperacaoRepository;
import liquibase.integration.spring.SpringLiquibase;
import org.h2.Driver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Properties;

/** Contexto Spring restrito: sem Boot, ConfigData, component scan, HTTP ou providers. */
@TestConfiguration(proxyBeanMethods = false)
@Import(CustodiaH2DataSourceConfiguration.class)
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.projeto.repositories",
        excludeFilters = @Filter(type = FilterType.CUSTOM,
                classes = CustodiaH2TestConfiguration.CustodiaOnlyRepositories.class))
public class CustodiaH2TestConfiguration {
    /** Compatibilidade com os testes de isolamento que invocam esta fábrica sem contexto Spring. */
    CustodiaDataSource dataSource(ConfigurableEnvironment environment) {
        return new CustodiaH2DataSourceConfiguration().dataSource(environment);
    }

    @Bean
    SpringLiquibase liquibase(CustodiaDataSource dataSource, ResourceLoader resourceLoader) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setResourceLoader(resourceLoader);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
        return liquibase;
    }

    @Bean
    @DependsOn("liquibase")
    LocalContainerEntityManagerFactoryBean entityManagerFactory(CustodiaDataSource dataSource) {
        HibernateJpaVendorAdapter vendor = new HibernateJpaVendorAdapter();
        vendor.setDatabase(Database.H2);
        vendor.setGenerateDdl(false);
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.projeto.entities");
        factory.setJpaVendorAdapter(vendor);
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
        return factory;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    /** Exclui todos os repositórios fora do escopo, sem carregar services. */
    public static final class CustodiaOnlyRepositories implements TypeFilter {
        private static final Set<String> ALLOWED = Set.of(MovimentacaoCustodiaRepository.class.getName(),
                EstornoMovimentacaoCustodiaRepository.class.getName(), OperacaoRepository.class.getName());

        @Override
        public boolean match(MetadataReader reader, MetadataReaderFactory factory) {
            return !ALLOWED.contains(reader.getClassMetadata().getClassName());
        }
    }

    /** Sem binding ou setters de URL; nenhuma conexão é aberta pelo construtor. */
    static final class CustodiaDataSource extends AbstractDataSource implements AutoCloseable {
        private final ConfigurableEnvironment environment;
        private final Driver driver = new Driver();
        private final String url = "jdbc:h2:mem:custodia_" + UUID.randomUUID();
        private final Properties credentials = new Properties();
        private Connection keeper;
        private boolean closed;

        CustodiaDataSource(ConfigurableEnvironment environment) {
            this.environment = environment;
            // org.h2.Driver explícito: não seleciona driver através de configuração externa.
            credentials.setProperty("user", "sa");
            credentials.setProperty("password", "");
        }

        @Override
        public synchronized Connection getConnection() throws SQLException {
            CustodiaH2IsolationInitializer.validar(environment);
            if (closed) throw new SQLException("Datasource de custódia encerrado");
            // Sem DB_CLOSE_DELAY=-1: o keeper mantém o banco até o fechamento do contexto.
            if (keeper == null) keeper = driver.connect(url, credentials);
            return driver.connect(url, credentials);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException("Credenciais externas não são aceitas no H2 de custódia");
        }

        @Override
        public synchronized void close() throws SQLException {
            closed = true;
            if (keeper != null) keeper.close();
        }
    }
}
