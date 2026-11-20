import { RETURN_STORAGE } from './return-navigation.service';
import { BreakpointObserver } from '@angular/cdk/layout';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { firstValueFrom, of } from 'rxjs';
import { routes } from '../../app.routes';
import { provideApiConfig } from '../config/api.config';
import { SessionService } from './session.service';

describe('Shell e contrato HTTP de sessão', () => {
  it('logout indisponível limpa acesso local e informa revogação não confirmada', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), provideApiConfig(),
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const http = TestBed.inject(HttpTestingController);
    const restored = firstValueFrom(TestBed.inject(SessionService).restore());
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'ana', papel: 'USER' }); http.expectOne('/api/auth/csrf').flush(null); await restored;
    const harness = await RouterTestingHarness.create('/dashboard');
    http.expectOne('/api/carteiras').flush([]); harness.detectChanges();
    harness.routeNativeElement!.querySelector<HTMLButtonElement>('[data-testid=logout]')!.click();
    http.expectOne('/api/auth/logout').flush({}, { status: 503, statusText: 'Unavailable' });
    await vi.waitFor(() => expect(TestBed.inject(Router).url).toBe('/login'));
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('[role=alert]')?.textContent).toContain('revogação no servidor');
    expect(TestBed.inject(SessionService).authenticated()).toBe(false);
    http.verify();
  });
  beforeEach(() => TestBed.configureTestingModule({ providers: [{ provide: RETURN_STORAGE, useValue: null }] }));
  it('encerra contexto e componentes protegidos quando a sessão termina', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), provideApiConfig(),
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const http = TestBed.inject(HttpTestingController);
    const session = TestBed.inject(SessionService);
    const restored = firstValueFrom(session.restore());
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'consulta', papel: 'USER' }); http.expectOne('/api/auth/csrf').flush(null); await restored;
    const harness = await RouterTestingHarness.create('/dashboard');
    http.expectOne('/api/carteiras').flush([]);
    session.end();
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('app-carteira-selector') ?? null).toBeNull();
    await harness.fixture.whenStable();
    await vi.waitFor(() => expect(TestBed.inject(Router).url).toBe('/login'));
    http.verify();
  });
  it.each(['/carteiras/nova', '/carteiras/1/editar', '/acoes/nova', '/corretoras/nova', '/operacoes/nova'])('USER não abre escrita em %s', async url => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), provideApiConfig(),
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const http = TestBed.inject(HttpTestingController);
    const restored = firstValueFrom(TestBed.inject(SessionService).restore());
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'consulta', papel: 'USER' }); http.expectOne('/api/auth/csrf').flush(null);
    await restored;
    await RouterTestingHarness.create(url);
    http.match('/api/carteiras').forEach(request => request.flush([]));
    expect(TestBed.inject(Router).url).toBe('/dashboard');
    http.verify();
  });
  it.each(['carteiras', 'acoes', 'corretoras', 'operacoes'])('USER consulta /%s vazio sem convite ou ação para cadastrar', async path => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), provideApiConfig(),
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const http = TestBed.inject(HttpTestingController);
    const restored = firstValueFrom(TestBed.inject(SessionService).restore());
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'consulta', papel: 'USER' }); http.expectOne('/api/auth/csrf').flush(null);
    await restored;
    const harness = await RouterTestingHarness.create('/' + path);
    http.match('/api/carteiras').forEach(request => request.flush([]));
    if (path === 'acoes' || path === 'corretoras') http.expectOne('/api/' + path).flush([]);
    harness.detectChanges();
    const element = harness.routeNativeElement!;
    expect(element.querySelector('h1')).not.toBeNull();
    expect(element.querySelector('a[href$="/nova"]')).toBeNull();
    const writeLabels = Array.from(element.querySelectorAll('button')).map(button => button.textContent).filter(label => /Cadastrar|Nova carteira|Nova operação/.test(label ?? ''));
    expect(writeLabels).toEqual([]);
    http.verify();
  });
  it('USER consulta Dashboard sem operações, cotação ou snapshot e mantém atualização por GET', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), provideApiConfig(),
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const http = TestBed.inject(HttpTestingController);
    const restored = firstValueFrom(TestBed.inject(SessionService).restore());
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'consulta', papel: 'USER' }); http.expectOne('/api/auth/csrf').flush(null);
    await restored;
    const harness = await RouterTestingHarness.create('/dashboard?carteiraId=1');
    http.expectOne('/api/carteiras').flush([{ id: 1, nome: 'Compartilhada', dataCriacao: '' }]);
    http.expectOne('/api/carteiras/1/resumo').flush('{"carteiraId":1,"resumos":[]}');
    http.expectOne('/api/carteiras/1/posicoes').flush('[]');
    http.expectOne('/api/carteiras/1/resultados-realizados').flush('[]');
    harness.detectChanges();
    http.expectOne('/api/carteiras/1/evolucao-patrimonial').flush('{"carteiraId":1,"pontos":[]}');
    harness.detectChanges();
    const text = harness.routeNativeElement!.textContent!;
    expect(text).toContain('Atualizar dados');
    expect(text).not.toContain('Registrar operação');
    expect(text).not.toContain('Atualizar cotações');
    expect(text).not.toContain('Registrar snapshot');
    http.verify();
  });
  it('mostra identidade e encerra o shell após logout confirmado', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), provideApiConfig(),
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const http = TestBed.inject(HttpTestingController);
    const restored = firstValueFrom(TestBed.inject(SessionService).restore());
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'ana', papel: 'USER' }); http.expectOne('/api/auth/csrf').flush(null);
    await restored;
    const harness = await RouterTestingHarness.create('/dashboard');
    http.expectOne('/api/carteiras').flush([]);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('ana');
    const button = harness.routeNativeElement?.querySelector<HTMLButtonElement>('[data-testid=logout]');
    expect(button).not.toBeNull();
    button!.click();
    http.expectOne('/api/auth/logout').flush(null);
    http.expectOne('/api/auth/csrf').flush(null);
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/login');
    expect(harness.routeNativeElement?.querySelector('app-carteira-selector') ?? null).toBeNull();
    http.verify();
  });
  it('acesso direto anônimo só abre login, sem inicializar coleção financeira', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), provideApiConfig(),
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const http = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create();
    const navigation = harness.navigateByUrl('/carteiras/12?origem=dashboard');
    await vi.waitFor(() => {
      const requests = http.match('/api/auth/me');
      expect(requests).toHaveLength(1);
      expect(harness.routeNativeElement?.querySelector('app-carteira-selector') ?? null).toBeNull();
      http.expectNone('/api/carteiras');
      requests[0].flush({}, { status: 401, statusText: 'Unauthorized' });
    });
    await navigation;
    expect(TestBed.inject(Router).url).toBe('/login');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Entrar');
    http.expectNone('/api/carteiras/12');
    http.verify();
  });
});
