import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { PosicaoResponse } from '../models/dashboard';
import { projectFinancialValues } from './position-chart-geometry';
import { groupPositions, moneyPresentation } from './position-chart-presentation';

@Component({
  selector: 'app-cost-value-chart',
  templateUrl: './cost-value-chart.component.html',
  styleUrl: './position-chart.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CostValueChartComponent {
  readonly positions = input.required<readonly PosicaoResponse[]>();
  protected readonly groups = computed(() => groupPositions(this.positions()).map(group => {
    const projections = projectFinancialValues(group.positions.flatMap(position => [position.custoPosicao, position.valorAtualPosicao]));
    return {
      currency: group.currency,
      rows: group.positions.map((position, index) => ({
        position,
        series: [
          { label: 'Custo', kind: 'cost', ...moneyPresentation(projections[index * 2], group.currency) },
          { label: 'Valor atual', kind: 'current', ...moneyPresentation(projections[index * 2 + 1], group.currency) }
        ]
      }))
    };
  }));
}
