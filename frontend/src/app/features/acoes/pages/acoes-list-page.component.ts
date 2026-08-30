import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { formatCotacao, formatDataHora, formatMercado } from '../acao-formatters';
import { AcoesService } from '../acoes.service';
import { AcaoResponse, Mercado, normalizeTicker } from '../models/acao';

@Component({
  selector: 'app-acoes-list-page',
  imports: [MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page" aria-labelledby="acoes-title">
      <header class="page-header"><div><h1 id="acoes-title">Ações</h1><p>Consulte as ações e suas últimas cotações persistidas.</p></div><a mat-flat-button routerLink="nova">Cadastrar nova ação</a></header>
      <form class="search" [formGroup]="searchForm" (ngSubmit)="search()" aria-label="Buscar ação por ticker e mercado" novalidate>
        <mat-form-field appearance="outline"><mat-label>Ticker</mat-label><input matInput formControlName="ticker" maxlength="30" aria-describedby="ticker-search-hint"/><mat-hint id="ticker-search-hint">Informe o ticker exato.</mat-hint>@if(searchForm.controls.ticker.invalid&&searchForm.controls.ticker.touched){<mat-error>Ticker é obrigatório.</mat-error>}</mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Mercado</mat-label><mat-select formControlName="mercado" aria-describedby="mercado-search-hint"><mat-option value="BRASIL">Brasil</mat-option><mat-option value="EUA">EUA</mat-option></mat-select><mat-hint id="mercado-search-hint">Selecione o mercado.</mat-hint>@if(searchForm.controls.mercado.invalid&&searchForm.controls.mercado.touched){<mat-error>Mercado é obrigatório.</mat-error>}</mat-form-field>
        <button mat-stroked-button type="submit" [disabled]="searching()" [attr.aria-busy]="searching()">Buscar ação</button><button mat-button type="button" (click)="clearSearch()">Limpar busca</button>
      </form>
      @if(searching()){<p class="message" role="status" aria-live="polite">Buscando ação…</p>}@else if(searchMessage()){<p class="message" role="status" aria-live="polite">{{searchMessage()}}</p>}
      @if(loading()){<div class="state" role="status" aria-live="polite"><mat-spinner diameter="36"/><span>Carregando ações…</span></div>}
      @else if(error()){<div class="state" role="alert"><p>{{error()!.message}}</p><button mat-stroked-button type="button" (click)="load()">Tentar carregar ações novamente</button></div>}
      @else if(acoes().length===0){<div class="state"><p>Nenhuma ação cadastrada.</p><a mat-stroked-button routerLink="nova">Cadastrar a primeira ação</a></div>}
      @else{<div class="stock-grid" aria-label="Ações cadastradas">@for(acao of acoes();track acao.id){<mat-card appearance="outlined"><mat-card-header><mat-card-title>{{acao.ticker}}</mat-card-title><mat-card-subtitle>{{acao.nomeEmpresa}}</mat-card-subtitle></mat-card-header><mat-card-content><dl><dt>Mercado</dt><dd>{{market(acao.mercado)}}</dd><dt>Última cotação persistida</dt><dd>{{quote(acao.cotacaoAtual,acao.moeda)}}</dd><dt>Atualizada em</dt><dd>{{dateTime(acao.dataHoraCotacao)}}</dd></dl></mat-card-content><mat-card-actions><a mat-button [routerLink]="[acao.id]" [attr.aria-label]="'Ver detalhes de '+acao.ticker">Ver detalhes</a></mat-card-actions></mat-card>}</div>}
    </section>`,
  styles: [`.page{display:grid;gap:1.5rem}.page-header{display:flex;justify-content:space-between;align-items:flex-start;gap:1rem;flex-wrap:wrap}h1{margin:0}.search{display:flex;align-items:flex-start;gap:.75rem;flex-wrap:wrap}.search mat-form-field{flex:1 1 15rem}.stock-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,18rem),1fr));gap:1rem}.state{min-height:8rem;display:grid;place-content:center;justify-items:center;gap:.75rem;text-align:center}.message{padding:.75rem;border-inline-start:4px solid var(--mat-sys-primary);background:var(--mat-sys-surface-container)}dl{display:grid;grid-template-columns:auto 1fr;gap:.5rem 1rem}dt{font-weight:600}dd{margin:0}@media(max-width:36rem){.search{display:grid}.search mat-form-field{width:100%}dl{grid-template-columns:1fr;gap:.25rem}dd{margin-bottom:.5rem}}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AcoesListPageComponent {
  private readonly service=inject(AcoesService); private readonly router=inject(Router); private readonly route=inject(ActivatedRoute); private readonly destroyRef=inject(DestroyRef);
  protected readonly acoes=signal<AcaoResponse[]>([]); protected readonly loading=signal(true); protected readonly searching=signal(false); protected readonly error=signal<NormalizedHttpError|null>(null); protected readonly searchMessage=signal<string|null>(null);
  protected readonly searchForm=new FormGroup({ticker:new FormControl('',{nonNullable:true,validators:[Validators.required,Validators.maxLength(30)]}),mercado:new FormControl<Mercado|null>(null,{validators:[Validators.required]})});
  protected readonly market=formatMercado; protected readonly quote=formatCotacao; protected readonly dateTime=formatDataHora;
  constructor(){this.load();}
  protected load():void{this.loading.set(true);this.error.set(null);this.service.listar().pipe(finalize(()=>this.loading.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:items=>this.acoes.set(items),error:(error:NormalizedHttpError)=>this.error.set(error)});}
  protected search():void{this.searchForm.markAllAsTouched();const mercado=this.searchForm.controls.mercado.value;if(this.searchForm.invalid||!mercado||this.searching())return;this.searching.set(true);this.searchMessage.set(null);this.service.buscarPorTickerEMercado(normalizeTicker(this.searchForm.controls.ticker.value),mercado).pipe(finalize(()=>this.searching.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:acao=>void this.router.navigate([acao.id],{relativeTo:this.route,info:{acao}}),error:(error:NormalizedHttpError)=>this.searchMessage.set(error.status===404&&error.code===null?'Ação não encontrada para o ticker e mercado informados.':error.message)});}
  protected clearSearch():void{this.searchForm.reset({ticker:'',mercado:null});this.searchMessage.set(null);}
}
