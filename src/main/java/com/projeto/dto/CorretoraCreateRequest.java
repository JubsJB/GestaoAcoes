package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class CorretoraCreateRequest {

    @Schema(description = "CNPJ com ou sem máscara", example = "12.345.678/0001-90")
    @NotBlank(message = "CNPJ é obrigatório")
    private String cnpj;

    @Schema(description = "Confirma explicitamente o cadastro quando a situação cadastral retornada não é ATIVA", example = "false")
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
