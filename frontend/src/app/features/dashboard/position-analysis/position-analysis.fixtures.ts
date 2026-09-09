import { PosicaoResponse } from '../models/dashboard';

// Deliberately independent fields: chart tests must not derive money from quantity/quotes.
export function positionFixture(overrides: Partial<PosicaoResponse> = {}): Readonly<PosicaoResponse> {
  return Object.freeze({
    acaoId: 9, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL', moeda: 'BRL',
    quantidadeAtual: '0.123456', precoMedio: '777.99', custoPosicao: '100.00',
    cotacaoAtual: '999.99', dataHoraCotacao: '2026-09-08T10:20:30.123456-03:00',
    valorAtualPosicao: '120.00', resultadoNaoRealizado: '-3.123456', rentabilidadePercentual: '81.000001',
    ...overrides
  });
}

export const EMPTY_POSITIONS: readonly PosicaoResponse[] = Object.freeze([]);
export const ONE_POSITION = Object.freeze([positionFixture()]);
export const THREE_POSITIONS = Object.freeze([
  positionFixture(),
  positionFixture({ acaoId: 2, ticker: 'AAPL', nomeEmpresa: 'Apple', mercado: 'EUA', moeda: 'USD', custoPosicao: '500', valorAtualPosicao: '600', resultadoNaoRealizado: '7', rentabilidadePercentual: '-1' }),
  positionFixture({ acaoId: 1, ticker: 'VALE3', nomeEmpresa: 'Vale', custoPosicao: '100', valorAtualPosicao: '80', resultadoNaoRealizado: '0', rentabilidadePercentual: '-0.00' })
]);
export const MANY_POSITIONS = Object.freeze(Array.from({ length: 100 }, (_, index) => positionFixture({
  acaoId: 100 - index, ticker: `ATIVO${100 - index}`, nomeEmpresa: `Empresa ${index} com nome extenso para leitura integral`,
  moeda: index % 2 ? 'USD' : 'BRL', mercado: index % 2 ? 'EUA' : 'BRASIL'
})));
export const EXTREME_POSITIONS = Object.freeze([
  positionFixture({ custoPosicao: '9007199254740993.01', valorAtualPosicao: '9007199254740993.02' }),
  positionFixture({ acaoId: 2, ticker: 'SMALL', custoPosicao: '0.00000000000000000001', valorAtualPosicao: '1e-400' }),
  positionFixture({ acaoId: 3, ticker: 'LARGE', custoPosicao: '1e400', valorAtualPosicao: '1e400' })
]);
