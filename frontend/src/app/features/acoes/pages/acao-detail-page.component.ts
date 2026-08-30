import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { formatCotacao, formatDataHora, formatMercado } from '../acao-formatters';
import { AcoesService } from '../acoes.service';
import { AcaoResponse } from '../models/acao';

@Component({selector:'app-acao-detail-page',imports:[MatButtonModule,MatCardModule,MatProgressSpinnerModule,RouterLink],template:`<section class="page" aria-labelledby="detail-title"><a mat-button routerLink="/acoes">← Voltar para ações</a>
  @if(loading()){<div class="state" role="status" aria-live="polite"><mat-spinner diameter="36"/>Carregando ação…</div>}
  @else if(notFound()){<div class="state" role="alert"><h1 id="detail-title">Ação não encontrada</h1><p>{{loadError()?.message}}</p><a mat-stroked-button routerLink="/acoes">Voltar para a listagem de ações</a></div>}
  @else if(loadError()){<div class="state" role="alert"><h1 id="detail-title">Não foi possível carregar a ação</h1><p>{{loadError()!.message}}</p><button mat-stroked-button type="button" (click)="load()">Tentar carregar a ação novamente</button></div>}
  @else if(acao();as item){<header class="page-header"><div><h1 id="detail-title">{{item.ticker}}</h1><p>{{item.nomeEmpresa}}</p></div><button mat-flat-button type="button" (click)="updateQuote()" [disabled]="updating()" [attr.aria-busy]="updating()">Atualizar cotação</button></header>
  @if(updating()){<p class="message" role="status" aria-live="polite">Atualizando cotação…</p>}@else if(updateError();as currentError){<div class="message error" role="alert"><p>{{currentError.message}}</p>@if(detailEntries(currentError).length){<dl>@for(entry of detailEntries(currentError);track entry[0]){<dt>{{entry[0]}}</dt><dd>{{entry[1]}}</dd>}</dl>}</div>}
  <div class="grid"><mat-card appearance="outlined"><mat-card-header><mat-card-title>Identificação</mat-card-title></mat-card-header><mat-card-content><dl><dt>Ticker</dt><dd>{{item.ticker}}</dd><dt>Empresa</dt><dd>{{item.nomeEmpresa}}</dd><dt>Mercado</dt><dd>{{market(item.mercado)}}</dd></dl></mat-card-content></mat-card><mat-card appearance="outlined"><mat-card-header><mat-card-title>Última cotação persistida</mat-card-title></mat-card-header><mat-card-content><dl><dt>Moeda</dt><dd>{{item.moeda}}</dd><dt>Cotação</dt><dd>{{quote(item.cotacaoAtual,item.moeda)}}</dd><dt>Atualizada em</dt><dd>{{dateTime(item.dataHoraCotacao)}}</dd></dl></mat-card-content></mat-card></div>}
  </section>`,styles:[`.page{display:grid;gap:1rem}.page-header{display:flex;justify-content:space-between;align-items:flex-start;gap:1rem;flex-wrap:wrap}h1{margin:0}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,20rem),1fr));gap:1rem}.state{min-height:10rem;display:grid;place-content:center;justify-items:center;gap:.75rem;text-align:center}.message{padding:.75rem;border-inline-start:4px solid var(--mat-sys-primary);background:var(--mat-sys-surface-container)}.error{border-color:var(--mat-sys-error)}dl{display:grid;grid-template-columns:minmax(8rem,auto) 1fr;gap:.5rem 1rem}dt{font-weight:600}dd{margin:0}@media(max-width:36rem){.page-header button{width:100%}dl{grid-template-columns:1fr;gap:.25rem}dd{margin-bottom:.5rem}}`],changeDetection:ChangeDetectionStrategy.OnPush})
export class AcaoDetailPageComponent{
  private readonly service=inject(AcoesService);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);private readonly snackBar=inject(MatSnackBar);private readonly destroyRef=inject(DestroyRef);private readonly id=Number(this.route.snapshot.paramMap.get('id'));
  protected readonly acao=signal<AcaoResponse|null>(this.navigationStock());protected readonly loading=signal(this.acao()===null);protected readonly loadError=signal<NormalizedHttpError|null>(null);protected readonly notFound=signal(false);protected readonly updating=signal(false);protected readonly updateError=signal<NormalizedHttpError|null>(null);protected readonly market=formatMercado;protected readonly quote=formatCotacao;protected readonly dateTime=formatDataHora;
  constructor(){if(!this.acao())this.load();}
  protected load():void{this.loading.set(true);this.loadError.set(null);this.notFound.set(false);this.service.buscarPorId(this.id).pipe(finalize(()=>this.loading.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:item=>this.acao.set(item),error:(error:NormalizedHttpError)=>{this.notFound.set(error.status===404&&error.code===null);this.loadError.set(error);}});}
  protected updateQuote():void{if(this.updating()||!this.acao())return;this.updating.set(true);this.updateError.set(null);this.service.atualizarCotacao(this.id).pipe(finalize(()=>this.updating.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:item=>{this.acao.set(item);this.snackBar.open('Cotação atualizada com sucesso.','Fechar',{duration:5000});},error:(error:NormalizedHttpError)=>this.updateError.set(error)});}
  protected detailEntries(error:NormalizedHttpError):[string,string][]{return Object.entries(error.details).map(([key,value])=>[key,typeof value==='string'?value:JSON.stringify(value)]);}
  private navigationStock():AcaoResponse|null{const candidate=(this.router.currentNavigation()?.extras.info as {acao?:AcaoResponse}|undefined)?.acao;return candidate?.id===this.id?candidate:null;}
}
