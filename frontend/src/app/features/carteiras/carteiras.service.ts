import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/config/api-base-url.token';
import { CarteiraCreateRequest, CarteiraResponse, CarteiraUpdateRequest } from './models/carteira';

@Injectable({ providedIn: 'root' })
export class CarteirasService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${inject(API_BASE_URL)}/carteiras`;

  listar(): Observable<CarteiraResponse[]> {
    return this.http.get<CarteiraResponse[]>(this.endpoint);
  }

  buscarPorId(id: number): Observable<CarteiraResponse> {
    return this.http.get<CarteiraResponse>(`${this.endpoint}/${encodeURIComponent(String(id))}`);
  }

  cadastrar(request: CarteiraCreateRequest): Observable<CarteiraResponse> {
    return this.http.post<CarteiraResponse>(this.endpoint, request);
  }

  atualizar(id: number, request: CarteiraUpdateRequest): Observable<CarteiraResponse> {
    return this.http.patch<CarteiraResponse>(`${this.endpoint}/${encodeURIComponent(String(id))}`, request);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${encodeURIComponent(String(id))}`);
  }
}
