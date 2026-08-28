package com.projeto.dto;

import com.projeto.entities.Moeda;

import java.math.BigDecimal;

public record EvolucaoPatrimonialMoedaResponse(Moeda moeda, BigDecimal patrimonioAtual) {
}
