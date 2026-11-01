package com.projeto.autenticacao;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.UsuarioRepository;
import com.projeto.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.servlet.http.Cookie;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReinicioAutenticacaoTest {
    private final String chaveReset=AuthRuntimeConfigTest.chave();
    @Test void reinicioPreservaCodigoTentativasConsumoERevogacao() throws Exception {
        String url="jdbc:h2:mem:reset_restart_"+UUID.randomUUID();
        String chave=AuthRuntimeConfigTest.chave(),senha=UUID.randomUUID().toString(),nova=UUID.randomUUID().toString(),nome="reset."+UUID.randomUUID();
        Instant inicio=Instant.parse("2030-01-01T10:00:00Z");String codigo;Cookie antigo;
        try(var keeper=DriverManager.getConnection(url,"sa","")) {
            try(var primeiro=iniciar(url,chave,Clock.fixed(inicio,ZoneOffset.UTC))) {
                var u=new Usuario(nome,Papel.ADMIN,OffsetDateTime.ofInstant(inicio,ZoneOffset.UTC));
                u.definirCredencialInicial(primeiro.getBean(PasswordEncoder.class).encode(senha),u.getCriadaEm());primeiro.getBean(UsuarioRepository.class).saveAndFlush(u);
                antigo=new Cookie("GESTAOACOES_AUTH",primeiro.getBean(SessaoService.class).login(nome,senha,null,"127.0.0.1").token());
                var http=mvc(primeiro);var csrf=tokenCsrf(http);
                var resposta=http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/usuarios/{id}/redefinicoes",u.getId()).secure(true).cookie(antigo,csrf)
                        .header("X-XSRF-TOKEN",csrf.getValue()).contentType("application/json").content("{\"versaoEsperada\":0}"))
                        .andExpect(status().isOk()).andReturn().getResponse();
                codigo=primeiro.getBean(tools.jackson.databind.json.JsonMapper.class).readTree(resposta.getContentAsString()).at("/autorizacao/codigo").asText();
                confirmarHttp(http,nome,"incorreto",nova,antigo).andExpect(status().isBadRequest());
            }
            try(var segundo=iniciar(url,chave,Clock.fixed(inicio.plusSeconds(899),ZoneOffset.UTC))) {
                org.junit.jupiter.api.Assertions.assertEquals(4,segundo.getBean(com.projeto.repositories.autenticacao.AutorizacaoRedefinicaoSenhaRepository.class).findAll().get(0).getTentativasRestantes());
                confirmarHttp(mvc(segundo),nome,codigo,nova,antigo).andExpect(status().isNoContent());
                mvc(segundo).perform(get("/auth/me").secure(true).cookie(antigo)).andExpect(status().isUnauthorized());
            }
            try(var terceiro=iniciar(url,chave,Clock.fixed(inicio.plusSeconds(899),ZoneOffset.UTC))) {
                confirmarHttp(mvc(terceiro),nome,codigo,nova,antigo).andExpect(status().isBadRequest());
                var registro=terceiro.getBean(com.projeto.repositories.autenticacao.AutorizacaoRedefinicaoSenhaRepository.class).findAll().get(0);
                org.junit.jupiter.api.Assertions.assertNotNull(registro.getConsumidaEm());
                var jdbc=new org.springframework.jdbc.core.JdbcTemplate(terceiro.getBean(javax.sql.DataSource.class));
                org.junit.jupiter.api.Assertions.assertEquals(3,jdbc.queryForObject("select tentativas from contador_abuso where tipo='REDEFINICAO_IP'",Integer.class));
                var novo=new Cookie("GESTAOACOES_AUTH",terceiro.getBean(SessaoService.class).login(nome,nova,null,"127.0.0.1").token());
                mvc(terceiro).perform(get("/auth/me").secure(true).cookie(novo)).andExpect(status().isOk());
            }
        }
    }
    private Cookie tokenCsrf(MockMvc http) throws Exception {
        return http.perform(get("/auth/csrf").secure(true)).andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
    private org.springframework.test.web.servlet.ResultActions confirmarHttp(MockMvc http,String nome,String codigo,String senha,Cookie antigo) throws Exception {
        var csrf=tokenCsrf(http);
        return http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/redefinicoes/confirmar").secure(true).cookie(antigo,csrf)
                .header("X-XSRF-TOKEN",csrf.getValue()).contentType("application/json")
                .content("{\"username\":\""+nome+"\",\"codigo\":\""+codigo+"\",\"senha\":\""+senha+"\",\"confirmacao\":\""+senha+"\"}"));
    }
    @Test void aumentarConfiguracaoAposReinicioNaoProlongaSessaoJaEmitida() throws Exception {
        String url="jdbc:h2:mem:auth_restart_"+UUID.randomUUID();
        String chave=AuthRuntimeConfigTest.chave(), senha=UUID.randomUUID().toString(), nome="limite."+UUID.randomUUID();
        Instant inicio=Instant.parse("2026-10-30T08:00:00Z");
        var relogio=new RelogioControlado(inicio,ZoneOffset.UTC);
        Cookie auth;
        try(var keeper=DriverManager.getConnection(url,"sa","")) {
            try(var primeiro=iniciar(url,chave,relogio,"PT1H")) {
                var u=new Usuario(nome,Papel.USER,OffsetDateTime.ofInstant(inicio,ZoneOffset.UTC));
                u.definirCredencialInicial(primeiro.getBean(PasswordEncoder.class).encode(senha),u.getCriadaEm());
                primeiro.getBean(UsuarioRepository.class).saveAndFlush(u);
                var sessoes=primeiro.getBean(SessaoService.class);
                var login=sessoes.login(nome,senha,null,"127.0.0.1");
                auth=new Cookie("GESTAOACOES_AUTH",login.token());
                var claims=primeiro.getBean(JwtService.class).validar(login.token());
                org.junit.jupiter.api.Assertions.assertEquals(inicio.plusSeconds(3600),claims.expiraEm());
                for(int minuto=9;minuto<=54;minuto+=9) {
                    relogio.agora.set(inicio.plusSeconds(minuto*60));
                    sessoes.atividade(claims);
                }
                relogio.agora.set(inicio.plusSeconds(3540));
                sessoes.atividade(claims);
            }
            try(var segundo=iniciar(url,chave,Clock.fixed(inicio.plusSeconds(3599),ZoneOffset.UTC),"PT4H")) {
                mvc(segundo).perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isOk())
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.expiraEm").value("2026-10-30T09:00:00Z"));
            }
            try(var terceiro=iniciar(url,chave,Clock.fixed(inicio.plusSeconds(3600),ZoneOffset.UTC),"PT4H")) {
                mvc(terceiro).perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized());
            }
        }
    }
    @Test void reinicioRealDosContextosPreservaSessaoRevogacaoEPrazoOcioso() throws Exception {
        String url="jdbc:h2:mem:auth_restart_"+UUID.randomUUID();
        String chave=AuthRuntimeConfigTest.chave(), senha=UUID.randomUUID().toString(), nome="reinicio."+UUID.randomUUID();
        Instant inicio=Instant.parse("2026-10-30T08:00:00Z");
        Cookie revogado, valido;
        // Conexão keeper mantém somente este H2 em memória durante o fechamento dos contextos.
        try(var keeper=DriverManager.getConnection(url,"sa","")) {
            try(var primeiro=iniciar(url,chave,Clock.fixed(inicio,ZoneOffset.UTC))) {
                var u=new Usuario(nome,Papel.USER,OffsetDateTime.ofInstant(inicio,ZoneOffset.UTC));
                u.definirCredencialInicial(primeiro.getBean(PasswordEncoder.class).encode(senha),u.getCriadaEm());
                primeiro.getBean(UsuarioRepository.class).saveAndFlush(u);
                var sessoes=primeiro.getBean(SessaoService.class);
                var login=sessoes.login(nome,senha,null,"127.0.0.1");
                revogado=new Cookie("GESTAOACOES_AUTH",login.token());
                sessoes.logout(primeiro.getBean(JwtService.class).validar(login.token()));
                valido=new Cookie("GESTAOACOES_AUTH",sessoes.login(nome,senha,null,"127.0.0.1").token());
                mvc(primeiro).perform(get("/auth/me").secure(true).cookie(valido)).andExpect(status().isOk());
            }
            try(var segundo=iniciar(url,chave,Clock.fixed(inicio.plusSeconds(540),ZoneOffset.UTC))) {
                mvc(segundo).perform(get("/auth/me").secure(true).cookie(revogado)).andExpect(status().isUnauthorized());
                mvc(segundo).perform(get("/auth/me").secure(true).cookie(valido)).andExpect(status().isOk());
            }
            try(var terceiro=iniciar(url,chave,Clock.fixed(inicio.plusSeconds(600),ZoneOffset.UTC))) {
                mvc(terceiro).perform(get("/auth/me").secure(true).cookie(valido)).andExpect(status().isUnauthorized());
            }
        }
    }
    private ConfigurableApplicationContext iniciar(String url,String chave,Clock clock) {
        return iniciar(url,chave,clock,"PT4H");
    }
    private ConfigurableApplicationContext iniciar(String url,String chave,Clock clock,String duracao) {
        return new SpringApplicationBuilder(GestaoacoesApplication.class).profiles("test").initializers(context -> {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("reinicio-isolado",Map.of(
                    "spring.datasource.url",url,"spring.datasource.username","sa","spring.datasource.password","",
                    "auth.jwt.secret",chave,"auth.absolute-duration",duracao,"server.address","127.0.0.1","server.port","0",
                    "spring.main.banner-mode","off","logging.level.root","WARN","auth.reset.secret",chaveReset)));
            // Clock é a fronteira de teste aprovada; não substituir autenticação ou persistência.
            context.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("applicationClock",clock));
        }).run();
    }
    private MockMvc mvc(ConfigurableApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup((WebApplicationContext)context).apply(springSecurity()).build();
    }
    private static final class RelogioControlado extends Clock {
        private final java.util.concurrent.atomic.AtomicReference<Instant> agora;
        private final ZoneId zona;
        RelogioControlado(Instant instante,ZoneId zona) { this(new java.util.concurrent.atomic.AtomicReference<>(instante),zona); }
        private RelogioControlado(java.util.concurrent.atomic.AtomicReference<Instant> agora,ZoneId zona) { this.agora=agora; this.zona=zona; }
        @Override public ZoneId getZone() { return zona; }
        @Override public Clock withZone(ZoneId zona) { return new RelogioControlado(agora,zona); }
        @Override public Instant instant() { return agora.get(); }
    }
}
