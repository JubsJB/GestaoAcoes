package com.projeto.entities;

import com.projeto.persistence.ReferenciaDataJdbcType;
import org.hibernate.annotations.JdbcType;

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
import java.math.RoundingMode;
import java.time.LocalDate;
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
        // O construtor legado não comprova a origem de dataHoraCotacao.
        this(ticker, nomeEmpresa, mercado, moeda, cotacaoAtual, dataHoraCotacao,
                ReferenciaTipoCotacao.DESCONHECIDA, null, null, null);
    }

    public Acao(
            String ticker, String nomeEmpresa, Mercado mercado, Moeda moeda,
            BigDecimal cotacaoAtual, OffsetDateTime dataHoraCotacao,
            ReferenciaTipoCotacao referenciaTipo, OffsetDateTime referenciaInstante,
            LocalDate referenciaData, OffsetDateTime cotacaoObtidaEm
    ) {
        if (referenciaTipo == null) {
            throw new IllegalArgumentException("O tipo da referência deve ser informado");
        }
        referenciaTipo.validar(referenciaInstante, referenciaData);
        this.ticker = ticker;
        this.nomeEmpresa = nomeEmpresa;
        this.mercado = mercado;
        this.moeda = moeda;
        this.cotacaoAtual = cotacaoAtual;
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

    /** Aplica uma observação já aceita pela regra sobre o estado protegido. */
    public void substituirObservacao(BigDecimal preco, ReferenciaTipoCotacao tipo,
            OffsetDateTime instante, LocalDate data, OffsetDateTime obtencao) {
        if (preco == null || preco.signum() <= 0 || tipo == null
                || tipo == ReferenciaTipoCotacao.DESCONHECIDA || obtencao == null) {
            throw new IllegalArgumentException("Observação confiável completa obrigatória");
        }
        tipo.validar(instante, data);
        BigDecimal exato;
        try {
            exato = preco.setScale(6, RoundingMode.UNNECESSARY);
            if (exato.precision() > 19) throw new ArithmeticException();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("A cotação deve ser exatamente representável em NUMERIC(19,6)", exception);
        }
        cotacaoAtual = exato;
        referenciaTipo = tipo;
        referenciaInstante = utc(instante);
        referenciaData = data;
        cotacaoObtidaEm = utc(obtencao);
        dataHoraCotacao = tipo == ReferenciaTipoCotacao.INSTANTE ? referenciaInstante : cotacaoObtidaEm;
    }

    /** A elegibilidade do avanço pertence à regra; este método não redecide. */
    public void aplicarObtencao(OffsetDateTime obtencao) {
        if (obtencao == null) throw new IllegalArgumentException("Obtenção obrigatória");
        cotacaoObtidaEm = utc(obtencao);
    }

}
