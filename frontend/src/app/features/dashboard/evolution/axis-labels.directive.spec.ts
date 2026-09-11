import { describe, expect, it } from 'vitest';
import { clampLabel, selectVisibleLabels } from './axis-labels.directive';
import { buildEvolutionGeometry } from './evolution-geometry';

describe('axis label space selection', () => {
  it.each([1200, 768, 390, 320, 160])('anchors natural-width endpoint text inside %i CSS px', width => {
    for (const textWidth of [80, 130, 260, 480]) {
      const first = clampLabel({ left: -12, right: textWidth - 12 }, width);
      const last = clampLabel({ left: width - 3, right: width - 3 + textWidth }, width);
      expect(first.left).toBe(0);
      expect(first.right - first.left).toBe(textWidth);
      expect(last.right - last.left).toBe(textWidth);
      const labels = [first, last];
      for (const index of selectVisibleLabels(labels, width)) {
        expect(labels[index].left).toBeGreaterThanOrEqual(0);
        expect(labels[index].right).toBeLessThanOrEqual(width);
      }
      if (textWidth * 2 + 8 <= width) expect(selectVisibleLabels(labels, width)).toEqual([0, 1]);
    }
  });
  it('handles zero, one and two labels and preserves endpoints when they fit', () => {
    expect(selectVisibleLabels([], 1000)).toEqual([]);
    expect(selectVisibleLabels([{ left: 40, right: 170 }], 1000)).toEqual([0]);
    expect(selectVisibleLabels([{ left: 40, right: 170 }, { left: 800, right: 930 }], 1000)).toEqual([0, 1]);
    expect(selectVisibleLabels([{ left: 40, right: 170 }, { left: 80, right: 210 }], 320)).toEqual([0]);
  });

  it('keeps well separated desktop labels and omits a colliding middle label', () => {
    expect(selectVisibleLabels([{ left: 40, right: 180 }, { left: 300, right: 440 }, { left: 600, right: 740 }, { left: 840, right: 980 }], 1000)).toEqual([0, 1, 2, 3]);
    expect(selectVisibleLabels([{ left: 40, right: 180 }, { left: 420, right: 560 }, { left: 440, right: 580 }, { left: 840, right: 980 }], 1000)).toEqual([0, 1, 3]);
  });

  it.each([1200, 768, 390, 320])('suppresses close initial timestamps at width %i without changing geometry', width => {
    const timestamps = ['2026-09-05T14:09:00Z', '2026-09-05T14:25:00Z', '2026-09-07T18:17:00Z'];
    const snapshots = timestamps.map((dataHoraSnapshot, i) => ({ snapshotId: i + 1, dataHoraSnapshot, patrimonios: [{ moeda: 'BRL' as const, patrimonioAtual: '100' }] }));
    const geometry = buildEvolutionGeometry(snapshots, 'BRL')!;
    const original = JSON.stringify(geometry);
    const labelWidth = Math.min(130, width * .45);
    const boxes = geometry.xLabels.map((point, i) => {
      const x = point.x / 720 * width;
      const left = x - (i === 0 ? 0 : i === 2 ? labelWidth : labelWidth / 2);
      return { left, right: left + labelWidth };
    });
    expect(selectVisibleLabels(boxes, width)).toEqual([0, 2]);
    expect(JSON.stringify(geometry)).toBe(original);
    expect(geometry.points.map(point => point.timestamp)).toEqual(timestamps);
  });

  it('uses measured text width under text zoom and excludes clipped labels', () => {
    expect(selectVisibleLabels([{ left: 10, right: 100 }, { left: 150, right: 240 }, { left: 300, right: 390 }], 390)).toEqual([0, 1, 2]);
    expect(selectVisibleLabels([{ left: 10, right: 190 }, { left: 105, right: 285 }, { left: 210, right: 390 }], 390)).toEqual([0, 2]);
    expect(selectVisibleLabels([{ left: 10, right: 410 }], 320)).toEqual([]);
    expect(selectVisibleLabels([{ left: 0, right: 0 }], 320)).toEqual([]);
  });
});
