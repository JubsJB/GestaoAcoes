package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(requiredProperties = {"acaoId", "ticker", "mercado", "moeda", "quantidadeConsolidada", "quantidadeAtribuidaTotal", "reconciliada", "diagnosticoReconciliacao", "corretoras", "diagnosticosCorretoras"})
public record PosicaoDistribuicaoResponse(Long acaoId, String ticker, Mercado mercado, Moeda moeda,
        BigDecimal quantidadeConsolidada, BigDecimal quantidadeAtribuidaTotal, boolean reconciliada,
        @Schema(nullable = true) DiagnosticoReconciliacaoResponse diagnosticoReconciliacao,
        List<QuantidadeCorretoraResponse> corretoras, List<DiagnosticoCorretoraResponse> diagnosticosCorretoras) {
    public PosicaoDistribuicaoResponse {
        corretoras = List.copyOf(corretoras);
        diagnosticosCorretoras = List.copyOf(diagnosticosCorretoras);
    }
}
