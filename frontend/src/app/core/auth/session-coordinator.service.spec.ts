import { BrowserFixture } from '../../../../test-support/auth-browser';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { authenticatedModule } from '../../../../test-support/authenticated-module';
import { AUTH_BROWSER } from './auth-browser';
import { SessionCoordinator } from './session-coordinator.service';
import { SessionService } from './session.service';
import { sessionFixture } from '../../../../test-support/session-fixture';
import { sessionInterceptor } from './session.interceptor';

describe('Atividade real e coordenação de sessão', () => {
  let browser: BrowserFixture;
  let http: HttpTestingController;
  beforeEach(() => {
    vi.useFakeTimers();
    browser = new BrowserFixture();
    authenticatedModule({ providers: [provideHttpClient(withInterceptors([sessionInterceptor])), provideRouter([]), { provide: AUTH_BROWSER, useValue: browser }] });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(SessionCoordinator).start();
    TestBed.tick();
  });
  afterEach(() => { try { http.verify(); } finally { TestBed.resetTestingModule(); vi.useRealTimers(); } });

  it('detecta revogação no próximo polling mesmo sem mensagem entre abas', () => {
    browser.receive = undefined;
    vi.advanceTimersByTime(30_000);
    http.expectOne('/api/auth/me').flush({}, { status: 401, statusText: 'Revoked' });
    expect(TestBed.inject(SessionService).authenticated()).toBe(false);
    expect(TestBed.inject(SessionCoordinator).masked()).toBe(true);
    http.expectNone('/api/auth/atividade');
  });

  it('voltar de aba oculta descarta a deduplicação antiga e aceita nova edição real', () => {
    const input = document.createElement('input');
    browser.emit('pointerdown', { target: input }); http.expectOne('/api/auth/atividade').flush(sessionFixture());
    browser.shown = false; browser.emit('visibilitychange'); browser.shown = true; browser.emit('visibilitychange');
    http.expectOne('/api/auth/me').flush(sessionFixture()); http.expectOne('/api/auth/csrf').flush(null); TestBed.tick();
    browser.emit('input', { target: input });
    http.expectOne('/api/auth/atividade').flush(sessionFixture());
  });

  it.each(['pointercancel', 'contextmenu'])('%s encerra gesto anterior sem renovar atividade', type => {
    const input = document.createElement('input'); browser.emit('pointerdown', { target: input }); http.expectOne('/api/auth/atividade').flush(sessionFixture());
    browser.emit(type, { target: input }); http.expectNone('/api/auth/atividade');
    browser.emit('input', { target: input }); http.expectOne('/api/auth/atividade').flush(sessionFixture());
  });

  it('503 na revalidação mantém máscara até nova leitura bem-sucedida por foco', () => {
    browser.emit('focus');
    http.expectOne('/api/auth/me').flush({ code: 'SEGURANCA_INDISPONIVEL' }, { status: 503, statusText: 'Unavailable' });
    expect(TestBed.inject(SessionService).status()).toBe('unavailable');
    expect(TestBed.inject(SessionCoordinator).masked()).toBe(true);
    TestBed.tick(); vi.advanceTimersByTime(30_000); http.expectNone('/api/auth/me');
    browser.emit('focus');
    http.expectOne('/api/auth/me').flush(sessionFixture()); http.expectOne('/api/auth/csrf').flush(null); TestBed.tick();
    expect(TestBed.inject(SessionCoordinator).masked()).toBe(false);
    http.expectNone('/api/auth/atividade');
  });

  it('sinais com identidade, permissões ou prazos arbitrários são ignorados', () => {
    browser.receive?.({ type: 'SESSION_CHANGED', id: 1, papel: 'ADMIN' });
    browser.receive?.({ type: 'ACTIVITY_UPDATED', inativaEm: '2099-01-01T00:00:00Z' });
    expect(TestBed.inject(SessionService).identity()?.id).toBe(919);
    http.expectNone('/api/auth/me'); http.expectNone('/api/auth/atividade');
  });

  it('atividade válida de outra aba impede logout pelo timer local de A', () => {
    for (let elapsed = 30_000; elapsed < 600_000; elapsed += 30_000) {
      vi.advanceTimersByTime(30_000);
      http.expectOne('/api/auth/me').flush(sessionFixture({ horarioServidor: new Date(Date.UTC(2026, 9, 31, 12) + elapsed).toISOString() }));
      http.expectOne('/api/auth/csrf').flush(null); TestBed.tick();
    }
    vi.advanceTimersByTime(30_000);
    http.expectOne('/api/auth/me').flush(sessionFixture({ horarioServidor: '2026-10-31T12:10:00Z', ultimaAtividadeEm: '2026-10-31T12:09:00Z', inativaEm: '2026-10-31T12:19:00Z' }));
    http.expectOne('/api/auth/csrf').flush(null); TestBed.tick();
    expect(TestBed.inject(SessionService).authenticated()).toBe(true);
    expect(TestBed.inject(SessionCoordinator).masked()).toBe(false);
    expect(TestBed.inject(SessionService).deadlines()?.idle).toBe(1_140_000);
    http.expectNone('/api/auth/logout'); http.expectNone('/api/auth/atividade');
  });

  it('atividade contínua não estende o limite absoluto das 16h', () => {
    const session = TestBed.inject(SessionService);
    session.restore().subscribe();
    http.expectOne('/api/auth/me').flush(sessionFixture({ horarioServidor: '2026-10-31T15:59:30Z', ultimaAtividadeEm: '2026-10-31T15:59:30Z', inativaEm: '2026-10-31T16:09:30Z' }));
    http.expectOne('/api/auth/csrf').flush(null); TestBed.tick();
    vi.advanceTimersByTime(29_000); browser.emit('pointerdown');
    http.expectOne('/api/auth/atividade').flush(sessionFixture({ horarioServidor: '2026-10-31T15:59:59Z', ultimaAtividadeEm: '2026-10-31T15:59:59Z', inativaEm: '2026-10-31T16:09:59Z' }));
    TestBed.tick(); vi.advanceTimersByTime(999);
    expect(TestBed.inject(SessionCoordinator).masked()).toBe(false);
    vi.advanceTimersByTime(1);
    expect(TestBed.inject(SessionCoordinator).masked()).toBe(true);
    http.expectOne('/api/auth/me').flush({}, { status: 401, statusText: 'Absolute expiry' });
    expect(session.authenticated()).toBe(false);
    browser.emit('pointerdown');
    http.expectNone('/api/auth/atividade'); http.expectNone('/api/auth/login');
  });

  it.each(['pointermove', 'mousemove', 'scroll', 'wheel'])('%s não registra atividade', type => {
    browser.emit(type); http.expectNone('/api/auth/atividade');
  });

  it('eventos sintéticos, repetição de tecla e aba oculta não registram atividade', () => {
    browser.emit('pointerdown', { isTrusted: false }); browser.emit('keydown', { repeat: true });
    browser.shown = false; browser.emit('visibilitychange'); browser.emit('pointerdown');
    http.expectNone('/api/auth/atividade');
  });

  it('falha CSRF não repete atividade nem escoa comando pendente automaticamente', () => {
    browser.emit('pointerdown'); browser.emit('pointerdown');
    http.expectOne('/api/auth/atividade').flush({ code: 'CSRF_INVALIDO' }, { status: 403, statusText: 'Forbidden' });
    http.expectOne('/api/auth/csrf').flush(null);
    http.expectNone('/api/auth/atividade');
    browser.emit('pointerdown'); http.expectOne('/api/auth/atividade').flush(sessionFixture());
  });

  it('login e logout sinalizam somente depois da confirmação autoritativa', () => {
    const session = TestBed.inject(SessionService);
    session.login('fixture-ui', 'senha de fixture isolada').subscribe();
    http.expectOne('/api/auth/csrf').flush(null);
    expect(browser.sent).toEqual([]);
    http.expectOne('/api/auth/login').flush(sessionFixture());
    http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/auth/me').flush(sessionFixture());
    http.expectOne('/api/auth/csrf').flush(null);
    expect(browser.sent).toEqual(['SESSION_CHANGED']);
    TestBed.tick();
    session.logout().subscribe();
    expect(browser.sent).toEqual(['SESSION_CHANGED']);
    http.expectOne('/api/auth/logout').flush(null);
    http.expectOne('/api/auth/csrf').flush(null);
    expect(browser.sent).toEqual(['SESSION_CHANGED', 'SESSION_ENDED']);
  });

  it('comunica apenas o tipo de sinal depois de atividade confirmada', () => {
    browser.emit('pointerdown');
    expect(browser.sent).toEqual([]);
    http.expectOne('/api/auth/atividade').flush(sessionFixture());
    expect(browser.sent).toEqual(['ACTIVITY_UPDATED']);
  });

  it('sinal de encerramento mascara e consulta o backend, sem confiar no canal como autenticação', () => {
    browser.receive?.({ type: 'SESSION_ENDED' });
    expect(TestBed.inject(SessionCoordinator).masked()).toBe(true);
    expect(TestBed.inject(SessionService).status()).toBe('checking');
    expect(TestBed.inject(SessionService).authenticated()).toBe(false);
    http.expectOne('/api/auth/me').flush({}, { status: 401, statusText: 'Revoked' });
    expect(TestBed.inject(SessionService).status()).toBe('anonymous');
    http.expectNone('/api/auth/logout'); http.expectNone('/api/auth/atividade');
    expect(browser.sent).toEqual([]);
  });

  it('resposta antiga de /me não desfaz atividade mais recente já confirmada', () => {
    vi.advanceTimersByTime(30_000);
    const reading = http.expectOne('/api/auth/me');
    browser.emit('pointerdown');
    http.expectOne('/api/auth/atividade').flush(sessionFixture({ horarioServidor: '2026-10-31T12:00:30Z', ultimaAtividadeEm: '2026-10-31T12:00:30Z', inativaEm: '2026-10-31T12:10:30Z' }));
    reading.flush(sessionFixture({ horarioServidor: '2026-10-31T12:00:29Z' }));
    http.expectOne('/api/auth/csrf').flush(null); TestBed.tick();
    expect(TestBed.inject(SessionService).identity()?.ultimaAtividadeEm).toBe('2026-10-31T12:00:30Z');
    expect(TestBed.inject(SessionService).identity()?.inativaEm).toBe('2026-10-31T12:10:30Z');
  });

  it('na borda exata de dez minutos mascara e revalida antes de exigir login, sem POST de logout', () => {
    for (let elapsed = 30_000; elapsed < 600_000; elapsed += 30_000) {
      vi.advanceTimersByTime(30_000);
      http.expectOne('/api/auth/me').flush(sessionFixture({ horarioServidor: new Date(Date.UTC(2026, 9, 31, 12) + elapsed).toISOString() }));
      http.expectOne('/api/auth/csrf').flush(null); TestBed.tick();
    }
    vi.advanceTimersByTime(29_999);
    expect(TestBed.inject(SessionCoordinator).masked()).toBe(false);
    vi.advanceTimersByTime(1);
    expect(TestBed.inject(SessionCoordinator).masked()).toBe(true);
    http.expectOne('/api/auth/me').flush({}, { status: 401, statusText: 'Expired' });
    expect(TestBed.inject(SessionService).status()).toBe('anonymous');
    http.expectNone('/api/auth/atividade'); http.expectNone('/api/auth/logout');
  });

  it('mascara aba oculta e só libera após /me e CSRF no retorno, sem atividade por foco', () => {
    const coordinator = TestBed.inject(SessionCoordinator);
    browser.shown = false; browser.emit('visibilitychange');
    expect(coordinator.masked()).toBe(true);
    browser.shown = true; browser.emit('visibilitychange');
    expect(coordinator.masked()).toBe(true);
    http.expectOne('/api/auth/me').flush(sessionFixture());
    expect(coordinator.masked()).toBe(true);
    http.expectOne('/api/auth/csrf').flush(null);
    expect(coordinator.masked()).toBe(false);
    http.expectNone('/api/auth/atividade');
  });

  it('fallback visível consulta /me aos 30s sem atividade e não consulta em aba oculta', () => {
    vi.advanceTimersByTime(30_000);
    http.expectOne('/api/auth/me').flush(sessionFixture({ horarioServidor: '2026-10-31T12:00:30Z' }));
    http.expectOne('/api/auth/csrf').flush(null);
    TestBed.tick();
    http.expectNone('/api/auth/atividade');
    browser.shown = false; browser.emit('visibilitychange');
    vi.advanceTimersByTime(60_000);
    http.expectNone('/api/auth/me');
    http.expectNone('/api/auth/atividade');
  });

  it('atividade confirmada atualiza o prazo público sem alterar o limite absoluto', () => {
    browser.emit('pointerdown');
    http.expectOne('/api/auth/atividade').flush(sessionFixture({ horarioServidor: '2026-10-31T12:03:00Z', ultimaAtividadeEm: '2026-10-31T12:03:00Z', inativaEm: '2026-10-31T12:13:00Z' }));
    expect(TestBed.inject(SessionService).identity()?.inativaEm).toBe('2026-10-31T12:13:00Z');
    expect(TestBed.inject(SessionService).identity()?.expiraEm).toBe('2026-10-31T16:00:00Z');
    http.verify();
  });

  it.each(['scrollend', 'click'])('reconhece %s confiável sem depender de polling', type => {
    const target = document.createElement('a'); target.href = '/dashboard';
    browser.emit(type, { target });
    http.expectOne('/api/auth/atividade').flush(sessionFixture());
    http.expectNone('/api/auth/atividade');
  });

  it('pointerdown, click e input/change do mesmo controle não criam nova atividade pendente', () => {
    const target = document.createElement('input'); target.type = 'checkbox';
    browser.emit('pointerdown', { target }); browser.emit('click', { target });
    browser.emit('input', { target }); browser.emit('change', { target });
    http.expectOne('/api/auth/atividade').flush(sessionFixture());
    http.expectNone('/api/auth/atividade');
  });

  it('deduplica teclado, edição e change sem perder a próxima interação', () => {
    const target = document.createElement('input');
    browser.emit('keydown', { target, repeat: false });
    browser.emit('input', { target }); browser.emit('change', { target }); browser.emit('keyup', { target });
    const first = http.match('/api/auth/atividade'); expect(first).toHaveLength(1);
    browser.emit('keydown', { target, repeat: false }); browser.emit('input', { target }); browser.emit('keyup', { target });
    first[0].flush(sessionFixture());
    http.expectOne('/api/auth/atividade').flush(sessionFixture());
    http.expectNone('/api/auth/atividade');
  });

  it('cem interações mantêm uma chamada em voo e escoam só a última pendente sem espera', () => {
    for (let i = 0; i < 100; i++) browser.emit('pointerdown');
    const requests = http.match('/api/auth/atividade');
    expect(requests).toHaveLength(1);
    requests[0].flush(sessionFixture());
    http.expectOne('/api/auth/atividade').flush(sessionFixture());
    http.expectNone('/api/auth/atividade');
  });

  it('pointerdown confiável em aba visível comunica atividade imediatamente e sem timestamp do cliente', () => {
    browser.emit('pointerdown');
    const activity = http.expectOne('/api/auth/atividade');
    expect(activity.request.method).toBe('POST');
    expect(activity.request.body).toBeNull();
    activity.flush(sessionFixture());
    http.expectNone('/api/auth/atividade');
  });
});
