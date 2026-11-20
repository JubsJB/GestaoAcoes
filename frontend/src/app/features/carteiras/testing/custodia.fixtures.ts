export function originalCustodia(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return { id: 10, tipoRegistro: 'MOVIMENTACAO', natureza: 'TRANSFERENCIA', situacao: 'ATIVA',
    carteiraId: 1, acaoId: 2, ticker: 'PETR4', mercado: 'BRASIL', corretoraOrigemId: 3,
    corretoraDestinoId: 4, quantidade: 40, dataEfetiva: '2026-10-01',
    dataRegistro: '2026-10-24T12:00:00.123456Z',
    posicaoCronologica: { ordemFinanceiraAncora: 1, numerador: '1', denominador: '2' },
    movimentacaoOriginalId: null, estornoId: null, motivo: null, ...overrides };
}

export function estornoCustodia(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return originalCustodia({ id: 20, tipoRegistro: 'ESTORNO', natureza: 'ESTORNO', situacao: null,
    movimentacaoOriginalId: 10, estornoId: 20, motivo: ' Correção ',
    dataRegistro: '2026-10-24T13:00:00Z', ...overrides });
}
