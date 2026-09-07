import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import type { PosicaoResponse } from '../../features/dashboard/models/dashboard';
import { financialOutcomeLabel, formatFinancialMoney, formatFinancialPercent, formatFinancialQuantity } from '../formatters/financial-value.formatter';
import { formatOffsetDateTime } from '../formatters/offset-date-time.formatter';

@Component({
  selector: 'app-portfolio-positions',
  template: `          @if (positions().length === 0) { <div class="app-state app-surface"><h3>Nenhuma posição aberta</h3><p>Registre uma compra para iniciar uma posição.</p></div> }
          @else { <div class="records" role="list">@for (position of positions(); track position.acaoId) {
            <article class="record app-surface" role="listitem"><header><div><strong>{{ position.ticker }}</strong><span>{{ position.nomeEmpresa }}</span></div><span class="currency-badge">{{ position.mercado }} · {{ position.moeda }}</span></header><dl>
              <div><dt>Quantidade</dt><dd>{{ quantity(position.quantidadeAtual, position.mercado) }}</dd></div>
              <div><dt>Preço médio</dt><dd>{{ money(position.precoMedio, position.moeda) }}</dd></div>
              <div><dt>Custo da posição</dt><dd>{{ money(position.custoPosicao, position.moeda) }}</dd></div>
              <div><dt>Cotação atual</dt><dd>{{ money(position.cotacaoAtual, position.moeda) }}</dd></div>
              <div><dt>Atualizada em</dt><dd>{{ dateTime(position.dataHoraCotacao) }}</dd></div>
              <div><dt>Valor atual</dt><dd>{{ money(position.valorAtualPosicao, position.moeda) }}</dd></div>
              <div><dt>Resultado não realizado</dt><dd>{{ money(position.resultadoNaoRealizado, position.moeda) }} · {{ outcome(position.resultadoNaoRealizado) }}</dd></div>
              <div><dt>Rentabilidade</dt><dd>{{ percent(position.rentabilidadePercentual) }} · {{ outcome(position.rentabilidadePercentual) }}</dd></div>
            </dl></article>
          }</div> }`,
  styles: [`.records{display:grid;gap:.75rem}.record{padding:1rem}.record header{display:flex;justify-content:space-between;gap:1rem}.record header>div{display:grid;gap:.2rem}.record header span,dt{color:var(--app-text-secondary)}.currency-badge{white-space:nowrap}.record dl{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.9rem;margin:1rem 0 0}.record dl div{min-width:0}.record dt{font-size:.75rem;text-transform:uppercase;letter-spacing:.04em;font-weight:700}.record dd{margin:.25rem 0 0;overflow-wrap:anywhere}@media(max-width:70rem){.record dl{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:42rem){.record header{align-items:stretch;flex-direction:column}.record dl{grid-template-columns:1fr}}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PortfolioPositionsComponent {
  readonly positions = input.required<readonly PosicaoResponse[]>();
  protected readonly money = formatFinancialMoney;
  protected readonly quantity = formatFinancialQuantity;
  protected readonly percent = formatFinancialPercent;
  protected readonly dateTime = formatOffsetDateTime;
  protected readonly outcome = financialOutcomeLabel;
}
