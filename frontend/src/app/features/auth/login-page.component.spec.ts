import { RETURN_STORAGE } from '../../core/auth/return-navigation.service';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { provideApiConfig } from '../../core/config/api.config';
import { LoginPageComponent } from './login-page.component';
import { ReturnNavigationService } from '../../core/auth/return-navigation.service';
import { SessionService } from '../../core/auth/session.service';
import { withInterceptors } from '@angular/common/http';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';
import { By } from '@angular/platform-browser';
import { AUTH_BROWSER } from '../../core/auth/auth-browser';
import { BrowserFixture } from '../../../../test-support/auth-browser';
import { sessionFixture } from '../../../../test-support/session-fixture';

@Component({ template: 'Destino autorizado' }) class AuthorizedDestination {}

describe('Login público', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [{ provide: RETURN_STORAGE, useValue: null }] }));
  it('continuar por interação confiável comunica atividade após validar a mesma sessão', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), provideRouter([{ path: 'dashboard', component: AuthorizedDestination }]), { provide: AUTH_BROWSER, useValue: new BrowserFixture() }] });
    const http = TestBed.inject(HttpTestingController);
    const identity = sessionFixture({ id: 7, username: 'usuario', papel: 'USER' });
    TestBed.inject(SessionService).restore().subscribe(); http.expectOne('/api/auth/me').flush(identity); http.expectOne('/api/auth/csrf').flush(null);
    const fixture = TestBed.createComponent(LoginPageComponent); fixture.detectChanges();
    fixture.debugElement.query(By.css('[data-testid=continue-session]')).triggerEventHandler('click', { isTrusted: true });
    http.expectOne('/api/auth/me').flush(identity); http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/auth/atividade').flush(identity);
    await fixture.whenStable(); expect(TestBed.inject(Router).url).toBe('/dashboard');
    http.expectNone('/api/auth/login'); http.verify();
  });
  it('429 respeita Retry-After preservado pelo normalizador sem repetir login', () => {
    vi.useFakeTimers();
    try {
      TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting(), provideApiConfig(), provideRouter([])] });
      const fixture = TestBed.createComponent(LoginPageComponent); fixture.detectChanges();
      const element: HTMLElement = fixture.nativeElement;
      for (const [name, value] of Object.entries({ username: 'usuario', senha: 'senha de fixture' })) {
        const input = element.querySelector<HTMLInputElement>(`[name=${name}]`)!; input.value = value; input.dispatchEvent(new Event('input'));
      }
      element.querySelector('form')!.dispatchEvent(new Event('submit'));
      const http = TestBed.inject(HttpTestingController); http.expectOne('/api/auth/csrf').flush(null);
      http.expectOne('/api/auth/login').flush({}, { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '2' } }); fixture.detectChanges();
      const button = element.querySelector<HTMLButtonElement>('button[type=submit]')!;
      expect(button.disabled).toBe(true);
      vi.advanceTimersByTime(1999); fixture.detectChanges(); expect(button.disabled).toBe(true);
      vi.advanceTimersByTime(1); fixture.detectChanges(); expect(button.disabled).toBe(false);
      http.expectNone('/api/auth/login'); http.verify();
    } finally { TestBed.resetTestingModule(); vi.useRealTimers(); }
  });
  it('permite continuar sessão validada em outra aba após nova leitura, sem novo login', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), provideRouter([{ path: 'dashboard', component: AuthorizedDestination }])] });
    const http = TestBed.inject(HttpTestingController);
    TestBed.inject(SessionService).restore().subscribe();
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'usuario', papel: 'USER' }); http.expectOne('/api/auth/csrf').flush(null);
    const fixture = TestBed.createComponent(LoginPageComponent); fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('[data-testid=continue-session]') as HTMLButtonElement | null;
    expect(button).not.toBeNull(); button!.click();
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'usuario', papel: 'USER' }); http.expectOne('/api/auth/csrf').flush(null);
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/dashboard');
    http.expectNone('/api/auth/login'); http.verify();
  });
  it('limpa campos quando outra geração invalida o contexto durante o login', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), provideRouter([])] });
    const fixture = TestBed.createComponent(LoginPageComponent); fixture.detectChanges();
    const fields = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    fields[0].value = 'anterior'; fields[0].dispatchEvent(new Event('input'));
    fields[1].value = 'rascunho secreto'; fields[1].dispatchEvent(new Event('input'));
    TestBed.inject(SessionService).unavailable(); fixture.detectChanges();
    expect(fields[0].value).toBe(''); expect(fields[1].value).toBe('');
    TestBed.inject(HttpTestingController).verify();
  });
  it.each([true, false])('valida carteira no retorno e trata exclusão; disponível=%s', async available => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), provideRouter([
      { path: 'operacoes', component: AuthorizedDestination }, { path: 'dashboard', component: AuthorizedDestination }
    ])] });
    TestBed.inject(ReturnNavigationService).capture('/operacoes?carteiraId=2', null);
    const fixture = TestBed.createComponent(LoginPageComponent); fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const username = element.querySelector<HTMLInputElement>('[name=username]')!;
    const password = element.querySelector<HTMLInputElement>('[name=senha]')!;
    username.value = 'usuario'; username.dispatchEvent(new Event('input'));
    password.value = 'senha de teste'; password.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/auth/login').flush({ id: 7 });
    http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'usuario', papel: 'USER' }); http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/carteiras').flush(available ? [{ id: 2, nome: 'Compartilhada', dataCriacao: '' }] : []);
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe(available ? '/operacoes?carteiraId=2' : '/dashboard');
    if (!available) expect(TestBed.inject(ReturnNavigationService).notice()).toContain('não está disponível');
    http.verify();
  });
  it('envia credenciais com handshake e apresenta recusa genérica sem preservar senha', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), provideRouter([])] });
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const username = element.querySelector<HTMLInputElement>('[name=username]')!;
    const password = element.querySelector<HTMLInputElement>('[name=senha]')!;
    username.value = 'usuario'; username.dispatchEvent(new Event('input'));
    password.value = 'senha de teste'; password.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    expect(element.querySelector('[role=alert]')?.textContent).toContain('Usuário ou senha inválidos');
    expect(password.value).toBe('');
    expect(element.querySelector('app-main-layout')).toBeNull();
    http.verify();
  });
});
