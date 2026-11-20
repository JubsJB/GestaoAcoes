import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { switchMap, map, Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api-base-url.token';

export interface Sessao {
  id: number;
  username: string;
  papel: 'ADMIN' | 'USER';
  horarioServidor: string;
  ultimaAtividadeEm: string;
  inativaEm: string;
  expiraEm: string;
}

export interface ConfirmacaoSenha { username: string; codigo: string; senha: string; confirmacao: string; }

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  csrf(): Observable<void> { return this.http.get<void>(`${this.base}/auth/csrf`); }
  me(): Observable<Sessao> { return this.http.get<Sessao>(`${this.base}/auth/me`); }
  logout(): Observable<void> { return this.http.post<void>(`${this.base}/auth/logout`, null); }
  activity(): Observable<Sessao> { return this.http.post<Sessao>(`${this.base}/auth/atividade`, null); }
  confirmPassword(command: ConfirmacaoSenha): Observable<void> {
    return this.csrf().pipe(switchMap(() => this.http.post<void>(`${this.base}/auth/redefinicoes/confirmar`, command)));
  }
  login(username: string, senha: string): Observable<Sessao> {
    return this.csrf().pipe(
      switchMap(() => this.http.post<Sessao>(`${this.base}/auth/login`, { username, senha })),
      switchMap(sessao => this.csrf().pipe(map(() => sessao)))
    );
  }
}
