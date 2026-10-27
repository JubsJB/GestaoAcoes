package com.projeto.custodia.postgresql;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;

/** Construção explícita pelo futuro bootstrap; sem construtor permissivo ou leitura de arquivo/env. */
public final class CustodiaPostgresInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private final CustodiaPostgresManifesto manifesto;
    private final Imagem pin;
    private final Inspecao inspecao;
    private final CustodiaPostgresDataSource.Conector conector;
    private final CustodiaPostgresDataSource compartilhado;
    public CustodiaPostgresInitializer(CustodiaPostgresManifesto m, Imagem pin, Inspecao i,
            CustodiaPostgresDataSource.Conector conector) {
        this(m, pin, i, conector, null);
    }
    /** Integração operacional: reutiliza o datasource já verificado e acompanhado pela limpeza. */
    public CustodiaPostgresInitializer(CustodiaPostgresManifesto m, Imagem pin, Inspecao i,
            CustodiaPostgresDataSource.Conector conector, CustodiaPostgresDataSource compartilhado) {
        exigir(m != null && pin != null && i != null && conector != null, "Bootstrap completo obrigatório");
        exigir(compartilhado == null || m.equals(compartilhado.manifesto()), "Datasource de outra execução");
        this.manifesto = m; this.pin = pin; this.inspecao = i; this.conector = conector;
        this.compartilhado = compartilhado;
    }
    @Override public void initialize(ConfigurableApplicationContext context) {
        CustodiaPostgresSeguranca.ambiente(context.getEnvironment());
        CustodiaPostgresSeguranca.identidade(manifesto, pin, inspecao.inspecionar(manifesto));
        exigir(context.getBeanFactory() instanceof DefaultListableBeanFactory, "BeanFactory não suportada");
        var factory = (DefaultListableBeanFactory) context.getBeanFactory();
        for (String nome : new String[]{"dataSource", "liquibase", "entityManagerFactory"}) {
            exigir(!factory.containsBean(nome) && !factory.containsBeanDefinition(nome), "Bean protegido já definido");
        }
        exigir(factory.getBeanNamesForType(javax.sql.DataSource.class, true, false).length == 0, "Datasource externo proibido");
        factory.setAllowBeanDefinitionOverriding(false);
        var ds = compartilhado == null
                ? new CustodiaPostgresDataSource(manifesto, pin, inspecao, context.getEnvironment(), conector) : compartilhado;
        var definicao = new org.springframework.beans.factory.support.RootBeanDefinition(CustodiaPostgresDataSource.class);
        definicao.setInstanceSupplier(() -> ds);
        definicao.setDestroyMethodName("close");
        factory.registerBeanDefinition("dataSource", definicao);
        // Rejeita datasources adicionais antes da instanciação de Liquibase/Hibernate.
        context.addBeanFactoryPostProcessor(bf -> exigir(java.util.Arrays.equals(new String[]{"dataSource"},
                bf.getBeanNamesForType(javax.sql.DataSource.class, true, false)), "Datasource adicional proibido"));
    }
}
