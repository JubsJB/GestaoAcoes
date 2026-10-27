package com.projeto.entities;

/**
 * Resultado explícito de consulta completa de vínculo para uma original.
 * O chamador garante o snapshot consistente; este objeto não consulta nem comprova commit.
 * Não deve ser construído a partir de uma tentativa de estorno ainda não confirmada.
 */
public final class VinculoEstornoConsultado {
    private final Long movimentacaoOriginalId;
    private final EstornoMovimentacaoCustodia estorno;

    private VinculoEstornoConsultado(Long movimentacaoOriginalId, EstornoMovimentacaoCustodia estorno) {
        MovimentacaoCustodia.validarId(movimentacaoOriginalId, "movimentacaoOriginalId");
        this.movimentacaoOriginalId = movimentacaoOriginalId;
        this.estorno = estorno;
    }

    public static VinculoEstornoConsultado ausenciaVerificada(Long movimentacaoOriginalId) {
        return new VinculoEstornoConsultado(movimentacaoOriginalId, null);
    }

    public static VinculoEstornoConsultado vinculoPresente(Long movimentacaoOriginalId,
            EstornoMovimentacaoCustodia estorno) {
        if (estorno == null) {
            throw new IllegalArgumentException("Vínculo presente exige estorno consultado");
        }
        return new VinculoEstornoConsultado(movimentacaoOriginalId, estorno);
    }

    public Long getMovimentacaoOriginalId() { return movimentacaoOriginalId; }
    public EstornoMovimentacaoCustodia getEstorno() { return estorno; }
}
