import { afterEveryRender, AfterViewInit, Directive, ElementRef, inject, OnDestroy } from '@angular/core';

export interface LabelBounds { left: number; right: number }

export function clampLabel(label: LabelBounds, width: number): LabelBounds {
  const offset = Math.max(-label.left, Math.min(0, width - label.right));
  return { left: label.left + offset, right: label.right + offset };
}

/** Presentation only: keep endpoint labels first, then fit chronological intermediates. */
export function selectVisibleLabels(labels: readonly LabelBounds[], width: number, gap = 8): number[] {
  const selected: number[] = [];
  const fits = (index: number) => {
    const label = labels[index];
    return label.right > label.left && label.left >= 0 && label.right <= width
      && selected.every(other => label.right + gap <= labels[other].left || label.left >= labels[other].right + gap);
  };
  for (const index of [...new Set([0, labels.length - 1, ...labels.map((_, i) => i)])]) {
    if (index >= 0 && index < labels.length && fits(index)) selected.push(index);
  }
  return selected.sort((a, b) => a - b);
}

@Directive({ selector: '[appAxisLabels]' })
export class AxisLabelsDirective implements AfterViewInit, OnDestroy {
  private readonly host: HTMLElement = inject(ElementRef).nativeElement;
  private observer?: ResizeObserver;

  constructor() {
    // Also handles data/position changes whose formatted label text is unchanged.
    afterEveryRender({ mixedReadWrite: () => this.measure() });
  }

  ngAfterViewInit(): void {
    if (typeof ResizeObserver !== 'undefined') {
      this.observer = new ResizeObserver(() => this.measure());
      this.observer.observe(this.host);
    }
  }

  ngOnDestroy(): void { this.observer?.disconnect(); }

  private measure(): void {
    const spans = Array.from(this.host.querySelectorAll('span'));
    const host = this.host.getBoundingClientRect();
    if (host.width <= 0) return;
    const measure = (compact: boolean) => spans.map(span => {
      span.style.translate = '0px';
      const text = compact ? span.dataset['compact'] : span.dataset['full'];
      if (text !== undefined && span.textContent !== text) span.textContent = text;
      const rect = span.getBoundingClientRect();
      const original = { left: rect.left - host.left, right: rect.right - host.left };
      const bounds = clampLabel(original, host.width);
      span.style.translate = `${bounds.left - original.left}px`;
      return { ...bounds, height: rect.height };
    });
    let rects = measure(false);
    let selected = selectVisibleLabels(rects, host.width);
    if (!selected.includes(0) || !selected.includes(spans.length - 1)) {
      rects = measure(true);
      selected = selectVisibleLabels(rects, host.width);
    }
    const visible = new Set(selected);
    let height = 0;
    spans.forEach((span, index) => {
      span.style.visibility = visible.has(index) ? 'visible' : 'hidden';
      if (visible.has(index)) height = Math.max(height, rects[index].height);
      // Text-only zoom/font changes can resize a label without resizing the chart.
      this.observer?.observe(span);
    });
    this.host.style.height = `${height}px`;
  }
}
