package com.projeto.services;

import com.projeto.entities.Mercado;
import com.projeto.integrations.ExternalApiErrorMapper;
import com.projeto.integrations.cotacao.CotacaoHistoricaData;
import com.projeto.integrations.cotacao.CotacaoHistoricaProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FechamentoHistoricoService {

    private static final int PRECISION = 19;
    private static final int SCALE = 6;

    private final Map<Mercado, CotacaoHistoricaProvider> providers;

    public FechamentoHistoricoService(List<CotacaoHistoricaProvider> providers) {
        try {
            this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                    CotacaoHistoricaProvider::mercado,
                    Function.identity()
            ));
        } catch (IllegalStateException exception) {
            throw new IllegalStateException("Mais de um provider histórico para o mesmo mercado", exception);
        }
    }

    public BigDecimal consultar(String ticker, Mercado mercado, LocalDate data) {
        CotacaoHistoricaProvider provider = providers.get(mercado);
        if (provider == null) {
            throw ExternalApiErrorMapper.unavailable("cotação histórica");
        }

        CotacaoHistoricaData cotacao = provider.consultarFechamento(ticker, data);
        if (cotacao == null || !ticker.equals(cotacao.ticker()) || !data.equals(cotacao.dataPregao())) {
            throw ExternalApiErrorMapper.invalidResponse("cotação histórica");
        }

        return validarPreco(cotacao.close());
    }

    private BigDecimal validarPreco(BigDecimal preco) {
        if (preco == null || preco.signum() <= 0 || preco.scale() > SCALE) {
            throw ExternalApiErrorMapper.invalidResponse("cotação histórica");
        }

        try {
            BigDecimal normalizado = preco.setScale(SCALE, RoundingMode.UNNECESSARY);
            if (normalizado.precision() > PRECISION) {
                throw ExternalApiErrorMapper.invalidResponse("cotação histórica");
            }
            return normalizado;
        } catch (ArithmeticException exception) {
            throw ExternalApiErrorMapper.invalidResponse("cotação histórica");
        }
    }
}
