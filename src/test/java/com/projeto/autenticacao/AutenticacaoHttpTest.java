package com.projeto.autenticacao;

import com.projeto.GestaoacoesApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacaoHttpTest {
    @Autowired MockMvc http;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired DataSource dataSource;
    @Autowired com.projeto.security.AuthRuntimeConfig runtime;
    static final String CHAVE_TESTE;
    static { byte[] bytes=new byte[64]; new java.security.SecureRandom().nextBytes(bytes); CHAVE_TESTE=java.util.Base64.getEncoder().encodeToString(bytes); }
    @org.springframework.test.context.DynamicPropertySource static void segredo(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.secret",()->CHAVE_TESTE);
    }
    @Test void assinaturaAlgoritmoIssuerAudienceDatasESidPrecisamSerValidos() throws Exception {
        Cookie auth=autenticar(Papel.USER);
        var parsed=com.nimbusds.jwt.SignedJWT.parse(auth.getValue());
        for (java.util.function.Consumer<com.nimbusds.jwt.JWTClaimsSet.Builder> mudar : java.util.List.<java.util.function.Consumer<com.nimbusds.jwt.JWTClaimsSet.Builder>>of(
                b->b.issuer("outro"), b->b.audience("outra"), b->b.expirationTime(new java.util.Date(0)),
                b->b.issueTime(java.util.Date.from(java.time.Instant.now().plusSeconds(60))),
                b->b.notBeforeTime(java.util.Date.from(java.time.Instant.now().plusSeconds(60))),
                b->b.claim("sid",UUID.randomUUID().toString()), b->b.subject("999999999"),
                b->b.claim("versaoSeguranca",42), b->b.claim("versaoSeguranca",1.5),
                b->b.claim("authorities",java.util.List.of("ROLE_ADMIN")), b->b.claim("authorities",java.util.List.of()),
                b->b.claim("sid","invalido"), b->b.subject("invalido"), b->b.claim("versaoSeguranca",null),
                b->b.issueTime(null), b->b.expirationTime(null), b->b.audience(java.util.List.of()))) {
            var builder=new com.nimbusds.jwt.JWTClaimsSet.Builder(parsed.getJWTClaimsSet()); mudar.accept(builder);
            var jwt=new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256),builder.build());
            jwt.sign(new com.nimbusds.jose.crypto.MACSigner(runtime.key().getEncoded()));
            http.perform(get("/auth/me").secure(true).cookie(new Cookie("GESTAOACOES_AUTH",jwt.serialize())))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTENTICACAO_NECESSARIA"));
        }
        var outroAlgoritmo=new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS512),parsed.getJWTClaimsSet());
        outroAlgoritmo.sign(new com.nimbusds.jose.crypto.MACSigner(runtime.key().getEncoded()));
        http.perform(get("/auth/me").secure(true).cookie(new Cookie("GESTAOACOES_AUTH",outroAlgoritmo.serialize()))).andExpect(status().isUnauthorized());
        byte[] outraChave=new byte[64]; new java.security.SecureRandom().nextBytes(outraChave);
        var assinaturaErrada=new com.nimbusds.jwt.SignedJWT(parsed.getHeader(),parsed.getJWTClaimsSet());
        assinaturaErrada.sign(new com.nimbusds.jose.crypto.MACSigner(outraChave));
        http.perform(get("/auth/me").secure(true).cookie(new Cookie("GESTAOACOES_AUTH",assinaturaErrada.serialize()))).andExpect(status().isUnauthorized());
        http.perform(get("/auth/me").secure(true).cookie(new Cookie("GESTAOACOES_AUTH","adulterado"))).andExpect(status().isUnauthorized());
        http.perform(get("/auth/me").secure(true).cookie(new Cookie("GESTAOACOES_AUTH",new com.nimbusds.jwt.PlainJWT(parsed.getJWTClaimsSet()).serialize()))).andExpect(status().isUnauthorized());
    }
    @Test void bearerUrlECookiesDuplicadosNaoFornecemCaminhoAlternativo() throws Exception {
        Cookie auth=autenticar(Papel.USER);
        http.perform(get("/auth/me").secure(true).header("Authorization","Bearer "+auth.getValue())).andExpect(status().isUnauthorized());
        http.perform(get("/auth/me").secure(true).param("access_token",auth.getValue())).andExpect(status().isUnauthorized());
        http.perform(get("/auth/me").secure(true).cookie(auth,auth)).andExpect(status().isUnauthorized());
    }
    @Test void csrfAusenteOuErradoNegaAntesDeEfeitosSemEncerrarSessao() throws Exception {
        Cookie auth=autenticar(Papel.ADMIN), csrf=csrf();
        for (String path : new String[]{"/auth/login","/auth/logout","/auth/atividade","/carteiras"}) {
            http.perform(post(path).secure(true).cookie(auth).header("Authorization","Bearer "+auth.getValue())
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALIDO"));
            http.perform(post(path).secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN","invalido")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALIDO"));
        }
        http.perform(post("/carteiras").secure(true)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALIDO"));
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isOk());
    }
    @Test void loginELogoutRenovamSomenteCsrfECookieInvalidoNaoImpedeLoginPublico() throws Exception {
        Cookie auth=autenticar(Papel.USER), csrf=csrf();
        var logout=http.perform(post("/auth/logout").secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue()))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        assertEquals(0,logout.getCookie("XSRF-TOKEN").getMaxAge());
        Cookie novo=csrf(); assertNotEquals(csrf.getValue(),novo.getValue());
        http.perform(post("/auth/login").secure(true).cookie(novo).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALIDO"));
        String senha=UUID.randomUUID().toString(), username="novo."+UUID.randomUUID();
        var u=new Usuario(username,Papel.USER,OffsetDateTime.now(java.time.ZoneOffset.UTC));
        u.definirCredencialInicial(encoder.encode(senha),u.getCriadaEm()); usuarios.saveAndFlush(u);
        var login=http.perform(post("/auth/login").secure(true).cookie(new Cookie("GESTAOACOES_AUTH","invalido"),novo)
                .header("X-XSRF-TOKEN",novo.getValue()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\""+username+"\",\"senha\":\""+senha+"\"}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertEquals(0,login.getCookie("XSRF-TOKEN").getMaxAge());
        assertNotNull(login.getCookie("GESTAOACOES_AUTH"));
    }
    @Test void loginNovoRevogaSomenteCookieAnteriorApresentado() throws Exception {
        Cookie outroNavegador=autenticar(Papel.USER), anterior=autenticar(Papel.USER), csrf=csrf();
        String senha=UUID.randomUUID().toString(), username="troca."+UUID.randomUUID();
        var u=new Usuario(username,Papel.USER,OffsetDateTime.now(java.time.ZoneOffset.UTC));
        u.definirCredencialInicial(encoder.encode(senha),u.getCriadaEm()); usuarios.saveAndFlush(u);
        http.perform(post("/auth/login").secure(true).cookie(anterior,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+username+"\",\"senha\":\""+senha+"\"}"))
                .andExpect(status().isOk());
        http.perform(get("/auth/me").secure(true).cookie(anterior)).andExpect(status().isUnauthorized());
        http.perform(get("/auth/me").secure(true).cookie(outroNavegador)).andExpect(status().isOk());
    }
    @Test void corsRejeitaOrigemExternaEDocumentacaoPublicaPermaneceAcessivel() throws Exception {
        http.perform(get("/auth/csrf").secure(true).header("Origin","https://fora.example")).andExpect(status().isForbidden());
        http.perform(options("/auth/login").secure(true).header("Origin","https://fora.example").header("Access-Control-Request-Method","POST"))
                .andExpect(status().isForbidden());
        http.perform(get("/v3/api-docs").secure(true)).andExpect(status().isOk());
    }
    @Test void indisponibilidadeDeAtivoOuVersaoInvalidaTodasAsSessoesSemVarredura() throws Exception {
        Cookie auth=autenticar(Papel.USER);
        long id=com.nimbusds.jwt.SignedJWT.parse(auth.getValue()).getJWTClaimsSet().getSubject()==null ? -1
                : Long.parseLong(com.nimbusds.jwt.SignedJWT.parse(auth.getValue()).getJWTClaimsSet().getSubject());
        var jdbc=new JdbcTemplate(dataSource);
        jdbc.update("update usuario set ativo=false,versao_seguranca=versao_seguranca+1 where id=?",id);
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized());
        jdbc.update("update usuario set ativo=true where id=?",id);
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized());
        assertEquals(0,jdbc.queryForObject("select count(*) from sessao_autenticacao where usuario_id=? and revogada_em is not null",Integer.class,id));
    }
    @Autowired @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
    org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping mappings;
    @Test void matrizCobreTodosOsMappingsFinanceirosReaisSemLiberarRotasFuturas() throws Exception {
        java.util.Set<String> esperado = java.util.Set.of(
                "GET /carteiras","GET /carteiras/{id}","GET /carteiras/{carteiraId}/operacoes","GET /carteiras/{carteiraId}/operacoes/sugestao-preco-venda",
                "GET /carteiras/{carteiraId}/posicoes","GET /carteiras/{carteiraId}/resultados-realizados","GET /carteiras/{carteiraId}/patrimonio",
                "GET /carteiras/{carteiraId}/resumo","GET /carteiras/{carteiraId}/evolucao-patrimonial","GET /carteiras/{carteiraId}/posicoes/distribuicao-corretoras",
                "GET /carteiras/{carteiraId}/movimentacoes-custodia","GET /carteiras/{carteiraId}/movimentacoes-custodia/{movimentacaoId}",
                "GET /acoes","GET /acoes/{id}","GET /acoes/por-ticker","GET /corretoras","GET /corretoras/{id}","GET /corretoras/por-cnpj",
                "GET /operacoes","GET /operacoes/{id}","GET /operacoes/previa-compra",
                "POST /carteiras","POST /acoes","POST /corretoras","POST /operacoes","POST /carteiras/{carteiraId}/snapshots",
                "POST /carteiras/{carteiraId}/atualizacoes-cotacoes","POST /carteiras/{carteiraId}/movimentacoes-custodia",
                "POST /carteiras/{carteiraId}/movimentacoes-custodia/{movimentacaoId}/estornos",
                "PATCH /carteiras/{id}","PATCH /acoes/{id}/cotacao","DELETE /carteiras/{id}");
        java.util.Set<String> reais = new java.util.HashSet<>();
        mappings.getHandlerMethods().forEach((mapping,handler) -> {
            if (handler.getBeanType().getPackageName().equals("com.projeto.resources")
                    && !handler.getBeanType().equals(com.projeto.resources.AutenticacaoResource.class)
                    && !handler.getBeanType().equals(com.projeto.resources.UsuarioResource.class))
                for (var method : mapping.getMethodsCondition().getMethods()) for (String path : mapping.getPatternValues()) {
                    reais.add(method+" "+path);
                    if (!method.name().equals("GET")) {
                        var protecao = handler.getMethodAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
                        assertNotNull(protecao,handler.toString()); assertTrue(protecao.value().contains("ADMIN"));
                    }
                }
        });
        assertEquals(esperado,reais,"Reconciliar matriz D6 quando surgir/remover mapping real");
        Cookie admin = autenticar(Papel.ADMIN), user = autenticar(Papel.USER), csrf = csrf();
        for (String contrato : reais) {
            String[] partes=contrato.split(" ",2);
            var method=org.springframework.http.HttpMethod.valueOf(partes[0]);
            String path=partes[1].replaceAll("\\{[^}]+}","999999999");
            var anon = http.perform(request(method,path).secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse();
            assertEquals(401,anon.getStatus(),contrato+" anônimo");
            var leitura = http.perform(request(method,path).secure(true).cookie(user,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse();
            if (!partes[0].equals("GET")) assertEquals(403,leitura.getStatus(),contrato+" USER");
            else assertTrue(leitura.getStatus()!=401 && leitura.getStatus()!=403 && leitura.getStatus()<500,contrato+" USER consulta");
            var escrita = http.perform(request(method,path).secure(true).cookie(admin,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse();
            assertTrue(escrita.getStatus()!=401 && escrita.getStatus()!=403 && escrita.getStatus()<500,contrato+" ADMIN");
        }
        http.perform(get("/acoes/999/futura").secure(true).cookie(admin)).andExpect(status().isForbidden());
        http.perform(get("/usuarios").secure(true).cookie(user)).andExpect(status().isForbidden());
    }
    @org.junit.jupiter.api.BeforeEach void limparLimites() { new JdbcTemplate(dataSource).update("delete from contador_abuso"); }
    @Test void cinquentaFalhasPorIpNaoPodemSerContornadasPorForwardedForOuUsername() throws Exception {
        Cookie csrf=csrf();
        for(int i=0;i<50;i++)
            http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                    .header("X-Forwarded-For","192.0.2."+i).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"inexistente."+i+"\",\"senha\":\""+UUID.randomUUID()+"\"}"))
                    .andExpect(status().isUnauthorized());
        http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .header("X-Forwarded-For","198.51.100.1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"outra.identidade\",\"senha\":\""+UUID.randomUUID()+"\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }
    @Test void contasInvalidasTemMesmoContratoHttpSemSegredos() throws Exception {
        Cookie csrf=csrf();
        for(int caso=0;caso<4;caso++) {
            String nome="condicao."+UUID.randomUUID(), senha=UUID.randomUUID().toString();
            if(caso!=0) {
                var u=new Usuario(nome,Papel.USER,OffsetDateTime.now(java.time.ZoneOffset.UTC));
                if(caso!=1) u.definirCredencialInicial(encoder.encode(senha),u.getCriadaEm());
                if(caso==2) u.setAtivo(false);
                usuarios.saveAndFlush(u);
            }
            String enviada=caso==3?UUID.randomUUID().toString():senha;
            var erro=http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+nome+"\",\"senha\":\""+enviada+"\"}"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTENTICACAO_NECESSARIA"))
                    .andExpect(jsonPath("$.message").value("Credenciais inválidas."))
                    .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store"))).andReturn().getResponse();
            assertFalse(erro.getContentAsString().contains(nome)); assertFalse(erro.getContentAsString().contains(enviada));
            assertNull(erro.getCookie("GESTAOACOES_AUTH"));
        }
    }
    @Test void httpPublicadoNaoAceitaAutenticacaoNemConfiaEmForwardedForjado() throws Exception {
        http.perform(get("/auth/csrf").secure(false).header("X-Forwarded-Proto","https"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("HTTPS_OBRIGATORIO"))
                .andExpect(cookie().doesNotExist("XSRF-TOKEN"));
    }
    @Test void cincoFalhasPersistidasLimitamLoginSemBloquearConta() throws Exception {
        Cookie csrf = csrf();
        for (int i=0; i<5; i++) {
            http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"nao.existe\",\"senha\":\""+UUID.randomUUID()+"\"}"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Credenciais inválidas."));
        }
        http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"nao.existe\",\"senha\":\""+UUID.randomUUID()+"\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }
    @Test void logoutRevogaCookieCompartilhadoSemDependerDoNavegador() throws Exception {
        Cookie auth = autenticar(Papel.ADMIN), csrf = csrf();
        http.perform(post("/auth/logout").secure(true).cookie(auth,csrf).header("X-XSRF-TOKEN",csrf.getValue()))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge("GESTAOACOES_AUTH",0));
        http.perform(get("/auth/me").secure(true).cookie(auth)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTENTICACAO_NECESSARIA"));
    }
    @Test void userConsultaEAdminPodeCriarCarteiraComCsrf() throws Exception {
        Cookie user = autenticar(Papel.USER);
        http.perform(get("/carteiras").secure(true).cookie(user)).andExpect(status().isOk());
        Cookie csrf = csrf();
        http.perform(post("/carteiras").secure(true).cookie(user,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Carteira compartilhada\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACESSO_NEGADO"));
        Cookie admin = autenticar(Papel.ADMIN);
        csrf = csrf();
        http.perform(post("/carteiras").secure(true).cookie(admin,csrf).header("X-XSRF-TOKEN",csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Carteira compartilhada\"}"))
                .andExpect(status().isCreated());
        http.perform(get("/carteiras").secure(true)).andExpect(status().isUnauthorized());
    }
    Cookie csrf() throws Exception {
        return http.perform(get("/auth/csrf").secure(true)).andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
    Cookie autenticar(Papel papel) throws Exception {
        String senha = UUID.randomUUID().toString(), username = "fixture."+UUID.randomUUID();
        var u = new Usuario(username, papel, OffsetDateTime.now(java.time.ZoneOffset.UTC));
        u.definirCredencialInicial(encoder.encode(senha),u.getCriadaEm()); usuarios.saveAndFlush(u);
        Cookie csrf = csrf();
        return http.perform(post("/auth/login").secure(true).cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+username+"\",\"senha\":\""+senha+"\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("GESTAOACOES_AUTH");
    }
    @Test void loginEmiteCookieProtegidoEReloadRestauraIdentidade() throws Exception {
        String senha = UUID.randomUUID().toString();
        String username = "aluno."+UUID.randomUUID();
        var usuario = new Usuario(username, Papel.USER, OffsetDateTime.now(java.time.ZoneOffset.UTC));
        usuario.definirCredencialInicial(encoder.encode(senha), usuario.getCriadaEm());
        usuarios.saveAndFlush(usuario);
        Cookie csrf = http.perform(get("/auth/csrf").secure(true)).andReturn().getResponse().getCookie("XSRF-TOKEN");
        var resposta = http.perform(post("/auth/login").secure(true).cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\""+username+"\",\"senha\":\""+senha+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.papel").value("USER")).andReturn().getResponse();
        Cookie auth = resposta.getCookie("GESTAOACOES_AUTH");
        assertNotNull(auth); assertTrue(auth.isHttpOnly()); assertTrue(auth.getSecure());
        assertEquals("/", auth.getPath()); assertEquals("Lax", auth.getAttribute("SameSite"));
        assertNull(auth.getDomain()); assertTrue(auth.getMaxAge() > 0 && auth.getMaxAge() <= 14400);
        assertFalse(resposta.getContentAsString().contains(auth.getValue()));
        assertFalse(resposta.getContentAsString().contains("senha"));
        assertNull(resposta.getCookie("JSESSIONID"));
        var reload = http.perform(get("/auth/me").secure(true).cookie(auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username))
                .andReturn().getResponse();
        assertNull(reload.getCookie("GESTAOACOES_AUTH"));
    }
    @Test void handshakePublicoEntregaSomenteCsrfSemSessaoHttp() throws Exception {
        var resposta = http.perform(get("/auth/csrf").secure(true))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse();
        var cookie = resposta.getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        assertFalse(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("/", cookie.getPath());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
        assertNull(resposta.getCookie("JSESSIONID"));
        assertNull(resposta.getCookie("GESTAOACOES_AUTH"));
    }
}
