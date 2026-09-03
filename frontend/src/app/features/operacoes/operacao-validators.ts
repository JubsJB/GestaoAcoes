import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Mercado } from '../acoes/models/acao';

const DECIMAL = /^\d+(?:[.,]\d+)?$/;
const CIVIL_DATE = /^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])$/;

export function normalizeDecimal(value: string): string | null {
  const text = value.trim();
  return DECIMAL.test(text) ? text.replace(',', '.') : null;
}

export function formatEditableDecimal(value: string): string | null {
  const normalized = normalizeDecimal(value);
  if (!normalized) return null;
  const [integer, fraction = ''] = normalized.split('.');
  const significantFraction = fraction.replace(/0+$/, '');
  return `${integer},${significantFraction.padEnd(2, '0')}`;
}

export function decimalError(value: string, maxPrecision = 19, maxScale = 6): ValidationErrors | null {
  const normalized = normalizeDecimal(value);
  if (!normalized) return { decimal: true };
  const [integer, fraction = ''] = normalized.split('.');
  const significantInteger = integer.replace(/^0+(?=\d)/, '');
  const maxInteger = maxPrecision - maxScale;
  if (significantInteger.length > maxInteger) return { integerDigits: { max: maxInteger } };
  if (fraction.length > maxScale) return { scale: { max: maxScale } };
  if (/^0+$/.test(integer) && (fraction === '' || /^0+$/.test(fraction))) return { positive: true };
  return null;
}

export function positiveDecimalValidator(): ValidatorFn {
  return (control: AbstractControl<string>) => control.value.trim() ? decimalError(control.value) : null;
}

export function quantityValidator(market: () => Mercado | null): ValidatorFn {
  return (control: AbstractControl<string>) => {
    if (!control.value.trim()) return null;
    const error = decimalError(control.value);
    if (error) return error;
    const normalized = normalizeDecimal(control.value)!;
    const fraction = normalized.split('.')[1] ?? '';
    return market() === 'BRASIL' && /[1-9]/.test(fraction) ? { brazilianInteger: true } : null;
  };
}

export function currentCivilDate(market: Mercado, now = new Date()): string {
  const timeZone = market === 'BRASIL' ? 'America/Sao_Paulo' : 'America/New_York';
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(now);
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((item) => item.type === type)?.value ?? '';
  return `${part('year')}-${part('month')}-${part('day')}`;
}

export function civilDateValidator(market: () => Mercado | null, now: () => Date = () => new Date()): ValidatorFn {
  return (control: AbstractControl<string>) => {
    const value = control.value.trim();
    if (!value) return null;
    if (!isCivilDate(value)) return { civilDate: true };
    const selectedMarket = market();
    return selectedMarket && value > currentCivilDate(selectedMarket, now()) ? { futureDate: true } : null;
  };
}

function isCivilDate(value: string): boolean {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return false;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  return month >= 1 && month <= 12 && day >= 1 && day <= days[month - 1];
}

export function formatCivilDate(value: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  return match ? `${match[3]}/${match[2]}/${match[1]}` : value;
}

export function formatDecimal(value: string): string {
  const [integer, fraction] = value.split('.');
  const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return fraction === undefined ? grouped : `${grouped},${fraction}`;
}

export function formatOperationQuantity(value: string, market: Mercado): string {
  const [integer, fraction] = value.split('.');
  if (market === 'BRASIL' && (!fraction || /^0+$/.test(fraction))) return formatDecimal(integer);
  return formatDecimal(value);
}

export function formatMoney(value: string, currency: 'BRL' | 'USD'): string {
  const match = /^(-?)(\d+)(?:\.(\d+))?$/.exec(value);
  if (!match) return `${value} ${currency}`;
  const sign = match[1];
  const integer = match[2];
  const fraction = match[3] ?? '';
  const cents = (fraction + '00').slice(0, 2);
  let minorUnits = BigInt(integer + cents);
  if ((fraction[2] ?? '0') >= '5') minorUnits += 1n;
  const padded = minorUnits.toString().padStart(3, '0');
  const whole = padded.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  const decimal = padded.slice(-2);
  const symbol = currency === 'BRL' ? 'R$' : 'US$';
  return `${sign}${symbol} ${whole},${decimal}`;
}

export function multiplyDecimals(left: string, right: string): string | null {
  const normalizedLeft = normalizeDecimal(left);
  const normalizedRight = normalizeDecimal(right);
  if (!normalizedLeft || !normalizedRight) return null;
  const [leftInteger, leftFraction = ''] = normalizedLeft.split('.');
  const [rightInteger, rightFraction = ''] = normalizedRight.split('.');
  const scale = leftFraction.length + rightFraction.length;
  const product = BigInt(leftInteger + leftFraction) * BigInt(rightInteger + rightFraction);
  const digits = product.toString().padStart(scale + 1, '0');
  if (scale === 0) return digits;
  return `${digits.slice(0, -scale)}.${digits.slice(-scale)}`;
}
