package com.projeto.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EvolucaoPatrimonialPontoResponse(
        Long snapshotId,
        OffsetDateTime dataHoraSnapshot,
        List<EvolucaoPatrimonialMoedaResponse> patrimonios
) {

    public EvolucaoPatrimonialPontoResponse {
        patrimonios = List.copyOf(patrimonios);
    }
}
