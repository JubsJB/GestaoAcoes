import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

import { NAVIGATION_ITEMS } from '../navigation-items';

const COMPACT_VIEWPORT = '(max-width: 959.98px)';

@Component({
  selector: 'app-main-layout',
  imports: [
    MatButtonModule,
    MatListModule,
    MatSidenavModule,
    MatToolbarModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MainLayoutComponent {
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly router = inject(Router);
  private readonly drawer = viewChild(MatSidenav);
  private readonly mainContent = viewChild.required<ElementRef<HTMLElement>>('mainContent');
  protected readonly navigationItems = NAVIGATION_ITEMS;
  protected readonly compactDrawerOpened = signal(false);
  protected readonly isCompact = toSignal(
    this.breakpointObserver.observe(COMPACT_VIEWPORT).pipe(map((result) => result.matches)),
    { initialValue: false }
  );

  constructor() {
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

  protected closeCompactDrawer(): void {
    this.compactDrawerOpened.set(false);
  }

  protected focusMainContent(): void {
    this.mainContent().nativeElement.focus();
  }
}
