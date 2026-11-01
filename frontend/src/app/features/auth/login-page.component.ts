import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { finalize, Observable, of, switchMap } from 'rxjs';
import { SessionService } from '../../core/auth/session.service';
import { ReturnNavigationService } from '../../core/auth/return-navigation.service';
import { RetryAfterState } from '../../core/http/retry-after-state';
import { AUTH_BROWSER } from '../../core/auth/auth-browser';

@Component({
  selector: 'app-login-page',
  providers: [RetryAfterState],
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './auth-page.scss',
  template: `<section aria-labelledby="login-title">
    <p>Gestão de Ações</p><h1 id="login-title">Entrar</h1>
    <p>Acesse suas carteiras com segurança.</p>
    @if (error()) { <p role="alert">{{ error() }}</p> }
    @if (retry.remaining()) { <p role="status">Tente novamente em {{ retry.remaining() }} segundos.</p> }
    @if (session.authenticated()) {
      <p role="status">Sessão validada para {{ session.identity()!.username }}.</p>
      <button mat-stroked-button type="button" data-testid="continue-session" [disabled]="busy()" (click)="continueSession($event)">Continuar</button>
    }
    <form [formGroup]="form" (ngSubmit)="submit()" [attr.aria-busy]="busy()">
      <mat-form-field><mat-label>Nome de usuário</mat-label>
        <input matInput name="username" formControlName="username" autocomplete="username" maxlength="100" required>
        @if (form.controls.username.touched && form.controls.username.invalid) { <mat-error>Informe o nome de usuário.</mat-error> }
      </mat-form-field>
      <mat-form-field><mat-label>Senha</mat-label>
        <input matInput name="senha" type="password" formControlName="senha" autocomplete="current-password" required>
        @if (form.controls.senha.touched && form.controls.senha.invalid) { <mat-error>Informe sua senha.</mat-error> }
      </mat-form-field>
      <button mat-flat-button type="submit" [disabled]="busy() || retry.remaining() > 0">{{ busy() ? 'Entrando…' : 'Entrar' }}</button>
    </form>
    <nav aria-label="Ajuda de acesso"><a routerLink="/definir-senha">Primeiro acesso</a><a routerLink="/redefinir-senha">Redefinir senha</a></nav>
  </section>`
})
export class LoginPageComponent {
  protected readonly session = inject(SessionService);
  protected readonly retry = inject(RetryAfterState);
  private readonly browser = inject(AUTH_BROWSER);
  private readonly router = inject(Router);
  private readonly returns = inject(ReturnNavigationService);
  private readonly destroy = inject(DestroyRef);
  protected readonly form = inject(FormBuilder).nonNullable.group({ username: ['', Validators.required], senha: ['', Validators.required] });
  protected readonly busy = signal(false);
  protected readonly error = signal('');

  constructor() {
    this.session.changes$.pipe(takeUntilDestroyed(this.destroy)).subscribe(() => {
      this.form.reset();
      this.error.set('');
    });
    if (this.router.currentNavigation()?.extras.state?.['logoutUnconfirmed']) {
      this.error.set('O acesso local foi encerrado. A revogação no servidor não pôde ser confirmada; outras abas podem continuar autenticadas.');
    }
  }

  protected submit(): void {
    if (this.busy() || this.retry.remaining() > 0) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const { username, senha } = this.form.getRawValue();
    this.form.controls.senha.reset();
    this.busy.set(true); this.error.set('');
    this.complete(this.session.login(username, senha), true);
  }

  protected continueSession(event?: Event): void {
    if (this.busy() || !this.session.authenticated()) return;
    this.form.reset(); this.busy.set(true); this.error.set('');
    const generation = this.session.generation;
    const trusted = event?.isTrusted === true && this.browser.visible();
    this.complete(this.session.restore().pipe(switchMap(valid => valid && trusted && generation === this.session.generation ? this.session.activity() : of(valid))));
  }

  private complete(authentication: Observable<boolean>, credentialAttempt = false): void {
    authentication.pipe(
      switchMap(valid => {
        const identity = this.session.identity();
        return valid && identity ? this.returns.destination(identity.id, identity.papel) : of(null);
      }),
      takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))
    ).subscribe({
      next: destination => {
        if (destination) void this.router.navigateByUrl(destination);
        else this.error.set('Não foi possível validar a sessão. Tente novamente.');
      },
      error: (error: { status?: number }) => {
        this.retry.apply(error);
        this.error.set(error.status === 401 ? credentialAttempt ? 'Usuário ou senha inválidos.' : 'A sessão expirou. Entre novamente para continuar.' : error.status === 429 ? 'Muitas tentativas. Aguarde o prazo informado pelo servidor antes de tentar novamente.' : error.status === 403 ? 'A proteção da sessão precisa ser atualizada. Envie o formulário novamente.' : 'Serviço de autenticação indisponível. Tente novamente.');
      }
    });
  }
}
