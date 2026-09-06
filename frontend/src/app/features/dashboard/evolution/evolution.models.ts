import { Currency } from '../../../shared/formatters/financial-value.formatter';

export interface EvolucaoPatrimonialMoedaResponse {
  moeda: Currency;
  patrimonioAtual: string;
}

export interface EvolucaoPatrimonialPontoResponse {
  snapshotId: number;
  dataHoraSnapshot: string;
  patrimonios: EvolucaoPatrimonialMoedaResponse[];
}

export interface EvolucaoPatrimonialResponse {
  carteiraId: number;
  pontos: EvolucaoPatrimonialPontoResponse[];
}

export interface SnapshotCarteiraMoedaResponse {
  moeda: Currency;
  patrimonioAtual: string;
}

export interface SnapshotCarteiraResponse {
  id: number;
  carteiraId: number;
  dataHoraSnapshot: string;
  patrimonios: SnapshotCarteiraMoedaResponse[];
}

const DECIMAL = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?$/;

export function isEvolucaoPatrimonialResponse(value: unknown): value is EvolucaoPatrimonialResponse {
  return isRecord(value) && isSafeId(value['carteiraId']) && Array.isArray(value['pontos'])
    && value['pontos'].every(isEvolutionPoint);
}

export function isSnapshotCarteiraResponse(value: unknown): value is SnapshotCarteiraResponse {
  return isRecord(value) && isSafeId(value['id']) && isSafeId(value['carteiraId'])
    && isIsoTimestamp(value['dataHoraSnapshot']) && Array.isArray(value['patrimonios'])
    && value['patrimonios'].every(isMoneyComponent);
}

function isEvolutionPoint(value: unknown): boolean {
  return isRecord(value) && isSafeId(value['snapshotId']) && isIsoTimestamp(value['dataHoraSnapshot'])
    && Array.isArray(value['patrimonios']) && value['patrimonios'].every(isMoneyComponent);
}

function isMoneyComponent(value: unknown): boolean {
  return isRecord(value) && isCurrency(value['moeda']) && isDecimal(value['patrimonioAtual']);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isSafeId(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0;
}

function isIsoTimestamp(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0 && !Number.isNaN(Date.parse(value));
}

function isCurrency(value: unknown): value is Currency {
  return value === 'BRL' || value === 'USD';
}

function isDecimal(value: unknown): value is string {
  return typeof value === 'string' && DECIMAL.test(value);
}
