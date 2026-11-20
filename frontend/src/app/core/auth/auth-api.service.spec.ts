import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { provideApiConfig } from '../config/api.config';
import { AuthApi } from './auth-api.service';

describe('AuthApi — transporte público', () => {
  it('restaura por leitura sem registrar atividade nem renovar credencial', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const result = firstValueFrom(TestBed.inject(AuthApi).me());
    http.expectOne('/api/auth/me').flush({ id: 8, papel: 'ADMIN' });
    expect(await result).toMatchObject({ id: 8 });
    http.verify();
  });
  it('obtém CSRF antes do login e novamente após a rotação, sem expor JWT', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const result = firstValueFrom(TestBed.inject(AuthApi).login('usuario', 'senha de teste'));
    http.expectOne('/api/auth/csrf').flush(null);
    const login = http.expectOne('/api/auth/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toEqual({ username: 'usuario', senha: 'senha de teste' });
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({ id: 7, username: 'usuario', papel: 'USER', horarioServidor: '2026-10-31T12:00:00Z', ultimaAtividadeEm: '2026-10-31T12:00:00Z', inativaEm: '2026-10-31T12:10:00Z', expiraEm: '2026-10-31T16:00:00Z' });
    http.expectOne('/api/auth/csrf').flush(null);
    expect(await result).toMatchObject({ id: 7, papel: 'USER' });
    http.verify();
  });
});
