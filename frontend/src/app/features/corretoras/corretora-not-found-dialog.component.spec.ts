import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { authenticatedModule } from '../../../../test-support/authenticated-module';

import { CorretoraNotFoundDialogComponent } from './corretora-not-found-dialog.component';

describe('CorretoraNotFoundDialogComponent', () => {
  let fixture: ComponentFixture<CorretoraNotFoundDialogComponent>;

  beforeEach(async () => {
    await authenticatedModule({
      imports: [CorretoraNotFoundDialogComponent],
      providers: [{ provide: MAT_DIALOG_DATA, useValue: { cnpj: '11.222.333/0001-81' } }]
    }).compileComponents();
    fixture = TestBed.createComponent(CorretoraNotFoundDialogComponent);
    fixture.detectChanges();
  });

  it('offers USER a return action without inviting registration', async () => {
    fixture.destroy();
    TestBed.resetTestingModule();
    await authenticatedModule({
      imports: [CorretoraNotFoundDialogComponent],
      providers: [{ provide: MAT_DIALOG_DATA, useValue: { cnpj: '11.222.333/0001-81' } }]
    }, 'USER').compileComponents();
    fixture = TestBed.createComponent(CorretoraNotFoundDialogComponent);
    fixture.detectChanges();
    const buttons = [...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];
    expect(buttons.map(button => button.textContent?.trim())).toEqual(['Voltar à listagem']);
    expect(fixture.nativeElement.textContent).toContain('11.222.333/0001-81');
    expect(fixture.nativeElement.textContent).not.toContain('iniciar um cadastro');
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
  });
});
