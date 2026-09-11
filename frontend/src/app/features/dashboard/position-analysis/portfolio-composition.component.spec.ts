import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { PortfolioCompositionComponent } from './portfolio-composition.component';
import { MANY_POSITIONS, positionFixture, THREE_POSITIONS } from './position-analysis.fixtures';
import { PosicaoResponse } from '../models/dashboard';

async function render(positions: readonly PosicaoResponse[]) {
  await TestBed.configureTestingModule({ imports: [PortfolioCompositionComponent] }).compileComponents();
  const fixture = TestBed.createComponent(PortfolioCompositionComponent);
  fixture.componentRef.setInput('positions', positions); fixture.detectChanges();
  return { fixture, root: fixture.nativeElement as HTMLElement };
}
describe('portfolio composition', () => {
  it.each(['BRL', 'USD'] as const)('renders one authoritative %s asset without percentages or total', async moeda => {
    const { root } = await render([positionFixture({ moeda, valorAtualPosicao: '12.123456789012' })]);
    expect(root.querySelectorAll('svg')).toHaveLength(1);
    expect(root.querySelector('.slice')?.getAttribute('stroke-dasharray')).toBe('1 0');
    expect(root.textContent).toContain('12,123456789012');
    expect(root.textContent).not.toMatch(/%|777|999|total/i);
    expect(root.querySelector('.ring-currency')?.textContent).toBe(moeda);
    expect(root.querySelectorAll('li')).toHaveLength(1);
  });
  it('uses independent currency groups and original item order', async () => {
    const before = JSON.stringify(THREE_POSITIONS);
    const { root, fixture } = await render(THREE_POSITIONS);
    const groups = root.querySelectorAll('.composition-group');
    expect(groups).toHaveLength(2);
    expect(Array.from(groups[0].querySelectorAll('li')).map(e => e.getAttribute('data-asset-id'))).toEqual(['9', '1']);
    expect(groups[1].querySelector('.slice')?.getAttribute('stroke-dasharray')).toBe('1 0');
    expect(JSON.stringify(THREE_POSITIONS)).toBe(before);
    fixture.componentRef.setInput('positions', []); fixture.detectChanges();
    expect(root.querySelector('svg')).toBeNull();
    expect(root.textContent).toContain('Nenhuma posição');
  });
  it('keeps zero and invalid values in text without fake arcs', async () => {
    const { root } = await render([
      positionFixture({ valorAtualPosicao: '0E-12' }),
      positionFixture({ acaoId: 2, moeda: 'USD', valorAtualPosicao: '-1' })
    ]);
    expect(root.querySelector('svg')).toBeNull();
    expect(root.textContent).toContain('R$ 0,00');
    expect(root.textContent).not.toContain('0E-12');
    expect(root.textContent).toContain('Sem valor para distribuir');
    expect(root.textContent).toContain('Composição indisponível');
    expect(root.querySelectorAll('li')).toHaveLength(2);
  });
  it('provides all names and values without hover or tab stops for 100 assets', async () => {
    const { root } = await render(MANY_POSITIONS);
    expect(root.querySelectorAll('li')).toHaveLength(100);
    for (const p of MANY_POSITIONS) expect(root.textContent).toContain(p.nomeEmpresa);
    expect(root.querySelector('button, [tabindex], [role="button"]')).toBeNull();
    expect(root.querySelectorAll('svg[aria-hidden="true"]')).toHaveLength(2);
    expect(root.textContent).toContain('Fatia pequena');
    expect(root.querySelector('.textured')).not.toBeNull();
  });
});
