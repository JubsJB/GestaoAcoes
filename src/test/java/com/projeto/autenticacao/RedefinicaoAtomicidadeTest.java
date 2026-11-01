package com.projeto.autenticacao;

import com.projeto.entities.autenticacao.Papel;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RedefinicaoAtomicidadeTest extends RedefinicaoHttpSupport {
    @ParameterizedTest @ValueSource(strings={"CONSUMO_USUARIO","CONSUMO_AUTORIZACAO","CONSUMO_AUDITORIA","EMISSAO_AUDITORIA","CADASTRO_AUDITORIA"})
    void falhaDePersistenciaReverteIntegralmenteSemPerderCodigoAnterior(String ponto) throws Exception {
        var alvo=conta(Papel.USER);var antigo=login(alvo);var auth=login(admin);var codigo=codigo(auth,alvo,0);var senha=UUID.randomUUID().toString();
        String tabela=ponto.equals("CONSUMO_USUARIO")?"usuario":ponto.equals("CONSUMO_AUTORIZACAO")?"autorizacao_redefinicao_senha":"auditoria_usuario";
        String evento=tabela.equals("auditoria_usuario")?"insert":"update";
        falhar(tabela,evento);
        try {
            org.springframework.test.web.servlet.ResultActions resposta;
            if(ponto.startsWith("CONSUMO")) resposta=confirmar(alvo.nome(),codigo,senha,senha);
            else if(ponto.startsWith("EMISSAO")) resposta=emitir(auth,alvo.id(),1);
            else resposta=postJson("/usuarios",mapper.writeValueAsString(Map.of("username","novo."+UUID.randomUUID(),"papel","USER")),auth);
            var corpo=resposta.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("SEGURANCA_INDISPONIVEL")).andReturn().getResponse().getContentAsString();
            assertFalse(corpo.contains(codigo));assertFalse(corpo.contains("SQL"));assertFalse(corpo.contains(senha));
        } finally {removerFalha();}
        var usuario=usuarios.findById(alvo.id()).orElseThrow();assertEquals(1,usuario.getVersao());assertEquals(0,usuario.getVersaoSeguranca());
        var autorizacao=autorizacoes.findAll().get(0);assertNull(autorizacao.getConsumidaEm());assertNull(autorizacao.getInvalidadaEm());assertEquals(5,autorizacao.getTentativasRestantes());
        assertEquals(2,usuarios.count());assertEquals(1,autorizacoes.count());
        assertEquals(1,jdbc().queryForObject("select count(*) from auditoria_usuario",Integer.class));
        http.perform(get("/auth/me").secure(true).cookie(antigo)).andExpect(status().isOk());
        if(ponto.startsWith("CONSUMO")) assertEquals(1,jdbc().queryForObject("select tentativas from contador_abuso where tipo='REDEFINICAO_IP'",Integer.class));
        confirmar(alvo.nome(),codigo,senha,senha).andExpect(status().isNoContent());login(new Conta(alvo.id(),alvo.nome(),senha));
    }
    @Test void falhaNoContadorNegaConfirmacaoAntesDeAlterarSenhaOuCodigo() throws Exception {
        var alvo=conta(Papel.USER);var codigo=codigo(login(admin),alvo,0);var senha=UUID.randomUUID().toString();
        falhar("contador_abuso","insert");
        try {confirmar(alvo.nome(),codigo,senha,senha).andExpect(status().isServiceUnavailable());}
        finally {removerFalha();}
        assertNull(autorizacoes.findAll().get(0).getConsumidaEm());assertEquals(0,usuarios.findById(alvo.id()).orElseThrow().getVersaoSeguranca());
        confirmar(alvo.nome(),codigo,senha,senha).andExpect(status().isNoContent());
    }
    void removerFalha() { jdbc().execute("drop trigger falha_redefinicao"); }
    void falhar(String tabela,String evento) {
        jdbc().execute("create trigger falha_redefinicao before "+evento+" on "+tabela+" for each row call 'com.projeto.autenticacao.RedefinicaoAtomicidadeTest$Falha'");
    }
    public static class Falha implements org.h2.api.Trigger {
        @Override public void fire(java.sql.Connection connection,Object[] anterior,Object[] novo) throws java.sql.SQLException {
            throw new java.sql.SQLException("Falha isolada de persistência para verificar rollback","45000");
        }
    }
}
