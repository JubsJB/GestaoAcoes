import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatNavList, MatListItem, MatListItemIcon, MatListItemTitle } from '@angular/material/list';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

import { AppIconComponent } from '../../shared/app-icon/app-icon.component';
import { CarteiraSelectorComponent } from '../carteira-selector/carteira-selector.component';
import { NAVIGATION_ITEMS, NavigationItem } from '../navigation-items';
import { SessionService } from '../../core/auth/session.service';
import { SessionBoundary } from '../../core/auth/session-boundary.service';
import { ReturnNavigationService } from '../../core/auth/return-navigation.service';
import { SessionCoordinator } from '../../core/auth/session-coordinator.service';

const COMPACT_VIEWPORT = '(max-width: 959.98px)';

@Component({
  selector: 'app-main-layout',
  imports: [
    MatButtonModule,
    MatNavList,
    MatListItem,
    MatListItemIcon,
    MatListItemTitle,
    MatSidenavModule,
    MatToolbarModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    AppIconComponent,
    CarteiraSelectorComponent
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MainLayoutComponent {
  protected readonly session = inject(SessionService);
  protected readonly boundary = inject(SessionBoundary);
  protected readonly returns = inject(ReturnNavigationService);
  protected readonly coordinator = inject(SessionCoordinator);
  protected readonly signingOut = signal(false);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly router = inject(Router);
  private readonly drawer = viewChild(MatSidenav);
  private readonly mainContent = viewChild.required<ElementRef<HTMLElement>>('mainContent');
  protected readonly navigationItems = computed<readonly NavigationItem[]>(() => this.session.admin() ? [...NAVIGATION_ITEMS, { label: 'Usuários', route: '/usuarios', icon: 'identity' }] : NAVIGATION_ITEMS);
  protected readonly compactDrawerOpened = signal(false);
  protected readonly desktopCollapsed = signal(false);
  protected readonly isCompact = toSignal(
    this.breakpointObserver.observe(COMPACT_VIEWPORT).pipe(map((result) => result.matches)),
    { initialValue: false }
  );
  protected readonly navigationCollapsed = computed(() => !this.isCompact() && this.desktopCollapsed());

  constructor() {
    this.boundary.start();
    effect(() => {
      // Every viewport mode starts with a closed mobile drawer, independently of desktop state.
      this.isCompact();
      this.compactDrawerOpened.set(false);
    });

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe(() => {
        if (!this.isCompact()) {
          return;
        }

        this.compactDrawerOpened.set(false);
        void this.drawer()?.close();
      });
  }

  protected toggleDrawer(): void {
    this.compactDrawerOpened.update((opened) => !opened);
  }

  protected logout(): void {
    if (this.signingOut()) return;
    this.signingOut.set(true);
    this.boundary.beforeManualLogout();
    this.session.logout().subscribe({
      next: () => { void this.router.navigateByUrl('/login'); },
      error: () => { void this.router.navigateByUrl('/login', { state: { logoutUnconfirmed: true } }); }
    });
  }

  protected retrySession(): void { this.session.restore().subscribe(); }

  protected toggleDesktopNavigation(): void {
    this.desktopCollapsed.update((collapsed) => !collapsed);
  }

  protected closeCompactDrawer(): void {
    this.compactDrawerOpened.set(false);
  }

  protected focusMainContent(): void {
    this.mainContent().nativeElement.focus();
  }
}
