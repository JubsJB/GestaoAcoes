import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { NavigationEnd, Router } from '@angular/router';
import { CarteiraContextService } from '../carteira/carteira-context.service';
import { CarteiraNavigationService } from '../carteira/carteira-navigation.service';
import { SuccessToastService } from '../../shared/success-toast/success-toast.service';
import { ReturnNavigationService } from './return-navigation.service';
import { SessionService } from './session.service';
import { requiresAdminPath } from './route-access';

@Injectable({ providedIn: 'root' })
export class SessionBoundary {
  private readonly session = inject(SessionService);
  private readonly context = inject(CarteiraContextService);
  private readonly navigation = inject(CarteiraNavigationService);
  private readonly dialogs = inject(MatDialog);
  private readonly toast = inject(SuccessToastService);
  private readonly router = inject(Router);
  private readonly returns = inject(ReturnNavigationService);
  private readonly destroy = inject(DestroyRef);
  private started = false;
  private manualLogout = false;
  private readonly changingState = signal(false);
  readonly changing = this.changingState.asReadonly();

  beforeManualLogout(): void { this.manualLogout = true; this.returns.clear(); }

  start(): void {
    if (this.started) return;
    this.started = true;
    this.session.changes$.pipe(takeUntilDestroyed(this.destroy)).subscribe(() => {
      this.changingState.set(true);
      const previous = this.session.identity()?.id ?? this.session.previousIdentity;
      const manual = this.manualLogout;
      if (!this.manualLogout && !publicRoute(this.router.url)) this.returns.capture(this.router.url, previous);
      this.manualLogout = false;
      this.navigation.stop();
      this.context.reset();
      this.dialogs.closeAll();
      this.toast.dismiss();
      const generation = this.session.generation;
      queueMicrotask(() => {
        if (this.destroy.destroyed || generation !== this.session.generation) return;
        const finish = () => { if (!this.destroy.destroyed && generation === this.session.generation) this.changingState.set(false); };
        if (manual) { finish(); return; }
        let navigation: Promise<boolean> | undefined;
        if (this.session.status() === 'anonymous' && !publicRoute(this.router.url)) navigation = this.router.navigateByUrl('/login');
        else if (this.session.authenticated() && previous !== null && this.session.identity()?.id !== previous) {
          this.returns.clear();
          navigation = this.router.navigateByUrl('/dashboard', { replaceUrl: true });
        } else if (this.session.authenticated() && !this.session.admin()) {
          const path = '/' + (this.router.parseUrl(this.router.url).root.children['primary']?.segments.map(segment => segment.path).join('/') ?? '');
          if (requiresAdminPath(path)) {
            this.returns.notice.set('Você não possui permissão para retornar à página solicitada.');
            navigation = this.router.navigateByUrl('/dashboard', { replaceUrl: true });
          }
        }
        if (navigation) void navigation.then(finish, () => this.session.unavailable()); else finish();
      });
    });
    this.router.events.pipe(takeUntilDestroyed(this.destroy)).subscribe(event => {
      if (event instanceof NavigationEnd && publicRoute(event.urlAfterRedirects)) { this.dialogs.closeAll(); this.toast.dismiss(); }
      if (event instanceof NavigationEnd && this.session.authenticated() && !publicRoute(event.urlAfterRedirects)) {
        this.returns.capture(event.urlAfterRedirects, this.session.identity()!.id);
      }
    });
  }
}

export function publicRoute(url: string): boolean {
  return /^\/(?:login|definir-senha|redefinir-senha)(?:[?#]|$)/.test(url);
}
