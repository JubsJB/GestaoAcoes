import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageHeaderComponent } from './page-header.component';

@Component({
  imports: [PageHeaderComponent],
  template: `<app-page-header headingId="test-title" title="Título muito longo que deve continuar legível" description="Descrição da página" eyebrow="Contexto" icon="stock"><button page-header-action type="button">Ação principal</button></app-page-header>`
})
class HostComponent {}

describe('PageHeaderComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  it('renders exactly one page heading and its description', () => {
    const headings = fixture.nativeElement.querySelectorAll('h1');
    expect(headings).toHaveLength(1);
    expect(headings[0].id).toBe('test-title');
    expect(headings[0].textContent).toContain('Título muito longo');
    expect(fixture.nativeElement.querySelector('.page-header__description')?.textContent).toBe('Descrição da página');
  });

  it('projects the action into a separate responsive area', () => {
    const action = fixture.nativeElement.querySelector('.page-header__action button');
    expect(action?.textContent).toBe('Ação principal');
    expect(fixture.nativeElement.querySelector('.page-header')).toBeTruthy();
  });

  it('renders optional contextual eyebrow and decorative local icon', () => {
    expect(fixture.nativeElement.querySelector('.page-header__eyebrow')?.textContent).toBe('Contexto');
    expect(fixture.nativeElement.querySelector('.page-header__icon app-icon svg')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.page-header__icon').getAttribute('aria-hidden')).toBe('true');
  });
});
