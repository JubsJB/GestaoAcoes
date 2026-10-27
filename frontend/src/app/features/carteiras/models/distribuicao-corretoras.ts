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

interface PrimeiroDeficitBase {
  id: number;
  dataEfetiva: string;
  saldoAposEvento: string;
}

export interface PrimeiroDeficitFinanceiroResponse extends PrimeiroDeficitBase {
  dominio: 'FINANCEIRO';
  natureza: 'COMPRA' | 'VENDA';
  posicaoCronologica: { ordemNoDia: number };
}

export interface PrimeiroDeficitCustodiaResponse extends PrimeiroDeficitBase {
  dominio: 'CUSTODIA';
  natureza: 'TRANSFERENCIA' | 'ATRIBUICAO';
  posicaoCronologica: { ordemFinanceiraAncora: number; numerador: string; denominador: string };
}

export type PrimeiroDeficitResponse = PrimeiroDeficitFinanceiroResponse | PrimeiroDeficitCustodiaResponse;

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
