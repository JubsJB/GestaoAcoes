package com.projeto.integrations.cotacao;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CotacaoHistoricaData(String ticker, LocalDate dataPregao, BigDecimal close) {}
