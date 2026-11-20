import { DestroyRef, effect, inject, Injectable, Injector, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, Subscription } from 'rxjs';
import { NavigationEnd, Router } from '@angular/router';
import { AUTH_BROWSER, AuthBrowser, SessionSignal } from './auth-browser';
import { SessionService } from './session.service';
import { publicRoute, SessionBoundary } from './session-boundary.service';

@Injectable({ providedIn: 'root' })
export class SessionCoordinator {
  private readonly browser = inject(AUTH_BROWSER);
  private readonly session = inject(SessionService);
  private readonly boundary = inject(SessionBoundary);
  private readonly destroy = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly router = inject(Router);
  private readonly routeState = signal('/');
  private started = false;
  private alive = true;
  private flight?: Subscription;
  private pending = false;
  private checking?: Subscription;
  private checkId = 0;
  private keyTarget: EventTarget | null = null;
  private pointerTarget: EventTarget | null = null;
  private activationTarget: EventTarget | null = null;
  private inputChanges = new WeakSet<EventTarget>();
  private removers: (() => void)[] = [];
  private polling?: ReturnType<typeof setInterval>;
  private deadlineTimer?: ReturnType<typeof setTimeout>;
  private readonly maskState = signal(false);
  readonly masked = this.maskState.asReadonly();
  private channel?: ReturnType<AuthBrowser['connect']>;

  start(): void {
    if (this.started) return;
    this.started = true;
    this.boundary.start();
    this.routeState.set(this.router.url);
    this.router.events.pipe(takeUntilDestroyed(this.destroy)).subscribe(event => {
      if (!(event instanceof NavigationEnd)) return;
      this.routeState.set(event.urlAfterRedirects);
      if (publicRoute(event.urlAfterRedirects)) { this.stopScope(); this.cancelCheck(); this.setMask(false); }
    });
    this.channel = this.browser.connect(message => this.receive(message));
    this.session.signals$.pipe(takeUntilDestroyed(this.destroy)).subscribe(signal => this.broadcast(signal));
    const permanent = ['focus', 'visibilitychange'].map(type => this.browser.listen(type, () => {
      if (!this.browser.visible()) { this.setMask(true); this.stopScope(); return; }
      if (publicRoute(this.routeState())) { this.stopScope(); this.setMask(false); return; }
      if (this.session.authenticated() || this.session.status() === 'unavailable') this.recheck(true);
    }));
    effect(() => {
      this.session.generationState();
      if (!this.session.authenticated() || publicRoute(this.routeState())) { this.stopScope(); return; }
      this.syncScope();
    }, { injector: this.injector });
    effect(() => {
      const deadlines = this.session.deadlines();
      if (this.deadlineTimer !== undefined) clearTimeout(this.deadlineTimer);
      this.deadlineTimer = undefined;
      if (!this.session.authenticated() || !this.browser.visible() || publicRoute(this.routeState())) return;
      if (!deadlines) { this.session.unavailable(); this.setMask(true); return; }
      const remaining = Math.min(deadlines.idle, deadlines.absolute) - performance.now();
      this.deadlineTimer = setTimeout(() => this.recheck(true), Math.max(0, remaining));
    }, { injector: this.injector });
    this.session.changes$.pipe(takeUntilDestroyed(this.destroy)).subscribe(() => {
      this.pending = false;
      this.cancelCheck();
      this.setMask(true);
      this.stopScope();
      this.keyTarget = null;
      this.pointerTarget = this.activationTarget = null;
      this.inputChanges = new WeakSet();
      this.flight?.unsubscribe();
      this.flight = undefined;
    });
    this.destroy.onDestroy(() => { this.alive = false; this.pending = false; this.flight?.unsubscribe(); this.cancelCheck(); this.stopScope(); permanent.forEach(remove => remove()); this.channel?.close(); this.browser.mask(false); });
  }

  private broadcast(signal: SessionSignal): void {
    try { this.channel?.send(signal); } catch { /* Fallback de /me continua autoritativo. */ }
  }

  private receive(message: unknown): void {
    if (!message || typeof message !== 'object' || Object.keys(message).length !== 1 || !('type' in message)) return;
    if (message.type === 'ACTIVITY_UPDATED') {
      if (this.session.authenticated()) this.recheck(false);
    } else if (message.type === 'SESSION_CHANGED' || message.type === 'SESSION_ENDED') {
      this.session.unavailable();
      this.setMask(true);
      this.recheck(true, true);
    }
  }

  private setMask(masked: boolean): void { this.maskState.set(masked); this.browser.mask(masked); }

  private recheck(mask: boolean, allowLogin = false): void {
    if (!this.browser.visible()) return;
    if (publicRoute(this.routeState()) && !(allowLogin && /^\/login(?:[?#]|$)/.test(this.routeState()))) return;
    if (mask) this.setMask(true);
    if (this.checking && !this.checking.closed) return;
    const check = ++this.checkId;
    this.checking = this.session.restore().pipe(takeUntilDestroyed(this.destroy), finalize(() => { if (check === this.checkId) this.checking = undefined; })).subscribe(valid => {
      if (valid && this.session.authenticated()) this.syncScope();
      else this.setMask(true);
    });
  }

  private cancelCheck(): void {
    this.checkId++;
    const checking = this.checking; this.checking = undefined; checking?.unsubscribe();
  }

  private syncScope(): void {
    if (!this.browser.visible() || !this.session.authenticated() || publicRoute(this.routeState())) { this.stopScope(); return; }
    this.setMask(false);
    if (this.removers.length) return;
    this.removers = ['pointerdown', 'pointercancel', 'contextmenu', 'keydown', 'keyup', 'input', 'change', 'click', 'scrollend', 'popstate'].map(type => this.browser.listen(type, event => this.interaction(event)));
    this.polling = setInterval(() => {
      if (this.browser.visible() && this.session.authenticated()) this.recheck(false);
    }, 30_000);
  }

  private stopScope(): void {
    this.keyTarget = this.pointerTarget = this.activationTarget = null;
    this.inputChanges = new WeakSet();
    this.removers.forEach(remove => remove()); this.removers = [];
    if (this.polling !== undefined) clearInterval(this.polling);
    this.polling = undefined;
    if (this.deadlineTimer !== undefined) clearTimeout(this.deadlineTimer);
    this.deadlineTimer = undefined;
  }

  private interaction(event: Event): void {
    if (!event.isTrusted || !this.browser.visible() || this.masked() || !this.session.authenticated()) return;
    const target = event.target instanceof Element ? event.target.closest('a,button,input,select,textarea') ?? event.target : event.target;
    if (event.type === 'pointercancel' || event.type === 'contextmenu') { this.pointerTarget = this.activationTarget = null; return; }
    if (event.type === 'keyup') { this.keyTarget = null; return; }
    if (event.type === 'pointerdown') {
      this.keyTarget = null;
      this.pointerTarget = this.activationTarget = target;
    }
    if (event.type === 'keydown') {
      if ((event as KeyboardEvent).repeat) return;
      this.pointerTarget = null;
      this.keyTarget = this.activationTarget = target;
    }
    if (event.type === 'click') {
      if (target instanceof Element && target.closest('a[href]') && target !== this.activationTarget) this.notifyActivity();
      const activation = this.activationTarget;
      queueMicrotask(() => {
        if (activation === this.activationTarget) this.pointerTarget = this.activationTarget = null;
      });
      return;
    }
    if (event.type === 'input' || event.type === 'change') {
      if (!target) return;
      if (event.type === 'change' && this.inputChanges.has(target)) { this.inputChanges.delete(target); return; }
      if (event.type === 'input') this.inputChanges.add(target);
      if (target === this.keyTarget || target === this.pointerTarget) return;
    }
    this.notifyActivity();
  }

  private notifyActivity(): void {
    if (this.flight && !this.flight.closed) { this.pending = true; return; }
    const generation = this.session.generation;
    this.flight = this.session.activity().pipe(finalize(() => {
      if (!this.alive || generation !== this.session.generation) return;
      this.flight = undefined;
      if (this.pending && this.session.authenticated()) { this.pending = false; this.notifyActivity(); }
    })).subscribe({ error: () => { this.pending = false; } });
  }
}
