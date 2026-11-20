import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';
import { API_BASE_URL } from '../../core/config/api-base-url.token';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { normalizeHttpError } from '../../core/http/http-error-normalizer';
import { parseLosslessJson } from '../../core/http/lossless-json';
import { DistribuicaoCarteiraResponse } from './models/distribuicao-corretoras';
import { parseDistribuicaoCorretoras } from './distribuicao-corretoras.contract';

@Injectable({ providedIn: 'root' })
export class DistribuicaoCorretorasService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${inject(API_BASE_URL)}/carteiras`;

  consultar(carteiraId: number): Observable<DistribuicaoCarteiraResponse> {
    return this.http.get(`${this.endpoint}/${encodeURIComponent(String(carteiraId))}/posicoes/distribuicao-corretoras`,
      { responseType: 'text' }).pipe(
      map(text => {
        const value = parseDistribuicaoCorretoras(text);
        if (value.carteiraId !== carteiraId) throw new Error('Contexto incorreto.');
        return value;
      }),
      catchError((error: unknown) => throwError(() => normalizeError(error)))
    );
  }
}

function normalizeError(error: unknown): NormalizedHttpError {
  const normalized = error as NormalizedHttpError;
  const original = error instanceof HttpErrorResponse ? error : normalized?.originalError;
  if (original instanceof HttpErrorResponse) {
    if (typeof original.error === 'string') {
      try {
        const body = parseLosslessJson<unknown>(original.error, new Set(['quantidadeConsolidada',
          'quantidadeAtribuidaTotal', 'quantidadeLiquida', 'quantidadeFinal', 'saldoAposEvento',
          'quantidadeDisponivel', 'quantidadeSolicitada']));
        return { ...normalizeHttpError(new HttpErrorResponse({ error: body, status: original.status,
          statusText: original.statusText, url: original.url ?? undefined })), originalError: original };
      }
      catch { /* Preserve the HTTP classification when the body is not JSON. */ }
    }
    return normalized?.kind ? normalized : normalizeHttpError(original);
  }
  return normalizeHttpError(new HttpErrorResponse({ error: null, status: 0,
    statusText: 'Resposta de distribuicao invalida.' }));
}
