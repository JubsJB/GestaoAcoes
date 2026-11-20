import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { sessionFixture } from '../../../../test-support/session-fixture';
import { SessionService } from '../../core/auth/session.service';
import { sessionInterceptor } from '../../core/auth/session.interceptor';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';
import { provideApiConfig } from '../../core/config/api.config';
import { PasswordPageComponent } from './password-page.component';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../../app.routes';

function fill(element: HTMLElement): void {
  for (const [name, value] of Object.entries({ username: 'usuario.teste', codigo: 'ABCDEFGHIJKLMNOPQRSTUVWXYA', senha: 'senha de fixture', confirmacao: 'senha de fixture' })) {
    const input = element.querySelector<HTMLInputElement>(`[name=${name}]`)!; input.value = value; input.dispatchEvent(new Event('input'));
  }
}

describe('Definição pública de senha', () => {
  afterEach(() => TestBed.resetTestingModule());
  it.each([['/definir-senha', 'Definir senha'], ['/redefinir-senha', 'Redefinir senha']])('abre %s sem sessão ou consultas financeiras', async (url, heading) => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const harness = await RouterTestingHarness.create(url);
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe(heading);
    expect(harness.routeNativeElement?.querySelector('app-carteira-selector')).toBeNull();
    TestBed.inject(HttpTestingController).expectNone(() => true);
  });

  it.each(['a'.repeat(72), '😀'.repeat(18), '😀'.repeat(8)])('aceita limites Unicode/UTF-8 sem truncamento (%#)', senha => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const fixture = TestBed.createComponent(PasswordPageComponent); fixture.detectChanges(); const element: HTMLElement = fixture.nativeElement; fill(element);
    for (const name of ['senha', 'confirmacao']) { const input = element.querySelector<HTMLInputElement>(`[name=${name}]`)!; input.value = senha; input.dispatchEvent(new Event('input')); }
    element.querySelector('form')!.dispatchEvent(new Event('submit')); const http = TestBed.inject(HttpTestingController); http.expectOne('/api/auth/csrf').flush(null);
    const request = http.expectOne('/api/auth/redefinicoes/confirmar'); expect(request.request.body.senha).toBe(senha); request.flush(null, { status: 204, statusText: 'No Content' }); http.verify();
  });

  it.each(['existente', 'inexistente'])('mantém erro genérico de código/identidade para %s sem consulta de existência', username => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(withInterceptors([httpErrorInterceptor, sessionInterceptor])), provideHttpClientTesting(), provideApiConfig()] });
    const fixture = TestBed.createComponent(PasswordPageComponent); fixture.detectChanges(); const element: HTMLElement = fixture.nativeElement; fill(element);
    const input = element.querySelector<HTMLInputElement>('[name=username]')!; input.value = username; input.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit')); const http = TestBed.inject(HttpTestingController); http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/auth/redefinicoes/confirmar').flush({ timeStamp: 0, status: 400, error: 'Bad Request', message: 'Detalhe que não deve ser exibido', path: '/auth/redefinicoes/confirmar', code: 'REDEFINICAO_INVALIDA', details: {} }, { status: 400, statusText: 'Bad Request' }); fixture.detectChanges();
    expect(element.querySelector('[role=alert]')?.textContent).toBe('Não foi possível confirmar a definição de senha. Confira os dados ou solicite um novo código ao administrador.');
    http.expectNone(request => request.url.startsWith('/api/usuarios')); http.verify();
  });

  it('CSRF rejeitado só busca token novo e permite nova submissão explícita', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(withInterceptors([httpErrorInterceptor, sessionInterceptor])), provideHttpClientTesting(), provideApiConfig()] });
    const fixture = TestBed.createComponent(PasswordPageComponent); fixture.detectChanges(); const element: HTMLElement = fixture.nativeElement; fill(element);
    element.querySelector('form')!.dispatchEvent(new Event('submit')); const http = TestBed.inject(HttpTestingController); http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/auth/redefinicoes/confirmar').flush({ code: 'CSRF_INVALIDO' }, { status: 403, statusText: 'Forbidden' }); http.expectOne('/api/auth/csrf').flush(null); fixture.detectChanges();
    expect(element.querySelector('[role=alert]')?.textContent).toContain('proteção do formulário');
    http.expectNone('/api/auth/redefinicoes/confirmar'); http.expectNone('/api/auth/login'); http.verify();
  });
  it('rejeição de senha do servidor orienta validação sem sugerir estado da conta', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(withInterceptors([httpErrorInterceptor, sessionInterceptor])), provideHttpClientTesting(), provideApiConfig()] });
    const fixture = TestBed.createComponent(PasswordPageComponent); fixture.detectChanges(); const element: HTMLElement = fixture.nativeElement;
    fill(element); element.querySelector('form')!.dispatchEvent(new Event('submit'));
    const http = TestBed.inject(HttpTestingController); http.expectOne('/api/auth/csrf').flush(null);
    http.expectOne('/api/auth/redefinicoes/confirmar').flush({ timeStamp: 0, status: 400, error: 'Bad Request', message: 'Validação', path: '/auth/redefinicoes/confirmar', code: 'SENHA_INVALIDA', details: {} }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges(); expect(element.querySelector('[role=alert]')?.textContent).toContain('nova senha e sua confirmação');
    http.expectNone('/api/auth/login'); http.verify();
  });
  it('429 impede nova confirmação até Retry-After sem consumo automático ou novo login', () => {
    vi.useFakeTimers();
    try {
      TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(withInterceptors([httpErrorInterceptor, sessionInterceptor])), provideHttpClientTesting(), provideApiConfig()] });
      const fixture = TestBed.createComponent(PasswordPageComponent); fixture.detectChanges(); const element: HTMLElement = fixture.nativeElement;
      fill(element); element.querySelector('form')!.dispatchEvent(new Event('submit'));
      const http = TestBed.inject(HttpTestingController); http.expectOne('/api/auth/csrf').flush(null);
      http.expectOne('/api/auth/redefinicoes/confirmar').flush({}, { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '2' } }); fixture.detectChanges();
      const button = element.querySelector<HTMLButtonElement>('button[type=submit]')!;
      expect(button.disabled).toBe(true); vi.advanceTimersByTime(2000); fixture.detectChanges(); expect(button.disabled).toBe(false);
      http.expectNone('/api/auth/redefinicoes/confirmar'); http.expectNone('/api/auth/login'); http.verify();
    } finally { TestBed.resetTestingModule(); vi.useRealTimers(); }
  });
  it.each([
    ['1234567', '1234567'],
    ['😀'.repeat(7), '😀'.repeat(7)],
    ['😀'.repeat(19), '😀'.repeat(19)],
    [' senha segura ', 'senha segura']
  ])('validação local não envia senha fora da política ou confirmação divergente (%#)', (senha, confirmacao) => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const fixture = TestBed.createComponent(PasswordPageComponent); fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    for (const [name, value] of Object.entries({ username: 'usuario.teste', codigo: 'ABCDEFGHIJKLMNOPQRSTUVWXYA', senha, confirmacao })) {
      const input = element.querySelector<HTMLInputElement>(`[name=${name}]`)!; input.value = value; input.dispatchEvent(new Event('input'));
    }
    element.querySelector('form')!.dispatchEvent(new Event('submit')); fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectNone(() => true);
    expect(element.querySelector('mat-error')).not.toBeNull();
  });
  it('confirma com CSRF, preserva senha literal e exige login separado após 204', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(withInterceptors([httpErrorInterceptor, sessionInterceptor])), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const session = TestBed.inject(SessionService);
    session.restore().subscribe(); http.expectOne('/api/auth/me').flush(sessionFixture({ username: 'usuario.teste', papel: 'USER' })); http.expectOne('/api/auth/csrf').flush(null);
    const fixture = TestBed.createComponent(PasswordPageComponent); fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const values = { username: ' Usuario.Teste ', codigo: 'abcd efgh-ijkl mnop-qrst uvwx-ya', senha: ' senha segura ', confirmacao: ' senha segura ' };
    for (const [name, value] of Object.entries(values)) {
      const input = element.querySelector<HTMLInputElement>(`[name=${name}]`)!; input.value = value; input.dispatchEvent(new Event('input'));
    }
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    http.expectOne('/api/auth/csrf').flush(null);
    const confirmation = http.expectOne('/api/auth/redefinicoes/confirmar');
    expect(confirmation.request.body).toEqual({ ...values, username: 'usuario.teste' });
    expect(confirmation.request.headers.has('Authorization')).toBe(false);
    confirmation.flush(null, { status: 204, statusText: 'No Content' }); fixture.detectChanges();
    expect(element.textContent).toContain('Senha definida com sucesso');
    expect(element.querySelector('input[type=password]')).toBeNull();
    expect(session.authenticated()).toBe(false);
    expect(TestBed.inject(Router).url).toBe('/');
    http.expectNone('/api/auth/login'); http.expectNone(request => request.url.startsWith('/api/usuarios')); http.verify();
  });
});
