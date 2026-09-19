import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, Subscription } from 'rxjs';

import { CarteiraContextService } from '../../../core/carteira/carteira-context.service';
import { CarteiraNavigationService } from '../../../core/carteira/carteira-navigation.service';
import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CorretoraNamesService } from '../../corretoras/corretora-names.service';
import { OperacaoResponse } from '../models/operacao';
import { formatCivilDate, formatMoney, formatOperationQuantity } from '../operacao-validators';
import { OperacoesService } from '../operacoes.service';

@Component({
  providers: [CorretoraNamesService],
  selector: 'app-operacoes-list-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatProgressSpinnerModule, PageHeaderComponent, RouterLink],
  template: `
    <section class="app-page" aria-labelledby="operations-title">
      <app-page-header headingId="operations-title" eyebrow="Histórico" icon="operation" title="Operações"
        [description]="context.active() ? 'Compras e vendas da carteira ' + context.active()!.nome + ', em ordem cronológica.' : 'Compras e vendas da carteira selecionada, em ordem cronológica.'">
        @if (context.active(); as carteira) {
          <a page-header-action mat-flat-button routerLink="nova" [queryParams]="{carteiraId: carteira.id, origem: 'operacoes'}">Nova operação</a>
        }
      </app-page-header>
      @if (context.loading()) {
        <div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36"/>Carregando carteiras…</div>
      } @else if (context.error()) {
        <app-feedback-alert variant="error" [message]="context.error()!.message" [details]="context.error()!.details"/>
        <div class="app-state"><button mat-stroked-button type="button" (click)="retryContext()">Tentar novamente</button></div>
      } @else if (context.invalidSelection()) {
        <app-feedback-alert variant="warning" message="A carteira informada não está disponível. Selecione uma carteira válida para continuar."/>
        <div class="app-state"><a mat-stroked-button routerLink="/carteiras">Ir para carteiras</a></div>
      } @else if (context.carteiras().length === 0) {
        <div class="app-state app-surface"><span class="app-state__icon" aria-hidden="true"><app-icon name="empty"/></span><h2>Nenhuma carteira cadastrada.</h2><p>Crie uma carteira antes de registrar operações.</p><a mat-stroked-button routerLink="/carteiras/nova">Criar carteira</a></div>
      } @else if (loading()) {
        <div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36"/>Carregando operações de {{context.active()!.nome}}…</div>
      } @else if (error()) {
        <app-feedback-alert variant="error" [message]="operationErrorMessage()" [details]="error()!.details"/>
        <div class="app-state"><button mat-stroked-button type="button" (click)="retryOperations()">Tentar novamente</button></div>
      } @else if (items().length === 0) {
        <div class="app-state app-surface"><span class="app-state__icon" aria-hidden="true"><app-icon name="empty"/></span><h2>Nenhuma operação nesta carteira.</h2><p>Registre a primeira compra ou venda em {{context.active()!.nome}}.</p><a mat-stroked-button routerLink="nova" [queryParams]="{carteiraId: context.activeId(), origem: 'operacoes'}">Registrar a primeira</a></div>
      } @else {
        <table class="collection-table" role="table"><caption>Operações da carteira {{context.active()!.nome}}</caption><thead role="rowgroup"><tr role="row"><th scope="col" role="columnheader">Ativo</th><th scope="col" role="columnheader">Tipo</th><th scope="col" role="columnheader">Data · ordem</th><th scope="col" role="columnheader">Carteira</th><th scope="col" role="columnheader" class="numeric">Quantidade</th><th scope="col" role="columnheader" class="numeric">Preço unitário</th><th scope="col" role="columnheader" class="numeric">Valor total</th><th scope="col" role="columnheader">Corretora</th><th scope="col" role="columnheader">Ações</th></tr></thead><tbody role="rowgroup">@for(item of items();track item.id){<tr role="row"><th scope="row" role="rowheader"><span class="cell-label" aria-hidden="true">Ativo</span>{{item.ticker}}<small>{{item.mercado}} · {{item.moeda??(item.mercado==='BRASIL'?'BRL':'USD')}}</small></th><td role="cell"><span class="cell-label" aria-hidden="true">Tipo</span>{{item.tipo}}</td><td role="cell"><span class="cell-label" aria-hidden="true">Data · ordem</span>{{date(item.dataOperacao)}} · ordem {{item.ordemNoDia}}</td><td role="cell"><span class="cell-label" aria-hidden="true">Carteira</span>#{{item.carteiraId}}</td><td role="cell" class="numeric"><span class="cell-label" aria-hidden="true">Quantidade</span>{{quantity(item.quantidade,item.mercado)}}</td><td role="cell" class="numeric"><span class="cell-label" aria-hidden="true">Preço unitário</span>{{money(item.precoUnitario,item.moeda??(item.mercado==='BRASIL'?'BRL':'USD'))}}</td><td role="cell" class="numeric"><span class="cell-label" aria-hidden="true">Valor total</span>{{money(item.valorTotal,item.moeda??(item.mercado==='BRASIL'?'BRL':'USD'))}}</td><td role="cell"><span class="cell-label" aria-hidden="true">Corretora</span>{{brokerNames.name(item.corretoraId)}}</td><td role="cell"><span class="cell-label" aria-hidden="true">Ações</span><a [routerLink]="[item.id]" [queryParams]="{carteiraId: context.activeId(), origem: 'operacoes'}" [info]="{operacao:item}" [attr.aria-label]="'Ver detalhes da operação '+item.id">Ver detalhes</a></td></tr>}</tbody></table>
      }
    </section>`,
  styleUrl: '../../../shared/collection/collection.scss',
  styles: [],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperacoesListPageComponent {
  protected readonly brokerNames = inject(CorretoraNamesService);
  protected readonly context = inject(CarteiraContextService);
  private readonly navigation = inject(CarteiraNavigationService);
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(OperacoesService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly items = signal<OperacaoResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly date = formatCivilDate;
  protected readonly quantity = formatOperationQuantity;
  protected readonly money = formatMoney;
  private request?: Subscription;
  private requestGeneration = 0;
  private contextKey = '';

  constructor() {
    this.navigation.bindOperations(this.route, this.destroyRef);
    this.context.state$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(state => {
      const key = `${state.status}|${state.activeId ?? ''}|${state.invalidSelection}`;
      if (key === this.contextKey) return;
      this.contextKey = key;
      this.invalidateOperations();
      if (state.status === 'ready' && state.activeId !== null && !state.invalidSelection) this.load(state.activeId);
    });
  }

  protected retryContext(): void { this.context.initialize(true).subscribe(); }
  protected retryOperations(): void { const id = this.context.activeId(); if (id !== null) { this.invalidateOperations(); this.load(id); } }
  protected operationErrorMessage(): string { return this.error()?.status === 404 ? 'A carteira selecionada não está mais disponível. Escolha outra carteira para continuar.' : this.error()!.message; }

  private invalidateOperations(): void {
    this.requestGeneration++;
    this.request?.unsubscribe();
    this.request = undefined;
    this.items.set([]);
    this.error.set(null);
    this.loading.set(false);
  }

  private load(id: number): void {
    const generation = ++this.requestGeneration;
    this.loading.set(true);
    this.request = this.service.listarPorCarteira(id).pipe(
      finalize(() => { if (generation === this.requestGeneration) this.loading.set(false); }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: items => {
        if (generation !== this.requestGeneration || this.context.activeId() !== id) return;
        this.items.set(items);
        this.brokerNames.loadFor(items);
      },
      error: (error: NormalizedHttpError) => {
        if (generation === this.requestGeneration && this.context.activeId() === id) this.error.set(error);
      }
    });
  }
}
