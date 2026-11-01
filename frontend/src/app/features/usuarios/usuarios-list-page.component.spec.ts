import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authenticatedModule } from '../../../../test-support/authenticated-module';
import { usuarioFixture } from '../../../../test-support/usuario-fixture';
import { UsuariosListPageComponent } from './usuarios-list-page.component';

describe('Listagem administrativa', () => {
  it('ADMIN consulta contas e abre detalhe sem campos para senhas ou códigos', () => {
    authenticatedModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(UsuariosListPageComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController); http.expectOne('/api/usuarios').flush([usuarioFixture()]); fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('h1')?.textContent).toBe('Usuários');
    expect(element.querySelector('tbody')?.textContent).toContain('alice');
    expect(element.querySelector('a[href="/usuarios/7"]')).not.toBeNull();
    expect(element.querySelector('a[href="/usuarios/novo"]')).not.toBeNull();
    expect(element.querySelector('input[type=password]')).toBeNull(); http.verify();
  });
});
