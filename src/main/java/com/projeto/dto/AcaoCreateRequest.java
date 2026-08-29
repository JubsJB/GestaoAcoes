package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.projeto.entities.Mercado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AcaoCreateRequest {

    @Schema(description = "Ticker da ação; será normalizado com trim e uppercase", example = "PETR4", maxLength = 30)
    @NotBlank(message = "Ticker é obrigatório")
    private String ticker;

    @Schema(description = "Mercado da ação", example = "BRASIL", allowableValues = {"BRASIL", "EUA"})
    @NotNull(message = "Mercado é obrigatório")
    private Mercado mercado;

    public AcaoCreateRequest() {
    }

    public AcaoCreateRequest(String ticker, Mercado mercado) {
        this.ticker = ticker;
        this.mercado = mercado;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Mercado getMercado() {
        return mercado;
    }

    public void setMercado(Mercado mercado) {
        this.mercado = mercado;
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("Campo não permitido no cadastro de ação: " + property);
    }
}
