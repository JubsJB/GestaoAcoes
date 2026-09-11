package com.projeto.config;
import com.projeto.entities.Mercado;
import com.projeto.integrations.cotacao.*;
import org.springframework.context.annotation.*;
import java.math.BigDecimal;

@Configuration
@Profile("test")
public class TestConfig {
    @Bean CotacaoHistoricaProvider brapiHistoricoStub() {
        return stub(Mercado.BRASIL);
    }
    @Bean CotacaoHistoricaProvider alphaHistoricoStub() {
        return stub(Mercado.EUA);
    }
    private CotacaoHistoricaProvider stub(Mercado mercado) {
        return new CotacaoHistoricaProvider() {
            public Mercado mercado() { return mercado; }
            public CotacaoHistoricaData consultarFechamento(String ticker, java.time.LocalDate data) {
                return new CotacaoHistoricaData(ticker, data, new BigDecimal("32.000000"));
            }
        };
    }
}
