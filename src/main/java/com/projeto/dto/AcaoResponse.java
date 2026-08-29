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
        @Schema(description = "Instante da cotação em formato ISO 8601", example = "2026-08-29T12:00:00Z", format = "date-time")
        OffsetDateTime dataHoraCotacao
) {
}
