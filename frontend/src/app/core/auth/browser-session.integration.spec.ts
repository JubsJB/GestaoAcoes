import { BreakpointObserver } from '@angular/cdk/layout';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { BrowserFixture } from '../../../../test-support/auth-browser';
import { sessionFixture } from '../../../../test-support/session-fixture';
import { AppComponent } from '../../app.component';
import { routes } from '../../app.routes';
import { provideApiConfig } from '../config/api.config';
import { AUTH_BROWSER } from './auth-browser';
import { RETURN_STORAGE } from './return-navigation.service';
import { sessionInterceptor } from './session.interceptor';
import { CarteiraContextService } from '../carteira/carteira-context.service';
import { SessionService } from './session.service';

describe('Sessão no app Angular', () => {
  it('fluxo público de senha não depende de revalidar o JWT anterior no foco', async () => {
    const browser = new BrowserFixture();
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(withInterceptors([sessionInterceptor])), provideHttpClientTesting(), provideApiConfig(),
      { provide: RETURN_STORAGE, useValue: null }, { provide: AUTH_BROWSER, useValue: browser },
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const http = TestBed.inject(HttpTestingController);
    TestBed.inject(SessionService).restore().subscribe(); http.expectOne('/api/auth/me').flush(sessionFixture()); http.expectOne('/api/auth/csrf').flush(null);
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/redefinir-senha'); fixture.detectChanges();
    for (const [name, value] of Object.entries({ username: 'usuario', codigo: 'ABCDEFGHIJKLMNOPQRSTUVWXYA', senha: 'senha de fixture', confirmacao: 'senha de fixture' })) {
      const input = fixture.nativeElement.querySelector(`[name=${name}]`) as HTMLInputElement; input.value = value; input.dispatchEvent(new Event('input'));
    }
    browser.emit('focus');
    http.expectNone('/api/auth/me');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    http.expectOne('/api/auth/csrf').flush(null); http.expectOne('/api/auth/redefinicoes/confirmar').flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Senha definida com sucesso');
    http.expectNone('/api/auth/login'); http.expectNone('/api/auth/atividade'); http.verify();
  });
  it('mesma conta agora USER não recria formulário administrativo da sessão anterior', async () => {
    const browser = new BrowserFixture();
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(withInterceptors([sessionInterceptor])), provideHttpClientTesting(), provideApiConfig(),
      { provide: RETURN_STORAGE, useValue: null }, { provide: AUTH_BROWSER, useValue: browser },
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController); const router = TestBed.inject(Router);
    const navigation = router.navigateByUrl('/carteiras/nova');
    await vi.waitFor(() => expect(http.match('/api/auth/me').map(request => {
      request.flush(sessionFixture()); http.expectOne('/api/auth/csrf').flush(null); return request;
    })).toHaveLength(1));
    await navigation; fixture.detectChanges(); http.expectOne('/api/carteiras').flush([]); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-carteira-form-page')).not.toBeNull();
    browser.receive?.({ type: 'SESSION_CHANGED' }); fixture.detectChanges(); await Promise.resolve();
    http.expectOne('/api/auth/me').flush(sessionFixture({ papel: 'USER' })); http.expectOne('/api/auth/csrf').flush(null);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-carteira-form-page')).toBeNull();
    await vi.waitFor(() => expect(router.url).toBe('/dashboard'));
    fixture.detectChanges(); http.expectOne('/api/carteiras').flush([]); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toBe('Dashboard');
    http.verify();
  });
  it('troca externa A → B não instancia o contexto anterior antes do dashboard limpo', async () => {
    const browser = new BrowserFixture();
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(withInterceptors([sessionInterceptor])), provideHttpClientTesting(), provideApiConfig(),
      { provide: RETURN_STORAGE, useValue: null }, { provide: AUTH_BROWSER, useValue: browser },
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController); const router = TestBed.inject(Router);
    const navigation = router.navigateByUrl('/dashboard?carteiraId=2');
    await vi.waitFor(() => expect(http.match('/api/auth/me').map(request => {
      request.flush(sessionFixture()); http.expectOne('/api/auth/csrf').flush(null); return request;
    })).toHaveLength(1));
    await navigation; fixture.detectChanges();
    const wallets = [{ id: 1, nome: 'A', dataCriacao: '' }, { id: 2, nome: 'B', dataCriacao: '' }];
    http.expectOne('/api/carteiras').flush(wallets);
    http.expectOne('/api/carteiras/2/resumo').flush('{"carteiraId":2,"resumos":[]}');
    http.expectOne('/api/carteiras/2/posicoes').flush('[]'); http.expectOne('/api/carteiras/2/resultados-realizados').flush('[]');
    fixture.detectChanges(); http.expectOne('/api/carteiras/2/evolucao-patrimonial').flush('{"carteiraId":2,"pontos":[]}');
    browser.receive?.({ type: 'SESSION_CHANGED' }); fixture.detectChanges();
    expect(TestBed.inject(CarteiraContextService).activeId()).toBeNull();
    await Promise.resolve();
    http.expectOne('/api/auth/me').flush(sessionFixture({ id: 920, username: 'outra', papel: 'USER' }));
    http.expectOne('/api/auth/csrf').flush(null); fixture.detectChanges();
    http.expectNone('/api/carteiras');
    await vi.waitFor(() => expect(router.url).toBe('/dashboard'));
    fixture.detectChanges(); http.expectOne('/api/carteiras').flush(wallets);
    http.expectOne('/api/carteiras/1/resumo').flush('{"carteiraId":1,"resumos":[]}');
    http.expectOne('/api/carteiras/1/posicoes').flush('[]'); http.expectOne('/api/carteiras/1/resultados-realizados').flush('[]');
    fixture.detectChanges(); http.expectOne('/api/carteiras/1/evolucao-patrimonial').flush('{"carteiraId":1,"pontos":[]}');
    expect(TestBed.inject(CarteiraContextService).activeId()).toBe(1);
    http.expectNone(request => request.url.startsWith('/api/carteiras/2/')); http.verify();
  });
  it('oculta conteúdo na aba suspensa e preserva instância somente após revalidação da mesma identidade', async () => {
    const browser = new BrowserFixture();
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(withInterceptors([sessionInterceptor])), provideHttpClientTesting(), provideApiConfig(),
      { provide: RETURN_STORAGE, useValue: null }, { provide: AUTH_BROWSER, useValue: browser },
      { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } }
    ] });
    const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const navigation = TestBed.inject(Router).navigateByUrl('/dashboard');
    await vi.waitFor(() => expect(http.match('/api/auth/me').map(request => {
      request.flush(sessionFixture()); http.expectOne('/api/auth/csrf').flush(null); return request;
    })).toHaveLength(1));
    await navigation; fixture.detectChanges(); http.expectOne('/api/carteiras').flush([]); fixture.detectChanges();
    const page = fixture.nativeElement.querySelector('app-dashboard-page');
    browser.shown = false; browser.emit('visibilitychange'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.session-content')?.hidden).toBe(true);
    browser.shown = true; browser.emit('visibilitychange');
    http.expectOne('/api/auth/me').flush(sessionFixture()); http.expectOne('/api/auth/csrf').flush(null);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.session-content')?.hidden).toBe(false);
    expect(fixture.nativeElement.querySelector('app-dashboard-page')).toBe(page);
    http.expectNone('/api/auth/atividade'); http.verify();
  });
});
