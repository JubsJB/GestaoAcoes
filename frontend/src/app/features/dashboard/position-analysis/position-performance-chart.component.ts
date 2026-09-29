import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { formatFinancialMoney, formatFinancialPercent } from '../../../shared/formatters/financial-value.formatter';
import { PosicaoResponse } from '../models/dashboard';
import { projectFinancialValues } from './position-chart-geometry';
import { exactPresentation, groupPositions } from './position-chart-presentation';

@Component({
  selector: 'app-position-performance-chart',
  templateUrl: './position-performance-chart.component.html',
  styleUrl: './position-performance-chart.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PositionPerformanceChartComponent {
  readonly positions = input.required<readonly PosicaoResponse[]>();
  protected readonly groups = computed(() => groupPositions(this.positions()).map(group => {
    // Each currency owns one monetary geometry domain. Percentages never enter this axis.
    const resultProjections = projectFinancialValues(group.positions.map(position => position.resultadoNaoRealizado));
    const returnProjections = projectFinancialValues(group.positions.map(position => position.rentabilidadePercentual));
    return {
      currency: group.currency,
      rows: group.positions.map((position, index) => {
        const result = resultProjections[index];
        const resultValue = result.authoritativeValue;
        const resultFormatted = formatFinancialMoney(resultValue, group.currency);
        const returnProjection = returnProjections[index];
        const returnValue = returnProjection.authoritativeValue;
        const returnFormatted = formatFinancialPercent(returnValue);
        return {
          position,
          ...result,
          resultFormatted: /[eE][+-]?\d/.test(resultFormatted) ? `${exactPresentation(resultValue).exact} ${group.currency}` : result.sign === 1 ? `+${resultFormatted}` : resultFormatted,
          resultOutcome: this.outcome(result.sign),
          resultExact: exactPresentation(resultValue),
          resultSmall: result.sign !== 0 && result.sign !== null && result.ratio !== null && result.ratio < .005,
          returnSign: returnProjection.sign,
          returnFormatted: /[eE][+-]?\d/.test(returnFormatted) ? `${exactPresentation(returnValue).exact}%` : returnFormatted,
          returnOutcome: this.outcome(returnProjection.sign),
          returnExact: exactPresentation(returnValue)
        };
      })
    };
  }));

  private outcome(sign: -1 | 0 | 1 | null): string {
    return sign === null ? 'Indisponível' : sign === 0 ? 'Neutro' : sign === 1 ? 'Positivo' : 'Negativo';
  }
}
