import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, EMPTY, forkJoin, Subject, switchMap, tap, map, distinctUntilChanged } from 'rxjs';

import { CarteiraContextService } from '../../core/carteira/carteira-context.service';
import { CarteiraNavigationService } from '../../core/carteira/carteira-navigation.service';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../shared/feedback-alert/feedback-alert.component';
import { financialOutcomeLabel, formatFinancialMoney, formatFinancialPercent } from '../../shared/formatters/financial-value.formatter';
import { PortfolioPositionsComponent } from '../../shared/portfolio-positions/portfolio-positions.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { DashboardService } from './dashboard.service';
import { PortfolioEvolutionComponent } from './evolution/portfolio-evolution.component';
import { DashboardFinancialData } from './models/dashboard';

@Component({
  selector: 'app-dashboard-page',
  imports: [PortfolioPositionsComponent, AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatCardModule, MatProgressSpinnerModule, PageHeaderComponent, PortfolioEvolutionComponent, RouterLink],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPageComponent {
  private readonly context = inject(CarteiraContextService);
  private readonly navigation = inject(CarteiraNavigationService);
  private readonly dashboardService = inject(DashboardService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly financialRequests = new Subject<number | null>();

  protected readonly carteiras = this.context.carteiras;
  protected readonly portfoliosLoading = this.context.loading;
  protected readonly portfoliosError = this.context.error;
  protected readonly invalidSelection = this.context.invalidSelection;
  protected readonly selected = this.context.active;
  protected readonly financialLoading = signal(false);
  protected readonly financialError = signal<NormalizedHttpError | null>(null);
  protected readonly data = signal<DashboardFinancialData | null>(null);
  protected readonly evolutionRefresh = signal(0);

  protected readonly money = formatFinancialMoney;
  protected readonly percent = formatFinancialPercent;
  protected readonly outcome = financialOutcomeLabel;

  constructor() {
    this.financialRequests.pipe(
      tap(id => {
        this.data.set(null);
        this.financialError.set(null);
        this.financialLoading.set(id !== null);
      }),
      switchMap(id => id === null ? EMPTY : forkJoin({
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
    ).subscribe(id => this.financialRequests.next(id));
  }

  protected loadPortfolios(): void { this.context.initialize(true).subscribe(); }

  protected reload(): void {
    const portfolio = this.selected();
    if (portfolio) {
      this.financialRequests.next(portfolio.id);
      this.evolutionRefresh.update(value => value + 1);
    }
  }

  protected openOperationDialog(): void {
    const carteira = this.selected();
    if (carteira) void this.router.navigate(['/operacoes/nova'], { queryParams: { carteiraId: carteira.id, origem: 'dashboard' } });
  }
}
