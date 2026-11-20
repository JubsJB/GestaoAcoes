import { AdminOnlyDirective } from '../../../core/auth/admin-only.directive';
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
import { OperacoesCollectionComponent } from '../operacoes-collection.component';
import { OperacoesService } from '../operacoes.service';

@Component({
  providers: [CorretoraNamesService],
  selector: 'app-operacoes-list-page',
  imports: [AdminOnlyDirective,AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatProgressSpinnerModule, OperacoesCollectionComponent, PageHeaderComponent, RouterLink],
  templateUrl: './operacoes-list-page.component.html',
  styleUrl: '../operacoes-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperacoesListPageComponent {
  private readonly brokerNames = inject(CorretoraNamesService);
  protected readonly context = inject(CarteiraContextService);
  private readonly navigation = inject(CarteiraNavigationService);
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(OperacoesService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly items = signal<OperacaoResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<NormalizedHttpError | null>(null);
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
