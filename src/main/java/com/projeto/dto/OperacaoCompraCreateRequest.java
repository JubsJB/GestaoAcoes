package com.projeto.dto;
import com.projeto.entities.Mercado;
import com.projeto.entities.TipoOperacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "OperacaoCompraCreateRequest",
        description = "COMPRA: o backend consulta o fechamento histórico bruto da data exata.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public final class OperacaoCompraCreateRequest extends OperacaoCreateRequest {
    public OperacaoCompraCreateRequest() {}
    public OperacaoCompraCreateRequest(Long carteiraId, String ticker, Mercado mercado, Long corretoraId,
                                       BigDecimal quantidade, LocalDate dataOperacao) {
        super(carteiraId, ticker, mercado, corretoraId, TipoOperacao.COMPRA, quantidade, dataOperacao);
    }
    @AssertTrue(message = "Tipo deve ser COMPRA")
    @Schema(hidden = true)
    public boolean isTipoCompra() { return getTipo() == TipoOperacao.COMPRA; }
}
