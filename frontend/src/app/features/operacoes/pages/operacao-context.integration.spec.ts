import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { CARTEIRA_STORAGE, CarteiraContextService } from '../../../core/carteira/carteira-context.service';
import { CarteiraNavigationService } from '../../../core/carteira/carteira-navigation.service';
import { provideApiConfig } from '../../../core/config/api.config';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { AcoesService } from '../../acoes/acoes.service';
import { CarteirasService } from '../../carteiras/carteiras.service';
import { CorretorasService } from '../../corretoras/corretoras.service';
import { OperacaoFormPageComponent } from './operacao-form-page.component';
import { OperacaoDetailPageComponent } from './operacao-detail-page.component';

@Component({ template: '<h1>Destino</h1>' })
class Destination {}
const A = { id: 1, nome: 'A', dataCriacao: '' };
const B = { ...A, id: 2, nome: 'B' };
type Form = { form: FormGroup; submit(): void; submitBlocked(): boolean };
describe('Operation origin through real Router and HTTP', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([
      { path: 'operacoes/nova', component: OperacaoFormPageComponent }, { path: 'operacoes/:id', component: OperacaoDetailPageComponent },
      { path: 'operacoes', component: Destination }, { path: 'dashboard', component: Destination }, { path: 'carteiras/:id', component: Destination }
    ]), provideHttpClient(), provideHttpClientTesting(), provideApiConfig(),
      { provide: CARTEIRA_STORAGE, useValue: null }, { provide: CarteirasService, useValue: { listar: () => of([B, A]) } },
      { provide: AcoesService, useValue: { listar: () => of([{ id: 3, ticker: 'AAPL', mercado: 'EUA', moeda: 'USD' }]) } },
      { provide: CorretorasService, useValue: { listar: () => of([]) } }, { provide: SuccessToastService, useValue: { show: vi.fn() } }
    ] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it.each(['COMPRA', 'VENDA'])('%s keeps A through a global switch to B and the POST', async tipo => {
    const harness = await RouterTestingHarness.create();
    const page = await harness.navigateByUrl('/operacoes/nova?carteiraId=1&origem=dashboard', OperacaoFormPageComponent);
    const form = page as unknown as Form;
    TestBed.inject(CarteiraNavigationService).select(2);
    expect(TestBed.inject(CarteiraContextService).activeId()).toBe(2);
    form.form.patchValue({ tipo, acaoKey: 'AAPL|EUA', quantidade: '0,5', dataOperacao: '2026-08-31' });
    if (tipo === 'VENDA') {
      http.expectOne(request => request.url === '/api/carteiras/1/operacoes/sugestao-preco-venda').flush('{"precoUnitarioSugerido":10.000000}');
      form.form.controls['precoUnitario'].setValue('12,30');
    } else {
      const preview = http.expectOne(request => request.url === '/api/operacoes/previa-compra');
      expect(preview.request.params.get('dataOperacao')).toBe('2026-08-31');
      preview.flush('{"ticker":"AAPL","mercado":"EUA","moeda":"USD","dataCotacao":"2026-08-31","precoUnitario":42.300000}');
    }
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('.context')?.textContent).toContain('A');
    expect(harness.routeNativeElement?.querySelector('[formcontrolname="carteiraId"]')).toBeNull();
    form.submit();
    const post = http.expectOne('/api/operacoes');
    expect(post.request.body).toEqual({ carteiraId: 1, ticker: 'AAPL', mercado: 'EUA', corretoraId: null, quantidade: '0.5', dataOperacao: '2026-08-31', tipo, ...(tipo === 'VENDA' ? { precoUnitario: '12.30' } : {}) });
    post.flush('{"id":9,"carteiraId":1,"ticker":"AAPL","mercado":"EUA","tipo":"COMPRA","quantidade":0.5,"precoUnitario":42.3,"valorTotal":21.15,"dataOperacao":"2026-08-31","ordemNoDia":1}');
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/dashboard?carteiraId=1');
  });

  it('reconstructs contextual and global return after a fresh page instance', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/operacoes/nova?carteiraId=1&origem=carteira');
    expect(harness.routeNativeElement?.querySelector('a.app-back-action')?.getAttribute('href')).toBe('/carteiras/1');
    await harness.navigateByUrl('/operacoes');
    TestBed.inject(CarteiraContextService).select(2);
    await harness.navigateByUrl('/operacoes/nova?carteiraId=1&origem=carteira');
    expect(harness.routeNativeElement?.querySelector('.context')?.textContent).toContain('A');
    expect(harness.routeNativeElement?.querySelector('a.app-back-action')?.getAttribute('href')).toBe('/carteiras/1');
    await harness.navigateByUrl('/operacoes');
    await harness.navigateByUrl('/operacoes/nova'); await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/operacoes/nova?carteiraId=1');
    expect(harness.routeNativeElement?.querySelector('a.app-back-action')?.getAttribute('href')).toBe('/operacoes');
  });
  it.each(['?carteiraId=99', '?carteiraId=bad&origem=carteira', '?origem=dashboard'])('blocks invalid origin %s without a fallback POST', async query => {
    const harness = await RouterTestingHarness.create();
    const page = await harness.navigateByUrl('/operacoes/nova' + query, OperacaoFormPageComponent);
    expect((page as unknown as Form).submitBlocked()).toBe(true);
    expect(harness.routeNativeElement?.textContent).toContain('carteira de origem não está disponível');
    http.expectNone('/api/operacoes');
  });
  it('rebuilds detail return from URL and never changes the operation portfolio to a conflicting origin', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/operacoes/9?carteiraId=2&origem=carteira');
    http.expectOne('/api/operacoes/9').flush('{"id":9,"carteiraId":1,"ticker":"AAPL","mercado":"EUA","tipo":"COMPRA","quantidade":1,"precoUnitario":10,"valorTotal":10,"dataOperacao":"2026-08-31","ordemNoDia":1}');
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Carteira#1');
    expect(harness.routeNativeElement?.querySelector('a.app-back-action')?.getAttribute('href')).toBe('/operacoes');
  });
});
