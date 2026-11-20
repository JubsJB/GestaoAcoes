package com.projeto.autenticacao;

import static org.junit.jupiter.api.Assertions.*;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.autenticacao.Papel;
import com.projeto.entities.autenticacao.Usuario;
import com.projeto.repositories.autenticacao.UsuarioRepository;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {GestaoacoesApplication.class, ApresentacaoHttpAuthTest.BancoDescartavel.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.location=classpath:/application-test.properties",
        "server.address=127.0.0.1",
        "app.public-origin=http://localhost:8081",
        "app.trusted-proxy-ip=127.0.0.1",
        "auth.allow-local-http=false",
        "server.forward-headers-strategy=native",
        "server.tomcat.remoteip.internal-proxies=127.0.0.1/32",
        "server.tomcat.remoteip.remote-ip-header=X-Forwarded-For",
        "server.tomcat.remoteip.protocol-header=X-Forwarded-Proto",
        "server.tomcat.remoteip.host-header=X-Forwarded-Host",
        "server.tomcat.remoteip.port-header=X-Forwarded-Port",
        "op.db.name=demo_fixture", "op.db.app-user=demo_fixture",
        "spring.datasource.url=jdbc:postgresql://postgres:5432/demo_fixture",
        "spring.datasource.username=demo_fixture", "spring.datasource.password=fixture",
        "integration.brasil-api.base-url=http://127.0.0.1:1",
        "integration.via-cep.base-url=http://127.0.0.1:1",
        "integration.brapi.base-url=http://127.0.0.1:1",
        "integration.alpha-vantage.base-url=http://127.0.0.1:1"
})
@ActiveProfiles({"operacional", "apresentacao-http"})
class ApresentacaoHttpAuthTest {
    @TestConfiguration(proxyBeanMethods = false)
    @PropertySource("classpath:application.properties")
    static class BancoDescartavel {
        @Bean(destroyMethod = "shutdown")
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        }
    }

    private static final String FORWARDING = "X-Forwarded-Proto: http\r\n"
            + "X-Forwarded-Host: localhost\r\nX-Forwarded-Port: 8081\r\n";
    private static final String ORIGIN = "Origin: http://localhost:8081\r\n";

    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @LocalServerPort int port;

    @ParameterizedTest
    @EnumSource(Papel.class)
    void sessaoEPermissoesPeloTransporteHttp(Papel papel) throws Exception {
        String username = "http." + UUID.randomUUID();
        String senha = UUID.randomUUID().toString();
        var usuario = new Usuario(username, papel, OffsetDateTime.now());
        usuario.definirCredencialInicial(encoder.encode(senha), usuario.getCriadaEm());
        usuarios.saveAndFlush(usuario);

        assertStatus(400, enviar("GET", "/auth/csrf", "", ""));
        assertStatus(400, enviar("GET", "/auth/csrf", FORWARDING + "X-Forwarded-For: 10.0.0.90\r\n", ""));
        assertStatus(403, enviar("GET", "/auth/csrf", FORWARDING + "Origin: http://localhost:8090\r\n", ""));
        assertStatus(400, enviar("GET", "/auth/csrf", FORWARDING.replace("Proto: http", "Proto: https"), ""));
        String dados = "{\"username\":\"" + username + "\",\"senha\":\"" + senha + "\"}";
        assertStatus(403, enviar("POST", "/auth/login", FORWARDING + ORIGIN, dados));

        String csrf = csrf();
        String login = enviar("POST", "/auth/login", FORWARDING + ORIGIN + cookies(csrf, ""), dados);
        assertStatus(200, login);
        assertTrue(atributos(cookie(login, "GESTAOACOES_AUTH")).contains("; httponly"));
        String auth = cookie(login, "GESTAOACOES_AUTH").split(";", 2)[0];
        String novoCsrf = csrf();
        assertStatus(200, enviar("GET", "/carteiras", FORWARDING + cookies(novoCsrf, auth), ""));
        assertStatus(papel == Papel.ADMIN ? 200 : 403,
                enviar("GET", "/usuarios", FORWARDING + cookies(novoCsrf, auth), ""));
        assertStatus(403, enviar("POST", "/auth/logout", FORWARDING + ORIGIN + "Cookie: " + auth + "\r\n", ""));
        String logout = enviar("POST", "/auth/logout", FORWARDING + ORIGIN + cookies(novoCsrf, auth), "");
        assertStatus(204, logout);
        assertTrue(atributos(cookie(logout, "GESTAOACOES_AUTH")).contains("max-age=0"));
        assertStatus(401, enviar("GET", "/auth/me", FORWARDING + cookies(novoCsrf, auth), ""));
    }

    private String csrf() throws Exception {
        String resposta = enviar("GET", "/auth/csrf", FORWARDING, "");
        assertStatus(204, resposta);
        String csrf = cookie(resposta, "XSRF-TOKEN");
        assertFalse(atributos(csrf).contains("; httponly"));
        return csrf.split(";", 2)[0].substring("XSRF-TOKEN=".length());
    }

    private String enviar(String method, String path, String headers, String body) throws Exception {
        try (var socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(5000);
            byte[] dados = body.getBytes(StandardCharsets.UTF_8);
            socket.getOutputStream().write((method + " " + path + " HTTP/1.1\r\nHost: localhost:8081\r\n"
                    + "Connection: close\r\nContent-Type: application/json\r\nContent-Length: " + dados.length
                    + "\r\n" + headers + "\r\n").getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().write(dados);
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String cookies(String csrf, String auth) {
        return "Cookie: XSRF-TOKEN=" + csrf + (auth.isEmpty() ? "" : "; " + auth)
                + "\r\nX-XSRF-TOKEN: " + csrf + "\r\n";
    }

    private static String atributos(String cookie) {
        String normalizado = cookie.toLowerCase(Locale.ROOT);
        assertTrue(normalizado.contains("; secure"));
        assertTrue(normalizado.contains("; samesite=lax"));
        assertFalse(normalizado.contains("; domain="));
        return normalizado;
    }

    private static String cookie(String resposta, String nome) {
        String prefix = "Set-Cookie: " + nome + "=";
        return resposta.lines().filter(line -> line.regionMatches(true, 0, prefix, 0, prefix.length()))
                .map(line -> line.substring("Set-Cookie: ".length())).findFirst().orElseThrow();
    }

    private static void assertStatus(int esperado, String resposta) {
        String status = resposta.lines().findFirst().orElse("");
        assertTrue(status.startsWith("HTTP/1.1 " + esperado + " "), status);
    }
}
