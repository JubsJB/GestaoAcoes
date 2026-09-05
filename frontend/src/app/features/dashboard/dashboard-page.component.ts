import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, EMPTY, forkJoin, Subject, switchMap, tap } from 'rxjs';

import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../shared/feedback-alert/feedback-alert.component';
import { financialOutcomeLabel, formatFinancialMoney, formatFinancialPercent, formatFinancialQuantity } from '../../shared/formatters/financial-value.formatter';
import { formatOffsetDateTime } from '../../shared/formatters/offset-date-time.formatter';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { SuccessToastService } from '../../shared/success-toast/success-toast.service';
import { CarteirasService } from '../carteiras/carteiras.service';
import { CarteiraResponse } from '../carteiras/models/carteira';
import { OperacaoResponse } from '../operacoes/models/operacao';
import { OperacaoFormPageComponent } from '../operacoes/pages/operacao-form-page.component';
import { DashboardService } from './dashboard.service';
import { DashboardFinancialData } from './models/dashboard';

@Component({
  selector: 'app-dashboard-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatCardModule, MatFormFieldModule, MatProgressSpinnerModule, MatSelectModule, PageHeaderComponent, RouterLink],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPageComponent {
  private readonly carteirasService = inject(CarteirasService);
  private readonly dashboardService = inject(DashboardService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly toast = inject(SuccessToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly financialRequests = new Subject<number | null>();
  private queryValue: string | null = null;

  protected readonly carteiras = signal<CarteiraResponse[]>([]);
  protected readonly portfoliosLoading = signal(true);
  protected readonly portfoliosError = signal<NormalizedHttpError | null>(null);
  protected readonly invalidSelection = signal(false);
  protected readonly selected = signal<CarteiraResponse | null>(null);
  protected readonly financialLoading = signal(false);
  protected readonly financialError = signal<NormalizedHttpError | null>(null);
  protected readonly data = signal<DashboardFinancialData | null>(null);

  protected readonly money = formatFinancialMoney;
  protected readonly quantity = formatFinancialQuantity;
  protected readonly percent = formatFinancialPercent;
  protected readonly dateTime = formatOffsetDateTime;
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

    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.queryValue = params.get('carteiraId');
      if (!this.portfoliosLoading()) this.reconcileSelection();
    });
    this.loadPortfolios();
  }

  protected loadPortfolios(): void {
    this.portfoliosLoading.set(true);
    this.portfoliosError.set(null);
    this.carteirasService.listar().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => {
        this.carteiras.set(items);
        this.portfoliosLoading.set(false);
        this.reconcileSelection();
      },
      error: (error: NormalizedHttpError) => {
        this.portfoliosError.set(error);
        this.portfoliosLoading.set(false);
        this.clearFinancialContext();
      }
    });
  }

  protected selectPortfolio(event: MatSelectChange): void {
    const id = event.value as number | null;
    if (id === null) return;
    const match = this.carteiras().find(item => item.id === id);
    if (!match) return;
    this.queryValue = String(id);
    this.activate(match);
    void this.router.navigate([], { relativeTo: this.route, queryParams: { carteiraId: id }, queryParamsHandling: 'merge' });
  }

  protected reload(): void {
    const portfolio = this.selected();
    if (portfolio) this.financialRequests.next(portfolio.id);
  }

  protected openOperationDialog(): void {
    const carteira = this.selected();
    if (!carteira) return;
    this.dialog.open(OperacaoFormPageComponent, {
      data: { carteira }, width: '56rem', maxWidth: 'calc(100vw - 2rem)', maxHeight: 'calc(100dvh - 2rem)',
      panelClass: 'app-create-dialog', autoFocus: 'first-tabbable', restoreFocus: true,
      ariaLabelledBy: 'operacao-form-title', ariaDescribedBy: 'operacao-form-description'
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((created: OperacaoResponse | undefined) => {
      if (!created) return;
      this.toast.show('Operação registrada com sucesso.');
      this.reload();
    });
  }

  private reconcileSelection(): void {
    const items = this.carteiras();
    this.invalidSelection.set(false);
    if (items.length === 0) {
      this.clearFinancialContext();
      return;
    }

    if (this.queryValue === null || this.queryValue === '') {
      if (items.length === 1) {
        this.queryValue = String(items[0].id);
        this.activate(items[0]);
        void this.router.navigate([], { relativeTo: this.route, queryParams: { carteiraId: items[0].id }, queryParamsHandling: 'merge', replaceUrl: true });
      } else {
        this.clearFinancialContext();
      }
      return;
    }

    if (!/^[1-9]\d*$/.test(this.queryValue)) {
      this.invalidSelection.set(true);
      this.clearFinancialContext(false);
      return;
    }
    const id = Number(this.queryValue);
    const match = items.find(item => item.id === id) ?? null;
    if (!match) {
      this.invalidSelection.set(true);
      this.clearFinancialContext(false);
      return;
    }
    this.activate(match);
  }

  private activate(match: CarteiraResponse): void {
    if (this.selected()?.id === match.id && (this.data() || this.financialLoading())) return;
    this.selected.set(match);
    this.invalidSelection.set(false);
    this.financialRequests.next(match.id);
  }

  private clearFinancialContext(clearInvalid = true): void {
    this.selected.set(null);
    this.data.set(null);
    this.financialError.set(null);
    this.financialLoading.set(false);
    if (clearInvalid) this.invalidSelection.set(false);
    this.financialRequests.next(null);
  }
}
