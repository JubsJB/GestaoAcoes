import { DestroyRef, inject, Injectable } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { CarteiraContextService } from './carteira-context.service';

@Injectable({ providedIn: 'root' })
export class CarteiraNavigationService {
  private readonly router = inject(Router);
  private readonly context = inject(CarteiraContextService);
  private readonly destroyRef = inject(DestroyRef);
  private started = false;

  start(): void {
    if (this.started) return;
    this.started = true;
    const synchronize = () => {
      const tree = this.router.parseUrl(this.router.url);
      const segments = tree.root.children['primary']?.segments.map(segment => segment.path) ?? [];
      if (segments[0] === 'carteiras' && segments.length === 2 && segments[1] !== 'nova') {
        this.context.resolveUrl(segments[1]);
      } else if (segments[0] !== 'dashboard' && !(segments[0] === 'operacoes' && segments[1] === 'nova')) {
        this.context.resolveUrl(null);
      }
    };
    this.router.events.pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef)).subscribe(synchronize);
    synchronize();
    this.context.initialize().subscribe();
  }

  bindDashboard(route: ActivatedRoute, destroyRef: DestroyRef): void {
    let raw: string | null = null;
    let normalized: string | null = null;
    route.queryParamMap.pipe(takeUntilDestroyed(destroyRef)).subscribe(params => {
      raw = params.get('carteiraId');
      const automatic = raw !== null && raw === normalized;
      normalized = null;
      this.context.resolveUrl(raw, !automatic);
    });
    this.context.state$.pipe(takeUntilDestroyed(destroyRef)).subscribe(state => {
      if (raw !== null || state.activeId === null || state.status !== 'ready') return;
      raw = normalized = String(state.activeId);
      void this.router.navigate([], { relativeTo: route, queryParams: { carteiraId: state.activeId }, queryParamsHandling: 'merge', replaceUrl: true });
    });
    this.context.initialize().subscribe();
  }

  select(id: number): void {
    if (!this.context.select(id)) return;
    const tree = this.router.parseUrl(this.router.url);
    const segments = tree.root.children['primary']?.segments.map(segment => segment.path) ?? [];
    if (segments[0] === 'dashboard') {
      tree.queryParams = { ...tree.queryParams, carteiraId: id };
      if (this.router.serializeUrl(tree) !== this.router.url) void this.router.navigateByUrl(tree);
    } else if (segments[0] === 'carteiras' && segments.length === 2 && segments[1] !== 'nova') {
      if (segments[1] !== String(id)) void this.router.navigate(['/carteiras', id], { queryParams: tree.queryParams });
    }
    // Forms own their captured identity. Changing the shell never rewrites their URL.
  }
}
