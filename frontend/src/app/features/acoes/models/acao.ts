import { CamposTemporaisCotacao } from '../../../shared/models/cotacao-temporal';

export type Mercado = 'BRASIL' | 'EUA';
export type Moeda = 'BRL' | 'USD';

export interface AcaoCreateRequest {
  ticker: string;
  mercado: Mercado;
}

export interface AcaoResponse extends CamposTemporaisCotacao {
  id: number;
  ticker: string;
  nomeEmpresa: string;
  mercado: Mercado;
  moeda: Moeda;
  cotacaoAtual: number;
}

export function normalizeTicker(value: string): string {
  return value.trim().toUpperCase();
}
