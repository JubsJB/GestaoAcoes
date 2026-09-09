import { describe, expect, it } from 'vitest';
import { projectFinancialValues } from './position-chart-geometry';

describe('position chart geometry (coordinates only)', () => {
  it('uses one linear scale, preserves order and compares decimal/scientific equality', () => {
    const values = Object.freeze(['80', '100.00', '120', '1e2', '0']);
    const projected = projectFinancialValues(values);
    expect(projected.map(item => item.authoritativeValue)).toEqual(values);
    [80 / 120, 100 / 120, 1, 100 / 120, 0].forEach((expected, index) => {
      expect(projected[index].ratio).toBeCloseTo(expected, 14);
    });
    expect(projected[1].ratio).toBe(projected[3].ratio);
  });

  it('retains exact sign with symmetric magnitudes and negative zero', () => {
    const projected = projectFinancialValues(['-25', '25.00', '-0e999999', '0.000']);
    expect(projected.map(item => [item.sign, item.ratio])).toEqual([[-1, 1], [1, 1], [0, 0], [0, 0]]);
  });

  it('handles empty and all-zero domains without an invented maximum', () => {
    expect(projectFinancialValues([])).toEqual([]);
    expect(projectFinancialValues(['0', '-0.000', '0e400']).map(item => item.ratio)).toEqual([0, 0, 0]);
  });

  it('normalizes overflow/underflow without expanding powers or losing nonzero values', () => {
    const values = Object.freeze(['1e400', '5e399', '1e-400', '-1e400']);
    const projected = projectFinancialValues(values);
    expect(projected.map(item => item.authoritativeValue)).toEqual(values);
    expect(projected.map(item => item.ratio)).toEqual([1, .5, 0, 1]);
    expect(projected[2].sign).toBe(1);
    expect(projectFinancialValues(['1e-400', '5e-401']).map(item => item.ratio)).toEqual([1, .5]);
    expect(projectFinancialValues(['1e999999999999999999999', '1']).map(item => item.ratio)).toEqual([1, 0]);
  });

  it('does not invent a minimum width for subpixel values', () => {
    const [small, large] = projectFinancialValues(['0.00001', '1000']);
    expect(small.ratio).toBeCloseTo(1e-8, 15);
    expect(small.ratio! * 1000).toBeLessThan(.001);
    expect(large.ratio).toBe(1);
  });

  it('keeps differences beyond binary precision in the authoritative source', () => {
    const values = Object.freeze(['9007199254740993.01', '9007199254740993.02']);
    const projected = projectFinancialValues(values);
    expect(projected.map(item => item.authoritativeValue)).toEqual(values);
    expect(projected.every(item => Number.isFinite(item.ratio) && item.ratio! > .99 && item.ratio! <= 1)).toBe(true);
    expect(values).toEqual(['9007199254740993.01', '9007199254740993.02']);
  });

  it('compares magnitude across leading/trailing zeros and fractional exponents', () => {
    expect(projectFinancialValues(['0.00120', '12e-4', '0.00012']).map(item => item.ratio)).toEqual([1, 1, .1]);
  });

  it('keeps invalid data distinguishable from zero without NaN or Infinity', () => {
    const projected = projectFinancialValues(['Infinity', 'NaN', '', '1.2.3', '0', '1']);
    expect(projected.slice(0, 4)).toEqual(['Infinity', 'NaN', '', '1.2.3'].map(authoritativeValue => ({ authoritativeValue, sign: null, ratio: null })));
    expect(projected.slice(4).map(item => item.ratio)).toEqual([0, 1]);
  });
});
