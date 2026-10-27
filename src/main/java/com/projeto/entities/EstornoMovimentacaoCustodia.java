package com.projeto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Vínculo de auditoria imutável; não executa neutralização nem garante unicidade no banco. */
@Entity
@Table(name = "estorno_movimentacao_custodia", uniqueConstraints = @UniqueConstraint(
        name = "uk_estorno_movimentacao_custodia_original", columnNames = "movimentacao_original_id"))
public class EstornoMovimentacaoCustodia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimentacao_original_id", nullable = false, updatable = false)
    private MovimentacaoCustodia movimentacaoOriginal;

    @Column(name = "data_registro", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE", updatable = false)
    private OffsetDateTime dataRegistro;

    @Column(name = "motivo", length = 1000, updatable = false)
    private String motivo;

    protected EstornoMovimentacaoCustodia() {
    }

    @PostLoad
    private void aposCarregamento() {
        validarCamposHistoricosLocais();
    }

    private void validarCamposHistoricosLocais() {
        MovimentacaoCustodia.validarId(id, "id");
        if (movimentacaoOriginal == null || dataRegistro == null) {
            throw new IllegalArgumentException("Original e data de registro históricas são obrigatórias");
        }
        if (motivo != null && motivo.length() > 1000) {
            throw new IllegalArgumentException("Motivo deve possuir no máximo 1000 caracteres");
        }
    }

    public void validarEstruturaHistorica() {
        validarCamposHistoricosLocais();
        MovimentacaoCustodia.exigirAssociacaoCarregada(movimentacaoOriginal, "movimentacaoOriginal");
        movimentacaoOriginal.validarEstruturaHistorica();
    }

    public EstornoMovimentacaoCustodia(Long id, MovimentacaoCustodia movimentacaoOriginal,
            String motivo, Clock clock) {
        if (id != null) MovimentacaoCustodia.validarId(id, "id");
        if (movimentacaoOriginal == null || clock == null) {
            throw new IllegalArgumentException("Movimentação original e relógio são obrigatórios");
        }
        if (motivo != null && motivo.length() > 1000) {
            throw new IllegalArgumentException("Motivo deve possuir no máximo 1000 caracteres");
        }
        this.id = id;
        this.movimentacaoOriginal = movimentacaoOriginal;
        this.dataRegistro = OffsetDateTime.now(clock);
        this.motivo = motivo == null || motivo.isBlank() ? null : motivo;
    }

    public Long getId() { return id; }
    public MovimentacaoCustodia getMovimentacaoOriginal() { return movimentacaoOriginal; }
    public Long getMovimentacaoOriginalId() { return movimentacaoOriginal.getId(); }
    public LocalDate getDataEfetiva() { return movimentacaoOriginal.getDataEfetiva(); }
    public Integer getOrdemFinanceiraAncora() { return movimentacaoOriginal.getOrdemFinanceiraAncora(); }
    public PosicaoRacionalCustodia getPosicaoRacional() { return movimentacaoOriginal.getPosicaoRacional(); }
    public OffsetDateTime getDataRegistro() { return dataRegistro; }
    public String getMotivo() { return motivo; }
}
