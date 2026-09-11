import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FeedbackAlertComponent, FeedbackVariant } from './feedback-alert.component';

describe('FeedbackAlertComponent', () => {
  let fixture: ComponentFixture<FeedbackAlertComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [FeedbackAlertComponent] }).compileComponents();
    fixture = TestBed.createComponent(FeedbackAlertComponent);
    fixture.componentRef.setInput('message', 'Mensagem contextual');
  });

  it.each([
    ['success', 'status', 'polite'],
    ['info', 'status', 'polite'],
    ['warning', 'alert', 'assertive'],
    ['error', 'alert', 'assertive']
  ] as const)('maps %s to accessible semantics', (variant, role, ariaLive) => {
    fixture.componentRef.setInput('variant', variant satisfies FeedbackVariant);
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('.feedback-alert');
    expect(alert.getAttribute('role')).toBe(role);
    expect(alert.getAttribute('aria-live')).toBe(ariaLive);
    expect(alert.textContent).toContain('Mensagem contextual');
    expect(fixture.nativeElement.querySelector('[aria-hidden="true"]')).toBeTruthy();
  });

  it('renders StandardError details without replacing the message', () => {
    fixture.componentRef.setInput('details', { provider: 'BRAPI', tentativa: 2 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Mensagem contextual');
    expect(fixture.nativeElement.textContent).toContain('provider');
    expect(fixture.nativeElement.textContent).toContain('BRAPI');
    expect(fixture.nativeElement.textContent).toContain('tentativa');
  });
});
