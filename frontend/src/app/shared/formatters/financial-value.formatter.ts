export type Currency = 'BRL' | 'USD';

const PLAIN_DECIMAL = /^(-?)(\d+)(?:\.(\d+))?$/;
const SCIENTIFIC_DECIMAL = /^(-?)(\d+)(?:\.(\d+))?[eE]([+-]?\d+)$/;
const ZERO_DECIMAL = /^-?0+(?:\.0+)?(?:[eE][+-]?\d+)?$/;

export function formatFinancialMoney(value: string, currency: Currency): string {
  const symbol = currency === 'BRL' ? 'R$' : 'US$';
  if (isFinancialZero(value)) return `${symbol} 0,00`;
  const plainValue = expandScientificDecimal(value);
  if (plainValue === null) return `${value} ${currency}`;
  const parts = PLAIN_DECIMAL.exec(plainValue);
  if (!parts) return `${value} ${currency}`;
  const [, sign, integer, fraction = ''] = parts;
  const cents = (fraction + '00').slice(0, 2);
  let minorUnits = BigInt(integer + cents);
  if ((fraction[2] ?? '0') >= '5') minorUnits += 1n;
  const padded = minorUnits.toString().padStart(3, '0');
  const whole = padded.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${sign}${symbol} ${whole},${padded.slice(-2)}`;
}

export function formatFinancialQuantity(value: string, market: 'BRASIL' | 'EUA'): string {
  const parts = PLAIN_DECIMAL.exec(value);
  if (!parts) return value;
  const [, sign, integer, fraction = ''] = parts;
  const relevantFraction = fraction.replace(/0+$/, '');
  const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  if (market === 'BRASIL' || !relevantFraction) return `${sign}${grouped}`;
  return `${sign}${grouped},${relevantFraction}`;
}

export function formatFinancialPercent(value: string): string {
  if (isFinancialZero(value)) return '0,00%';
  const plainValue = expandScientificDecimal(value);
  if (plainValue === null) return `${value}%`;
  const parts = PLAIN_DECIMAL.exec(plainValue);
  if (!parts) return `${value}%`;
  const negative = parts[1] === '-';
  const absolute = plainValue.replace(/^-/, '');
  const formatted = formatFinancialMoney(absolute, 'BRL').replace('R$ ', '');
  return `${negative ? '-' : '+'}${formatted}%`;
}

export function financialOutcomeLabel(value: string): 'Positivo' | 'Negativo' | 'Neutro' {
  if (isFinancialZero(value)) return 'Neutro';
  return value.startsWith('-') ? 'Negativo' : 'Positivo';
}

function isFinancialZero(value: string): boolean {
  return ZERO_DECIMAL.test(value);
}

function expandScientificDecimal(value: string): string | null {
  if (PLAIN_DECIMAL.test(value)) return value;
  const match = SCIENTIFIC_DECIMAL.exec(value);
  if (!match) return null;
  const [, sign, integer, fraction = '', exponentToken] = match;
  const exponent = parseExponent(exponentToken);
  if (exponent === null) return null;
  const digits = integer + fraction;
  const decimalIndex = integer.length + exponent;
  if (decimalIndex <= 0) return `${sign}0.${'0'.repeat(-decimalIndex)}${digits}`;
  if (decimalIndex >= digits.length) return `${sign}${digits}${'0'.repeat(decimalIndex - digits.length)}`;
  return `${sign}${digits.slice(0, decimalIndex)}.${digits.slice(decimalIndex)}`;
}

function parseExponent(token: string): number | null {
  const negative = token.startsWith('-');
  const digits = token.replace(/^[+-]/, '');
  if (digits.length > 4) return null;
  let magnitude = 0;
  for (const digit of digits) magnitude = magnitude * 10 + digit.charCodeAt(0) - 48;
  if (magnitude > 1000) return null;
  return negative ? -magnitude : magnitude;
}
