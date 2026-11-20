package com.projeto.config;

import com.projeto.entities.Mercado;
import com.projeto.integrations.cep.CepProvider;
import com.projeto.integrations.cnpj.CnpjProvider;
import com.projeto.integrations.cotacao.CotacaoData;
import com.projeto.integrations.cotacao.CotacaoProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("dados-demonstracao")
class DemonstracaoProvidersConfig {
    @Bean CatalogoDemonstracao catalogoDemonstracao() { return new CatalogoDemonstracao(); }

    @Bean CnpjProvider cnpjDemonstracao(CatalogoDemonstracao catalogo) { return catalogo::cnpj; }

    @Bean CepProvider cepDemonstracao(CatalogoDemonstracao catalogo) { return catalogo::cep; }

    @Bean CotacaoProvider cotacaoBrasileiraDemonstracao(CatalogoDemonstracao catalogo) {
        return new CotacaoDemonstracao(catalogo, Mercado.BRASIL);
    }

    @Bean CotacaoProvider cotacaoAmericanaDemonstracao(CatalogoDemonstracao catalogo) {
        return new CotacaoDemonstracao(catalogo, Mercado.EUA);
    }

    private record CotacaoDemonstracao(CatalogoDemonstracao catalogo, Mercado mercado) implements CotacaoProvider {
        @Override public CotacaoData consultar(String ticker) {
            return catalogo.cotacao(mercado, ticker);
        }
    }
}
