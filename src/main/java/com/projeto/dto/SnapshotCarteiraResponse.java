package com.projeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

public record SnapshotCarteiraResponse(
        Long id,
        Long carteiraId,
        @Schema(description = "Instante UTC em que o snapshot foi criado", format = "date-time")
        OffsetDateTime dataHoraSnapshot,
        List<SnapshotCarteiraMoedaResponse> patrimonios
) {

    public SnapshotCarteiraResponse {
        patrimonios = List.copyOf(patrimonios);
    }
}
