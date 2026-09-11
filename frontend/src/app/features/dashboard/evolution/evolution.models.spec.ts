import { describe, expect, it } from 'vitest';

import { parseLosslessJson } from '../../../core/http/lossless-json';
import { isEvolucaoPatrimonialResponse, isSnapshotCarteiraResponse } from './evolution.models';

const FIELDS = new Set(['patrimonioAtual']);

describe('evolution contracts', () => {
  it('preserva NUMERIC(38,12), decimal longo, escala e estrutura aninhada', () => {
    const response = parseLosslessJson<unknown>(
      '{"carteiraId":1,"pontos":[{"snapshotId":2,"dataHoraSnapshot":"2026-09-04T10:00:00Z","patrimonios":[{"moeda":"BRL","patrimonioAtual":99999999999999999999999999.123456789012}]}]}',
      FIELDS
    );
    expect(isEvolucaoPatrimonialResponse(response)).toBe(true);
    if (isEvolucaoPatrimonialResponse(response)) {
      expect(response.pontos[0].patrimonios[0].patrimonioAtual).toBe('99999999999999999999999999.123456789012');
    }
  });

  it('preserva notação científica e snapshots vazios', () => {
    const response = parseLosslessJson<unknown>(
      '{"carteiraId":1,"pontos":[{"snapshotId":2,"dataHoraSnapshot":"2026-09-04T10:00:00Z","patrimonios":[]},{"snapshotId":3,"dataHoraSnapshot":"2026-09-04T11:00:00Z","patrimonios":[{"moeda":"USD","patrimonioAtual":1.20E+3}]}]}',
      FIELDS
    );
    expect(isEvolucaoPatrimonialResponse(response)).toBe(true);
    if (isEvolucaoPatrimonialResponse(response)) expect(response.pontos[1].patrimonios[0].patrimonioAtual).toBe('1.20E+3');
  });

  it.each([
    { carteiraId: 0, pontos: [] },
    { carteiraId: 1, pontos: [{ snapshotId: 1, dataHoraSnapshot: 'inválida', patrimonios: [] }] },
    { carteiraId: 1, pontos: [{ snapshotId: 1, dataHoraSnapshot: '2026-09-04T10:00:00Z', patrimonios: [{ moeda: 'EUR', patrimonioAtual: '1.0' }] }] },
    { carteiraId: 1, pontos: [{ snapshotId: 1, dataHoraSnapshot: '2026-09-04T10:00:00Z', patrimonios: [{ moeda: 'BRL', patrimonioAtual: null }] }] },
    { carteiraId: 1, pontos: null }
  ])('rejeita contrato de evolução inválido %#', value => expect(isEvolucaoPatrimonialResponse(value)).toBe(false));

  it('valida resposta do POST e rejeita ID inseguro', () => {
    expect(isSnapshotCarteiraResponse({ id: 4, carteiraId: 1, dataHoraSnapshot: '2026-09-04T10:00:00Z', patrimonios: [] })).toBe(true);
    expect(isSnapshotCarteiraResponse({ id: Number.MAX_SAFE_INTEGER + 1, carteiraId: 1, dataHoraSnapshot: '2026-09-04T10:00:00Z', patrimonios: [] })).toBe(false);
  });

  it('rejeita JSON malformado e campo decimal inválido', () => {
    expect(() => parseLosslessJson('{', FIELDS)).toThrow();
    expect(() => parseLosslessJson('{"patrimonioAtual":x}', FIELDS)).toThrow();
  });
});
