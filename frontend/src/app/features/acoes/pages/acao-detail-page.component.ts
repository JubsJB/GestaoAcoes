import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { formatOffsetDateTime } from '../../../shared/formatters/offset-date-time.formatter';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { StickyBackComponent } from '../../../shared/sticky-back/sticky-back.component';
import { formatCotacao, formatMercado } from '../acao-formatters';
import { AcoesService } from '../acoes.service';
import { AcaoResponse } from '../models/acao';

@Component({
  selector: 'app-acao-detail-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatCardModule, MatProgressSpinnerModule, PageHeaderComponent, RouterLink, StickyBackComponent],
  template: `
    <section class="app-page" aria-labelledby="detail-title">
      <app-sticky-back route="/acoes" label="Voltar para ações" />
      @if (acao(); as item) {
        <app-page-header headingId="detail-title" eyebrow="Detalhe da ação" icon="stock" [title]="item.ticker" [description]="item.nomeEmpresa">
          <button page-header-action mat-stroked-button type="button" (click)="updateQuote()" [disabled]="updating()" [attr.aria-busy]="updating()">Atualizar cotação</button>
        </app-page-header>
      } @else {
        <app-page-header headingId="detail-title" eyebrow="Ações" icon="stock" [title]="notFound() ? 'Ação não encontrada' : loadError() ? 'Não foi possível carregar a ação' : 'Detalhe da ação'" description="Consulte a identificação e a última cotação persistida." />
      }

      @if (loadError()) { <app-feedback-alert variant="error" [message]="loadError()!.message" [details]="loadError()!.details" /> }
      @else if (updateError()) { <app-feedback-alert variant="error" [message]="updateError()!.message" [details]="updateError()!.details" /> }
      @if(updating()){<p class="progress" role="status" aria-live="polite">Atualizando cotação…</p>}

      @if(loading()){<div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36"/>Carregando ação…</div>}
      @else if(loadError()){
        <div class="app-state">@if(notFound()){<a mat-stroked-button routerLink="/acoes">Voltar para a listagem de ações</a>}@else{<button mat-stroked-button type="button" (click)="load()">Tentar carregar a ação novamente</button>}</div>
      }
      @else if(acao();as item){
        <div class="grid">
          <mat-card class="section-card" appearance="outlined"><div class="section-card__heading"><app-icon name="identity" aria-hidden="true" /><h2>Identificação</h2></div><mat-card-content><dl class="data-list"><div><dt>Ticker</dt><dd>{{item.ticker}}</dd></div><div><dt>Empresa</dt><dd>{{item.nomeEmpresa}}</dd></div><div><dt>Mercado</dt><dd><span class="status-badge">{{market(item.mercado)}}</span></dd></div></dl></mat-card-content></mat-card>
          <mat-card class="section-card quote-card" appearance="outlined"><div class="section-card__heading"><app-icon name="quote" aria-hidden="true" /><h2>Última cotação persistida</h2></div><mat-card-content><p class="quote-note">Valor fornecido pelo backend na referência abaixo; não representa garantia de cotação em tempo real.</p><dl class="data-list"><div><dt>Moeda</dt><dd><span class="status-badge">{{item.moeda}}</span></dd></div><div><dt>Cotação</dt><dd class="quote-value">{{quote(item.cotacaoAtual,item.moeda)}}</dd></div><div><dt>Atualizada em</dt><dd>{{dateTime(item.dataHoraCotacao)}}</dd></div></dl></mat-card-content></mat-card>
        </div>
      }
    </section>`,
  styles: [`
    .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,22rem),1fr));gap:1rem;align-items:start}.section-card mat-card-content{padding:1.25rem}.progress{margin:0;color:var(--app-text-secondary)}.quote-card{background:linear-gradient(145deg,#fff,#fbfcf8)}.quote-note{margin-top:0;color:var(--app-text-secondary);font-size:.875rem;line-height:1.5}.quote-value{font-size:1.25rem;font-weight:720;color:var(--app-text-primary)}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AcaoDetailPageComponent {
  private readonly service = inject(AcoesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly successToast = inject(SuccessToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly id = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly acao = signal<AcaoResponse | null>(this.navigationStock());
  protected readonly loading = signal(this.acao() === null);
  protected readonly loadError = signal<NormalizedHttpError | null>(null);
  protected readonly notFound = signal(false);
  protected readonly updating = signal(false);
  protected readonly updateError = signal<NormalizedHttpError | null>(null);
  protected readonly market = formatMercado;
  protected readonly quote = formatCotacao;
  protected readonly dateTime = formatOffsetDateTime;

  constructor() { if (!this.acao()) this.load(); }

  protected load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.notFound.set(false);
    this.service.buscarPorId(this.id).pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (item) => this.acao.set(item),
      error: (error: NormalizedHttpError) => { this.notFound.set(error.status === 404 && error.code === null); this.loadError.set(error); }
    });
  }

  protected updateQuote(): void {
    if (this.updating() || !this.acao()) return;
    this.updating.set(true);
    this.updateError.set(null);
    this.service.atualizarCotacao(this.id).pipe(finalize(() => this.updating.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (item) => { this.acao.set(item); this.successToast.show('Cotação atualizada com sucesso.'); },
      error: (error: NormalizedHttpError) => this.updateError.set(error)
    });
  }

  private navigationStock(): AcaoResponse | null {
    const candidate = (this.router.currentNavigation()?.extras.info as { acao?: AcaoResponse } | undefined)?.acao;
    return candidate?.id === this.id ? candidate : null;
  }
}
