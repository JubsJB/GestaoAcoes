import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { formatCnpj, onlyDigits } from '../corretora-formatters';
import { Corretora } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';

@Component({
  selector: 'app-corretoras-list-page',
  imports: [MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page" aria-labelledby="corretoras-title">
      <header class="page-header">
        <div><h1 id="corretoras-title">Corretoras</h1><p>Consulte e cadastre instituições por CNPJ.</p></div>
        <a mat-flat-button routerLink="nova">Cadastrar corretora</a>
      </header>

      <form class="search" (ngSubmit)="search()" aria-label="Buscar corretora por CNPJ">
        <mat-form-field appearance="outline">
          <mat-label>CNPJ exato</mat-label>
          <input matInput [formControl]="searchControl" inputmode="numeric" maxlength="18" aria-describedby="search-hint" />
          <mat-hint id="search-hint">Informe os 14 dígitos, com ou sem máscara.</mat-hint>
          @if (searchControl.invalid && searchControl.touched) { <mat-error>Informe um CNPJ com 14 dígitos.</mat-error> }
        </mat-form-field>
        <button mat-stroked-button type="submit" [disabled]="searching()">Buscar</button>
        <button mat-button type="button" (click)="clearSearch()">Limpar</button>
      </form>

      @if (searching()) { <p class="message" role="status" aria-live="polite">Buscando corretora por CNPJâ€¦</p> }
      @else if (searchMessage()) { <p class="message" role="status" aria-live="polite">{{ searchMessage() }}</p> }
      @if (loading()) {
        <div class="state" role="status"><mat-spinner diameter="36" /><span>Carregando corretoras…</span></div>
      } @else if (errorMessage()) {
        <div class="state" role="alert"><p>{{ errorMessage() }}</p><button mat-stroked-button type="button" (click)="load()">Tentar novamente</button></div>
      } @else if (corretoras().length === 0) {
        <div class="state"><p>Nenhuma corretora cadastrada.</p><a mat-stroked-button routerLink="nova">Cadastrar a primeira</a></div>
      } @else {
        <div class="broker-grid" aria-label="Corretoras cadastradas">
          @for (corretora of corretoras(); track corretora.id) {
            <mat-card appearance="outlined">
              <mat-card-header><mat-card-title>{{ corretora.razaoSocial }}</mat-card-title></mat-card-header>
              <mat-card-content><p><strong>CNPJ:</strong> {{ formatCnpj(corretora.cnpj) }}</p><p>{{ corretora.cidade }} — {{ corretora.uf }}</p></mat-card-content>
              <mat-card-actions><a mat-button [routerLink]="[corretora.id]" [attr.aria-label]="'Ver detalhes de ' + corretora.razaoSocial">Ver detalhes</a></mat-card-actions>
            </mat-card>
          }
        </div>
      }
    </section>
  `,
  styles: [`
    .page{display:grid;gap:1.5rem}.page-header{display:flex;justify-content:space-between;align-items:flex-start;gap:1rem;flex-wrap:wrap}h1{margin:0}.search{display:flex;align-items:flex-start;gap:.75rem;flex-wrap:wrap}.search mat-form-field{flex:1 1 18rem}.broker-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,18rem),1fr));gap:1rem}.state{min-height:8rem;display:grid;place-content:center;justify-items:center;gap:.75rem;text-align:center}.message{padding:.75rem;border-inline-start:4px solid var(--mat-sys-primary);background:var(--mat-sys-surface-container)}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CorretorasListPageComponent {
  private readonly service = inject(CorretorasService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly corretoras = signal<Corretora[]>([]);
  protected readonly loading = signal(true);
  protected readonly searching = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly searchMessage = signal<string | null>(null);
  protected readonly searchControl = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^(?:\d{14}|\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2})$/)] });
  protected readonly formatCnpj = formatCnpj;

  constructor() { this.load(); }

  protected load(): void {
    this.loading.set(true); this.errorMessage.set(null);
    this.service.listar().pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (items) => this.corretoras.set(items),
      error: (error: NormalizedHttpError) => this.errorMessage.set(error.message)
    });
  }

  protected search(): void {
    this.searchControl.markAsTouched();
    if (this.searchControl.invalid || this.searching()) return;
    this.searching.set(true); this.searchMessage.set(null);
    this.service.buscarPorCnpj(onlyDigits(this.searchControl.value)).pipe(finalize(() => this.searching.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (corretora) => void this.router.navigate([corretora.id], { relativeTo: this.route, info: { corretora } }),
      error: (error: NormalizedHttpError) => this.searchMessage.set(error.status === 404 ? 'Nenhuma corretora encontrada para este CNPJ.' : error.message)
    });
  }

  protected clearSearch(): void { this.searchControl.reset(); this.searchMessage.set(null); }
}
