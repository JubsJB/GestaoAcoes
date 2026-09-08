import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { CarteiraContextService } from '../../../core/carteira/carteira-context.service';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { formatOffsetDateTime } from '../../../shared/formatters/offset-date-time.formatter';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { CarteirasService } from '../carteiras.service';
import { CarteiraResponse } from '../models/carteira';
import { CarteiraFormPageComponent } from './carteira-form-page.component';

@Component({
  selector: 'app-carteiras-list-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatProgressSpinnerModule, PageHeaderComponent, RouterLink],
  template: `
    <section class="app-page collection-page" aria-labelledby="carteiras-title">
      <app-page-header headingId="carteiras-title" eyebrow="Organização" icon="portfolio" title="Carteiras" description="Gerencie as carteiras que organizam seus investimentos.">
        <button page-header-action mat-flat-button type="button" (click)="openCreateDialog()">Nova carteira</button>
      </app-page-header>

      @if (error()) { <app-feedback-alert variant="error" [message]="error()!.message" [details]="error()!.details" /> }
      <div class="collection-region" data-scroll-region="records">
        @if (loading()) {
          <div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36" /><span>Carregando carteiras…</span></div>
        } @else if (error()) {
          <div class="app-state"><button mat-stroked-button type="button" (click)="load()">Tentar novamente</button></div>
        } @else if (carteiras().length === 0) {
          <div class="app-state app-surface"><span class="app-state__icon" aria-hidden="true"><app-icon name="empty" /></span><h2>Você ainda não possui carteiras cadastradas.</h2><p>Cadastre a primeira carteira para começar sua organização.</p><button mat-stroked-button type="button" (click)="openCreateDialog()">Cadastrar a primeira</button></div>
        } @else {
          <table class="collection-table" role="table">
            <caption>Carteiras cadastradas</caption>
            <thead role="rowgroup"><tr role="row">
              <th scope="col" role="columnheader">Carteira</th>
              <th scope="col" role="columnheader">Data de criação</th>
              <th scope="col" role="columnheader">Contexto</th>
              <th scope="col" role="columnheader">Ações</th>
            </tr></thead>
            <tbody role="rowgroup">
              @for (carteira of carteiras(); track carteira.id) {
                <tr role="row">
                  <th scope="row" role="rowheader"><span class="cell-label" aria-hidden="true">Carteira</span>{{ carteira.nome }}</th>
                  <td role="cell"><span class="cell-label" aria-hidden="true">Data de criação</span>{{ dateTime(carteira.dataCriacao) }}</td>
                  <td role="cell"><span class="cell-label" aria-hidden="true">Contexto</span>@if (activeId() === carteira.id) { <span class="status-badge">Ativa</span> } @else { <span>—</span> }</td>
                  <td role="cell"><span class="cell-label" aria-hidden="true">Ações</span><a [routerLink]="[carteira.id]" [state]="{}" [info]="{ carteira }" [attr.aria-label]="'Ver detalhes da carteira ' + carteira.nome">Ver detalhes</a></td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>
    </section>
  `,
  styleUrl: '../../../shared/collection/collection.scss',
  styles: [`
    .collection-page{height:calc(100dvh - 8rem);overflow:hidden;display:flex;flex-direction:column}.collection-region{min-height:0;overflow:auto;padding:.125rem .25rem .75rem 0;overscroll-behavior:contain;flex:1 1 auto}
    @media(max-width:959.98px){.collection-page{height:calc(100dvh - 5.5rem)}}@media(max-width:36rem){.collection-page{height:auto;overflow:visible}.collection-region{overflow:visible;padding-right:0}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CarteirasListPageComponent {
  protected readonly activeId = inject(CarteiraContextService).activeId;
  private readonly service = inject(CarteirasService);
  private readonly dialog = inject(MatDialog);
  private readonly successToast = inject(SuccessToastService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly carteiras = signal<CarteiraResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly dateTime = formatOffsetDateTime;

  constructor() { this.load(); }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.listar().pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (items) => this.carteiras.set(items),
      error: (error: NormalizedHttpError) => this.error.set(error)
    });
  }

  protected openCreateDialog(): void {
    this.dialog.open(CarteiraFormPageComponent, {
      data: { mode: 'create' }, panelClass: ['app-create-dialog', 'app-dialog--form'], autoFocus: 'first-tabbable', restoreFocus: true,
      ariaLabelledBy: 'carteira-form-title', ariaDescribedBy: 'carteira-form-description'
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((created: CarteiraResponse | undefined) => {
      if (!created) return;
      this.carteiras.update((items) => [...items, created].sort((left, right) => left.id - right.id));
      this.successToast.show('Carteira cadastrada com sucesso.');
    });
  }
}
