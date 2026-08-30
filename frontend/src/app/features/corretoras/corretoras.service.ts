import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/config/api-base-url.token';
import { Corretora, CorretoraCreateRequest } from './models/corretora';

@Injectable({ providedIn: 'root' })
export class CorretorasService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${inject(API_BASE_URL)}/corretoras`;

  listar(): Observable<Corretora[]> {
    return this.http.get<Corretora[]>(this.endpoint);
  }

  buscarPorId(id: number): Observable<Corretora> {
    return this.http.get<Corretora>(`${this.endpoint}/${encodeURIComponent(String(id))}`);
  }

  buscarPorCnpj(cnpj: string): Observable<Corretora> {
    return this.http.get<Corretora>(`${this.endpoint}/por-cnpj`, {
      params: new HttpParams().set('cnpj', cnpj)
    });
  }

  cadastrar(request: CorretoraCreateRequest): Observable<Corretora> {
    return this.http.post<Corretora>(this.endpoint, request);
  }
}
