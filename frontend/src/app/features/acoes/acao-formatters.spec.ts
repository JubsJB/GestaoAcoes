import { describe, expect, it } from 'vitest';

import { formatCotacao, formatDataHora, formatMercado } from './acao-formatters';
import { normalizeTicker } from './models/acao';

describe('Ação presentation formatters', () => {
  it('formats both markets without changing their domain values', () => {
    expect(formatMercado('BRASIL')).toBe('Brasil');
    expect(formatMercado('EUA')).toBe('EUA');
  });

  it('uses the currency supplied by the DTO for BRL and USD', () => {
    expect(formatCotacao(12.5, 'BRL')).toContain('R$');
    expect(formatCotacao(12.5, 'USD')).toContain('US$');
  });

  it('formats a valid ISO timestamp and safely preserves an invalid value', () => {
    expect(formatDataHora('2026-08-29T12:00:00Z')).not.toBe('2026-08-29T12:00:00Z');
    expect(formatDataHora('sem-data')).toBe('sem-data');
  });

  it('only trims and uppercases the ticker', () => {
    expect(normalizeTicker('  brk.b  ')).toBe('BRK.B');
    expect(normalizeTicker(' petr-4 ')).toBe('PETR-4');
  });
});
