import { describe, expect, it } from 'vitest';
import { compositionGeometry } from './composition-geometry';

describe('composition geometry only', () => {
  it('projects proportional values without producing monetary totals or labels', () => {
    const values = Object.freeze(['100', '100', '200']);
    const result = compositionGeometry(values);
    expect(result.slices.map(s => s.sweep)).toEqual([.25, .25, .5]);
    expect(result.slices.map(s => s.start)).toEqual([0, .25, .5]);
    expect(result.slices.map(s => s.authoritativeValue)).toEqual(values);
    expect(result).not.toHaveProperty('total');
  });
  it.each([{ values: [] }, { values: ['0E-12', '-0.00'] }])('has no fictitious ring for $values', ({ values }) => {
    expect(compositionGeometry(values).state).toBe('zero');
    expect(compositionGeometry(values).slices.every(s => s.sweep === 0)).toBe(true);
  });
  it.each(['-1', 'invalid'])('invalidates the entire group for %s', value => {
    const result = compositionGeometry(['100', value]);
    expect(result.state).toBe('invalid');
    expect(result.slices.map(s => s.sweep)).toEqual([0, 0]);
  });
  it('keeps zeros and scientific underflow without a minimum arc', () => {
    const result = compositionGeometry(['1e400', '1e-400', '0E-12']);
    expect(result.slices.map(s => s.sweep)).toEqual([1, 0, 0]);
    expect(result.slices[1].small).toBe(true);
    expect(result.slices.every(s => Number.isFinite(s.x) && Number.isFinite(s.y))).toBe(true);
  });
  it('retains all 100 items with a full geometric turn', () => {
    const result = compositionGeometry(Array.from({ length: 100 }, () => '9007199254740993.01'));
    expect(result.slices).toHaveLength(100);
    expect(result.slices.reduce((sum, s) => sum + s.sweep, 0)).toBeCloseTo(1, 12);
  });
});
