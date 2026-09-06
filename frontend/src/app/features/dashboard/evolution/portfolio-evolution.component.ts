import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { Currency, formatFinancialMoney } from '../../../shared/formatters/financial-value.formatter';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { buildEvolutionGeometry, EvolutionGeometry, EvolutionGeometryPoint, segmentPath } from './evolution-geometry';
import { EvolucaoPatrimonialResponse } from './evolution.models';
import { EvolutionService } from './evolution.service';

@Component({
  selector: 'app-portfolio-evolution',
  imports: [FeedbackAlertComponent, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './portfolio-evolution.component.html',
  styleUrl: './portfolio-evolution.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PortfolioEvolutionComponent {
  readonly carteiraId = input.required<number>();
  readonly carteiraNome = input.required<string>();
  readonly refreshToken = input(0);

  private readonly service = inject(EvolutionService);
  private readonly toast = inject(SuccessToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly localReload = signal(0);

  protected readonly loading = signal(true);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly data = signal<EvolucaoPatrimonialResponse | null>(null);
  protected readonly creationError = signal<NormalizedHttpError | null>(null);
  protected readonly creatingForId = signal<number | null>(null);
  protected readonly activePoint = signal<string | null>(null);
  protected readonly creating = computed(() => this.creatingForId() !== null);
  protected readonly brl = computed(() => this.geometry('BRL'));
  protected readonly usd = computed(() => this.geometry('USD'));
  protected readonly observationCount = computed(() => this.data()?.pontos.length ?? 0);

  protected readonly money = formatFinancialMoney;
  protected readonly path = segmentPath;

  constructor() {
    effect(onCleanup => {
      const id = this.carteiraId();
      this.refreshToken();
      this.localReload();
      this.data.set(null);
      this.error.set(null);
      this.creationError.set(null);
      this.activePoint.set(null);
      this.loading.set(true);
      const subscription = this.service.consultar(id).pipe(
        finalize(() => {
          if (this.carteiraId() === id) this.loading.set(false);
        })
      ).subscribe({
        next: response => {
          if (this.carteiraId() === id) this.data.set(response);
        },
        error: (error: NormalizedHttpError) => {
          if (this.carteiraId() === id) this.error.set(error);
        }
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  protected retry(): void {
    this.localReload.update(value => value + 1);
  }

  protected registerSnapshot(): void {
    if (this.creating()) return;
    const originId = this.carteiraId();
    const originName = this.carteiraNome();
    this.creatingForId.set(originId);
    this.creationError.set(null);
    this.service.registrarSnapshot(originId).pipe(
      finalize(() => this.creatingForId.set(null)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => {
        if (this.carteiraId() !== originId) return;
        this.toast.show(`Snapshot de ${originName} registrado com sucesso.`);
        this.localReload.update(value => value + 1);
      },
      error: (error: NormalizedHttpError) => {
        if (this.carteiraId() === originId) this.creationError.set(error);
      }
    });
  }

  protected pointKey(currency: Currency, point: EvolutionGeometryPoint): string {
    return `${currency}-${point.snapshotId}`;
  }

  protected showPoint(currency: Currency, point: EvolutionGeometryPoint): void {
    this.activePoint.set(this.pointKey(currency, point));
  }

  protected hidePoint(currency: Currency, point: EvolutionGeometryPoint): void {
    if (this.activePoint() === this.pointKey(currency, point)) this.activePoint.set(null);
  }

  protected pointLabel(currency: Currency, point: EvolutionGeometryPoint): string {
    return `${currency}, ${formatFinancialMoney(point.authoritativeValue, currency)}, ${formatLocalDateTime(point.timestamp)}`;
  }

  protected tooltipX(point: EvolutionGeometryPoint): number {
    if (point.x < 92) return 92;
    if (point.x > 628) return 628;
    return point.x;
  }

  protected localDate(value: string): string {
    return formatLocalDateTime(value);
  }

  protected shortDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date);
  }

  private geometry(currency: Currency): EvolutionGeometry | null {
    return buildEvolutionGeometry(this.data()?.pontos ?? [], currency);
  }
}

export function formatLocalDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const formatted = new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
    second: '2-digit', fractionalSecondDigits: 3, hour12: false
  }).format(date);
  return `${formatted} (horário local)`;
}
