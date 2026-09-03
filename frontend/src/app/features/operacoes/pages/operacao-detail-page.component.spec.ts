import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { OperacaoResponse } from '../models/operacao';
import { OperacoesService } from '../operacoes.service';
import { OperacaoDetailPageComponent } from './operacao-detail-page.component';

const usa: OperacaoResponse = { id: 7, carteiraId: 1, ticker: 'AAPL', mercado: 'EUA', moeda: 'USD', corretoraId: null, tipo: 'COMPRA', quantidade: '1.500000', precoUnitario: '40.126000', dataOperacao: '2026-09-01', ordemNoDia: 1, valorTotal: '60.189000000000' };
const brasil: OperacaoResponse = { ...usa, ticker: 'PETR4', mercado: 'BRASIL', moeda: 'BRL', tipo: 'VENDA', quantidade: '5.000000', precoUnitario: '40.000000', valorTotal: '200.000000000000' };

describe('OperacaoDetailPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  async function create(info?: OperacaoResponse, failure?: NormalizedHttpError) {
    const service = { buscarPorId: vi.fn().mockReturnValue(failure ? throwError(() => failure) : of(usa)) };
    await TestBed.configureTestingModule({ imports: [OperacaoDetailPageComponent], providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '7' } } } }, { provide: OperacoesService, useValue: service }] }).compileComponents();
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'currentNavigation').mockReturnValue(info ? ({ extras: { info: { operacao: info, origin: '/carteiras/1' } } } as never) : null);
    const fixture = TestBed.createComponent(OperacaoDetailPageComponent);
    fixture.detectChanges();
    return { fixture, service };
  }

  it('formata quantidade inteira e valores BRL sem alterar o DTO autoritativo', async () => {
    const original = structuredClone(brasil);
    const { fixture, service } = await create(brasil);
    const text = fixture.nativeElement.textContent;
    expect(service.buscarPorId).not.toHaveBeenCalled();
    expect(text).toContain('Quantidade5');
    expect(text).toContain('Preço negociadoR$ 40,00');
    expect(text).toContain('Valor totalR$ 200,00');
    expect(brasil).toEqual(original);
  });

  it('formata USD com símbolo e arredondamento somente visual', async () => {
    const { fixture } = await create(usa);
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Quantidade1,500000');
    expect(text).toContain('Preço negociadoUS$ 40,13');
    expect(text).toContain('Valor totalUS$ 60,19');
  });

  it('não recalcula valor total e não renderiza ordem no dia', async () => {
    const authoritative = { ...brasil, quantidade: '5.000000', precoUnitario: '40.000000', valorTotal: '999.000000000000', ordemNoDia: 77 };
    const { fixture } = await create(authoritative);
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Valor totalR$ 999,00');
    expect(text).not.toContain('R$ 200,00');
    expect(text).not.toContain('Ordem no dia');
    expect(text).not.toContain('77');
  });

  it('faz GET no reload', async () => {
    const { service } = await create();
    expect(service.buscarPorId).toHaveBeenCalledWith(7);
  });

  it('apresenta 404 dedicado', async () => {
    const error = { status: 404, message: 'Ausente', details: {}, code: null } as unknown as NormalizedHttpError;
    const { fixture } = await create(undefined, error);
    expect(fixture.nativeElement.textContent).toContain('Operação não encontrada');
    expect(fixture.nativeElement.textContent).toContain('Voltar para operações');
  });
});
