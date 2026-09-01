import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { StickyBackComponent } from '../../../shared/sticky-back/sticky-back.component';
import { AcoesService } from '../acoes.service';
import { AcaoResponse, Mercado, normalizeTicker } from '../models/acao';

export interface AcaoCreateDialogData { readonly ticker?: string; readonly mercado?: Mercado; }

@Component({
  selector: 'app-acao-create-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule, PageHeaderComponent, ReactiveFormsModule, RouterLink, StickyBackComponent],
  template: `
    <section class="app-page" [class.app-dialog-page]="isDialog" aria-labelledby="create-title">
      @if (!isDialog) { <app-sticky-back route="/acoes" label="Voltar para ações" /> }
      <app-page-header headingId="create-title" eyebrow="Ações" icon="stock" title="Cadastrar ação" description="Informe o ticker e o mercado da ação que deseja cadastrar." />
      @if(error()){<app-feedback-alert variant="error" [message]="error()!.message" [details]="error()!.details" />}
      <form class="app-form-surface app-surface" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="surface-heading"><app-icon name="stock" aria-hidden="true" /><div><h2>Identificação da ação</h2><p>Use o ticker negociado e selecione o mercado correspondente.</p></div></div>
        <mat-form-field appearance="outline"><mat-label>Ticker</mat-label><input matInput formControlName="ticker" maxlength="30" aria-describedby="ticker-hint"/><mat-hint id="ticker-hint">Até 30 caracteres; espaços externos e letras minúsculas serão normalizados.</mat-hint>@if(form.controls.ticker.invalid&&form.controls.ticker.touched){<mat-error>Ticker é obrigatório e deve ter até 30 caracteres.</mat-error>}</mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Mercado</mat-label><mat-select formControlName="mercado" aria-describedby="mercado-hint"><mat-option value="BRASIL">Brasil</mat-option><mat-option value="EUA">EUA</mat-option></mat-select><mat-hint id="mercado-hint">Selecione Brasil ou EUA.</mat-hint>@if(form.controls.mercado.invalid&&form.controls.mercado.touched){<mat-error>Mercado é obrigatório.</mat-error>}</mat-form-field>
        <div class="app-actions app-actions--stack-compact"><button mat-flat-button type="submit" [disabled]="submitting()" [attr.aria-busy]="submitting()">Cadastrar ação</button>@if(isDialog){<button mat-button type="button" (click)="cancel()">Cancelar</button>}@else{<a mat-button routerLink="/acoes">Cancelar</a>}</div>
        @if(submitting()){<div role="status" aria-live="polite" class="progress"><mat-spinner diameter="28"/>Processando cadastro…</div>}
      </form>
    </section>`,
  styles: [`form{display:grid;gap:1.25rem}.progress{display:flex;align-items:center;gap:.75rem}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AcaoCreatePageComponent {
  private readonly service = inject(AcoesService);
  private readonly successToast = inject(SuccessToastService);
  private readonly router = inject(Router);
  private readonly dialogRef = inject(MatDialogRef<AcaoCreatePageComponent, AcaoResponse | undefined>, { optional: true });
  private readonly dialogData = inject<AcaoCreateDialogData | null>(MAT_DIALOG_DATA, { optional: true });
  private readonly destroyRef = inject(DestroyRef);
  protected readonly submitting = signal(false);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly isDialog = this.dialogRef !== null;
  protected readonly form = new FormGroup({ ticker: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(30)] }), mercado: new FormControl<Mercado | null>(null, { validators: [Validators.required] }) });

  constructor() {
    const prefill = this.dialogPrefill() ?? this.navigationPrefill();
    if (prefill) this.form.setValue(prefill);
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    const mercado = this.form.controls.mercado.value;
    if (this.form.invalid || !mercado || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    this.service.criar({ ticker: normalizeTicker(this.form.controls.ticker.value), mercado }).pipe(finalize(() => this.submitting.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (acao) => {
        if (this.dialogRef) { this.dialogRef.close(acao); return; }
        this.successToast.show('Ação cadastrada com sucesso.');
        void this.router.navigate(['/acoes', acao.id], { info: { acao } });
      },
      error: (error: NormalizedHttpError) => this.error.set(error)
    });
  }

  protected cancel(): void { this.dialogRef?.close(); }

  private dialogPrefill(): { ticker: string; mercado: Mercado } | null {
    const data = this.dialogData;
    if (!data || typeof data.ticker !== 'string' || (data.mercado !== 'BRASIL' && data.mercado !== 'EUA')) return null;
    const ticker = normalizeTicker(data.ticker);
    return ticker.length > 0 && ticker.length <= 30 ? { ticker, mercado: data.mercado } : null;
  }

  private navigationPrefill(): { ticker: string; mercado: Mercado } | null {
    const info = this.router.currentNavigation()?.extras.info;
    if (!info || typeof info !== 'object' || !('ticker' in info) || !('mercado' in info)) return null;
    if (typeof info.ticker !== 'string' || (info.mercado !== 'BRASIL' && info.mercado !== 'EUA')) return null;
    const ticker = normalizeTicker(info.ticker);
    return ticker.length > 0 && ticker.length <= 30 ? { ticker, mercado: info.mercado } : null;
  }
}
