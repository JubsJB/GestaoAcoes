import { FormControl } from '@angular/forms';
import { civilDateValidator, currentCivilDate, decimalError, formatCivilDate, formatDecimal, formatEditableDecimal, multiplyDecimals, normalizeDecimal, quantityValidator } from './operacao-validators';

describe('operation validators', () => {
  it('normaliza vírgula e ponto sem coerção binária', () => {
    expect(normalizeDecimal(' 123,4500 ')).toBe('123.4500');
    expect(normalizeDecimal('123.4500')).toBe('123.4500');
    expect(normalizeDecimal('1x')).toBeNull();
  });

  it('formata sugestão para edição sem arredondar nem perder precisão', () => {
    expect(formatEditableDecimal('40.000000')).toBe('40,00');
    expect(formatEditableDecimal('48.200000')).toBe('48,20');
    expect(formatEditableDecimal('48.123400')).toBe('48,1234');
    expect(formatEditableDecimal('9999999999999.123456')).toBe('9999999999999,123456');
  });

  it('valida positividade, 13 inteiros e 6 decimais lexicalmente', () => {
    expect(decimalError('0')).toEqual({ positive: true });
    expect(decimalError('-1')).toEqual({ decimal: true });
    expect(decimalError('1.123456')).toBeNull();
    expect(decimalError('1.1234567')).toEqual({ scale: { max: 6 } });
    expect(decimalError('12345678901234')).toEqual({ integerDigits: { max: 13 } });
    expect(decimalError('9999999999999.123456')).toBeNull();
    expect(decimalError('00000000000001.123456')).toBeNull();
  });

  it('exige inteiro matemático no Brasil e aceita fração nos EUA', () => {
    let market: 'BRASIL' | 'EUA' = 'BRASIL';
    const control = new FormControl('2.000000', { nonNullable: true });
    expect(quantityValidator(() => market)(control)).toBeNull();
    control.setValue('2,5');
    expect(quantityValidator(() => market)(control)).toEqual({ brazilianInteger: true });
    market = 'EUA';
    expect(quantityValidator(() => market)(control)).toBeNull();
  });

  it('rejeita quantidade zero, negativa e fora dos limites', () => {
    const market = () => 'EUA' as const;
    expect(quantityValidator(market)(new FormControl('0', { nonNullable: true }))).toEqual({ positive: true });
    expect(quantityValidator(market)(new FormControl('-1', { nonNullable: true }))).toEqual({ decimal: true });
    expect(quantityValidator(market)(new FormControl('12345678901234', { nonNullable: true }))).toEqual({ integerDigits: { max: 13 } });
    expect(quantityValidator(market)(new FormControl('1.1234567', { nonNullable: true }))).toEqual({ scale: { max: 6 } });
  });

  it('calcula a data civil por mercado e não desloca YYYY-MM-DD', () => {
    const edge = new Date('2026-09-02T02:30:00Z');
    expect(currentCivilDate('BRASIL', edge)).toBe('2026-09-01');
    expect(currentCivilDate('EUA', edge)).toBe('2026-09-01');
    let market: 'BRASIL' | 'EUA' = 'BRASIL';
    const validator = civilDateValidator(() => market, () => edge);
    expect(validator(new FormControl('2026-09-02', { nonNullable: true }))).toEqual({ futureDate: true });
    expect(validator(new FormControl('2026-09-01', { nonNullable: true }))).toBeNull();
    expect(formatCivilDate('2026-09-01')).toBe('01/09/2026');
  });

  it('formata decimais longos sem arredondar ou truncar', () => {
    expect(formatDecimal('12345678901234567890123456.123456789012')).toBe('12.345.678.901.234.567.890.123.456,123456789012');
  });

  it('multiplica decimais textualmente sem coerção para number', () => {
    expect(multiplyDecimals('5', '48.200000')).toBe('241.000000');
    expect(multiplyDecimals('2', '48,20')).toBe('96.40');
    expect(multiplyDecimals('9999999999999.123456', '9999999999999.654321')).toBe('99999999999987777770000000.303002853376');
    expect(multiplyDecimals('inválido', '1')).toBeNull();
  });
});
