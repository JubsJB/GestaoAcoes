import { convertToParamMap } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import { describe, expect, it, vi, beforeEach } from 'vitest';

import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { SuccessToastService } from '../../shared/success-toast/success-toast.service';
import { CarteirasService } from '../carteiras/carteiras.service';
import { CarteiraResponse } from '../carteiras/models/carteira';
import { DashboardPageComponent } from './dashboard-page.component';
import { DashboardService } from './dashboard.service';
import { DashboardFinancialData } from './models/dashboard';
import { MatDialog } from '@angular/material/dialog';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideApiConfig } from '../../core/config/api.config';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';

const A: CarteiraResponse = { id: 1, nome: 'Principal', dataCriacao: '2026-01-01T10:00:00Z' };
const B: CarteiraResponse = { id: 2, nome: 'Exterior', dataCriacao: '2026-01-02T10:00:00Z' };
const DATA: DashboardFinancialData = {
  resumo: { carteiraId: 1, resumos: [
    { moeda: 'BRL', custoTotalPosicoes: '200.000000', patrimonioAtual: '241.000000', resultadoNaoRealizadoTotal: '0E-12', rentabilidadePercentual: '20.500000' },
    { moeda: 'USD', custoTotalPosicoes: '100.000000', patrimonioAtual: '90.000000', resultadoNaoRealizadoTotal: '-10.000000', rentabilidadePercentual: '-10.000000' }
  ] },
  posicoes: [{ acaoId: 4, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL', moeda: 'BRL', quantidadeAtual: '5.000000', precoMedio: '40.000000', custoPosicao: '200.000000', cotacaoAtual: '48.200000', dataHoraCotacao: '2026-09-03T10:00:00-03:00', valorAtualPosicao: '241.000000', resultadoNaoRealizado: '41.000000', rentabilidadePercentual: '20.500000' }],
  resultados: [{ acaoId: 8, ticker: 'AAPL', nomeEmpresa: 'Apple', mercado: 'EUA', moeda: 'USD', resultadoRealizado: '-50.123456' }]
};
const ERROR = { kind: 'standard', status: 422, code: 'CALCULO_POSICAO_FORA_DA_PRECISAO', message: 'Cálculo fora da precisão', details: {}, standardError: null, originalError: null } as unknown as NormalizedHttpError;

describe('DashboardPageComponent', () => {
  let query: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  beforeEach(() => query = new BehaviorSubject(convertToParamMap({})));

  async function create(portfolios$: any, financial?: Partial<Record<'resumo' | 'posicoes' | 'resultados', any>>) {
    const carteiras = { listar: vi.fn().mockReturnValue(portfolios$) };
    const dashboard = {
      obterResumo: vi.fn().mockReturnValue(financial?.resumo ?? of(DATA.resumo)),
      listarPosicoes: vi.fn().mockReturnValue(financial?.posicoes ?? of(DATA.posicoes)),
      listarResultadosRealizados: vi.fn().mockReturnValue(financial?.resultados ?? of(DATA.resultados))
    };
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(undefined) }) };
    await TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { queryParamMap: query } }, { provide: CarteirasService, useValue: carteiras }, { provide: DashboardService, useValue: dashboard }, { provide: MatDialog, useValue: dialog }, { provide: SuccessToastService, useValue: { show: vi.fn() } }]
    }).compileComponents();
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();
    return { fixture, carteiras, dashboard, router, dialog };
  }

  it('anuncia loading e permite retry após erro da lista', async () => {
    const pending = new Subject<CarteiraResponse[]>();
    const result = await create(pending);
    expect(result.fixture.nativeElement.textContent).toContain('Carregando carteiras');
    pending.error(ERROR); result.fixture.detectChanges();
    expect(result.fixture.nativeElement.textContent).toContain('Cálculo fora da precisão');
    result.carteiras.listar.mockReturnValueOnce(of([]));
    (result.fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); result.fixture.detectChanges();
    expect(result.carteiras.listar).toHaveBeenCalledTimes(2);
  });

  it('mostra empty state e não consulta finanças com zero carteiras', async () => {
    const { fixture, dashboard } = await create(of([]));
    expect(fixture.nativeElement.textContent).toContain('Nenhuma carteira cadastrada');
    expect(fixture.nativeElement.querySelector('a[href="/carteiras/nova"]')).toBeTruthy();
    expect(dashboard.obterResumo).not.toHaveBeenCalled();
  });

  it('seleciona automaticamente a única carteira, atualiza URL e carrega as três fontes', async () => {
    const { fixture, dashboard, router } = await create(of([A]));
    fixture.detectChanges();
    expect(router.navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { carteiraId: 1 }, replaceUrl: true }));
    expect(dashboard.obterResumo).toHaveBeenCalledWith(1);
    expect(dashboard.listarPosicoes).toHaveBeenCalledWith(1);
    expect(dashboard.listarResultadosRealizados).toHaveBeenCalledWith(1);
  });

  it('não escolhe arbitrariamente entre múltiplas carteiras', async () => {
    const { fixture, dashboard } = await create(of([A, B]));
    expect(fixture.nativeElement.textContent).toContain('Selecione uma carteira');
    expect(dashboard.obterResumo).not.toHaveBeenCalled();
  });

  it.each(['abc', '0', '-1', '99'])('trata query parameter inválido %s sem request financeiro', async value => {
    query.next(convertToParamMap({ carteiraId: value }));
    const { fixture, dashboard } = await create(of([A, B]));
    expect(fixture.nativeElement.textContent).toContain('não está disponível');
    expect(dashboard.obterResumo).not.toHaveBeenCalled();
  });

  it('restaura query válida e apresenta cards BRL/USD sem total combinado', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    const { fixture } = await create(of([A, B])); fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('R$ 241,00');
    expect(text).toContain('R$ 0,00');
    expect(text).toContain('Neutro');
    expect(text).not.toContain('0E-12 BRL');
    expect(text).toContain('US$ 90,00');
    expect(text).toContain('-US$ 10,00');
    expect(text).not.toContain('Patrimônio total');
  });

  it('exibe posições completas, contagem estrutural e resultados individuais', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    const { fixture } = await create(of([A])); fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('1 posição aberta');
    expect(text).toContain('PETR4'); expect(text).toContain('Preço médio'); expect(text).toContain('R$ 48,20');
    expect(text).toContain('AAPL'); expect(text).toContain('-US$ 50,12'); expect(text).toContain('Negativo');
    expect(fixture.nativeElement.querySelectorAll('.result')).toHaveLength(1);
  });

  it('trata coleções financeiras vazias como estados normais', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    const empty = { resumo: of({ carteiraId: 1, resumos: [] }), posicoes: of([]), resultados: of([]) };
    const { fixture } = await create(of([A]), empty); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Resumo ainda indisponível');
    expect(fixture.nativeElement.textContent).toContain('Nenhuma posição aberta');
    expect(fixture.nativeElement.textContent).toContain('Nenhum resultado realizado');
  });

  it('apresenta erro financeiro e retry refaz as três consultas', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    const { fixture, dashboard } = await create(of([A]), { resumo: throwError(() => ERROR) }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Cálculo fora da precisão');
    dashboard.obterResumo.mockReturnValueOnce(of(DATA.resumo));
    const retry = Array.from(fixture.nativeElement.querySelectorAll('button')).find((button: any) => button.textContent.includes('Tentar novamente')) as HTMLButtonElement;
    retry.click(); fixture.detectChanges();
    expect(dashboard.obterResumo).toHaveBeenCalledTimes(2);
  });

  it.each([
    [404, null, 'Carteira não encontrada'],
    [409, 'HISTORICO_OPERACOES_INCONSISTENTE', 'Histórico inconsistente'],
    [422, 'CALCULO_POSICAO_FORA_DA_PRECISAO', 'Cálculo fora da precisão']
  ])('apresenta erro financeiro %i normalizado', async (status, code, message) => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    const error = { ...ERROR, status, code, message } as NormalizedHttpError;
    const { fixture } = await create(of([A]), { resumo: throwError(() => error) }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(message);
    expect(fixture.nativeElement.textContent).toContain('Tentar novamente');
  });

  it('reload atualiza as três fontes da seleção corrente', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    const { fixture, dashboard } = await create(of([A])); fixture.detectChanges();
    const reload = Array.from(fixture.nativeElement.querySelectorAll('button')).find((button: any) => button.textContent.includes('Atualizar dados')) as HTMLButtonElement;
    reload.click(); fixture.detectChanges();
    expect(dashboard.obterResumo).toHaveBeenCalledTimes(2);
    expect(dashboard.listarPosicoes).toHaveBeenCalledTimes(2);
    expect(dashboard.listarResultadosRealizados).toHaveBeenCalledTimes(2);
  });

  it('cancela resposta obsoleta ao trocar rapidamente de carteira', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    const first = new Subject<any>();
    const second = new Subject<any>();
    const result = await create(of([A, B]), { resumo: first });
    result.dashboard.obterResumo.mockReturnValueOnce(second);
    query.next(convertToParamMap({ carteiraId: '2' })); result.fixture.detectChanges();
    first.next(DATA.resumo); first.complete(); result.fixture.detectChanges();
    expect(result.fixture.nativeElement.textContent).not.toContain('R$ 241,00');
    second.next({ ...DATA.resumo, carteiraId: 2 }); second.complete(); result.fixture.detectChanges();
    expect(result.fixture.nativeElement.textContent).toContain('R$ 241,00');
  });

  it('oferece navegação contextual e estrutura acessível sem detalhe fictício', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    const { fixture, dialog } = await create(of([A])); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toBe('Dashboard');
    expect(fixture.nativeElement.querySelector('mat-label')?.textContent).toContain('Carteira');
    expect(fixture.nativeElement.querySelector('a[href="/carteiras/1"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.record a')).toBeFalsy();
    const register = Array.from(fixture.nativeElement.querySelectorAll('button')).find((button: any) => button.textContent.includes('Registrar operação')) as HTMLButtonElement;
    register.click(); expect(dialog.open).toHaveBeenCalled();
  });

  it('integra HttpErrorResponse, interceptor, service e mensagem da UI', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [provideRouter([]), provideApiConfig(), provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { queryParamMap: query } },
        { provide: CarteirasService, useValue: { listar: () => of([A]) } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
        { provide: SuccessToastService, useValue: { show: vi.fn() } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/carteiras/1/resumo').flush('{"carteiraId":1,"resumos":[]}');
    http.expectOne('/api/carteiras/1/resultados-realizados').flush('[]');
    http.expectOne('/api/carteiras/1/posicoes').flush('{"timeStamp":1,"status":409,"error":"Conflict","message":"Histórico inconsistente","path":"/carteiras/1/posicoes","code":"HISTORICO_OPERACOES_INCONSISTENTE","details":{}}', { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Histórico inconsistente');
    http.verify();
  });
});
