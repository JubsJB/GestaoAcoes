import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize, Observable, switchMap } from 'rxjs';
import { SessionService } from '../../core/auth/session.service';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { AdminConfirmDialogComponent } from './admin-confirm-dialog.component';
import { OneTimeCodeDialogComponent } from './one-time-code-dialog.component';
import { EmissaoAutorizacao, Papel, Usuario } from './usuario.model';
import { UsuariosApi } from './usuarios-api.service';

type Action = 'perfil' | 'situacao' | 'codigo';

@Component({ selector: 'app-usuario-detail-page', changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatSelectModule, RouterLink, PageHeaderComponent], styleUrl: './usuarios-page.scss',
  template: `<section><app-page-header title="Consultar usuário" icon="identity" /><a mat-button routerLink="/usuarios">Voltar à lista</a>
    @if (error()) { <p role="alert">{{ error() }}</p> }
    @if (notice()) { <p role="status">{{ notice() }}</p> }
    <button mat-button (click)="reload()" [disabled]="busy()">Atualizar dados</button>
    @if (loading()) { <p role="status">Carregando usuário…</p> }
    @if (user(); as user) {
      <h2>{{ user.username }}</h2><dl><dt>Situação</dt><dd>{{ user.ativo ? 'Ativo' : 'Bloqueado' }}</dd><dt>Credencial</dt><dd>{{ user.credencialDefinida ? 'Senha definida' : 'Definição inicial pendente' }}</dd><dt>Versão</dt><dd>{{ user.versao }}</dd></dl>
      <form (submit)="$event.preventDefault(); confirm('perfil')" [attr.aria-busy]="busy()">
        <mat-form-field><mat-label>Perfil</mat-label><mat-select [formControl]="role"><mat-option value="USER">USER — somente consulta</mat-option><mat-option value="ADMIN">ADMIN — administração e alterações</mat-option></mat-select></mat-form-field>
        <button mat-flat-button type="submit" data-testid="save-role" [disabled]="busy() || uncertain() || loading()">Salvar perfil</button>
      </form>
      <div class="actions"><button mat-stroked-button data-testid="toggle-status" (click)="confirm('situacao')" [disabled]="busy() || uncertain() || loading()">{{ user.ativo ? 'Bloquear' : 'Habilitar' }}</button>
      <button mat-stroked-button data-testid="issue-code" (click)="confirm('codigo')" [disabled]="!user.ativo || busy() || uncertain() || loading()">{{ user.credencialDefinida ? 'Autorizar redefinição' : 'Gerar código inicial' }}</button></div>
      <p>Alterações efetivas de perfil ou bloqueio encerram as sessões anteriores. Habilitar não restaura sessões ou códigos antigos.</p>
    }
  </section>` })
export class UsuarioDetailPageComponent {
  private readonly api = inject(UsuariosApi); private readonly session = inject(SessionService);
  private readonly dialog = inject(MatDialog); private readonly destroy = inject(DestroyRef); private readonly route = inject(ActivatedRoute);
  private id = 0; private revision = 0;
  protected readonly user = signal<Usuario | null>(null); protected readonly loading = signal(false); protected readonly busy = signal(false);
  protected readonly uncertain = signal(false); protected readonly error = signal(''); protected readonly notice = signal('');
  protected readonly role = new FormControl<Papel>('USER', { nonNullable: true });
  constructor() {
    this.route.paramMap.pipe(switchMap(params => {
      this.id = Number(params.get('id')); this.revision++; this.user.set(null); this.error.set(''); this.notice.set(''); this.busy.set(false);
      if (!Number.isSafeInteger(this.id) || this.id <= 0) { this.error.set('Identificador de usuário inválido.'); return EMPTY; }
      this.loading.set(true); return this.api.buscar(this.id).pipe(catchError(error => { this.failure(error); return EMPTY; }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe({ next: user => this.accept(user), error: error => this.failure(error) });
    this.session.changes$.pipe(takeUntilDestroyed(this.destroy)).subscribe(() => { this.revision++; this.user.set(null); this.role.reset(); this.error.set(''); this.notice.set(''); });
  }
  protected reload(): void {
    if (this.busy() || !Number.isSafeInteger(this.id) || this.id <= 0) return;
    const revision = ++this.revision; this.loading.set(true); this.error.set(''); this.notice.set('');
    this.api.buscar(this.id).pipe(takeUntilDestroyed(this.destroy), finalize(() => { if (revision === this.revision) this.loading.set(false); })).subscribe({ next: user => { if (revision === this.revision) this.accept(user); }, error: error => { if (revision === this.revision) this.failure(error); } });
  }
  private accept(user: Usuario): void { this.user.set(user); this.role.setValue(user.papel); this.uncertain.set(false); }
  protected confirm(action: Action): void {
    const before = this.user(); if (!before || this.busy() || this.loading() || this.uncertain()) return;
    const papel = this.role.value; const revision = this.revision; const generation = this.session.generation;
    this.busy.set(true); this.error.set(''); this.notice.set('');
    const title = action === 'perfil' ? 'Confirmar perfil' : action === 'codigo' ? 'Emitir novo código' : before.ativo ? 'Bloquear usuário' : 'Habilitar usuário';
    const message = action === 'codigo' ? `Gerar um código para ${before.username}? O anterior será invalidado e o novo será exibido somente uma vez.` : `${title}: ${before.username}. Alterações efetivas de perfil ou bloqueio invalidam as sessões antigas. Habilitar não recupera códigos ou sessões.`;
    this.dialog.open(AdminConfirmDialogComponent, { data: { title, message }, closeOnNavigation: true }).afterClosed().pipe(takeUntilDestroyed(this.destroy)).subscribe(confirmed => {
      if (revision !== this.revision || generation !== this.session.generation || !this.session.admin()) { this.busy.set(false); return; }
      if (!confirmed) { this.busy.set(false); return; }
      const request: Observable<Usuario | EmissaoAutorizacao> = action === 'codigo' ? this.api.autorizar(before.id, before.versao) : action === 'perfil' ? this.api.perfil(before.id, papel, before.versao) : this.api.situacao(before.id, !before.ativo, before.versao);
      request.pipe(takeUntilDestroyed(this.destroy), finalize(() => { if (revision === this.revision) this.busy.set(false); })).subscribe({
        next: response => {
          if (revision !== this.revision || generation !== this.session.generation) return;
          const updated = 'usuario' in response ? response.usuario : response;
          this.accept(updated); this.notice.set('Operação concluída.');
          if ('autorizacao' in response) this.dialog.open(OneTimeCodeDialogComponent, { data: { username: updated.username, ...response.autorizacao }, closeOnNavigation: true, width: '36rem', maxWidth: '95vw' });
          if ((before.papel !== updated.papel || before.ativo !== updated.ativo) && updated.id === this.session.identity()?.id) this.session.passwordConfirmed();
        }, error: error => { if (revision === this.revision) this.failure(error); }
      });
    });
  }
  private failure(error: { status?: number; code?: string }): void {
    this.uncertain.set(true);
    this.error.set(error.code === 'ULTIMO_ADMIN_ATIVO' ? 'A operação eliminaria o último administrador ativo com senha definida.' : error.status === 409 ? 'Os dados mudaram ou a conta não permite esta operação. Atualize os dados antes de tentar novamente.' : error.status === 404 ? 'Usuário não encontrado.' : error.status === 403 ? 'A operação não foi autorizada. Atualize os dados e verifique sua sessão.' : 'Não foi possível confirmar o resultado. Atualize os dados antes de realizar outra operação. Códigos emitidos não podem ser recuperados.');
  }
}
