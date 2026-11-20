import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/config/api-base-url.token';
import { EmissaoAutorizacao, emissaoResponse, invalidUserResponse, Papel, Usuario, usuarioResponse } from './usuario.model';

@Injectable({ providedIn: 'root' })
export class UsuariosApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_BASE_URL)}/usuarios`;

  buscar(id: number): Observable<Usuario> {
    return this.http.get<unknown>(`${this.base}/${id}`, { cache: 'no-store' }).pipe(map(value => this.forId(value, id)));
  }

  perfil(id: number, papel: Papel, versaoEsperada: number): Observable<Usuario> {
    return this.http.patch<unknown>(`${this.base}/${id}/perfil`, { papel, versaoEsperada }, { cache: 'no-store' }).pipe(map(value => this.forId(value, id)));
  }

  situacao(id: number, ativo: boolean, versaoEsperada: number): Observable<Usuario> {
    return this.http.patch<unknown>(`${this.base}/${id}/situacao`, { ativo, versaoEsperada }, { cache: 'no-store' }).pipe(map(value => this.forId(value, id)));
  }

  autorizar(id: number, versaoEsperada: number): Observable<EmissaoAutorizacao> {
    return this.http.post<unknown>(`${this.base}/${id}/redefinicoes`, { versaoEsperada }, { cache: 'no-store' }).pipe(map(value => {
      const emission = emissaoResponse(value);
      if (emission.usuario.id !== id) throw invalidUserResponse();
      return emission;
    }));
  }

  private forId(value: unknown, id: number): Usuario {
    const usuario = usuarioResponse(value);
    if (usuario.id !== id) throw invalidUserResponse();
    return usuario;
  }

  cadastrar(command: { username: string; papel: Papel }): Observable<EmissaoAutorizacao> {
    const username = command.username.trim().toLowerCase();
    return this.http.post<unknown>(this.base, { username, papel: command.papel }, { cache: 'no-store' }).pipe(map(value => {
      const emission = emissaoResponse(value);
      if (emission.usuario.username !== username) throw invalidUserResponse();
      return emission;
    }));
  }

  listar(): Observable<Usuario[]> {
    return this.http.get<unknown>(this.base, { cache: 'no-store' }).pipe(map(value => {
      if (!Array.isArray(value)) throw invalidUserResponse();
      const users = value.map(usuarioResponse);
      if (new Set(users.map(user => user.id)).size !== users.length) throw invalidUserResponse();
      return users;
    }));
  }
}
