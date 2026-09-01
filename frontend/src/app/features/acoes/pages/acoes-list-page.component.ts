import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { formatOffsetDateTime } from '../../../shared/formatters/offset-date-time.formatter';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { AcaoNotFoundDialogComponent } from '../acao-not-found-dialog.component';
import { formatCotacao, formatMercado } from '../acao-formatters';
import { AcoesService } from '../acoes.service';
import { AcaoResponse, Mercado, normalizeTicker } from '../models/acao';
import { AcaoCreatePageComponent } from './acao-create-page.component';

@Component({
  selector: 'app-acoes-list-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule, PageHeaderComponent, ReactiveFormsModule, RouterLink],
  template: `
    <section class="app-page collection-page" aria-labelledby="acoes-title">
      <app-page-header headingId="acoes-title" eyebrow="Ativos" icon="stock" title="Ações" description="Consulte as ações e suas últimas cotações persistidas.">
        <button page-header-action mat-flat-button type="button" (click)="openCreateDialog()">Cadastrar nova ação</button>
      </app-page-header>
      @if (loadError()) { <app-feedback-alert variant="error" [message]="loadError()!.message" [details]="loadError()!.details" /> }
      @else if (searchError()) { <app-feedback-alert variant="error" [message]="searchError()!.message" [details]="searchError()!.details" /> }

      <form class="search app-search-surface app-surface" [formGroup]="searchForm" (ngSubmit)="search()" aria-label="Buscar ação por ticker e mercado" novalidate>
        <mat-form-field appearance="outline"><mat-label>Ticker</mat-label><input matInput formControlName="ticker" maxlength="30" aria-describedby="ticker-search-hint"/><mat-hint id="ticker-search-hint">Informe o ticker exato.</mat-hint>@if(searchForm.controls.ticker.invalid&&searchForm.controls.ticker.touched){<mat-error>Ticker é obrigatório.</mat-error>}</mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Mercado</mat-label><mat-select formControlName="mercado" aria-describedby="mercado-search-hint"><mat-option value="BRASIL">Brasil</mat-option><mat-option value="EUA">EUA</mat-option></mat-select><mat-hint id="mercado-search-hint">Selecione o mercado.</mat-hint>@if(searchForm.controls.mercado.invalid&&searchForm.controls.mercado.touched){<mat-error>Mercado é obrigatório.</mat-error>}</mat-form-field>
        <div class="app-actions app-actions--stack-compact"><button mat-stroked-button type="submit" [disabled]="searching()" [attr.aria-busy]="searching()">Buscar ação</button><button mat-button type="button" (click)="clearSearch()">Limpar busca</button></div>
      </form>

      @if (searching()) { <p class="search-progress" role="status" aria-live="polite">Buscando ação…</p> }

      <div class="collection-region" data-scroll-region="records">
        @if(loading()){<div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36"/><span>Carregando ações…</span></div>}
        @else if(loadError()){<div class="app-state"><button mat-stroked-button type="button" (click)="load()">Tentar carregar ações novamente</button></div>}
        @else if(acoes().length===0){<div class="app-state app-surface"><span class="app-state__icon" aria-hidden="true"><app-icon name="empty" /></span><h2>Você ainda não possui ações cadastradas.</h2><p>Adicione seu primeiro ativo para acompanhar seus dados de mercado.</p><button mat-stroked-button type="button" (click)="openCreateDialog()">Cadastrar a primeira ação</button></div>}
        @else{
          <div class="stock-grid" aria-label="Ações cadastradas">
            @for(acao of acoes();track acao.id){
              <mat-card class="entity-card entity-card--compact" appearance="outlined">
                <div class="entity-card__heading">
                  <span class="entity-card__icon" aria-hidden="true"><app-icon name="stock" /></span>
                  <div class="entity-card__identity"><mat-card-title>{{acao.ticker}}</mat-card-title><mat-card-subtitle>{{acao.nomeEmpresa}}</mat-card-subtitle></div>
                  <span class="market-badge">{{market(acao.mercado)}} · {{acao.moeda}}</span>
                </div>
                <mat-card-content class="entity-card__data"><div><span>Última cotação persistida</span><strong>{{quote(acao.cotacaoAtual,acao.moeda)}}</strong></div><div><span>Atualizada em</span><strong>{{dateTime(acao.dataHoraCotacao)}}</strong></div></mat-card-content>
                <mat-card-actions><a mat-button [routerLink]="[acao.id]" [attr.aria-label]="'Ver detalhes de '+acao.ticker">Ver detalhes</a></mat-card-actions>
              </mat-card>
            }
          </div>
        }
      </div>
    </section>`,
  styles: [`
    .collection-page{height:calc(100dvh - 8rem);overflow:hidden}.collection-region{min-height:0;overflow:auto;padding:.125rem .25rem .75rem 0;overscroll-behavior:contain}.search{display:grid;grid-template-columns:minmax(12rem,1fr) minmax(12rem,1fr) auto;align-items:start;gap:1rem}.stock-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,20rem),1fr));gap:.75rem}.entity-card{border-color:var(--app-border-subtle);border-radius:var(--app-card-radius);background:var(--app-surface-card);box-shadow:0 .2rem .75rem rgb(31 36 29 / 5%)}.entity-card--compact{padding:.9rem 1rem .65rem}.entity-card__heading{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:start;gap:.75rem;min-width:0}.entity-card__icon{width:2.25rem;height:2.25rem;display:grid;place-items:center;border-radius:.7rem;color:var(--app-brand-primary);background:var(--app-surface-selected)}.entity-card__identity{min-width:0}.entity-card mat-card-title{font-size:1.05rem;font-weight:720;line-height:1.25}.entity-card mat-card-subtitle{margin-top:.15rem;font-size:.8rem;overflow-wrap:anywhere;line-height:1.35}.market-badge{display:inline-flex;align-items:center;min-height:1.65rem;padding:.2rem .55rem;border:1px solid #cbd5bd;border-radius:999px;color:#405136;background:var(--app-surface-selected);font-size:.68rem;font-weight:750;white-space:nowrap}.entity-card__data{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:.75rem 1rem;padding:1rem 0 .35rem}.entity-card__data div{display:grid;gap:.2rem;min-width:0}.entity-card__data span{color:var(--app-text-secondary);font-size:.68rem;font-weight:700;letter-spacing:.055em;text-transform:uppercase}.entity-card__data strong{font-size:.86rem;font-weight:620;overflow-wrap:anywhere}.entity-card mat-card-actions{min-height:auto;padding:.25rem 0 0;border-top:1px solid var(--app-border-subtle)}.search-progress{margin:0;color:var(--app-text-secondary)}
    .collection-page{display:flex;flex-direction:column}.collection-region{flex:1 1 auto}
    @media(max-width:959.98px){.collection-page{height:calc(100dvh - 5.5rem)}}
    @media(max-width:48rem){.search{grid-template-columns:1fr 1fr}.search .app-actions{grid-column:1/-1}}
    @media(max-width:36rem){.collection-page{height:auto;overflow:visible}.collection-region{overflow:visible;padding-right:0}.search{grid-template-columns:1fr}.search .app-actions{grid-column:auto}.entity-card__heading{grid-template-columns:auto minmax(0,1fr)}.market-badge{grid-column:2}.entity-card__data{grid-template-columns:1fr}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AcoesListPageComponent {
  private readonly service = inject(AcoesService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly successToast = inject(SuccessToastService);
  protected readonly acoes = signal<AcaoResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly searching = signal(false);
  protected readonly loadError = signal<NormalizedHttpError | null>(null);
  protected readonly searchError = signal<NormalizedHttpError | null>(null);
  protected readonly searchForm = new FormGroup({ ticker: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(30)] }), mercado: new FormControl<Mercado | null>(null, { validators: [Validators.required] }) });
  protected readonly market = formatMercado;
  protected readonly quote = formatCotacao;
  protected readonly dateTime = formatOffsetDateTime;

  constructor() { this.load(); }

  protected load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.service.listar().pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({ next: (items) => this.acoes.set(items), error: (error: NormalizedHttpError) => this.loadError.set(error) });
  }

  protected search(): void {
    this.searchForm.markAllAsTouched();
    const mercado = this.searchForm.controls.mercado.value;
    if (this.searchForm.invalid || !mercado || this.searching()) return;
    const ticker = normalizeTicker(this.searchForm.controls.ticker.value);
    this.searching.set(true);
    this.searchError.set(null);
    this.service.buscarPorTickerEMercado(ticker, mercado).pipe(finalize(() => this.searching.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (acao) => void this.router.navigate([acao.id], { relativeTo: this.route, info: { acao } }),
      error: (error: NormalizedHttpError) => {
        if (error.status === 404 && error.code === null) { this.openNotFoundDialog(ticker, mercado); return; }
        this.searchError.set(error);
      }
    });
  }

  private openNotFoundDialog(ticker: string, mercado: Mercado): void {
    this.dialog.open(AcaoNotFoundDialogComponent, {
      data: { ticker, mercado },
      width: '32rem',
      maxWidth: 'calc(100vw - 2rem)',
      panelClass: 'app-not-found-dialog',
      autoFocus: 'first-tabbable',
      restoreFocus: true
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((register: boolean | undefined) => {
      if (register) this.openCreateDialog({ ticker, mercado });
    });
  }

  protected openCreateDialog(data: { ticker?: string; mercado?: Mercado } = {}): void {
    this.dialog.open(AcaoCreatePageComponent, {
      data,
      width: '42rem',
      maxWidth: 'calc(100vw - 2rem)',
      maxHeight: 'calc(100dvh - 2rem)',
      panelClass: 'app-create-dialog',
      autoFocus: 'first-tabbable',
      restoreFocus: true
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((created: AcaoResponse | undefined) => {
      if (!created) return;
      this.acoes.update((items) => [...items, created]);
      this.successToast.show('Ação cadastrada com sucesso.');
    });
  }

  protected clearSearch(): void {
    this.searchForm.reset({ ticker: '', mercado: null });
    this.searchError.set(null);
  }
}
