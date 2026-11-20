package com.projeto.dto;

import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A cotação representa o último preço disponibilizado pelo provider e não garante tempo real.
 */
public record AcaoResponse(
        Long id,
        String ticker,
        String nomeEmpresa,
        @Schema(description = "Mercado da ação", allowableValues = {"BRASIL", "EUA"})
        Mercado mercado,
        @Schema(description = "Moeda da cotação", allowableValues = {"BRL", "USD"})
        Moeda moeda,
        @Schema(description = "Última cotação persistida; não garante tempo real", example = "32.123456")
        BigDecimal cotacaoAtual,
        @Schema(description = "Campo legado de compatibilidade; a referência real está em referenciaMercadoCotacao", example = "2026-08-29T12:00:00Z", format = "date-time")
        OffsetDateTime dataHoraCotacao,
        @Schema(description = "Referência de mercado tipada {tipo, instante, data}; DESCONHECIDA mantém instante e data nulos", requiredMode = Schema.RequiredMode.REQUIRED)
        ReferenciaMercadoCotacaoResponse referenciaMercadoCotacao,
        @Schema(description = "Instante UTC de obtenção válida da observação do provider, quando conhecido; nulo no legado. Não é referência de mercado nem possui monotonicidade global entre observações", type = "string", format = "date-time", nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime cotacaoObtidaEm
) {
    /** Compatibilidade de construção de estados legados, sem inferir metadados. */
    public AcaoResponse(Long id, String ticker, String nomeEmpresa, Mercado mercado, Moeda moeda,
                        BigDecimal cotacaoAtual, OffsetDateTime dataHoraCotacao) {
        this(id, ticker, nomeEmpresa, mercado, moeda, cotacaoAtual, dataHoraCotacao,
                new ReferenciaMercadoCotacaoResponse(com.projeto.entities.ReferenciaTipoCotacao.DESCONHECIDA,
                        null, null), null);
    }
}
