import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';

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
  imports: [AppIconComponent, FeedbackAlertComponent, MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, PageHeaderComponent, ReactiveFormsModule, RouterLink],
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
          <table class="collection-table" role="table">
            <caption>Corretoras cadastradas</caption>
            <thead role="rowgroup"><tr role="row">
              <th scope="col" role="columnheader">Instituição</th>
              <th scope="col" role="columnheader">CNPJ</th>
              <th scope="col" role="columnheader">Localidade</th>
              <th scope="col" role="columnheader">Situação cadastral</th>
              <th scope="col" role="columnheader">Ações</th>
            </tr></thead>
            <tbody role="rowgroup">
              @for (corretora of corretoras(); track corretora.id) {
                <tr role="row">
                  <th scope="row" role="rowheader"><span class="cell-label" aria-hidden="true">Instituição</span>{{ corretora.razaoSocial }}
                    @if (corretora.nomeFantasia && corretora.nomeFantasia !== corretora.razaoSocial) { <small>{{ corretora.nomeFantasia }}</small> }
                  </th>
                  <td role="cell"><span class="cell-label" aria-hidden="true">CNPJ</span>{{ formatCnpj(corretora.cnpj) }}</td>
                  <td role="cell"><span class="cell-label" aria-hidden="true">Localidade</span>{{ corretora.cidade }} — {{ corretora.uf }}</td>
                  <td role="cell"><span class="cell-label" aria-hidden="true">Situação cadastral</span><strong [class]="'status-badge status-badge--' + statusVariant(corretora.situacaoCadastral)" [attr.data-status-variant]="statusVariant(corretora.situacaoCadastral)">{{ corretora.situacaoCadastral }}</strong></td>
                  <td role="cell" class="actions-cell"><span class="cell-label" aria-hidden="true">Ações</span><a class="details-link" [routerLink]="[corretora.id]" [attr.aria-label]="'Ver detalhes de ' + corretora.razaoSocial">Ver detalhes</a></td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>
    </section>
  `,
  styleUrl: '../../../shared/collection/refined-collection.scss',
  styles: [`
    .collection-page{height:calc(100dvh - 8rem);overflow:hidden;display:flex;flex-direction:column}.collection-region{min-height:0;overflow:auto;padding:.125rem .25rem .75rem 0;overscroll-behavior:contain;flex:1 1 auto}.search{display:flex;align-items:flex-start;gap:1rem}.search mat-form-field{flex:1 1 18rem}.search-progress{margin:0;color:var(--app-text-secondary)}
    @media(max-width:959.98px){.collection-page{height:calc(100dvh - 5.5rem)}}
    @media(max-width:36rem){.collection-page{height:auto;overflow:visible}.collection-region{overflow:visible;padding-right:0}.search{display:grid;grid-template-columns:1fr}.search mat-form-field{width:100%}}
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
      panelClass: ['app-not-found-dialog', 'app-dialog--compact'],
      autoFocus: 'first-tabbable',
      restoreFocus: true
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((register: boolean | undefined) => {
      if (register) this.openCreateDialog({ cnpj });
    });
  }

  protected openCreateDialog(data: { cnpj?: string } = {}): void {
    this.dialog.open(CorretoraCreatePageComponent, {
      data,
      panelClass: ['app-create-dialog', 'app-dialog--form'],
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
