import { computed, inject, Injectable, Injector, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, shareReplay, Subject, switchMap, tap, throwError } from 'rxjs';
import { AuthApi, Sessao } from './auth-api.service';
import { SessionSignal } from './auth-browser';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly injector = inject(Injector);
  private get api(): AuthApi { return this.injector.get(AuthApi); }
  private readonly current = signal<Sessao | null>(null);
  readonly identity = this.current.asReadonly();
  private readonly clockAnchor = signal(0);
  readonly deadlines = computed(() => {
    const identity = this.current();
    if (!identity) return null;
    const server = Date.parse(identity.horarioServidor);
    const idle = Date.parse(identity.inativaEm);
    const absolute = Date.parse(identity.expiraEm);
    if (![server, idle, absolute].every(Number.isFinite)) return null;
    return { idle: this.clockAnchor() + idle - server, absolute: this.clockAnchor() + absolute - server };
  });
  private readonly phase = signal<'unknown' | 'checking' | 'authenticated' | 'anonymous' | 'unavailable'>('unknown');
  readonly status = this.phase.asReadonly();
  readonly authenticated = computed(() => this.phase() === 'authenticated' && this.current() !== null);
  readonly admin = computed(() => this.authenticated() && this.current()?.papel === 'ADMIN');
  private pending?: Observable<boolean>;
  private epoch = 0;
  private lastId: number | null = null;
  private lastRole: Sessao['papel'] | null = null;
  private lastExpiry: string | null = null;
  private readonly epochState = signal(0);
  readonly generationState = this.epochState.asReadonly();
  private readonly changes = new Subject<void>();
  readonly changes$ = this.changes.asObservable();
  private readonly signals = new Subject<SessionSignal>();
  readonly signals$ = this.signals.asObservable();
  get generation(): number { return this.epoch; }
  get previousIdentity(): number | null { return this.lastId; }

  private advance(): void {
    this.epoch++;
    this.epochState.set(this.epoch);
    this.changes.next();
  }

  private acceptMetadata(identity: Sessao, started: number): void {
    const current = this.current();
    if (current && current.id === identity.id && current.papel === identity.papel && current.expiraEm === identity.expiraEm) {
      const activity = Date.parse(identity.ultimaAtividadeEm);
      const previous = Date.parse(current.ultimaAtividadeEm);
      if (activity < previous || (activity === previous && Date.parse(identity.horarioServidor) < Date.parse(current.horarioServidor))) return;
    }
    this.clockAnchor.set(started);
    this.current.set(identity);
  }

  end(): void {
    this.advance();
    this.pending = undefined;
    this.current.set(null);
    this.phase.set('anonymous');
  }

  unavailable(): void {
    this.advance();
    this.pending = undefined;
    this.current.set(null);
    this.phase.set('unavailable');
  }

  passwordConfirmed(): void { this.end(); this.signals.next('SESSION_CHANGED'); }

  login(username: string, senha: string): Observable<boolean> {
    this.end();
    const generation = this.epoch;
    return this.api.login(username, senha).pipe(
      switchMap(() => generation === this.epoch ? this.restore() : of(false)),
      tap(valid => { if (valid && this.authenticated()) this.signals.next('SESSION_CHANGED'); })
    );
  }

  logout(): Observable<void> {
    const generation = this.epoch;
    return this.api.logout().pipe(
      tap(() => {
        if (generation === this.epoch) { this.end(); this.signals.next('SESSION_ENDED'); }
      }),
      switchMap(() => this.api.csrf()),
      finalize(() => { if (generation === this.epoch) this.end(); })
    );
  }

  activity(): Observable<boolean> {
    if (!this.authenticated()) return of(false);
    const generation = this.epoch;
    const started = performance.now();
    return this.api.activity().pipe(
      map(identity => {
        if (generation !== this.epoch) return false;
        const current = this.current();
        if (!current || identity.id !== current.id || identity.papel !== current.papel || identity.expiraEm !== current.expiraEm) {
          this.unavailable(); return false;
        }
        this.acceptMetadata(identity, started);
        this.signals.next('ACTIVITY_UPDATED');
        return true;
      }),
      catchError((error: { status?: number }) => {
        if (generation === this.epoch) {
          if (error.status === 401) this.end();
          else if (error.status === 503 || error.status === 0) this.unavailable();
        }
        return throwError(() => error);
      })
    );
  }

  restore(): Observable<boolean> {
    if (this.pending) return this.pending;
    const generation = this.epoch;
    const started = performance.now();
    if (!this.authenticated()) this.phase.set('checking');
    const request: Observable<boolean> = this.api.me().pipe(
      switchMap(identity => generation === this.epoch ? this.api.csrf().pipe(map(() => identity)) : of(null)),
      map(identity => {
        if (!identity || generation !== this.epoch) return false;
        if (this.lastId !== null && (this.lastId !== identity.id || this.lastRole !== identity.papel || this.lastExpiry !== identity.expiraEm)) this.advance();
        this.lastId = identity.id;
        this.lastRole = identity.papel;
        this.lastExpiry = identity.expiraEm;
        this.acceptMetadata(identity, started); this.phase.set('authenticated');
        return true;
      }),
      catchError((error: { status?: number }) => {
        if (generation !== this.epoch) return of(false);
        if (error.status === 401) this.end(); else this.unavailable();
        return of(false);
      }),
      finalize(() => { if (this.pending === request) this.pending = undefined; }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.pending = request;
    return request;
  }
}
