package com.projeto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "acao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_acao_ticker_mercado",
                columnNames = {"ticker", "mercado"}
        )
)
public class Acao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String ticker;

    @Column(name = "nome_empresa", nullable = false, length = 255)
    private String nomeEmpresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Mercado mercado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moeda moeda;

    @Column(name = "cotacao_atual", nullable = false, precision = 19, scale = 6)
    private BigDecimal cotacaoAtual;

    @Column(name = "data_hora_cotacao", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime dataHoraCotacao;

    protected Acao() {
    }

    public Acao(
            String ticker,
            String nomeEmpresa,
            Mercado mercado,
            Moeda moeda,
            BigDecimal cotacaoAtual,
            OffsetDateTime dataHoraCotacao
    ) {
        this.ticker = ticker;
        this.nomeEmpresa = nomeEmpresa;
        this.mercado = mercado;
        this.moeda = moeda;
        this.cotacaoAtual = cotacaoAtual;
        this.dataHoraCotacao = dataHoraCotacao;
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public Mercado getMercado() {
        return mercado;
    }

    public Moeda getMoeda() {
        return moeda;
    }

    public BigDecimal getCotacaoAtual() {
        return cotacaoAtual;
    }

    public OffsetDateTime getDataHoraCotacao() {
        return dataHoraCotacao;
    }

    public void atualizarCotacao(BigDecimal novaCotacao, OffsetDateTime novaDataHoraCotacao) {
        if (novaCotacao == null || novaCotacao.signum() <= 0) {
            throw new IllegalArgumentException("A cotação deve ser maior que zero");
        }
        if (novaDataHoraCotacao == null) {
            throw new IllegalArgumentException("A data/hora da cotação deve ser informada");
        }

        OffsetDateTime timestampUtc = novaDataHoraCotacao.withOffsetSameInstant(ZoneOffset.UTC);
        if (dataHoraCotacao != null && !timestampUtc.isAfter(dataHoraCotacao)) {
            throw new IllegalArgumentException("A data/hora da nova cotação deve ser posterior à atual");
        }

        cotacaoAtual = novaCotacao;
        dataHoraCotacao = timestampUtc;
    }
}
