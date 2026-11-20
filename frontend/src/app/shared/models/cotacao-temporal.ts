/** Valores ISO recebidos da API; DATA civil não representa um horário. */
export type ReferenciaMercadoCotacaoResponse =
  | { tipo: 'DATA'; data: string; instante: null }
  | { tipo: 'INSTANTE'; data: null; instante: string }
  | { tipo: 'DESCONHECIDA'; data: null; instante: null };

export interface CamposTemporaisCotacao {
  referenciaMercadoCotacao: ReferenciaMercadoCotacaoResponse;
  cotacaoObtidaEm: string | null;
  /** Compatibilidade legada; nunca substitui referência ou obtenção. */
  dataHoraCotacao: string;
}

export function isReferenciaMercadoCotacao(value: unknown): value is ReferenciaMercadoCotacaoResponse {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return false;
  const ref = value as Record<string, unknown>;
  switch (ref['tipo']) {
    case 'DATA': return isDataCivil(ref['data']) && ref['instante'] === null;
    case 'INSTANTE': return isInstanteTextual(ref['instante']) && ref['data'] === null;
    case 'DESCONHECIDA': return ref['data'] === null && ref['instante'] === null;
    default: return false;
  }
}

export function isCamposTemporaisCotacao(value: Record<string, unknown>): boolean {
  return isReferenciaMercadoCotacao(value['referenciaMercadoCotacao'])
    && (value['cotacaoObtidaEm'] === null || isInstanteTextual(value['cotacaoObtidaEm']))
    && isInstanteTextual(value['dataHoraCotacao']);
}

function isDataCivil(value: unknown): value is string {
  return typeof value === 'string' && /^[+-]?\d{4,}-\d{2}-\d{2}$/.test(value);
}

function isInstanteTextual(value: unknown): value is string {
  return typeof value === 'string'
    && /^[+-]?\d{4,}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d{1,9})?)?(?:Z|[+-]\d{2}:\d{2}(?::\d{2})?)$/.test(value);
}

/** Limites de aquisição não são aplicados retroativamente aos DTOs persistidos. */
export function isNovaAquisicaoCotacao(value: Record<string, unknown>): boolean {
  if (!isCamposTemporaisCotacao(value)) return false;
  const obtained = parseInstanteMicros(value['cotacaoObtidaEm']);
  if (obtained === null) return false;
  const ref = value['referenciaMercadoCotacao'] as ReferenciaMercadoCotacaoResponse;
  switch (ref.tipo) {
    case 'DATA': return isDataCivilAquisicao(ref.data);
    case 'DESCONHECIDA': return true;
    case 'INSTANTE': {
      const instant = parseInstanteMicros(ref.instante);
      return instant !== null && instant >= 0n && instant <= obtained;
    }
  }
}

export function isDataCivilAquisicao(value: unknown): value is string {
  if (typeof value !== 'string') return false;
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  return match !== null && Number(match[1]) >= 1
    && validCalendar(Number(match[1]), Number(match[2]), Number(match[3]));
}

function validCalendar(year: number, month: number, day: number): boolean {
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  return month >= 1 && month <= 12 && day >= 1 && day <= days[month - 1];
}

/** Gregorian/offset arithmetic in integers; no Date, clock or precision loss. */
export function parseInstanteMicros(value: unknown): bigint | null {
  if (typeof value !== 'string') return null;
  const m = /^(\d{4}|\+\d{5,9}|-\d{4,9})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?(Z|([+-])(\d{2}):(\d{2})(?::(\d{2}))?)$/.exec(value);
  if (!m) return null;
  const year = Number(m[1]);
  if (!Number.isSafeInteger(year) || m[1] === '-0000' || Math.abs(year) > 999999999
    || !validCalendar(year, Number(m[2]), Number(m[3]))) return null;
  const hour = Number(m[4]), minute = Number(m[5]), second = Number(m[6] ?? 0);
  const oh = Number(m[10] ?? 0), om = Number(m[11] ?? 0), os = Number(m[12] ?? 0);
  const fraction = m[7] ?? '';
  if (hour > 23 || minute > 59 || second > 59 || oh > 18 || om > 59 || os > 59
    || (oh === 18 && (om !== 0 || os !== 0)) || /[1-9]/.test(fraction.slice(6))) return null;
  const y = year - (Number(m[2]) <= 2 ? 1 : 0);
  const era = Math.floor(y / 400), yo = y - era * 400;
  const month = Number(m[2]) + (Number(m[2]) > 2 ? -3 : 9);
  const days = era * 146097 + yo * 365 + Math.floor(yo / 4) - Math.floor(yo / 100)
    + Math.floor((153 * month + 2) / 5) + Number(m[3]) - 1 - 719468;
  const offset = (m[9] === '-' ? -1 : 1) * (oh * 3600 + om * 60 + os);
  return (BigInt(days) * 86400n + BigInt(hour * 3600 + minute * 60 + second - offset)) * 1000000n
    + BigInt(fraction.slice(0, 6).padEnd(6, '0'));
}
