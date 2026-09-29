import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { PosicaoResponse } from '../models/dashboard';
import { PortfolioCompositionComponent } from './portfolio-composition.component';
import { PositionPerformanceChartComponent } from './position-performance-chart.component';

@Component({
  selector: 'app-position-analysis',
  imports: [PositionPerformanceChartComponent, PortfolioCompositionComponent],
  template: `
    <section class="analysis-section" aria-labelledby="portfolio-distribution-title">
      <header class="section-heading">
        <h2 id="portfolio-distribution-title">Distribuição da carteira</h2>
        <p>Onde o patrimônio da carteira está alocado por ativo.</p>
      </header>
      @if (positions().length === 0) {
        <div class="app-state app-surface">
          <h3>Nenhuma posição para distribuir</h3>
          <p>A distribuição aparecerá quando esta carteira tiver posições abertas.</p>
        </div>
      } @else {
        <app-portfolio-composition [positions]="positions()" />
      }
    </section>

    <section class="analysis-section" aria-labelledby="asset-performance-title">
      <header class="section-heading">
        <h2 id="asset-performance-title">Desempenho por ativo</h2>
        <p>Resultado não realizado em destaque, com a rentabilidade de cada posição como complemento.</p>
      </header>
      @if (positions().length === 0) {
        <div class="app-state app-surface">
          <h3>Nenhuma posição aberta para comparar</h3>
          <p>O desempenho aparecerá quando esta carteira tiver posições abertas.</p>
        </div>
      } @else {
        <app-position-performance-chart [positions]="positions()" />
      }
    </section>
  `,
  styles: ':host{display:grid;gap:1.25rem;min-width:0;container-type:inline-size}.analysis-section{display:grid;gap:1rem;min-width:0}.section-heading h2,.section-heading p{margin:0}.section-heading h2{font-size:1.25rem;letter-spacing:-.025em}.section-heading p{margin-top:.35rem;color:var(--app-text-secondary);overflow-wrap:anywhere}.analysis-section app-portfolio-composition{margin-top:0}',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PositionAnalysisComponent {
  readonly positions = input.required<readonly PosicaoResponse[]>();
}
