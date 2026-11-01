import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { provideApiConfig } from '../../core/config/api.config';
import { UsuariosApi } from './usuarios-api.service';
import { usuarioFixture } from '../../../../test-support/usuario-fixture';

describe('Contrato administrativo Angular', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] }));
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  it.each([{ versao: Number.MAX_SAFE_INTEGER + 1 }, { id: 0 }, { username: 'inválido' }, { papel: 'OWNER' }, { ativo: null }, { criadaEm: 'não-data' }])('rejeita DTO administrativo inconsistente %j sem repassar detalhes', async patch => {
    const response = firstValueFrom(TestBed.inject(UsuariosApi).buscar(7)); const rejection = expect(response).rejects.toMatchObject({ status: 503 });
    TestBed.inject(HttpTestingController).expectOne('/api/usuarios/7').flush({ ...usuarioFixture(), ...patch }); await rejection;
  });
  it('não mistura respostas de outro usuário nem aceita IDs duplicados', async () => {
    const api = TestBed.inject(UsuariosApi); const http = TestBed.inject(HttpTestingController);
    const detail = firstValueFrom(api.buscar(7)); const rejected = expect(detail).rejects.toMatchObject({ status: 503 }); http.expectOne('/api/usuarios/7').flush(usuarioFixture({ id: 8 })); await rejected;
    const list = firstValueFrom(api.listar()); const duplicate = expect(list).rejects.toMatchObject({ status: 503 }); http.expectOne('/api/usuarios').flush([usuarioFixture(), usuarioFixture()]); await duplicate;
  });
  it('envia perfil/situação e autorização somente com versão pública esperada', async () => {
    const api = TestBed.inject(UsuariosApi); const http = TestBed.inject(HttpTestingController);
    const profile = firstValueFrom(api.perfil(7, 'ADMIN', 3)); const patch = http.expectOne('/api/usuarios/7/perfil'); expect(patch.request.body).toEqual({ papel: 'ADMIN', versaoEsperada: 3 }); patch.flush(usuarioFixture({ papel: 'ADMIN', versao: 4 })); expect((await profile).versao).toBe(4);
    const status = firstValueFrom(api.situacao(7, false, 4)); const blocked = http.expectOne('/api/usuarios/7/situacao'); expect(blocked.request.body).toEqual({ ativo: false, versaoEsperada: 4 }); blocked.flush(usuarioFixture({ ativo: false, versao: 5 })); expect((await status).ativo).toBe(false);
  });
  it.each(['codigo-invalido', 'ABCDEFGHIJKLMNOPQRSTUVWXYA'])('não exibe autorização inconsistente recebida do servidor', async codigo => {
    const response = firstValueFrom(TestBed.inject(UsuariosApi).autorizar(7, 3)); const rejected = expect(response).rejects.toMatchObject({ status: 503 });
    TestBed.inject(HttpTestingController).expectOne('/api/usuarios/7/redefinicoes').flush({ usuario: usuarioFixture(), autorizacao: { codigo, motivo: 'INICIAL', expiraEm: '2026-10-31T12:15:00Z' } }); await rejected;
  });
  it('consulta usuários com versão pública e não repassa campos sensíveis inesperados', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const response = firstValueFrom(TestBed.inject(UsuariosApi).listar());
    const user = { id: 7, username: 'alice', papel: 'USER', ativo: true, credencialDefinida: true, criadaEm: '2026-10-31T12:00:00Z', atualizadaEm: '2026-10-31T12:00:00Z', versao: 3 };
    const request = http.expectOne('/api/usuarios'); expect(request.request.method).toBe('GET');
    request.flush([{ ...user, senhaHash: 'fixture-sem-credencial', versaoSeguranca: 19, codigo: 'nao-retornar' }]);
    expect(await response).toEqual([user]); http.verify();
  });
});
