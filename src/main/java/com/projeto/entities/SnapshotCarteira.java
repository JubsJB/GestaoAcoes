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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "snapshot_carteira",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_snapshot_carteira_carteira_data_hora",
                columnNames = {"carteira_id", "data_hora_snapshot"}
        )
)
public class SnapshotCarteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carteira_id", nullable = false)
    private Carteira carteira;

    @Column(name = "data_hora_snapshot", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime dataHoraSnapshot;

    protected SnapshotCarteira() {
    }

    public SnapshotCarteira(Carteira carteira, OffsetDateTime dataHoraSnapshot) {
        if (carteira == null) {
            throw new IllegalArgumentException("A Carteira deve ser informada");
        }
        if (dataHoraSnapshot == null) {
            throw new IllegalArgumentException("A data/hora do snapshot deve ser informada");
        }
        this.carteira = carteira;
        this.dataHoraSnapshot = dataHoraSnapshot.withOffsetSameInstant(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Carteira getCarteira() {
        return carteira;
    }

    public OffsetDateTime getDataHoraSnapshot() {
        return dataHoraSnapshot;
    }
}
