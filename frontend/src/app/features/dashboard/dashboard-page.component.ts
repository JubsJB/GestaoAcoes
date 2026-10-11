import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { SuccessToastService } from '../../shared/success-toast/success-toast.service';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, EMPTY, forkJoin, Subject, switchMap, tap, map, distinctUntilChanged } from 'rxjs';

import { CarteiraContextService } from '../../core/carteira/carteira-context.service';
import { CarteiraNavigationService } from '../../core/carteira/carteira-navigation.service';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent, FeedbackVariant } from '../../shared/feedback-alert/feedback-alert.component';
import { financialOutcomeLabel, formatFinancialMoney, formatFinancialPercent } from '../../shared/formatters/financial-value.formatter';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { DashboardService } from './dashboard.service';
import { PortfolioEvolutionComponent } from './evolution/portfolio-evolution.component';
import { DashboardFinancialData } from './models/dashboard';
import { AtualizacaoCotacoesCarteiraResponse } from './models/atualizacao-cotacoes';
import { PositionAnalysisComponent } from './position-analysis/position-analysis.component';

type OrigemAtualizacaoCotacoes = Readonly<{ carteiraId: number; nome: string }>;
type PedidoDadosFinanceiros = { carteiraId: number | null; origemAtualizacao?: OrigemAtualizacaoCotacoes };
type TentativaAtualizacaoCotacoes =
  | { readonly origem: OrigemAtualizacaoCotacoes; readonly estado: 'em-andamento' }
  | { readonly origem: OrigemAtualizacaoCotacoes; readonly estado: 'concluida'; readonly resultado: AtualizacaoCotacoesCarteiraResponse }
  | { readonly origem: OrigemAtualizacaoCotacoes; readonly estado: 'falha'; readonly erro: NormalizedHttpError };

@Component({
  selector: 'app-dashboard-page',
  imports: [PositionAnalysisComponent, AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatCardModule, MatProgressSpinnerModule, PageHeaderComponent, PortfolioEvolutionComponent, RouterLink],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPageComponent {
  private readonly context = inject(CarteiraContextService);
  private readonly navigation = inject(CarteiraNavigationService);
  private readonly dashboardService = inject(DashboardService);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly toast = inject(SuccessToastService);
  private operationOpening = false;
  private readonly destroyRef = inject(DestroyRef);
  private readonly financialRequests = new Subject<PedidoDadosFinanceiros>();
  private readonly financialReloadOrigin = signal<OrigemAtualizacaoCotacoes | null>(null);

  protected readonly carteiras = this.context.carteiras;
  protected readonly portfoliosLoading = this.context.loading;
  protected readonly portfoliosError = this.context.error;
  protected readonly invalidSelection = this.context.invalidSelection;
  protected readonly selected = this.context.active;
  protected readonly financialLoading = signal(false);
  protected readonly financialError = signal<NormalizedHttpError | null>(null);
  protected readonly data = signal<DashboardFinancialData | null>(null);
  protected readonly evolutionRefresh = signal(0);
  // Pertence à tentativa capturada, não aos dados/erros da seleção corrente.
  protected readonly quoteAttempt = signal<TentativaAtualizacaoCotacoes | null>(null);
  protected readonly quoteUpdating = computed(() => this.quoteAttempt()?.estado === 'em-andamento');
  protected readonly quoteDisabledReason = computed(() => {
    if (this.quoteUpdating()) return `Atualizando cotações de ${this.quoteAttempt()!.origem.nome}.`;
    const portfolio = this.selected();
    if (!portfolio || this.portfoliosLoading() || this.invalidSelection()) return 'Selecione uma carteira válida para atualizar as cotações.';
    const financial = this.data();
    if (this.financialLoading()) {
      return 'Aguarde o carregamento das posições para atualizar as cotações.';
    }
    if (this.financialError() || !financial || financial.resumo.carteiraId !== portfolio.id) {
      return 'Os dados das posições estão indisponíveis. Use Atualizar dados para carregá-los novamente.';
    }
    return financial.posicoes.length === 0 ? 'Esta carteira não possui posições abertas para atualizar cotações.' : null;
  });
  protected readonly quoteFeedback = computed<{ variant: FeedbackVariant; message: string } | null>(() => {
    const attempt = this.quoteAttempt();
    if (!attempt || attempt.estado === 'em-andamento') return null;
    const origin = `Atualização de cotações da carteira ${attempt.origem.nome} (ID ${attempt.origem.carteiraId}): `;
    if (attempt.estado === 'falha') {
      const message = attempt.erro.kind === 'standard'
        ? attempt.erro.message
        : 'Resultado não confirmado. Atualizações já confirmadas podem ter sido preservadas. Use Atualizar dados para consultar os indicadores antes de decidir por uma nova tentativa.';
      return { variant: 'error', message: origin + message };
    }
    const { atualizadas, semNovaCotacao, falhas } = attempt.resultado.resumo;
    const counts = `${atualizadas} atualizadas; ${semNovaCotacao} sem nova cotação; ${falhas} com falha.`;
    if (attempt.resultado.resultados.length === 0) {
      return { variant: 'info', message: origin + 'Nenhuma posição aberta foi processada. ' + counts };
    }
    if (falhas === 0 && semNovaCotacao === 0) {
      return { variant: 'success', message: origin + 'Cotações atualizadas com sucesso. ' + counts };
    }
    if (falhas === 0 && atualizadas === 0) {
      return { variant: 'info', message: origin + 'Nenhuma nova cotação. ' + counts };
    }
    const failed = attempt.resultado.resultados.filter(item => item.resultado === 'FALHA').map(item => item.ticker);
    const failures = failed.length ? ` Ativos com falha: ${failed.join(', ')}.` : '';
    if (atualizadas === 0 && semNovaCotacao === 0) {
      return { variant: 'error', message: origin + 'Todas as Ações falharam individualmente. ' + counts + failures };
    }
    return { variant: 'warning', message: origin + 'Resultado parcial. ' + counts + failures };
  });
  protected readonly financialErrorMessage = computed(() => {
    const error = this.financialError();
    const origin = this.financialReloadOrigin();
    return origin && error
      ? `Não foi possível recarregar os indicadores da carteira atual. O resultado da atualização de cotações da carteira ${origin.nome} (ID ${origin.carteiraId}) foi preservado. ${error.message}`
      : error?.message ?? '';
  });

  protected readonly money = formatFinancialMoney;
  protected readonly percent = formatFinancialPercent;
  protected readonly outcome = financialOutcomeLabel;

  constructor() {
    this.financialRequests.pipe(
      tap(({ carteiraId, origemAtualizacao }) => {
        this.financialReloadOrigin.set(origemAtualizacao ?? null);
        this.data.set(null);
        this.financialError.set(null);
        this.financialLoading.set(carteiraId !== null);
      }),
      switchMap(({ carteiraId: id }) => id === null ? EMPTY : forkJoin({
        resumo: this.dashboardService.obterResumo(id),
        posicoes: this.dashboardService.listarPosicoes(id),
        resultados: this.dashboardService.listarResultadosRealizados(id)
      }).pipe(
        catchError((error: NormalizedHttpError) => {
          this.financialError.set(error);
          this.financialLoading.set(false);
          return EMPTY;
        })
      )),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(data => {
      this.data.set(data);
      this.financialLoading.set(false);
    });

    this.navigation.bindDashboard(this.route, this.destroyRef);
    this.context.state$.pipe(
      map(state => state.status === 'ready' ? state.activeId : null),
      distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)
    ).subscribe(id => this.financialRequests.next({ carteiraId: id }));
  }

  protected loadPortfolios(): void { this.context.initialize(true).subscribe(); }

  protected reload(origemAtualizacao?: OrigemAtualizacaoCotacoes): void {
    const portfolio = this.selected();
    if (portfolio) {
      this.financialRequests.next({ carteiraId: portfolio.id, origemAtualizacao });
      this.evolutionRefresh.update(value => value + 1);
    }
  }

  protected atualizarCotacoes(): void {
    const portfolio = this.selected();
    if (!portfolio || this.quoteDisabledReason() !== null) return;
    const origem: OrigemAtualizacaoCotacoes = Object.freeze({ carteiraId: portfolio.id, nome: portfolio.nome });
    // Guarda síncrona antes de chamar o service, inclusive sem renderizar o template.
    this.quoteAttempt.set({ origem, estado: 'em-andamento' });
    this.dashboardService.atualizarCotacoes(origem.carteiraId).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: resultado => {
        this.quoteAttempt.set({ origem, estado: 'concluida', resultado });
        // O feedback pertence à origem; os quatro GETs pertencem à seleção corrente.
        this.reload(origem);
      },
      error: (erro: NormalizedHttpError) => this.quoteAttempt.set({ origem, estado: 'falha', erro })
    });
  }

  protected async openOperationDialog(): Promise<void> {
    const carteira = this.selected();
    if (!carteira || this.operationOpening) return;
    this.operationOpening = true;
    try {
      const { OperacaoFormPageComponent } = await import('../operacoes/pages/operacao-form-page.component');
      if (this.destroyRef.destroyed) return;
      const ref = this.dialog.open(OperacaoFormPageComponent, {
        data: { carteira }, panelClass: ['app-create-dialog', 'app-dialog--extended'],
        autoFocus: 'first-tabbable', restoreFocus: true,
        ariaLabelledBy: 'operacao-form-title', ariaDescribedBy: 'operacao-form-description'
      });
      ref.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(item => {
        this.operationOpening = false;
        if (item) {
          this.toast.show('Operação registrada com sucesso.');
          if (this.selected()?.id === carteira.id) this.financialRequests.next({ carteiraId: carteira.id });
        }
      });
    } catch { this.operationOpening = false; }
  }
}
