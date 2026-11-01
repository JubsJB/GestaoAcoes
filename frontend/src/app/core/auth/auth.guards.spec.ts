import { RETURN_STORAGE, ReturnNavigationService } from './return-navigation.service';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideApiConfig } from '../config/api.config';
import { adminGuard, sessionGuard } from './auth.guards';

let opened = 0;
@Component({ selector: 'test-financial-page', template: 'Finanças protegidas' }) class FinancialPage { constructor() { opened++; } }
@Component({ selector: 'test-login-page', template: 'Entrar' }) class LoginPage {}

describe('Rotas protegidas', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [{ provide: RETURN_STORAGE, useValue: null }] }));
  it.each([7, 9])('reload respeita vínculo anterior da identidade 7; identidade atual=%s', async id => {
    opened = 0;
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), provideRouter([
      { path: 'dashboard', component: FinancialPage, canActivate: [sessionGuard] }, { path: 'login', component: LoginPage }
    ])] });
    TestBed.inject(ReturnNavigationService).capture('/dashboard?carteiraId=2', 7);
    const http = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create();
    const navigation = harness.navigateByUrl('/dashboard?carteiraId=2');
    await vi.waitFor(() => expect(http.match('/api/auth/me').map(request => {
      expect(opened).toBe(0); request.flush({ id, username: 'fixture', papel: 'USER' });
      http.expectOne('/api/auth/csrf').flush(null); return request;
    })).toHaveLength(1));
    await navigation;
    expect(TestBed.inject(Router).url).toBe(id === 7 ? '/dashboard?carteiraId=2' : '/dashboard');
    expect(opened).toBe(1);
    http.verify();
  });
  it('USER não instancia formulário administrativo por acesso direto', async () => {
    opened = 0;
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), provideRouter([
      { path: 'operacoes/nova', component: FinancialPage, canActivate: [sessionGuard, adminGuard] },
      { path: 'dashboard', component: LoginPage }
    ])] });
    const http = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create();
    const navigation = harness.navigateByUrl('/operacoes/nova?carteiraId=2');
    await vi.waitFor(() => expect(http.match('/api/auth/me').map(request => {
      request.flush({ id: 7, username: 'user', papel: 'USER' }); http.expectOne('/api/auth/csrf').flush(null); return request;
    })).toHaveLength(1));
    await navigation;
    expect(opened).toBe(0);
    expect(TestBed.inject(Router).url).toBe('/dashboard');
    http.verify();
  });
  it('não instancia finanças antes de /me e nega acesso direto anônimo', async () => {
    opened = 0;
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), provideRouter([
      { path: 'dashboard', component: FinancialPage, canActivate: [sessionGuard] },
      { path: 'login', component: LoginPage }
    ])] });
    const http = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create();
    const navigation = harness.navigateByUrl('/dashboard?carteiraId=2');
    await vi.waitFor(() => expect(http.match('/api/auth/me').map(request => {
      expect(opened).toBe(0);
      request.flush({}, { status: 401, statusText: 'Unauthorized' });
      return request;
    })).toHaveLength(1));
    await navigation;
    expect(opened).toBe(0);
    expect(TestBed.inject(Router).url).toBe('/login');
    http.verify();
  });
});
