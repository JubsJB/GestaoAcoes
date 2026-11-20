package com.projeto.autenticacao;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.UsuarioRepository;
import com.projeto.security.SessaoService;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=GestaoacoesApplication.class,properties="auth.test.outage=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class SegurancaIndisponivelHttpTest {
    @Autowired MockMvc http;
    @Autowired DataSource dataSource;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired SessaoService sessoes;
    @Test void poolIndisponivelNegaIdentidadeENegocioCom503SemReusarPermissao() throws Exception {
        String senha=UUID.randomUUID().toString(), nome="falha."+UUID.randomUUID();
        var u=new Usuario(nome,Papel.ADMIN,OffsetDateTime.now(ZoneOffset.UTC));
        u.definirCredencialInicial(encoder.encode(senha),u.getCriadaEm()); usuarios.saveAndFlush(u);
        Cookie auth=new Cookie("GESTAOACOES_AUTH",sessoes.login(nome,senha,null,"127.0.0.1").token());
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isOk());
        ((HikariDataSource)dataSource).close();
        for (String path : new String[]{"/auth/me","/carteiras"})
            http.perform(get(path).secure(true).cookie(auth)).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("SEGURANCA_INDISPONIVEL"))
                    .andExpect(cookie().doesNotExist("GESTAOACOES_AUTH"));
        http.perform(get("/auth/csrf").secure(true)).andExpect(status().isNoContent());
    }
}
