import { formatOffsetDateTime } from './offset-date-time.formatter';

describe('formatOffsetDateTime', () => {
  it('formats an OffsetDateTime in pt-BR using the browser local timezone', () => {
    const value = '2026-08-30T14:45:00-03:00';
    const date = new Date(value);
    const pad = (part: number) => String(part).padStart(2, '0');
    const expected = `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} às ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    expect(formatOffsetDateTime(value)).toBe(expected);
  });

  it('does not mutate the DTO value and preserves invalid input', () => {
    const dto = { dataHoraCotacao: 'valor-inválido' };
    expect(formatOffsetDateTime(dto.dataHoraCotacao)).toBe('valor-inválido');
    expect(dto.dataHoraCotacao).toBe('valor-inválido');
  });
});
