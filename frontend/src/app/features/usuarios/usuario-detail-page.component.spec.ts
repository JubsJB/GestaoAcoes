import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { authenticatedModule } from '../../../../test-support/authenticated-module';
import { usuarioFixture } from '../../../../test-support/usuario-fixture';
import { UsuarioDetailPageComponent } from './usuario-detail-page.component';
import { SessionService } from '../../core/auth/session.service';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';

describe('Consulta e alterações administrativas', () => {
  it('permite consultar outro usuário após uma primeira consulta 404 na mesma instância de rota', async () => {
    authenticatedModule({ providers: [provideRouter([{ path: 'usuarios/:id', component: UsuarioDetailPageComponent }])] });
    const harness = await RouterTestingHarness.create(); const first = harness.navigateByUrl('/usuarios/999'); const http = TestBed.inject(HttpTestingController);
    await vi.waitFor(() => http.expectOne('/api/usuarios/999').flush({}, { status: 404, statusText: 'Not Found' })); await first;
    const second = harness.navigateByUrl('/usuarios/7');
    await vi.waitFor(() => http.expectOne('/api/usuarios/7').flush(usuarioFixture())); await second; harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('alice'); http.verify();
  });
  async function open(overrides = {}, self = false) {
    authenticatedModule({ providers: [provideHttpClient(withInterceptors([httpErrorInterceptor])), provideRouter([{ path: 'usuarios/:id', component: UsuarioDetailPageComponent }])] });
    if (self) {
      const session = TestBed.inject(SessionService); session.restore().subscribe();
      TestBed.inject(HttpTestingController).expectOne('/api/auth/me').flush({ ...session.identity(), id: 7 });
      TestBed.inject(HttpTestingController).expectOne('/api/auth/csrf').flush(null);
    }
    const harness = await RouterTestingHarness.create(); const navigation = harness.navigateByUrl('/usuarios/7');
    const http = TestBed.inject(HttpTestingController);
    await vi.waitFor(() => http.expectOne('/api/usuarios/7').flush(usuarioFixture(overrides))); await navigation; harness.detectChanges();
    return { harness, http, element: harness.routeNativeElement!, overlay: TestBed.inject(OverlayContainer).getContainerElement() };
  }
  async function confirm(context: Awaited<ReturnType<typeof open>>, selector: string) {
    context.element.querySelector<HTMLButtonElement>(selector)!.click(); context.harness.detectChanges();
    const closed = firstValueFrom(TestBed.inject(MatDialog).afterAllClosed);
    context.overlay.querySelector<HTMLButtonElement>('[data-testid=confirm-admin]')!.click();
    await closed;
  }
  it.each(['ULTIMO_ADMIN_ATIVO', 'VERSAO_USUARIO_OBSOLETA'])('409 %s preserva estado, não repete escrita e exige consulta atualizada', async code => {
    const context = await open(); await confirm(context, '[data-testid=toggle-status]');
    const request = context.http.expectOne('/api/usuarios/7/situacao'); request.flush({ timeStamp: Date.now(), status: 409, error: 'Conflict', message: 'Recusa de negócio', path: '/usuarios/7/situacao', code, details: {} }, { status: 409, statusText: 'Conflict' }); context.harness.detectChanges();
    expect(context.element.textContent).toContain(code === 'ULTIMO_ADMIN_ATIVO' ? 'último administrador' : 'Atualize os dados');
    expect(context.element.querySelector<HTMLButtonElement>('[data-testid=toggle-status]')!.disabled).toBe(true);
    expect(context.element.textContent).toContain('Ativo'); context.http.expectNone(request => request.method === 'PATCH'); context.http.verify();
  });
  it('habilita conta bloqueada sem sugerir restauração de sessões ou códigos', async () => {
    const context = await open({ ativo: false }); await confirm(context, '[data-testid=toggle-status]');
    const request = context.http.expectOne('/api/usuarios/7/situacao'); expect(request.request.body).toEqual({ ativo: true, versaoEsperada: 3 }); request.flush(usuarioFixture({ versao: 4 }));
    context.harness.detectChanges(); expect(context.element.textContent).toContain('Ativo'); expect(context.element.textContent).toContain('não restaura'); context.http.verify();
  });
  it('cancelar confirmação não envia comando', async () => {
    const context = await open(); context.element.querySelector<HTMLButtonElement>('[data-testid=toggle-status]')!.click(); context.harness.detectChanges();
    context.overlay.querySelector<HTMLButtonElement>('button[mat-dialog-close]')!.click(); await Promise.resolve();
    context.http.expectNone(request => request.method !== 'GET'); context.http.verify();
  });
  it.each([false, true])('autoalteração de perfil encerra a sessão apenas quando efetiva=%s', async effective => {
    const context = await open({ papel: 'ADMIN' }, true);
    const session = TestBed.inject(SessionService);
    await confirm(context, '[data-testid=save-role]');
    const patch = context.http.expectOne('/api/usuarios/7/perfil'); expect(patch.request.body).toEqual({ papel: 'ADMIN', versaoEsperada: 3 });
    patch.flush(usuarioFixture({ papel: effective ? 'USER' : 'ADMIN', versao: effective ? 4 : 3 }));
    expect(session.authenticated()).toBe(!effective); context.http.verify();
  });
  it('emissão atualiza versão pública e não encerra a sessão; novo PATCH usa a versão recebida', async () => {
    const context = await open(); await confirm(context, '[data-testid=issue-code]');
    const issuance = context.http.expectOne('/api/usuarios/7/redefinicoes'); expect(issuance.request.body).toEqual({ versaoEsperada: 3 });
    issuance.flush({ usuario: usuarioFixture({ versao: 4 }), autorizacao: { codigo: 'ABCDEFGHIJKLMNOPQRSTUVWXYA', motivo: 'REDEFINICAO', expiraEm: '2026-10-31T12:15:00Z' } }); context.harness.detectChanges();
    expect(TestBed.inject(SessionService).authenticated()).toBe(true);
    context.overlay.querySelector<HTMLButtonElement>('[data-testid=close-code]')!.click(); await Promise.resolve();
    await confirm(context, '[data-testid=toggle-status]'); const patch = context.http.expectOne('/api/usuarios/7/situacao'); expect(patch.request.body).toEqual({ ativo: false, versaoEsperada: 4 }); patch.flush(usuarioFixture({ ativo: false, versao: 5 })); context.http.verify();
  });
  it('carrega o usuário e exige confirmação antes de bloquear com a versão corrente', async () => {
    authenticatedModule({ providers: [provideRouter([{ path: 'usuarios/:id', component: UsuarioDetailPageComponent }])] });
    const harness = await RouterTestingHarness.create(); const navigation = harness.navigateByUrl('/usuarios/7');
    const http = TestBed.inject(HttpTestingController);
    await vi.waitFor(() => http.expectOne('/api/usuarios/7').flush(usuarioFixture())); await navigation; harness.detectChanges();
    const element = harness.routeNativeElement!;
    expect(element.textContent).toContain('alice');
    element.querySelector<HTMLButtonElement>('[data-testid=toggle-status]')!.click(); harness.detectChanges();
    http.expectNone(request => request.method === 'PATCH');
    const overlay = TestBed.inject(OverlayContainer).getContainerElement();
    overlay.querySelector<HTMLButtonElement>('[data-testid=confirm-admin]')!.click();
    await vi.waitFor(() => {
      const request = http.expectOne('/api/usuarios/7/situacao');
      expect(request.request.method).toBe('PATCH'); expect(request.request.body).toEqual({ ativo: false, versaoEsperada: 3 });
      request.flush(usuarioFixture({ ativo: false, versao: 4 }));
    });
    harness.detectChanges(); expect(element.textContent).toContain('Bloqueado');
    expect(element.querySelector<HTMLButtonElement>('[data-testid=toggle-status]')!.textContent).toContain('Habilitar'); http.verify();
  });
});
