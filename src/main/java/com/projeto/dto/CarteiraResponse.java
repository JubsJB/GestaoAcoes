package com.projeto.dto;

import java.time.OffsetDateTime;

public record CarteiraResponse(
        Long id,
        String nome,
        OffsetDateTime dataCriacao
) {
}
