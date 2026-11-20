import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { EvolutionValueLabelsDirective } from './evolution-value-labels.directive';

@Component({ imports: [EvolutionValueLabelsDirective], template: `<div appEvolutionValueLabels><div class="value-axis"><span data-value-y="18">R$ 100,00</span><span data-value-y="19">R$ 101,00</span><span data-value-y="120">R$ 150,00</span><span data-value-y="222">R$ 200,00</span><span data-value-y="100">R$ 9999999999999999999999999,00</span></div><svg viewBox="0 0 720 260"></svg></div>` })
class LabelHost {}

describe('referências monetárias de observações', () => {
  afterEach(() => { TestBed.resetTestingModule(); vi.unstubAllGlobals(); });
  it('mostra referências sem colisão ou corte, reavalia no resize e desconecta ao destruir', () => {
    let resize!: ResizeObserverCallback;
    const disconnect = vi.fn();
    vi.stubGlobal('ResizeObserver', class {
      constructor(callback: ResizeObserverCallback) { resize = callback; }
      observe() {} disconnect = disconnect;
    });
    const fixture = TestBed.createComponent(LabelHost);
    const root = fixture.nativeElement as HTMLElement;
    const host = root.firstElementChild as HTMLElement;
    const axis = host.querySelector<HTMLElement>('.value-axis')!;
    const svg = host.querySelector('svg')!;
    const labels = Array.from(axis.querySelectorAll('span'));
    let height = 260;
    const rect = (width: number, h: number) => ({ x: 0, y: 0, top: 0, left: 0, right: width, bottom: h, width, height: h, toJSON() {} });
    vi.spyOn(host, 'getBoundingClientRect').mockImplementation(() => rect(460, height));
    vi.spyOn(axis, 'getBoundingClientRect').mockImplementation(() => rect(88, height));
    vi.spyOn(svg, 'getBoundingClientRect').mockImplementation(() => rect(360, height));
    labels.forEach((label, i) => vi.spyOn(label, 'getBoundingClientRect').mockImplementation(() => rect(i === 4 ? 250 : 70, 14)));
    fixture.detectChanges();
    const visible = () => labels.filter(label => label.style.visibility === 'visible').map(label => label.textContent);
    expect(visible()).toEqual(['R$ 100,00', 'R$ 150,00', 'R$ 200,00']);
    expect(labels[0].style.top).toBe('11px');
    height = 40; resize([], {} as ResizeObserver);
    expect(visible()).toEqual(['R$ 100,00', 'R$ 200,00']);
    expect(labels.map(label => label.dataset['valueY'])).toEqual(['18', '19', '120', '222', '100']);
    fixture.destroy(); expect(disconnect).toHaveBeenCalledOnce();
  });
});
