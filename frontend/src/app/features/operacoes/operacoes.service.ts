import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';

import { API_BASE_URL } from '../../core/config/api-base-url.token';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { isStandardError } from '../../core/http/http-error-normalizer';
import { parseOperationJson } from './lossless-operation-json';
import { Mercado } from '../acoes/models/acao';
import { OperacaoCreateRequest, OperacaoResponse, PreviaPrecoCompraResponse, SugestaoPrecoVendaResponse } from './models/operacao';

@Injectable({ providedIn: 'root' })
export class OperacoesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  private readonly endpoint = `${this.baseUrl}/operacoes`;

  cadastrar(request: OperacaoCreateRequest): Observable<OperacaoResponse> {
    return this.request<OperacaoResponse>(this.http.post(this.endpoint, request, { responseType: 'text' }));
  }

  listar(): Observable<OperacaoResponse[]> {
    return this.request<OperacaoResponse[]>(this.http.get(this.endpoint, { responseType: 'text' }));
  }

  buscarPorId(id: number): Observable<OperacaoResponse> {
    return this.request<OperacaoResponse>(this.http.get(`${this.endpoint}/${encodeURIComponent(String(id))}`, { responseType: 'text' }));
  }

  listarPorCarteira(carteiraId: number): Observable<OperacaoResponse[]> {
    return this.request<OperacaoResponse[]>(this.http.get(`${this.baseUrl}/carteiras/${encodeURIComponent(String(carteiraId))}/operacoes`, { responseType: 'text' }));
  }

  obterPreviaCompra(ticker: string, mercado: Mercado, dataOperacao: string): Observable<PreviaPrecoCompraResponse> {
    const params = new HttpParams().set('ticker', ticker).set('mercado', mercado).set('dataOperacao', dataOperacao);
    return this.request<PreviaPrecoCompraResponse>(this.http.get(`${this.endpoint}/previa-compra`, { params, responseType: 'text' }));
  }

  obterSugestaoPrecoVenda(carteiraId: number, ticker: string, mercado: Mercado, dataOperacao: string): Observable<SugestaoPrecoVendaResponse> {
    const params = new HttpParams().set('ticker', ticker).set('mercado', mercado).set('dataOperacao', dataOperacao);
    const carteira = encodeURIComponent(String(carteiraId));
    return this.request<SugestaoPrecoVendaResponse>(this.http.get(`${this.baseUrl}/carteiras/${carteira}/operacoes/sugestao-preco-venda`, { params, responseType: 'text' }));
  }

  private request<T>(source: Observable<string>): Observable<T> {
    return source.pipe(map((text) => parseOperationJson<T>(text)), catchError((error: unknown) => throwError(() => this.parseTextError(error))));
  }

  private parseTextError(error: unknown): unknown {
    const normalized = error as NormalizedHttpError;
    const original = normalized?.originalError;
    if (!original || typeof original.error !== 'string') return error;
    try {
      const standard = parseOperationJson<unknown>(original.error);
      if (!isStandardError(standard)) return error;
      return { kind: 'standard', status: standard.status, code: standard.code, message: standard.message, details: standard.details, standardError: standard, originalError: original } satisfies NormalizedHttpError;
    } catch { return error; }
  }
}
