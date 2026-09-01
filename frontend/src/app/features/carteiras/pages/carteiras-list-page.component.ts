import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
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
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatCardModule, MatProgressSpinnerModule, PageHeaderComponent, RouterLink],
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
          <div class="portfolio-grid" aria-label="Carteiras cadastradas">
            @for (carteira of carteiras(); track carteira.id) {
              <mat-card class="entity-card" appearance="outlined">
                <div class="entity-card__heading"><span class="entity-card__icon" aria-hidden="true"><app-icon name="portfolio" /></span><mat-card-title>{{ carteira.nome }}</mat-card-title></div>
                <mat-card-content><span>Data de criação</span><strong>{{ dateTime(carteira.dataCriacao) }}</strong></mat-card-content>
                <mat-card-actions><a mat-button [routerLink]="[carteira.id]" [state]="{}" [info]="{ carteira }" [attr.aria-label]="'Ver detalhes da carteira ' + carteira.nome">Ver detalhes</a></mat-card-actions>
              </mat-card>
            }
          </div>
        }
      </div>
    </section>
  `,
  styles: [`
    .collection-page{height:calc(100dvh - 8rem);overflow:hidden;display:flex;flex-direction:column}.collection-region{min-height:0;overflow:auto;padding:.125rem .25rem .75rem 0;overscroll-behavior:contain;flex:1 1 auto}.portfolio-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,18rem),1fr));gap:.75rem}.entity-card{padding:.9rem 1rem .65rem;border-color:var(--app-border-subtle);border-radius:var(--app-card-radius);background:var(--app-surface-card);box-shadow:0 .2rem .75rem rgb(31 36 29 / 5%);min-width:0}.entity-card__heading{display:flex;align-items:center;gap:.75rem;min-width:0}.entity-card__icon{width:2.25rem;height:2.25rem;display:grid;place-items:center;flex:0 0 auto;border-radius:.7rem;color:var(--app-brand-primary);background:var(--app-surface-selected)}mat-card-title{font-size:1rem;font-weight:680;overflow-wrap:anywhere;line-height:1.3}mat-card-content{display:grid;gap:.2rem;padding:1rem 0 .7rem}mat-card-content span{color:var(--app-text-secondary);font-size:.68rem;font-weight:700;letter-spacing:.055em;text-transform:uppercase}mat-card-content strong{font-size:.84rem;font-weight:600;overflow-wrap:anywhere}mat-card-actions{min-height:auto;padding:.25rem 0 0;border-top:1px solid var(--app-border-subtle)}
    @media(max-width:959.98px){.collection-page{height:calc(100dvh - 5.5rem)}}@media(max-width:36rem){.collection-page{height:auto;overflow:visible}.collection-region{overflow:visible;padding-right:0}.portfolio-grid{grid-template-columns:1fr}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CarteirasListPageComponent {
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
      data: { mode: 'create' }, width: '36rem', maxWidth: 'calc(100vw - 2rem)', maxHeight: 'calc(100dvh - 2rem)',
      panelClass: 'app-create-dialog', autoFocus: 'first-tabbable', restoreFocus: true,
      ariaLabelledBy: 'carteira-form-title', ariaDescribedBy: 'carteira-form-description'
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((created: CarteiraResponse | undefined) => {
      if (!created) return;
      this.carteiras.update((items) => [...items, created].sort((left, right) => left.id - right.id));
      this.successToast.show('Carteira cadastrada com sucesso.');
    });
  }
}
