import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { PosicaoResponse } from '../models/dashboard';
import { compositionGeometry } from './composition-geometry';
import { groupPositions, moneyPresentation } from './position-chart-presentation';

@Component({
  selector: 'app-portfolio-composition',
  templateUrl: './portfolio-composition.component.html',
  styleUrl: './portfolio-composition.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PortfolioCompositionComponent {
  readonly positions = input.required<readonly PosicaoResponse[]>();
  protected readonly colors = ['#246650', '#548777', '#394e49', '#76998d', '#456966', '#63756b'];
  protected readonly groups = computed(() => groupPositions(this.positions()).map(group => {
    const geometry = compositionGeometry(group.positions.map(p => p.valorAtualPosicao));
    return { currency: group.currency, state: geometry.state,
      rows: group.positions.map((position, index) => ({ position, ...geometry.slices[index],
        ...moneyPresentation(geometry.slices[index], group.currency),
        small: geometry.slices[index].small })) };
  }));
}
