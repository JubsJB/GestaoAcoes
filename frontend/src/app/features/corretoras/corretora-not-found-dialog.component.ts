import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

import { SessionService } from '../../core/auth/session.service';

export interface CorretoraNotFoundDialogData {
  readonly cnpj: string;
}

@Component({
  selector: 'app-corretora-not-found-dialog',
  imports: [A11yModule, MatButtonModule, MatDialogModule],
  template: `
    <div class="broker-dialog-layout">
    <h2 mat-dialog-title>Corretora não cadastrada</h2>
    <mat-dialog-content>
      <p class="dialog-subtitle">Nenhuma corretora cadastrada foi encontrada para este CNPJ.</p>
      <dl class="dialog-context"><dt>CNPJ consultado</dt><dd>{{ data.cnpj }}</dd></dl>
      @if (session.admin()) {
        <p class="dialog-support">Você pode iniciar um cadastro com esse CNPJ preenchido.</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions class="app-dialog-actions" align="end">
      <span class="app-dialog-actions__item"><button mat-button type="button" [mat-dialog-close]="false" cdkFocusInitial>{{ session.admin() ? 'Cancelar' : 'Voltar à listagem' }}</button></span>
      @if (session.admin()) {
        <span class="app-dialog-actions__item"><button mat-flat-button type="button" [mat-dialog-close]="true">Cadastrar corretora</button></span>
      }
    </mat-dialog-actions>
    </div>
  `,
  styleUrl: './corretora-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CorretoraNotFoundDialogComponent {
  protected readonly session = inject(SessionService);
  protected readonly data = inject<CorretoraNotFoundDialogData>(MAT_DIALOG_DATA);
}
