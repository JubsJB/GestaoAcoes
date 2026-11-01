import { Sessao } from '../src/app/core/auth/auth-api.service';

export function sessionFixture(overrides: Partial<Sessao> = {}): Sessao {
  return {
    id: 919, username: 'fixture-ui', papel: 'ADMIN',
    horarioServidor: '2026-10-31T12:00:00Z', ultimaAtividadeEm: '2026-10-31T12:00:00Z',
    inativaEm: '2026-10-31T12:10:00Z', expiraEm: '2026-10-31T16:00:00Z',
    ...overrides
  };
}
