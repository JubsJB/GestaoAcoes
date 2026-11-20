package com.projeto.autenticacao;

import com.projeto.security.*;
import com.projeto.entities.autenticacao.Papel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.projeto.entities.autenticacao.Usuario;
import com.projeto.repositories.autenticacao.UsuarioRepository;
import com.projeto.services.exceptions.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig({AutenticacaoPersistenceTest.Config.class, CredenciaisBootstrapTest.Config.class,
        CredenciaisConfig.class, CredenciaisService.class, BootstrapAdminService.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CredenciaisBootstrapTest {
    @Autowired BootstrapAdminService bootstrap;
    @Autowired CredenciaisService credenciais;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired DataSource dataSource;
    @BeforeEach void limpar() {
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("delete from auditoria_usuario");
        jdbc.update("delete from usuario");
    }
    @Test
    void bootstrapPermiteAutenticarPrimeiroAdministradorComSenhaRuntime() {
        String senha = UUID.randomUUID().toString();
        assertTrue(bootstrap.executar("  Primeiro.Admin  ", senha));
        var identidade = credenciais.autenticar("PRIMEIRO.ADMIN", senha);
        assertEquals("primeiro.admin", identidade.username());
        assertEquals(Papel.ADMIN, identidade.papel());
    }
    @Test void senhaIncorretaNaoAutenticaNemMudaSenha() {
        String senha = UUID.randomUUID().toString();
        bootstrap.executar("existente", senha);
        assertInvalida(() -> credenciais.autenticar("existente", UUID.randomUUID().toString()));
        assertEquals("existente", credenciais.autenticar("existente", senha).username());
    }
    @Test void identidadeInexistenteUsaMesmaResposta() {
        assertInvalida(() -> credenciais.autenticar("inexistente", UUID.randomUUID().toString()));
    }
    @Test void bloqueadaNaoAutenticaMesmoComSenhaCorreta() {
        String senha = UUID.randomUUID().toString();
        bootstrap.executar("bloqueada", senha);
        var usuario = usuarios.findByUsername("bloqueada").orElseThrow();
        usuario.setAtivo(false); usuarios.saveAndFlush(usuario);
        assertInvalida(() -> credenciais.autenticar("bloqueada", senha));
    }
    @Test void contaSemCredencialNaoAutentica() {
        usuarios.saveAndFlush(new Usuario("inicial", Papel.USER, OffsetDateTime.parse("2026-10-30T08:00:00Z")));
        assertInvalida(() -> credenciais.autenticar("inicial", UUID.randomUUID().toString()));
    }
    @Test void limiteDeSenhaConsideraBytesUtf8SemTruncarOuTrim() {
        assertEquals(400, assertThrows(ApiException.class,
                () -> credenciais.autenticar("valida", "á".repeat(37))).getStatus().value());
        assertEquals(400, assertThrows(ApiException.class,
                () -> credenciais.autenticar("valida", "curta")).getStatus().value());
        String senha = "  " + UUID.randomUUID() + "  ";
        bootstrap.executar("espacos", senha);
        assertEquals("espacos", credenciais.autenticar("espacos", senha).username());
        assertInvalida(() -> credenciais.autenticar("espacos", senha.trim()));
    }
    @Test void bcryptUsaSaltEHashPersistidoNuncaContemSenha() {
        String senha = UUID.randomUUID().toString();
        String primeiro = encoder.encode(senha), segundo = encoder.encode(senha);
        assertNotEquals(primeiro, segundo);
        assertTrue(encoder.matches(senha, primeiro));
        assertFalse(primeiro.contains(senha));
    }
    @Test void reexecucaoNaoSobrescrevePrimeiroAdministrador() {
        String senha = UUID.randomUUID().toString();
        assertTrue(bootstrap.executar("primeiro", senha));
        assertFalse(bootstrap.executar("segundo", UUID.randomUUID().toString()));
        assertEquals("primeiro", credenciais.autenticar("primeiro", senha).username());
        assertEquals(1, usuarios.count());
    }
    @Test void bootstrapConcorrenteCriaExatamenteUmAdministrador() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(1);
        try {
            Callable<Boolean> chamada = () -> { inicio.await(); return bootstrap.executar("admin."+UUID.randomUUID(), UUID.randomUUID().toString()); };
            var a = pool.submit(chamada); var b = pool.submit(chamada); inicio.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(1, usuarios.count());
        } finally { pool.shutdownNow(); }
    }
    @Test void bootstrapRecusaAusenciaDeCredenciaisEUsernameForaDaPolitica() {
        assertThrows(ApiException.class, () -> bootstrap.executar("valido", null));
        assertThrows(ApiException.class, () -> bootstrap.executar(null, UUID.randomUUID().toString()));
        assertThrows(ApiException.class, () -> bootstrap.executar("inválido", UUID.randomUUID().toString()));
        assertEquals(0, usuarios.count());
    }
    @Test void comandoOperacionalEncerraContextoSemServidorHttp() throws Exception {
        String senha = UUID.randomUUID().toString();
        var env = new org.springframework.mock.env.MockEnvironment()
                .withProperty("spring.main.web-application-type", "none")
                .withProperty("AUTH_BOOTSTRAP_USERNAME", "operacional")
                .withProperty("AUTH_BOOTSTRAP_PASSWORD", senha);
        var contexto = new org.springframework.context.support.GenericApplicationContext();
        contexto.refresh();
        new BootstrapAdminCommand(bootstrap, env, contexto).run(null);
        assertFalse(contexto.isActive());
        assertEquals("operacional", credenciais.autenticar("operacional", senha).username());
    }
    private void assertInvalida(org.junit.jupiter.api.function.Executable chamada) {
        var erro = assertThrows(ApiException.class, chamada);
        assertEquals(401, erro.getStatus().value());
        assertEquals("AUTENTICACAO_NECESSARIA", erro.getCode());
        assertEquals("Credenciais inválidas.", erro.getMessage());
        assertTrue(erro.getDetails().isEmpty());
    }
    @Configuration(proxyBeanMethods = false)
    static class Config {
        @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-10-30T08:00:00Z"), ZoneOffset.UTC); }
    }
}
