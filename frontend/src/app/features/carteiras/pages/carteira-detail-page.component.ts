import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { distinctUntilChanged, map, Subscription } from 'rxjs';
import { CarteiraContextService, carteiraId } from '../../../core/carteira/carteira-context.service';
import { DashboardService } from '../../dashboard/dashboard.service';
import { PosicaoResponse } from '../../dashboard/models/dashboard';
import { PortfolioPositionsComponent } from '../../../shared/portfolio-positions/portfolio-positions.component';
import { normalizeHttpError } from '../../../core/http/http-error-normalizer';
import { HttpErrorResponse } from '@angular/common/http';
import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { formatOffsetDateTime } from '../../../shared/formatters/offset-date-time.formatter';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { StickyBackComponent } from '../../../shared/sticky-back/sticky-back.component';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { OperacaoResponse } from '../../operacoes/models/operacao';
import { formatCivilDate, formatDecimal } from '../../operacoes/operacao-validators';
import { OperacoesService } from '../../operacoes/operacoes.service';
import { OperacaoFormPageComponent } from '../../operacoes/pages/operacao-form-page.component';
import { CarteiraDeleteConfirmDialogComponent } from '../carteira-delete-confirm-dialog.component';
import { CarteirasService } from '../carteiras.service';
import { CarteiraResponse } from '../models/carteira';
import { CarteiraFormPageComponent } from './carteira-form-page.component';

@Component({selector:'app-carteira-detail-page',imports:[PortfolioPositionsComponent,AppIconComponent,FeedbackAlertComponent,MatButtonModule,MatCardModule,MatProgressSpinnerModule,PageHeaderComponent,RouterLink,StickyBackComponent],template:`
<section class="app-page" aria-labelledby="carteira-detail-title"><app-sticky-back route="/carteiras" label="Voltar para carteiras"/>
@if(carteira();as item){<app-page-header headingId="carteira-detail-title" eyebrow="Detalhe da carteira" icon="portfolio" [title]="item.nome" description="Dados básicos, posições abertas e histórico da carteira."><div page-header-action class="app-actions app-actions--stack-compact"><button mat-stroked-button type="button" (click)="openEditDialog()">Editar</button><button mat-flat-button type="button" (click)="openDeleteDialog()">Excluir</button></div></app-page-header>}@else{<app-page-header headingId="carteira-detail-title" eyebrow="Carteiras" icon="portfolio" [title]="notFound()?'Carteira não encontrada':error()?'Não foi possível carregar a carteira':'Detalhe da carteira'" description="Consulte os dados básicos da carteira."/>}
@if(error()){<app-feedback-alert variant="error" [message]="error()!.message" [details]="error()!.details"/>}
@if(loading()){<div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36"/>Carregando carteira…</div>}@else if(error()){<div class="app-state">@if(notFound()){<a mat-stroked-button routerLink="/carteiras">Voltar para a listagem</a>}@else{<button mat-stroked-button type="button" (click)="load()">Tentar novamente</button>}</div>}@else if(carteira();as item){
<mat-card class="section-card" appearance="outlined"><div class="section-card__heading"><app-icon name="identity" aria-hidden="true"/><h2>Identificação</h2></div><mat-card-content><dl class="data-list"><div><dt>Nome</dt><dd>{{item.nome}}</dd></div><div><dt>Identificador</dt><dd>{{item.id}}</dd></div><div><dt>Data de criação</dt><dd>{{dateTime(item.dataCriacao)}}</dd></div></dl></mat-card-content></mat-card>
<section aria-labelledby="portfolio-positions-title"><h2 id="portfolio-positions-title">Posições abertas</h2>
@if(positionsLoading()){<div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="32"/>Carregando posições…</div>}
@else if(positionsError()){<app-feedback-alert variant="error" [message]="positionsError()!.message" [details]="positionsError()!.details"/><button mat-stroked-button type="button" (click)="loadPositions()">Tentar novamente</button>}
@else{<app-portfolio-positions [positions]="positions()"/>}</section>
<section class="history" aria-labelledby="portfolio-history-title"><div class="history__heading"><div><h2 id="portfolio-history-title">Histórico de operações</h2><p>Compras e vendas desta carteira na ordem cronológica.</p></div><button mat-flat-button type="button" (click)="openOperationDialog()">Registrar operação</button></div>
@if(historyError()){<app-feedback-alert variant="error" [message]="historyError()!.message" [details]="historyError()!.details"/>}
@if(historyLoading()){<div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="32"/>Carregando histórico…</div>}@else if(historyError()){<div class="app-state"><button mat-stroked-button type="button" (click)="loadHistory()">Tentar novamente</button></div>}@else if(history().length===0){<div class="app-state app-surface"><h3>Nenhuma operação nesta carteira.</h3><p>Registre a primeira movimentação.</p></div>}@else{<div class="history-list">@for(op of history();track op.id){<a class="history-item app-surface" [routerLink]="['/operacoes',op.id]" [info]="{operacao:op}" [queryParams]="{carteiraId:item.id,origem:'carteira'}"><strong>{{op.tipo}} · {{op.ticker}}</strong><span>{{civilDate(op.dataOperacao)}} · ordem {{op.ordemNoDia}}</span><span>{{decimal(op.quantidade)}} × {{decimal(op.precoUnitario)}} {{op.moeda??(op.mercado==='BRASIL'?'BRL':'USD')}}</span><span>Total {{decimal(op.valorTotal)}} · {{op.corretoraId==null?'Sem corretora':'Corretora #'+op.corretoraId}}</span></a>}</div>}</section>}
</section>`,styles:[`.section-card{max-width:48rem}.section-card mat-card-content{padding:1.25rem}.data-list dd{overflow-wrap:anywhere}.history{display:grid;gap:1rem}.history__heading{display:flex;align-items:center;justify-content:space-between;gap:1rem}.history__heading h2,.history__heading p{margin:0}.history__heading p{margin-top:.25rem;color:var(--app-text-secondary)}.history-list{display:grid;gap:.75rem}.history-item{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.75rem;padding:1rem;color:inherit;text-decoration:none}.history-item:focus-visible{outline:3px solid var(--app-brand-primary);outline-offset:2px}.history-item span{overflow-wrap:anywhere;color:var(--app-text-secondary)}@media(max-width:48rem){.history-item{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:36rem){.history__heading{align-items:stretch;flex-direction:column}.history-item{grid-template-columns:1fr}}`],changeDetection:ChangeDetectionStrategy.OnPush})
export class CarteiraDetailPageComponent {
  private readonly service = inject(CarteirasService);
  private readonly context = inject(CarteiraContextService);
  private readonly dashboard = inject(DashboardService);
  private readonly operationsService = inject(OperacoesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly successToast = inject(SuccessToastService);
  private readonly destroyRef = inject(DestroyRef);
  private id: number | null = null;
  private generation = 0;
  private entityRequest?: Subscription;
  private historyRequest?: Subscription;
  private positionsRequest?: Subscription;
  private readonly createdOperations = new Map<number, OperacaoResponse>();
  private closeDialogs: (() => void)[] = [];
  protected readonly carteira = signal<CarteiraResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly notFound = signal(false);
  protected readonly history = signal<OperacaoResponse[]>([]);
  protected readonly historyLoading = signal(false);
  protected readonly historyError = signal<NormalizedHttpError | null>(null);
  protected readonly positions = signal<PosicaoResponse[]>([]);
  protected readonly positionsLoading = signal(false);
  protected readonly positionsError = signal<NormalizedHttpError | null>(null);
  protected readonly dateTime = formatOffsetDateTime;
  protected readonly civilDate = formatCivilDate;
  protected readonly decimal = formatDecimal;

  constructor() {
    this.route.paramMap.pipe(map(params => params.get('id')), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe(raw => {
      this.generation++;
      this.entityRequest?.unsubscribe(); this.historyRequest?.unsubscribe(); this.positionsRequest?.unsubscribe();
      this.closeDialogs.forEach(close => close()); this.closeDialogs = [];
      this.id = carteiraId(raw);
      this.carteira.set(null); this.history.set([]); this.positions.set([]);
      this.createdOperations.clear();
      this.error.set(null); this.historyError.set(null); this.positionsError.set(null);
      this.historyLoading.set(false); this.positionsLoading.set(false); this.loading.set(false);
      this.context.resolveUrl(raw ?? '');
      const transient = this.navigationPortfolio();
      if (transient) this.accept(transient); else this.load();
    });
    this.context.initialize().subscribe();
    this.destroyRef.onDestroy(() => { this.generation++; this.closeDialogs.forEach(close => close()); });
  }

  protected load(): void {
    this.entityRequest?.unsubscribe();
    this.error.set(null); this.notFound.set(false);
    if (this.id === null) {
      this.notFound.set(true);
      this.error.set(normalizeHttpError(new HttpErrorResponse({ status: 404, statusText: 'Carteira inválida' })));
      return;
    }
    this.loading.set(true);
    const generation = this.generation;
    this.entityRequest = this.service.buscarPorId(this.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: item => { if (generation === this.generation) this.accept(item); },
      error: (error: NormalizedHttpError) => {
        if (generation !== this.generation) return;
        this.loading.set(false); this.notFound.set(error.status === 404); this.error.set(error);
        if (error.status === 404) this.context.resolveUrl('');
      }
    });
  }
  private accept(item: CarteiraResponse): void {
    if (item.id !== this.id) return;
    this.carteira.set(item); this.loading.set(false);
    this.context.upsert(item); this.context.resolveUrl(String(item.id));
    this.loadHistory(); this.loadPositions();
  }
  protected loadHistory(): void {
    const item = this.carteira(); if (!item) return;
    this.historyRequest?.unsubscribe(); this.historyLoading.set(true); this.historyError.set(null);
    this.historyRequest = this.operationsService.listarPorCarteira(item.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => { this.history.set(this.withCreatedOperations(items)); this.historyLoading.set(false); },
      error: (error: NormalizedHttpError) => { this.historyError.set(error); this.historyLoading.set(false); }
    });
  }
  protected loadPositions(): void {
    const item = this.carteira(); if (!item) return;
    this.positionsRequest?.unsubscribe(); this.positionsLoading.set(true); this.positionsError.set(null);
    this.positionsRequest = this.dashboard.listarPosicoes(item.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => { this.positions.set(items); this.positionsLoading.set(false); },
      error: (error: NormalizedHttpError) => { this.positionsError.set(error); this.positionsLoading.set(false); }
    });
  }
  protected openOperationDialog(): void {
    const carteira = this.carteira(); if (!carteira) return;
    const generation = this.generation;
    const ref = this.dialog.open(OperacaoFormPageComponent, { data: { carteira }, panelClass: ['app-create-dialog', 'app-dialog--extended'], autoFocus: 'first-tabbable', restoreFocus: true, ariaLabelledBy: 'operacao-form-title', ariaDescribedBy: 'operacao-form-description' });
    this.closeDialogs.push(() => ref.close());
    ref.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((created: OperacaoResponse | undefined) => {
      if (!created || generation !== this.generation || created.carteiraId !== carteira.id) return;
      this.createdOperations.set(created.id, created);
      this.history.update(items => this.withCreatedOperations(items));
      this.successToast.show('Operação registrada com sucesso.'); this.loadPositions();
    });
  }
  protected openEditDialog(): void {
    const carteira = this.carteira(); if (!carteira) return;
    const generation = this.generation;
    const ref = this.dialog.open(CarteiraFormPageComponent, { data: { mode: 'edit', carteira }, panelClass: ['app-create-dialog', 'app-dialog--form'], autoFocus: 'first-tabbable', restoreFocus: true, ariaLabelledBy: 'carteira-form-title', ariaDescribedBy: 'carteira-form-description' });
    this.closeDialogs.push(() => ref.close());
    ref.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((updated: CarteiraResponse | undefined) => {
      if (!updated || generation !== this.generation || updated.id !== carteira.id) return;
      this.carteira.set(updated); this.successToast.show('Carteira atualizada com sucesso.');
    });
  }
  protected openDeleteDialog(): void {
    const carteira = this.carteira(); if (!carteira) return;
    const generation = this.generation;
    const ref = this.dialog.open(CarteiraDeleteConfirmDialogComponent, { data: carteira, panelClass: 'app-dialog--compact', autoFocus: 'first-tabbable', restoreFocus: true });
    this.closeDialogs.push(() => ref.close());
    ref.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((deleted: boolean | undefined) => {
      if (deleted !== true || generation !== this.generation) return;
      void this.router.navigate(['/carteiras']).then(ok => { if (ok) this.successToast.show('Carteira excluída com sucesso.'); });
    });
  }
  private navigationPortfolio(): CarteiraResponse | null {
    const item = (this.router.currentNavigation()?.extras.info as { carteira?: CarteiraResponse } | undefined)?.carteira;
    return item && item.id === this.id && typeof item.nome === 'string' && typeof item.dataCriacao === 'string' ? item : null;
  }
  private withCreatedOperations(items: OperacaoResponse[]): OperacaoResponse[] {
    if (this.createdOperations.size === 0) return items;
    const merged = new Map(items.map(item => [item.id, item]));
    this.createdOperations.forEach((item, id) => merged.set(id, item));
    return [...merged.values()].sort(compareOperations);
  }
}
function compareOperations(a: OperacaoResponse, b: OperacaoResponse): number {
  return a.dataOperacao.localeCompare(b.dataOperacao) || a.ordemNoDia - b.ordemNoDia || a.id - b.id;
}
