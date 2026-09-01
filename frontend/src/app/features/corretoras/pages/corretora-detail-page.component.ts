import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { formatOffsetDateTime } from '../../../shared/formatters/offset-date-time.formatter';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { StickyBackComponent } from '../../../shared/sticky-back/sticky-back.component';
import { displayOptional, formatCep, formatCnpj } from '../corretora-formatters';
import { Corretora } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';

@Component({
  selector: 'app-corretora-detail-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatCardModule, MatProgressSpinnerModule, PageHeaderComponent, RouterLink, StickyBackComponent],
  template: `
    <section class="app-page" aria-labelledby="detail-title">
      <app-sticky-back route="/corretoras" label="Voltar para corretoras" />
      @if (corretora(); as item) {
        <app-page-header headingId="detail-title" eyebrow="Detalhe da corretora" icon="broker" [title]="item.razaoSocial" [description]="'CNPJ ' + formatCnpj(item.cnpj)" />
      } @else {
        <app-page-header headingId="detail-title" eyebrow="Corretoras" icon="broker" [title]="notFound() ? 'Corretora não encontrada' : error() ? 'Não foi possível carregar a corretora' : 'Detalhe da corretora'" description="Consulte os dados cadastrais e de validação da instituição." />
      }

      @if (error()) { <app-feedback-alert variant="error" [message]="error()!.message" [details]="error()!.details" /> }
      @if (loading()) {
        <div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36" /> Carregando corretora…</div>
      } @else if (error()) {
        <div class="app-state">
          @if (notFound()) { <a mat-stroked-button routerLink="/corretoras">Voltar para a listagem</a> }
          @else { <button mat-stroked-button type="button" (click)="load()">Tentar novamente</button> }
        </div>
      } @else if (corretora(); as item) {
        <div class="grid">
          <mat-card class="section-card" appearance="outlined"><div class="section-card__heading"><app-icon name="identity" aria-hidden="true" /><h2>Identificação e contato</h2></div><mat-card-content><dl class="data-list"><div><dt>Nome fantasia</dt><dd>{{ optional(item.nomeFantasia) }}</dd></div><div><dt>E-mail</dt><dd>{{ optional(item.email) }}</dd></div><div><dt>Telefone</dt><dd>{{ optional(item.telefone) }}</dd></div></dl></mat-card-content></mat-card>
          <mat-card class="section-card" appearance="outlined"><div class="section-card__heading"><app-icon name="location" aria-hidden="true" /><h2>Endereço</h2></div><mat-card-content><address>{{ item.logradouro }}, {{ optional(item.numero) }}<br />{{ optional(item.complemento) }}<br />{{ item.bairro }} — {{ item.cidade }}/{{ item.uf }}<br />CEP {{ formatCep(item.cep) }}</address></mat-card-content></mat-card>
          <mat-card class="section-card" appearance="outlined"><div class="section-card__heading"><app-icon name="status" aria-hidden="true" /><h2>Situação</h2></div><mat-card-content><dl class="data-list"><div><dt>Situação cadastral</dt><dd><span class="status-badge">{{ item.situacaoCadastral }}</span></dd></div><div><dt>Mercado financeiro</dt><dd>{{ item.validadaMercadoFinanceiro ? 'Validação realizada' : 'Validação ainda não realizada' }}</dd></div><div><dt>Data de cadastro</dt><dd>{{ dateTime(item.dataCadastro) }}</dd></div></dl></mat-card-content></mat-card>
        </div>
      }
    </section>
  `,
  styles: [`
    .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,20rem),1fr));gap:1rem;align-items:start}.section-card mat-card-content{padding:1.25rem}.section-card:nth-child(1){grid-column:span 2}address{padding:1.25rem;font-style:normal;font-weight:550;line-height:1.7;overflow-wrap:anywhere}@media(max-width:48rem){.section-card:nth-child(1){grid-column:auto}}
  `],
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
