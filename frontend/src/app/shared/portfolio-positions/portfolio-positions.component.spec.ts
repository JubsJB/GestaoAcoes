import { TestBed } from '@angular/core/testing';
import { PortfolioPositionsComponent } from './portfolio-positions.component';
import { PosicaoResponse } from '../../features/dashboard/models/dashboard';

describe('PortfolioPositionsComponent', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('mantém uma tabela com todos os valores, moeda, referência e resultados textuais', () => {
    const fixture = TestBed.createComponent(PortfolioPositionsComponent);
    const base: PosicaoResponse = { acaoId: 1, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL', moeda: 'BRL', quantidadeAtual: '5', precoMedio: '40', custoPosicao: '200', cotacaoAtual: '48.2', dataHoraCotacao: '2026-01-01T12:00:00Z', valorAtualPosicao: '241', resultadoNaoRealizado: '41', rentabilidadePercentual: '20.5' };
    fixture.componentRef.setInput('positions', [base, { ...base, acaoId: 2, moeda: 'USD', resultadoNaoRealizado: '-1', rentabilidadePercentual: '0' }]);
    fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelectorAll('table')).toHaveLength(1);
    expect(root.querySelectorAll('tbody tr')).toHaveLength(2);
    expect(root.querySelectorAll('thead th[scope="col"]')).toHaveLength(8);
    const row = root.querySelector('tbody tr')!;
    expect(row.querySelectorAll('td.numeric')).toHaveLength(7);
    expect(row.textContent).toContain('Petrobras');
    for (const value of ['R$ 40,00', 'R$ 200,00', 'R$ 48,20', 'R$ 241,00', 'R$ 41,00']) expect(row.textContent).toContain(value);
    expect(row.querySelector('.quote-reference')?.textContent).toContain('01/01/2026');
    expect(root.textContent).toContain('USD');
    for (const label of ['Positivo', 'Negativo', 'Neutro']) expect(root.textContent).toContain(label);
  });
});
