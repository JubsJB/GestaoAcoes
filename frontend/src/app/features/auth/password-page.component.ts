import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ErrorStateMatcher } from '@angular/material/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthApi } from '../../core/auth/auth-api.service';
import { SessionService } from '../../core/auth/session.service';
import { matchingPasswords, passwordPolicy } from './password-validators';
import { RetryAfterState } from '../../core/http/retry-after-state';

@Component({
  selector: 'app-password-page',
  providers: [RetryAfterState],
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, RouterLink],
  styleUrl: './auth-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<section aria-labelledby="password-title">
    <p>Gestão de Ações</p><h1 id="password-title">{{ initial ? 'Definir senha' : 'Redefinir senha' }}</h1>
    @if (success()) {
      <h2 role="status">Senha definida com sucesso</h2><p>Entre com sua senha para continuar. Esta confirmação não inicia uma sessão.</p><a mat-flat-button routerLink="/login">Entrar</a>
    } @else {
      <p>Informe o código recebido do administrador. Letras maiúsculas ou minúsculas, espaços e hífens são aceitos.</p>
      @if (error()) { <p role="alert">{{ error() }}</p> }
      @if (retry.remaining()) { <p role="status">Tente novamente em {{ retry.remaining() }} segundos.</p> }
      <form [formGroup]="form" (ngSubmit)="submit()" [attr.aria-busy]="busy()">
        <mat-form-field><mat-label>Nome de usuário</mat-label><input matInput name="username" formControlName="username" autocomplete="username" required><mat-error>Informe seu nome de usuário.</mat-error></mat-form-field>
        <mat-form-field><mat-label>Código autorizado</mat-label><input matInput name="codigo" formControlName="codigo" autocomplete="one-time-code" spellcheck="false" required><mat-error>Informe o código recebido.</mat-error></mat-form-field>
        <mat-form-field><mat-label>Nova senha</mat-label><input matInput name="senha" formControlName="senha" type="password" autocomplete="new-password" required><mat-hint>No mínimo 8 caracteres; até 72 bytes UTF-8, sem remover espaços.</mat-hint><mat-error>Use no mínimo 8 caracteres e no máximo 72 bytes UTF-8.</mat-error></mat-form-field>
        <mat-form-field><mat-label>Confirme a nova senha</mat-label><input matInput name="confirmacao" formControlName="confirmacao" type="password" autocomplete="new-password" [errorStateMatcher]="confirmationMatcher" required><mat-error>A confirmação deve corresponder exatamente à nova senha.</mat-error></mat-form-field>
        <button mat-flat-button type="submit" [disabled]="busy() || retry.remaining() > 0">{{ busy() ? 'Confirmando…' : 'Definir senha' }}</button>
      </form>
      <nav aria-label="Voltar ao acesso"><a routerLink="/login">Voltar ao login</a></nav>
    }
  </section>`
})
export class PasswordPageComponent {
  private readonly api = inject(AuthApi);
  private readonly session = inject(SessionService);
  private readonly destroy = inject(DestroyRef);
  protected readonly retry = inject(RetryAfterState);
  protected readonly initial = inject(ActivatedRoute).snapshot.data['mode'] === 'initial';
  protected readonly form = inject(FormBuilder).nonNullable.group({ username: ['', Validators.required], codigo: ['', Validators.required], senha: ['', [Validators.required, passwordPolicy]], confirmacao: ['', Validators.required] }, { validators: matchingPasswords });
  protected readonly confirmationMatcher: ErrorStateMatcher = { isErrorState: control => Boolean(control?.touched && (control.invalid || this.form.hasError('confirmationMismatch'))) };
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal(false);

  constructor() {
    this.session.changes$.pipe(takeUntilDestroyed(this.destroy)).subscribe(() => { this.form.reset(); this.error.set(''); });
  }

  protected submit(): void {
    if (this.busy() || this.retry.remaining() > 0) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const values = this.form.getRawValue();
    const command = { ...values, username: values.username.trim().toLowerCase() };
    this.form.controls.senha.reset(); this.form.controls.confirmacao.reset(); this.form.controls.codigo.reset();
    this.busy.set(true); this.error.set('');
    this.api.confirmPassword(command).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))).subscribe({
      next: () => { this.session.passwordConfirmed(); this.form.reset(); this.success.set(true); },
      error: (error: { status?: number; code?: string }) => {
        this.retry.apply(error);
        this.error.set(error.status === 400 && error.code === 'SENHA_INVALIDA' ? 'Confira a nova senha e sua confirmação: no mínimo 8 caracteres e no máximo 72 bytes UTF-8.' : error.status === 403 ? 'Não foi possível validar a proteção do formulário. Envie novamente.' : error.status === 429 ? 'Muitas tentativas. Aguarde antes de enviar novamente.' : error.status === 503 || error.status === 0 ? 'Serviço indisponível. Não foi possível confirmar o resultado.' : 'Não foi possível confirmar a definição de senha. Confira os dados ou solicite um novo código ao administrador.');
      }
    });
  }
}
