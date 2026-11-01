import { inject, Injectable, InjectionToken, Injector, signal } from '@angular/core';
import { Router } from '@angular/router';
import { map, Observable, of } from 'rxjs';
import { carteiraId, CarteiraContextService } from '../carteira/carteira-context.service';
import { requiresAdminPath } from './route-access';

export const RETURN_STORAGE = new InjectionToken<Pick<Storage, 'getItem' | 'setItem' | 'removeItem'> | null>('Return navigation storage', {
  providedIn: 'root', factory: () => { try { return globalThis.sessionStorage; } catch { return null; } }
});
const KEY = 'gestaoacoes.retorno.v1';
interface Intent { url: string; identity: number | null; expires: number; }

@Injectable({ providedIn: 'root' })
export class ReturnNavigationService {
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly storage = inject(RETURN_STORAGE);
  private memory: Intent | null = null;
  readonly notice = signal('');

  mayResume(identity: number): boolean {
    const intent = this.read();
    if (!intent || !Number.isFinite(intent.expires) || Date.now() >= intent.expires || intent.expires > Date.now() + 60 * 60 * 1000) return true;
    return intent.identity === null || intent.identity === identity;
  }

  destination(identity: number, papel: 'ADMIN' | 'USER'): Observable<string> {
    const url = this.consume(identity, papel);
    const tree = this.router.parseUrl(url);
    const segments = tree.root.children['primary']?.segments ?? [];
    const wallet = segments[0]?.path === 'carteiras' ? carteiraId(segments[1]?.path ?? null) : carteiraId(tree.queryParams['carteiraId'] ?? null);
    if (wallet === null) return of(url);
    const context = this.injector.get(CarteiraContextService);
    return context.initialize().pipe(map(valid => {
      if (valid && context.carteiras().some(item => item.id === wallet)) return url;
      this.notice.set('A carteira solicitada não está disponível. Escolha um destino de consulta.');
      return '/dashboard';
    }));
  }

  capture(url: string, identity: number | null): void {
    const safe = this.canonical(url);
    if (safe === null) { this.clear(); this.notice.set('O destino solicitado não é válido. Escolha uma página pelo menu.'); return; }
    const previous = this.read();
    this.memory = { url: safe, identity: identity ?? (previous && previous.expires > Date.now() ? previous.identity : null), expires: Date.now() + 60 * 60 * 1000 };
    try { this.storage?.setItem(KEY, JSON.stringify(this.memory)); } catch { /* Continuidade em memória. */ }
  }

  consume(identity: number, papel: 'ADMIN' | 'USER'): string {
    this.notice.set('');
    const intent = this.read();
    this.clear();
    if (!intent || !Number.isFinite(intent.expires) || Date.now() >= intent.expires || intent.expires > Date.now() + 60 * 60 * 1000) return '/dashboard';
    if (intent?.identity !== null && intent?.identity !== identity) return '/dashboard';
    const destination = intent && typeof intent.url === 'string' ? this.canonical(intent.url) ?? '/dashboard' : '/dashboard';
    const path = this.router.parseUrl(destination).root.children['primary']?.segments.map(segment => segment.path).join('/') ?? '';
    if (papel !== 'ADMIN' && requiresAdminPath('/' + path)) {
      this.notice.set('Você não possui permissão para retornar à página solicitada.');
      return '/dashboard';
    }
    return destination;
  }

  clear(): void {
    this.memory = null;
    try { this.storage?.removeItem(KEY); } catch { /* Armazenamento indisponível. */ }
  }

  private read(): Intent | null {
    try { return JSON.parse(this.storage?.getItem(KEY) ?? 'null') ?? this.memory; } catch { return this.memory; }
  }

  private canonical(url: string): string | null {
    if (!url.startsWith('/') || url.startsWith('//') || url.includes('\\')) return null;
    try {
      const tree = this.router.parseUrl(url);
      const primary = tree.root.children['primary'];
      if (!primary || Object.keys(tree.root.children).some(key => key !== 'primary') || primary.children && Object.keys(primary.children).length) return null;
      if (primary.segments.some(segment => Object.keys(segment.parameters).length)) return null;
      const path = '/' + primary.segments.map(segment => segment.path).join('/');
      if (!/^\/(?:dashboard|carteiras(?:\/(?:nova|[1-9]\d*(?:\/editar)?))?|(?:acoes|corretoras|operacoes)(?:\/(?:[1-9]\d*|nova))?|usuarios(?:\/(?:[1-9]\d*|novo))?)$/.test(path)) return null;
      if (primary.segments.some(segment => /^\d+$/.test(segment.path) && !Number.isSafeInteger(Number(segment.path)))) return null;
      const query: Record<string, string> = {};
      const id = tree.queryParams['carteiraId'];
      if (id !== undefined && (typeof id !== 'string' || !/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(Number(id)))) return null;
      if ((path === '/dashboard' || path.startsWith('/operacoes')) && typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))) query['carteiraId'] = id;
      const origin = tree.queryParams['origem'];
      if (origin !== undefined && (typeof origin !== 'string' || !['dashboard', 'carteira', 'operacoes'].includes(origin))) return null;
      if (/^\/operacoes\/(?:[1-9]\d*|nova)$/.test(path) && typeof origin === 'string' && ['dashboard', 'carteira', 'operacoes'].includes(origin)) query['origem'] = origin;
      return this.router.serializeUrl(this.router.createUrlTree([path], { queryParams: query, fragment: tree.fragment === 'main-content' ? tree.fragment : undefined }));
    } catch { return null; }
  }
}
