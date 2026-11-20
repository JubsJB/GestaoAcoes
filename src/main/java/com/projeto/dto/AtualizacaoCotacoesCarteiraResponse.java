package com.projeto.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lote concluído, inclusive sucesso parcial ou todas as falhas isoladas. Carteira sem posições abertas retorna resultados vazios e contadores zero.")
public record AtualizacaoCotacoesCarteiraResponse(
        @Schema(description = "Identificador da Carteira processada", requiredMode = Schema.RequiredMode.REQUIRED)
        Long carteiraId,
        @Schema(description = "Contadores correspondentes exatamente aos resultados retornados", requiredMode = Schema.RequiredMode.REQUIRED)
        ResumoAtualizacaoCotacoesResponse resumo,
        @Schema(description = "Uma entrada por Ação selecionada pelo backend, sem duplicidade, em ordem crescente de acaoId", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ResultadoAtualizacaoCotacaoResponse> resultados) {
    public AtualizacaoCotacoesCarteiraResponse {
        resultados = List.copyOf(resultados);
    }
}
