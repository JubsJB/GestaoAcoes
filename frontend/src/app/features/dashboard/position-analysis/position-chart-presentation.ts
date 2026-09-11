import { Currency, formatFinancialMoney } from '../../../shared/formatters/financial-value.formatter';
import { PosicaoResponse } from '../models/dashboard';
import { FinancialProjection } from './position-chart-geometry';

/** Stable partition: neither the collection nor the assets within a currency are sorted. */
export function groupPositions(positions: readonly PosicaoResponse[]) {
  const groups = new Map<Currency, PosicaoResponse[]>();
  for (const position of positions) {
    const group = groups.get(position.moeda);
    if (group) group.push(position);
    else groups.set(position.moeda, [position]);
  }
  return Array.from(groups, ([currency, items]) => ({ currency, positions: items }));
}

export function moneyPresentation(projection: FinancialProjection, currency: Currency) {
  const value = projection.authoritativeValue;
  return {
    ...projection,
    formatted: /[eE][+-]?\d/.test(formatFinancialMoney(value, currency)) ? `${exactPresentation(value).exact} ${currency}` : formatFinancialMoney(value, currency),
    // Existing money formatting rounds to cents. Preserve meaningful extra digits visibly.
    ...exactPresentation(value),
    small: projection.sign !== null && projection.sign !== 0 && projection.ratio !== null && projection.ratio < .005
  };
}

/** Text-only expansion: original financial tokens and projections remain untouched. */
export function exactPresentation(value: string) {
  if (/^-?0+(?:\.0+)?(?:[eE][+-]?\d+)?$/.test(value)) return { complete: false, exact: '0,00' };
  const match = /^(-?)(\d+)(?:\.(\d+))?(?:[eE]([+-]?\d+))?$/.exec(value);
  if (!match) return { complete: false, exact: value };
  const [, sign, integer, fraction = '', exponent] = match;
  let whole = integer, decimals = fraction;
  if (exponent) {
    let shifts = BigInt(exponent);
    if (shifts > 1000n || shifts < -1000n || integer.length + fraction.length > 4096) {
      return { complete: true, exact: `${sign}${integer}${fraction ? ',' + fraction : ''} × 10^${exponent}` };
    }
    while (shifts > 0n) { whole += decimals[0] ?? '0'; decimals = decimals.slice(1); shifts--; }
    while (shifts < 0n) { decimals = (whole.slice(-1) || '0') + decimals; whole = whole.slice(0, -1); shifts++; }
  }
  return {
    complete: !!exponent || /[1-9]/.test(decimals.slice(2)),
    exact: `${sign}${whole || '0'}${decimals ? ',' + decimals : ''}`
  };
}
