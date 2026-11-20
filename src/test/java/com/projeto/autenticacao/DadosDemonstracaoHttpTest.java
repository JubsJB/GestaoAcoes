package com.projeto.autenticacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.projeto.entities.autenticacao.Papel;
import com.projeto.entities.autenticacao.Usuario;
import com.projeto.integrations.cotacao.CotacaoHistoricaProvider;
import jakarta.servlet.http.Cookie;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
@ActiveProfiles(value = {"operacional", "apresentacao-http", "dados-demonstracao"}, inheritProfiles = false)
@TestPropertySource(properties = {"op.db.name=demonstracao",
        "spring.datasource.url=jdbc:postgresql://postgres:5432/demonstracao",
        "app.demo-run-id=8be50ff0-6da3-4b3e-bdd6-afa8c127640a"})
class DadosDemonstracaoHttpTest extends ApresentacaoHttpAuthTest {
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;

    @Test void cadastraECapturaCarteiraMultimoedaSemClientesExternos() throws Exception {
        assertFalse(context.containsBean("brapiRestClient"));
        assertFalse(context.containsBean("alphaVantageRestClient"));
        assertFalse(context.containsBean("brasilApiRestClient"));
        assertFalse(context.containsBean("viaCepRestClient"));
        assertTrue(context.getBeansOfType(CotacaoHistoricaProvider.class).isEmpty());

        mvc.perform(viaProxy(get("/apresentacao/catalogo"))).andExpect(status().isUnauthorized());
        Cookie leitor = login(Papel.USER);
        mvc.perform(viaProxy(get("/apresentacao/catalogo")).cookie(leitor)).andExpect(status().isForbidden());
        Cookie admin = login(Papel.ADMIN);
        Cookie csrf = token();
        mvc.perform(viaProxy(get("/apresentacao/catalogo")).cookie(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("dados-demonstracao"));
        mvc.perform(viaProxy(post("/acoes")).cookie(admin).contentType("application/json")
                .content("{\"ticker\":\"DEMA3\",\"mercado\":\"BRASIL\"}")).andExpect(status().isForbidden());
        mvc.perform(comando("/acoes", "{\"ticker\":\"XXXX3\",\"mercado\":\"BRASIL\"}", admin, csrf))
                .andExpect(status().isNotFound());
        mvc.perform(comando("/acoes", "{\"ticker\":\"DEMA3\",\"mercado\":\"BRASIL\"}", admin, csrf))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.cotacaoAtual").value(24));
        mvc.perform(comando("/acoes", "{\"ticker\":\"DEMA3\",\"mercado\":\"BRASIL\"}", admin, csrf))
                .andExpect(status().isConflict());
        mvc.perform(comando("/acoes", "{\"ticker\":\"ZDMO\",\"mercado\":\"EUA\"}", admin, csrf))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.cotacaoAtual").value(120));
        mvc.perform(comando("/acoes", "{\"ticker\":\"DEMB4\",\"mercado\":\"BRASIL\"}", admin, csrf))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.cotacaoAtual").value(27));
        var corretora = mvc.perform(comando("/corretoras", "{\"cnpj\":\"99999999000191\"}", admin, csrf))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.validadaMercadoFinanceiro").value(false))
                .andReturn().getResponse().getHeader("Location");
        var carteira = mvc.perform(comando("/carteiras", "{\"nome\":\"Cenário isolado\"}", admin, csrf))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        var sul = mvc.perform(comando("/corretoras", "{\"cnpj\":\"99999998000147\"}", admin, csrf))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        var equilibrio = mvc.perform(comando("/carteiras", "{\"nome\":\"Equilíbrio isolado\"}", admin, csrf))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        long id = id(carteira), corretoraId = id(corretora), idSul = id(sul), idEquilibrio = id(equilibrio);
        operar(admin, csrf, id, corretoraId, "DEMA3", "BRASIL", "COMPRA", "100", 20, "2026-10-20");
        operar(admin, csrf, id, corretoraId, "DEMA3", "BRASIL", "COMPRA", "50", 26, "2026-10-21");
        operar(admin, csrf, id, corretoraId, "ZDMO", "EUA", "COMPRA", "2.5", 100, "2026-10-20");
        operar(admin, csrf, id, idSul, "DEMB4", "BRASIL", "COMPRA", "10", 30, "2026-10-20");
        operar(admin, csrf, idEquilibrio, idSul, "DEMB4", "BRASIL", "COMPRA", "20", 30, "2026-10-20");
        operar(admin, csrf, idEquilibrio, idSul, "ZDMO", "EUA", "COMPRA", "1.5", 110, "2026-10-20");
        mvc.perform(comando("/carteiras/"+id+"/snapshots", "", admin, csrf)).andExpect(status().isCreated());
        mvc.perform(comando("/carteiras/"+idEquilibrio+"/snapshots", "", admin, csrf)).andExpect(status().isCreated());
        operar(admin, csrf, id, corretoraId, "DEMA3", "BRASIL", "VENDA", "40", 28, "2026-10-23");
        mvc.perform(comando("/carteiras/"+id+"/snapshots", "", admin, csrf)).andExpect(status().isCreated());
        operar(admin, csrf, idEquilibrio, idSul, "DEMB4", "BRASIL", "VENDA", "5", 28, "2026-10-23");
        mvc.perform(comando("/carteiras/"+idEquilibrio+"/snapshots", "", admin, csrf)).andExpect(status().isCreated());
        mvc.perform(viaProxy(get("/carteiras/"+idEquilibrio+"/resumo")).cookie(leitor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resumos[0].patrimonioAtual").value(405))
                .andExpect(jsonPath("$.resumos[0].resultadoNaoRealizadoTotal").value(-45))
                .andExpect(jsonPath("$.resumos[1].patrimonioAtual").value(180));
        mvc.perform(viaProxy(get("/carteiras/"+id+"/resultados-realizados")).cookie(leitor))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].resultadoRealizado").value(240));
        mvc.perform(viaProxy(get("/carteiras/"+idEquilibrio+"/resultados-realizados")).cookie(leitor))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].resultadoRealizado").value(-10));
        mvc.perform(viaProxy(get("/carteiras/"+id+"/resumo")).cookie(leitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumos[0].patrimonioAtual").value(2910))
                .andExpect(jsonPath("$.resumos[1].patrimonioAtual").value(300))
                .andExpect(jsonPath("$.resumos[0].resultadoNaoRealizadoTotal").value(190));
        mvc.perform(viaProxy(get("/carteiras/"+id+"/posicoes")).cookie(leitor)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].precoMedio").value(22));
        mvc.perform(viaProxy(get("/carteiras/"+id+"/evolucao-patrimonial")).cookie(leitor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pontos.length()").value(2))
                .andExpect(jsonPath("$.pontos[0].patrimonios[0].patrimonioAtual").value(3870))
                .andExpect(jsonPath("$.pontos[1].patrimonios[0].patrimonioAtual").value(2910));
    }

    private Cookie login(Papel papel) throws Exception {
        String username = "simulado."+UUID.randomUUID();
        String senha = UUID.randomUUID().toString();
        var usuario = new Usuario(username, papel, OffsetDateTime.now());
        usuario.definirCredencialInicial(encoder.encode(senha), usuario.getCriadaEm());
        usuarios.saveAndFlush(usuario);
        return mvc.perform(comando("/auth/login", "{\"username\":\""+username+"\",\"senha\":\""+senha+"\"}", null, token()))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("GESTAOACOES_AUTH");
    }

    private Cookie token() throws Exception {
        return mvc.perform(viaProxy(get("/auth/csrf"))).andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }

    private void operar(Cookie admin, Cookie csrf, long id, long corretora, String ticker, String mercado,
                        String tipo, String quantidade, int preco, String data) throws Exception {
        String json = """
                {"carteiraId":%d,"corretoraId":%d,"ticker":"%s","mercado":"%s","tipo":"%s",
                 "quantidade":%s,"precoUnitario":%d,"dataOperacao":"%s"}
                """.formatted(id, corretora, ticker, mercado, tipo, quantidade, preco, data);
        mvc.perform(comando("/operacoes", json, admin, csrf)).andExpect(status().isCreated());
    }

    private long id(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf('/')+1));
    }

    private MockHttpServletRequestBuilder comando(String rota, String json, Cookie admin, Cookie csrf) {
        var request = viaProxy(post(rota)).contentType("application/json").content(json)
                .cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue());
        return admin == null ? request : request.cookie(admin);
    }

    private MockHttpServletRequestBuilder viaProxy(MockHttpServletRequestBuilder request) {
        return request.header("Host","localhost:8081").header("X-Forwarded-Proto","http")
                .header("X-Forwarded-Host","localhost").header("X-Forwarded-Port","8081");
    }
}
