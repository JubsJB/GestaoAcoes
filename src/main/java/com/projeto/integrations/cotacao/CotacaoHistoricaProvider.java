package com.projeto.integrations.cotacao;
import com.projeto.entities.Mercado;
import java.time.LocalDate;
public interface CotacaoHistoricaProvider {
    Mercado mercado();
    CotacaoHistoricaData consultarFechamento(String ticker, LocalDate data);
}
