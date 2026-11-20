import { localMinuteExpectation } from './cotacao-temporal.fixtures';
import { formatCivilDate, formatCotacaoObtidaEm, formatReferenciaCotacao } from './cotacao-temporal.formatter';
import { formatOffsetDateTime } from './offset-date-time.formatter';

describe('apresentação temporal da cotação', () => {
  afterEach(() => vi.unstubAllGlobals());

  it.each([['0001-01-01', '01/01/0001'], ['9999-12-31', '31/12/9999'], ['2026-10-02', '02/10/2026'], ['+10000-10-02', '02/10/+10000'], ['-0001-10-02', '02/10/-0001'], ['2024-02-29', '29/02/2024']])('formata DATA nova ou persistida %s sem construir Date nem deslocar dia', (value, expected) => {
    const forbiddenDate = vi.fn(() => { throw new Error('DATA não é instante'); });
    vi.stubGlobal('Date', forbiddenDate);
    expect(formatCivilDate(value)).toBe(expected);
    expect(formatReferenciaCotacao({ tipo: 'DATA', data: value, instante: null })).toBe(`Cotação de ${expected}`);
    expect(forbiddenDate).not.toHaveBeenCalled();
  });

  it('preserva texto civil fora do formato contratado sem fabricar timestamp', () => {
    expect(formatCivilDate('indisponível')).toBe('indisponível');
  });

  it('formata INSTANTE com data/hora local conforme formatter existente', () => {
    const text = '2026-10-02T23:30:59.123456-03:00';
    const date = new Date(text);
    const pad = (value: number) => String(value).padStart(2, '0');
    const local = `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} às ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    expect(formatReferenciaCotacao({ tipo: 'INSTANTE', data: null, instante: text })).toBe(`Cotação de ${local}`);
  });

  it.each(['123456', '123456000', '999999000'])('offsets equivalentes e fração .%s conservam o instante e o DTO textual', fraction => {
    const a = Object.freeze({ tipo: 'INSTANTE' as const, data: null, instante: `2026-10-02T23:30:59.${fraction}-03:00` });
    const b = Object.freeze({ tipo: 'INSTANTE' as const, data: null, instante: `2026-10-03T02:30:59.${fraction}Z` });
    const before = JSON.stringify([a, b]);
    expect(formatReferenciaCotacao(a)).toBe(formatReferenciaCotacao(b));
    expect(JSON.stringify([a, b])).toBe(before);
    expect(formatCotacaoObtidaEm(a.instante)).toBe(formatCotacaoObtidaEm(b.instante));
  });

  it('não arredonda a fração para o próximo minuto e não muda precisão dos dados', () => {
    const before = '2026-10-03T02:30:59.999999Z';
    const next = '2026-10-03T02:31:00Z';
    expect(formatCotacaoObtidaEm(before)).not.toBe(formatCotacaoObtidaEm(next));
  });

  it('offset em segundos conserva a fração antes da fronteira de minuto', () => {
    const value = '2026-10-03T02:30:29.999999000+00:00:30';
    const expected = localMinuteExpectation().replace(':30', ':29');
    expect(formatCotacaoObtidaEm(value)).toBe(expected);
    expect(formatCotacaoObtidaEm('2026-10-03T02:30:30.000000000+00:00:30')).toBe(localMinuteExpectation());
    expect(value).toBe('2026-10-03T02:30:29.999999000+00:00:30');
  });

  it.each([
    '2026-10-03T02:30:29.123456Z',
    '2026-10-03T03:30:29.123456+01:00',
    '2026-10-02T23:30:29.123456-03:00',
    '2026-10-03T02:30:59.123456+00:00:30',
    '2026-10-03T02:29:59.123456000-00:00:30',
    '2026-10-03T02:30:29.999999000Z'
  ])('referência e obtenção interpretam %s no fuso local sem mudar o DTO', text => {
    const dto = Object.freeze({ tipo: 'INSTANTE' as const, data: null, instante: text });
    expect(formatReferenciaCotacao(dto)).toBe(`Cotação de ${localMinuteExpectation()}`);
    expect(formatCotacaoObtidaEm(text)).toBe(localMinuteExpectation());
    expect(dto.instante).toBe(text);
  });

  it('DESCONHECIDA permanece explícita e não consulta relógio', () => {
    vi.stubGlobal('Date', vi.fn(() => { throw new Error('Sem referência'); }));
    expect(formatReferenciaCotacao({ tipo: 'DESCONHECIDA', data: null, instante: null })).toBe('Referência da cotação indisponível');
  });

  it('obtenção nula é explícita; conhecida é formatada separadamente', () => {
    expect(formatCotacaoObtidaEm(null)).toBe('Não informada');
    expect(formatCotacaoObtidaEm('2026-10-04T10:20:30.123456-03:00')).toBe(formatOffsetDateTime('2026-10-04T10:20:30.123456-03:00'));
  });
});
