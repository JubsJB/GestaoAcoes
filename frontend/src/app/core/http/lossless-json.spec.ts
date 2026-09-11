import { describe, expect, it } from 'vitest';

import { parseLosslessJson } from './lossless-json';

describe('parseLosslessJson', () => {
  const fields = new Set(['value']);

  it.each(['0', '40.000000', '48.200000', '48.123400', '0.000001', '9999999999999.123456', '-50.123456', '123456789012345678.123456'])(
    'preserva exatamente o token decimal %s',
    (token) => expect(parseLosslessJson<{ value: string }>(`{"value":${token}}`, fields).value).toBe(token)
  );

  it('preserva valores em objetos aninhados e coleções sem converter IDs', () => {
    const value = parseLosslessJson<{ id: number; rows: { value: string }[] }>(
      '{"id":7,"rows":[{"value":40.000000},{"value":-0.000001}]}', fields
    );
    expect(value).toEqual({ id: 7, rows: [{ value: '40.000000' }, { value: '-0.000001' }] });
  });

  it.each(['', '   ', '{"value":}', '{"value":--1}', '{"value":1'])(
    'falha com resposta vazia ou malformada: %s',
    (text) => expect(() => parseLosslessJson(text, fields)).toThrow()
  );
});
