import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { PositionAnalysisComponent } from './position-analysis.component';
import { positionFixture, THREE_POSITIONS } from './position-analysis.fixtures';

async function render(positions = THREE_POSITIONS) {
  await TestBed.configureTestingModule({ imports: [PositionAnalysisComponent] }).compileComponents();
  const fixture = TestBed.createComponent(PositionAnalysisComponent);
  fixture.componentRef.setInput('positions', positions);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('PositionAnalysisComponent', () => {
  it('apresenta distribuição antes de desempenho em seções próprias', async () => {
    const root = await render();
    expect(Array.from(root.querySelectorAll('h2')).map(heading => heading.id)).toEqual([
      'portfolio-distribution-title', 'asset-performance-title'
    ]);
    expect(root.querySelectorAll('app-portfolio-composition')).toHaveLength(1);
    expect(root.querySelectorAll('app-position-performance-chart')).toHaveLength(1);
  });

  it('não apresenta Custo × Valor atual nem gráfico percentual independente', async () => {
    const root = await render();
    expect(root.querySelector('app-cost-value-chart')).toBeNull();
    expect(root.textContent).not.toContain('Custo × Valor atual');
    expect(root.querySelectorAll('app-position-performance-chart')).toHaveLength(1);
  });

  it('mantém vazios distintos para distribuição e desempenho', async () => {
    const root = await render([]);
    expect(root.textContent).toContain('Nenhuma posição para distribuir');
    expect(root.textContent).toContain('Nenhuma posição aberta para comparar');
    expect(root.querySelector('app-portfolio-composition')).toBeNull();
    expect(root.querySelector('app-position-performance-chart')).toBeNull();
  });

  it('repassa a coleção recebida sem solicitar ou recalcular dados', async () => {
    const positions = [positionFixture({ acaoId: 71, ticker: 'ORIGINAL', resultadoNaoRealizado: '7', rentabilidadePercentual: '3' })];
    const before = JSON.stringify(positions);
    const root = await render(positions);
    expect(root.textContent).toContain('ORIGINAL');
    expect(root.textContent).toContain('R$ 7,00');
    expect(root.textContent).toContain('3,00%');
    expect(JSON.stringify(positions)).toBe(before);
  });
});
