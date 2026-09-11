package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.projeto.entities.Mercado;
import com.projeto.entities.TipoOperacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "tipo", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OperacaoCompraCreateRequest.class, name = "COMPRA"),
    @JsonSubTypes.Type(value = OperacaoVendaCreateRequest.class, name = "VENDA")
})
@Schema(oneOf = {OperacaoCompraCreateRequest.class, OperacaoVendaCreateRequest.class},
        discriminatorProperty = "tipo", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public abstract class OperacaoCreateRequest {
    @NotNull(message = "Carteira é obrigatória")
    private Long carteiraId;
    @NotBlank(message = "Ticker é obrigatório")
    private String ticker;
    @NotNull(message = "Mercado é obrigatório")
    private Mercado mercado;
    private Long corretoraId;
    @NotNull(message = "Tipo é obrigatório")
    private TipoOperacao tipo;
    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0", inclusive = false, message = "Quantidade deve ser maior que zero")
    @Digits(integer = 13, fraction = 6, message = "Quantidade deve possuir precisão máxima 19 e escala máxima 6")
    private BigDecimal quantidade;
    @NotNull(message = "Data da operação é obrigatória")
    private LocalDate dataOperacao;

    protected OperacaoCreateRequest() {}
    protected OperacaoCreateRequest(Long carteiraId, String ticker, Mercado mercado, Long corretoraId,
                                    TipoOperacao tipo, BigDecimal quantidade, LocalDate dataOperacao) {
        this.carteiraId = carteiraId; this.ticker = ticker; this.mercado = mercado;
        this.corretoraId = corretoraId; this.tipo = tipo; this.quantidade = quantidade;
        this.dataOperacao = dataOperacao;
    }
    public Long getCarteiraId() { return carteiraId; }
    public void setCarteiraId(Long carteiraId) { this.carteiraId = carteiraId; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public Mercado getMercado() { return mercado; }
    public void setMercado(Mercado mercado) { this.mercado = mercado; }
    public Long getCorretoraId() { return corretoraId; }
    public void setCorretoraId(Long corretoraId) { this.corretoraId = corretoraId; }
    public TipoOperacao getTipo() { return tipo; }
    public void setTipo(TipoOperacao tipo) { this.tipo = tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public LocalDate getDataOperacao() { return dataOperacao; }
    public void setDataOperacao(LocalDate dataOperacao) { this.dataOperacao = dataOperacao; }
    @JsonAnySetter
    public final void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("Campo não permitido no cadastro de operação: " + property);
    }
}
