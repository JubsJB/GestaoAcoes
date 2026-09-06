import { Currency } from '../../../shared/formatters/financial-value.formatter';
import { EvolucaoPatrimonialPontoResponse } from './evolution.models';

export interface EvolutionGeometryPoint {
  snapshotId: number;
  timestamp: string;
  authoritativeValue: string;
  x: number;
  y: number;
}

export interface EvolutionGeometry {
  currency: Currency;
  segments: EvolutionGeometryPoint[][];
  points: EvolutionGeometryPoint[];
  xLabels: EvolutionGeometryPoint[];
}

const WIDTH = 720;
const HEIGHT = 260;
const LEFT = 44;
const RIGHT = 18;
const TOP = 18;
const BOTTOM = 38;

/** The only boundary allowed to approximate financial strings for SVG pixel geometry. */
export function buildEvolutionGeometry(
  snapshots: readonly EvolucaoPatrimonialPontoResponse[],
  currency: Currency
): EvolutionGeometry | null {
  const rawSegments: RawPoint[][] = [];
  let current: RawPoint[] = [];
  snapshots.forEach((snapshot, snapshotIndex) => {
    const component = snapshot.patrimonios.find(item => item.moeda === currency);
    if (!component) {
      if (current.length) rawSegments.push(current);
      current = [];
      return;
    }
    const approximateValue = Number(component.patrimonioAtual);
    const timestampMs = Date.parse(snapshot.dataHoraSnapshot);
    if (!Number.isFinite(approximateValue) || !Number.isFinite(timestampMs)) return;
    current.push({
      snapshotId: snapshot.snapshotId,
      timestamp: snapshot.dataHoraSnapshot,
      authoritativeValue: component.patrimonioAtual,
      approximateValue,
      timestampMs,
      snapshotIndex
    });
  });
  if (current.length) rawSegments.push(current);
  const rawPoints = rawSegments.flat();
  if (!rawPoints.length) return null;

  const timeMin = Math.min(...rawPoints.map(point => point.timestampMs));
  const timeMax = Math.max(...rawPoints.map(point => point.timestampMs));
  const valueMin = Math.min(...rawPoints.map(point => point.approximateValue));
  const valueMax = Math.max(...rawPoints.map(point => point.approximateValue));
  const indexMax = Math.max(1, snapshots.length - 1);
  const plotWidth = WIDTH - LEFT - RIGHT;
  const plotHeight = HEIGHT - TOP - BOTTOM;

  const convert = (point: RawPoint): EvolutionGeometryPoint => ({
    snapshotId: point.snapshotId,
    timestamp: point.timestamp,
    authoritativeValue: point.authoritativeValue,
    x: timeMax === timeMin
      ? LEFT + plotWidth * point.snapshotIndex / indexMax
      : LEFT + plotWidth * (point.timestampMs - timeMin) / (timeMax - timeMin),
    y: valueMax === valueMin
      ? TOP + plotHeight / 2
      : TOP + plotHeight * (valueMax - point.approximateValue) / (valueMax - valueMin)
  });
  const segments = rawSegments.map(segment => segment.map(convert));
  const points = segments.flat();
  return { currency, segments, points, xLabels: selectAxisLabels(points) };
}

export function segmentPath(segment: readonly EvolutionGeometryPoint[]): string {
  return segment.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ');
}

function selectAxisLabels(points: readonly EvolutionGeometryPoint[]): EvolutionGeometryPoint[] {
  if (points.length <= 4) return [...points];
  const indexes = new Set([0, Math.floor((points.length - 1) / 3), Math.floor(2 * (points.length - 1) / 3), points.length - 1]);
  return [...indexes].sort((a, b) => a - b).map(index => points[index]);
}

interface RawPoint {
  snapshotId: number;
  timestamp: string;
  authoritativeValue: string;
  approximateValue: number;
  timestampMs: number;
  snapshotIndex: number;
}
