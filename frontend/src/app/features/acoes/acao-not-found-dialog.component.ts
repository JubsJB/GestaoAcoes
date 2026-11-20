import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

import { formatMercado } from './acao-formatters';
import { Mercado } from './models/acao';
import { SessionService } from '../../core/auth/session.service';

export interface AcaoNotFoundDialogData {
  readonly ticker: string;
  readonly mercado: Mercado;
}

@Component({
  selector: 'app-acao-not-found-dialog',
  imports: [A11yModule, MatButtonModule, MatDialogModule],
  template: `
    <div class="stock-dialog-layout">
    <h2 mat-dialog-title>Ação não cadastrada</h2>
    <mat-dialog-content>
      <p>Não encontramos uma ação cadastrada com este ticker e mercado.</p>
      <dl class="dialog-context"><div><dt>Ticker · mercado</dt><dd>{{ data.ticker }} · {{ market(data.mercado) }}</dd></div></dl>
      @if (session.admin()) {
        <p class="dialog-support">Você pode voltar à listagem ou iniciar um cadastro com essa combinação preenchida.</p>
      } @else {
        <p class="dialog-support">Retorne à listagem para consultar outra ação.</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions class="app-dialog-actions" align="end">
      <span class="app-dialog-actions__item"><button mat-button type="button" [mat-dialog-close]="false" cdkFocusInitial>{{ session.admin() ? 'Cancelar' : 'Voltar à listagem' }}</button></span>
      @if (session.admin()) {
        <span class="app-dialog-actions__item"><button mat-flat-button type="button" [mat-dialog-close]="true">Cadastrar ação</button></span>
      }
    </mat-dialog-actions>
    </div>
  `,
  styleUrl: './acao-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AcaoNotFoundDialogComponent {
  protected readonly session = inject(SessionService);
  protected readonly data = inject<AcaoNotFoundDialogData>(MAT_DIALOG_DATA);
  protected readonly market = formatMercado;
}
