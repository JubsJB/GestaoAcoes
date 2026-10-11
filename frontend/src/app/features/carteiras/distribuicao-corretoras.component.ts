import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription } from 'rxjs';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../shared/app-icon/app-icon.component';
import { formatFinancialQuantity } from '../../shared/formatters/financial-value.formatter';
import { formatCivilDate } from '../operacoes/operacao-validators';
import { DistribuicaoCorretorasService } from './distribuicao-corretoras.service';
import { DistribuicaoCarteiraResponse, PosicaoDistribuicaoResponse } from './models/distribuicao-corretoras';

@Component({
  selector: 'app-distribuicao-corretoras',
  imports: [AppIconComponent, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './distribuicao-corretoras.component.html',
  styleUrl: './distribuicao-corretoras.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DistribuicaoCorretorasComponent {
  readonly carteiraId = input.required<number>();
  readonly refreshVersion = input(0);
  private readonly service = inject(DistribuicaoCorretorasService);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  private generation = 0;
  protected readonly data = signal<DistribuicaoCarteiraResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly announcement = signal('');
  protected readonly quantity = formatFinancialQuantity;
  protected readonly civilDate = formatCivilDate;

  constructor() {
    effect(onCleanup => {
      const id = this.carteiraId();
      this.refreshVersion();
      this.load(id);
      onCleanup(() => { this.generation++; this.request?.unsubscribe(); });
    });
  }

  protected retry(): void { this.load(this.carteiraId()); }

  protected hasWarnings(position: PosicaoDistribuicaoResponse): boolean {
    return position.diagnosticoReconciliacao !== null
      || position.diagnosticosCorretoras.some(d => d.saldoNegativoAtual || d.possuiDeficitHistorico);
  }

  protected errorMessage(): string {
    const failure = this.error();
    if (failure?.originalError?.statusText === 'Resposta de distribuicao invalida.') {
      return 'Não foi possível validar os dados da distribuição. Tente novamente.';
    }
    return failure?.kind === 'standard' ? failure.message
      : 'Não foi possível carregar a distribuição por corretora. Tente novamente.';
  }

  private load(id: number): void {
    const generation = ++this.generation;
    this.request?.unsubscribe();
    this.data.set(null); this.error.set(null); this.loading.set(true);
    this.announcement.set('Carregando distribuição por corretora.');
    this.request = this.service.consultar(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: value => {
        if (generation !== this.generation || value.carteiraId !== id) return;
        this.data.set(value); this.loading.set(false);
        this.announcement.set(value.posicoes.length === 0 ? 'Nenhuma distribuição de posições abertas disponível.'
          : value.posicoes.some(p => this.hasWarnings(p))
            ? 'Distribuição carregada. Há avisos nas posições.' : 'Distribuição carregada.');
      },
      error: (failure: NormalizedHttpError) => {
        if (generation !== this.generation) return;
        this.error.set(failure); this.loading.set(false); this.announcement.set('');
      }
    });
  }
}
