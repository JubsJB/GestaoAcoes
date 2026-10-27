package com.projeto.services;

/** Contexto interno e motivo; dados quantitativos e auditoria pertencem à original/servidor. */
public record EstornoCustodiaCommand(Long carteiraId, Long movimentacaoOriginalId, String motivo) {
    EstornoCustodiaCommand validada() {
        ValidacaoEntradaCustodia.id(carteiraId, "carteiraId");
        ValidacaoEntradaCustodia.id(movimentacaoOriginalId, "movimentacaoOriginalId");
        if (motivo != null && motivo.length() > 1000) {
            throw ValidacaoEntradaCustodia.invalida("motivo", "Motivo deve possuir no máximo 1000 caracteres");
        }
        return new EstornoCustodiaCommand(carteiraId, movimentacaoOriginalId,
                motivo == null || motivo.isBlank() ? null : motivo);
    }
}
