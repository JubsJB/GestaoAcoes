import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { StickyBackComponent } from './sticky-back.component';

describe('StickyBackComponent', () => {
  let fixture: ComponentFixture<StickyBackComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [StickyBackComponent], providers: [provideRouter([])] }).compileComponents();
    fixture = TestBed.createComponent(StickyBackComponent);
  });

  it.each([
    ['/corretoras', 'Voltar para corretoras'],
    ['/acoes', 'Voltar para ações']
  ])('apresenta ação textual acessível para %s', (route, label) => {
    fixture.componentRef.setInput('route', route);
    fixture.componentRef.setInput('label', label);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;
    expect(link.textContent).toContain(label);
    expect(link.getAttribute('href')).toBe(route);
    expect(link.getAttribute('aria-label')).toBe(label);
    expect(link.tabIndex).toBe(0);
    expect(fixture.nativeElement.querySelector('.sticky-back__marker')).toBeNull();
    expect(link.classList).not.toContain('app-back-action--compact');
  });
});
