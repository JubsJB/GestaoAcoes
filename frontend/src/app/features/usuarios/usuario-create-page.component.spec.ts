import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { authenticatedModule } from '../../../../test-support/authenticated-module';
import { usuarioFixture } from '../../../../test-support/usuario-fixture';
import { UsuarioCreatePageComponent } from './usuario-create-page.component';
import { SessionService } from '../../core/auth/session.service';

@Component({ selector: 'test-created-user', template: 'Consulta do usuário criado' }) class CreatedUserDestination {}

describe('Cadastro administrativo e código inicial', () => {
  it('resposta tardia de emissão não abre código depois de encerrar a identidade', () => {
    authenticatedModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(UsuarioCreatePageComponent); fixture.detectChanges(); const element: HTMLElement = fixture.nativeElement;
    const username = element.querySelector<HTMLInputElement>('[name=username]')!; username.value = 'novo.usuario'; username.dispatchEvent(new Event('input')); element.querySelector('form')!.dispatchEvent(new Event('submit'));
    const http = TestBed.inject(HttpTestingController); const request = http.expectOne('/api/usuarios');
    TestBed.inject(SessionService).end();
    request.flush({ usuario: usuarioFixture({ username: 'novo.usuario', credencialDefinida: false }), autorizacao: { codigo: 'ABCDEFGHIJKLMNOPQRSTUVWXYA', motivo: 'INICIAL', expiraEm: '2026-10-31T12:15:00Z' } }); fixture.detectChanges();
    expect(TestBed.inject(OverlayContainer).getContainerElement().querySelector('[data-testid=one-time-code]')).toBeNull(); http.verify();
  });
  it('envia somente username/perfil e exibe o código uma vez, sem senha administrativa', async () => {
    authenticatedModule({ providers: [provideRouter([{ path: 'usuarios/:id', component: CreatedUserDestination }])] });
    const fixture = TestBed.createComponent(UsuarioCreatePageComponent); fixture.detectChanges(); const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('input[type=password]')).toBeNull();
    const username = element.querySelector<HTMLInputElement>('[name=username]')!; username.value = ' Novo.Usuario '; username.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    const http = TestBed.inject(HttpTestingController); const request = http.expectOne('/api/usuarios');
    expect(request.request.method).toBe('POST'); expect(request.request.body).toEqual({ username: 'novo.usuario', papel: 'USER' });
    request.flush({ usuario: usuarioFixture({ id: 8, username: 'novo.usuario', credencialDefinida: false, versao: 1 }), autorizacao: { codigo: 'ABCDEFGHIJKLMNOPQRSTUVWXYA', motivo: 'INICIAL', expiraEm: new Date(Date.now() + 900_000).toISOString() } }, { status: 201, statusText: 'Created', headers: { Location: '/usuarios/8', 'Cache-Control': 'no-store' } });
    fixture.detectChanges();
    const overlay = TestBed.inject(OverlayContainer).getContainerElement();
    await vi.waitFor(() => expect(overlay.querySelector<HTMLInputElement>('[data-testid=one-time-code]')?.value === 'ABCDEFGHIJKLMNOPQRSTUVWXYA').toBe(true));
    expect(overlay.textContent).toContain('novo.usuario');
    overlay.querySelector<HTMLButtonElement>('[data-testid=close-code]')!.click();
    await vi.waitFor(() => expect(TestBed.inject(Router).url).toBe('/usuarios/8'));
    expect(overlay.querySelector('[data-testid=one-time-code]')).toBeNull();
    http.expectNone(request => request.url.includes('/redefinicoes')); http.verify();
  });
});
