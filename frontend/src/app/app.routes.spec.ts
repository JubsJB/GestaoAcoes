import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { routes } from './app.routes';
import { provideApiConfig } from './core/config/api.config';

const desktopBreakpointObserver = {
  observe: () => of<BreakpointState>({ matches: false, breakpoints: {} })
};

describe('application routes', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
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
    const harness = await RouterTestingHarness.create('/');
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

  it('resolves the functional broker and stock features and the remaining structural placeholders', async () => {
    const harness = await RouterTestingHarness.create('/dashboard');
    const destinations = [
      ['/dashboard', 'Dashboard'],
      ['/acoes', 'Ações'],
      ['/carteiras', 'Carteiras'],
      ['/operacoes', 'Operações']
    ] as const;

    for (const [url, heading] of destinations) {
      await harness.navigateByUrl(url);
      if (url === '/acoes') {
        httpTesting.expectOne('/api/acoes').flush([]);
      }
      expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe(heading);
    }

    await harness.navigateByUrl('/corretoras');
    httpTesting.expectOne('/api/corretoras').flush([]);
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Corretoras');
  });

  it('renders NotFound inside the shell and preserves an unknown URL', async () => {
    const harness = await RouterTestingHarness.create('/rota-inexistente');
    const router = TestBed.inject(Router);

    expect(router.url).toBe('/rota-inexistente');
    expect(harness.routeNativeElement?.textContent).toContain('Gestão de Ações');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Página não encontrada');
    expect(harness.routeNativeElement?.querySelector('section a[href="/dashboard"]')?.textContent).toContain(
      'Voltar para o Dashboard'
    );
  });
});
