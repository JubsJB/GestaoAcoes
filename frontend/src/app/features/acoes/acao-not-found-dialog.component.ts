import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

import { AppIconComponent } from '../../shared/app-icon/app-icon.component';
import { formatMercado } from './acao-formatters';
import { Mercado } from './models/acao';

export interface AcaoNotFoundDialogData {
  readonly ticker: string;
  readonly mercado: Mercado;
}

@Component({
  selector: 'app-acao-not-found-dialog',
  imports: [A11yModule, AppIconComponent, MatButtonModule, MatDialogModule],
  template: `
    <div class="dialog-heading">
      <span class="dialog-heading__icon" aria-hidden="true"><app-icon name="search" /></span>
      <div>
        <span class="dialog-eyebrow">Resultado da busca</span>
        <h2 mat-dialog-title>Ação não cadastrada</h2>
      </div>
    </div>
    <mat-dialog-content>
      <p>Não existe ação cadastrada para <strong>{{ data.ticker }}</strong> no mercado <strong>{{ market(data.mercado) }}</strong>.</p>
      <p class="dialog-support">Você pode voltar à listagem ou iniciar um cadastro com essa combinação preenchida.</p>
    </mat-dialog-content>
    <mat-dialog-actions class="app-dialog-actions" align="end">
      <span class="app-dialog-actions__item"><button mat-button type="button" [mat-dialog-close]="false" cdkFocusInitial>Cancelar</button></span>
      <span class="app-dialog-actions__item"><button mat-flat-button type="button" [mat-dialog-close]="true">Cadastrar ação</button></span>
    </mat-dialog-actions>
  `,
  styles: [`
    :host{display:block;max-width:32rem;color:var(--app-text-primary)}
    .dialog-heading{display:flex;align-items:center;gap:.875rem;padding:1.5rem 1.5rem .25rem}
    .dialog-heading__icon{width:2.75rem;height:2.75rem;display:grid;place-items:center;flex:0 0 auto;border-radius:.875rem;color:#765537;background:var(--app-surface-warm)}
    .dialog-heading__icon app-icon{width:1.4rem;height:1.4rem}
    .dialog-eyebrow{display:block;color:#765537;font-size:.7rem;font-weight:750;letter-spacing:.08em;text-transform:uppercase}
    h2[mat-dialog-title]{margin:.2rem 0 0;padding:0;font-size:1.25rem;line-height:1.25}
    mat-dialog-content{padding-top:.75rem;color:var(--app-text-primary)}
    mat-dialog-content p{line-height:1.55}.dialog-support{color:var(--app-text-secondary)}
    @media(max-width:36rem){.dialog-heading{padding:1.25rem 1.25rem .25rem}.dialog-heading__icon{width:2.5rem;height:2.5rem}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AcaoNotFoundDialogComponent {
  protected readonly data = inject<AcaoNotFoundDialogData>(MAT_DIALOG_DATA);
  protected readonly market = formatMercado;
}
