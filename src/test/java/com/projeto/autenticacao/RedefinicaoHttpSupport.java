package com.projeto.autenticacao;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.*;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=GestaoacoesApplication.class,properties={"auth.test.redefinicao=true","spring.datasource.hikari.connection-init-sql=SET LOCK_TIMEOUT 10000"})
@AutoConfigureMockMvc(print=org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE) @ActiveProfiles("test")
abstract class RedefinicaoHttpSupport {
    @Autowired MockMvc http;
    @Autowired UsuarioRepository usuarios;
    @Autowired AutorizacaoRedefinicaoSenhaRepository autorizacoes;
    @Autowired DataSource dataSource;
    @Autowired PasswordEncoder encoder;
    @Autowired JsonMapper mapper;
    @Autowired com.projeto.security.JwtService jwt;
    @Autowired com.projeto.security.SessaoService sessoes;
    @MockitoSpyBean(name="applicationClock") Clock clock;
    Conta admin;
    final Instant inicio=Instant.parse("2030-01-01T10:00:00Z");
    @BeforeEach void preparar() {
        quando(inicio);
        var jdbc=jdbc();
        for(String tabela:new String[]{"auditoria_usuario","autorizacao_redefinicao_senha","sessao_autenticacao","contador_abuso","usuario"}) jdbc.update("delete from "+tabela);
        admin=conta(Papel.ADMIN);
    }
    void quando(Instant instante) {
        org.mockito.Mockito.doReturn(instante).when(clock).instant();
        org.mockito.Mockito.doReturn(instante.toEpochMilli()).when(clock).millis();
    }
    JdbcTemplate jdbc() {return new JdbcTemplate(dataSource);}
    Conta conta(Papel papel) {
        var senha=UUID.randomUUID().toString(); var nome="teste."+UUID.randomUUID();
        var u=new Usuario(nome,papel,OffsetDateTime.now(clock)); u.definirCredencialInicial(encoder.encode(senha),u.getCriadaEm());usuarios.saveAndFlush(u);
        return new Conta(u.getId(),nome,senha);
    }
    Cookie csrf() throws Exception {return http.perform(get("/auth/csrf").secure(true)).andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN");}
    ResultActions postJson(String path,String body,Cookie... cookies) throws Exception {
        var csrf=csrf();var todos=new ArrayList<>(Arrays.asList(cookies));todos.add(csrf);
        return http.perform(post(path).secure(true).cookie(todos.toArray(Cookie[]::new)).header("X-XSRF-TOKEN",csrf.getValue()).contentType(MediaType.APPLICATION_JSON).content(body));
    }
    Cookie login(Conta conta) throws Exception {
        return postJson("/auth/login",mapper.writeValueAsString(Map.of("username",conta.nome(),"senha",conta.senha())))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("GESTAOACOES_AUTH");
    }
    ResultActions emitir(Cookie auth,Long id,long versao) throws Exception {return postJson("/usuarios/"+id+"/redefinicoes","{\"versaoEsperada\":"+versao+"}",auth);}
    ResultActions situacao(Cookie auth,Long id,boolean ativo,long versao) throws Exception {
        var csrf=csrf();return http.perform(patch("/usuarios/{id}/situacao",id).secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":"+ativo+",\"versaoEsperada\":"+versao+"}"));
    }
    String codigo(Cookie auth,Conta alvo,long versao) throws Exception {
        return mapper.readTree(emitir(auth,alvo.id(),versao).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).at("/autorizacao/codigo").asText();
    }
    ResultActions confirmar(String username,String codigo,String senha,String confirmacao,Cookie... cookies) throws Exception {
        return postJson("/auth/redefinicoes/confirmar",mapper.writeValueAsString(Map.of("username",username,"codigo",codigo,"senha",senha,"confirmacao",confirmacao)),cookies);
    }
    void como(Cookie cookie,Runnable teste) {
        var claims=jwt.validar(cookie.getValue());var principal=sessoes.autenticar(claims);
        var auth=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal,null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+principal.papel())));
        auth.setDetails(claims);org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {teste.run();} finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    }
    record Conta(Long id,String nome,String senha) {@Override public String toString(){return "Conta[fixture protegida]";}}
}
