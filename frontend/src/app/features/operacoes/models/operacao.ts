import { Mercado, Moeda } from '../../acoes/models/acao';

export type TipoOperacao = 'COMPRA' | 'VENDA';

export interface OperacaoCreateRequestBase {
  carteiraId: number;
  ticker: string;
  mercado: Mercado;
  corretoraId?: number | null;
  quantidade: string;
  dataOperacao: string;
}

export interface OperacaoCompraCreateRequest extends OperacaoCreateRequestBase {
  tipo: 'COMPRA';
}

export interface OperacaoVendaCreateRequest extends OperacaoCreateRequestBase {
  tipo: 'VENDA';
  precoUnitario: string;
}

export type OperacaoCreateRequest =
  | OperacaoCompraCreateRequest
  | OperacaoVendaCreateRequest;

export interface PreviaPrecoCompraResponse {
  ticker: string;
  mercado: Mercado;
  moeda: Moeda;
  dataCotacao: string;
  precoUnitario: string;
}

export interface SugestaoPrecoVendaResponse {
  precoUnitarioSugerido: string | null;
}

export interface OperacaoResponse extends OperacaoCreateRequestBase {
  id: number;
  tipo: TipoOperacao;
  precoUnitario: string;
  ordemNoDia: number;
  valorTotal: string;
  moeda?: Moeda;
}
