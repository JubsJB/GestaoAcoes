import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { Corretora } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';
import { CorretoraCreatePageComponent } from './corretora-create-page.component';

const BROKER: Corretora = { id: 2, cnpj: '11222333000181', razaoSocial: 'Nova Corretora', nomeFantasia: null, email: null, telefone: null, cep: '01001000', logradouro: 'Praça', numero: null, complemento: null, bairro: 'Sé', cidade: 'São Paulo', uf: 'SP', situacaoCadastral: 'ATIVA', validadaMercadoFinanceiro: false, dataCadastro: '2026-08-29T12:00:00Z' };
const conflict = (code: string) => ({ status: 409, code, message: 'Confirmação necessária', details: { situacaoCadastral: 'SUSPENSA' } }) as unknown as NormalizedHttpError;

describe('CorretoraCreatePageComponent', () => {
  let fixture: ComponentFixture<CorretoraCreatePageComponent>;
  let service: { cadastrar: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };
  let snackBar: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = { cadastrar: vi.fn().mockReturnValue(of(BROKER)) };
    dialog = { open: vi.fn() };
    snackBar = { open: vi.fn() };
    await TestBed.configureTestingModule({ imports: [CorretoraCreatePageComponent], providers: [provideRouter([]), { provide: CorretorasService, useValue: service }, { provide: MatDialog, useValue: dialog }, { provide: MatSnackBar, useValue: snackBar }] }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(CorretoraCreatePageComponent);
    fixture.detectChanges();
  });

  function submit(): void {
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = '11.222.333/0001-81';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
  }

  it('possui somente CNPJ e envia o payload inicial exato', () => {
    expect(fixture.nativeElement.querySelectorAll('input')).toHaveLength(1);
    expect(fixture.nativeElement.textContent).not.toContain('confirmarSituacaoCadastralNaoAtiva');
    submit();
    expect(service.cadastrar).toHaveBeenCalledWith({ cnpj: '11222333000181' });
  });

  it('associa label, orientação e erro acessível ao único campo', () => {
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(fixture.nativeElement.querySelector('mat-label')?.textContent).toBe('CNPJ');
    expect(input.getAttribute('aria-describedby')).toContain('cnpj-hint');
    expect(fixture.nativeElement.querySelector('mat-error')?.textContent).toContain('Informe um CNPJ com 14 dígitos');
  });

  it('impede envio inválido e submissão concorrente', () => {
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    expect(service.cadastrar).not.toHaveBeenCalled();
    service.cadastrar.mockReturnValue(new Subject<Corretora>());
    submit();
    submit();
    expect(service.cadastrar).toHaveBeenCalledTimes(1);
  });

  it('anuncia sucesso, reutiliza o DTO e navega sem solicitar detalhe', () => {
    const router = TestBed.inject(Router);
    submit();
    expect(snackBar.open).toHaveBeenCalledWith('Corretora cadastrada com sucesso.', 'Fechar', { duration: 5000 });
    expect(router.navigate).toHaveBeenCalledWith(['/corretoras', 2], { info: { corretora: BROKER } });
    expect(service.cadastrar).toHaveBeenCalledTimes(1);
  });

  it('cancela o 409 específico sem segundo POST', () => {
    service.cadastrar.mockReturnValueOnce(throwError(() => conflict('SITUACAO_CADASTRAL_NAO_ATIVA')));
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });
    submit();
    expect(dialog.open).toHaveBeenCalled();
    expect(service.cadastrar).toHaveBeenCalledTimes(1);
  });

  it('confirma o 409 específico com segundo payload exato', () => {
    service.cadastrar.mockReturnValueOnce(throwError(() => conflict('SITUACAO_CADASTRAL_NAO_ATIVA'))).mockReturnValueOnce(of(BROKER));
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    submit();
    expect(service.cadastrar).toHaveBeenNthCalledWith(2, { cnpj: '11222333000181', confirmarSituacaoCadastralNaoAtiva: true });
  });

  it('não abre confirmação para outro 409', () => {
    service.cadastrar.mockReturnValueOnce(throwError(() => conflict('CORRETORA_DUPLICADA')));
    submit();
    fixture.detectChanges();
    expect(dialog.open).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Confirmação necessária');
  });

  it('mantém o fluxo serializado enquanto o diálogo está aberto e libera após cancelar', () => {
    const decision = new Subject<boolean | undefined>();
    service.cadastrar.mockReturnValueOnce(throwError(() => conflict('SITUACAO_CADASTRAL_NAO_ATIVA'))).mockReturnValue(of(BROKER));
    dialog.open.mockReturnValue({ afterClosed: () => decision });
    submit();
    fixture.detectChanges();
    submit();
    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(service.cadastrar).toHaveBeenCalledTimes(1);
    expect((fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBe(true);
    decision.next(false);
    decision.complete();
    fixture.detectChanges();
    expect((fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBe(false);
    submit();
    expect(service.cadastrar).toHaveBeenCalledTimes(2);
  });

  it('trata fechamento do diálogo como cancelamento sem segundo POST', () => {
    service.cadastrar.mockReturnValueOnce(throwError(() => conflict('SITUACAO_CADASTRAL_NAO_ATIVA')));
    dialog.open.mockReturnValue({ afterClosed: () => of(undefined) });
    submit();
    expect(service.cadastrar).toHaveBeenCalledTimes(1);
  });

  it('mantém bloqueio durante o segundo POST e envia uma única confirmação', () => {
    const confirmedRequest = new Subject<Corretora>();
    service.cadastrar.mockReturnValueOnce(throwError(() => conflict('SITUACAO_CADASTRAL_NAO_ATIVA'))).mockReturnValueOnce(confirmedRequest);
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    submit();
    fixture.detectChanges();
    submit();
    expect(service.cadastrar).toHaveBeenCalledTimes(2);
    expect(service.cadastrar).toHaveBeenNthCalledWith(2, { cnpj: '11222333000181', confirmarSituacaoCadastralNaoAtiva: true });
    expect((fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBe(true);
    confirmedRequest.next(BROKER);
    confirmedRequest.complete();
  });

  it('configura restauração de foco e fornece conteúdo contextual ao diálogo', () => {
    service.cadastrar.mockReturnValueOnce(throwError(() => conflict('SITUACAO_CADASTRAL_NAO_ATIVA')));
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });
    submit();
    expect(dialog.open).toHaveBeenCalledWith(expect.any(Function), expect.objectContaining({ restoreFocus: true, data: { message: 'Confirmação necessária', situacaoCadastral: 'SUSPENSA' } }));
  });
});
