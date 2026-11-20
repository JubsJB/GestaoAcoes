package com.projeto.autenticacao;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=GestaoacoesApplication.class,properties="spring.datasource.hikari.connection-init-sql=SET LOCK_TIMEOUT 10000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessaoInatividadeHttpTest {
    static final Instant INICIO=Instant.parse("2026-10-30T08:00:00Z");
    @MockitoBean(name="applicationClock") Clock clock;
    @Autowired MockMvc http;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired DataSource dataSource;
    final AtomicReference<Instant> agora=new AtomicReference<>();
    String username, senha;
    Long usuarioId;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    @BeforeEach void preparar() {
        agora.set(INICIO);
        when(clock.instant()).thenAnswer(i->agora.get()); when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.millis()).thenAnswer(i->agora.get().toEpochMilli());
        new JdbcTemplate(dataSource).update("delete from contador_abuso");
        username="sessao."+UUID.randomUUID(); senha=UUID.randomUUID().toString();
        var u=new Usuario(username,Papel.USER,OffsetDateTime.ofInstant(INICIO,ZoneOffset.UTC));
        u.definirCredencialInicial(encoder.encode(senha),u.getCriadaEm()); usuarioId=usuarios.saveAndFlush(u).getId();
    }
    @Test void atividadeAntesDaBordaRenovaApenasInatividadeDaPropriaSessao() throws Exception {
        Cookie auth=login(), csrf=csrf();
        agora.set(INICIO.plusSeconds(599).plusNanos(999999000));
        var resposta=http.perform(post("/auth/atividade").secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"horarioCliente\":\"2099-01-01T00:00:00Z\",\"sid\":\"outro\",\"usuarioId\":99999999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(usuarioId))
                .andExpect(jsonPath("$.ultimaAtividadeEm").value("2026-10-30T08:09:59.999999Z"))
                .andExpect(jsonPath("$.expiraEm").value("2026-10-30T12:00:00Z")).andReturn().getResponse();
        assertNull(resposta.getCookie("GESTAOACOES_AUTH")); assertFalse(resposta.getContentAsString().contains(auth.getValue()));
        agora.set(INICIO.plusSeconds(600));
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.inativaEm").value("2026-10-30T08:19:59.999999Z"));
    }
    @Test void dezMinutosExatosNegamMeEAtividadeMesmoComPollingECsrf() throws Exception {
        Cookie auth=login(), csrf=csrf();
        for(int minuto=1;minuto<10;minuto++) {
            agora.set(INICIO.plusSeconds(minuto*60));
            http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.ultimaAtividadeEm").value("2026-10-30T08:00:00Z"));
            http.perform(get("/auth/csrf").secure(true).cookie(auth)).andExpect(status().isNoContent());
            http.perform(get("/carteiras").secure(true).cookie(auth)).andExpect(status().isOk());
        }
        agora.set(INICIO.plusSeconds(600));
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSAO_EXPIRADA")).andExpect(cookie().doesNotExist("GESTAOACOES_AUTH"));
        http.perform(post("/auth/atividade").secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SESSAO_EXPIRADA"));
        agora.set(INICIO.plusSeconds(601));
        http.perform(post("/auth/atividade").secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue()))
                .andExpect(status().isUnauthorized());
    }
    @Test void quatroHorasExatasExpiramMesmoComAtividadeContinuaSemRenovarJwtCookie() throws Exception {
        Cookie auth=login(), csrf=csrf();
        var jwt=com.nimbusds.jwt.SignedJWT.parse(auth.getValue()).getJWTClaimsSet();
        assertEquals(INICIO.plusSeconds(14400),jwt.getExpirationTime().toInstant()); assertEquals(14400,auth.getMaxAge());
        for(int minuto=9;minuto<240;minuto+=9) {
            agora.set(INICIO.plusSeconds(minuto*60));
            var response=http.perform(post("/auth/atividade").secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.expiraEm").value("2026-10-30T12:00:00Z")).andReturn().getResponse();
            assertNull(response.getCookie("GESTAOACOES_AUTH"));
        }
        agora.set(INICIO.plusSeconds(14399).plusNanos(999999000));
        http.perform(post("/auth/atividade").secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())).andExpect(status().isOk());
        agora.set(INICIO.plusSeconds(14400));
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized());
        http.perform(post("/auth/atividade").secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())).andExpect(status().isUnauthorized());
        agora.set(INICIO.plusSeconds(14401));
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized());
    }
    @Test void exemploOitoOitoEQuarentaDozeMantemPrazosIndependentes() throws Exception {
        Cookie auth=login(), csrf=csrf();
        for(int minuto=5;minuto<=30;minuto+=5) {
            agora.set(INICIO.plusSeconds(minuto*60));
            http.perform(post("/auth/atividade").secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())).andExpect(status().isOk());
        }
        agora.set(INICIO.plusSeconds(2399));
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.inativaEm").value("2026-10-30T08:40:00Z"))
                .andExpect(jsonPath("$.expiraEm").value("2026-10-30T12:00:00Z"));
        agora.set(INICIO.plusSeconds(2400));
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized());
    }
    @Test void atividadeDeOutraAbaMantemSidComumSemAfetarOutroNavegador() throws Exception {
        Cookie abaA=login(), abaB=new Cookie("GESTAOACOES_AUTH",abaA.getValue()), outro=login(), csrf=csrf();
        String outroSid=com.nimbusds.jwt.SignedJWT.parse(outro.getValue()).getJWTClaimsSet().getStringClaim("sid");
        agora.set(INICIO.plusSeconds(540));
        http.perform(post("/auth/atividade").secure(true).cookie(abaB,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"sid\":\""+outroSid+"\",\"usuarioId\":99999999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(usuarioId));
        agora.set(INICIO.plusSeconds(600));
        http.perform(get("/auth/me").secure(true).cookie(abaA)).andExpect(status().isOk());
        http.perform(get("/auth/me").secure(true).cookie(outro)).andExpect(status().isUnauthorized());
        http.perform(post("/auth/logout").secure(true).cookie(abaA,csrf).header("X-XSRF-TOKEN",csrf.getValue())).andExpect(status().isNoContent());
        http.perform(get("/auth/me").secure(true).cookie(abaB)).andExpect(status().isUnauthorized());
    }
    @Test void limiteDeLoginExpiraNaJanelaSemBloquearContaPermanentemente() throws Exception {
        Cookie csrf=csrf();
        for(int i=0;i<5;i++) http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+username+"\",\"senha\":\""+UUID.randomUUID()+"\"}"))
                .andExpect(status().isUnauthorized());
        http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+username+"\",\"senha\":\""+senha+"\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","900"));
        agora.set(INICIO.plusSeconds(900));
        login();
    }
    @Test void atividadeEsperandoLockReavaliaBordaAposEspera() throws Exception {
        verificarEsperaDeLock(false);
    }
    @Test void atividadeEsperandoLockReavaliaVersaoAposCommitExterno() throws Exception {
        verificarEsperaDeLock(true);
    }
    private void verificarEsperaDeLock(boolean alterarVersao) throws Exception {
        Cookie auth=login(), csrf=csrf();
        agora.set(INICIO.plusSeconds(599));
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var pronto=new java.util.concurrent.CountDownLatch(1); var liberar=new java.util.concurrent.CountDownLatch(1);
        var transaction=new org.springframework.transaction.support.TransactionTemplate(manager);
        try {
            var holder=pool.submit(()->transaction.executeWithoutResult(s->{
                usuarios.findByIdForUpdate(usuarioId).orElseThrow();
                if(alterarVersao) new JdbcTemplate(dataSource).update("update usuario set versao_seguranca=versao_seguranca+1 where id=?",usuarioId);
                pronto.countDown();
                try { assertTrue(liberar.await(10,java.util.concurrent.TimeUnit.SECONDS)); }
                catch(InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
            }));
            assertTrue(pronto.await(5,java.util.concurrent.TimeUnit.SECONDS));
            var atividade=pool.submit(()->http.perform(post("/auth/atividade").secure(true).cookie(auth,csrf)
                    .header("X-XSRF-TOKEN",csrf.getValue())).andReturn().getResponse().getStatus());
            esperarBloqueio();
            if(!alterarVersao) agora.set(INICIO.plusSeconds(600));
            liberar.countDown(); holder.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(401,atividade.get(10,java.util.concurrent.TimeUnit.SECONDS));
            http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized());
        } finally { liberar.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS)); }
    }
    void esperarBloqueio() {
        var jdbc=new JdbcTemplate(dataSource);long limite=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while(jdbc.queryForObject("select count(*) from information_schema.sessions where blocker_id is not null",Integer.class)==0 && System.nanoTime()<limite)
            java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(10));
        assertTrue(jdbc.queryForObject("select count(*) from information_schema.sessions where blocker_id is not null",Integer.class)>0,"request deve realmente esperar lock no H2");
    }
    @Test void logoutConcorrenteComAtividadeNaoPodeSerDesfeito() throws Exception {
        Cookie auth=login(), csrf=csrf();
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2); var iniciar=new java.util.concurrent.CountDownLatch(1);
        try {
            var atividade=pool.submit(()->{ iniciar.await(); return http.perform(post("/auth/atividade").secure(true).cookie(auth,csrf)
                    .header("X-XSRF-TOKEN",csrf.getValue())).andReturn().getResponse().getStatus(); });
            var logout=pool.submit(()->{ iniciar.await(); return http.perform(post("/auth/logout").secure(true).cookie(auth,csrf)
                    .header("X-XSRF-TOKEN",csrf.getValue())).andReturn().getResponse().getStatus(); });
            iniciar.countDown(); assertEquals(204,logout.get(15,java.util.concurrent.TimeUnit.SECONDS));
            assertTrue(java.util.Set.of(200,401).contains(atividade.get(15,java.util.concurrent.TimeUnit.SECONDS)));
            http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized());
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS)); }
    }
    Cookie csrf() throws Exception {
        return http.perform(get("/auth/csrf").secure(true)).andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
    Cookie login() throws Exception {
        Cookie csrf=csrf();
        return http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+username+"\",\"senha\":\""+senha+"\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("GESTAOACOES_AUTH");
    }
}
