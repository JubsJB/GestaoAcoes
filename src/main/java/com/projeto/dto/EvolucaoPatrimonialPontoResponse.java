package com.projeto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

public record EvolucaoPatrimonialPontoResponse(
        Long snapshotId,
        @Schema(description = "Instante persistido do snapshot", format = "date-time")
        OffsetDateTime dataHoraSnapshot,
        List<EvolucaoPatrimonialMoedaResponse> patrimonios
) {

    public EvolucaoPatrimonialPontoResponse {
        patrimonios = List.copyOf(patrimonios);
    }
}
