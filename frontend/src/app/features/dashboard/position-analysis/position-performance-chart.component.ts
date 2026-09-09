import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { formatFinancialMoney, formatFinancialPercent } from '../../../shared/formatters/financial-value.formatter';
import { PosicaoResponse } from '../models/dashboard';
import { projectFinancialValues } from './position-chart-geometry';
import { groupPositions } from './position-chart-presentation';

@Component({
  selector: 'app-position-performance-chart',
  templateUrl: './position-performance-chart.component.html',
  styleUrl: './position-performance-chart.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PositionPerformanceChartComponent {
  readonly positions = input.required<readonly PosicaoResponse[]>();
  readonly metric = input.required<'result' | 'return'>();
  protected readonly title = computed(() => this.metric() === 'result' ? 'Resultado não realizado' : 'Rentabilidade por ativo');
  protected readonly groups = computed(() => groupPositions(this.positions()).map(group => {
    const percent = this.metric() === 'return';
    // Project only the authoritative field. Each currency/metric owns its geometry domain.
    const projections = projectFinancialValues(group.positions.map(p => percent ? p.rentabilidadePercentual : p.resultadoNaoRealizado));
    return {
      currency: group.currency,
      rows: group.positions.map((position, index) => {
        const projection = projections[index];
        const value = projection.authoritativeValue;
        const formatted = percent ? formatFinancialPercent(value) : formatFinancialMoney(value, group.currency);
        return {
          position, ...projection,
          formatted: !percent && projection.sign === 1 ? `+${formatted}` : formatted,
          outcome: projection.sign === null ? 'Indisponível' : projection.sign === 0 ? 'Neutro' : projection.sign === 1 ? 'Positivo' : 'Negativo',
          complete: /[eE]/.test(value) || /[1-9]/.test((value.split('.')[1] ?? '').slice(2)),
          small: projection.sign !== 0 && projection.sign !== null && projection.ratio !== null && projection.ratio < .005
        };
      })
    };
  }));
}
