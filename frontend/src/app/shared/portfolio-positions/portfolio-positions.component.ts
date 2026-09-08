import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import type { PosicaoResponse } from '../../features/dashboard/models/dashboard';
import { financialOutcomeLabel, formatFinancialMoney, formatFinancialPercent, formatFinancialQuantity } from '../formatters/financial-value.formatter';
import { formatOffsetDateTime } from '../formatters/offset-date-time.formatter';

@Component({
  selector: 'app-portfolio-positions',
  template: `          @if (positions().length === 0) { <div class="app-state app-surface"><h3>Nenhuma posição aberta</h3><p>Registre uma compra para iniciar uma posição.</p></div> }
          @else { <table class="collection-table" role="table"><caption>Posições abertas</caption><thead role="rowgroup"><tr role="row"><th scope="col" role="columnheader">Ativo</th><th scope="col" role="columnheader">Quantidade</th><th scope="col" role="columnheader">Preço médio</th><th scope="col" role="columnheader">Custo da posição</th><th scope="col" role="columnheader">Cotação atual</th><th scope="col" role="columnheader">Valor atual</th><th scope="col" role="columnheader">Resultado não realizado</th><th scope="col" role="columnheader">Rentabilidade</th></tr></thead><tbody role="rowgroup">@for(position of positions();track position.acaoId){<tr role="row"><th scope="row" role="rowheader"><span class="cell-label" aria-hidden="true">Ativo</span>{{position.ticker}}<small>{{position.nomeEmpresa}}</small><small>{{position.mercado}} · {{position.moeda}}</small></th><td role="cell" class="numeric"><span class="cell-label" aria-hidden="true">Quantidade</span>{{quantity(position.quantidadeAtual, position.mercado)}}</td><td role="cell" class="numeric"><span class="cell-label" aria-hidden="true">Preço médio</span>{{money(position.precoMedio, position.moeda)}}</td><td role="cell" class="numeric"><span class="cell-label" aria-hidden="true">Custo da posição</span>{{money(position.custoPosicao, position.moeda)}}</td><td role="cell" class="numeric"><span class="cell-label" aria-hidden="true">Cotação atual</span>{{money(position.cotacaoAtual, position.moeda)}}<small class="quote-reference">Atualizada em {{dateTime(position.dataHoraCotacao)}}</small></td><td role="cell" class="numeric"><span class="cell-label" aria-hidden="true">Valor atual</span>{{money(position.valorAtualPosicao, position.moeda)}}</td><td role="cell" class="numeric"><span class="cell-label" aria-hidden="true">Resultado não realizado</span>{{money(position.resultadoNaoRealizado, position.moeda)}}<small>{{outcome(position.resultadoNaoRealizado)}}</small></td><td role="cell" class="numeric"><span class="cell-label" aria-hidden="true">Rentabilidade</span>{{percent(position.rentabilidadePercentual)}}<small>{{outcome(position.rentabilidadePercentual)}}</small></td></tr>}</tbody></table> }`,
  styleUrl: './portfolio-positions.component.scss',
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
