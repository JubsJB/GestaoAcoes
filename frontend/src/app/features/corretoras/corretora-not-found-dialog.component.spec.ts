import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

import { CorretoraNotFoundDialogComponent } from './corretora-not-found-dialog.component';

describe('CorretoraNotFoundDialogComponent', () => {
  let fixture: ComponentFixture<CorretoraNotFoundDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CorretoraNotFoundDialogComponent],
      providers: [{ provide: MAT_DIALOG_DATA, useValue: { cnpj: '11.222.333/0001-81' } }]
    }).compileComponents();
    fixture = TestBed.createComponent(CorretoraNotFoundDialogComponent);
    fixture.detectChanges();
  });

  it('presents an informative title, searched CNPJ and two explicit actions', () => {
    const buttons = [...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];
    expect(fixture.nativeElement.querySelector('[mat-dialog-title]')?.textContent).toContain('Corretora não cadastrada');
    expect(fixture.nativeElement.textContent).toContain('11.222.333/0001-81');
    expect(buttons.map((button) => button.textContent?.trim())).toEqual(['Cancelar', 'Cadastrar corretora']);
    const actions = fixture.nativeElement.querySelector('.app-dialog-actions') as HTMLElement;
    const slots = [...actions.children] as HTMLElement[];
    expect(slots).toHaveLength(2);
    expect(slots.every((slot) => slot.classList.contains('app-dialog-actions__item'))).toBe(true);
    expect(slots.map((slot) => slot.querySelector('button')?.textContent?.trim())).toEqual(['Cancelar', 'Cadastrar corretora']);
    expect(actions.querySelector(':scope > button')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-icon svg')?.getAttribute('aria-hidden')).toBe('true');
  });
});
