import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RETURN_STORAGE, ReturnNavigationService } from './return-navigation.service';

describe('Retorno seguro de navegação', () => {
  it.each(['/operacoes/12?carteiraId=abc', '/operacoes/12?carteiraId=2&carteiraId=3', '/carteiras/nova/editar', '/acoes/12/editar', '/carteiras/9007199254740992', '/operacoes/12?origem=externa'])('destino inválido %s elimina intenção anterior em vez de retomar contexto antigo', url => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: RETURN_STORAGE, useValue: null }] });
    const service = TestBed.inject(ReturnNavigationService);
    service.capture('/carteiras/12', 7); service.capture(url, 7);
    expect(service.consume(7, 'ADMIN')).toBe('/dashboard');
  });
  it.each(['https://outra-origem.invalid/carteiras/12', '//outra-origem.invalid', '/login', '/definir-senha', '/redefinir-senha', '/carteiras/12;codigo=secreto', '/dashboard(aux:usuarios)', '/desconhecida'])('descarta destino não permitido %s', url => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: RETURN_STORAGE, useValue: null }] });
    const service = TestBed.inject(ReturnNavigationService);
    service.capture(url, 7);
    expect(service.consume(7, 'ADMIN')).toBe('/dashboard');
  });
  it('reload anônimo não apaga vínculo conhecido da intenção anterior', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: RETURN_STORAGE, useValue: null }] });
    const service = TestBed.inject(ReturnNavigationService);
    service.capture('/carteiras/12', 7);
    service.capture('/carteiras/12', null);
    expect(service.consume(9, 'USER')).toBe('/dashboard');
  });
  it.each(['/usuarios', '/usuarios/novo', '/carteiras/nova', '/carteiras/12/editar', '/operacoes/nova?carteiraId=2'])('revalida papel antes do retorno a %s', url => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: RETURN_STORAGE, useValue: null }] });
    const service = TestBed.inject(ReturnNavigationService);
    service.capture(url, 7);
    expect(service.consume(7, 'USER')).toBe('/dashboard');
    expect(service.notice()).toContain('permissão');
  });
  it('descarta intenção na igualdade de sessenta minutos', () => {
    vi.useFakeTimers();
    try {
      TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: RETURN_STORAGE, useValue: null }] });
      const service = TestBed.inject(ReturnNavigationService);
      service.capture('/carteiras/12', 7);
      vi.advanceTimersByTime(60 * 60 * 1000);
      expect(service.consume(7, 'ADMIN')).toBe('/dashboard');
    } finally { vi.useRealTimers(); }
  });
  it('retomada conhecida não é transferida para outra identidade', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: RETURN_STORAGE, useValue: null }] });
    const service = TestBed.inject(ReturnNavigationService);
    service.capture('/operacoes?carteiraId=2', 7);
    expect(service.consume(9, 'ADMIN')).toBe('/dashboard');
  });
  it('primeiro acesso anônimo conserva somente carteiraId/origem e consome uma vez', () => {
    const data = new Map<string, string>();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: RETURN_STORAGE, useValue: { getItem: (key: string) => data.get(key) ?? null, setItem: (key: string, value: string) => data.set(key, value), removeItem: (key: string) => data.delete(key) } }] });
    const service = TestBed.inject(ReturnNavigationService);
    service.capture('/operacoes/12?carteiraId=2&origem=carteira&senha=segredo&natureza=TRANSFERENCIA#main-content', null);
    expect([...data.values()].join()).not.toContain('segredo');
    expect(service.consume(7, 'USER')).toBe('/operacoes/12?carteiraId=2&origem=carteira#main-content');
    expect(service.consume(7, 'USER')).toBe('/dashboard');
  });
});
