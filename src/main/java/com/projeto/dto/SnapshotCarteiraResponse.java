package com.projeto.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record SnapshotCarteiraResponse(
        Long id,
        Long carteiraId,
        OffsetDateTime dataHoraSnapshot,
        List<SnapshotCarteiraMoedaResponse> patrimonios
) {

    public SnapshotCarteiraResponse {
        patrimonios = List.copyOf(patrimonios);
    }
}
