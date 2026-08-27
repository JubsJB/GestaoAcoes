package com.projeto.dto;

import java.util.List;

public record PatrimonioResponse(
        Long carteiraId,
        List<PatrimonioMoedaResponse> patrimonios
) {
}
