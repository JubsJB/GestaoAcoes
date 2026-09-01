import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { formatOffsetDateTime } from '../../../shared/formatters/offset-date-time.formatter';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { StickyBackComponent } from '../../../shared/sticky-back/sticky-back.component';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { CarteiraDeleteConfirmDialogComponent } from '../carteira-delete-confirm-dialog.component';
import { CarteirasService } from '../carteiras.service';
import { CarteiraResponse } from '../models/carteira';
import { CarteiraFormPageComponent } from './carteira-form-page.component';

@Component({
  selector: 'app-carteira-detail-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatCardModule, MatProgressSpinnerModule, PageHeaderComponent, RouterLink, StickyBackComponent],
  template: `
    <section class="app-page" aria-labelledby="carteira-detail-title">
      <app-sticky-back route="/carteiras" label="Voltar para carteiras" />
      @if (carteira(); as item) {
        <app-page-header headingId="carteira-detail-title" eyebrow="Detalhe da carteira" icon="portfolio" [title]="item.nome" description="Dados básicos da carteira.">
          <div page-header-action class="app-actions app-actions--stack-compact"><button mat-stroked-button type="button" (click)="openEditDialog()">Editar</button><button mat-flat-button type="button" (click)="openDeleteDialog()">Excluir</button></div>
        </app-page-header>
      } @else {
        <app-page-header headingId="carteira-detail-title" eyebrow="Carteiras" icon="portfolio" [title]="notFound() ? 'Carteira não encontrada' : error() ? 'Não foi possível carregar a carteira' : 'Detalhe da carteira'" description="Consulte os dados básicos da carteira." />
      }
      @if (error()) { <app-feedback-alert variant="error" [message]="error()!.message" [details]="error()!.details" /> }
      @if (loading()) {
        <div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36" /> Carregando carteira…</div>
      } @else if (error()) {
        <div class="app-state">@if (notFound()) { <a mat-stroked-button routerLink="/carteiras">Voltar para a listagem</a> } @else { <button mat-stroked-button type="button" (click)="load()">Tentar novamente</button> }</div>
      } @else if (carteira(); as item) {
        <mat-card class="section-card" appearance="outlined"><div class="section-card__heading"><app-icon name="identity" aria-hidden="true" /><h2>Identificação</h2></div><mat-card-content><dl class="data-list"><div><dt>Nome</dt><dd>{{ item.nome }}</dd></div><div><dt>Identificador</dt><dd>{{ item.id }}</dd></div><div><dt>Data de criação</dt><dd>{{ dateTime(item.dataCriacao) }}</dd></div></dl></mat-card-content></mat-card>
      }
    </section>
  `,
  styles: [`.section-card{max-width:48rem}.section-card mat-card-content{padding:1.25rem}.data-list dd{overflow-wrap:anywhere}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CarteiraDetailPageComponent {
  private readonly service = inject(CarteirasService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly successToast = inject(SuccessToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly id = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly carteira = signal<CarteiraResponse | null>(this.navigationPortfolio());
  protected readonly loading = signal(this.carteira() === null);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly notFound = signal(false);
  protected readonly dateTime = formatOffsetDateTime;

  constructor() { if (!this.carteira()) this.load(); }

  protected load(): void {
    this.loading.set(true); this.error.set(null); this.notFound.set(false);
    this.service.buscarPorId(this.id).pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (item) => this.carteira.set(item),
      error: (error: NormalizedHttpError) => { this.notFound.set(error.status === 404); this.error.set(error); }
    });
  }

  protected openEditDialog(): void {
    const carteira = this.carteira(); if (!carteira) return;
    this.dialog.open(CarteiraFormPageComponent, {
      data: { mode: 'edit', carteira }, width: '36rem', maxWidth: 'calc(100vw - 2rem)', maxHeight: 'calc(100dvh - 2rem)',
      panelClass: 'app-create-dialog', autoFocus: 'first-tabbable', restoreFocus: true,
      ariaLabelledBy: 'carteira-form-title', ariaDescribedBy: 'carteira-form-description'
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((updated: CarteiraResponse | undefined) => {
      if (!updated) return; this.carteira.set(updated); this.successToast.show('Carteira atualizada com sucesso.');
    });
  }

  protected openDeleteDialog(): void {
    const carteira = this.carteira(); if (!carteira) return;
    this.dialog.open(CarteiraDeleteConfirmDialogComponent, { data: carteira, width: '32rem', maxWidth: 'calc(100vw - 2rem)', autoFocus: 'first-tabbable', restoreFocus: true })
      .afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((deleted: boolean | undefined) => {
        if (deleted !== true) return;
        void this.router.navigate(['/carteiras']).then((navigated) => { if (navigated) this.successToast.show('Carteira excluída com sucesso.'); });
      });
  }

  private navigationPortfolio(): CarteiraResponse | null {
    const candidate = (this.router.currentNavigation()?.extras.info as { carteira?: CarteiraResponse } | undefined)?.carteira;
    return candidate && candidate.id === this.id && typeof candidate.nome === 'string' && typeof candidate.dataCriacao === 'string' ? candidate : null;
  }
}
