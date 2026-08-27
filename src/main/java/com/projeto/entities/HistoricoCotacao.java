package com.projeto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "historico_cotacao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_historico_cotacao_acao_data_hora",
                columnNames = {"acao_id", "data_hora_cotacao"}
        )
)
public class HistoricoCotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal cotacao;

    @Column(name = "data_hora_cotacao", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime dataHoraCotacao;

    protected HistoricoCotacao() {
    }

    public HistoricoCotacao(Acao acao, BigDecimal cotacao, OffsetDateTime dataHoraCotacao) {
        if (acao == null) {
            throw new IllegalArgumentException("A Ação deve ser informada");
        }
        if (cotacao == null || cotacao.signum() <= 0) {
            throw new IllegalArgumentException("A cotação deve ser maior que zero");
        }
        if (dataHoraCotacao == null) {
            throw new IllegalArgumentException("A data/hora da cotação deve ser informada");
        }

        BigDecimal cotacaoExata;
        try {
            cotacaoExata = cotacao.setScale(6, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("A cotação deve ser exatamente representável em NUMERIC(19,6)", exception);
        }
        if (cotacaoExata.precision() > 19) {
            throw new IllegalArgumentException("A cotação deve ser exatamente representável em NUMERIC(19,6)");
        }

        this.acao = acao;
        this.cotacao = cotacaoExata;
        this.dataHoraCotacao = dataHoraCotacao;
    }

    public Long getId() {
        return id;
    }

    public Acao getAcao() {
        return acao;
    }

    public BigDecimal getCotacao() {
        return cotacao;
    }

    public OffsetDateTime getDataHoraCotacao() {
        return dataHoraCotacao;
    }
}
