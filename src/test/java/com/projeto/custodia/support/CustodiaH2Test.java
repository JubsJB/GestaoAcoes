package com.projeto.custodia.support;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt-in para fases futuras autorizadas: inicializar este contexto EXECUTA Liquibase.
 * Não combinar com SpringBootTest, outros profiles, configurações ou datasources.
 * Não há binding de argumentos de inicialização: não se usa SpringApplication.
 * Fontes externas adicionadas ao Environment são verificadas antes do refresh.
 * Fecha o contexto ao final da classe, descartando o H2 exclusivo.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(SpringExtension.class)
@ActiveProfiles(value = CustodiaH2IsolationInitializer.PROFILE, inheritProfiles = false)
@ContextConfiguration(classes = CustodiaH2TestConfiguration.class,
        initializers = CustodiaH2IsolationInitializer.class, inheritLocations = false,
        inheritInitializers = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public @interface CustodiaH2Test {
}
