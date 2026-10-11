import { Mercado } from '../../acoes/models/acao';
import { Currency } from '../../../shared/formatters/financial-value.formatter';
import { CamposTemporaisCotacao } from '../../../shared/models/cotacao-temporal';

export interface ResumoMoedaResponse {
  moeda: Currency;
  custoTotalPosicoes: string;
  patrimonioAtual: string;
  resultadoNaoRealizadoTotal: string;
  rentabilidadePercentual: string;
}

export interface ResumoCarteiraResponse {
  carteiraId: number;
  resumos: ResumoMoedaResponse[];
}

export interface PosicaoResponse extends CamposTemporaisCotacao {
  acaoId: number;
  ticker: string;
  nomeEmpresa: string;
  mercado: Mercado;
  moeda: Currency;
  quantidadeAtual: string;
  precoMedio: string;
  custoPosicao: string;
  cotacaoAtual: string;
  valorAtualPosicao: string;
  resultadoNaoRealizado: string;
  rentabilidadePercentual: string;
}

export interface ResultadoRealizadoResponse {
  acaoId: number;
  ticker: string;
  nomeEmpresa: string;
  mercado: Mercado;
  moeda: Currency;
  resultadoRealizado: string;
}

export interface DashboardFinancialData {
  resumo: ResumoCarteiraResponse;
  posicoes: PosicaoResponse[];
  resultados: ResultadoRealizadoResponse[];
}
