import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSidenav, MatSidenavContainer } from '@angular/material/sidenav';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { MainLayoutComponent } from './main-layout.component';

@Component({ template: '<h1>Destino de teste</h1>' })
class TestDestinationComponent {}

class BreakpointObserverStub {
  private readonly state = new BehaviorSubject<BreakpointState>({ matches: false, breakpoints: {} });

  observe() {
    return this.state.asObservable();
  }

  setCompact(matches: boolean): void {
    this.state.next({ matches, breakpoints: {} });
  }
}

describe('MainLayoutComponent', () => {
  let fixture: ComponentFixture<MainLayoutComponent>;
  let breakpointObserver: BreakpointObserverStub;

  async function createLayout(compact = false): Promise<void> {
    breakpointObserver.setCompact(compact);
    fixture = TestBed.createComponent(MainLayoutComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    breakpointObserver = new BreakpointObserverStub();

    TestBed.configureTestingModule({
      imports: [MainLayoutComponent],
      providers: [
        provideRouter([
          { path: 'dashboard', component: TestDestinationComponent },
          { path: 'corretoras', component: TestDestinationComponent },
          { path: 'acoes', component: TestDestinationComponent },
          { path: 'carteiras', component: TestDestinationComponent },
          { path: 'operacoes', component: TestDestinationComponent }
        ]),
        { provide: BreakpointObserver, useValue: breakpointObserver }
      ]
    });
  });

  it('creates a cohesive shell with the application title and five destinations', async () => {
    await createLayout();
    const navigationLinks = fixture.nativeElement.querySelectorAll('nav a');

    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.nativeElement.querySelector('mat-toolbar').textContent).toContain('Gestão de Ações');
    expect(Array.from(navigationLinks).map((link) => (link as HTMLAnchorElement).textContent?.trim())).toEqual([
      'Dashboard',
      'Corretoras',
      'Ações',
      'Carteiras',
      'Operações'
    ]);
    expect(Array.from(navigationLinks).map((link) => (link as HTMLAnchorElement).getAttribute('href'))).toEqual([
      '/dashboard',
      '/corretoras',
      '/acoes',
      '/carteiras',
      '/operacoes'
    ]);
    expect(fixture.nativeElement.querySelectorAll('nav a app-icon')).toHaveLength(5);
    expect(Array.from(fixture.nativeElement.querySelectorAll('nav a app-icon svg')).every((icon) => (icon as SVGElement).getAttribute('aria-hidden') === 'true')).toBe(true);
    expect(Array.from(navigationLinks).every((link) => {
      const icon = (link as HTMLElement).querySelector('app-icon');
      const label = (link as HTMLElement).querySelector('[matlistitemtitle]');
      return Boolean(icon && label && (icon.compareDocumentPosition(label) & Node.DOCUMENT_POSITION_FOLLOWING));
    })).toBe(true);
    expect(Array.from(navigationLinks).every((link) => (link as HTMLElement).querySelector('app-icon')?.getAttribute('aria-label') === null)).toBe(true);
    expect(fixture.nativeElement.querySelector('.app-brand__mark app-icon')).toBeTruthy();
  });

  it('keeps toolbar outside the bounded workspace and exposes one main scroll region', async () => {
    await createLayout();
    const toolbar = fixture.nativeElement.querySelector('mat-toolbar') as HTMLElement;
    const container = fixture.nativeElement.querySelector('mat-sidenav-container') as HTMLElement;
    const workspace = fixture.nativeElement.querySelector('mat-sidenav-content.workspace') as HTMLElement;
    const main = fixture.nativeElement.querySelector('main.main-content') as HTMLElement;

    expect(toolbar.compareDocumentPosition(container) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(workspace.contains(main)).toBe(true);
    expect(main.getAttribute('tabindex')).toBe('-1');
    expect(fixture.nativeElement.querySelectorAll('main')).toHaveLength(1);
  });

  it('keeps the sidenav opened in side mode on desktop', async () => {
    await createLayout();
    const sidenav = fixture.debugElement.query(By.directive(MatSidenav)).componentInstance as MatSidenav;

    expect(sidenav.mode).toBe('side');
    expect(sidenav.opened).toBe(true);
    expect(fixture.nativeElement.querySelector('.menu-button')).toBeNull();
  });

  it('starts closed in over mode and toggles from an accessible button on compact viewports', async () => {
    await createLayout(true);
    await TestBed.inject(Router).navigateByUrl('/dashboard');
    await fixture.whenStable();
    fixture.detectChanges();
    const sidenav = fixture.debugElement.query(By.directive(MatSidenav)).componentInstance as MatSidenav;
    const button = fixture.nativeElement.querySelector('.menu-button') as HTMLButtonElement;

    expect(sidenav.mode).toBe('over');
    expect(sidenav.opened).toBe(false);
    expect(button.getAttribute('aria-label')).toBe('Abrir ou fechar menu principal');
    expect(button.getAttribute('aria-controls')).toBe('primary-navigation');
    expect(button.getAttribute('aria-expanded')).toBe('false');

    button.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(button.getAttribute('aria-expanded')).toBe('true');
    expect(sidenav.opened).toBe(true);
  });

  it('closes the compact drawer after selecting a destination', async () => {
    await createLayout(true);
    const sidenav = fixture.debugElement.query(By.directive(MatSidenav)).componentInstance as MatSidenav;
    await sidenav.open();
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('a[href="/dashboard"]') as HTMLAnchorElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(TestBed.inject(Router).url).toBe('/dashboard');
    expect(sidenav.opened).toBe(false);
  });

  it('closes the compact drawer after programmatic navigation', async () => {
    await createLayout(true);
    const sidenav = fixture.debugElement.query(By.directive(MatSidenav)).componentInstance as MatSidenav;
    await sidenav.open();

    await TestBed.inject(Router).navigateByUrl('/corretoras');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(sidenav.opened).toBe(false);
  });

  it('closes the compact drawer through backdrop and Escape', async () => {
    await createLayout(true);
    const sidenav = fixture.debugElement.query(By.directive(MatSidenav)).componentInstance as MatSidenav;
    const container = fixture.debugElement.query(By.directive(MatSidenavContainer))
      .componentInstance as MatSidenavContainer;
    const button = fixture.nativeElement.querySelector('.menu-button') as HTMLButtonElement;

    button.click();
    fixture.detectChanges();
    container.backdropClick.emit();
    fixture.detectChanges();
    expect(sidenav.opened).toBe(false);

    button.click();
    fixture.detectChanges();
    fixture.debugElement
      .query(By.directive(MatSidenav))
      .nativeElement.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    expect(sidenav.opened).toBe(false);
  });

  it('marks the active route semantically and with a visible class', async () => {
    await createLayout();
    await TestBed.inject(Router).navigateByUrl('/dashboard');
    await fixture.whenStable();
    fixture.detectChanges();

    const activeLink = fixture.nativeElement.querySelector('a[href="/dashboard"]') as HTMLAnchorElement;

    expect(activeLink.getAttribute('aria-current')).toBe('page');
    expect(activeLink.classList.contains('active-navigation-item')).toBe(true);
  });

  it('exposes the main navigation and skip link with accessible semantics', async () => {
    await createLayout();

    expect(fixture.nativeElement.querySelector('nav').getAttribute('aria-label')).toBe('Navegação principal');
    expect(fixture.nativeElement.querySelector('.skip-link').getAttribute('href')).toBe('#main-content');
    expect(fixture.nativeElement.querySelector('main').getAttribute('id')).toBe('main-content');
    expect(fixture.nativeElement.querySelector('main').getAttribute('tabindex')).toBe('-1');

    (fixture.nativeElement.querySelector('.skip-link') as HTMLAnchorElement).click();
    expect(document.activeElement).toBe(fixture.nativeElement.querySelector('main'));
  });
});
