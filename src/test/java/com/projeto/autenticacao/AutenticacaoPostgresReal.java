package com.projeto.autenticacao;

import java.lang.annotation.*;
import org.junit.jupiter.api.extension.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.annotation.DirtiesContext;

@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME) @Inherited
@ExtendWith(AutenticacaoPostgresReal.Ciclo.class)
@ActiveProfiles(value="custodia-pg17",inheritProfiles=false)
@ContextConfiguration(initializers=AutenticacaoPostgresExecucao.Initializer.class)
@SpringBootTest(classes=com.projeto.GestaoacoesApplication.class,properties={"spring.datasource.hikari.connection-init-sql=","logging.level.root=WARN","logging.level.org.hibernate.orm.jdbc.error=OFF","logging.level.org.hibernate.SQL=OFF","logging.level.org.hibernate.orm.jdbc.bind=OFF"})
@AutoConfigureMockMvc(print=org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@interface AutenticacaoPostgresReal {
    class Ciclo implements ExecutionCondition,AfterTestExecutionCallback {
        @Override public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            boolean selecionado=System.getProperty("auth.pg.real")!=null || !System.getProperty("test","").isBlank();
            if(!selecionado)return ConditionEvaluationResult.disabled("PostgreSQL real exige seleção explícita");
            if(!"EXECUTAR_009".equals(System.getProperty("auth.pg.real")))throw new ExtensionConfigurationException("Seleção PostgreSQL exige -Dauth.pg.real=EXECUTAR_009");
            context.getRoot().getStore(ExtensionContext.Namespace.create(Ciclo.class)).getOrComputeIfAbsent("execucao",k -> (ExtensionContext.Store.CloseableResource) AutenticacaoPostgresExecucao::fecharSeAberta);
            return ConditionEvaluationResult.enabled("Homologação PostgreSQL explicitamente autorizada");
        }
        @Override public void afterTestExecution(ExtensionContext contexto)throws Exception {
            AutenticacaoPostgresExecucao.registrar("testes",contexto.getRequiredTestClass().getSimpleName()+"."+contexto.getRequiredTestMethod().getName(),contexto.getExecutionException().isEmpty()?"PASS":"FAIL");
        }
    }
}
