package com.projeto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(requiredProperties = {"carteiraId", "posicoes"})
public record DistribuicaoCarteiraResponse(Long carteiraId, List<PosicaoDistribuicaoResponse> posicoes) {
    public DistribuicaoCarteiraResponse { posicoes = List.copyOf(posicoes); }
}
