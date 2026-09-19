import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { routes } from './app.routes';
import { provideApiConfig } from './core/config/api.config';
import { CARTEIRA_STORAGE } from './core/carteira/carteira-context.service';

const desktopBreakpointObserver = {
  observe: () => of<BreakpointState>({ matches: false, breakpoints: {} })
};

describe('application routes', () => {
  let httpTesting: HttpTestingController;
  let readPreference: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    readPreference = vi.fn().mockReturnValue(null);
    TestBed.configureTestingModule({
      providers: [
        { provide: CARTEIRA_STORAGE, useValue: { getItem: readPreference, setItem: vi.fn(), removeItem: vi.fn() } },
        provideRouter(routes),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideApiConfig(),
        { provide: BreakpointObserver, useValue: desktopBreakpointObserver }
      ]
    });

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('redirects the root route exactly to dashboard inside the shell', async () => {
    const harness = await RouterTestingHarness.create();
    const navigation = harness.navigateByUrl('/');
    let requests = httpTesting.match('/api/carteiras');
    await vi.waitFor(() => {
      if (requests.length === 0) requests = httpTesting.match('/api/carteiras');
      expect(requests).toHaveLength(1);
    }, { timeout: 10_000 });
    requests[0].flush([]);
    await navigation;
    const router = TestBed.inject(Router);

    expect(router.url).toBe('/dashboard');
    expect(harness.routeNativeElement?.textContent).toContain('Gestão de Ações');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Dashboard');
  });

  it('declares an independent lazy boundary for every main destination', () => {
    const shellRoute = routes[0];
    const lazyPaths = shellRoute.children
      ?.filter((route) => route.loadChildren)
      .map((route) => route.path);

    expect(lazyPaths).toEqual(['dashboard', 'corretoras', 'acoes', 'carteiras', 'operacoes']);
  });

  it('shares shell/dashboard loading and honors the explicit URL before requesting finances', async () => {
    readPreference.mockReturnValue('1');
    const harness = await RouterTestingHarness.create();
    const navigation = harness.navigateByUrl('/dashboard?carteiraId=2');
    await vi.waitFor(() => expect(httpTesting.match(request => request.url === '/api/carteiras').map(request => {
      request.flush([{ id: 1, nome: 'A', dataCriacao: '' }, { id: 2, nome: 'B', dataCriacao: '' }]);
      return request;
    })).toHaveLength(1));
    await navigation;
    httpTesting.expectNone(request => request.url.startsWith('/api/carteiras/1/'));
    httpTesting.expectOne('/api/carteiras/2/resumo').flush('{"carteiraId":2,"resumos":[]}');
    httpTesting.expectOne('/api/carteiras/2/posicoes').flush('[]');
    httpTesting.expectOne('/api/carteiras/2/resultados-realizados').flush('[]');
    harness.detectChanges();
    httpTesting.expectOne('/api/carteiras/2/evolucao-patrimonial').flush('{"carteiraId":2,"pontos":[]}');
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('select')?.value).toBe('2');
    expect(harness.routeNativeElement?.querySelector('app-dashboard-page mat-select')).toBeNull();
    expect(harness.routeNativeElement?.querySelector('nav a[href="/operacoes"]')?.textContent).toContain('Operações');
  });

  it('resolves all functional features including dashboard', async () => {
    const harness = await RouterTestingHarness.create();
    const firstNavigation = harness.navigateByUrl('/dashboard');
    let requests = httpTesting.match('/api/carteiras');
    await vi.waitFor(() => {
      if (requests.length === 0) requests = httpTesting.match('/api/carteiras');
      expect(requests).toHaveLength(1);
    }, { timeout: 10_000 });
    requests[0].flush([]);
    await firstNavigation;
    const destinations = [
      ['/dashboard', 'Dashboard'],
      ['/acoes', 'Ações'],
      ['/carteiras', 'Carteiras'],
      ['/operacoes', 'Operações']
    ] as const;

    for (const [url, heading] of destinations) {
      await harness.navigateByUrl(url);
      if (url === '/dashboard') {
        httpTesting.match('/api/carteiras').forEach(request => request.flush([]));
      }
      if (url === '/acoes') {
        httpTesting.expectOne('/api/acoes').flush([]);
      }
      if (url === '/carteiras') {
        httpTesting.expectOne('/api/carteiras').flush([]);
      }
      if (url === '/operacoes') {
        httpTesting.expectNone(request => request.url.includes('/operacoes'));
      }
      expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe(heading);
    }

    await harness.navigateByUrl('/corretoras');
    httpTesting.expectOne('/api/corretoras').flush([]);
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Corretoras');
  }, 10_000);

  it('renders NotFound inside the shell and preserves an unknown URL', async () => {
    const harness = await RouterTestingHarness.create('/rota-inexistente');
    httpTesting.expectOne('/api/carteiras').flush([]);
    const router = TestBed.inject(Router);

    expect(router.url).toBe('/rota-inexistente');
    expect(harness.routeNativeElement?.textContent).toContain('Gestão de Ações');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Página não encontrada');
    expect(harness.routeNativeElement?.querySelector('section a[href="/dashboard"]')?.textContent).toContain(
      'Voltar para o Dashboard'
    );
  });

  it('opens contextual operation history from the menu and follows the global portfolio selector', async () => {
    const harness = await RouterTestingHarness.create('/rota-inexistente');
    httpTesting.expectOne('/api/carteiras').flush([
      { id: 1, nome: 'Carteira A', dataCriacao: '' },
      { id: 2, nome: 'Carteira B', dataCriacao: '' }
    ]);
    harness.detectChanges();
    const link = harness.routeNativeElement?.querySelector('nav a[href="/operacoes"]') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/operacoes');
    link.click();
    await harness.fixture.whenStable();
    const request = httpTesting.expectOne('/api/carteiras/1/operacoes');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush(JSON.stringify([{ id: 1, carteiraId: 1, ticker: 'PETR4', mercado: 'BRASIL', corretoraId: null, tipo: 'COMPRA', quantidade: 10, precoUnitario: 30, valorTotal: 300, dataOperacao: '2026-01-01', ordemNoDia: 1 }]));
    harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/operacoes?carteiraId=1');
    const select = harness.routeNativeElement?.querySelector('#global-carteira') as HTMLSelectElement;
    expect(select.value).toBe('1');
    select.value = '2';
    select.dispatchEvent(new Event('change'));
    await harness.fixture.whenStable();
    httpTesting.expectOne('/api/carteiras/2/operacoes').flush(JSON.stringify([{ id: 2, carteiraId: 2, ticker: 'AAPL', mercado: 'EUA', corretoraId: null, tipo: 'COMPRA', quantidade: 1, precoUnitario: 200, valorTotal: 200, dataOperacao: '2026-01-02', ordemNoDia: 1 }]));
    harness.detectChanges();
    expect(select.value).toBe('2');
    expect(TestBed.inject(Router).url).toBe('/operacoes?carteiraId=2');
    const rows = harness.routeNativeElement?.querySelectorAll('tbody tr');
    expect(rows).toHaveLength(1);
    expect(rows?.[0].textContent).toContain('AAPL');
    expect(rows?.[0].textContent).not.toContain('PETR4');
    expect(harness.routeNativeElement?.textContent).toContain('Carteira B');
    expect(harness.routeNativeElement?.querySelector('app-operacoes-list-page select')).toBeNull();
    httpTesting.expectNone('/api/operacoes');
    httpTesting.expectNone(() => true);
    expect(link.getAttribute('aria-current')).toBe('page');

    (harness.routeNativeElement?.querySelector('nav a[href="/acoes"]') as HTMLAnchorElement).click();
    await harness.fixture.whenStable();
    httpTesting.expectOne('/api/acoes').flush([]);
    harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/acoes');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Ações');
    expect(harness.routeNativeElement?.querySelector('nav a[aria-current="page"]')?.getAttribute('aria-label')).toBe('Ativos');
  });
});
