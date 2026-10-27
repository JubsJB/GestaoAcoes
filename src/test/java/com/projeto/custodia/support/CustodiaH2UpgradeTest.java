package com.projeto.custodia.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt-in exclusivo para futuros testes de upgrade autorizados; não executa migrations ou JPA no refresh.
 * Não combinar com CustodiaH2Test, SpringBootTest, outros perfis/configurações ou datasources.
 * Conexões abertas programaticamente deverão ser fechadas antes do término da classe.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(SpringExtension.class)
@ActiveProfiles(value = CustodiaH2IsolationInitializer.PROFILE, inheritProfiles = false)
@ContextConfiguration(classes = CustodiaH2UpgradeTestConfiguration.class,
        initializers = CustodiaH2IsolationInitializer.class, inheritLocations = false,
        inheritInitializers = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public @interface CustodiaH2UpgradeTest {
}
