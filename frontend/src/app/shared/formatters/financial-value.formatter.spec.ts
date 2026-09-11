import { describe, expect, it } from 'vitest';

import { financialOutcomeLabel, formatFinancialMoney, formatFinancialPercent, formatFinancialQuantity } from './financial-value.formatter';

describe('financial value formatters', () => {
  it('formata BRL e USD textualmente, inclusive valores grandes e negativos', () => {
    expect(formatFinancialMoney('48.200000', 'BRL')).toBe('R$ 48,20');
    expect(formatFinancialMoney('123456789012345678.123456', 'USD')).toBe('US$ 123.456.789.012.345.678,12');
    expect(formatFinancialMoney('-50.123456', 'BRL')).toBe('-R$ 50,12');
  });

  it('formata quantidade e percentual sem coerção numérica', () => {
    expect(formatFinancialQuantity('5.000000', 'BRASIL')).toBe('5');
    expect(formatFinancialQuantity('5.250000', 'EUA')).toBe('5,25');
    expect(formatFinancialPercent('16.670000')).toBe('+16,67%');
    expect(formatFinancialPercent('-2.500000')).toBe('-2,50%');
  });

  it('classifica o sinal por texto', () => {
    expect(financialOutcomeLabel('1.00')).toBe('Positivo');
    expect(financialOutcomeLabel('-1.00')).toBe('Negativo');
    expect(financialOutcomeLabel('0.000')).toBe('Neutro');
  });

  it.each(['0', '0.0', '0.00', '0.000000', '0E-12', '0E-6', '0E+3', '-0', '-0.00', '-0E-12'])(
    'normaliza zero BigDecimal %s somente para apresentação', value => {
      expect(formatFinancialMoney(value, 'BRL')).toBe('R$ 0,00');
      expect(formatFinancialMoney(value, 'USD')).toBe('US$ 0,00');
      expect(formatFinancialPercent(value)).toBe('0,00%');
      expect(financialOutcomeLabel(value)).toBe('Neutro');
    }
  );

  it.each([
    ['0.000001', 'Positivo'], ['-0.000001', 'Negativo'], ['1E-12', 'Positivo'], ['-1E-12', 'Negativo']
  ] as const)('não confunde valor não-zero %s com zero', (value, label) => {
    expect(financialOutcomeLabel(value)).toBe(label);
    expect(formatFinancialMoney(value, 'BRL')).not.toContain(`${value} BRL`);
  });

  it('expande notação científica não-zero textualmente antes da formatação', () => {
    expect(formatFinancialMoney('1.5E+3', 'BRL')).toBe('R$ 1.500,00');
    expect(formatFinancialMoney('-2E-4', 'USD')).toBe('-US$ 0,00');
    expect(formatFinancialPercent('1E-12')).toBe('+0,00%');
  });
});
