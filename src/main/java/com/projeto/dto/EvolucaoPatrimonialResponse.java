package com.projeto.dto;

import java.util.List;

public record EvolucaoPatrimonialResponse(
        Long carteiraId,
        List<EvolucaoPatrimonialPontoResponse> pontos
) {

    public EvolucaoPatrimonialResponse {
        pontos = List.copyOf(pontos);
    }
}
