import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';

import { API_BASE_URL } from '../../core/config/api-base-url.token';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { isStandardError } from '../../core/http/http-error-normalizer';
import { parseLosslessJson } from '../../core/http/lossless-json';
import { PosicaoResponse, ResultadoRealizadoResponse, ResumoCarteiraResponse } from './models/dashboard';

const DECIMAL_FIELDS = new Set([
  'custoTotalPosicoes', 'patrimonioAtual', 'resultadoNaoRealizadoTotal', 'rentabilidadePercentual',
  'quantidadeAtual', 'precoMedio', 'custoPosicao', 'cotacaoAtual', 'valorAtualPosicao',
  'resultadoNaoRealizado', 'resultadoRealizado'
]);
const DECIMAL = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?$/;

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${inject(API_BASE_URL)}/carteiras`;

  obterResumo(carteiraId: number): Observable<ResumoCarteiraResponse> {
    return this.request(`${this.url(carteiraId)}/resumo`, isResumo);
  }

  listarPosicoes(carteiraId: number): Observable<PosicaoResponse[]> {
    return this.request(`${this.url(carteiraId)}/posicoes`, isPosicoes);
  }

  listarResultadosRealizados(carteiraId: number): Observable<ResultadoRealizadoResponse[]> {
    return this.request(`${this.url(carteiraId)}/resultados-realizados`, isResultados);
  }

  private url(carteiraId: number): string {
    return `${this.endpoint}/${encodeURIComponent(String(carteiraId))}`;
  }

  private request<T>(url: string, guard: (value: unknown) => value is T): Observable<T> {
    return this.http.get(url, { responseType: 'text' }).pipe(
      map(text => {
        const value = parseLosslessJson<unknown>(text, DECIMAL_FIELDS);
        if (!guard(value)) throw new Error('Resposta financeira inválida.');
        return value;
      }),
      catchError((error: unknown) => throwError(() => normalizeServiceError(error)))
    );
  }
}

function normalizeServiceError(error: unknown): unknown {
  const normalized = error as NormalizedHttpError;
  const original = normalized?.originalError;
  if (original && typeof original.error === 'string') {
    try {
      const standard = parseLosslessJson<unknown>(original.error, DECIMAL_FIELDS);
      if (isStandardError(standard)) {
        return { kind: 'standard', status: standard.status, code: standard.code, message: standard.message, details: standard.details, standardError: standard, originalError: original } satisfies NormalizedHttpError;
      }
    } catch { /* mantém a classificação original */ }
  }
  if (normalized?.kind) return error;
  return { kind: 'technical', status: 0, code: null, message: 'Resposta financeira inválida.', details: {}, standardError: null, originalError: error } as unknown as NormalizedHttpError;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
function isId(value: unknown): value is number { return typeof value === 'number' && Number.isSafeInteger(value) && value > 0; }
function isText(value: unknown): value is string { return typeof value === 'string'; }
function isDecimal(value: unknown): value is string { return typeof value === 'string' && DECIMAL.test(value); }
function isCurrency(value: unknown): value is 'BRL' | 'USD' { return value === 'BRL' || value === 'USD'; }
function isMarket(value: unknown): value is 'BRASIL' | 'EUA' { return value === 'BRASIL' || value === 'EUA'; }

function isResumoItem(value: unknown): boolean {
  return isRecord(value) && isCurrency(value['moeda']) && isDecimal(value['custoTotalPosicoes'])
    && isDecimal(value['patrimonioAtual']) && isDecimal(value['resultadoNaoRealizadoTotal'])
    && isDecimal(value['rentabilidadePercentual']);
}
function isResumo(value: unknown): value is ResumoCarteiraResponse {
  return isRecord(value) && isId(value['carteiraId']) && Array.isArray(value['resumos']) && value['resumos'].every(isResumoItem);
}
function isPosicao(value: unknown): boolean {
  return isRecord(value) && isId(value['acaoId']) && isText(value['ticker']) && isText(value['nomeEmpresa'])
    && isMarket(value['mercado']) && isCurrency(value['moeda']) && isDecimal(value['quantidadeAtual'])
    && isDecimal(value['precoMedio']) && isDecimal(value['custoPosicao']) && isDecimal(value['cotacaoAtual'])
    && isText(value['dataHoraCotacao']) && isDecimal(value['valorAtualPosicao'])
    && isDecimal(value['resultadoNaoRealizado']) && isDecimal(value['rentabilidadePercentual']);
}
function isPosicoes(value: unknown): value is PosicaoResponse[] { return Array.isArray(value) && value.every(isPosicao); }
function isResultado(value: unknown): boolean {
  return isRecord(value) && isId(value['acaoId']) && isText(value['ticker']) && isText(value['nomeEmpresa'])
    && isMarket(value['mercado']) && isCurrency(value['moeda']) && isDecimal(value['resultadoRealizado']);
}
function isResultados(value: unknown): value is ResultadoRealizadoResponse[] { return Array.isArray(value) && value.every(isResultado); }
