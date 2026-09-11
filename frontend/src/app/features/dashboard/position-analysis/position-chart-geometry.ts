/** Coordinates only. Never use these approximations for labels, models or payloads. */
export interface FinancialProjection {
  readonly authoritativeValue: string;
  readonly sign: -1 | 0 | 1 | null;
  /** Absolute length relative to the largest magnitude; null means invalid, not zero. */
  readonly ratio: number | null;
}

interface Magnitude {
  readonly sign: -1 | 0 | 1;
  readonly digits: string;
  readonly exponent: bigint;
}

function magnitude(value: string): Magnitude | null {
  const match = /^(-?)(\d+)(?:\.(\d+))?(?:[eE]([+-]?\d+))?$/.exec(value);
  if (!match) return null;
  const [, negative, integer, fraction = '', exponent = '0'] = match;
  const digits = (integer + fraction).replace(/^0+/, '');
  if (!digits) return { sign: 0, digits: '0', exponent: 0n };
  return {
    sign: negative ? -1 : 1,
    digits: digits.replace(/0+$/, ''),
    exponent: BigInt(exponent) - BigInt(fraction.length) + BigInt(digits.length - 1)
  };
}

function larger(left: Magnitude, right: Magnitude): boolean {
  if (left.exponent !== right.exponent) return left.exponent > right.exponent;
  const length = Math.max(left.digits.length, right.digits.length);
  return left.digits.padEnd(length, '0') > right.digits.padEnd(length, '0');
}

/** Preserves input order and strings, including scientific overflow/underflow and signed zero. */
export function projectFinancialValues(values: readonly string[]): readonly FinancialProjection[] {
  const magnitudes = values.map(magnitude);
  let maximum: Magnitude | null = null;
  for (const item of magnitudes) {
    if (item && item.sign !== 0 && (!maximum || larger(item, maximum))) maximum = item;
  }
  return values.map((authoritativeValue, index) => {
    const item = magnitudes[index];
    if (!item) return { authoritativeValue, sign: null, ratio: null };
    if (item.sign === 0 || !maximum) return { authoritativeValue, sign: item.sign, ratio: 0 };
    const difference = item.exponent - maximum.exponent;
    // Only bounded significands/exponent differences enter binary arithmetic. No expansion
    // proportional to the exponent and no Number(authoritativeValue), even for 1e400.
    const ratio = difference < -324n ? 0 :
      Number(`0.${item.digits.slice(0, 16)}`) / Number(`0.${maximum.digits.slice(0, 16)}`)
      * 10 ** Number(difference);
    return { authoritativeValue, sign: item.sign, ratio: Math.min(1, Math.max(0, ratio)) };
  });
}
