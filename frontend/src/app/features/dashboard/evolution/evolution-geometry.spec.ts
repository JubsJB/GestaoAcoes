import { describe, expect, it } from 'vitest';

import { buildEvolutionGeometry, segmentPath } from './evolution-geometry';
import { EvolucaoPatrimonialPontoResponse } from './evolution.models';

const points: EvolucaoPatrimonialPontoResponse[] = [
  { snapshotId: 1, dataHoraSnapshot: '2026-09-04T10:00:00Z', patrimonios: [{ moeda: 'BRL', patrimonioAtual: '100.000000000000' }] },
  { snapshotId: 2, dataHoraSnapshot: '2026-09-04T10:00:00.100Z', patrimonios: [{ moeda: 'BRL', patrimonioAtual: '110.000000000000' }, { moeda: 'USD', patrimonioAtual: '99999999999999999999999999.123456789012' }] },
  { snapshotId: 3, dataHoraSnapshot: '2026-09-04T10:00:00.200Z', patrimonios: [{ moeda: 'BRL', patrimonioAtual: '120.000000000000' }] },
  { snapshotId: 4, dataHoraSnapshot: '2026-09-04T10:00:00.300Z', patrimonios: [] },
  { snapshotId: 5, dataHoraSnapshot: '2026-09-04T10:00:00.400Z', patrimonios: [{ moeda: 'BRL', patrimonioAtual: '130.000000000000' }] }
];

describe('evolution geometry', () => {
  it('mantém string autoritativa e restringe aproximação às coordenadas', () => {
    const geometry = buildEvolutionGeometry(points, 'USD')!;
    expect(geometry.points).toHaveLength(1);
    expect(geometry.points[0].authoritativeValue).toBe('99999999999999999999999999.123456789012');
    expect(Object.keys(geometry.points[0])).not.toContain('approximateValue');
    expect(geometry.points[0].x).toEqual(expect.any(Number));
    expect(geometry.points[0].y).toEqual(expect.any(Number));
  });

  it('interrompe gaps sem zeros, carry-forward ou interpolação', () => {
    const geometry = buildEvolutionGeometry(points, 'BRL')!;
    expect(geometry.points.map(point => point.authoritativeValue)).toEqual([
      '100.000000000000', '110.000000000000', '120.000000000000', '130.000000000000'
    ]);
    expect(geometry.segments.map(segment => segment.length)).toEqual([3, 1]);
    expect(geometry.points.some(point => point.authoritativeValue === '0')).toBe(false);
    expect(segmentPath(geometry.segments[1]).startsWith('M')).toBe(true);
    expect(segmentPath(geometry.segments[1])).not.toContain('L');
  });

  it('trata domínio único e reduz apenas labels, não pontos', () => {
    const many = Array.from({ length: 1000 }, (_, index) => ({
      snapshotId: index + 1,
      dataHoraSnapshot: new Date(Date.UTC(2026, 0, 1, 0, 0, index)).toISOString(),
      patrimonios: [{ moeda: 'BRL' as const, patrimonioAtual: '1.000000000000' }]
    }));
    const geometry = buildEvolutionGeometry(many, 'BRL')!;
    expect(geometry.points).toHaveLength(1000);
    expect(geometry.xLabels.length).toBeLessThanOrEqual(4);
    expect(geometry.points.every(point => Number.isFinite(point.x) && Number.isFinite(point.y))).toBe(true);
  });
});
