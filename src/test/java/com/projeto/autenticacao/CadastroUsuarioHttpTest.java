package com.projeto.autenticacao;

import org.junit.jupiter.api.Test;
import com.projeto.entities.autenticacao.Papel;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CadastroUsuarioHttpTest extends RedefinicaoHttpSupport {
    @org.springframework.beans.factory.annotation.Autowired com.projeto.services.UsuarioAdministracaoService servico;
    @Test void cadastroEEmissaoExigemAdminTambemNoServicoPublico() throws Exception {
        assertThrows(org.springframework.security.core.AuthenticationException.class,()->servico.cadastrar("novo",Papel.USER));
        assertThrows(org.springframework.security.core.AuthenticationException.class,()->servico.autorizarRedefinicao(admin.id(),0L));
        var user=conta(Papel.USER);
        como(login(user),()-> {
            assertThrows(org.springframework.security.access.AccessDeniedException.class,()->servico.cadastrar("novo",Papel.ADMIN));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,()->servico.autorizarRedefinicao(admin.id(),0L));
        });
        assertEquals(2,usuarios.count());assertEquals(0,autorizacoes.count());
    }
    @Test void duplicidadeNormalizadaRetorna409SemDuplicarAutorizacao() throws Exception {
        var auth=login(admin);String username="novo."+UUID.randomUUID();
        postJson("/usuarios",mapper.writeValueAsString(Map.of("username",username,"papel","USER")),auth).andExpect(status().isCreated());
        postJson("/usuarios",mapper.writeValueAsString(Map.of("username"," "+username.toUpperCase(Locale.ROOT)+" ","papel","ADMIN")),auth)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USUARIO_DUPLICADO"));
        assertEquals(2,usuarios.count());assertEquals(1,autorizacoes.count());
        assertEquals(1,jdbc().queryForObject("select count(*) from auditoria_usuario where acao='CADASTRO' and resultado='RECUSADO'",Integer.class));
    }
    @Test void adminSemCredencialSoSeTornaOperacionalAposDefinicaoInicial() throws Exception {
        var auth=login(admin);String nome="admin."+UUID.randomUUID();
        var resposta=postJson("/usuarios",mapper.writeValueAsString(Map.of("username",nome,"papel","ADMIN")),auth).andExpect(status().isCreated()).andReturn().getResponse();
        var json=mapper.readTree(resposta.getContentAsString());long id=json.at("/usuario/id").asLong();
        assertEquals(1,usuarios.countByPapelAndAtivoTrueAndCredencialDefinidaTrue(Papel.ADMIN));
        String codigo=mapper.readTree(emitir(auth,id,1).andExpect(status().isOk()).andExpect(jsonPath("$.autorizacao.motivo").value("INICIAL"))
                .andReturn().getResponse().getContentAsString()).at("/autorizacao/codigo").asText();
        String senha=UUID.randomUUID().toString();confirmar(nome,codigo,senha,senha).andExpect(status().isNoContent());
        assertEquals(2,usuarios.countByPapelAndAtivoTrueAndCredencialDefinidaTrue(Papel.ADMIN));login(new Conta(id,nome,senha));
    }
    @Test void cadastroNaoAceitaSenhaAtorOuCamposExtrasENaoEhPublico() throws Exception {
        var user=conta(Papel.USER);var auth=login(admin);String corpo="{\"username\":\"novo\",\"papel\":\"USER\"}";
        postJson("/usuarios",corpo).andExpect(status().isUnauthorized());
        postJson("/usuarios",corpo,login(user)).andExpect(status().isForbidden());
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/usuarios").secure(true).cookie(auth).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
        for(String invalido:List.of("{}","{\"username\":\"!\",\"papel\":\"USER\"}","{\"username\":\"novo\",\"papel\":\"USER\",\"senha\":\"proibida\"}","{\"username\":\"novo\",\"papel\":\"USER\",\"atorId\":1}"))
            postJson("/usuarios",invalido,auth).andExpect(status().isBadRequest());
        assertEquals(2,usuarios.count());assertEquals(0,autorizacoes.count());
    }
    @Test void emissaoExigeVersaoENaoRecebeSenhaOuAtor() throws Exception {
        var alvo=conta(Papel.USER);var auth=login(admin);String path="/usuarios/"+alvo.id()+"/redefinicoes";
        for(String corpo:List.of("{}","{\"versaoEsperada\":-1}","{\"versaoEsperada\":0,\"senha\":\"proibida\"}","{\"versaoEsperada\":0,\"atorId\":1}"))
            postJson(path,corpo,auth).andExpect(status().isBadRequest());
        emitir(auth,99999999L,0).andExpect(status().isNotFound());assertEquals(0,autorizacoes.count());
    }
    @Test void cadastroCriaContaSemSenhaECodigoInicialAteLoginSeparado() throws Exception {
        var auth=login(admin);String username="novo."+UUID.randomUUID();
        var resposta=postJson("/usuarios",mapper.writeValueAsString(Map.of("username"," "+username.toUpperCase(Locale.ROOT)+" ","papel","USER")),auth)
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.usuario.username").value(username)).andExpect(jsonPath("$.usuario.credencialDefinida").value(false))
                .andExpect(jsonPath("$.usuario.versao").value(1)).andExpect(jsonPath("$.autorizacao.motivo").value("INICIAL")).andReturn().getResponse();
        var json=mapper.readTree(resposta.getContentAsString());long id=json.at("/usuario/id").asLong();
        assertEquals("/usuarios/"+id,resposta.getHeader("Location"));
        var senha=UUID.randomUUID().toString();
        postJson("/auth/login",mapper.writeValueAsString(Map.of("username",username,"senha",senha))).andExpect(status().isUnauthorized());
        confirmar(username,json.at("/autorizacao/codigo").asText(),senha,senha).andExpect(status().isNoContent());
        login(new Conta(id,username,senha));
        assertEquals(1,usuarios.findById(id).orElseThrow().getVersaoSeguranca());
        assertEquals(1,jdbc().queryForObject("select count(*) from auditoria_usuario where ator_id=? and alvo_id=? and acao='CADASTRO' and resultado='SUCESSO'",Integer.class,admin.id(),id));
    }
}
