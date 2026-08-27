package com.projeto.dto;

import java.util.List;

public record ResumoCarteiraResponse(
        Long carteiraId,
        List<ResumoMoedaResponse> resumos
) {
}
