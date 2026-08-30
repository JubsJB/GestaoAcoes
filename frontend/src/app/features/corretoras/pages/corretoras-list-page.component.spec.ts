import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { Corretora } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';
import { CorretorasListPageComponent } from './corretoras-list-page.component';

const BROKER: Corretora = { id: 1, cnpj: '11222333000181', razaoSocial: 'Corretora Teste', nomeFantasia: null, email: null, telefone: null, cep: '01001000', logradouro: 'Praça', numero: null, complemento: null, bairro: 'Sé', cidade: 'São Paulo', uf: 'SP', situacaoCadastral: 'ATIVA', validadaMercadoFinanceiro: false, dataCadastro: '2026-08-29T12:00:00Z' };
const error = (status: number, message: string) => ({ status, message, code: null, details: {} }) as NormalizedHttpError;

describe('CorretorasListPageComponent', () => {
  let fixture: ComponentFixture<CorretorasListPageComponent>;
  let service: { listar: ReturnType<typeof vi.fn>; buscarPorCnpj: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = { listar: vi.fn().mockReturnValue(of([BROKER])), buscarPorCnpj: vi.fn().mockReturnValue(of(BROKER)) };
    await TestBed.configureTestingModule({ imports: [CorretorasListPageComponent], providers: [provideRouter([]), { provide: CorretorasService, useValue: service }] }).compileComponents();
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
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
    expect(fixture.nativeElement.querySelector('a[aria-label="Ver detalhes de Corretora Teste"]')).toBeTruthy();
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

  it('busca apenas no submit e navega para o detalhe', () => {
    const router = TestBed.inject(Router);
    component().searchControl.setValue('11.222.333/0001-81');
    component().search();
    expect(service.buscarPorCnpj).toHaveBeenCalledWith('11222333000181');
    expect(router.navigate).toHaveBeenCalled();
  });

  it('apresenta empty e erro recuperável', () => {
    service.listar.mockReturnValueOnce(of([]));
    component().load();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nenhuma corretora');
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

  it('trata 404 da busca sem apagar a coleção e limpa sem novo GET', () => {
    service.buscarPorCnpj.mockReturnValueOnce(throwError(() => error(404, 'Não encontrada')));
    component().searchControl.setValue('11.222.333/0001-81');
    component().search();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nenhuma corretora encontrada para este CNPJ');
    expect(fixture.nativeElement.textContent).toContain('Corretora Teste');
    component().clearSearch();
    fixture.detectChanges();
    expect(service.listar).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Corretora Teste');
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
