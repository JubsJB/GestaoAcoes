package com.projeto.autenticacao;

import com.projeto.entities.autenticacao.Papel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RedefinicaoSenhaHttpTest extends RedefinicaoHttpSupport {
    @Test void codigoNaoVazaNosLogsQuandoJsonEhMalformado() throws Exception {
        var alvo=conta(Papel.USER);var auth=login(admin);var codigo=codigo(auth,alvo,0);var senha=java.util.UUID.randomUUID().toString();
        var root=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var mvc=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("org.springframework.web.servlet.mvc.method.annotation");
        var nivel=mvc.getLevel();var captura=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();captura.start();root.addAppender(captura);mvc.setLevel(ch.qos.logback.classic.Level.DEBUG);
        try {
            var resposta=postJson("/auth/redefinicoes/confirmar","{\"username\":\""+alvo.nome()+"\",\"codigo\":X"+codigo+"}").andExpect(status().isBadRequest()).andReturn().getResponse();
            assertFalse(resposta.getContentAsString().contains(codigo));
            postJson("/auth/redefinicoes/confirmar",mapper.writeValueAsString(java.util.Map.of("username",alvo.nome(),"codigo",codigo,"senha",java.util.Map.of("campo",senha),"confirmacao",senha)))
                    .andExpect(status().isBadRequest());
            for(var evento:captura.list) {assertFalse(evento.getFormattedMessage().contains(codigo));assertFalse(evento.getFormattedMessage().contains(senha));}
            assertEquals(5,autorizacoes.findAll().get(0).getTentativasRestantes());
            assertEquals(2,jdbc().queryForObject("select tentativas from contador_abuso where tipo='REDEFINICAO_IP'",Integer.class));
        } finally {mvc.setLevel(nivel);root.detachAppender(captura);captura.stop();}
    }
    @Test void codigoESenhaNaoAparecemEmListagensOuLogsMesmoComMvcDebug() throws Exception {
        var alvo=conta(Papel.USER);var auth=login(admin);var senha=java.util.UUID.randomUUID().toString();
        var root=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var mvc=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("org.springframework.web.servlet.mvc.method.annotation");
        var nivel=mvc.getLevel();var captura=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();captura.start();root.addAppender(captura);mvc.setLevel(ch.qos.logback.classic.Level.DEBUG);
        try {
            String codigo=codigo(auth,alvo,0);
            for(String path:java.util.List.of("/usuarios","/usuarios/"+alvo.id())) {
                String corpo=http.perform(get(path).secure(true).cookie(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                assertFalse(corpo.contains(codigo));assertFalse(corpo.contains("digest"));assertFalse(corpo.contains("senhaHash"));
            }
            for(var valor:jdbc().queryForMap("select * from autorizacao_redefinicao_senha").values()) assertFalse(String.valueOf(valor).contains(codigo));
            confirmar(alvo.nome(),codigo,senha,senha).andExpect(status().isNoContent());
            assertFalse(captura.list.isEmpty());
            for(var evento:captura.list) {assertFalse(evento.getFormattedMessage().contains(codigo));assertFalse(evento.getFormattedMessage().contains(senha));}
        } finally {mvc.setLevel(nivel);root.detachAppender(captura);captura.stop();}
    }
    @Test void falhasPublicasNaoRevelamExistenciaEstadoOuMotivoDaRecusa() throws Exception {
        var ativo=conta(Papel.USER);var bloqueado=conta(Papel.USER);var expirado=conta(Papel.USER);var auth=login(admin);
        String a=codigo(auth,ativo,0),b=codigo(auth,bloqueado,0),c=codigo(auth,expirado,0),senha=java.util.UUID.randomUUID().toString();
        situacao(auth,bloqueado.id(),false,1).andExpect(status().isOk());
        String mensagem=null;
        for(var par:java.util.List.of(new String[]{ativo.nome(),"incorreto"},new String[]{"ausente."+java.util.UUID.randomUUID(),a},new String[]{bloqueado.nome(),b},new String[]{"!",a})) {
            var resposta=confirmar(par[0],par[1],senha,senha).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REDEFINICAO_INVALIDA")).andReturn().getResponse();
            var atual=mapper.readTree(resposta.getContentAsString()).get("message").asText();if(mensagem==null) mensagem=atual;else assertEquals(mensagem,atual);
        }
        confirmar(ativo.nome(),a,senha,senha).andExpect(status().isNoContent());
        var reuso=confirmar(ativo.nome(),a,senha,senha).andExpect(status().isBadRequest()).andReturn().getResponse();
        assertEquals(mensagem,mapper.readTree(reuso.getContentAsString()).get("message").asText());
        quando(inicio.plusSeconds(900));
        var expiracao=confirmar(expirado.nome(),c,senha,senha).andExpect(status().isBadRequest()).andReturn().getResponse();
        assertEquals(mensagem,mapper.readTree(expiracao.getContentAsString()).get("message").asText());assertEquals(4,usuarios.count());
    }
    @Test void cinquentaJsonsMalformadosContamPorIpERetentativaLiberaNaBorda() throws Exception {
        for(int i=0;i<50;i++) postJson("/auth/redefinicoes/confirmar","{").andExpect(status().isBadRequest());
        postJson("/auth/redefinicoes/confirmar","{").andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","900"));
        quando(inicio.plusSeconds(900));
        postJson("/auth/redefinicoes/confirmar","{").andExpect(status().isBadRequest());
        assertEquals(1,usuarios.count());
    }
    @Test void camposDesconhecidosTambemContamNoLimiteDaIdentidade() throws Exception {
        String corpo="{\"username\":\"inexistente\",\"atorId\":1}";
        for(int i=0;i<10;i++) postJson("/auth/redefinicoes/confirmar",corpo).andExpect(status().isBadRequest());
        postJson("/auth/redefinicoes/confirmar",corpo).andExpect(status().isTooManyRequests());
    }
    @Test void corpoExcessivoContaPorIpSemSerReproduzidoNoErro() throws Exception {
        var resposta=postJson("/auth/redefinicoes/confirmar","x".repeat(17000)).andExpect(status().isBadRequest()).andReturn().getResponse();
        assertFalse(resposta.getContentAsString().contains("x".repeat(20)));
        assertEquals(1,jdbc().queryForObject("select tentativas from contador_abuso where tipo='REDEFINICAO_IP'",Integer.class));
    }
    @Test void senhaInvalidaOuDivergenteNaoConsomeTentativasDoCodigo() throws Exception {
        var alvo=conta(Papel.USER);var codigo=codigo(login(admin),alvo,0);
        for(var par:java.util.List.of(new String[]{"curta","curta"},new String[]{"senha-valida","diferente"},new String[]{"é".repeat(36)+"a","é".repeat(36)+"a"}))
            confirmar(alvo.nome(),codigo,par[0],par[1]).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SENHA_INVALIDA"));
        assertEquals(5,autorizacoes.findAll().get(0).getTentativasRestantes());
        assertNull(autorizacoes.findAll().get(0).getConsumidaEm());
        var senha="é".repeat(36);
        confirmar(alvo.nome(),codigo,senha,senha).andExpect(status().isNoContent());login(new Conta(alvo.id(),alvo.nome(),senha));
    }
    @Test void csrfRejeitadoNaoConsomeCodigoECodigoAceitaCaixaEspacosEHifens() throws Exception {
        var alvo=conta(Papel.USER);var codigo=codigo(login(admin),alvo,0);var senha=java.util.UUID.randomUUID().toString();
        String corpo=mapper.writeValueAsString(java.util.Map.of("username",alvo.nome(),"codigo",codigo,"senha",senha,"confirmacao",senha));
        http.perform(post("/auth/redefinicoes/confirmar").secure(true).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
        var csrf=csrf();
        http.perform(post("/auth/redefinicoes/confirmar").secure(true).cookie(csrf).header("X-XSRF-TOKEN","invalido").contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
        assertEquals(5,autorizacoes.findAll().get(0).getTentativasRestantes());
        assertEquals(0,jdbc().queryForObject("select count(*) from contador_abuso where tipo like 'REDEFINICAO%'",Integer.class));
        String formatado=" "+codigo.substring(0,10).toLowerCase(java.util.Locale.ROOT)+"-"+codigo.substring(10).toLowerCase(java.util.Locale.ROOT)+" ";
        confirmar(alvo.nome().toUpperCase(java.util.Locale.ROOT),formatado,senha,senha).andExpect(status().isNoContent());
    }
    @Test void codigoSubstituidoOuConsumidoNaoPodeSerReutilizado() throws Exception {
        var alvo=conta(Papel.USER);var auth=login(admin);var primeiro=codigo(auth,alvo,0);var segundo=codigo(auth,alvo,1);var senha=java.util.UUID.randomUUID().toString();
        confirmar(alvo.nome(),primeiro,senha,senha).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REDEFINICAO_INVALIDA"));
        confirmar(alvo.nome(),segundo,senha,senha).andExpect(status().isNoContent());
        confirmar(alvo.nome(),segundo,senha,senha).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REDEFINICAO_INVALIDA"));
        assertEquals(1,usuarios.findById(alvo.id()).orElseThrow().getVersaoSeguranca());
    }
    @Test void bloqueioEHabilitacaoNaoRestauramCodigoNemTrocamSenha() throws Exception {
        var alvo=conta(Papel.USER);var auth=login(admin);var codigo=codigo(auth,alvo,0);var senha=java.util.UUID.randomUUID().toString();
        situacao(auth,alvo.id(),false,1).andExpect(status().isOk());
        confirmar(alvo.nome(),codigo,senha,senha).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REDEFINICAO_INVALIDA"));
        situacao(auth,alvo.id(),true,2).andExpect(status().isOk());
        confirmar(alvo.nome(),codigo,senha,senha).andExpect(status().isBadRequest());login(alvo);
    }
    @Test void codigoNaoPodeSerTransferidoParaOutraIdentidade() throws Exception {
        var a=conta(Papel.USER);var b=conta(Papel.USER);var auth=login(admin);var codigo=codigo(auth,a,0);codigo(auth,b,0);var senha=java.util.UUID.randomUUID().toString();
        confirmar(b.nome(),codigo,senha,senha).andExpect(status().isBadRequest());
        confirmar(a.nome(),codigo,senha,senha).andExpect(status().isNoContent());login(b);
    }
    @Test void emissaoExigeAdminEProtecaoCsrf() throws Exception {
        var alvo=conta(Papel.USER);var user=login(alvo);var auth=login(admin);String path="/usuarios/"+alvo.id()+"/redefinicoes";
        postJson(path,"{\"versaoEsperada\":0}").andExpect(status().isUnauthorized());
        emitir(user,alvo.id(),0).andExpect(status().isForbidden());
        http.perform(post(path).secure(true).cookie(auth).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"versaoEsperada\":0}")).andExpect(status().isForbidden());
        assertEquals(0,autorizacoes.count());
    }
    @Test void limitaDezRequisicoesMesmoComSenhaInvalidaEIdentidadeInexistente() throws Exception {
        var username="inexistente."+java.util.UUID.randomUUID();
        for(int i=0;i<10;i++) confirmar(username,"incorreto","curta","outra").andExpect(status().isBadRequest());
        confirmar(username,"incorreto","curta","outra").andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","900"));
        assertEquals(1,usuarios.count());
    }
    @Test void cincoErrosPersistemEImpedemUsoPosteriorDoCodigoCorreto() throws Exception {
        var alvo=conta(Papel.USER);var codigo=codigo(login(admin),alvo,0);var senha=java.util.UUID.randomUUID().toString();
        for(int i=0;i<5;i++) confirmar(alvo.nome(),"codigo-incorreto",senha,senha).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REDEFINICAO_INVALIDA"));
        var autorizacao=autorizacoes.findAll().get(0);assertEquals(0,autorizacao.getTentativasRestantes());assertNotNull(autorizacao.getInvalidadaEm());
        confirmar(alvo.nome(),codigo,senha,senha).andExpect(status().isBadRequest());
        login(alvo);
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(longs={899,900})
    void expiracaoTemBordaExataDeQuinzeMinutos(long segundos) throws Exception {
        var alvo=conta(Papel.USER);var codigo=codigo(login(admin),alvo,0);var senha=java.util.UUID.randomUUID().toString();
        quando(inicio.plusSeconds(segundos));
        var resposta=confirmar(alvo.nome(),codigo,senha,senha);
        if(segundos==899) resposta.andExpect(status().isNoContent());else resposta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REDEFINICAO_INVALIDA"));
    }
    @Test void consumoPublicoTrocaSenhaERevogaTodasAsSessoesSemEmitirJwt() throws Exception {
        var alvo=conta(Papel.USER);var a=login(alvo);var b=login(alvo);var auth=login(admin);
        var codigo=codigo(auth,alvo,0);var nova=java.util.UUID.randomUUID().toString();
        var resposta=confirmar(alvo.nome(),codigo,nova,nova,new jakarta.servlet.http.Cookie("GESTAOACOES_AUTH","cookie-antigo-invalido"))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        assertNull(resposta.getCookie("GESTAOACOES_AUTH"));assertEquals("",resposta.getContentAsString());
        for(var cookie:new jakarta.servlet.http.Cookie[]{a,b}) http.perform(get("/auth/me").secure(true).cookie(cookie)).andExpect(status().isUnauthorized());
        postJson("/auth/login",mapper.writeValueAsString(java.util.Map.of("username",alvo.nome(),"senha",alvo.senha()))).andExpect(status().isUnauthorized());
        login(new Conta(alvo.id(),alvo.nome(),nova));
        assertNotNull(autorizacoes.findAll().get(0).getConsumidaEm());
        assertEquals(1,jdbc().queryForObject("select count(*) from auditoria_usuario where ator_id=? and alvo_id=? and acao='REDEFINICAO_SENHA' and resultado='SUCESSO'",Integer.class,alvo.id(),alvo.id()));
    }
    @Test void recusasAdministrativasSaoAuditadasSemMudarContaOuCodigos() throws Exception {
        var alvo=conta(Papel.USER);var auth=login(admin);
        var u=usuarios.findById(alvo.id()).orElseThrow();u.setAtivo(false);u=usuarios.saveAndFlush(u);
        emitir(auth,alvo.id(),99).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLITO_VERSAO"));
        emitir(auth,alvo.id(),u.getVersao()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USUARIO_BLOQUEADO"));
        assertEquals(2,jdbc().queryForObject("select count(*) from auditoria_usuario where acao='AUTORIZACAO_REDEFINICAO' and resultado='RECUSADO'",Integer.class));
        assertEquals(0,autorizacoes.count());
        assertEquals(u.getVersao(),usuarios.findById(alvo.id()).orElseThrow().getVersao());
    }
    @Test void segundaEmissaoSubstituiAnteriorMesmoComClockIgual() throws Exception {
        var alvo=conta(Papel.USER);var auth=login(admin);
        var primeira=emitir(auth,alvo.id(),0).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var segunda=emitir(auth,alvo.id(),1).andExpect(status().isOk()).andExpect(jsonPath("$.usuario.versao").value(2)).andReturn().getResponse().getContentAsString();
        assertNotEquals(mapper.readTree(primeira).at("/autorizacao/codigo").asText(),mapper.readTree(segunda).at("/autorizacao/codigo").asText());
        assertEquals(1,autorizacoes.findAll().stream().filter(a->a.getInvalidadaEm()==null).count());
        assertEquals(0,usuarios.findById(alvo.id()).orElseThrow().getVersaoSeguranca());
    }
    @Test void emissaoAdministrativaRetornaCodigoUmaVezSemRevogarSessao() throws Exception {
        var alvo=conta(Papel.USER);var antigo=login(alvo);var auth=login(admin);
        var r=emitir(auth,alvo.id(),0).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.usuario.versao").value(1)).andExpect(jsonPath("$.usuario.versaoSeguranca").doesNotExist())
                .andExpect(jsonPath("$.autorizacao.motivo").value("REDEFINICAO")).andExpect(jsonPath("$.autorizacao.expiraEm").value("2030-01-01T10:15:00Z"))
                .andReturn().getResponse();
        var codigo=mapper.readTree(r.getContentAsString()).at("/autorizacao/codigo").asText();
        assertTrue(codigo.matches("[A-Z2-7]{26}"));
        assertEquals(0,usuarios.findById(alvo.id()).orElseThrow().getVersaoSeguranca());
        assertNotEquals(codigo,autorizacoes.findAll().get(0).getDigest());
        http.perform(get("/auth/me").secure(true).cookie(antigo)).andExpect(status().isOk());
        http.perform(get("/usuarios/{id}",alvo.id()).secure(true).cookie(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.autorizacao").doesNotExist());
    }
}
