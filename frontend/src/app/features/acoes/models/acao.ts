export type Mercado = 'BRASIL' | 'EUA';
export type Moeda = 'BRL' | 'USD';

export interface AcaoCreateRequest {
  ticker: string;
  mercado: Mercado;
}

export interface AcaoResponse {
  id: number;
  ticker: string;
  nomeEmpresa: string;
  mercado: Mercado;
  moeda: Moeda;
  cotacaoAtual: number;
  dataHoraCotacao: string;
}

export function normalizeTicker(value: string): string {
  return value.trim().toUpperCase();
}
