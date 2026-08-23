package com.projeto.dto;

import com.projeto.entities.Mercado;
import com.projeto.entities.TipoOperacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OperacaoResponse(
        Long id,
        Long carteiraId,
        String ticker,
        Mercado mercado,
        Long corretoraId,
        TipoOperacao tipo,
        BigDecimal quantidade,
        BigDecimal precoUnitario,
        LocalDate dataOperacao,
        Integer ordemNoDia,
        BigDecimal valorTotal
) {
}
