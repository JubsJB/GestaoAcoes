import { Usuario } from '../src/app/features/usuarios/usuario.model';

export function usuarioFixture(overrides: Partial<Usuario> = {}): Usuario {
  return { id: 7, username: 'alice', papel: 'USER', ativo: true, credencialDefinida: true, criadaEm: '2026-10-31T12:00:00Z', atualizadaEm: '2026-10-31T12:00:00Z', versao: 3, ...overrides };
}
