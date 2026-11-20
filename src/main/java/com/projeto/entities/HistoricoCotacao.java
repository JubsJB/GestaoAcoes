package com.projeto.entities;

import com.projeto.persistence.ReferenciaDataJdbcType;
import org.hibernate.annotations.JdbcType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "historico_cotacao",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_historico_cotacao_acao_instante",
                        columnNames = {"acao_id", "referencia_instante"}),
                @UniqueConstraint(name = "uk_historico_cotacao_acao_data",
                        columnNames = {"acao_id", "referencia_data"})
        }
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

    @Enumerated(EnumType.STRING)
    @Column(name = "referencia_tipo", nullable = false, length = 12)
    private ReferenciaTipoCotacao referenciaTipo = ReferenciaTipoCotacao.DESCONHECIDA;

    @Column(name = "referencia_instante", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime referenciaInstante;

    @Column(name = "referencia_data")
    @JdbcType(ReferenciaDataJdbcType.class)
    private LocalDate referenciaData;

    @Column(name = "cotacao_obtida_em", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime cotacaoObtidaEm;

    protected HistoricoCotacao() {
    }

    public HistoricoCotacao(Acao acao, BigDecimal cotacao, OffsetDateTime dataHoraCotacao) {
        this(acao, cotacao, dataHoraCotacao, ReferenciaTipoCotacao.DESCONHECIDA, null, null, null);
    }

    public HistoricoCotacao(
            Acao acao, BigDecimal cotacao, OffsetDateTime dataHoraCotacao,
            ReferenciaTipoCotacao referenciaTipo, OffsetDateTime referenciaInstante,
            LocalDate referenciaData, OffsetDateTime cotacaoObtidaEm
    ) {
        if (referenciaTipo == null) {
            throw new IllegalArgumentException("O tipo da referência deve ser informado");
        }
        referenciaTipo.validar(referenciaInstante, referenciaData);
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
        this.referenciaTipo = referenciaTipo;
        this.referenciaInstante = utc(referenciaInstante);
        this.referenciaData = referenciaData;
        this.cotacaoObtidaEm = utc(cotacaoObtidaEm);
    }

    private static OffsetDateTime utc(OffsetDateTime valor) {
        return valor == null ? null : valor.withOffsetSameInstant(ZoneOffset.UTC);
    }

    public ReferenciaTipoCotacao getReferenciaTipo() { return referenciaTipo; }

    public OffsetDateTime getReferenciaInstante() { return referenciaInstante; }

    public LocalDate getReferenciaData() { return referenciaData; }

    public OffsetDateTime getCotacaoObtidaEm() { return cotacaoObtidaEm; }

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
