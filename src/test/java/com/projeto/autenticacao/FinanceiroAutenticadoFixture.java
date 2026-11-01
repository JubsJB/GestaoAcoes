package com.projeto.autenticacao;

import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.UsuarioRepository;
import com.projeto.security.SessaoService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Adapta somente testes financeiros que a importam; não desliga filtros nem simula Authentication. */
@TestConfiguration(proxyBeanMethods = false)
public class FinanceiroAutenticadoFixture {
    @Bean SqlFinanceiro sqlFinanceiro() { return new SqlFinanceiro(); }
    @Bean org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer sqlFinanceiroCustomizer(SqlFinanceiro counter) {
        return properties -> properties.put("hibernate.session_factory.statement_inspector",counter);
    }
    /** Mantém o orçamento financeiro original de duas consultas, separado da autenticação persistida. */
    public static final class SqlFinanceiro implements org.hibernate.resource.jdbc.spi.StatementInspector {
        private static final java.util.regex.Pattern SEGURANCA = java.util.regex.Pattern.compile(
                "\\b(usuario|sessao_autenticacao|controle_seguranca|contador_abuso|auditoria_usuario|autorizacao_redefinicao_senha)\\b",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        private final ThreadLocal<Long> quantidade = ThreadLocal.withInitial(() -> 0L);
        @Override public String inspect(String sql) {
            if (!SEGURANCA.matcher(sql).find()) quantidade.set(quantidade.get()+1);
            return sql;
        }
        public void limpar() { quantidade.set(0L); }
        public long quantidade() { return quantidade.get(); }
    }
    @Bean MockMvcBuilderCustomizer financeiroAutenticado(UsuarioRepository usuarios, PasswordEncoder encoder,
            SessaoService sessoes, CookieCsrfTokenRepository csrf, Clock clock, PlatformTransactionManager manager) {
        String username = "http."+UUID.randomUUID(), senha = UUID.randomUUID().toString();
        String hash = encoder.encode(senha);
        var transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return builder -> builder.defaultRequest(get("/").with(request -> {
            // Executar após @BeforeEach: testes existentes podem controlar o Clock da aplicação.
            transaction.executeWithoutResult(status -> {
                if (usuarios.findByUsername(username).isEmpty()) {
                    var agora = OffsetDateTime.now(clock);
                    var usuario = new Usuario(username, Papel.ADMIN, agora);
                    usuario.definirCredencialInicial(hash, agora); usuarios.saveAndFlush(usuario);
                }
            });
            String token = sessoes.login(username, senha, null, request.getRemoteAddr()).token();
            String antiCsrf = csrf.generateToken(request).getToken();
            request.setSecure(true);
            request.setCookies(new Cookie("GESTAOACOES_AUTH", token), new Cookie("XSRF-TOKEN", antiCsrf));
            request.addHeader("X-XSRF-TOKEN", antiCsrf);
            return request;
        }));
    }
}
