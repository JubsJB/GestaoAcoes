package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.projeto.entities.Mercado;
import com.projeto.entities.SituacaoMovimentacaoCustodia;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(requiredProperties={"id","tipoRegistro","natureza","situacao","carteiraId","acaoId","ticker","mercado",
        "corretoraOrigemId","corretoraDestinoId","quantidade","dataEfetiva","dataRegistro","posicaoCronologica",
        "movimentacaoOriginalId","estornoId","motivo"},
        description="Registro de controle interno; identidade discriminada por tipoRegistro e id, sem comprovar custódia externa")
public record RegistroCustodiaResponse(Long id, TipoRegistro tipoRegistro, Natureza natureza,
        @Schema(implementation=SituacaoSchema.class) SituacaoMovimentacaoCustodia situacao, Long carteiraId, Long acaoId, String ticker,
        Mercado mercado, @Schema(nullable=true) Long corretoraOrigemId, Long corretoraDestinoId, BigDecimal quantidade,
        LocalDate dataEfetiva, OffsetDateTime dataRegistro, PosicaoCustodiaResponse posicaoCronologica,
        @Schema(nullable=true) Long movimentacaoOriginalId, @Schema(nullable=true) Long estornoId,
        @Schema(nullable=true) String motivo) {
    public enum TipoRegistro { MOVIMENTACAO, ESTORNO }
    public enum Natureza { TRANSFERENCIA, ATRIBUICAO, ESTORNO }
    /** OpenAPI 3.1: null precisa ser alternativa ao enum, não somente ao tipo string. */
    @Schema(name="SituacaoRegistroCustodia",anyOf={SituacaoMovimentacaoCustodia.class,NuloSchema.class})
    public static final class SituacaoSchema { private SituacaoSchema() { } }
    @Schema(name="NuloCustodia",types={"null"})
    public static final class NuloSchema { private NuloSchema() { } }
}
