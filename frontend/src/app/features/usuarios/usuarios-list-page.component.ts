import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { FeedbackAlertComponent } from '../../shared/feedback-alert/feedback-alert.component';
import { Usuario } from './usuario.model';
import { UsuariosApi } from './usuarios-api.service';

@Component({
  selector: 'app-usuarios-list-page',
  imports: [MatButtonModule, RouterLink, PageHeaderComponent, FeedbackAlertComponent],
  styleUrl: './usuarios-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<section class="app-page" aria-labelledby="usuarios-title">
    <app-page-header headingId="usuarios-title" icon="identity" eyebrow="Administração" title="Usuários" description="Gerencie o acesso às carteiras compartilhadas.">
      <a page-header-action mat-flat-button routerLink="/usuarios/novo">Novo usuário</a>
    </app-page-header>
    @if (error()) { <app-feedback-alert variant="error" [message]="error()"/><button mat-stroked-button type="button" (click)="load()">Tentar novamente</button> }
    @if (loading()) { <p role="status">Carregando usuários…</p> }
    @else if (!error() && users().length === 0) { <p role="status">Nenhuma conta disponível para consulta.</p> }
    @else if (!error()) {
      <div class="table-scroll app-surface"><table class="collection-table"><caption>Contas cadastradas</caption>
        <thead><tr><th scope="col">Usuário</th><th scope="col">Perfil</th><th scope="col">Situação</th><th scope="col">Senha</th><th scope="col">Ações</th></tr></thead>
        <tbody>@for (user of users(); track user.id) { <tr><th scope="row">{{ user.username }}</th><td>{{ user.papel }}</td><td>{{ user.ativo ? 'Ativo' : 'Bloqueado' }}</td><td>{{ user.credencialDefinida ? 'Definida' : 'Pendente' }}</td><td><a [routerLink]="['/usuarios', user.id]" [attr.aria-label]="'Consultar usuário ' + user.username">Ver usuário</a></td></tr> }</tbody>
      </table></div>
    }
  </section>`
})
export class UsuariosListPageComponent {
  private readonly api = inject(UsuariosApi);
  private readonly destroy = inject(DestroyRef);
  protected readonly users = signal<Usuario[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  constructor() { this.load(); }

  protected load(): void {
    if (this.loading()) return;
    this.loading.set(true); this.error.set('');
    this.api.listar().pipe(takeUntilDestroyed(this.destroy), finalize(() => this.loading.set(false))).subscribe({
      next: users => this.users.set(users),
      error: (error: { status?: number }) => { this.users.set([]); this.error.set(error.status === 403 ? 'Você não possui permissão para consultar usuários.' : 'Não foi possível consultar usuários. Tente novamente quando o serviço estiver disponível.'); }
    });
  }
}
