import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { Corretora } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';
import { CorretoraCreatePageComponent } from './corretora-create-page.component';

const BROKER: Corretora = { id: 8, cnpj: '11222333000181', razaoSocial: 'Nova', nomeFantasia: null, email: null, telefone: null, cep: '01001000', logradouro: 'Praça', numero: null, complemento: null, bairro: 'Sé', cidade: 'São Paulo', uf: 'SP', situacaoCadastral: 'ATIVA', validadaMercadoFinanceiro: false, dataCadastro: '2026-08-30T12:00:00Z' };
const CONFLICT = { status: 409, code: 'SITUACAO_CADASTRAL_NAO_ATIVA', message: 'Confirmar', details: { situacaoCadastral: 'SUSPENSA' } } as unknown as NormalizedHttpError;

describe('CorretoraCreatePageComponent em dialog', () => {
  let fixture: ComponentFixture<CorretoraCreatePageComponent>;
  let service: { cadastrar: ReturnType<typeof vi.fn> };
  let ref: { close: ReturnType<typeof vi.fn> };
  let confirm: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = { cadastrar: vi.fn().mockReturnValue(of(BROKER)) };
    ref = { close: vi.fn() };
    confirm = { open: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [CorretoraCreatePageComponent],
      providers: [provideRouter([]), { provide: CorretorasService, useValue: service }, { provide: SuccessToastService, useValue: { show: vi.fn() } }, { provide: MatDialog, useValue: confirm }, { provide: MatDialogRef, useValue: ref }, { provide: MAT_DIALOG_DATA, useValue: { cnpj: '11222333000181' } }]
    }).compileComponents();
    fixture = TestBed.createComponent(CorretoraCreatePageComponent);
    fixture.detectChanges();
  });

  const submit = () => (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

  it('preenche somente CNPJ sem POST automático e cancela sem HTTP', () => {
    expect((fixture.nativeElement.querySelector('input') as HTMLInputElement).value).toBe('11.222.333/0001-81');
    expect(fixture.nativeElement.querySelectorAll('input')).toHaveLength(1);
    expect(service.cadastrar).not.toHaveBeenCalled();
    const cancel = [...fixture.nativeElement.querySelectorAll('button')].find((button: HTMLButtonElement) => button.textContent?.trim() === 'Cancelar') as HTMLButtonElement;
    cancel.click();
    expect(ref.close).toHaveBeenCalledWith();
    expect(service.cadastrar).not.toHaveBeenCalled();
  });

  it('fecha com o DTO após POST explícito único', () => { submit(); expect(service.cadastrar).toHaveBeenCalledWith({ cnpj: '11222333000181' }); expect(ref.close).toHaveBeenCalledWith(BROKER); });

  it('preserva confirmação excepcional com exatamente um segundo POST', () => {
    service.cadastrar.mockReturnValueOnce(throwError(() => CONFLICT)).mockReturnValueOnce(of(BROKER));
    confirm.open.mockReturnValue({ afterClosed: () => of(true) });
    submit();
    expect(service.cadastrar).toHaveBeenCalledTimes(2);
    expect(service.cadastrar).toHaveBeenNthCalledWith(2, { cnpj: '11222333000181', confirmarSituacaoCadastralNaoAtiva: true });
    expect(ref.close).toHaveBeenCalledWith(BROKER);
  });
});
