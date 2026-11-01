import { RETURN_STORAGE } from './return-navigation.service';
import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { SessionService } from './session.service';
import { SessionBoundary } from './session-boundary.service';
import { firstValueFrom } from 'rxjs';

@Component({ template: '<input value="rascunho não salvo">' }) class UnsavedDialog {}
@Component({ template: 'Entrar' }) class LoginDestination {}
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideApiConfig } from '../config/api.config';
import { CARTEIRA_STORAGE, CarteiraContextService } from '../carteira/carteira-context.service';

describe('Isolamento do contexto financeiro', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [{ provide: RETURN_STORAGE, useValue: null }] }));
  it.each([401, 503])('revalidação %s elimina dados que não podem permanecer liberados', async status => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), provideRouter([{ path: 'login', component: LoginDestination }]), { provide: CARTEIRA_STORAGE, useValue: null }] });
    TestBed.inject(SessionBoundary).start();
    const session = TestBed.inject(SessionService);
    const context = TestBed.inject(CarteiraContextService);
    const http = TestBed.inject(HttpTestingController);
    const restored = firstValueFrom(session.restore());
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'a', papel: 'ADMIN' }); http.expectOne('/api/auth/csrf').flush(null); await restored;
    context.initialize().subscribe();
    http.expectOne('/api/carteiras').flush([{ id: 1, nome: 'A', dataCriacao: '' }]);
    const checked = firstValueFrom(session.restore());
    http.expectOne('/api/auth/me').flush({}, { status, statusText: 'Falha' });
    expect(await checked).toBe(false);
    expect(context.carteiras()).toEqual([]);
    expect(session.status()).toBe(status === 401 ? 'anonymous' : 'unavailable');
    http.verify();
  });
  it('encerrar sessão limpa carteira e diálogos antes de outra identidade', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), provideRouter([{ path: 'login', component: LoginDestination }]), { provide: CARTEIRA_STORAGE, useValue: null }] });
    TestBed.inject(SessionBoundary).start();
    const context = TestBed.inject(CarteiraContextService);
    const http = TestBed.inject(HttpTestingController);
    context.initialize().subscribe();
    http.expectOne('/api/carteiras').flush([{ id: 1, nome: 'A', dataCriacao: '' }]);
    context.select(1);
    const dialogs = TestBed.inject(MatDialog);
    dialogs.open(UnsavedDialog);
    TestBed.inject(SessionService).end();
    expect(context.carteiras()).toEqual([]);
    expect(context.activeId()).toBeNull();
    await vi.waitFor(() => expect(dialogs.openDialogs).toHaveLength(0));
    http.verify();
  });
  it('remove preferência global legada e conserva seleção somente em memória da sessão', () => {
    const storage = { getItem: vi.fn(() => '2'), setItem: vi.fn(), removeItem: vi.fn() };
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), { provide: CARTEIRA_STORAGE, useValue: storage }] });
    const context = TestBed.inject(CarteiraContextService);
    const http = TestBed.inject(HttpTestingController);
    context.initialize().subscribe();
    http.expectOne('/api/carteiras').flush([{ id: 1, nome: 'A', dataCriacao: '' }, { id: 2, nome: 'B', dataCriacao: '' }]);
    expect(context.activeId()).toBe(1);
    context.select(2); context.resolveUrl(null);
    expect(context.activeId()).toBe(2);
    expect(storage.getItem).not.toHaveBeenCalled();
    expect(storage.setItem).not.toHaveBeenCalled();
    expect(storage.removeItem).toHaveBeenCalledWith('gestaoacoes.carteira-ativa.v1');
    context.reset();
    expect(context.activeId()).toBeNull();
    http.verify();
  });
  it('encerramento cancela a coleção anterior, limpa mutações e aceita somente dados novos', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig(), { provide: CARTEIRA_STORAGE, useValue: null }] });
    const context = TestBed.inject(CarteiraContextService);
    const http = TestBed.inject(HttpTestingController);
    context.initialize().subscribe();
    const old = http.expectOne('/api/carteiras');
    context.upsert({ id: 1, nome: 'Anterior', dataCriacao: '' });
    context.reset();
    expect(old.cancelled).toBe(true);
    expect(context.carteiras()).toEqual([]);
    expect(context.activeId()).toBeNull();
    context.initialize().subscribe();
    http.expectOne('/api/carteiras').flush([{ id: 2, nome: 'Nova', dataCriacao: '' }]);
    expect(context.carteiras().map(item => item.id)).toEqual([2]);
    expect(context.activeId()).toBe(2);
    http.verify();
  });
});
