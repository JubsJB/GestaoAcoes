import { HttpErrorResponse } from '@angular/common/http';

export type Papel = 'ADMIN' | 'USER';
export interface Usuario {
  id: number;
  username: string;
  papel: Papel;
  ativo: boolean;
  credencialDefinida: boolean;
  criadaEm: string;
  atualizadaEm: string;
  versao: number;
}

export function usuarioResponse(value: unknown): Usuario {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw invalidUserResponse();
  const { id, username, papel, ativo, credencialDefinida, criadaEm, atualizadaEm, versao } = value as Record<string, unknown>;
  if (typeof id !== 'number' || !Number.isSafeInteger(id) || id <= 0
    || typeof username !== 'string' || !/^[a-z0-9._-]{3,100}$/.test(username)
    || (papel !== 'ADMIN' && papel !== 'USER') || typeof ativo !== 'boolean' || typeof credencialDefinida !== 'boolean'
    || typeof criadaEm !== 'string' || !Number.isFinite(Date.parse(criadaEm))
    || typeof atualizadaEm !== 'string' || !Number.isFinite(Date.parse(atualizadaEm))
    || typeof versao !== 'number' || !Number.isSafeInteger(versao) || versao < 0) throw invalidUserResponse();
  return { id, username, papel, ativo, credencialDefinida, criadaEm, atualizadaEm, versao };
}

export function invalidUserResponse(): HttpErrorResponse {
  return new HttpErrorResponse({ status: 503, statusText: 'Resposta administrativa inválida' });
}

export interface EmissaoAutorizacao {
  usuario: Usuario;
  autorizacao: { codigo: string; motivo: 'INICIAL' | 'REDEFINICAO'; expiraEm: string };
}

export function emissaoResponse(value: unknown): EmissaoAutorizacao {
  if (!value || typeof value !== 'object') throw invalidUserResponse();
  const envelope = value as Record<string, unknown>;
  const usuario = usuarioResponse(envelope['usuario']);
  const authorization = envelope['autorizacao'];
  if (!authorization || typeof authorization !== 'object') throw invalidUserResponse();
  const { codigo, motivo, expiraEm } = authorization as Record<string, unknown>;
  if (typeof codigo !== 'string' || !/^[A-Z2-7]{26}$/.test(codigo)
    || (motivo !== 'INICIAL' && motivo !== 'REDEFINICAO') || motivo !== (usuario.credencialDefinida ? 'REDEFINICAO' : 'INICIAL')
    || typeof expiraEm !== 'string' || !Number.isFinite(Date.parse(expiraEm))) throw invalidUserResponse();
  return { usuario, autorizacao: { codigo, motivo, expiraEm } };
}
