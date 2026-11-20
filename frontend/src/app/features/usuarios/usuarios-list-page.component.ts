import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { FeedbackAlertComponent } from '../../shared/feedback-alert/feedback-alert.component';
import { AppIconComponent } from '../../shared/app-icon/app-icon.component';
import { Usuario } from './usuario.model';
import { UsuariosApi } from './usuarios-api.service';

@Component({ host: { class: 'app-blue-scope' },
  selector: 'app-usuarios-list-page',
  imports: [MatButtonModule, RouterLink, FeedbackAlertComponent, AppIconComponent],
  styleUrl: './usuarios-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<section class="app-page" aria-labelledby="usuarios-title">
    <header class="admin-heading">
      <div><p class="admin-context">Administração</p><h1 id="usuarios-title">Usuários</h1></div>
      <a mat-flat-button routerLink="/usuarios/novo"><span aria-hidden="true">＋</span> Novo usuário</a>
      <p class="admin-description">Contas e permissões de acesso.</p>
    </header>
    <div class="accounts-collection" role="region" aria-labelledby="contas-title">
    @if (!loading() && !error()) {
      <dl class="user-summary" aria-label="Resumo de usuários">
        <div><dt><app-icon name="identity" aria-hidden="true"/><span>Total<span class="summary-prefix"> de usuários</span></span></dt><dd>{{ users().length }}</dd></div>
        <div class="summary-active"><dt><app-icon name="success" aria-hidden="true"/><span><span class="summary-prefix">Usuários </span>ativos</span></dt><dd>{{ activeCount() }}</dd></div>
        <div class="summary-blocked"><dt><app-icon name="error" aria-hidden="true"/><span><span class="summary-prefix">Usuários </span>bloqueados</span></dt><dd>{{ users().length - activeCount() }}</dd></div>
      </dl>
    }
    <div class="collection-panel">
    <div class="collection-overview"><h2 id="contas-title">Contas cadastradas</h2></div>
    @if (error()) { <app-feedback-alert variant="error" [message]="error()"/><button mat-stroked-button type="button" (click)="load()">Tentar novamente</button> }
    @if (loading()) { <p role="status">Carregando usuários…</p> }
    @else if (!error() && users().length === 0) { <p role="status">Nenhuma conta disponível para consulta.</p> }
    @else if (!error()) {
      <div class="table-scroll"><table class="collection-table" role="table"><caption>Contas cadastradas</caption>
        <thead><tr><th scope="col">Usuário</th><th scope="col">Perfil</th><th scope="col">Situação</th><th scope="col">Senha</th><th scope="col">Ações</th></tr></thead>
        <tbody>@for (user of users(); track user.id) {
          <tr role="row">
            <th scope="row" role="rowheader"><span class="account-identity"><span class="account-avatar" aria-hidden="true">{{ monogram(user.username) }}</span><span class="account-name">@for (part of nameParts(user.username); track $index) {<span>{{ part }}</span>@if (!$last) {<wbr>}}</span></span></th>
            <td role="cell"><span class="cell-label">Perfil</span><span class="role-badge" [class.role-badge--admin]="user.papel === 'ADMIN'">{{ user.papel }}</span></td>
            <td role="cell"><span class="cell-label">Situação</span><span class="status-badge" [class.status-badge--positive]="user.ativo" [class.status-badge--error]="!user.ativo">{{ user.ativo ? 'Ativo' : 'Bloqueado' }}</span></td>
            <td role="cell"><span class="cell-label">Senha</span><span class="credential" [class.credential--pending]="!user.credencialDefinida">{{ user.credencialDefinida ? 'Definida' : 'Pendente' }}</span></td>
            <td role="cell"><span class="cell-label action-label">Ações</span><a mat-stroked-button class="account-link" [routerLink]="['/usuarios', user.id]" [attr.aria-label]="'Consultar usuário ' + user.username">Ver usuário <span aria-hidden="true">→</span></a></td>
          </tr>
        }</tbody>
      </table></div>
    }
    </div>
    </div>
  </section>`
})
export class UsuariosListPageComponent {
  private readonly api = inject(UsuariosApi);
  private readonly destroy = inject(DestroyRef);
  protected readonly users = signal<Usuario[]>([]);
  protected readonly activeCount = computed(() => this.users().filter(user => user.ativo).length);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  constructor() { this.load(); }

  protected monogram(username: string): string {
    const parts = username.split(/[._-]+/).filter(Boolean);
    return (parts.length > 1 ? parts[0][0] + parts[1][0] : (parts[0] ?? username).slice(0, 2)).toUpperCase();
  }

  protected nameParts(username: string): string[] {
    return username.match(/[^._-]+[._-]*|[._-]+/g) ?? [username];
  }

  protected load(): void {
    if (this.loading()) return;
    this.loading.set(true); this.error.set('');
    this.api.listar().pipe(takeUntilDestroyed(this.destroy), finalize(() => this.loading.set(false))).subscribe({
      next: users => this.users.set(users),
      error: (error: { status?: number }) => { this.users.set([]); this.error.set(error.status === 403 ? 'Você não possui permissão para consultar usuários.' : 'Não foi possível consultar usuários. Tente novamente quando o serviço estiver disponível.'); }
    });
  }
}
