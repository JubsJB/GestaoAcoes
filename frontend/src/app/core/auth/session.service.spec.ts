import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { provideApiConfig } from '../config/api.config';
import { SessionService } from './session.service';

export const SESSION_FIXTURE = { id: 7, username: 'usuario', papel: 'USER', horarioServidor: '2026-10-31T12:00:00Z', ultimaAtividadeEm: '2026-10-31T12:00:00Z', inativaEm: '2026-10-31T12:10:00Z', expiraEm: '2026-10-31T16:00:00Z' };

describe('SessionService — restauração', () => {
  it('não libera sessão restaurada antes de materializar CSRF atual', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const session = TestBed.inject(SessionService);
    const result = firstValueFrom(session.restore());
    http.expectOne('/api/auth/me').flush(SESSION_FIXTURE);
    expect(session.authenticated()).toBe(false);
    http.expectOne('/api/auth/csrf').flush(null);
    expect(await result).toBe(true);
    http.verify();
  });
  it('logout confirmado revoga no servidor, limpa estado e renova apenas CSRF', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const session = TestBed.inject(SessionService);
    const restored = firstValueFrom(session.restore());
    http.expectOne('/api/auth/me').flush(SESSION_FIXTURE); http.expectOne('/api/auth/csrf').flush(null);
    await restored;
    const result = firstValueFrom(session.logout());
    http.expectOne('/api/auth/logout').flush(null);
    http.expectOne('/api/auth/csrf').flush(null);
    await result;
    expect(session.status()).toBe('anonymous');
    expect(session.identity()).toBeNull();
    http.verify();
  });
  it('login só libera identidade após CSRF rotacionado e /me autoritativo', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const session = TestBed.inject(SessionService);
    const result = firstValueFrom(session.login('usuario', 'senha de teste'));
    http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/auth/login').flush(SESSION_FIXTURE);
    expect(session.authenticated()).toBe(false);
    http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/auth/me').flush(SESSION_FIXTURE); http.expectOne('/api/auth/csrf').flush(null);
    expect(await result).toBe(true);
    expect(session.authenticated()).toBe(true);
    http.verify();
  });
  it('descarta /me atrasado depois de encerrar a geração anterior', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const session = TestBed.inject(SessionService);
    const old = firstValueFrom(session.restore());
    const request = http.expectOne('/api/auth/me');
    session.end();
    request.flush(SESSION_FIXTURE);
    expect(await old).toBe(false);
    expect(session.identity()).toBeNull();
    expect(session.status()).toBe('anonymous');
    http.verify();
  });
  it.each([401, 503])('nega shell em %s e distingue anonimato de indisponibilidade', async status => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const session = TestBed.inject(SessionService);
    const result = firstValueFrom(session.restore());
    http.expectOne('/api/auth/me').flush({}, { status, statusText: 'Falha' });
    expect(await result).toBe(false);
    expect(session.authenticated()).toBe(false);
    expect(session.status()).toBe(status === 401 ? 'anonymous' : 'unavailable');
    http.verify();
  });
  it('bloqueia o contexto até /me e compartilha uma restauração concorrente', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const session = TestBed.inject(SessionService);
    expect(session.authenticated()).toBe(false);
    const a = firstValueFrom(session.restore());
    const b = firstValueFrom(session.restore());
    expect(session.status()).toBe('checking');
    http.expectOne('/api/auth/me').flush(SESSION_FIXTURE); http.expectOne('/api/auth/csrf').flush(null);
    expect(await a).toBe(true);
    expect(await b).toBe(true);
    expect(session.identity()).toMatchObject({ id: 7, papel: 'USER' });
    expect(session.admin()).toBe(false);
    http.verify();
  });
});
