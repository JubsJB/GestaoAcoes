import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { PosicaoResponse } from '../models/dashboard';
import { PositionAnalysisComponent } from './position-analysis.component';
import { CostValueChartComponent } from './cost-value-chart.component';
import { EMPTY_POSITIONS, EXTREME_POSITIONS, MANY_POSITIONS, ONE_POSITION, positionFixture, THREE_POSITIONS } from './position-analysis.fixtures';

async function render(positions: readonly PosicaoResponse[]) {
  await TestBed.configureTestingModule({ imports: [PositionAnalysisComponent] }).compileComponents();
  const fixture = TestBed.createComponent(PositionAnalysisComponent);
  fixture.componentRef.setInput('positions', positions);
  fixture.detectChanges();
  return { fixture, root: (fixture.nativeElement.querySelector('app-cost-value-chart') ?? fixture.nativeElement) as HTMLElement };
}

function widths(root: Element) {
  return Array.from(root.querySelectorAll('.bar')).map(bar => bar.querySelector('rect')?.getAttribute('width') ?? '0');
}

describe('PositionAnalysis / Custo × Valor atual', () => {
  it('explains an empty collection without empty graphics or future placeholders', async () => {
    const { root } = await render(EMPTY_POSITIONS);
    expect(root.textContent).toContain('Nenhuma posição aberta para comparar');
    expect(root.querySelector('svg, app-cost-value-chart')).toBeNull();
    expect(root.textContent).not.toMatch(/Resultado não realizado|Rentabilidade/);
  });

  it('shows one asset with both authoritative series, not quantity times price', async () => {
    const { root } = await render(ONE_POSITION);
    expect(root.querySelectorAll('li')).toHaveLength(1);
    expect(root.textContent).toContain('PETR4');
    expect(root.textContent).toContain('Petrobras');
    expect(root.textContent).toContain('R$ 100,00');
    expect(root.textContent).toContain('R$ 120,00');
    expect(root.textContent).not.toContain('777');
    expect(root.textContent).not.toContain('999');
    expect(root.querySelectorAll('dt')).toHaveLength(2);
    expect(widths(root).map(Number)).toEqual([100 / 120 * 1000, 1000]);
    expect(ONE_POSITION[0].quantidadeAtual).toBe('0.123456');
  });

  it.each(['BRL', 'USD'] as const)('renders only the present currency %s', async currency => {
    const { root } = await render([positionFixture({ moeda: currency, mercado: currency === 'BRL' ? 'BRASIL' : 'EUA' })]);
    expect(root.querySelectorAll('.chart-group')).toHaveLength(1);
    expect(root.querySelector('h4')?.textContent).toBe(currency);
    expect(root.querySelector('.amount')?.textContent).toBe(currency === 'BRL' ? 'R$ 100,00' : 'US$ 100,00');
  });

  it('separates currencies, shares cost/current scale within each, preserves received asset order', async () => {
    const { root } = await render(THREE_POSITIONS);
    const groups = root.querySelectorAll('.chart-group');
    expect(groups).toHaveLength(2);
    expect(Array.from(groups[0].querySelectorAll('li')).map(row => row.getAttribute('data-asset-id'))).toEqual(['9', '1']);
    [100 / 120 * 1000, 1000, 100 / 120 * 1000, 80 / 120 * 1000].forEach((expected, index) => {
      expect(Number(widths(groups[0])[index])).toBeCloseTo(expected, 10);
    });
    expect(widths(groups[0])[0]).toBe(widths(groups[0])[2]);
    expect(widths(groups[1]).map(Number)).toEqual([500 / 600 * 1000, 1000]);
    expect(root.textContent).toContain('Compare quanto foi investido em cada ativo com o valor atual da posição.');
    expect(root.textContent).not.toMatch(/escala independente|Mesma escala|Variação/);
    expect(THREE_POSITIONS.map(item => item.acaoId)).toEqual([9, 2, 1]);
  });

  it.each([
    ['100', '100', ['1000', '1000']],
    ['0', '100', ['0', '1000']],
    ['100', '0', ['1000', '0']],
    ['0', '-0.00', ['0', '0']]
  ])('represents equality and zero faithfully (%s / %s)', async (cost, current, expected) => {
    const { root } = await render([positionFixture({ custoPosicao: cost as string, valorAtualPosicao: current as string })]);
    expect(widths(root)).toEqual(expected);
    expect(root.querySelectorAll('dt')).toHaveLength(2);
    if (cost === '0' || current === '0') expect(root.textContent).toContain('R$ 0,00');
    expect(root.textContent).not.toMatch(/Lucro|Prejuízo/);
  });

  it('retains long decimal differences, scientific extremes and full tiny values without minimum bars', async () => {
    const before = JSON.stringify(EXTREME_POSITIONS);
    const { root } = await render(EXTREME_POSITIONS);
    expect(root.querySelectorAll('li')).toHaveLength(3);
    expect(root.textContent).toContain('R$ 9.007.199.254.740.993,01');
    expect(root.textContent).toContain('R$ 9.007.199.254.740.993,02');
    expect(root.textContent).toContain('Valor completo: 0.00000000000000000001 BRL');
    expect(root.textContent).toContain('Valor completo: 1e-400 BRL');
    expect(root.textContent).toContain('Valor completo: 1e400 BRL');
    const small = root.querySelector('[data-asset-id="2"]')!;
    expect(small.textContent).toContain('Valor não nulo; a barra pode não ser perceptível');
    expect(widths(small)).toEqual(['0', '0']);
    expect(widths(root).map(Number).every(value => Number.isFinite(value) && value >= 0 && value <= 1000)).toBe(true);
    expect(JSON.stringify(EXTREME_POSITIONS)).toBe(before);
  });

  it('keeps all 100 assets and a single permanent textual equivalent without tab stops', async () => {
    const { root } = await render(MANY_POSITIONS);
    expect(root.querySelectorAll('li')).toHaveLength(100);
    expect(root.querySelectorAll('dt')).toHaveLength(200);
    expect(root.querySelectorAll('.amount')).toHaveLength(200);
    expect(root.querySelector('button, a, input, [tabindex], [role="button"]')).toBeNull();
    expect(root.querySelectorAll('svg.bar[aria-hidden="true"][focusable="false"]')).toHaveLength(200);
    for (const group of root.querySelectorAll('.chart-group')) {
      const currency = group.querySelector('h4')!.textContent!.slice(0, 3);
      expect(Array.from(group.querySelectorAll('li')).map(item => item.getAttribute('data-asset-id')))
        .toEqual(MANY_POSITIONS.filter(item => item.moeda === currency).map(item => String(item.acaoId)));
    }
    for (const asset of MANY_POSITIONS) expect(root.textContent).toContain(asset.nomeEmpresa);
  });

  it('keeps extended identity and unrounded fractional value in HTML outside the SVG', async () => {
    const ticker = 'TICKER'.repeat(20);
    const company = 'Empresa com nome muito extenso '.repeat(10);
    const { root, fixture } = await render([positionFixture({ ticker, nomeEmpresa: company, custoPosicao: '12.12345678901234567890' })]);
    expect(root.querySelector('.asset-heading')?.textContent).toContain(ticker);
    expect(root.querySelector('.asset-heading')?.textContent).toContain(company);
    expect(root.textContent).toContain('Valor completo: 12.12345678901234567890 BRL');
    expect(root.querySelector('svg text')).toBeNull();
    fixture.componentRef.setInput('positions', EMPTY_POSITIONS); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('li')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain(ticker);
  });

  it('preserves first-seen currency order and a genuinely tiny nonzero bar', async () => {
    const { root } = await render([
      positionFixture({ moeda: 'USD', mercado: 'EUA', custoPosicao: '0.00001', valorAtualPosicao: '1000' }),
      positionFixture({ acaoId: 3, moeda: 'BRL' })
    ]);
    expect(Array.from(root.querySelectorAll('h4')).map(heading => heading.id)).toEqual(['cost-value-USD', 'cost-value-BRL']);
    const row = root.querySelector('li')!;
    expect(Number(widths(row)[0])).toBeGreaterThan(0);
    expect(Number(widths(row)[0])).toBeLessThan(.001);
    expect(row.textContent).toContain('Valor completo: 0.00001 USD');
    expect(row.textContent).toContain('Valor não nulo');
  });

  it('keeps an explicit textual fallback for unexpected negative/invalid cost values', async () => {
    const { root } = await render([positionFixture({ custoPosicao: '-100', valorAtualPosicao: 'inválido' })]);
    expect(widths(root)).toEqual(['0', '0']);
    expect(root.textContent).toContain('-R$ 100,00');
    expect(root.textContent).toContain('inválido BRL');
    expect(root.textContent).toContain('Valor indisponível nesta escala de comparação');
  });

  it('provides fluid structural reflow, full HTML text and distinct series without animation', async () => {
    const { root } = await render(THREE_POSITIONS);
    const styles = (CostValueChartComponent as unknown as { ɵcmp: { styles: string[] } }).ɵcmp.styles.join('');
    expect(styles).toContain('repeat(auto-fit, minmax(min(100%, 22rem), 1fr))');
    expect(styles).toContain('grid-template-columns: minmax(0, 1fr) minmax(0, 1fr)');
    expect(styles).toContain('min-width: 0');
    expect(styles).toContain('overflow-wrap: anywhere');
    expect(styles).not.toMatch(/white-space: nowrap|text-overflow: ellipsis|animation:|transition:|overflow-y:/);
    expect(styles).toMatch(/\.cost[^}]+fill: none;[^}]+stroke:/);
    expect(styles).toMatch(/\.current[^}]+fill:/);
    expect(root.querySelectorAll('.cost dt')).toHaveLength(3);
    expect(root.querySelectorAll('.current dt')).toHaveLength(3);
    expect(root.querySelector('svg text, [title]')).toBeNull();
  });
  it.each(['BRL', 'USD'] as const)('preserva diferenças pequenas e grandes no par gráfico %s', async moeda => {
    const { root } = await render([
      positionFixture({ acaoId: 1, moeda, custoPosicao: '1566', valorAtualPosicao: '1569' }),
      positionFixture({ acaoId: 2, moeda, custoPosicao: '100', valorAtualPosicao: '1500' })
    ]);
    const rows = root.querySelectorAll('.chart-asset');
    const near = widths(rows[0]).map(Number);
    const far = widths(rows[1]).map(Number);
    expect(near[0] / near[1]).toBeCloseTo(1566 / 1569, 10);
    expect(near[1] - near[0]).toBeLessThan(2);
    expect(far[1] / far[0]).toBeCloseTo(15, 10);
    expect(root.querySelectorAll('.comparison-plot')).toHaveLength(2);
    expect(root.querySelectorAll('.cost dt')).toHaveLength(2);
    expect(root.querySelectorAll('.current dt')).toHaveLength(2);
    expect(root.querySelector('[role="progressbar"]')).toBeNull();
  });

});
