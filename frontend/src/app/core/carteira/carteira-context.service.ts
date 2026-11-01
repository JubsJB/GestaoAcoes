import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, InjectionToken } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BehaviorSubject, catchError, finalize, map, Observable, of, shareReplay, Subject, takeUntil, tap } from 'rxjs';
import { CarteirasService } from '../../features/carteiras/carteiras.service';
import { CarteiraResponse } from '../../features/carteiras/models/carteira';
import { NormalizedHttpError } from '../errors/normalized-http-error';
import { normalizeHttpError } from '../http/http-error-normalizer';

export const CARTEIRA_STORAGE_KEY = 'gestaoacoes.carteira-ativa.v1';
export const CARTEIRA_STORAGE = new InjectionToken<Pick<Storage, 'getItem' | 'setItem' | 'removeItem'> | null>(
  'Carteira preference storage', { providedIn: 'root', factory: () => {
    try { return globalThis.localStorage; } catch { return null; }
  } }
);

export function carteiraId(value: string | null): number | null {
  if (value === null || !/^[1-9]\d*$/.test(value)) return null;
  const id = Number(value);
  return Number.isSafeInteger(id) ? id : null;
}

interface ContextState {
  items: readonly CarteiraResponse[];
  activeId: number | null;
  status: 'idle' | 'loading' | 'ready' | 'error';
  error: NormalizedHttpError | null;
  invalidSelection: boolean;
}

@Injectable({ providedIn: 'root' })
export class CarteiraContextService {
  private readonly service = inject(CarteirasService);
  private readonly storage = inject(CARTEIRA_STORAGE);
  private readonly subject = new BehaviorSubject<ContextState>({ items: [], activeId: null, status: 'idle', error: null, invalidSelection: false });
  readonly state$ = this.subject.asObservable();
  private readonly state = toSignal(this.state$, { requireSync: true });
  readonly carteiras = computed(() => this.state().items);
  readonly activeId = computed(() => this.state().activeId);
  readonly active = computed(() => this.carteiras().find(item => item.id === this.activeId()) ?? null);
  readonly loading = computed(() => this.state().status === 'idle' || this.state().status === 'loading');
  readonly error = computed(() => this.state().error);
  readonly invalidSelection = computed(() => this.state().invalidSelection);
  private memoryId: number | null = null;
  private preferenceId: number | null = null;
  private requestedUrl: string | null = null;
  private explicitUrl = true;
  private pending?: Observable<boolean>;
  private epoch = 0;
  private readonly cancelled = new Subject<void>();
  // Confirmed mutations overlay any older collection request still in flight.
  private readonly mutations = new Map<number, CarteiraResponse | null>();

  constructor() {
    try { this.storage?.removeItem(CARTEIRA_STORAGE_KEY); } catch { /* Preferência legada nunca será lida. */ }
  }

  initialize(retry = false): Observable<boolean> {
    if (this.pending) return this.pending;
    if (!retry && this.state().status === 'ready') return of(true);
    if (!retry && this.state().status === 'error') return of(false);
    this.publish({ status: 'loading', error: null, activeId: null });
    const epoch = this.epoch;
    const request = this.service.listar().pipe(
      takeUntil(this.cancelled),
      tap(items => {
        if (!Array.isArray(items) || items.some(item => !item || typeof item.id !== 'number' || carteiraId(String(item.id)) === null || typeof item.nome !== 'string' || typeof item.dataCriacao !== 'string') || new Set(items.map(item => item.id)).size !== items.length) {
          throw new HttpErrorResponse({ status: 0, statusText: 'Resposta de carteiras inválida' });
        }
        const collection = new Map(items.map(item => [item.id, this.identity(item)]));
        this.mutations.forEach((item, id) => item ? collection.set(id, item) : collection.delete(id));
        this.mutations.clear();
        this.publish({ items: [...collection.values()], status: 'ready', error: null });
        this.resolve();
      }),
      map(() => true),
      catchError((error: NormalizedHttpError | HttpErrorResponse) => {
        this.publish({ status: 'error', activeId: null, error: error instanceof HttpErrorResponse ? normalizeHttpError(error) : error });
        return of(false);
      }),
      finalize(() => { if (epoch === this.epoch) this.pending = undefined; }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    this.pending = request;
    return request;
  }

  reset(): void {
    this.epoch++;
    this.cancelled.next();
    this.pending = undefined;
    this.mutations.clear();
    this.memoryId = null;
    this.requestedUrl = null;
    this.explicitUrl = true;
    this.persist(null);
    this.subject.next({ items: [], activeId: null, status: 'idle', error: null, invalidSelection: false });
  }

  resolveUrl(value: string | null, explicit = true): void {
    this.requestedUrl = value;
    this.explicitUrl = explicit;
    this.resolve();
  }

  select(id: number): boolean {
    if (this.state().status !== 'ready' || !this.carteiras().some(item => item.id === id)) return false;
    this.requestedUrl = null;
    this.memoryId = id;
    this.persist(id);
    this.publish({ activeId: id, invalidSelection: false });
    return true;
  }

  upsert(item: CarteiraResponse): void {
    item = this.identity(item);
    this.mutations.set(item.id, item);
    const items = [...this.carteiras()];
    const index = items.findIndex(current => current.id === item.id);
    if (index < 0) items.push(item); else items[index] = item;
    this.publish({ items });
    if (this.state().status === 'ready') this.resolve();
  }

  remove(id: number): void {
    this.mutations.set(id, null);
    if (this.readPreference() === id) this.persist(null);
    if (this.memoryId === id) this.memoryId = null;
    if (carteiraId(this.requestedUrl) === id) this.requestedUrl = null;
    this.publish({ items: this.carteiras().filter(item => item.id !== id) });
    if (this.state().status === 'ready') this.resolve();
  }

  private resolve(): void {
    if (this.state().status !== 'ready') return;
    const valid = (id: number | null) => id !== null && this.carteiras().some(item => item.id === id);
    if (this.requestedUrl !== null) {
      const id = carteiraId(this.requestedUrl);
      if (!valid(id)) { this.publish({ activeId: null, invalidSelection: true }); return; }
      this.memoryId = id;
      if (this.explicitUrl) this.persist(id);
      this.publish({ activeId: id, invalidSelection: false });
      return;
    }
    const preference = this.readPreference();
    if (preference !== null && !valid(preference)) this.persist(null);
    const first = this.carteiras().reduce<number | null>((id, item) => id === null || item.id < id ? item.id : id, null);
    const id = valid(this.memoryId) ? this.memoryId : valid(preference) ? preference : first;
    this.memoryId = id;
    this.publish({ activeId: id, invalidSelection: false });
  }

  private readPreference(): number | null {
    return this.preferenceId;
  }
  private identity(item: CarteiraResponse): CarteiraResponse {
    return { id: item.id, nome: item.nome, dataCriacao: item.dataCriacao };
  }
  private persist(id: number | null): void {
    this.preferenceId = id;
    try {
      this.storage?.removeItem(CARTEIRA_STORAGE_KEY);
    } catch { /* Selection remains available in memory. */ }
  }
  private publish(patch: Partial<ContextState>): void { this.subject.next({ ...this.subject.value, ...patch }); }
}
