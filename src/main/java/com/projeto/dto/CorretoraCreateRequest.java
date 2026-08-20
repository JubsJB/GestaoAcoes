package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;

public class CorretoraCreateRequest {

    @NotBlank(message = "CNPJ é obrigatório")
    private String cnpj;

    private Boolean confirmarSituacaoCadastralNaoAtiva;

    public CorretoraCreateRequest() {
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public Boolean getConfirmarSituacaoCadastralNaoAtiva() {
        return confirmarSituacaoCadastralNaoAtiva;
    }

    public void setConfirmarSituacaoCadastralNaoAtiva(Boolean confirmarSituacaoCadastralNaoAtiva) {
        this.confirmarSituacaoCadastralNaoAtiva = confirmarSituacaoCadastralNaoAtiva;
    }

    public boolean isConfirmacaoSituacaoCadastralNaoAtiva() {
        return Boolean.TRUE.equals(confirmarSituacaoCadastralNaoAtiva);
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("Campo não permitido no cadastro de corretora: " + property);
    }
}
