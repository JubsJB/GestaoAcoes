import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

import { CorretoraConfirmDialogComponent } from './corretora-confirm-dialog.component';

describe('CorretoraConfirmDialogComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CorretoraConfirmDialogComponent],
      providers: [{ provide: MAT_DIALOG_DATA, useValue: { message: 'Confirmação necessária', situacaoCadastral: 'SUSPENSA' } }]
    }).compileComponents();
  });

  it('explica a situação e oferece ações inequívocas por botão', () => {
    const fixture = TestBed.createComponent(CorretoraConfirmDialogComponent);
    fixture.detectChanges();
    const buttons = [...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[];
    expect(fixture.nativeElement.querySelector('h2')?.textContent).toContain('Confirmar cadastro');
    expect(fixture.nativeElement.textContent).toContain('não é ATIVA');
    expect(fixture.nativeElement.textContent).toContain('SUSPENSA');
    expect(fixture.nativeElement.textContent).toContain('Confirmação necessária');
    expect(buttons.map((button) => button.textContent?.trim())).toEqual(['Cancelar', 'Confirmar cadastro']);
    expect(buttons[1].hasAttribute('cdkfocusinitial')).toBe(true);
  });
});
