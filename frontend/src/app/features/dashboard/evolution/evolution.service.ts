import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api-base-url.token';
import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { isStandardError } from '../../../core/http/http-error-normalizer';
import { parseLosslessJson } from '../../../core/http/lossless-json';
import {
  EvolucaoPatrimonialResponse,
  isEvolucaoPatrimonialResponse,
  isSnapshotCarteiraResponse,
  SnapshotCarteiraResponse
} from './evolution.models';

const DECIMAL_FIELDS = new Set(['patrimonioAtual']);

@Injectable({ providedIn: 'root' })
export class EvolutionService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${inject(API_BASE_URL)}/carteiras`;

  consultar(carteiraId: number): Observable<EvolucaoPatrimonialResponse> {
    return this.request(
      `${this.portfolioUrl(carteiraId)}/evolucao-patrimonial`,
      text => parseAndValidate(text, isEvolucaoPatrimonialResponse)
    );
  }

  registrarSnapshot(carteiraId: number): Observable<SnapshotCarteiraResponse> {
    return this.http.post(`${this.portfolioUrl(carteiraId)}/snapshots`, null, { responseType: 'text' }).pipe(
      map(text => parseAndValidate(text, isSnapshotCarteiraResponse)),
      catchError(error => throwError(() => normalizeLosslessError(error)))
    );
  }

  private request<T>(url: string, parse: (text: string) => T): Observable<T> {
    return this.http.get(url, { responseType: 'text' }).pipe(
      map(parse),
      catchError(error => throwError(() => normalizeLosslessError(error)))
    );
  }

  private portfolioUrl(carteiraId: number): string {
    return `${this.endpoint}/${encodeURIComponent(String(carteiraId))}`;
  }
}

function parseAndValidate<T>(text: string, guard: (value: unknown) => value is T): T {
  const value = parseLosslessJson<unknown>(text, DECIMAL_FIELDS);
  if (!guard(value)) throw new Error('Resposta de evolução patrimonial inválida.');
  return value;
}

function normalizeLosslessError(error: unknown): unknown {
  const normalized = error as NormalizedHttpError;
  const original = normalized?.originalError;
  if (original && typeof original.error === 'string') {
    try {
      const standard = parseLosslessJson<unknown>(original.error, DECIMAL_FIELDS);
      if (isStandardError(standard)) {
        return {
          kind: 'standard', status: standard.status, code: standard.code, message: standard.message,
          details: standard.details, standardError: standard, originalError: original
        } satisfies NormalizedHttpError;
      }
    } catch { /* preserva a classificação recebida */ }
  }
  if (normalized?.kind) return error;
  return {
    kind: 'technical', status: 0, code: null, message: 'Resposta de evolução patrimonial inválida.',
    details: {}, standardError: null, originalError: error
  } as unknown as NormalizedHttpError;
}
