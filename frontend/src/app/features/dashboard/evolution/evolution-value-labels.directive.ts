import { afterEveryRender, AfterViewInit, Directive, ElementRef, inject, OnDestroy } from '@angular/core';

@Directive({ selector: '[appEvolutionValueLabels]' })
export class EvolutionValueLabelsDirective implements AfterViewInit, OnDestroy {
  private readonly host: HTMLElement = inject(ElementRef).nativeElement;
  private observer?: ResizeObserver;

  constructor() { afterEveryRender({ mixedReadWrite: () => this.measure() }); }

  ngAfterViewInit(): void {
    if (typeof ResizeObserver !== 'undefined') {
      this.observer = new ResizeObserver(() => this.measure());
      this.observer.observe(this.host);
    }
  }

  ngOnDestroy(): void { this.observer?.disconnect(); }

  private measure(): void {
    const axis = this.host.querySelector<HTMLElement>('.value-axis');
    const svg = this.host.querySelector('svg');
    if (!axis || !svg) return;
    const plot = svg.getBoundingClientRect();
    const bounds = axis.getBoundingClientRect();
    if (bounds.width <= 0 || plot.height <= 0) return;
    // Only screen coordinates are projected. Monetary strings are never parsed.
    const candidates = Array.from(axis.querySelectorAll<HTMLElement>('[data-value-y]')).map(label => {
      this.observer?.observe(label);
      label.style.visibility = 'hidden';
      const rect = label.getBoundingClientRect();
      const center = plot.top - bounds.top + Number(label.dataset['valueY']) / 260 * plot.height;
      label.style.top = `${center - rect.height / 2}px`;
      return { label, center, top: center - rect.height / 2, bottom: center + rect.height / 2, width: rect.width, height: rect.height };
    }).filter(item => Number.isFinite(item.center) && item.width > 0 && item.width <= bounds.width && item.height > 0)
      .sort((a, b) => a.center - b.center);
    const selected: typeof candidates = [];
    for (const item of new Set([candidates[0], candidates.at(-1), ...candidates])) {
      if (!item || selected.length === 3) continue;
      if (selected.every(other => item.bottom + 8 <= other.top || item.top >= other.bottom + 8)) {
        selected.push(item); item.label.style.visibility = 'visible';
      }
    }
  }
}
