import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { Corretora } from '../models/corretora';
import { CorretorasService } from '../corretoras.service';
import { CorretoraDetailPageComponent } from './corretora-detail-page.component';

const BROKER: Corretora = { id: 3, cnpj: '11222333000181', razaoSocial: 'Corretora Detalhada', nomeFantasia: null, email: null, telefone: null, cep: '01001000', logradouro: 'Praça da Sé', numero: null, complemento: null, bairro: 'Sé', cidade: 'São Paulo', uf: 'SP', situacaoCadastral: 'SUSPENSA', validadaMercadoFinanceiro: false, dataCadastro: '2026-08-29T12:00:00Z' };
const httpError = (status: number, message: string) => ({ status, message, code: null, details: {} }) as NormalizedHttpError;

describe('CorretoraDetailPageComponent', () => {
  async function create(info?: Corretora, error?: NormalizedHttpError): Promise<{ fixture: ComponentFixture<CorretoraDetailPageComponent>; service: { buscarPorId: ReturnType<typeof vi.fn> } }> {
    const service = { buscarPorId: vi.fn().mockReturnValue(error ? throwError(() => error) : of(BROKER)) };
    await TestBed.configureTestingModule({ imports: [CorretoraDetailPageComponent], providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => String(BROKER.id) } } } }, { provide: CorretorasService, useValue: service }] }).compileComponents();
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'currentNavigation').mockReturnValue(info ? ({ extras: { info: { corretora: info } } } as never) : null);
    const fixture = TestBed.createComponent(CorretoraDetailPageComponent);
    fixture.detectChanges();
    return { fixture, service };
  }

  afterEach(() => TestBed.resetTestingModule());

  it('usa DTO transitório sem GET e apresenta o contrato completo', async () => {
    const { fixture, service } = await create(BROKER);
    const text = fixture.nativeElement.textContent;
    expect(service.buscarPorId).not.toHaveBeenCalled();
    expect(text).toContain('Corretora Detalhada');
    expect(text).toContain('11.222.333/0001-81');
    expect(text).toContain('Praça da Sé');
    expect(text).toContain('São Paulo/SP');
    expect(text).toContain('CEP 01001-000');
    expect(text).toContain('SUSPENSA');
    expect(text).toContain('Validação ainda não realizada');
    expect(text).toMatch(/29\/08\/2026 às \d{2}:\d{2}/);
    expect(fixture.nativeElement.querySelectorAll('mat-card')).toHaveLength(3);
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });

  it('mantém a ação de retorno navegável na estrutura sticky do workspace', async () => {
    const { fixture } = await create(BROKER);
    const back = fixture.nativeElement.querySelector('a.app-back-action') as HTMLAnchorElement;
    expect(back).toBeTruthy();
    expect(back.getAttribute('href')).toBe('/corretoras');
  });

  it('representa todos os campos opcionais nulos sem inventar valores', async () => {
    const { fixture } = await create(BROKER);
    expect(fixture.nativeElement.textContent.match(/Não informado/g)).toHaveLength(5);
    expect(fixture.nativeElement.textContent).not.toContain('null');
  });

  it('usa GET correto em acesso direto ou refresh sem DTO transitório', async () => {
    const { service } = await create();
    expect(service.buscarPorId).toHaveBeenCalledTimes(1);
    expect(service.buscarPorId).toHaveBeenCalledWith(3);
  });

  it('apresenta 404 com recuperação para a listagem', async () => {
    const { fixture } = await create(undefined, httpError(404, 'Não encontrada'));
    expect(fixture.nativeElement.textContent).toContain('Corretora não encontrada');
    expect(fixture.nativeElement.textContent).toContain('Voltar para corretoras');
  });

  it('apresenta erro não-404 e executa GET novamente no retry', async () => {
    const { fixture, service } = await create(undefined, httpError(500, 'Falha temporária'));
    service.buscarPorId.mockReturnValueOnce(of(BROKER));
    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar a corretora');
    expect(fixture.nativeElement.textContent).toContain('Falha temporária');
    const retry = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    retry.click();
    fixture.detectChanges();
    expect(service.buscarPorId).toHaveBeenCalledTimes(2);
    expect(service.buscarPorId).toHaveBeenLastCalledWith(3);
    expect(fixture.nativeElement.textContent).toContain('Corretora Detalhada');
  });
});
