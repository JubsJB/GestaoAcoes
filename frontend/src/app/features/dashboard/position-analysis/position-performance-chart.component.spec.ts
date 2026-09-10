import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { PosicaoResponse } from '../models/dashboard';
import { PositionAnalysisComponent } from './position-analysis.component';
import { PositionPerformanceChartComponent } from './position-performance-chart.component';
import { MANY_POSITIONS, positionFixture } from './position-analysis.fixtures';

async function render(metric: 'result' | 'return', positions: readonly PosicaoResponse[]) {
  await TestBed.configureTestingModule({ imports: [PositionPerformanceChartComponent] }).compileComponents();
  const fixture = TestBed.createComponent(PositionPerformanceChartComponent);
  fixture.componentRef.setInput('metric', metric);
  fixture.componentRef.setInput('positions', positions);
  fixture.detectChanges();
  return { fixture, root: fixture.nativeElement as HTMLElement };
}
const field = (metric: 'result' | 'return') => metric === 'result' ? 'resultadoNaoRealizado' : 'rentabilidadePercentual';

describe.each(['result', 'return'] as const)('Performance %s', metric => {
  it.each(['25', '-25', '0', '-0.00'])('shows the authoritative sign/value for a single asset: %s', async value => {
    const { root } = await render(metric, [positionFixture({ [field(metric)]: value })]);
    expect(root.querySelectorAll('li')).toHaveLength(1);
    const zero = value === '0' || value === '-0.00';
    expect(root.textContent).toContain(zero ? 'Neutro' : value.startsWith('-') ? 'Negativo' : 'Positivo');
    expect(root.textContent).toContain(metric === 'result' ? (zero ? 'R$ 0,00' : value.startsWith('-') ? '-R$ 25,00' : '+R$ 25,00') : (zero ? '0,00%' : value.startsWith('-') ? '-25,00%' : '+25,00%'));
    expect(root.querySelectorAll('rect')).toHaveLength(metric === 'return' || zero ? 0 : 1);
    if (metric === 'return') {
      const dot = root.querySelector<HTMLElement>('.dot')!;
      expect(dot.style.left).toBe(zero ? '50%' : value.startsWith('-') ? '0%' : '100%');
      expect(root.querySelector('.axis-labels')?.textContent).toContain('0%');
      expect(dot.classList.contains('neutral')).toBe(zero);
    } else if (!zero) {
      expect(root.querySelector('rect')?.getAttribute('x')).toBe(value.startsWith('-') ? '0' : '500');
      expect(root.querySelector('rect')?.getAttribute('width')).toBe('500');
    }
  });
  it('preserves symmetric magnitudes and independent currency domains without recomputing fields', async () => {
    const positions = Object.freeze([
      positionFixture({ acaoId: 1, [field(metric)]: '10' }),
      positionFixture({ acaoId: 2, [field(metric)]: '-10' }),
      positionFixture({ acaoId: 3, moeda: 'USD', [field(metric)]: '1000' }),
      positionFixture({ acaoId: 4, moeda: 'USD', [field(metric)]: '10' })
    ]);
    const before = JSON.stringify(positions);
    const { root } = await render(metric, positions);
    expect(Array.from(root.querySelectorAll('h4')).map(e => e.textContent)).toEqual(['BRL', 'USD']);
    if (metric === 'result') {
      expect(Array.from(root.querySelectorAll('rect')).map(e => e.getAttribute('width'))).toEqual(['500', '500', '500', '5']);
    } else {
      expect(Array.from(root.querySelectorAll<HTMLElement>('.dot')).map(e => e.style.left)).toEqual(['100%', '0%', '100%', '50.5%']);
    }
    expect(JSON.stringify(positions)).toBe(before);
    expect(root.textContent).not.toContain('20,00'); // cost=100 and current=120 are deliberately unrelated.
  });
  it.each(['BRL', 'USD'] as const)('renders only the currency present: %s', async moeda => {
    const { root } = await render(metric, [positionFixture({ moeda })]);
    expect(root.querySelectorAll('h4')).toHaveLength(1);
    expect(root.querySelector('h4')?.textContent).toBe(moeda);
  });
  it('keeps tiny values, scientific extremes and exact long strings', async () => {
    const values = ['1e400', '-1e400', '1e-400', '-0.00000000000001', '9007199254740993.01'];
    const { root } = await render(metric, values.map((value, i) => positionFixture({ acaoId: i, [field(metric)]: value })));
    expect(root.querySelectorAll('li')).toHaveLength(5);
    expect(root.textContent).toContain('Valor completo: 1' + '0'.repeat(400));
      expect(root.textContent).toContain('Valor completo: 0,' + '0'.repeat(399) + '1');
      expect(root.textContent).toContain('Valor completo: -0,00000000000001');
    expect(root.textContent).toContain('9.007.199.254.740.993,01');
    expect(root.textContent).toContain('Valor não nulo');
    expect(root.querySelectorAll('.value.positive')).toHaveLength(3);
    expect(root.querySelectorAll('.value.negative')).toHaveLength(2);
    if (metric === 'return') {
      expect(root.querySelectorAll('.dot')).toHaveLength(values.length);
      for (const dot of root.querySelectorAll<HTMLElement>('.dot')) {
        expect(Number.parseFloat(dot.style.left)).toBeGreaterThanOrEqual(0);
        expect(Number.parseFloat(dot.style.left)).toBeLessThanOrEqual(100);
      }
    }
    for (const rect of root.querySelectorAll('rect')) {
      expect(Number(rect.getAttribute('width'))).toBeGreaterThanOrEqual(0);
      expect(Number(rect.getAttribute('width'))).toBeLessThanOrEqual(500);
    }
  });
  it('preserves the full dataset and textual equivalent without focusable chart elements', async () => {
    const { root } = await render(metric, MANY_POSITIONS);
    expect(root.querySelectorAll('li')).toHaveLength(100);
    expect(root.querySelectorAll(metric === 'return' ? '.dot-plot[aria-hidden="true"]' : 'svg[aria-hidden="true"][focusable="false"]')).toHaveLength(100);
    expect(root.querySelector('button,a,[tabindex]')).toBeNull();
    for (const position of MANY_POSITIONS) expect(root.textContent).toContain(position.nomeEmpresa);
    expect(root.querySelectorAll('.sr-only')).toHaveLength(100);
  });
  it('keeps an empty dataset empty', async () => {
    const { root } = await render(metric, []);
    expect(root.querySelector('li,h4,svg')).toBeNull();
  });
});

it('integrates the three charts in order and removes all on an empty portfolio', async () => {
  await TestBed.configureTestingModule({ imports: [PositionAnalysisComponent] }).compileComponents();
  const fixture = TestBed.createComponent(PositionAnalysisComponent);
  fixture.componentRef.setInput('positions', [positionFixture({ ticker: 'ABCDEFGH', nomeEmpresa: 'Empresa com nome muito extenso '.repeat(8) })]);
  fixture.detectChanges();
  const root = fixture.nativeElement as HTMLElement;
  expect(Array.from(root.querySelectorAll('h3')).map(e => e.textContent)).toEqual(['Composição da carteira', 'Custo × Valor atual', 'Resultado não realizado', 'Rentabilidade por ativo']);
  expect(root.querySelectorAll('li')).toHaveLength(4);
  expect(Array.from(root.querySelector('.overview-panels')!.children).map(e => e.tagName.toLowerCase())).toEqual(['app-portfolio-composition', 'app-cost-value-chart']);
  expect(root.querySelectorAll('.performance-panels app-position-performance-chart')).toHaveLength(2);
  expect(root.querySelectorAll('app-portfolio-composition li')).toHaveLength(1);
  fixture.componentRef.setInput('positions', []); fixture.detectChanges();
  expect(root.querySelector('app-position-performance-chart')).toBeNull();
  expect(root.textContent).toContain('Nenhuma posição aberta');
});
