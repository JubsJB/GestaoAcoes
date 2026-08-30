import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, finalize, switchMap } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { CorretoraConfirmDialogComponent } from '../corretora-confirm-dialog.component';
import { formatCnpj, onlyDigits } from '../corretora-formatters';
import { Corretora, CorretoraCreateRequest } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';

@Component({
  selector: 'app-corretora-create-page',
  imports: [MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page" aria-labelledby="create-title">
      <a mat-button routerLink="..">← Voltar para corretoras</a>
      <div><h1 id="create-title">Cadastrar corretora</h1><p>Informe somente o CNPJ. Os dados cadastrais serão validados e preenchidos pelo backend.</p></div>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>CNPJ</mat-label>
          <input matInput formControlName="cnpj" inputmode="numeric" maxlength="18" (input)="formatInput()" aria-describedby="cnpj-hint" />
          <mat-hint id="cnpj-hint">14 dígitos, com ou sem máscara.</mat-hint>
          @if (form.controls.cnpj.invalid && form.controls.cnpj.touched) { <mat-error>Informe um CNPJ com 14 dígitos.</mat-error> }
        </mat-form-field>
        @if (errorMessage()) { <p role="alert" class="error">{{ errorMessage() }}</p> }
        <div class="actions"><button mat-flat-button type="submit" [disabled]="submitting()">Cadastrar</button><a mat-button routerLink="..">Cancelar</a></div>
        @if (submitting()) { <div role="status" class="progress"><mat-spinner diameter="28" /> Processando cadastro…</div> }
      </form>
    </section>
  `,
  styles: [`.page{max-width:42rem;display:grid;gap:1.25rem}h1{margin-bottom:.25rem}form{display:grid;gap:1rem}.actions,.progress{display:flex;align-items:center;gap:.75rem}.error{color:var(--mat-sys-error)}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CorretoraCreatePageComponent {
  private readonly service = inject(CorretorasService); private readonly dialog = inject(MatDialog); private readonly snackBar = inject(MatSnackBar); private readonly router = inject(Router); private readonly destroyRef = inject(DestroyRef);
  protected readonly submitting = signal(false); protected readonly errorMessage = signal<string | null>(null);
  protected readonly form = new FormGroup({ cnpj: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^(?:\D*\d){14}\D*$/)] }) });

  protected formatInput(): void { const control=this.form.controls.cnpj; control.setValue(formatCnpj(control.value), { emitEvent:false }); }
  protected submit(): void { this.form.markAllAsTouched(); if (this.form.invalid || this.submitting()) return; this.send({cnpj: onlyDigits(this.form.controls.cnpj.value)}); }
  private send(request: CorretoraCreateRequest): void {
    this.submitting.set(true); this.errorMessage.set(null);
    this.service.cadastrar(request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next:(broker)=>this.complete(broker),error:(error:NormalizedHttpError)=>this.handleError(error,request.cnpj)});
  }
  private handleError(error: NormalizedHttpError, cnpj:string): void {
    if (error.status !== 409 || error.code !== 'SITUACAO_CADASTRAL_NAO_ATIVA') { this.errorMessage.set(error.message); this.submitting.set(false); return; }
    const status=typeof error.details['situacaoCadastral']==='string' ? error.details['situacaoCadastral'] : null;
    this.dialog.open(CorretoraConfirmDialogComponent,{data:{message:error.message,situacaoCadastral:status},restoreFocus:true}).afterClosed().pipe(
      switchMap((confirmed) => confirmed === true
        ? this.service.cadastrar({cnpj,confirmarSituacaoCadastralNaoAtiva:true})
        : EMPTY),
      finalize(() => this.submitting.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({next:(broker)=>this.complete(broker),error:(nextError:NormalizedHttpError)=>this.errorMessage.set(nextError.message)});
  }
  private complete(corretora:Corretora):void { this.snackBar.open('Corretora cadastrada com sucesso.','Fechar',{duration:5000}); void this.router.navigate(['/corretoras',corretora.id],{info:{corretora}}).finally(() => this.submitting.set(false)); }
}
