import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { AcoesService } from '../acoes.service';
import { AcaoResponse } from '../models/acao';
import { AcaoCreatePageComponent } from './acao-create-page.component';

const STOCK: AcaoResponse = { id: 7, ticker: 'AAPL', nomeEmpresa: 'Apple', mercado: 'EUA', moeda: 'USD', cotacaoAtual: 200, dataHoraCotacao: '2026-08-30T12:00:00Z' };

describe('AcaoCreatePageComponent em dialog', () => {
  let fixture: ComponentFixture<AcaoCreatePageComponent>;
  let service: { criar: ReturnType<typeof vi.fn> };
  let ref: { close: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = { criar: vi.fn().mockReturnValue(of(STOCK)) };
    ref = { close: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [AcaoCreatePageComponent],
      providers: [provideRouter([]), { provide: AcoesService, useValue: service }, { provide: SuccessToastService, useValue: { show: vi.fn() } }, { provide: MatDialogRef, useValue: ref }, { provide: MAT_DIALOG_DATA, useValue: { ticker: ' aapl ', mercado: 'EUA' } }]
    }).compileComponents();
    fixture = TestBed.createComponent(AcaoCreatePageComponent);
    fixture.detectChanges();
  });

  it('preenche somente ticker e mercado sem POST automático', () => {
    expect((fixture.nativeElement.querySelector('input') as HTMLInputElement).value).toBe('AAPL');
    const component = fixture.componentInstance as unknown as { form: { controls: { mercado: { value: string | null } } } };
    expect(component.form.controls.mercado.value).toBe('EUA');
    expect(service.criar).not.toHaveBeenCalled();
  });

  it('fecha com o DTO do POST único após submissão explícita', () => {
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    expect(service.criar).toHaveBeenCalledWith({ ticker: 'AAPL', mercado: 'EUA' });
    expect(service.criar).toHaveBeenCalledTimes(1);
    expect(ref.close).toHaveBeenCalledWith(STOCK);
  });

  it('cancela sem HTTP', () => {
    const cancel = [...fixture.nativeElement.querySelectorAll('button')].find((button: HTMLButtonElement) => button.textContent?.trim() === 'Cancelar') as HTMLButtonElement;
    cancel.click();
    expect(ref.close).toHaveBeenCalledWith();
    expect(service.criar).not.toHaveBeenCalled();
  });
});
