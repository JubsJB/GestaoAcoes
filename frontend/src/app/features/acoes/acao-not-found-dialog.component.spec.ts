import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

import { AcaoNotFoundDialogComponent } from './acao-not-found-dialog.component';

describe('AcaoNotFoundDialogComponent', () => {
  let fixture: ComponentFixture<AcaoNotFoundDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AcaoNotFoundDialogComponent],
      providers: [{ provide: MAT_DIALOG_DATA, useValue: { ticker: 'XPTO', mercado: 'BRASIL' } }]
    }).compileComponents();
    fixture = TestBed.createComponent(AcaoNotFoundDialogComponent);
    fixture.detectChanges();
  });

  it('presents an informative title, ticker, market and two explicit actions', () => {
    const buttons = [...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];
    expect(fixture.nativeElement.querySelector('[mat-dialog-title]')?.textContent).toContain('Ação não cadastrada');
    expect(fixture.nativeElement.textContent).toContain('XPTO');
    expect(fixture.nativeElement.textContent).toContain('Brasil');
    expect(buttons.map((button) => button.textContent?.trim())).toEqual(['Cancelar', 'Cadastrar ação']);
    const actions = fixture.nativeElement.querySelector('.app-dialog-actions') as HTMLElement;
    const slots = [...actions.children] as HTMLElement[];
    expect(slots).toHaveLength(2);
    expect(slots.every((slot) => slot.classList.contains('app-dialog-actions__item'))).toBe(true);
    expect(slots.map((slot) => slot.querySelector('button')?.textContent?.trim())).toEqual(['Cancelar', 'Cadastrar ação']);
    expect(actions.querySelector(':scope > button')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-icon svg')?.getAttribute('aria-hidden')).toBe('true');
  });
});
