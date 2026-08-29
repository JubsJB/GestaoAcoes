package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.projeto.entities.Mercado;
import com.projeto.entities.TipoOperacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OperacaoCreateRequest {

    @Schema(description = "Identificador da carteira", example = "1")
    @NotNull(message = "Carteira é obrigatória")
    private Long carteiraId;

    @Schema(description = "Ticker da ação", example = "PETR4", maxLength = 30)
    @NotBlank(message = "Ticker é obrigatório")
    private String ticker;

    @Schema(description = "Mercado da ação", example = "BRASIL", allowableValues = {"BRASIL", "EUA"})
    @NotNull(message = "Mercado é obrigatório")
    private Mercado mercado;

    @Schema(description = "Identificador opcional da corretora", example = "1", nullable = true)
    private Long corretoraId;

    @Schema(description = "Tipo da operação", example = "COMPRA", allowableValues = {"COMPRA", "VENDA"})
    @NotNull(message = "Tipo é obrigatório")
    private TipoOperacao tipo;

    @Schema(description = "Quantidade negociada, com escala máxima de 6 casas", example = "100.000000")
    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0", inclusive = false, message = "Quantidade deve ser maior que zero")
    @Digits(integer = 13, fraction = 6, message = "Quantidade deve possuir precisão máxima 19 e escala máxima 6")
    private BigDecimal quantidade;

    @Schema(description = "Preço unitário na moeda da ação, com escala máxima de 6 casas", example = "32.150000")
    @NotNull(message = "Preço unitário é obrigatório")
    @DecimalMin(value = "0", inclusive = false, message = "Preço unitário deve ser maior que zero")
    @Digits(integer = 13, fraction = 6, message = "Preço unitário deve possuir precisão máxima 19 e escala máxima 6")
    private BigDecimal precoUnitario;

    @Schema(description = "Data efetiva da operação", example = "2026-08-29", format = "date")
    @NotNull(message = "Data da operação é obrigatória")
    private LocalDate dataOperacao;

    @Schema(description = "Desempate cronológico entre operações na mesma data", example = "1", minimum = "1")
    @NotNull(message = "Ordem no dia é obrigatória")
    @Positive(message = "Ordem no dia deve ser maior que zero")
    private Integer ordemNoDia;

    public OperacaoCreateRequest() {
    }

    public OperacaoCreateRequest(
            Long carteiraId,
            String ticker,
            Mercado mercado,
            Long corretoraId,
            TipoOperacao tipo,
            BigDecimal quantidade,
            BigDecimal precoUnitario,
            LocalDate dataOperacao,
            Integer ordemNoDia
    ) {
        this.carteiraId = carteiraId;
        this.ticker = ticker;
        this.mercado = mercado;
        this.corretoraId = corretoraId;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.dataOperacao = dataOperacao;
        this.ordemNoDia = ordemNoDia;
    }

    public Long getCarteiraId() {
        return carteiraId;
    }

    public void setCarteiraId(Long carteiraId) {
        this.carteiraId = carteiraId;
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

    public Long getCorretoraId() {
        return corretoraId;
    }

    public void setCorretoraId(Long corretoraId) {
        this.corretoraId = corretoraId;
    }

    public TipoOperacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoOperacao tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public LocalDate getDataOperacao() {
        return dataOperacao;
    }

    public void setDataOperacao(LocalDate dataOperacao) {
        this.dataOperacao = dataOperacao;
    }

    public Integer getOrdemNoDia() {
        return ordemNoDia;
    }

    public void setOrdemNoDia(Integer ordemNoDia) {
        this.ordemNoDia = ordemNoDia;
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("Campo não permitido no cadastro de operação: " + property);
    }
}
