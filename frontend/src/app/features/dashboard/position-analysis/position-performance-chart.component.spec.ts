import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { PosicaoResponse } from '../models/dashboard';
import { MANY_POSITIONS, positionFixture } from './position-analysis.fixtures';
import { PositionPerformanceChartComponent } from './position-performance-chart.component';

async function render(positions: readonly PosicaoResponse[]) {
  await TestBed.configureTestingModule({ imports: [PositionPerformanceChartComponent] }).compileComponents();
  const fixture = TestBed.createComponent(PositionPerformanceChartComponent);
  fixture.componentRef.setInput('positions', positions);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('PositionPerformanceChartComponent', () => {
  it('consolida resultado monetário principal e rentabilidade complementar por ativo', async () => {
    const root = await render([positionFixture({ resultadoNaoRealizado: '25.00', rentabilidadePercentual: '12.50' })]);
    expect(root.textContent).toContain('Resultado não realizado e rentabilidade');
    expect(root.textContent).toContain('+R$ 25,00');
    expect(root.textContent).toContain('Rentabilidade');
    expect(root.textContent).toContain('12,50%');
    expect(root.querySelectorAll('svg')).toHaveLength(1);
    expect(root.querySelector('.dot-plot')).toBeNull();
  });

  it.each([
    ['25', '8', 'Positivo', 'Positivo'],
    ['-25', '-8', 'Negativo', 'Negativo'],
    ['0.000', '-0.00', 'Neutro', 'Neutro'],
    ['25', '-8', 'Positivo', 'Negativo'],
    ['-25', '8', 'Negativo', 'Positivo']
  ])('preserva sinais independentes de resultado %s e rentabilidade %s', async (result, rentability, resultState, returnState) => {
    const root = await render([positionFixture({ resultadoNaoRealizado: result, rentabilidadePercentual: rentability })]);
    expect(root.querySelector('.value')!.textContent).toContain(resultState);
    expect(root.querySelector('.return-value')!.textContent).toContain(returnState);
  });

  it('mantém grupos e escalas monetárias independentes para BRL e USD', async () => {
    const root = await render([
      positionFixture({ acaoId: 1, moeda: 'BRL', resultadoNaoRealizado: '10', rentabilidadePercentual: '9999' }),
      positionFixture({ acaoId: 2, moeda: 'BRL', resultadoNaoRealizado: '-10', rentabilidadePercentual: '1' }),
      positionFixture({ acaoId: 3, moeda: 'USD', mercado: 'EUA', resultadoNaoRealizado: '1000', rentabilidadePercentual: '-1' }),
      positionFixture({ acaoId: 4, moeda: 'USD', mercado: 'EUA', resultadoNaoRealizado: '-10', rentabilidadePercentual: '-9999' })
    ]);
    const groups = root.querySelectorAll('.currency');
    expect(groups).toHaveLength(2);
    expect(groups[0].querySelector('h4')?.textContent).toBe('BRL');
    expect(groups[1].querySelector('h4')?.textContent).toBe('USD');
    expect(groups[0].textContent).toContain('R$');
    expect(groups[1].textContent).toContain('US$');
    for (const [index, expected] of [[0, [[500, 500], [0, 500]]], [1, [[500, 500], [495, 5]]]] as const) {
      const bars = groups[index].querySelectorAll('svg rect');
      expect(bars).toHaveLength(expected.length);
      expected.forEach(([x, width], barIndex) => {
        expect(Number(bars[barIndex].getAttribute('x'))).toBeCloseTo(x, 10);
        expect(Number(bars[barIndex].getAttribute('width'))).toBeCloseTo(width, 10);
        expect(bars[barIndex].classList.contains(barIndex === 0 ? 'positive' : 'negative')).toBe(true);
      });
    }
  });

  it.each(['0', '-0.00'])('não desenha barra monetária para zero %s, preservando eixo e percentual', async value => {
    const root = await render([positionFixture({ resultadoNaoRealizado: value, rentabilidadePercentual: '12.50' })]);
    expect(root.querySelector('svg rect')).toBeNull();
    expect(root.querySelector('svg .zero')?.getAttribute('d')).toBe('M 500 0 V 24');
    expect(root.querySelector('.value')?.textContent).toContain('R$ 0,00');
    expect(root.querySelector('.value')?.textContent).toContain('Neutro');
    expect(root.querySelector('.return-value')?.textContent).toContain('+12,50%');
  });

  it('preserva valores lossless e geometria finita para extremos', async () => {
    const root = await render([
      positionFixture({ acaoId: 1, resultadoNaoRealizado: '1e400', rentabilidadePercentual: '-0.000000000001' }),
      positionFixture({ acaoId: 2, resultadoNaoRealizado: '1e-400', rentabilidadePercentual: '9007199254740993.01' })
    ]);
    expect(root.textContent).toContain('Resultado completo:');
    expect(root.textContent).toContain('-0,000000000001%');
    expect(root.textContent).toContain('9.007.199.254.740.993,01%');
    for (const rect of root.querySelectorAll('rect')) {
      expect(rect.getAttribute('x')).not.toMatch(/NaN|Infinity/);
      expect(rect.getAttribute('width')).not.toMatch(/NaN|Infinity/);
    }
  });

  it('apresenta os 100 ativos integralmente sem controles ou foco gráfico', async () => {
    const root = await render(MANY_POSITIONS);
    expect(root.querySelectorAll('li')).toHaveLength(100);
    expect(root.querySelector('button, [tabindex], [role="button"]')).toBeNull();
    expect(root.querySelectorAll('svg[aria-hidden="true"][focusable="false"]')).toHaveLength(100);
    for (const position of MANY_POSITIONS) expect(root.textContent).toContain(position.nomeEmpresa);
  });
});
