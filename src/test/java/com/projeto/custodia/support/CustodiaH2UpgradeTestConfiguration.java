package com.projeto.custodia.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Preparação em etapas: importa somente o datasource. Não importar a configuração completa aqui:
 * seus beans executariam o master e iniciariam Hibernate durante o refresh.
 * Liquibase programático futuro deverá receber exclusivamente a conexão deste datasource.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import(CustodiaH2DataSourceConfiguration.class)
public class CustodiaH2UpgradeTestConfiguration {
    private final CustodiaH2TestConfiguration.CustodiaDataSource dataSource;

    public CustodiaH2UpgradeTestConfiguration(CustodiaH2TestConfiguration.CustodiaDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Apenas prepara a fábrica, sem criar EntityManagerFactory ou abrir conexão.
     * Não é @Bean: Spring não chama afterPropertiesSet automaticamente.
     * Na futura Fase 5.1, somente após confirmar a aplicação dos dois changeSets de 008,
     * o teste poderá chamar afterPropertiesSet e deverá chamar destroy em finally,
     * antes do descarte do contexto/datasource. Reutiliza os mesmos mapeamentos e validate.
     */
    public LocalContainerEntityManagerFactoryBean prepararValidacaoHibernate() {
        return new CustodiaH2TestConfiguration().entityManagerFactory(dataSource);
    }
}
