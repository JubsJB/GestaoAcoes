import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

import { AppIconComponent } from '../../shared/app-icon/app-icon.component';

export interface CorretoraNotFoundDialogData {
  readonly cnpj: string;
}

@Component({
  selector: 'app-corretora-not-found-dialog',
  imports: [A11yModule, AppIconComponent, MatButtonModule, MatDialogModule],
  template: `
    <div class="dialog-heading">
      <span class="dialog-heading__icon" aria-hidden="true"><app-icon name="search" /></span>
      <div>
        <span class="dialog-eyebrow">Resultado da busca</span>
        <h2 mat-dialog-title>Corretora não cadastrada</h2>
      </div>
    </div>
    <mat-dialog-content>
      <p>Nenhuma corretora cadastrada foi encontrada para o CNPJ <strong>{{ data.cnpj }}</strong>.</p>
      <p class="dialog-support">Você pode voltar à listagem ou iniciar um cadastro com esse CNPJ preenchido.</p>
    </mat-dialog-content>
    <mat-dialog-actions class="app-dialog-actions" align="end">
      <span class="app-dialog-actions__item"><button mat-button type="button" [mat-dialog-close]="false" cdkFocusInitial>Cancelar</button></span>
      <span class="app-dialog-actions__item"><button mat-flat-button type="button" [mat-dialog-close]="true">Cadastrar corretora</button></span>
    </mat-dialog-actions>
  `,
  styles: [`:host{display:block;max-width:32rem;color:var(--app-text-primary)}`],
  styleUrl: '../../shared/dialog/dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CorretoraNotFoundDialogComponent {
  protected readonly data = inject<CorretoraNotFoundDialogData>(MAT_DIALOG_DATA);
}
