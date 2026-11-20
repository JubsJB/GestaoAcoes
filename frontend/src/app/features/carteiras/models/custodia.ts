import { Mercado } from '../../acoes/models/acao';

export type NaturezaCustodia = 'TRANSFERENCIA' | 'ATRIBUICAO';
export type SituacaoCustodia = 'ATIVA' | 'ESTORNADA';
export interface PosicaoCustodia {
  ordemFinanceiraAncora: number;
  numerador: string;
  denominador: string;
}
interface CadastroBase {
  ticker: string;
  mercado: Mercado;
  corretoraDestinoId: number;
  quantidade: string;
  dataEfetiva: string;
}
export type CadastroCustodia = CadastroBase & (
  { natureza: 'TRANSFERENCIA'; corretoraOrigemId: number }
  | { natureza: 'ATRIBUICAO'; corretoraOrigemId?: null });
export interface EstornoCustodiaRequest { motivo?: string | null }
export interface FiltrosCustodia { natureza?: NaturezaCustodia | 'ESTORNO'; situacao?: SituacaoCustodia }
interface RegistroBase {
  id: number;
  carteiraId: number;
  acaoId: number;
  ticker: string;
  mercado: Mercado;
  corretoraOrigemId: number | null;
  corretoraDestinoId: number;
  quantidade: string;
  dataEfetiva: string;
  dataRegistro: string;
  posicaoCronologica: PosicaoCustodia;
}
export interface MovimentacaoCustodia extends RegistroBase {
  tipoRegistro: 'MOVIMENTACAO';
  natureza: NaturezaCustodia;
  situacao: SituacaoCustodia;
  movimentacaoOriginalId: null;
  estornoId: number | null;
  motivo: null;
}
export interface EstornoCustodia extends RegistroBase {
  tipoRegistro: 'ESTORNO';
  natureza: 'ESTORNO';
  situacao: null;
  movimentacaoOriginalId: number;
  estornoId: number;
  motivo: string | null;
}
export type RegistroCustodia = MovimentacaoCustodia | EstornoCustodia;
export interface DetalheCustodia { movimentacao: MovimentacaoCustodia; estorno: EstornoCustodia | null }
