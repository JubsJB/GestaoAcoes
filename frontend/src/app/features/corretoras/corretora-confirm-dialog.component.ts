import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface CorretoraConfirmationData {
  message: string;
  situacaoCadastral: string | null;
}

@Component({
  selector: 'app-corretora-confirm-dialog',
  imports: [A11yModule, MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Confirmar cadastro</h2>
    <mat-dialog-content>
      <p>A situação cadastral retornada pelo backend não é ATIVA.</p>
      @if (data.situacaoCadastral) {
        <p><strong>Situação:</strong> {{ data.situacaoCadastral }}</p>
      }
      <p>{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button type="button" color="primary" [mat-dialog-close]="true" cdkFocusInitial>
        Confirmar cadastro
      </button>
    </mat-dialog-actions>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CorretoraConfirmDialogComponent {
  protected readonly data = inject<CorretoraConfirmationData>(MAT_DIALOG_DATA);
}
