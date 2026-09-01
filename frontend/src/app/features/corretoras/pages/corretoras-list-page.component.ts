import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AppIconComponent } from '../../../shared/app-icon/app-icon.component';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { corretoraStatusVariant, formatCnpj, onlyDigits } from '../corretora-formatters';
import { Corretora } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';
import { CorretoraNotFoundDialogComponent } from '../corretora-not-found-dialog.component';
import { CorretoraCreatePageComponent } from './corretora-create-page.component';

@Component({
  selector: 'app-corretoras-list-page',
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, PageHeaderComponent, ReactiveFormsModule, RouterLink],
  template: `
    <section class="app-page collection-page" aria-labelledby="corretoras-title">
      <app-page-header headingId="corretoras-title" eyebrow="Instituições financeiras" icon="broker" title="Corretoras" description="Consulte e cadastre instituições por CNPJ.">
        <button page-header-action mat-flat-button type="button" (click)="openCreateDialog()">Cadastrar corretora</button>
      </app-page-header>

      @if (loadError()) {
        <app-feedback-alert variant="error" [message]="loadError()!.message" [details]="loadError()!.details" />
      } @else if (searchError()) {
        <app-feedback-alert variant="error" [message]="searchError()!.message" [details]="searchError()!.details" />
      }

      <form class="search app-search-surface app-surface" [formGroup]="searchForm" (ngSubmit)="search()" aria-label="Buscar corretora por CNPJ" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>CNPJ exato</mat-label>
          <input matInput formControlName="cnpj" inputmode="numeric" maxlength="18" aria-describedby="search-hint" />
          <mat-hint id="search-hint">Informe os 14 dígitos, com ou sem máscara.</mat-hint>
          @if (searchControl.invalid && searchControl.touched) { <mat-error>Informe um CNPJ com 14 dígitos.</mat-error> }
        </mat-form-field>
        <div class="app-actions app-actions--stack-compact">
          <button mat-stroked-button type="submit" [disabled]="searching()" [attr.aria-busy]="searching()">Buscar</button>
          <button mat-button type="button" (click)="clearSearch()">Limpar</button>
        </div>
      </form>

      @if (searching()) { <p class="search-progress" role="status" aria-live="polite">Buscando corretora por CNPJ…</p> }

      <div class="collection-region" data-scroll-region="records">
        @if (loading()) {
          <div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36" /><span>Carregando corretoras…</span></div>
        } @else if (loadError()) {
          <div class="app-state"><button mat-stroked-button type="button" (click)="load()">Tentar novamente</button></div>
        } @else if (corretoras().length === 0) {
          <div class="app-state app-surface"><span class="app-state__icon" aria-hidden="true"><app-icon name="empty" /></span><h2>Você ainda não possui corretoras cadastradas.</h2><p>Cadastre sua primeira instituição para começar a organizar seus investimentos.</p><button mat-stroked-button type="button" (click)="openCreateDialog()">Cadastrar a primeira</button></div>
        } @else {
          <div class="broker-grid" aria-label="Corretoras cadastradas">
            @for (corretora of corretoras(); track corretora.id) {
              <mat-card class="entity-card entity-card--compact" appearance="outlined">
                <div class="entity-card__heading">
                  <span class="entity-card__icon" aria-hidden="true"><app-icon name="broker" /></span>
                  <div class="entity-card__identity">
                    <mat-card-title>{{ corretora.razaoSocial }}</mat-card-title>
                    @if (corretora.nomeFantasia && corretora.nomeFantasia !== corretora.razaoSocial) {
                      <mat-card-subtitle>{{ corretora.nomeFantasia }}</mat-card-subtitle>
                    }
                  </div>
                </div>
                <mat-card-content class="entity-card__data">
                  <div><span>CNPJ</span><strong>{{ formatCnpj(corretora.cnpj) }}</strong></div>
                  <div><span>Localidade</span><strong>{{ corretora.cidade }} — {{ corretora.uf }}</strong></div>
                  <div><span>Situação cadastral</span><strong class="status-badge" [class]="'status-badge status-badge--' + statusVariant(corretora.situacaoCadastral)" [attr.data-status-variant]="statusVariant(corretora.situacaoCadastral)">{{ corretora.situacaoCadastral }}</strong></div>
                </mat-card-content>
                <mat-card-actions><a mat-button [routerLink]="[corretora.id]" [attr.aria-label]="'Ver detalhes de ' + corretora.razaoSocial">Ver detalhes</a></mat-card-actions>
              </mat-card>
            }
          </div>
        }
      </div>
    </section>
  `,
  styles: [`
    .collection-page{height:calc(100dvh - 8rem);overflow:hidden}.collection-region{min-height:0;overflow:auto;padding:.125rem .25rem .75rem 0;overscroll-behavior:contain}.search{display:flex;align-items:flex-start;gap:1rem}.search mat-form-field{flex:1 1 18rem}.broker-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,20rem),1fr));gap:.75rem}.entity-card{border-color:var(--app-border-subtle);border-radius:var(--app-card-radius);background:var(--app-surface-card);box-shadow:0 .2rem .75rem rgb(31 36 29 / 5%)}.entity-card--compact{padding:.9rem 1rem .65rem}.entity-card__heading{display:flex;align-items:flex-start;gap:.75rem;min-width:0}.entity-card__icon{width:2.25rem;height:2.25rem;display:grid;place-items:center;flex:0 0 auto;border-radius:.7rem;color:var(--app-brand-primary);background:var(--app-surface-selected)}.entity-card__identity{min-width:0}.entity-card mat-card-title{font-size:1rem;font-weight:680;overflow-wrap:anywhere;line-height:1.3}.entity-card mat-card-subtitle{margin-top:.15rem;font-size:.8rem;overflow-wrap:anywhere}.entity-card__data{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:.7rem 1rem;padding:1rem 0 .35rem}.entity-card__data div{display:grid;gap:.2rem;min-width:0}.entity-card__data span:not(.status-badge){color:var(--app-text-secondary);font-size:.68rem;font-weight:700;letter-spacing:.055em;text-transform:uppercase}.entity-card__data strong{font-size:.84rem;font-weight:600;overflow-wrap:anywhere}.entity-card__data div:last-child{grid-column:1/-1}.entity-card mat-card-actions{min-height:auto;padding:.25rem 0 0;border-top:1px solid var(--app-border-subtle)}.search-progress{margin:0;color:var(--app-text-secondary)}
    .collection-page{display:flex;flex-direction:column}.collection-region{flex:1 1 auto}
    @media(max-width:959.98px){.collection-page{height:calc(100dvh - 5.5rem)}}
    @media(max-width:36rem){.collection-page{height:auto;overflow:visible}.collection-region{overflow:visible;padding-right:0}.search{display:grid;grid-template-columns:1fr}.search mat-form-field{width:100%}.entity-card__data{grid-template-columns:1fr}.entity-card__data div:last-child{grid-column:auto}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CorretorasListPageComponent {
  private readonly service = inject(CorretorasService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly successToast = inject(SuccessToastService);
  protected readonly corretoras = signal<Corretora[]>([]);
  protected readonly loading = signal(true);
  protected readonly searching = signal(false);
  protected readonly loadError = signal<NormalizedHttpError | null>(null);
  protected readonly searchError = signal<NormalizedHttpError | null>(null);
  protected readonly searchForm = new FormGroup({
    cnpj: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^(?:\d{14}|\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2})$/)] })
  });
  protected readonly searchControl = this.searchForm.controls.cnpj;
  protected readonly formatCnpj = formatCnpj;
  protected readonly statusVariant = corretoraStatusVariant;

  constructor() { this.load(); }

  protected load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.service.listar().pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (items) => this.corretoras.set(items),
      error: (error: NormalizedHttpError) => this.loadError.set(error)
    });
  }

  protected search(): void {
    this.searchControl.markAsTouched();
    if (this.searchControl.invalid || this.searching()) return;
    this.searching.set(true);
    this.searchError.set(null);
    const cnpj = onlyDigits(this.searchControl.value);
    this.service.buscarPorCnpj(cnpj).pipe(finalize(() => this.searching.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (corretora) => void this.router.navigate([corretora.id], { relativeTo: this.route, info: { corretora } }),
      error: (error: NormalizedHttpError) => {
        if (error.status === 404 && error.code === null) { this.openNotFoundDialog(cnpj); return; }
        this.searchError.set(error);
      }
    });
  }

  private openNotFoundDialog(cnpj: string): void {
    this.dialog.open(CorretoraNotFoundDialogComponent, {
      data: { cnpj: formatCnpj(cnpj) },
      width: '32rem',
      maxWidth: 'calc(100vw - 2rem)',
      panelClass: 'app-not-found-dialog',
      autoFocus: 'first-tabbable',
      restoreFocus: true
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((register: boolean | undefined) => {
      if (register) this.openCreateDialog({ cnpj });
    });
  }

  protected openCreateDialog(data: { cnpj?: string } = {}): void {
    this.dialog.open(CorretoraCreatePageComponent, {
      data,
      width: '42rem',
      maxWidth: 'calc(100vw - 2rem)',
      maxHeight: 'calc(100dvh - 2rem)',
      panelClass: 'app-create-dialog',
      autoFocus: 'first-tabbable',
      restoreFocus: true
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((created: Corretora | undefined) => {
      if (!created) return;
      this.corretoras.update((items) => [...items, created]);
      this.successToast.show('Corretora cadastrada com sucesso.');
    });
  }

  protected clearSearch(): void {
    this.searchControl.reset();
    this.searchError.set(null);
  }
}
