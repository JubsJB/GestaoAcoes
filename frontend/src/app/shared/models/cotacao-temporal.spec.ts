import { isCamposTemporaisCotacao, isDataCivilAquisicao, isNovaAquisicaoCotacao, parseInstanteMicros } from './cotacao-temporal';

function state(instant: string, obtained = '2026-10-10T00:00:00Z') {
  return { referenciaMercadoCotacao: { tipo: 'INSTANTE', instante: instant, data: null },
    cotacaoObtidaEm: obtained, dataHoraCotacao: '1960-01-01T00:00:00Z' };
}

describe('domínio de aquisição separado da leitura', () => {
  it.each(['0001-01-01', '9999-12-31', '2000-02-29', '2024-02-29'])('aceita DATA %s sem Date', value => {
    const spy = vi.spyOn(globalThis, 'Date').mockImplementation(() => { throw new Error('Date proibido'); });
    try {
      expect(isDataCivilAquisicao(value)).toBe(true);
      expect(isNovaAquisicaoCotacao({ ...state('1970-01-01T00:00:00Z'),
        referenciaMercadoCotacao: { tipo: 'DATA', data: value, instante: null } })).toBe(true);
    } finally { spy.mockRestore(); }
  });

  it.each(['0000-01-01', '-0001-01-01', '+10000-01-01', '10000-01-01', '1900-02-29',
    '2023-02-29', '2024-04-31', '2024-00-01', '2024-01-00'])('rejeita DATA %s', value => {
    expect(isDataCivilAquisicao(value)).toBe(false);
  });

  it.each(['1970-01-01T00:00:00Z', '1969-12-31T23:00:00-01:00',
    '1970-01-01T00:00:30+00:00:30', '2026-10-10T00:00:00.000000000Z'])('aceita piso/equivalência/igualdade %s', value => {
    expect(isNovaAquisicaoCotacao(state(value))).toBe(true);
  });

  it.each(['1969-12-31T23:59:59.999999Z', '2026-10-10T00:00:00.000001Z',
    '2026-10-09T23:59:59.999999001Z', '2026-02-30T00:00:00Z', '2026-10-09T24:00:00Z',
    '2026-10-09T23:60:00Z', '2026-10-09T23:59:60Z', '2026-10-09T00:00:00+18:00:01',
    '2026-10-09T00:00:00+00:60'])('rejeita aquisição inválida %s', value => {
    expect(isNovaAquisicaoCotacao(state(value))).toBe(false);
  });

  it('compara micros sem arredondamento, relógio ou mutação', () => {
    const value = Object.freeze(state('2026-10-09T23:59:59.999999Z'));
    expect(parseInstanteMicros(value.cotacaoObtidaEm)! - parseInstanteMicros(value.referenciaMercadoCotacao.instante)!).toBe(1n);
    expect(isNovaAquisicaoCotacao(value)).toBe(true);
    expect(value.referenciaMercadoCotacao.instante).toBe('2026-10-09T23:59:59.999999Z');
  });

  it('preserva leitura anterior sem aplicar limites financeiros retroativamente', () => {
    for (const value of [state('1960-01-01T00:00:00Z'), state('2027-01-01T00:00:00Z'),
      { ...state('1970-01-01T00:00:00Z'), cotacaoObtidaEm: null },
      { ...state('1970-01-01T00:00:00Z'), referenciaMercadoCotacao: { tipo: 'DATA', data: '+10000-01-01', instante: null } }]) {
      expect(isCamposTemporaisCotacao(value)).toBe(true);
      expect(isNovaAquisicaoCotacao(value)).toBe(false);
    }
  });
});
