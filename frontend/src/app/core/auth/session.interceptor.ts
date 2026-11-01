import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, EMPTY, filter, takeUntil, throwError } from 'rxjs';
import { API_BASE_URL } from '../config/api-base-url.token';
import { SessionService } from './session.service';
import { AuthApi } from './auth-api.service';

export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  const base = inject(API_BASE_URL);
  const target = new URL(request.url, globalThis.location.origin);
  const api = new URL(base, globalThis.location.origin);
  if (target.origin !== api.origin || !(target.pathname === api.pathname || target.pathname.startsWith(`${api.pathname}/`))) {
    return next(request);
  }
  const session = inject(SessionService);
  const auth = inject(AuthApi);
  const generation = session.generation;
  const publicPaths = ['/auth/csrf', '/auth/login', '/auth/me', '/auth/redefinicoes/confirmar'];
  const publicRequest = publicPaths.some(path => target.pathname === `${api.pathname}${path}`);
  const response = next(request);
  const scoped = !publicRequest && ['GET', 'HEAD'].includes(request.method) ? response.pipe(takeUntil(session.changes$)) : response;
  return scoped.pipe(
    filter(() => session.generation === generation),
    catchError(error => {
      if (session.generation !== generation) return EMPTY;
      if (!publicRequest && error instanceof HttpErrorResponse && error.status === 401) session.end();
      if (!publicRequest && error instanceof HttpErrorResponse && error.status === 503 && securityCode(error.error) === 'SEGURANCA_INDISPONIVEL') session.unavailable();
      if (error instanceof HttpErrorResponse && error.status === 403 && securityCode(error.error) === 'CSRF_INVALIDO') {
        auth.csrf().subscribe({ error: () => { /* A próxima submissão continua explícita. */ } });
      }
      return throwError(() => error);
    })
  );
};

function securityCode(body: unknown): unknown {
  if (typeof body === 'string') {
    try { body = JSON.parse(body); } catch { return null; }
  }
  return body && typeof body === 'object' && 'code' in body ? body.code : null;
}
