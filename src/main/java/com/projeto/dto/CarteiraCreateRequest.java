package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CarteiraCreateRequest {

    @Schema(description = "Nome da carteira, normalizado com trim", example = "Carteira principal", maxLength = 255)
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome deve possuir no máximo 255 caracteres")
    private String nome;

    public CarteiraCreateRequest() {
    }

    public CarteiraCreateRequest(String nome) {
        setNome(nome);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome == null ? null : nome.trim();
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("Campo não permitido no cadastro de carteira: " + property);
    }
}
