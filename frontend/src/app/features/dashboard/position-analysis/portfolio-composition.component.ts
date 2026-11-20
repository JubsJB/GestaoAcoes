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
  // Approved categories, followed by darker companions for portfolios above six assets.
  // Numbers and textures continue to distinguish categories when the palette repeats.
  protected readonly colors = ['#315EAB', '#D39A64', '#478F89', '#8875AD', '#C87986', '#8A98A7', '#21437C', '#986139', '#27615D', '#5E4B82', '#914855', '#536475'];
  protected readonly labelColors = ['#fff', '#0b1220', '#0b1220', '#0b1220', '#0b1220', '#0b1220', '#fff', '#fff', '#fff', '#fff', '#fff', '#fff'];
  protected readonly groups = computed(() => groupPositions(this.positions()).map(group => {
    const geometry = compositionGeometry(group.positions.map(p => p.valorAtualPosicao));
    return { currency: group.currency, state: geometry.state,
      rows: group.positions.map((position, index) => ({ position, ...geometry.slices[index],
        ...moneyPresentation(geometry.slices[index], group.currency),
        small: geometry.slices[index].small })) };
  }));
}
