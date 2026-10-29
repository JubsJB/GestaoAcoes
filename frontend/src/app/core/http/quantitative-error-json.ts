import { codigoQuantitativo } from '../errors/quantitative-errors';
import { parseLosslessJson } from './lossless-json';

const DECIMAIS = new Set(['quantidade', 'precoUnitario', 'precoUnitarioSugerido', 'valorTotal',
  'quantidadeDisponivel', 'quantidadeSolicitada', 'saldoAntes', 'saldoDepois', 'deficitAntes', 'deficitDepois', 'saldoAposEvento']);
const IDS = new Set(['id', 'carteiraId', 'acaoId', 'corretoraId', 'movimentacaoOriginalId', 'estornoId', 'ordemNoDia', 'ordemFinanceiraAncora']);

/** Exclusivo de respostas de erro; não muda o parsing dos DTOs financeiros de sucesso. */
export function parseErroQuantitativo(text: string): unknown {
  const body = parseLosslessJson<unknown>(text, DECIMAIS);
  if (!objeto(body) || !codigoQuantitativo(body['code'])) return body;
  const exato = parseLosslessJson<unknown>(text, new Set([...DECIMAIS, ...IDS]));
  if (objeto(exato)) restaurar(exato['details']);
  return exato;
}
function objeto(v: unknown): v is Record<string, unknown> { return v !== null && typeof v === 'object' && !Array.isArray(v); }
function restaurar(v: unknown): void {
  if (Array.isArray(v)) { v.forEach(restaurar); return; }
  if (!objeto(v)) return;
  for (const [key, value] of Object.entries(v)) {
    if (IDS.has(key) && value !== null) {
      if (typeof value !== 'string' || !/^(?:0|[1-9]\d*)$/.test(value)) throw new Error('Identidade quantitativa inválida.');
      const numero = BigInt(value), ordem = key === 'ordemNoDia' || key === 'ordemFinanceiraAncora';
      if (numero < (key === 'ordemFinanceiraAncora' ? 0n : 1n)
          || numero > (ordem ? 2147483647n : BigInt(Number.MAX_SAFE_INTEGER))) throw new Error('Identidade quantitativa não representável.');
      v[key] = Number(value);
    } else restaurar(value);
  }
}
