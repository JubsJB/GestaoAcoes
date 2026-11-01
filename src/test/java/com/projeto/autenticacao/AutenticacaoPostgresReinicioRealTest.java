package com.projeto.autenticacao;

import com.projeto.GestaoacoesApplication;
import com.projeto.security.BootstrapAdminService;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AutenticacaoPostgresReal.Ciclo.class)
class AutenticacaoPostgresReinicioRealTest {
    private final Instant inicio=Instant.parse("2030-01-01T10:00:00Z");
    @Test void reinicioDeAplicacaoEBancoPreservaTentativasConsumoRevogacaoEPrazos()throws Exception {
        var run=AutenticacaoPostgresExecucao.obter();
        String senhaAdmin=UUID.randomUUID().toString(),senhaInicial=UUID.randomUUID().toString(),senhaNova=UUID.randomUUID().toString();
        Cookie admin,antigaA,antigaB,revogada,ociosa;String codigo;long id;
        try(var ctx=iniciar(inicio)) {
            var jdbc=jdbc(ctx);
            for(String tabela:List.of("auditoria_usuario","autorizacao_redefinicao_senha","sessao_autenticacao","contador_abuso","usuario"))jdbc.update("delete from "+tabela);
            assertTrue(ctx.getBean(BootstrapAdminService.class).executar("reinicio.admin",senhaAdmin));
            var c=new Cliente(ctx);admin=c.login("reinicio.admin",senhaAdmin);
            var cadastro=c.json(c.post("/usuarios",Map.of("username","reinicio.user","papel","USER"),admin).andExpect(status().isCreated()));
            id=cadastro.at("/usuario/id").asLong();String inicial=cadastro.at("/autorizacao/codigo").asText();assertFalse(inicial.isBlank());
            c.confirmar(inicial,senhaInicial).andExpect(status().isNoContent());
            antigaA=c.login("reinicio.user",senhaInicial);antigaB=c.login("reinicio.user",senhaInicial);
            long versao=c.json(c.http.perform(get("/usuarios/{id}",id).secure(true).cookie(admin)).andExpect(status().isOk())).path("versao").asLong();
            codigo=c.json(c.post("/usuarios/"+id+"/redefinicoes",Map.of("versaoEsperada",versao),admin).andExpect(status().isOk())).at("/autorizacao/codigo").asText();
            c.confirmar("INVALIDO",senhaNova).andExpect(status().isBadRequest());
            assertEquals(4,jdbc.queryForObject("select tentativas_restantes from autorizacao_redefinicao_senha where consumida_em is null and invalidada_em is null",Integer.class));
        }
        // Não há pool/contexto aberto durante o reinício; volume e IDs continuam os do manifesto.
        run.reiniciarBanco();
        try(var ctx=iniciar(inicio.plusSeconds(300))) {
            var c=new Cliente(ctx);c.http.perform(get("/auth/me").secure(true).cookie(antigaA)).andExpect(status().isOk());
            assertEquals(4,jdbc(ctx).queryForObject("select tentativas_restantes from autorizacao_redefinicao_senha where consumida_em is null and invalidada_em is null",Integer.class));
            var resposta=c.confirmar(codigo,senhaNova).andExpect(status().isNoContent()).andReturn().getResponse();assertNull(resposta.getCookie("GESTAOACOES_AUTH"));
            for(var antiga:List.of(antigaA,antigaB))c.http.perform(get("/auth/me").secure(true).cookie(antiga)).andExpect(status().isUnauthorized());
            revogada=c.login("reinicio.user",senhaNova);ociosa=c.login("reinicio.user",senhaNova);
            c.post("/auth/logout",null,revogada).andExpect(status().isNoContent());
        }
        try(var ctx=iniciar(inicio.plusSeconds(900))) {
            var c=new Cliente(ctx);
            for(var cookie:List.of(antigaA,antigaB,revogada,ociosa))c.http.perform(get("/auth/me").secure(true).cookie(cookie)).andExpect(status().isUnauthorized());
            c.post("/auth/atividade",null,ociosa).andExpect(status().isUnauthorized());
            c.confirmar(codigo,senhaNova).andExpect(status().isBadRequest());
            assertEquals(2,jdbc(ctx).queryForObject("select count(*) from autorizacao_redefinicao_senha where consumida_em is not null",Integer.class));
            assertEquals(2,jdbc(ctx).queryForObject("select versao_seguranca from usuario where id=?",Integer.class,id));
        }
        System.out.println("AUTH_PG_REINICIO_APLICACAO_COMPROVADO contextos=3 codigoTentativasConsumoRevogacaoPrazos=true");
    }
    private ConfigurableApplicationContext iniciar(Instant agora) {
        return new SpringApplicationBuilder(GestaoacoesApplication.class).profiles("custodia-pg17")
                .properties("spring.main.banner-mode=off","logging.level.root=WARN","logging.level.org.hibernate.orm.jdbc.error=OFF")
                .initializers(new AutenticacaoPostgresExecucao.Initializer(),ctx->ctx.addBeanFactoryPostProcessor(f->f.registerSingleton("applicationClock",Clock.fixed(agora,ZoneOffset.UTC)))).run();
    }
    private JdbcTemplate jdbc(ConfigurableApplicationContext ctx){return new JdbcTemplate(ctx.getBean(javax.sql.DataSource.class));}
    private static final class Cliente {
        final MockMvc http;final JsonMapper mapper;
        Cliente(ConfigurableApplicationContext ctx){http=MockMvcBuilders.webAppContextSetup((WebApplicationContext)ctx).apply(springSecurity()).build();mapper=ctx.getBean(JsonMapper.class);}
        ResultActions post(String path,Object body,Cookie... auth)throws Exception {
            var csrf=http.perform(get("/auth/csrf").secure(true)).andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN");
            var cookies=new ArrayList<>(List.of(auth));cookies.add(csrf);
            var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).secure(true).cookie(cookies.toArray(Cookie[]::new)).header("X-XSRF-TOKEN",csrf.getValue());
            if(body!=null)request.contentType("application/json").content(mapper.writeValueAsString(body));return http.perform(request);
        }
        Cookie login(String nome,String senha)throws Exception{return post("/auth/login",Map.of("username",nome,"senha",senha)).andExpect(status().isOk()).andReturn().getResponse().getCookie("GESTAOACOES_AUTH");}
        ResultActions confirmar(String codigo,String senha)throws Exception{return post("/auth/redefinicoes/confirmar",Map.of("username","reinicio.user","codigo",codigo,"senha",senha,"confirmacao",senha));}
        tools.jackson.databind.JsonNode json(ResultActions resultado)throws Exception{return mapper.readTree(resultado.andReturn().getResponse().getContentAsString());}
    }
}
