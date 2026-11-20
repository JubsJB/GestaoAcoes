import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, defer, map, Observable, throwError } from 'rxjs';
import { API_BASE_URL } from '../../core/config/api-base-url.token';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { normalizeHttpError } from '../../core/http/http-error-normalizer';
import { CadastroCustodia, DetalheCustodia, EstornoCustodiaRequest, FiltrosCustodia,
  MovimentacaoCustodia, RegistroCustodia } from './models/custodia';
import { dataCivilCustodia, idCustodia, lerCustodia, parseDetalheCustodia,
  parseHistoricoCustodia, parseRegistroCustodia, quantidadeCustodia } from './custodia.contract';

@Injectable({ providedIn: 'root' })
export class CustodiaService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${inject(API_BASE_URL)}/carteiras`;

  cadastrar(carteiraId: number, entrada: CadastroCustodia): Observable<MovimentacaoCustodia> {
    return this.executar(() => {
      const path = this.path(carteiraId);
      permitidos(entrada, ['natureza', 'ticker', 'mercado', 'corretoraOrigemId', 'corretoraDestinoId', 'quantidade', 'dataEfetiva']);
      if (!entrada || !['TRANSFERENCIA', 'ATRIBUICAO'].includes(entrada.natureza)
          || typeof entrada.ticker !== 'string' || !entrada.ticker.trim()
          || !idCustodia(entrada.corretoraDestinoId) || !quantidadeCustodia(entrada.quantidade, entrada.mercado)
          || !dataCivilCustodia(entrada.dataEfetiva)
          || (entrada.natureza === 'TRANSFERENCIA'
            ? !idCustodia(entrada.corretoraOrigemId) || entrada.corretoraOrigemId === entrada.corretoraDestinoId
            : entrada.corretoraOrigemId != null)) throw new Error('Cadastro de custódia inválido.');
      const body = { natureza: entrada.natureza, ticker: entrada.ticker, mercado: entrada.mercado,
        corretoraDestinoId: entrada.corretoraDestinoId, quantidade: entrada.quantidade, dataEfetiva: entrada.dataEfetiva,
        ...(entrada.corretoraOrigemId !== undefined ? { corretoraOrigemId: entrada.corretoraOrigemId } : {}) };
      return this.http.post(path, body, { responseType: 'text' }).pipe(map(text => {
        const v = parseRegistroCustodia(text, carteiraId);
        if (v.tipoRegistro !== 'MOVIMENTACAO' || v.natureza !== entrada.natureza || v.situacao !== 'ATIVA')
          throw new Error('Resposta de cadastro inválida.');
        return v;
      }));
    });
  }
  historico(carteiraId: number, filtros: FiltrosCustodia = {}): Observable<RegistroCustodia[]> {
    return this.executar(() => {
      const path = this.path(carteiraId);
      permitidos(filtros, ['natureza', 'situacao']);
      let params = new HttpParams();
      if (filtros.natureza !== undefined) {
        if (!['TRANSFERENCIA', 'ATRIBUICAO', 'ESTORNO'].includes(filtros.natureza)) throw new Error('Natureza inválida.');
        params = params.set('natureza', filtros.natureza);
      }
      if (filtros.situacao !== undefined) {
        if (!['ATIVA', 'ESTORNADA'].includes(filtros.situacao)) throw new Error('Situação inválida.');
        params = params.set('situacao', filtros.situacao);
      }
      return this.http.get(path, { params, responseType: 'text' }).pipe(map(text => parseHistoricoCustodia(text, carteiraId)));
    });
  }
  detalhe(carteiraId: number, movimentacaoId: number): Observable<DetalheCustodia> {
    return this.executar(() => this.http.get(this.path(carteiraId, movimentacaoId), { responseType: 'text' })
      .pipe(map(text => parseDetalheCustodia(text, carteiraId, movimentacaoId))));
  }
  estornar(carteiraId: number, movimentacaoId: number, entrada?: EstornoCustodiaRequest): Observable<DetalheCustodia> {
    return this.executar(() => {
      const path = this.path(carteiraId, movimentacaoId);
      if (entrada !== undefined) {
        permitidos(entrada, ['motivo']);
        if (entrada.motivo !== undefined && entrada.motivo !== null
            && (typeof entrada.motivo !== 'string' || entrada.motivo.length > 1000)) throw new Error('Motivo inválido.');
      }
      const body = entrada === undefined ? null : entrada.motivo === undefined ? {} : { motivo: entrada.motivo };
      return this.http.post(`${path}/estornos`, body, { responseType: 'text' }).pipe(map(text => {
        const v = parseDetalheCustodia(text, carteiraId, movimentacaoId);
        if (!v.estorno) throw new Error('Estorno não confirmado na resposta.');
        return v;
      }));
    });
  }
  private path(carteiraId: number, movimentacaoId?: number): string {
    if (!idCustodia(carteiraId) || movimentacaoId !== undefined && !idCustodia(movimentacaoId)) throw new Error('Identificação inválida.');
    return `${this.endpoint}/${carteiraId}/movimentacoes-custodia${movimentacaoId === undefined ? '' : `/${movimentacaoId}`}`;
  }
  private executar<T>(request: () => Observable<T>): Observable<T> {
    // Sem retry, GET após POST ou mutação de telas: o consumidor controla o próximo passo.
    return defer(request).pipe(catchError((error: unknown) => throwError(() => erroCustodia(error))));
  }
}

function permitidos(value: unknown, keys: string[]): void {
  if (value === null || typeof value !== 'object' || Array.isArray(value)
      || Object.keys(value).some(k => !keys.includes(k))) throw new Error('Campo não permitido.');
}
function erroCustodia(error: unknown): NormalizedHttpError {
  const normalizado = error as NormalizedHttpError;
  const original = error instanceof HttpErrorResponse ? error : normalizado?.originalError;
  if (original instanceof HttpErrorResponse) {
    if (typeof original.error === 'string') {
      try {
        const body = lerCustodia(original.error, false);
        return { ...normalizeHttpError(new HttpErrorResponse({ error: body, status: original.status,
          statusText: original.statusText, url: original.url ?? undefined })), originalError: original };
      } catch { return normalizeHttpError(new HttpErrorResponse({ status: original.status, error: null })); }
    }
    return normalizado?.kind ? normalizado : normalizeHttpError(original);
  }
  return normalizeHttpError(new HttpErrorResponse({ status: 0, error: null, statusText: 'Contrato de custódia inválido.' }));
}
