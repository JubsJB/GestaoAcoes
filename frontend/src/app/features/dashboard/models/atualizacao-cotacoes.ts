import { Mercado } from '../../acoes/models/acao';
import { CamposTemporaisCotacao } from '../../../shared/models/cotacao-temporal';

export type { ReferenciaMercadoCotacaoResponse } from '../../../shared/models/cotacao-temporal';

/** Preço decimal textual após parsing lossless, na moeda do mercado. */
export interface EstadoCotacaoResponse extends CamposTemporaisCotacao {
  cotacaoAtual: string;
}

export interface ErroAtualizacaoCotacaoResponse {
  code: string;
  message: string;
}

interface ItemAtualizacaoCotacao {
  acaoId: number;
  ticker: string;
  mercado: Mercado;
  motivo: string;
  cotacao: EstadoCotacaoResponse;
}

export type ResultadoAtualizacaoCotacaoResponse = ItemAtualizacaoCotacao & (
  | { resultado: 'ATUALIZADA' | 'SEM_NOVA_COTACAO'; erro: null }
  | { resultado: 'FALHA'; erro: ErroAtualizacaoCotacaoResponse }
);

export interface ResumoAtualizacaoCotacoesResponse {
  atualizadas: number;
  semNovaCotacao: number;
  falhas: number;
}

export interface AtualizacaoCotacoesCarteiraResponse {
  carteiraId: number;
  resumo: ResumoAtualizacaoCotacoesResponse;
  resultados: ResultadoAtualizacaoCotacaoResponse[];
}
