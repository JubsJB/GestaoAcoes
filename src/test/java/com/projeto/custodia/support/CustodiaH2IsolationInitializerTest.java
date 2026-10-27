package com.projeto.custodia.support;

import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Somente objetos de configuração: sem refresh, beans de persistência ou chamadas JDBC. */
class CustodiaH2IsolationInitializerTest {
    private final CustodiaH2IsolationInitializer initializer = new CustodiaH2IsolationInitializer();

    private MockEnvironment seguro() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("custodia-h2");
        return environment;
    }

    private ConfigurableApplicationContext contexto(MockEnvironment environment,
            DefaultListableBeanFactory factory) {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        when(context.getBeanFactory()).thenReturn(factory);
        return context;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://localhost:5432/gestaoacoesdb",
            "jdbc:h2:file:./custodia",
            "jdbc:h2:tcp://localhost/mem:custodia",
            "jdbc:h2:mem:custodia;INIT=RUNSCRIPT FROM 'externo.sql'",
            "jdbc:h2:mem:externo"
    })
    void rejeitaQualquerUrlExternaAntesDeCriarDatasource(String url) {
        MockEnvironment environment = seguro().withProperty("spring.datasource.url", url);
        ConfigurableApplicationContext context = contexto(environment, new DefaultListableBeanFactory());
        try (MockedConstruction<Driver> drivers = mockConstruction(Driver.class)) {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> initializer.initialize(context));
            assertFalse(error.getMessage().contains(url));
            assertThrows(IllegalStateException.class,
                    () -> new CustodiaH2TestConfiguration().dataSource(environment));
            assertTrue(drivers.constructed().isEmpty());
        }
        verify(context, never()).refresh();
        verify(context, never()).getBeanFactory();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "spring.liquibase.url", "spring.liquibase.user", "spring.liquibase.password",
            "spring.datasource.driver-class-name", "spring.datasource.hikari.connection-init-sql",
            "spring.jpa.properties.hibernate.connection.url", "spring.sql.init.mode",
            "hibernate.connection.url", "jakarta.persistence.jdbc.url", "spring.application.json",
            "spring.profiles.active", "spring.profiles.include", "spring.profiles.default"
    })
    void rejeitaPropriedadesDeConexaoLiquibasePerfisEInicializacao(String key) {
        MockEnvironment environment = seguro().withProperty(key, "externo");
        assertThrows(IllegalStateException.class, () -> CustodiaH2IsolationInitializer.validar(environment));
    }

    @Test
    void rejeitaVariavelDeAmbienteSemAlterarAmbienteDoProcesso() {
        MockEnvironment environment = seguro();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("ambiente-simulado",
                Map.of("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost/banco")));
        assertThrows(IllegalStateException.class, () -> initializer.initialize(
                contexto(environment, new DefaultListableBeanFactory())));
    }

    @Test
    void rejeitaPropriedadeJvmSemAlterarSystemProperties() {
        MockEnvironment environment = seguro();
        Properties properties = new Properties();
        properties.setProperty("spring.liquibase.url", "jdbc:h2:file:externo");
        environment.getPropertySources().addFirst(new PropertiesPropertySource("jvm-simulada", properties));
        assertThrows(IllegalStateException.class, () -> initializer.initialize(
                contexto(environment, new DefaultListableBeanFactory())));
    }

    @Test
    void rejeitaArgumentosExternosAdicionadosAoEnvironment() {
        MockEnvironment environment = seguro();
        environment.getPropertySources().addFirst(new SimpleCommandLinePropertySource(
                "--spring.datasource.url=jdbc:h2:mem:externo;INIT=SELECT 1"));
        assertThrows(IllegalStateException.class, () -> initializer.initialize(
                contexto(environment, new DefaultListableBeanFactory())));
    }

    @Test
    void examinaTambemFontesDeMenorPrioridade() {
        MockEnvironment environment = seguro();
        environment.getPropertySources().addFirst(new MapPropertySource("alta", Map.of("inofensiva", "ok")));
        environment.getPropertySources().addLast(new MapPropertySource("baixa",
                Map.of("SPRING_LIQUIBASE_URL", "jdbc:postgresql://localhost/banco")));
        assertThrows(IllegalStateException.class, () -> CustodiaH2IsolationInitializer.validar(environment));
    }

    @Test
    void rejeitaFonteNaoEnumeravel() {
        MockEnvironment environment = seguro();
        environment.getPropertySources().addLast(new PropertySource<Object>("opaca", new Object()) {
            @Override public Object getProperty(String name) { return null; }
        });
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> CustodiaH2IsolationInitializer.validar(environment));
        assertTrue(error.getMessage().contains("não enumerável"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "test", "prod"})
    void rejeitaPerfilAtivoIncompativel(String profile) {
        MockEnvironment environment = seguro();
        environment.addActiveProfile(profile);
        assertThrows(IllegalStateException.class, () -> CustodiaH2IsolationInitializer.validar(environment));
    }

    @Test
    void rejeitaPerfilDefaultDev() {
        MockEnvironment environment = seguro();
        environment.setDefaultProfiles("dev");
        assertThrows(IllegalStateException.class, () -> CustodiaH2IsolationInitializer.validar(environment));
    }

    @Test
    void aceitaAmbienteSeguroSemInicializarContextoOuBeans() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerBeanDefinition("sentinela", new RootBeanDefinition(Object.class));
        ConfigurableApplicationContext context = contexto(seguro().withProperty("inofensiva", "ok"), factory);
        assertDoesNotThrow(() -> initializer.initialize(context));
        assertEquals(0, factory.getSingletonCount());
        assertFalse(factory.isAllowBeanDefinitionOverriding());
        verify(context, never()).refresh();
    }

    @Test
    void impedeSobrescritaDeBeansSemInstanciaLos() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        initializer.initialize(contexto(seguro(), factory));
        factory.registerBeanDefinition("dataSource", new RootBeanDefinition(Object.class));
        assertThrows(BeanDefinitionOverrideException.class,
                () -> factory.registerBeanDefinition("dataSource", new RootBeanDefinition(String.class)));
        assertEquals(0, factory.getSingletonCount());
    }

    @Test
    void geraUrlsMemoriaDistintasECredenciaisFixasSemInvocarDriver() throws Exception {
        try (MockedConstruction<Driver> drivers = mockConstruction(Driver.class);
                var first = new CustodiaH2TestConfiguration().dataSource(seguro());
                var second = new CustodiaH2TestConfiguration().dataSource(seguro())) {
            String url = (String) ReflectionTestUtils.getField(first, "url");
            String secondUrl = (String) ReflectionTestUtils.getField(second, "url");
            String prefix = "jdbc:h2:mem:custodia_";
            assertNotNull(url);
            assertTrue(url.startsWith(prefix));
            assertEquals(prefix + UUID.fromString(url.substring(prefix.length())), url);
            assertNotEquals(url, secondUrl);
            assertTrue(secondUrl.startsWith(prefix));
            assertFalse(url.contains(";"));
            Properties credentials = (Properties) ReflectionTestUtils.getField(first, "credentials");
            assertEquals(Map.of("user", "sa", "password", ""), credentials);
            assertNull(ReflectionTestUtils.getField(first, "keeper"));
            assertNull(ReflectionTestUtils.getField(second, "keeper"));
            assertEquals(2, drivers.constructed().size());
            drivers.constructed().forEach(driver -> verifyNoInteractions(driver));
        }
    }
}
