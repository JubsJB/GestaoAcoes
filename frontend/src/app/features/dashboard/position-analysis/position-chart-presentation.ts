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
    formatted: formatFinancialMoney(value, currency),
    // Existing money formatting rounds to cents. Preserve meaningful extra digits visibly.
    complete: /[eE]/.test(value) || /[1-9]/.test((value.split('.')[1] ?? '').slice(2)),
    small: projection.sign !== null && projection.sign !== 0 && projection.ratio !== null && projection.ratio < .005
  };
}
