import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { OneTimeCodeDialogComponent } from './one-time-code-dialog.component';

@Component({ template: '<button>Origem</button>' }) class Host {}

describe('Entrega única do código ao administrador', () => {
  const original = Object.getOwnPropertyDescriptor(navigator, 'clipboard');
  afterEach(() => { if (original) Object.defineProperty(navigator, 'clipboard', original); else Reflect.deleteProperty(navigator, 'clipboard'); });
  it.each([true, false])('cópia explícita e tratamento de clipboard disponível=%s sem persistência', async available => {
    const writeText = vi.fn().mockImplementation(() => available ? Promise.resolve() : Promise.reject(new Error('Indisponível')));
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } });
    TestBed.configureTestingModule({ imports: [Host] }); const fixture = TestBed.createComponent(Host); fixture.detectChanges();
    TestBed.inject(MatDialog).open(OneTimeCodeDialogComponent, { data: { username: 'fixture.usuario', codigo: 'ABCDEFGHIJKLMNOPQRSTUVWXYA', motivo: 'INICIAL', expiraEm: '2026-10-31T12:15:00Z' } }); TestBed.tick();
    const overlay = TestBed.inject(OverlayContainer).getContainerElement();
    expect(writeText).not.toHaveBeenCalled();
    const button = [...overlay.querySelectorAll<HTMLButtonElement>('button')].find(button => button.textContent?.includes('Copiar'))!; button.click();
    await vi.waitFor(() => { fixture.detectChanges(); expect(overlay.textContent).toContain(available ? 'Código copiado' : 'copie manualmente'); });
    expect(writeText).toHaveBeenCalledTimes(1);
    overlay.querySelector('[role=dialog]')!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', keyCode: 27, bubbles: true }));
    await vi.waitFor(() => expect(overlay.querySelector('[data-testid=one-time-code]')).toBeNull());
  });
});
