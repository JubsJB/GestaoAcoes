import { ReferenciaMercadoCotacaoResponse } from '../models/cotacao-temporal';

// Independent literal oracles. Tests deliberately select a timezone so their
// expectations exercise the browser-local policy, rather than the formatter.
export function localMinuteExpectation(): string {
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  const expected: Record<string, string> = {
    'America/Los_Angeles': '02/10/2026 às 19:30',
    'Pacific/Kiritimati': '03/10/2026 às 16:30',
    'America/Sao_Paulo': '02/10/2026 às 23:30',
    'UTC': '03/10/2026 às 02:30'
  };
  if (!expected[zone]) throw new Error(`Configure TZ para um dos fusos de teste: ${zone}`);
  return expected[zone];
}

export function temporalPresentationCases(): [ReferenciaMercadoCotacaoResponse, string][] {
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  const epoch: Record<string, string> = {
    UTC: '01/01/1970 às 00:00',
    'America/Los_Angeles': '31/12/1969 às 16:00',
    'Pacific/Kiritimati': '31/12/1969 às 13:20',
    'America/Sao_Paulo': '31/12/1969 às 21:00'
  };
  return [
    [{ tipo: 'DATA', data: '0001-01-01', instante: null }, 'Cotação de 01/01/0001'],
    [{ tipo: 'DATA', data: '9999-12-31', instante: null }, 'Cotação de 31/12/9999'],
    [{ tipo: 'DATA', data: '2000-02-29', instante: null }, 'Cotação de 29/02/2000'],
    [{ tipo: 'INSTANTE', data: null, instante: '1970-01-01T00:00:00Z' }, `Cotação de ${epoch[zone]}`],
    [{ tipo: 'INSTANTE', data: null, instante: '1970-01-01T00:00:30.000000000+00:00:30' }, `Cotação de ${epoch[zone]}`],
    [{ tipo: 'DATA', data: '2026-10-02', instante: null }, 'Cotação de 02/10/2026'],
    [{ tipo: 'DATA', data: '+10000-10-02', instante: null }, 'Cotação de 02/10/+10000'],
    [{ tipo: 'DATA', data: '-0001-10-02', instante: null }, 'Cotação de 02/10/-0001'],
    [{ tipo: 'INSTANTE', data: null, instante: '2026-10-03T02:30:59.123456+00:00:30' }, `Cotação de ${localMinuteExpectation()}`],
    [{ tipo: 'DESCONHECIDA', data: null, instante: null }, 'Referência da cotação indisponível']
  ];
}
