import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { displayOptional, formatCep, formatCnpj } from '../corretora-formatters';
import { Corretora } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';

@Component({
  selector:'app-corretora-detail-page', imports:[DatePipe,MatButtonModule,MatCardModule,MatProgressSpinnerModule,RouterLink],
  template:`<section class="page" aria-labelledby="detail-title"><a mat-button routerLink="/corretoras">← Voltar para corretoras</a>
  @if(loading()){<div class="state" role="status"><mat-spinner diameter="36"/> Carregando corretora…</div>}
  @else if(errorMessage()){<div class="state" role="alert"><h1 id="detail-title">{{notFound()?'Corretora não encontrada':'Não foi possível carregar a corretora'}}</h1><p>{{errorMessage()}}</p><button mat-stroked-button (click)="load()">Tentar novamente</button></div>}
  @else if(corretora();as item){<h1 id="detail-title">{{item.razaoSocial}}</h1><p>CNPJ {{formatCnpj(item.cnpj)}}</p><div class="grid">
  <mat-card appearance="outlined"><mat-card-header><mat-card-title>Identificação e contato</mat-card-title></mat-card-header><mat-card-content><dl><dt>Nome fantasia</dt><dd>{{optional(item.nomeFantasia)}}</dd><dt>E-mail</dt><dd>{{optional(item.email)}}</dd><dt>Telefone</dt><dd>{{optional(item.telefone)}}</dd></dl></mat-card-content></mat-card>
  <mat-card appearance="outlined"><mat-card-header><mat-card-title>Endereço</mat-card-title></mat-card-header><mat-card-content><address>{{item.logradouro}}, {{optional(item.numero)}}<br/>{{optional(item.complemento)}}<br/>{{item.bairro}} — {{item.cidade}}/{{item.uf}}<br/>CEP {{formatCep(item.cep)}}</address></mat-card-content></mat-card>
  <mat-card appearance="outlined"><mat-card-header><mat-card-title>Situação</mat-card-title></mat-card-header><mat-card-content><dl><dt>Situação cadastral</dt><dd>{{item.situacaoCadastral}}</dd><dt>Mercado financeiro</dt><dd>{{item.validadaMercadoFinanceiro?'Validação realizada':'Validação ainda não realizada'}}</dd><dt>Data de cadastro</dt><dd>{{item.dataCadastro|date:'medium'}}</dd></dl></mat-card-content></mat-card></div>}
  </section>`,
  styles:[`.page{display:grid;gap:1rem}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,20rem),1fr));gap:1rem}.state{min-height:10rem;display:grid;place-content:center;justify-items:center;gap:.75rem;text-align:center}dl{display:grid;grid-template-columns:minmax(8rem,auto) 1fr;gap:.5rem 1rem}dt{font-weight:600}dd{margin:0}address{font-style:normal;line-height:1.7}@media(max-width:36rem){dl{grid-template-columns:1fr;gap:.25rem}dd{margin-bottom:.5rem}}`], changeDetection:ChangeDetectionStrategy.OnPush
})
export class CorretoraDetailPageComponent {
 private readonly service=inject(CorretorasService);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);private readonly destroyRef=inject(DestroyRef);private readonly id=Number(this.route.snapshot.paramMap.get('id'));
 protected readonly corretora=signal<Corretora|null>(this.navigationBroker());protected readonly loading=signal(this.corretora()===null);protected readonly errorMessage=signal<string|null>(null);protected readonly notFound=signal(false);protected readonly formatCnpj=formatCnpj;protected readonly formatCep=formatCep;protected readonly optional=displayOptional;
 constructor(){if(!this.corretora())this.load();}
 protected load():void{this.loading.set(true);this.errorMessage.set(null);this.notFound.set(false);this.service.buscarPorId(this.id).pipe(finalize(()=>this.loading.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:item=>this.corretora.set(item),error:(error:NormalizedHttpError)=>{this.notFound.set(error.status===404);this.errorMessage.set(error.message);}});}
 private navigationBroker():Corretora|null{const candidate=(this.router.currentNavigation()?.extras.info as {corretora?:Corretora}|undefined)?.corretora;return candidate?.id===this.id?candidate:null;}
}
