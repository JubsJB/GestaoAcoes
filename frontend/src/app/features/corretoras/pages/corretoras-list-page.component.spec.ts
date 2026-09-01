import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { Corretora } from '../models/corretora';
import { CorretoraNotFoundDialogComponent } from '../corretora-not-found-dialog.component';
import { CorretorasService } from '../corretoras.service';
import { CorretorasListPageComponent } from './corretoras-list-page.component';
import { CorretoraCreatePageComponent } from './corretora-create-page.component';

const BROKER: Corretora = { id: 1, cnpj: '11222333000181', razaoSocial: 'Corretora Teste', nomeFantasia: null, email: null, telefone: null, cep: '01001000', logradouro: 'Praça', numero: null, complemento: null, bairro: 'Sé', cidade: 'São Paulo', uf: 'SP', situacaoCadastral: 'ATIVA', validadaMercadoFinanceiro: false, dataCadastro: '2026-08-29T12:00:00Z' };
const error = (status: number, message: string, code: string | null = null) => ({ status, message, code, details: {} }) as NormalizedHttpError;

describe('CorretorasListPageComponent', () => {
  let fixture: ComponentFixture<CorretorasListPageComponent>;
  let service: { listar: ReturnType<typeof vi.fn>; buscarPorCnpj: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };
  let dialogClosed: Subject<boolean | Corretora | undefined>;
  let toast: { show: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = { listar: vi.fn().mockReturnValue(of([BROKER])), buscarPorCnpj: vi.fn().mockReturnValue(of(BROKER)) };
    dialogClosed = new Subject<boolean | Corretora | undefined>();
    dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => dialogClosed.asObservable() }) };
    toast = { show: vi.fn() };
    await TestBed.configureTestingModule({ imports: [CorretorasListPageComponent], providers: [provideRouter([]), { provide: CorretorasService, useValue: service }, { provide: MatDialog, useValue: dialog }, { provide: SuccessToastService, useValue: toast }] }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(CorretorasListPageComponent);
    fixture.detectChanges();
  });

  const component = () => fixture.componentInstance as unknown as { searchControl: { setValue(value: string): void }; search(): void; clearSearch(): void; load(): void };

  it('carrega e apresenta a coleção completa', () => {
    expect(service.listar).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Corretora Teste');
  });

  it('usa estrutura fluida de cards e controles com nomes acessíveis', () => {
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toBe('Corretoras');
    expect(fixture.nativeElement.querySelector('mat-label')?.textContent).toContain('CNPJ exato');
    expect(fixture.nativeElement.querySelector('.broker-grid')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-scroll-region="records"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.entity-card--compact')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
    expect(fixture.nativeElement.querySelector('a[aria-label="Ver detalhes de Corretora Teste"]')).toBeTruthy();
  });

  it.each([
    ['ATIVA', 'positive'],
    ['SUSPENSA', 'warning'],
    ['INATIVA', 'error'],
    ['EM ANÁLISE', 'neutral']
  ] as const)('preserva o status %s e aplica somente a variante visual %s', (status, variant) => {
    service.listar.mockReturnValueOnce(of([{ ...BROKER, situacaoCadastral: status }]));
    component().load();
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-status-variant]') as HTMLElement;
    expect(badge.textContent?.trim()).toBe(status);
    expect(badge.dataset['statusVariant']).toBe(variant);
  });

  it('não busca durante digitação e limpa sem recarregar', () => {
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = '11.222.333/0001-81';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(service.buscarPorCnpj).not.toHaveBeenCalled();
    component().clearSearch();
    expect(service.listar).toHaveBeenCalledTimes(1);
  });

  it.each(['abc12345678000195', '12A345B678/0001-95'])('rejeita caracteres inválidos na busca: %s', (value) => {
    component().searchControl.setValue(value);
    component().search();
    fixture.detectChanges();
    expect(service.buscarPorCnpj).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Informe um CNPJ com 14 dígitos');
  });

  it.each(['12345678000195', '12.345.678/0001-95'])('aceita formato coerente de CNPJ: %s', (value) => {
    component().searchControl.setValue(value);
    component().search();
    expect(service.buscarPorCnpj).toHaveBeenCalledWith('12345678000195');
  });

  it('intercepta o submit DOM, dispara a busca e navega com o DTO transitório', () => {
    const router = TestBed.inject(Router);
    component().searchControl.setValue('11.222.333/0001-81');
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    const submit = new Event('submit', { bubbles: true, cancelable: true });
    const dispatched = form.dispatchEvent(submit);
    expect(service.buscarPorCnpj).toHaveBeenCalledWith('11222333000181');
    expect(router.navigate).toHaveBeenCalledWith([BROKER.id], expect.objectContaining({ info: { corretora: BROKER } }));
    expect(dispatched).toBe(false);
    expect(submit.defaultPrevented).toBe(true);
  });

  it('apresenta empty e erro recuperável', () => {
    service.listar.mockReturnValueOnce(of([]));
    component().load();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Você ainda não possui corretoras cadastradas');
    service.listar.mockReturnValueOnce(throwError(() => error(500, 'Falha ao listar')));
    component().load();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Falha ao listar');
  });

  it('executa nova listagem quando o usuário aciona retry', () => {
    service.listar.mockReturnValueOnce(throwError(() => error(500, 'Falha ao listar'))).mockReturnValueOnce(of([BROKER]));
    component().load();
    fixture.detectChanges();
    const retry = [...fixture.nativeElement.querySelectorAll('button')].find((button: HTMLButtonElement) => button.textContent?.includes('Tentar novamente')) as HTMLButtonElement;
    retry.click();
    fixture.detectChanges();
    expect(service.listar).toHaveBeenCalledTimes(3);
    expect(fixture.nativeElement.textContent).toContain('Corretora Teste');
  });

  it('abre dialog no 404 local sem apagar a coleção e limpa sem novo GET', () => {
    service.buscarPorCnpj.mockReturnValueOnce(throwError(() => error(404, 'Não encontrada')));
    component().searchControl.setValue('11.222.333/0001-81');
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    expect(dialog.open).toHaveBeenCalledWith(CorretoraNotFoundDialogComponent, expect.objectContaining({
      data: { cnpj: '11.222.333/0001-81' },
      autoFocus: 'first-tabbable',
      restoreFocus: true
    }));
    expect(fixture.nativeElement.textContent).toContain('Corretora Teste');
    component().clearSearch();
    fixture.detectChanges();
    expect(service.listar).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Corretora Teste');
  });

  it('mantém 404 com code como StandardError e não como ausência local', () => {
    service.buscarPorCnpj.mockReturnValueOnce(throwError(() => error(404, 'Falha da BrasilAPI', 'BRASILAPI_INDISPONIVEL')));
    component().searchControl.setValue('11.222.333/0001-81');
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Falha da BrasilAPI');
    expect(dialog.open).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Corretora Teste');
  });

  it('cancela o dialog sem navegar ou fazer HTTP', () => {
    const router = TestBed.inject(Router);
    service.buscarPorCnpj.mockReturnValueOnce(throwError(() => error(404, 'Ausente')));
    component().searchControl.setValue('11.222.333/0001-81');
    component().search();
    dialogClosed.next(false);
    dialogClosed.complete();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(service.buscarPorCnpj).toHaveBeenCalledTimes(1);
    expect(service.listar).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Corretora Teste');
  });

  it('abre cadastro contextual pelo CTA do dialog com somente o CNPJ', () => {
    const router = TestBed.inject(Router);
    service.buscarPorCnpj.mockReturnValueOnce(throwError(() => error(404, 'Ausente')));
    component().searchControl.setValue('11.222.333/0001-81');
    component().search();
    dialogClosed.next(true);
    dialogClosed.complete();
    expect(dialog.open).toHaveBeenNthCalledWith(2, CorretoraCreatePageComponent, expect.objectContaining({ data: { cnpj: '11222333000181' } }));
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('abre cadastro vazio pela listagem e incorpora o DTO retornado sem GET redundante', () => {
    const result = new Subject<Corretora | undefined>();
    dialog.open.mockReturnValueOnce({ afterClosed: () => result });
    const button = [...fixture.nativeElement.querySelectorAll('button')].find((item: HTMLButtonElement) => item.textContent?.includes('Cadastrar corretora')) as HTMLButtonElement;
    button.click();
    expect(dialog.open).toHaveBeenCalledWith(CorretoraCreatePageComponent, expect.objectContaining({ data: {} }));
    const created = { ...BROKER, id: 9, razaoSocial: 'Nova Instituição' };
    result.next(created);
    result.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nova Instituição');
    expect(service.listar).toHaveBeenCalledTimes(1);
    expect(toast.show).toHaveBeenCalledWith('Corretora cadastrada com sucesso.');
  });

  it('anuncia semanticamente o carregamento da busca', () => {
    const pending = new Subject<Corretora>();
    service.buscarPorCnpj.mockReturnValueOnce(pending);
    component().searchControl.setValue('11222333000181');
    component().search();
    fixture.detectChanges();
    const status = fixture.nativeElement.querySelector('[role="status"][aria-live="polite"]');
    expect(status?.textContent).toContain('Buscando corretora por CNPJ');
    pending.next(BROKER);
    pending.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Buscando corretora por CNPJ');
  });

  it('comunica loading sem estado vazio prematuro', () => {
    service.listar.mockReturnValueOnce(new Subject<Corretora[]>());
    component().load();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Carregando corretoras');
    expect(fixture.nativeElement.textContent).not.toContain('Nenhuma corretora');
  });
});
