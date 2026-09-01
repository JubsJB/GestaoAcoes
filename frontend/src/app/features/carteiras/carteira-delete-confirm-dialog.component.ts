import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { FeedbackAlertComponent } from '../../shared/feedback-alert/feedback-alert.component';
import { CarteirasService } from './carteiras.service';
import { CarteiraResponse } from './models/carteira';

@Component({
  selector: 'app-carteira-delete-confirm-dialog',
  imports: [FeedbackAlertComponent, MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Excluir carteira?</h2>
    <mat-dialog-content>
      <p>Você está prestes a excluir <strong>{{ data.nome }}</strong>. Esta ação só será executada após sua confirmação.</p>
      @if (error()) { <app-feedback-alert variant="error" [message]="error()!.message" [details]="error()!.details" /> }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [disabled]="deleting()" (click)="cancel()">Cancelar</button>
      <button mat-flat-button type="button" [disabled]="deleting()" [attr.aria-busy]="deleting()" (click)="confirm()">Excluir</button>
    </mat-dialog-actions>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CarteiraDeleteConfirmDialogComponent {
  protected readonly data = inject<CarteiraResponse>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<CarteiraDeleteConfirmDialogComponent, boolean>);
  private readonly service = inject(CarteirasService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly deleting = signal(false);
  protected readonly error = signal<NormalizedHttpError | null>(null);

  protected cancel(): void { if (!this.deleting()) this.dialogRef.close(false); }

  protected confirm(): void {
    if (this.deleting()) return;
    this.deleting.set(true);
    this.error.set(null);
    this.service.excluir(this.data.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.dialogRef.close(true),
      error: (error: NormalizedHttpError) => { this.error.set(error); this.deleting.set(false); }
    });
  }
}
