import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Mercado } from '../acoes/models/acao';

const DECIMAL = /^\d+(?:[.,]\d+)?$/;
const CIVIL_DATE = /^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])$/;

export function normalizeDecimal(value: string): string | null {
  const text = value.trim();
  return DECIMAL.test(text) ? text.replace(',', '.') : null;
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
