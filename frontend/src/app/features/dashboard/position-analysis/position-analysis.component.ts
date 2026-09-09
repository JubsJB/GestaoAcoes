import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { PosicaoResponse } from '../models/dashboard';
import { CostValueChartComponent } from './cost-value-chart.component';
import { PositionPerformanceChartComponent } from './position-performance-chart.component';

@Component({
  selector: 'app-position-analysis',
  imports: [CostValueChartComponent, PositionPerformanceChartComponent],
  template: `
    <section aria-labelledby="position-analysis-title">
      <h2 id="position-analysis-title">Análise por ativo</h2>
      @if (positions().length === 0) {
        <div class="app-state app-surface">
          <h3>Nenhuma posição aberta para comparar</h3>
          <p>A análise aparecerá quando esta carteira tiver posições abertas.</p>
        </div>
      } @else {
        <app-cost-value-chart [positions]="positions()" />
        <div class="performance-panels">
          <app-position-performance-chart metric="result" [positions]="positions()" />
          <app-position-performance-chart metric="return" [positions]="positions()" />
        </div>
      }
    </section>
  `,
  styles: ':host { display: block; min-width: 0; }.performance-panels{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,24rem),1fr));gap:1rem;margin-top:1.5rem;align-items:start}',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PositionAnalysisComponent {
  readonly positions = input.required<readonly PosicaoResponse[]>();
}
