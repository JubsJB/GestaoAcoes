import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog } from '@angular/material/dialog';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { SessionService } from '../../core/auth/session.service';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { Papel } from './usuario.model';
import { UsuariosApi } from './usuarios-api.service';
import { OneTimeCodeDialogComponent } from './one-time-code-dialog.component';

@Component({
  selector: 'app-usuario-create-page', changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, RouterLink, PageHeaderComponent], styleUrl: './usuarios-page.scss',
  template: `<section><app-page-header title="Cadastrar usuário" icon="identity" description="O próprio usuário define sua senha com um código de uso único." />
    @if (error()) { <p role="alert">{{ error() }}</p> }
    <form [formGroup]="form" (ngSubmit)="submit()" [attr.aria-busy]="busy()">
      <mat-form-field><mat-label>Nome de usuário</mat-label><input matInput name="username" formControlName="username" autocomplete="off" required><mat-hint>3 a 100 letras, números, pontos, hífens ou sublinhados.</mat-hint><mat-error>{{ form.controls.username.hasError('duplicate') ? 'Nome de usuário já utilizado.' : 'Informe um nome de usuário válido.' }}</mat-error></mat-form-field>
      <mat-form-field><mat-label>Perfil</mat-label><mat-select formControlName="papel"><mat-option value="USER">USER — somente consulta</mat-option><mat-option value="ADMIN">ADMIN — administração e alterações</mat-option></mat-select></mat-form-field>
      <div class="actions"><a mat-button routerLink="/usuarios">Cancelar</a><button mat-flat-button type="submit" [disabled]="busy()">{{ busy() ? 'Cadastrando…' : 'Cadastrar e gerar código' }}</button></div>
    </form></section>`
})
export class UsuarioCreatePageComponent {
  private readonly api = inject(UsuariosApi); private readonly session = inject(SessionService);
  private readonly dialog = inject(MatDialog); private readonly router = inject(Router); private readonly destroy = inject(DestroyRef);
  protected readonly form = inject(FormBuilder).nonNullable.group({ username: ['', [Validators.required, control => /^[a-z0-9._-]{3,100}$/.test(String(control.value).trim().toLowerCase()) ? null : { username: true }]], papel: ['USER' as Papel, Validators.required] });
  protected readonly busy = signal(false); protected readonly error = signal('');
  constructor() { this.session.changes$.pipe(takeUntilDestroyed(this.destroy)).subscribe(() => this.form.reset()); }
  protected submit(): void {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true); this.error.set(''); const generation = this.session.generation;
    this.api.cadastrar(this.form.getRawValue()).pipe(takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))).subscribe({
      next: emission => {
        if (generation !== this.session.generation || !this.session.admin()) return;
        const id = emission.usuario.id;
        this.form.reset();
        this.dialog.open(OneTimeCodeDialogComponent, { data: { username: emission.usuario.username, ...emission.autorizacao }, closeOnNavigation: true, width: '36rem', maxWidth: '95vw' }).afterClosed().pipe(takeUntilDestroyed(this.destroy)).subscribe(() => {
          if (this.session.generation === generation && this.session.admin()) void this.router.navigate(['/usuarios', id]);
        });
      },
      error: (error: { status?: number; code?: string }) => {
        if (generation !== this.session.generation) return;
        if (error.status === 409 && error.code === 'USUARIO_DUPLICADO') { this.form.controls.username.setErrors({ duplicate: true }); this.form.controls.username.markAsTouched(); this.error.set('Nome de usuário já utilizado. Escolha outro.'); }
        else this.error.set(error.status === 403 ? 'A operação não foi autorizada. Confira sua sessão e envie novamente se permitido.' : 'Não foi possível confirmar o cadastro. Consulte a lista antes de tentar novamente; um código emitido não pode ser recuperado.');
      }
    });
  }
}
