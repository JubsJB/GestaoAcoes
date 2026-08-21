package com.projeto.dto;

import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A cotação representa o último preço disponibilizado pelo provider e não garante tempo real.
 */
public record AcaoResponse(
        Long id,
        String ticker,
        String nomeEmpresa,
        Mercado mercado,
        Moeda moeda,
        BigDecimal cotacaoAtual,
        OffsetDateTime dataHoraCotacao
) {
}
