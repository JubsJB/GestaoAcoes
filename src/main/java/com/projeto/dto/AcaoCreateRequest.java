package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.projeto.entities.Mercado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AcaoCreateRequest {

    @NotBlank(message = "Ticker é obrigatório")
    private String ticker;

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
