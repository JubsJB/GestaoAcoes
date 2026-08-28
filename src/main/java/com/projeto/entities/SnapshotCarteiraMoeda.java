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
import java.math.RoundingMode;

@Entity
@Table(
        name = "snapshot_carteira_moeda",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_snapshot_carteira_moeda_snapshot_moeda",
                columnNames = {"snapshot_carteira_id", "moeda"}
        )
)
public class SnapshotCarteiraMoeda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_carteira_id", nullable = false)
    private SnapshotCarteira snapshotCarteira;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moeda moeda;

    @Column(name = "patrimonio_atual", nullable = false, precision = 38, scale = 12)
    private BigDecimal patrimonioAtual;

    protected SnapshotCarteiraMoeda() {
    }

    public SnapshotCarteiraMoeda(
            SnapshotCarteira snapshotCarteira,
            Moeda moeda,
            BigDecimal patrimonioAtual
    ) {
        if (snapshotCarteira == null) {
            throw new IllegalArgumentException("O snapshot da Carteira deve ser informado");
        }
        if (moeda == null) {
            throw new IllegalArgumentException("A moeda deve ser informada");
        }
        if (patrimonioAtual == null || patrimonioAtual.signum() <= 0) {
            throw new IllegalArgumentException("O patrimônio atual deve ser maior que zero");
        }
        BigDecimal valorExato;
        try {
            valorExato = patrimonioAtual.setScale(12, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "O patrimônio atual deve ser exatamente representável em NUMERIC(38,12)",
                    exception
            );
        }
        if (valorExato.precision() > 38) {
            throw new IllegalArgumentException(
                    "O patrimônio atual deve ser exatamente representável em NUMERIC(38,12)"
            );
        }
        this.snapshotCarteira = snapshotCarteira;
        this.moeda = moeda;
        this.patrimonioAtual = valorExato;
    }

    public Long getId() {
        return id;
    }

    public SnapshotCarteira getSnapshotCarteira() {
        return snapshotCarteira;
    }

    public Moeda getMoeda() {
        return moeda;
    }

    public BigDecimal getPatrimonioAtual() {
        return patrimonioAtual;
    }
}
