package com.projeto.entities;

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
import java.time.LocalDate;

@Entity
@Table(
        name = "operacao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_operacao_carteira_acao_data_ordem",
                columnNames = {"carteira_id", "acao_id", "data_operacao", "ordem_no_dia"}
        )
)
public class Operacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carteira_id", nullable = false)
    private Carteira carteira;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corretora_id")
    private Corretora corretora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoOperacao tipo;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 6)
    private BigDecimal precoUnitario;

    @Column(name = "data_operacao", nullable = false)
    private LocalDate dataOperacao;

    @Column(name = "ordem_no_dia", nullable = false)
    private Integer ordemNoDia;

    @Column(name = "valor_total", nullable = false, precision = 38, scale = 12)
    private BigDecimal valorTotal;

    protected Operacao() {
    }

    public Operacao(
            Carteira carteira,
            Acao acao,
            Corretora corretora,
            TipoOperacao tipo,
            BigDecimal quantidade,
            BigDecimal precoUnitario,
            LocalDate dataOperacao,
            Integer ordemNoDia,
            BigDecimal valorTotal
    ) {
        this.carteira = carteira;
        this.acao = acao;
        this.corretora = corretora;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.dataOperacao = dataOperacao;
        this.ordemNoDia = ordemNoDia;
        this.valorTotal = valorTotal;
    }

    public Long getId() {
        return id;
    }

    public Carteira getCarteira() {
        return carteira;
    }

    public Acao getAcao() {
        return acao;
    }

    public Corretora getCorretora() {
        return corretora;
    }

    public TipoOperacao getTipo() {
        return tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public LocalDate getDataOperacao() {
        return dataOperacao;
    }

    public Integer getOrdemNoDia() {
        return ordemNoDia;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }
}
