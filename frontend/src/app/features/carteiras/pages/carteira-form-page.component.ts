import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { StickyBackComponent } from '../../../shared/sticky-back/sticky-back.component';
import { CarteirasService } from '../carteiras.service';
import { CarteiraResponse } from '../models/carteira';

export interface CarteiraFormDialogData {
  readonly mode: 'create' | 'edit';
  readonly carteira?: CarteiraResponse;
}

function nonBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim().length > 0 ? null : { blank: true };
}

@Component({
  selector: 'app-carteira-form-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, PageHeaderComponent, ReactiveFormsModule, RouterLink, StickyBackComponent],
  template: `
    <section class="app-page" [class.app-dialog-page]="isDialog" aria-labelledby="carteira-form-title" aria-describedby="carteira-form-description">
      @if (!isDialog) { <app-sticky-back route="/carteiras" label="Voltar para carteiras" /> }
      <app-page-header headingId="carteira-form-title" eyebrow="Carteiras" icon="portfolio" [title]="isEdit ? 'Editar carteira' : 'Nova carteira'" [description]="isEdit ? 'Altere somente o nome da carteira.' : 'Crie uma carteira para organizar seus investimentos.'" />
      <p id="carteira-form-description" class="sr-only">{{ isEdit ? 'Formulário para editar o nome da carteira.' : 'Formulário para cadastrar uma carteira.' }}</p>

      @if (error()) { <app-feedback-alert variant="error" [message]="error()!.message" [details]="error()!.details" /> }
      @if (loading()) {
        <div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36" /> Carregando carteira…</div>
      } @else {
        <form class="app-form-surface app-surface" [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="surface-heading"><app-icon name="identity" aria-hidden="true" /><div><h2>Identificação</h2><p>O nome pode ser alterado depois.</p></div></div>
          <mat-form-field appearance="outline">
            <mat-label>Nome</mat-label>
            <input matInput formControlName="nome" maxlength="255" autocomplete="off" aria-describedby="carteira-name-hint" />
            <mat-hint id="carteira-name-hint">Até 255 caracteres.</mat-hint>
            @if (form.controls.nome.invalid && form.controls.nome.touched) { <mat-error>Informe um nome não vazio com até 255 caracteres.</mat-error> }
          </mat-form-field>
          <div class="app-actions app-actions--stack-compact">
            <button mat-flat-button type="submit" [disabled]="submitting()" [attr.aria-busy]="submitting()">{{ isEdit ? 'Salvar alterações' : 'Cadastrar' }}</button>
            @if (isDialog) { <button mat-button type="button" (click)="cancel()">Cancelar</button> }
            @else { <a mat-button routerLink="/carteiras">Cancelar</a> }
          </div>
          @if (submitting()) { <div class="progress" role="status" aria-live="polite"><mat-spinner diameter="28" /> Salvando carteira…</div> }
        </form>
      }
    </section>
  `,
  styles: [`form{display:grid;gap:1.25rem;min-width:0}mat-form-field{width:100%}.progress{display:flex;align-items:center;gap:.75rem}.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CarteiraFormPageComponent {
  private readonly service = inject(CarteirasService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly successToast = inject(SuccessToastService);
  private readonly dialogRef = inject(MatDialogRef<CarteiraFormPageComponent, CarteiraResponse | undefined>, { optional: true });
  private readonly dialogData = inject<CarteiraFormDialogData | null>(MAT_DIALOG_DATA, { optional: true });
  private readonly destroyRef = inject(DestroyRef);
  protected readonly isDialog = this.dialogRef !== null;
  protected readonly isEdit = this.dialogData?.mode === 'edit' || this.route.snapshot.data['mode'] === 'edit';
  private readonly id = this.dialogData?.carteira?.id ?? Number(this.route.snapshot.paramMap.get('id'));
  protected readonly loading = signal(this.isEdit && !this.dialogData?.carteira);
  protected readonly submitting = signal(false);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly form = new FormGroup({ nome: new FormControl('', { nonNullable: true, validators: [Validators.required, nonBlank, Validators.maxLength(255)] }) });

  constructor() {
    if (this.dialogData?.carteira) this.form.controls.nome.setValue(this.dialogData.carteira.nome);
    else if (this.isEdit) this.load();
  }

  private load(): void {
    this.error.set(null);
    this.service.buscarPorId(this.id).pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (carteira) => this.form.controls.nome.setValue(carteira.nome),
      error: (error: NormalizedHttpError) => this.error.set(error)
    });
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    const request = { nome: this.form.controls.nome.value.trim() };
    const operation = this.isEdit ? this.service.atualizar(this.id, request) : this.service.cadastrar(request);
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (carteira) => this.complete(carteira),
      error: (error: NormalizedHttpError) => { this.error.set(error); this.submitting.set(false); }
    });
  }

  private complete(carteira: CarteiraResponse): void {
    if (this.dialogRef) { this.dialogRef.close(carteira); this.submitting.set(false); return; }
    void this.router.navigate(['/carteiras', carteira.id], { info: { carteira } }).then((navigated) => {
      if (navigated) this.successToast.show(this.isEdit ? 'Carteira atualizada com sucesso.' : 'Carteira cadastrada com sucesso.');
    }).finally(() => this.submitting.set(false));
  }

  protected cancel(): void { this.dialogRef?.close(); }
}
