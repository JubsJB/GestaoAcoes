import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { AcoesService } from './acoes.service';
import { ACOES_ROUTES } from './acoes.routes';

describe('Ações routes',()=>{
  const stock={id:7,ticker:'PETR4',nomeEmpresa:'Petrobras',mercado:'BRASIL' as const,moeda:'BRL' as const,cotacaoAtual:30,dataHoraCotacao:'2026-08-29T12:00:00Z'};
  const service={listar:()=>of([]),buscarPorId:()=>of(stock)};
  beforeEach(()=>TestBed.configureTestingModule({providers:[provideRouter(ACOES_ROUTES),{provide:AcoesService,useValue:service}]}));
  it('lazy-loads the list at the feature root',async()=>{const harness=await RouterTestingHarness.create('/');expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Ações');});
  it('resolves nova before the id parameter',async()=>{const harness=await RouterTestingHarness.create('/nova');expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Cadastrar ação');expect(TestBed.inject(Router).url).toBe('/nova');});
  it('lazy-loads the detail route',async()=>{const harness=await RouterTestingHarness.create('/7');expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('PETR4');});
  it('contains no placeholder component import',()=>{expect(JSON.stringify(ACOES_ROUTES)).not.toContain('Placeholder');expect(ACOES_ROUTES.map(route=>route.path)).toEqual(['','nova',':id']);});
});
