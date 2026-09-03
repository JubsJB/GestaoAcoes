package com.projeto.services;
import com.projeto.entities.Mercado;
import com.projeto.entities.TipoOperacao;
import java.math.BigDecimal;
import java.time.LocalDate;
public record OperacaoPersistenceCommand(Long carteiraId, String ticker, Mercado mercado, Long corretoraId,
        TipoOperacao tipo, BigDecimal quantidade, BigDecimal precoUnitario, LocalDate dataOperacao) {}
