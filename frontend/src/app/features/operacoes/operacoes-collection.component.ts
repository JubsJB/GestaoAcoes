import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CorretoraNamesService } from '../corretoras/corretora-names.service';
import { OperacaoResponse } from './models/operacao';
import { formatCivilDate, formatMoney, formatOperationQuantity } from './operacao-validators';

@Component({
  selector: 'app-operacoes-collection',
  imports: [RouterLink],
  templateUrl: './operacoes-collection.component.html',
  styleUrl: './operacoes-collection.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperacoesCollectionComponent {
  readonly items = input.required<readonly OperacaoResponse[]>();
  readonly carteiraId = input.required<number>();
  readonly origem = input.required<'carteira' | 'operacoes'>();
  readonly caption = input('Histórico de operações');

  protected readonly brokerNames = inject(CorretoraNamesService);
  protected readonly date = formatCivilDate;
  protected readonly quantity = formatOperationQuantity;
  protected readonly money = formatMoney;
}
