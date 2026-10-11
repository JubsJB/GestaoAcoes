import { temporalPresentationCases } from '../formatters/cotacao-temporal.fixtures';
import { TestBed } from '@angular/core/testing';
import { PortfolioPositionsComponent } from './portfolio-positions.component';
import { PosicaoResponse } from '../../features/dashboard/models/dashboard';
import { formatFinancialMoney } from '../formatters/financial-value.formatter';

describe('PortfolioPositionsComponent', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('mantém uma tabela com todos os valores, moeda, referência e resultados textuais', () => {
    const fixture = TestBed.createComponent(PortfolioPositionsComponent);
    const base: PosicaoResponse = { acaoId: 1, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL', moeda: 'BRL', quantidadeAtual: '5', precoMedio: '40', custoPosicao: '200', cotacaoAtual: '48.2', dataHoraCotacao: '2026-01-01T12:00:00Z', referenciaMercadoCotacao: { tipo: 'DESCONHECIDA', data: null, instante: null }, cotacaoObtidaEm: null, valorAtualPosicao: '241', resultadoNaoRealizado: '41', rentabilidadePercentual: '20.5' };
    fixture.componentRef.setInput('positions', [base, { ...base, acaoId: 2, moeda: 'USD', resultadoNaoRealizado: '-1', rentabilidadePercentual: '0' }]);
    fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelectorAll('table')).toHaveLength(1);
    expect(root.querySelectorAll('tbody tr')).toHaveLength(2);
    expect(root.querySelectorAll('thead th[scope="col"]')).toHaveLength(8);
    const row = root.querySelector('tbody tr')!;
    expect(row.querySelectorAll('td.numeric')).toHaveLength(7);
    expect(row.textContent).toContain('Petrobras');
    expect(row.querySelector('.asset-name')?.textContent).toBe('PETR4');
    expect(row.querySelector('.market-badge')?.textContent).toContain('BRL');
    expect(row.querySelector('.primary-value')?.textContent).toBe('R$ 241,00');
    expect(root.querySelector('[data-outcome="Positivo"] .outcome')?.textContent).toBe('Positivo');
    expect(root.querySelector('[data-outcome="Negativo"] .outcome')?.textContent).toBe('Negativo');
    expect(root.querySelector('[data-outcome="Neutro"] .outcome')?.textContent).toBe('Neutro');
    for (const value of ['R$ 40,00', 'R$ 200,00', 'R$ 48,20', 'R$ 241,00', 'R$ 41,00']) expect(row.textContent).toContain(value);
    expect(row.querySelector('.quote-reference')?.textContent).toContain('Referência da cotação indisponível');
    expect(row.querySelector('.quote-reference')?.textContent).not.toContain('01/01/2026');
    expect(root.textContent).toContain('USD');
    for (const label of ['Positivo', 'Negativo', 'Neutro']) expect(root.textContent).toContain(label);
  });

  it.each(temporalPresentationCases())('usa referência $tipo sem legado e preserva decimal textual', (reference, expected) => {
    const fixture = TestBed.createComponent(PortfolioPositionsComponent);
    const position = Object.freeze({ acaoId: 1, ticker: 'AAPL', nomeEmpresa: 'Apple', mercado: 'EUA', moeda: 'USD',
      quantidadeAtual: '5', precoMedio: '40', custoPosicao: '200', cotacaoAtual: '9999999999999.123456',
      dataHoraCotacao: '1999-01-01T00:00:00Z', referenciaMercadoCotacao: Object.freeze(reference), cotacaoObtidaEm: null,
      valorAtualPosicao: '241', resultadoNaoRealizado: '41', rentabilidadePercentual: '20.5' } satisfies PosicaoResponse);
    fixture.componentRef.setInput('positions', Object.freeze([position])); fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('.quote-reference')?.textContent).toBe(expected);
    expect(root.textContent).not.toContain('1999');
    expect(root.textContent).toContain(formatFinancialMoney(position.cotacaoAtual, 'USD'));
    expect(position.cotacaoAtual).toBe('9999999999999.123456');
    expect(root.querySelectorAll('table')).toHaveLength(1);
  });
});
