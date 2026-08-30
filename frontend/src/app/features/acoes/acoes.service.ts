import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/config/api-base-url.token';
import { AcaoCreateRequest, AcaoResponse, Mercado } from './models/acao';

@Injectable({ providedIn: 'root' })
export class AcoesService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${inject(API_BASE_URL)}/acoes`;

  criar(request: AcaoCreateRequest): Observable<AcaoResponse> {
    return this.http.post<AcaoResponse>(this.endpoint, request);
  }

  listar(): Observable<AcaoResponse[]> {
    return this.http.get<AcaoResponse[]>(this.endpoint);
  }

  buscarPorId(id: number): Observable<AcaoResponse> {
    return this.http.get<AcaoResponse>(`${this.endpoint}/${encodeURIComponent(String(id))}`);
  }

  buscarPorTickerEMercado(ticker: string, mercado: Mercado): Observable<AcaoResponse> {
    return this.http.get<AcaoResponse>(`${this.endpoint}/por-ticker`, {
      params: new HttpParams().set('ticker', ticker).set('mercado', mercado)
    });
  }

  atualizarCotacao(id: number): Observable<AcaoResponse> {
    return this.http.patch<AcaoResponse>(`${this.endpoint}/${encodeURIComponent(String(id))}/cotacao`, null);
  }
}
