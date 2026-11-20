import { parseLosslessJson } from '../../core/http/lossless-json';
import { DetalheCustodia, EstornoCustodia, MovimentacaoCustodia, RegistroCustodia } from './models/custodia';

const IDS = new Set(['id', 'carteiraId', 'acaoId', 'corretoraOrigemId', 'corretoraDestinoId',
  'movimentacaoOriginalId', 'estornoId', 'ordemFinanceiraAncora', 'ordemNoDia']);
const DECIMAIS = ['quantidade', 'quantidadeDisponivel', 'quantidadeSolicitada', 'saldoAntes',
  'saldoDepois', 'deficitAntes', 'deficitDepois', 'saldoAposEvento'];
const CAMPOS = ['id', 'tipoRegistro', 'natureza', 'situacao', 'carteiraId', 'acaoId', 'ticker',
  'mercado', 'corretoraOrigemId', 'corretoraDestinoId', 'quantidade', 'dataEfetiva', 'dataRegistro',
  'posicaoCronologica', 'movimentacaoOriginalId', 'estornoId', 'motivo'];

function objeto(v: unknown): v is Record<string, unknown> { return typeof v === 'object' && v !== null && !Array.isArray(v); }
function campos(v: unknown, nomes: string[]): v is Record<string, unknown> {
  return objeto(v) && Object.keys(v).length === nomes.length && nomes.every(n => Object.hasOwn(v, n));
}
export function idCustodia(v: unknown): v is number { return typeof v === 'number' && Number.isSafeInteger(v) && v > 0; }
function inteiro(v: unknown, minimo: number): boolean {
  return typeof v === 'number' && Number.isInteger(v) && v >= minimo && v <= 2147483647;
}
function texto(v: unknown): v is string { return typeof v === 'string' && !!v.trim(); }
function restaurarIds(v: unknown): void {
  if (Array.isArray(v)) { v.forEach(restaurarIds); return; }
  if (!objeto(v)) return;
  for (const [k, valor] of Object.entries(v)) {
    if (IDS.has(k) && valor !== null) {
      if (typeof valor !== 'string' || !/^(?:0|[1-9]\d*)$/.test(valor)
          || BigInt(valor) > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error('Identificador inválido.');
      // Conversão somente de identificadores previamente provados seguros; nunca de decimais/frações.
      v[k] = Number(valor);
      if (k === 'ordemFinanceiraAncora' ? !inteiro(v[k], 0)
          : k === 'ordemNoDia' ? !inteiro(v[k], 1) : !idCustodia(v[k])) throw new Error('Identificador inválido.');
    } else restaurarIds(valor);
  }
}
export function lerCustodia(text: string, resposta = true): unknown {
  const valor = parseLosslessJson<unknown>(text, new Set([...IDS, ...DECIMAIS]), resposta);
  restaurarIds(valor);
  return valor;
}
export function dataCivilCustodia(v: unknown): v is string {
  if (typeof v !== 'string') return false;
  const m = /^((?:\d{4}|-\d{4,9}|\+[1-9]\d{4,8}))-(\d{2})-(\d{2})$/.exec(v);
  if (!m || m[1].startsWith('-') && (BigInt(m[1]) === 0n || m[1].length > 5 && m[1][1] === '0')) return false;
  const ano = BigInt(m[1]), mes = Number(m[2]), dia = Number(m[3]);
  const bissexto = ano % 4n === 0n && (ano % 100n !== 0n || ano % 400n === 0n);
  return mes >= 1 && mes <= 12 && dia >= 1 && dia <= [31, bissexto ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][mes - 1];
}
function instante(v: unknown): boolean {
  if (typeof v !== 'string') return false;
  const m = /^(.*?)T(\d{2}):(\d{2}):(\d{2})(?:\.\d{1,9})?(Z|[+-](\d{2}):(\d{2})(?::(\d{2}))?)$/.exec(v);
  if (!m || !dataCivilCustodia(m[1]) || +m[2] > 23 || +m[3] > 59 || +m[4] > 59) return false;
  return m[5] === 'Z' || +m[6] <= 18 && +m[7] <= 59 && +(m[8] ?? 0) <= 59
    && (+m[6] < 18 || +m[7] === 0 && +(m[8] ?? 0) === 0);
}
export function quantidadeCustodia(v: unknown, mercado: unknown): v is string {
  if (typeof v !== 'string' || !['BRASIL', 'EUA'].includes(String(mercado))) return false;
  const m = /^(0|[1-9]\d*)(?:\.(\d+))?(?:[eE]([+-]?\d+))?$/.exec(v);
  if (!m) return false;
  const valor = decimalExato(m);
  return valor !== null && valor.escala <= (mercado === 'BRASIL' ? 0n : 6n)
    && BigInt(valor.digitos.length) - valor.escala <= 13n;
}
function decimalExato(m: RegExpExecArray): { digitos: string; escala: bigint } | null {
  let digitos = (m[1] + (m[2] ?? '')).replace(/^0+/, '');
  if (!digitos) return null;
  let escala = BigInt((m[2] ?? '').length) - BigInt(m[3] ?? '0');
  const finais = /0+$/.exec(digitos)?.[0].length ?? 0;
  if (finais) { digitos = digitos.slice(0, -finais); escala -= BigInt(finais); }
  return { digitos, escala };
}
function mesmaQuantidade(a: string, b: string): boolean {
  const expressao = /^(0|[1-9]\d*)(?:\.(\d+))?(?:[eE]([+-]?\d+))?$/;
  const x = decimalExato(expressao.exec(a)!), y = decimalExato(expressao.exec(b)!);
  return x?.digitos === y?.digitos && x?.escala === y?.escala;
}
function racional(v: unknown): boolean {
  if (!campos(v, ['ordemFinanceiraAncora', 'numerador', 'denominador']) || !inteiro(v['ordemFinanceiraAncora'], 0)
      || typeof v['numerador'] !== 'string' || typeof v['denominador'] !== 'string'
      || !/^[1-9]\d{0,511}$/.test(v['numerador']) || !/^[1-9]\d{0,511}$/.test(v['denominador'])) return false;
  let n = BigInt(v['numerador']), d = BigInt(v['denominador']);
  if (n >= d) return false;
  while (d) { const resto = n % d; n = d; d = resto; }
  return n === 1n;
}
export function isRegistroCustodia(v: unknown, carteiraId: number): v is RegistroCustodia {
  if (!idCustodia(carteiraId) || !campos(v, CAMPOS) || v['carteiraId'] !== carteiraId
      || !idCustodia(v['id']) || !idCustodia(v['acaoId']) || !texto(v['ticker'])
      || !idCustodia(v['corretoraDestinoId']) || !(v['corretoraOrigemId'] === null || idCustodia(v['corretoraOrigemId']))
      || v['corretoraOrigemId'] === v['corretoraDestinoId'] || !quantidadeCustodia(v['quantidade'], v['mercado'])
      || !dataCivilCustodia(v['dataEfetiva']) || !instante(v['dataRegistro']) || !racional(v['posicaoCronologica'])) return false;
  if (v['tipoRegistro'] === 'MOVIMENTACAO') return v['movimentacaoOriginalId'] === null && v['motivo'] === null
    && (v['natureza'] === 'TRANSFERENCIA' && idCustodia(v['corretoraOrigemId'])
      || v['natureza'] === 'ATRIBUICAO' && v['corretoraOrigemId'] === null)
    && (v['situacao'] === 'ATIVA' && v['estornoId'] === null || v['situacao'] === 'ESTORNADA' && idCustodia(v['estornoId']));
  return v['tipoRegistro'] === 'ESTORNO' && v['natureza'] === 'ESTORNO' && v['situacao'] === null
    && idCustodia(v['movimentacaoOriginalId']) && v['estornoId'] === v['id']
    && (v['motivo'] === null || typeof v['motivo'] === 'string' && v['motivo'].length <= 1000 && !!v['motivo'].trim());
}
export function vinculoCustodia(m: MovimentacaoCustodia, e: EstornoCustodia): boolean {
  return m.situacao === 'ESTORNADA' && m.estornoId === e.id && e.movimentacaoOriginalId === m.id
    && mesmaQuantidade(m.quantidade, e.quantidade)
    && ['carteiraId', 'acaoId', 'ticker', 'mercado', 'corretoraOrigemId', 'corretoraDestinoId', 'dataEfetiva']
      .every(k => m[k as keyof MovimentacaoCustodia] === e[k as keyof EstornoCustodia])
    && m.posicaoCronologica.ordemFinanceiraAncora === e.posicaoCronologica.ordemFinanceiraAncora
    && m.posicaoCronologica.numerador === e.posicaoCronologica.numerador
    && m.posicaoCronologica.denominador === e.posicaoCronologica.denominador;
}

/** Fronteira de leitura do contrato público de custódia. */
export function parseRegistroCustodia(text: string, carteiraId: number): RegistroCustodia {
  const v = lerCustodia(text);
  if (!isRegistroCustodia(v, carteiraId)) throw new Error('Resposta de custódia inválida.');
  return v;
}
export function parseHistoricoCustodia(text: string, carteiraId: number): RegistroCustodia[] {
  const v = lerCustodia(text);
  if (!idCustodia(carteiraId) || !Array.isArray(v) || !v.every(r => isRegistroCustodia(r, carteiraId))
      || new Set(v.map(r => `${r.tipoRegistro}:${r.id}`)).size !== v.length) throw new Error('Histórico de custódia inválido.');
  const originais = new Map(v.filter((r): r is MovimentacaoCustodia => r.tipoRegistro === 'MOVIMENTACAO').map(r => [r.id, r]));
  for (const e of v) if (e.tipoRegistro === 'ESTORNO') {
    const m = originais.get(e.movimentacaoOriginalId);
    if (m?.tipoRegistro === 'MOVIMENTACAO' && !vinculoCustodia(m, e)) throw new Error('Vínculo inconsistente.');
  }
  return v;
}
export function parseDetalheCustodia(text: string, carteiraId: number, movimentacaoId: number): DetalheCustodia {
  const v = lerCustodia(text);
  if (!idCustodia(movimentacaoId) || !campos(v, ['movimentacao', 'estorno'])
      || !isRegistroCustodia(v['movimentacao'], carteiraId) || v['movimentacao'].tipoRegistro !== 'MOVIMENTACAO'
      || v['movimentacao'].id !== movimentacaoId) throw new Error('Detalhe de custódia inválido.');
  const m = v['movimentacao'], e = v['estorno'];
  if (e === null ? m.situacao !== 'ATIVA'
    : !isRegistroCustodia(e, carteiraId) || e.tipoRegistro !== 'ESTORNO' || !vinculoCustodia(m, e)) throw new Error('Vínculo inconsistente.');
  return v as unknown as DetalheCustodia;
}
