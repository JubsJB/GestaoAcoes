import { projectFinancialValues } from './position-chart-geometry';

/** Only normalized drawing weights are summed. No monetary total or displayed percentage. */
export function compositionGeometry(values: readonly string[]) {
  const projections = projectFinancialValues(values);
  const invalid = projections.some(p => p.sign === null || p.sign === -1);
  const weight = invalid ? 0 : projections.reduce((sum, p) => sum + (p.ratio ?? 0), 0);
  let offset = 0;
  return {
    state: invalid ? 'invalid' : weight === 0 ? 'zero' : 'ready',
    slices: projections.map(projection => {
      const sweep = weight ? (projection.ratio ?? 0) / weight : 0;
      const start = offset;
      offset += sweep;
      const angle = (start + sweep / 2) * 2 * Math.PI - Math.PI / 2;
      return { ...projection, sweep, start, x: 120 + Math.cos(angle) * 83, y: 120 + Math.sin(angle) * 83,
        small: projection.sign === 1 && sweep < .04 };
    })
  };
}
