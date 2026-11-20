package com.projeto.config;

import com.projeto.entities.Mercado;
import com.projeto.integrations.cep.CepData;
import com.projeto.integrations.cnpj.CnpjData;
import com.projeto.integrations.cotacao.CotacaoData;
import com.projeto.services.exceptions.ApiException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;

public final class CatalogoDemonstracao {
    public record Acao(String ticker, String nome, Mercado mercado, String moeda, BigDecimal preco) {}
    public record Corretora(String cnpj, String nome, String cep, String logradouro) {}

    public List<Acao> acoes() {
        return List.of(new Acao("DEMA3", "Demo Aurora", Mercado.BRASIL, "BRL", new BigDecimal("24.00")),
                new Acao("DEMB4", "Demo Horizonte", Mercado.BRASIL, "BRL", new BigDecimal("27.00")),
                new Acao("ZDMO", "Demo Orbit", Mercado.EUA, "USD", new BigDecimal("120.00")));
    }

    public List<Corretora> corretoras() {
        return List.of(new Corretora("99999999000191", "Corretora Demo Norte", "99999001", "Rua Fictícia Norte"),
                new Corretora("99999998000147", "Corretora Demo Sul", "99999002", "Rua Fictícia Sul"));
    }

    CotacaoData cotacao(Mercado mercado, String ticker) {
        var acao = acoes().stream().filter(a -> a.mercado() == mercado && a.ticker().equals(ticker))
                .findFirst().orElseThrow(() -> desconhecido("TICKER_INEXISTENTE"));
        return new CotacaoData(ticker, acao.nome(), acao.moeda(), acao.preco(), null, false);
    }

    CnpjData cnpj(String cnpj) {
        var corretora = corretoras().stream().filter(c -> c.cnpj().equals(cnpj)).findFirst()
                .orElseThrow(() -> desconhecido("CNPJ_INEXISTENTE"));
        return new CnpjData(cnpj, corretora.nome() + " (fictícia)", corretora.nome(), null,
                null, corretora.cep(), "1", null, "ATIVA");
    }

    CepData cep(String cep) {
        var corretora = corretoras().stream().filter(c -> c.cep().equals(cep)).findFirst()
                .orElseThrow(() -> desconhecido("CEP_INEXISTENTE"));
        return new CepData(cep, corretora.logradouro(), "Bairro Fictício", "São Paulo", "SP");
    }

    private ApiException desconhecido(String codigo) {
        return new ApiException(HttpStatus.NOT_FOUND, codigo, "Identificador ausente no catálogo fictício.");
    }
}
