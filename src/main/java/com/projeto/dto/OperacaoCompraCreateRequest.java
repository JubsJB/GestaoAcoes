package com.projeto.dto;
import com.projeto.entities.Mercado;
import com.projeto.entities.TipoOperacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "OperacaoCompraCreateRequest",
        description = "COMPRA: o preço unitário é obrigatoriamente informado pelo cliente e nenhum provider histórico é consultado.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public final class OperacaoCompraCreateRequest extends OperacaoCreateRequest {
    @NotNull(message = "Preço unitário é obrigatório")
    @DecimalMin(value = "0", inclusive = false, message = "Preço unitário deve ser maior que zero")
    @Digits(integer = 13, fraction = 6, message = "Preço unitário deve possuir precisão máxima 19 e escala máxima 6")
    @Schema(description = "Preço unitário efetivamente informado para a COMPRA.")
    private BigDecimal precoUnitario;
    public OperacaoCompraCreateRequest() {}
    public OperacaoCompraCreateRequest(Long carteiraId, String ticker, Mercado mercado, Long corretoraId,
                                      BigDecimal quantidade, LocalDate dataOperacao, BigDecimal precoUnitario) {
        super(carteiraId, ticker, mercado, corretoraId, TipoOperacao.COMPRA, quantidade, dataOperacao);
        this.precoUnitario = precoUnitario;
    }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    @AssertTrue(message = "Tipo deve ser COMPRA")
    @Schema(hidden = true)
    public boolean isTipoCompra() { return getTipo() == TipoOperacao.COMPRA; }
}
