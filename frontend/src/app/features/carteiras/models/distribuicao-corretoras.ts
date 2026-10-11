import { Mercado, Moeda } from '../../acoes/models/acao';

/** Quantidades sao tokens numericos originais, preservados como strings apos parsing lossless. */
export interface DistribuicaoCarteiraResponse {
  carteiraId: number;
  posicoes: PosicaoDistribuicaoResponse[];
}

export interface PosicaoDistribuicaoResponse {
  acaoId: number;
  ticker: string;
  mercado: Mercado;
  moeda: Moeda;
  quantidadeConsolidada: string;
  quantidadeAtribuidaTotal: string;
  reconciliada: boolean;
  diagnosticoReconciliacao: DiagnosticoReconciliacaoResponse | null;
  corretoras: QuantidadeCorretoraResponse[];
  diagnosticosCorretoras: DiagnosticoCorretoraResponse[];
}

export interface QuantidadeCorretoraResponse {
  corretoraId: number | null;
  nome: string;
  quantidadeLiquida: string;
}

export interface PrimeiroDeficitResponse {
  operacaoId: number;
  dataOperacao: string;
  ordemNoDia: number;
  saldoAposOperacao: string;
}

export interface DiagnosticoCorretoraResponse {
  corretoraId: number | null;
  nome: string;
  quantidadeFinal: string;
  saldoNegativoAtual: boolean;
  possuiDeficitHistorico: boolean;
  primeiroDeficit: PrimeiroDeficitResponse | null;
}

export interface DiagnosticoReconciliacaoResponse {
  codigo: 'DIVERGENCIA_QUANTIDADES';
}
