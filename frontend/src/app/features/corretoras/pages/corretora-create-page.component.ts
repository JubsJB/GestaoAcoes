import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, finalize, switchMap } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { StickyBackComponent } from '../../../shared/sticky-back/sticky-back.component';
import { CorretoraConfirmDialogComponent } from '../corretora-confirm-dialog.component';
import { formatCnpj, onlyDigits } from '../corretora-formatters';
import { Corretora, CorretoraCreateRequest } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';

export interface CorretoraCreateDialogData { readonly cnpj?: string; }

@Component({
  selector: 'app-corretora-create-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, PageHeaderComponent, ReactiveFormsModule, RouterLink, StickyBackComponent],
  template: `
    <section class="app-page" [class.app-dialog-page]="isDialog" aria-labelledby="create-title">
      @if (!isDialog) { <app-sticky-back route="/corretoras" label="Voltar para corretoras" /> }
      <app-page-header headingId="create-title" eyebrow="Corretoras" icon="broker" title="Cadastrar corretora" description="Informe o CNPJ da instituição que deseja cadastrar." />
      @if (error()) { <app-feedback-alert variant="error" [message]="error()!.message" [details]="error()!.details" /> }
      <form class="app-form-surface app-surface" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="surface-heading"><app-icon name="identity" aria-hidden="true" /><div><h2>Identificação da instituição</h2><p>Os demais dados serão consultados com segurança durante o cadastro.</p></div></div>
        <mat-form-field appearance="outline">
          <mat-label>CNPJ</mat-label>
          <input matInput formControlName="cnpj" inputmode="numeric" maxlength="18" (input)="formatInput()" aria-describedby="cnpj-hint" />
          <mat-hint id="cnpj-hint">14 dígitos, com ou sem máscara.</mat-hint>
          @if (form.controls.cnpj.invalid && form.controls.cnpj.touched) { <mat-error>Informe um CNPJ com 14 dígitos.</mat-error> }
        </mat-form-field>
        <div class="app-actions app-actions--stack-compact"><button mat-flat-button type="submit" [disabled]="submitting()" [attr.aria-busy]="submitting()">Cadastrar</button>@if(isDialog){<button mat-button type="button" (click)="cancel()">Cancelar</button>}@else{<a mat-button routerLink="..">Cancelar</a>}</div>
        @if (submitting()) { <div role="status" aria-live="polite" class="progress"><mat-spinner diameter="28" /> Processando cadastro…</div> }
      </form>
    </section>
  `,
  styles: [`form{display:grid;gap:1.25rem}.progress{display:flex;align-items:center;gap:.75rem}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CorretoraCreatePageComponent {
  private readonly service = inject(CorretorasService);
  private readonly dialog = inject(MatDialog);
  private readonly successToast = inject(SuccessToastService);
  private readonly router = inject(Router);
  private readonly dialogRef = inject(MatDialogRef<CorretoraCreatePageComponent, Corretora | undefined>, { optional: true });
  private readonly dialogData = inject<CorretoraCreateDialogData | null>(MAT_DIALOG_DATA, { optional: true });
  private readonly destroyRef = inject(DestroyRef);
  protected readonly submitting = signal(false);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly isDialog = this.dialogRef !== null;
  protected readonly form = new FormGroup({ cnpj: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^(?:\D*\d){14}\D*$/)] }) });

  constructor() {
    const cnpj = this.dialogCnpj() ?? this.navigationCnpj();
    if (cnpj) this.form.controls.cnpj.setValue(formatCnpj(cnpj));
  }

  protected formatInput(): void {
    const control = this.form.controls.cnpj;
    control.setValue(formatCnpj(control.value), { emitEvent: false });
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.submitting()) return;
    this.send({ cnpj: onlyDigits(this.form.controls.cnpj.value) });
  }

  private send(request: CorretoraCreateRequest): void {
    this.submitting.set(true);
    this.error.set(null);
    this.service.cadastrar(request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (broker) => this.complete(broker),
      error: (error: NormalizedHttpError) => this.handleError(error, request.cnpj)
    });
  }

  private handleError(error: NormalizedHttpError, cnpj: string): void {
    if (error.status !== 409 || error.code !== 'SITUACAO_CADASTRAL_NAO_ATIVA') {
      this.error.set(error);
      this.submitting.set(false);
      return;
    }
    const status = typeof error.details['situacaoCadastral'] === 'string' ? error.details['situacaoCadastral'] : null;
    this.dialog.open(CorretoraConfirmDialogComponent, { data: { message: error.message, situacaoCadastral: status }, restoreFocus: true }).afterClosed().pipe(
      switchMap((confirmed) => confirmed === true ? this.service.cadastrar({ cnpj, confirmarSituacaoCadastralNaoAtiva: true }) : EMPTY),
      finalize(() => this.submitting.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({ next: (broker) => this.complete(broker), error: (nextError: NormalizedHttpError) => this.error.set(nextError) });
  }

  private complete(corretora: Corretora): void {
    if (this.dialogRef) { this.dialogRef.close(corretora); this.submitting.set(false); return; }
    this.successToast.show('Corretora cadastrada com sucesso.');
    void this.router.navigate(['/corretoras', corretora.id], { info: { corretora } }).finally(() => this.submitting.set(false));
  }

  protected cancel(): void { this.dialogRef?.close(); }

  private dialogCnpj(): string | null {
    if (!this.dialogData || typeof this.dialogData.cnpj !== 'string') return null;
    const cnpj = onlyDigits(this.dialogData.cnpj);
    return cnpj.length === 14 ? cnpj : null;
  }

  private navigationCnpj(): string | null {
    const info = this.router.currentNavigation()?.extras.info;
    if (!info || typeof info !== 'object' || !('cnpj' in info) || typeof info.cnpj !== 'string') return null;
    const cnpj = onlyDigits(info.cnpj);
    return cnpj.length === 14 ? cnpj : null;
  }
}
