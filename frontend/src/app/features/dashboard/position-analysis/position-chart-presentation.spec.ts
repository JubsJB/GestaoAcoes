import { describe, expect, it } from 'vitest';
import { exactPresentation, moneyPresentation } from './position-chart-presentation';
import { projectFinancialValues } from './position-chart-geometry';

describe('lossless chart presentation', () => {
  it.each(['0', '-0.00', '0E-12', '0.000000', '0e9999'])('presents zero %s without raw supplements', value => {
    const projection = projectFinancialValues([value])[0];
    expect(moneyPresentation(projection, 'USD')).toMatchObject({ formatted: 'US$ 0,00', complete: false, authoritativeValue: value });
  });
  it.each([
    ['12.12345678901234567890', '12,12345678901234567890'],
    ['-1e-12', '-0,000000000001'],
    ['9007199254740993.01', '9007199254740993,01'],
    ['1e4000', '1 × 10^4000'],
    ['-1.2345e-4000', '-1,2345 × 10^-4000']
  ])('retains exact textual digits for %s', (value, exact) => {
    expect(exactPresentation(value).exact).toBe(exact);
  });
});
