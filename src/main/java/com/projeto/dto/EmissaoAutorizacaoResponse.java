package com.projeto.dto;

import com.projeto.entities.autenticacao.MotivoRedefinicao;
import java.time.OffsetDateTime;

public record EmissaoAutorizacaoResponse(UsuarioResponse usuario,Autorizacao autorizacao) {
    public record Autorizacao(String codigo,MotivoRedefinicao motivo,OffsetDateTime expiraEm) {
        @Override public String toString(){return "Autorizacao[código protegido]";}
    }
}
