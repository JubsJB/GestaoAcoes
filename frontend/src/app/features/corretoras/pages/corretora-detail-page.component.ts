import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { formatOffsetDateTime } from '../../../shared/formatters/offset-date-time.formatter';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { StickyBackComponent } from '../../../shared/sticky-back/sticky-back.component';
import { displayOptional, formatCep, formatCnpj } from '../corretora-formatters';
import { Corretora } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';

@Component({
  selector: 'app-corretora-detail-page',
  imports: [FeedbackAlertComponent, MatButtonModule, MatCardModule, MatProgressSpinnerModule, PageHeaderComponent, RouterLink, StickyBackComponent],
  template: `
    <section class="app-page" aria-labelledby="detail-title">
      <app-sticky-back route="/corretoras" label="Voltar para corretoras" />
      @if (corretora(); as item) {
        <app-page-header headingId="detail-title" eyebrow="Instituições financeiras" title="Detalhe da corretora" description="Dados cadastrais da instituição." />
      } @else {
        <app-page-header headingId="detail-title" eyebrow="Corretoras" [title]="notFound() ? 'Corretora não encontrada' : error() ? 'Não foi possível carregar a corretora' : 'Detalhe da corretora'" description="Consulte os dados cadastrais e de validação da instituição." />
      }

      @if (error()) { <app-feedback-alert variant="error" [message]="error()!.message" /> }
      @if (loading()) {
        <div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36" /> Carregando corretora…</div>
      } @else if (error()) {
        <div class="app-state">
          @if (notFound()) { <a mat-stroked-button routerLink="/corretoras">Voltar para a listagem</a> }
          @else { <button mat-stroked-button type="button" (click)="load()">Tentar novamente</button> }
        </div>
      } @else if (corretora(); as item) {
        <mat-card class="broker-panel broker-identity" appearance="outlined"><mat-card-content>
          <p class="broker-eyebrow">Razão social</p><h2>{{ item.razaoSocial }}</h2>
          <dl class="data-list identity-facts"><div><dt>CNPJ</dt><dd>{{ formatCnpj(item.cnpj) }}</dd></div><div><dt>Situação cadastral</dt><dd><span class="status-badge">{{ item.situacaoCadastral }}</span></dd></div><div><dt>Data de cadastro</dt><dd>{{ dateTime(item.dataCadastro) }}</dd></div></dl>
        </mat-card-content></mat-card>
        <div class="broker-detail-grid">
          <mat-card class="broker-panel" appearance="outlined"><mat-card-content><h2>Identificação e contato</h2><dl class="data-list contact-facts"><div><dt>Nome fantasia</dt><dd>{{ optional(item.nomeFantasia) }}</dd></div><div><dt>Mercado financeiro</dt><dd>{{ item.validadaMercadoFinanceiro ? 'Validação realizada' : 'Validação ainda não realizada' }}</dd></div><div><dt>E-mail</dt><dd>{{ optional(item.email) }}</dd></div><div><dt>Telefone</dt><dd>{{ optional(item.telefone) }}</dd></div></dl></mat-card-content></mat-card>
          <mat-card class="broker-panel" appearance="outlined"><mat-card-content><h2>Endereço</h2><dl class="data-list address-facts"><div class="full-width"><dt>Logradouro</dt><dd>{{ item.logradouro }}</dd></div><div><dt>Número</dt><dd>{{ optional(item.numero) }}</dd></div><div><dt>Complemento</dt><dd>{{ optional(item.complemento) }}</dd></div><div><dt>Bairro</dt><dd>{{ item.bairro }}</dd></div><div><dt>Cidade / UF</dt><dd>{{ item.cidade }}/{{ item.uf }}</dd></div><div class="full-width"><dt>CEP</dt><dd>{{ formatCep(item.cep) }}</dd></div></dl></mat-card-content></mat-card>
        </div>
      }
    </section>
  `,
  styleUrl: '../corretora-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CorretoraDetailPageComponent {
  private readonly service = inject(CorretorasService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly id = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly corretora = signal<Corretora | null>(this.navigationBroker());
  protected readonly loading = signal(this.corretora() === null);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly notFound = signal(false);
  protected readonly formatCnpj = formatCnpj;
  protected readonly formatCep = formatCep;
  protected readonly optional = displayOptional;
  protected readonly dateTime = formatOffsetDateTime;

  constructor() { if (!this.corretora()) this.load(); }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.notFound.set(false);
    this.service.buscarPorId(this.id).pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (item) => this.corretora.set(item),
      error: (error: NormalizedHttpError) => { this.notFound.set(error.status === 404); this.error.set(error); }
    });
  }

  private navigationBroker(): Corretora | null {
    const candidate = (this.router.currentNavigation()?.extras.info as { corretora?: Corretora } | undefined)?.corretora;
    return candidate?.id === this.id ? candidate : null;
  }
}
