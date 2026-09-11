import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuccessToastComponent } from './success-toast.component';
import { SUCCESS_TOAST_DATA, SuccessToastRef } from './success-toast.tokens';

describe('SuccessToastComponent', () => {
  let fixture: ComponentFixture<SuccessToastComponent>;
  const ref = { dismiss: vi.fn() };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SuccessToastComponent],
      providers: [
        { provide: SUCCESS_TOAST_DATA, useValue: { message: 'Cadastro concluído.' } },
        { provide: SuccessToastRef, useValue: ref }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(SuccessToastComponent);
    fixture.detectChanges();
  });

  it('apresenta ícone local, mensagem e região de status sem roubar foco', () => {
    const status = fixture.nativeElement.querySelector('[role="status"]') as HTMLElement;
    expect(status.getAttribute('aria-live')).toBe('polite');
    expect(status.textContent).toContain('Cadastro concluído.');
    expect(fixture.nativeElement.querySelector('app-icon svg')?.getAttribute('aria-hidden')).toBe('true');
    expect(document.activeElement).not.toBe(fixture.nativeElement.querySelector('button'));
  });

  it('permite fechamento manual acessível', () => {
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    expect(ref.dismiss).toHaveBeenCalledTimes(1);
  });
});
