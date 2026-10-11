import { parseLosslessJson } from '../../core/http/lossless-json';
import { DistribuicaoCarteiraResponse } from './models/distribuicao-corretoras';

const IDS = new Set(['carteiraId', 'acaoId', 'corretoraId', 'operacaoId', 'ordemNoDia']);
const NUMERIC_FIELDS = new Set([...IDS, 'quantidadeConsolidada', 'quantidadeAtribuidaTotal',
  'quantidadeLiquida', 'quantidadeFinal', 'saldoAposOperacao']);
const DECIMAL = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?$/;

export function parseDistribuicaoCorretoras(text: string): DistribuicaoCarteiraResponse {
  const value = parseLosslessJson<unknown>(text, NUMERIC_FIELDS, true);
  restoreIdentifiers(value);
  if (!isDistribuicaoCarteiraResponse(value)) throw new Error('Resposta de distribuicao invalida.');
  return value;
}

function restoreIdentifiers(value: unknown): void {
  if (Array.isArray(value)) { value.forEach(restoreIdentifiers); return; }
  if (!record(value)) return;
  for (const [key, item] of Object.entries(value)) {
    if (IDS.has(key) && item !== null) {
      if (typeof item !== 'string' || !/^[1-9]\d*$/.test(item)
          || BigInt(item) > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error('Identificador nao representavel.');
      value[key] = Number(item);
    } else restoreIdentifiers(item);
  }
}

export function isDistribuicaoCarteiraResponse(value: unknown): value is DistribuicaoCarteiraResponse {
  return fields(value, ['carteiraId', 'posicoes']) && id(value['carteiraId'])
    && Array.isArray(value['posicoes']) && value['posicoes'].every(position)
    && unique(value['posicoes'], 'acaoId');
}

function position(value: unknown): boolean {
  if (!fields(value, ['acaoId', 'ticker', 'mercado', 'moeda', 'quantidadeConsolidada',
    'quantidadeAtribuidaTotal', 'reconciliada', 'diagnosticoReconciliacao', 'corretoras', 'diagnosticosCorretoras'])
      || !id(value['acaoId']) || !text(value['ticker'])
      || !(value['mercado'] === 'BRASIL' && value['moeda'] === 'BRL'
        || value['mercado'] === 'EUA' && value['moeda'] === 'USD')
      || !decimal(value['quantidadeConsolidada'], 'positive') || !decimal(value['quantidadeAtribuidaTotal'])
      || typeof value['reconciliada'] !== 'boolean'
      || !Array.isArray(value['corretoras']) || !value['corretoras'].every(group)
      || !Array.isArray(value['diagnosticosCorretoras']) || !value['diagnosticosCorretoras'].every(diagnostic)
      || !unique(value['corretoras'], 'corretoraId') || !unique(value['diagnosticosCorretoras'], 'corretoraId')) return false;
  const reconciliation = value['diagnosticoReconciliacao'];
  const diagnostics = new Map(value['diagnosticosCorretoras'].map(d => [d['corretoraId'], d]));
  if (!value['corretoras'].every(g => diagnostics.get(g['corretoraId'])?.['nome'] === g['nome'])) return false;
  return value['reconciliada'] ? reconciliation === null
    : fields(reconciliation, ['codigo']) && reconciliation['codigo'] === 'DIVERGENCIA_QUANTIDADES';
}

function group(value: unknown): boolean {
  return fields(value, ['corretoraId', 'nome', 'quantidadeLiquida']) && broker(value)
    && decimal(value['quantidadeLiquida'], 'nonzero');
}

function diagnostic(value: unknown): boolean {
  if (!fields(value, ['corretoraId', 'nome', 'quantidadeFinal', 'saldoNegativoAtual',
    'possuiDeficitHistorico', 'primeiroDeficit']) || !broker(value) || !decimal(value['quantidadeFinal'])
      || typeof value['saldoNegativoAtual'] !== 'boolean' || typeof value['possuiDeficitHistorico'] !== 'boolean'
      || value['saldoNegativoAtual'] && !value['possuiDeficitHistorico']) return false;
  return value['possuiDeficitHistorico'] ? firstDeficit(value['primeiroDeficit']) : value['primeiroDeficit'] === null;
}

function firstDeficit(value: unknown): boolean {
  return fields(value, ['operacaoId', 'dataOperacao', 'ordemNoDia', 'saldoAposOperacao'])
    && id(value['operacaoId']) && id(value['ordemNoDia']) && value['ordemNoDia'] <= 2147483647
    && civilDate(value['dataOperacao']) && decimal(value['saldoAposOperacao'], 'negative');
}

function broker(value: Record<string, unknown>): boolean {
  return (value['corretoraId'] === null ? value['nome'] === 'Sem corretora informada' : id(value['corretoraId']))
    && text(value['nome']);
}

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
function fields(value: unknown, keys: string[]): value is Record<string, unknown> {
  return record(value) && Object.keys(value).length === keys.length
    && keys.every(key => Object.hasOwn(value, key));
}
function id(value: unknown): value is number { return typeof value === 'number' && Number.isSafeInteger(value) && value > 0; }
function text(value: unknown): value is string { return typeof value === 'string' && value.trim().length > 0; }
function unique(values: Record<string, unknown>[], key: string): boolean {
  return new Set(values.map(value => value[key])).size === values.length;
}
function decimal(value: unknown, sign?: 'positive' | 'negative' | 'nonzero'): value is string {
  if (typeof value !== 'string' || !DECIMAL.test(value)) return false;
  if (!sign) return true;
  const nonzero = /[1-9]/.test(value.split(/[eE]/)[0]);
  return nonzero && (sign === 'nonzero' || (sign === 'negative') === value.startsWith('-'));
}

// This is an operation LocalDate, not the restricted financial quotation DATA domain.
function civilDate(value: unknown): boolean {
  if (typeof value !== 'string') return false;
  const match = /^((?:\d{4}|-\d{4,9}|\+[1-9]\d{4,8}))-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return false;
  const year = BigInt(match[1]);
  if (match[1].startsWith('-') && (year === 0n || match[1].length > 5 && match[1][1] === '0')) return false;
  const month = Number(match[2]), day = Number(match[3]);
  const leap = year % 4n === 0n && (year % 100n !== 0n || year % 400n === 0n);
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  return month >= 1 && month <= 12 && day >= 1 && day <= days[month - 1];
}
