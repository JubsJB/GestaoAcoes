import { convertToParamMap } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import { describe, expect, it, vi, beforeEach } from 'vitest';

import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { SuccessToastService } from '../../shared/success-toast/success-toast.service';
import { CarteirasService } from '../carteiras/carteiras.service';
import { CarteiraResponse } from '../carteiras/models/carteira';
import { CARTEIRA_STORAGE, CarteiraContextService } from '../../core/carteira/carteira-context.service';
import { DashboardPageComponent } from './dashboard-page.component';
import { DashboardService } from './dashboard.service';
import { EvolutionService } from './evolution/evolution.service';
import { DashboardFinancialData } from './models/dashboard';
import { AtualizacaoCotacoesCarteiraResponse } from './models/atualizacao-cotacoes';
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
  posicoes: [{ acaoId: 4, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL', moeda: 'BRL', quantidadeAtual: '5.000000', precoMedio: '40.000000', custoPosicao: '200.000000', cotacaoAtual: '48.200000', dataHoraCotacao: '2026-09-03T10:00:00-03:00', referenciaMercadoCotacao: { tipo: 'DESCONHECIDA', data: null, instante: null }, cotacaoObtidaEm: null, valorAtualPosicao: '241.000000', resultadoNaoRealizado: '41.000000', rentabilidadePercentual: '20.500000' }],
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
    const evolution = {
      consultar: vi.fn().mockImplementation((id: number) => of({ carteiraId: id, pontos: [] })),
      registrarSnapshot: vi.fn()
    };
    await TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [{ provide: CARTEIRA_STORAGE, useValue: null }, provideRouter([]), { provide: ActivatedRoute, useValue: { queryParamMap: query } }, { provide: CarteirasService, useValue: carteiras }, { provide: DashboardService, useValue: dashboard }, { provide: EvolutionService, useValue: evolution }, { provide: MatDialog, useValue: dialog }, { provide: SuccessToastService, useValue: { show: vi.fn() } }]
    }).compileComponents();
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();
    return { fixture, carteiras, dashboard, evolution, router, dialog };
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

  it('seleciona a primeira por id ASC entre múltiplas carteiras', async () => {
    const { fixture, dashboard } = await create(of([A, B]));
    expect(fixture.nativeElement.textContent).toContain('Principal');
    expect(dashboard.obterResumo).toHaveBeenCalledWith(1);
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
    const groups = fixture.nativeElement.querySelectorAll('.currency-group');
    expect(groups).toHaveLength(2);
    expect(Array.from(groups).every(group => !(group as HTMLElement).classList.contains('currency-group--single'))).toBe(true);
  });

  it.each([
    ['BRL', 'USD'],
    ['USD', 'BRL']
  ] as const)('apresenta somente o grupo %s recebido sem criar %s', async (present, absent) => {
    const summary = { carteiraId: 1, resumos: DATA.resumo.resumos.filter(item => item.moeda === present) };
    const positions = DATA.posicoes.map(position => ({ ...position, moeda: present, mercado: present === 'BRL' ? 'BRASIL' : 'EUA' }));
    const { fixture } = await create(of([A]), { resumo: of(summary), posicoes: of(positions) });
    fixture.detectChanges();
    const groups = fixture.nativeElement.querySelectorAll('.currency-group');
    expect(groups).toHaveLength(1);
    expect(groups[0].querySelector('h3')?.textContent).toBe(present);
    expect(fixture.nativeElement.querySelector(`#currency-${absent}`)).toBeNull();
    expect(groups[0].classList).toContain('currency-group--single');
    expect(groups[0].querySelectorAll('mat-card')).toHaveLength(4);
  });

  it('usa toda a largura com quatro indicadores em linha e reflow responsivo para uma moeda', async () => {
    const summary = { carteiraId: 1, resumos: [DATA.resumo.resumos[0]] };
    const { fixture } = await create(of([A]), { resumo: of(summary) });
    fixture.detectChanges();
    const styles = (DashboardPageComponent as unknown as { ɵcmp: { styles: string[] } }).ɵcmp.styles.join('');
    const groups = fixture.nativeElement.querySelectorAll('.currency-group');
    expect(groups).toHaveLength(1);
    expect(groups[0].classList).toContain('currency-group--single');
    expect(styles).toMatch(/\.currency-group--single[^\{]*\{[^}]*grid-column:\s*1\s*\/\s*-1[^}]*justify-self:\s*stretch[^}]*width:\s*100%/);
    expect(styles).toMatch(/\.currency-group--single[^\{]*\.summary-grid[^\{]*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1\.4fr\)\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/);
    expect(styles).toMatch(/@container\s*\(max-width:\s*70rem\)[^{]*\{[^}]*\.summary-grid[^\{]*\{[^}]*grid-template-columns:\s*repeat\(2/);
    expect(styles).toMatch(/@container\s*\(max-width:\s*42rem\)[^{]*\{[^}]*\.summary-grid[^\{]*\{[^}]*grid-template-columns:\s*1fr/);
  });

  it('exibe análise resumida, contagem estrutural e resultados individuais sem tabela de posições', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    const { fixture } = await create(of([A])); fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('1 posição aberta');
    expect(text).toContain('PETR4'); expect(text).toContain('Resultado não realizado'); expect(text).toContain('20,50%');
    expect(text).not.toContain('Preço médio'); expect(fixture.nativeElement.querySelector('app-portfolio-positions')).toBeNull();
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
    expect(fixture.nativeElement.querySelector('.currency-groups')).toBeNull();
    expect(fixture.nativeElement.querySelector('.currency-group')).toBeNull();
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
    const { fixture, dashboard, evolution } = await create(of([A])); fixture.detectChanges();
    const reload = Array.from(fixture.nativeElement.querySelectorAll('button')).find((button: any) => button.textContent.includes('Atualizar dados')) as HTMLButtonElement;
    reload.click(); fixture.detectChanges();
    expect(dashboard.obterResumo).toHaveBeenCalledTimes(2);
    expect(dashboard.listarPosicoes).toHaveBeenCalledTimes(2);
    expect(dashboard.listarResultadosRealizados).toHaveBeenCalledTimes(2);
    expect(evolution.consultar).toHaveBeenCalledTimes(2);
    expect(evolution.registrarSnapshot).not.toHaveBeenCalled();
  });

  it('ordena resumo, evolução, distribuição, desempenho e resultados, com quatro indicadores por moeda e sem seletor local', async () => {
    const { fixture, dashboard, evolution, carteiras } = await create(of([A])); fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    expect(Array.from(root.querySelectorAll('h2')).map(heading => heading.id).filter(id => ['summary-title', 'evolution-title', 'portfolio-distribution-title', 'asset-performance-title', 'results-title'].includes(id)))
      .toEqual(['summary-title', 'evolution-title', 'portfolio-distribution-title', 'asset-performance-title', 'results-title']);
    expect(root.querySelector('app-position-analysis')?.textContent).not.toContain('Custo × Valor atual');
    expect(root.querySelector('select, mat-select, app-carteira-selector')).toBeNull();
    expect(root.querySelectorAll('.currency-group')).toHaveLength(2);
    root.querySelectorAll('.summary-grid').forEach(group => {
      expect(group.querySelectorAll('mat-card')).toHaveLength(4);
      expect(group.querySelector('.summary-primary')?.textContent).toContain('Patrimônio atual');
      expect(group.querySelector('.summary-result')?.textContent).toContain('Resultado não realizado');
      expect(group.querySelectorAll('.summary-supporting')).toHaveLength(2);
    });
    for (const selector of ['app-position-analysis', 'app-portfolio-composition', 'app-position-performance-chart', 'app-portfolio-evolution']) expect(root.querySelectorAll(selector)).toHaveLength(1);
    expect(root.querySelector('app-cost-value-chart')).toBeNull();
    const actions = root.querySelector('.dashboard-actions')!;
    expect(actions.querySelector('.portfolio-context')).not.toBeNull();
    for (const label of ['Ver carteira', 'Registrar operação', 'Atualizar dados']) expect(actions.textContent).toContain(label);
    expect(root.querySelector('app-portfolio-evolution')?.textContent).toContain('Registrar patrimônio atual');
    expect(evolution.consultar).toHaveBeenCalledTimes(1);
    expect(evolution.registrarSnapshot).not.toHaveBeenCalled();
    expect(root.querySelector('app-portfolio-positions')).toBeNull();
    expect(root.querySelectorAll('.result')).toHaveLength(1);
    expect(carteiras.listar).toHaveBeenCalledTimes(1);
    expect(dashboard.obterResumo).toHaveBeenCalledTimes(1);
    expect(dashboard.listarPosicoes).toHaveBeenCalledTimes(1);
    expect(dashboard.listarResultadosRealizados).toHaveBeenCalledTimes(1);
    expect(evolution.consultar).toHaveBeenCalledTimes(1);
    expect(evolution.registrarSnapshot).not.toHaveBeenCalled();
  });

  it('não remonta evolução quando indicadores terminam de carregar ou falham', async () => {
    const pending = new Subject<any>();
    const { fixture, dashboard, evolution } = await create(of([A]), { resumo: pending }); fixture.detectChanges();
    const component = fixture.nativeElement.querySelector('app-portfolio-evolution');
    expect(fixture.nativeElement.querySelector('app-position-analysis')).toBeNull();
    expect(component).toBeTruthy();
    expect(evolution.consultar).toHaveBeenCalledTimes(1);
    pending.next(DATA.resumo); pending.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-position-analysis')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('app-portfolio-evolution')).toBe(component);
    expect(evolution.consultar).toHaveBeenCalledTimes(1);
    dashboard.obterResumo.mockReturnValueOnce(throwError(() => ERROR));
    const reload = Array.from(fixture.nativeElement.querySelectorAll('button')).find((button: any) => button.textContent.includes('Atualizar dados')) as HTMLButtonElement;
    reload.click(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-portfolio-evolution')).toBe(component);
    expect(fixture.nativeElement.querySelector('app-position-analysis')).toBeNull();
    expect(evolution.consultar).toHaveBeenCalledTimes(2);
    expect(evolution.registrarSnapshot).not.toHaveBeenCalled();
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
    const { fixture, router } = await create(of([A])); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toBe('Dashboard');
    expect(fixture.nativeElement.querySelector('mat-select')).toBeNull();
    expect(fixture.nativeElement.querySelector('.portfolio-context')?.textContent).toContain('Principal');
    expect(fixture.nativeElement.querySelector('a[href="/carteiras/1"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.record a')).toBeFalsy();
    const register = Array.from(fixture.nativeElement.querySelectorAll('button')).find((button: any) => button.textContent.includes('Registrar operação')) as HTMLButtonElement;
    await fixture.componentInstance['openOperationDialog'](); expect(TestBed.inject(MatDialog).open).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({ data: { carteira: A }, restoreFocus: true })); expect(router.navigate).not.toHaveBeenCalled();
  });

  it('dialog preserva origem e nao recarrega outra carteira no sucesso', async () => {
    query.next(convertToParamMap({carteiraId:'1'}));
    const {fixture,dashboard,evolution}=await create(of([A,B]));
    const closed=new Subject<unknown>(); const open=vi.mocked(TestBed.inject(MatDialog).open);
    open.mockReturnValue({afterClosed:()=>closed} as never);
    await fixture.componentInstance['openOperationDialog']();
    query.next(convertToParamMap({carteiraId:'2'})); fixture.detectChanges();
    const count=dashboard.obterResumo.mock.calls.length;
    closed.next({id:99,carteiraId:1}); closed.complete();
    expect(open).toHaveBeenCalledWith(expect.anything(),expect.objectContaining({data:{carteira:A}}));
    expect(dashboard.obterResumo).toHaveBeenCalledTimes(count);
    expect(evolution.registrarSnapshot).not.toHaveBeenCalled();
  });

  it('sucesso atualiza tres consultas da origem sem snapshots e impede abertura dupla', async () => {
    query.next(convertToParamMap({carteiraId:'1'}));
    const {fixture,dashboard,evolution}=await create(of([A])); const closed=new Subject<unknown>();
    const open=vi.mocked(TestBed.inject(MatDialog).open); open.mockReturnValue({afterClosed:()=>closed} as never);
    await Promise.all([fixture.componentInstance['openOperationDialog'](),fixture.componentInstance['openOperationDialog']()]);
    expect(open).toHaveBeenCalledTimes(1); closed.next({id:99,carteiraId:1}); closed.complete();
    expect(dashboard.obterResumo).toHaveBeenCalledTimes(2); expect(dashboard.listarPosicoes).toHaveBeenCalledTimes(2);
    expect(dashboard.listarResultadosRealizados).toHaveBeenCalledTimes(2); expect(evolution.consultar).toHaveBeenCalledTimes(1);
    expect(evolution.registrarSnapshot).not.toHaveBeenCalled();
  });

  it('remove a análise anterior durante troca de contexto e preserva histórico mesmo sem posições atuais', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    const { fixture, dashboard, evolution } = await create(of([A, B]));
    expect(fixture.nativeElement.querySelector('app-position-analysis')?.textContent).toContain('PETR4');
    const pendingPositions = new Subject<DashboardFinancialData['posicoes']>();
    dashboard.listarPosicoes.mockReturnValueOnce(pendingPositions);
    evolution.consultar.mockReturnValueOnce(of({ carteiraId: 2, pontos: [
      { snapshotId: 1, dataHoraSnapshot: '2026-09-08T10:00:00Z', patrimonios: [{ moeda: 'USD', patrimonioAtual: '345.67' }] }
    ] }));
    query.next(convertToParamMap({ carteiraId: '2' })); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-position-analysis')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-portfolio-evolution')?.textContent).toContain('US$ 345,67');
    pendingPositions.next([]); pendingPositions.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-position-analysis')?.textContent).toContain('Nenhuma posição aberta para comparar');
    expect(fixture.nativeElement.querySelector('app-position-analysis')?.textContent).not.toContain('PETR4');
    expect(fixture.nativeElement.querySelector('app-portfolio-evolution')?.textContent).toContain('US$ 345,67');
    expect(fixture.nativeElement.querySelector('app-portfolio-evolution')?.textContent).toContain('Registros do patrimônio');
    expect(evolution.registrarSnapshot).not.toHaveBeenCalled();
  });

  it('preserva quatro GETs por carga/reload e evolução independente sem POST automático', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    await TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [{ provide: CARTEIRA_STORAGE, useValue: null }, provideRouter([]), provideApiConfig(), provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { queryParamMap: query } },
        { provide: CarteirasService, useValue: { listar: () => of([A]) } },
        { provide: SuccessToastService, useValue: { show: vi.fn() } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(DashboardPageComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    const requests = http.match(() => true);
    expect(requests.map(request => [request.request.method, request.request.url]).sort()).toEqual([
      ['GET', '/api/carteiras/1/evolucao-patrimonial'], ['GET', '/api/carteiras/1/posicoes'],
      ['GET', '/api/carteiras/1/resultados-realizados'], ['GET', '/api/carteiras/1/resumo']
    ]);
    const evolution = requests.find(request => request.request.url.endsWith('/evolucao-patrimonial'))!;
    requests.find(request => request.request.url.endsWith('/resumo'))!.flush(JSON.stringify(DATA.resumo));
    requests.find(request => request.request.url.endsWith('/posicoes'))!.flush(JSON.stringify(DATA.posicoes));
    requests.find(request => request.request.url.endsWith('/resultados-realizados'))!.flush('[]');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('R$ 241,00');
    expect(fixture.nativeElement.textContent).toContain('Carregando histórico do patrimônio');
    expect(fixture.nativeElement.querySelector('app-position-analysis')?.textContent).toContain('PETR4');
    expect(fixture.nativeElement.querySelectorAll('app-position-analysis .currency')).toHaveLength(1);
    expect(fixture.nativeElement.querySelectorAll('app-position-performance-chart')).toHaveLength(1);
    expect(fixture.nativeElement.querySelectorAll('app-position-performance-chart li')).toHaveLength(1);
    evolution.flush('falha', { status: 500, statusText: 'Erro' }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('R$ 241,00');
    http.expectNone(() => true);
    const reload = Array.from(fixture.nativeElement.querySelectorAll('button')).find((button: any) => button.textContent.includes('Atualizar dados')) as HTMLButtonElement;
    reload.click(); fixture.detectChanges();
    const next = http.match(() => true);
    expect(next.map(request => request.request.url).sort()).toEqual(requests.map(request => request.request.url).sort());
    expect(next.every(request => request.request.method === 'GET')).toBe(true);
    for (const request of next) {
      const url = request.request.url;
      request.flush(url.endsWith('/resumo') ? JSON.stringify(DATA.resumo) : url.endsWith('/evolucao-patrimonial') ? '{"carteiraId":1,"pontos":[]}' : '[]');
    }
    fixture.detectChanges();
    http.verify();
  });

  function quoteResponse(id: number): AtualizacaoCotacoesCarteiraResponse {
    return { carteiraId: id, resumo: { atualizadas: 0, semNovaCotacao: 1, falhas: 0 }, resultados: [{
      acaoId: 4, ticker: 'PETR4', mercado: 'BRASIL', resultado: 'SEM_NOVA_COTACAO', motivo: 'MESMA_OBSERVACAO',
      cotacao: { cotacaoAtual: '1234567890123.000001',
        referenciaMercadoCotacao: { tipo: 'INSTANTE', data: null, instante: '2026-10-04T10:20:30.123456Z' },
        cotacaoObtidaEm: '2026-10-04T10:21:30.999999Z', dataHoraCotacao: '2026-10-04T10:20:30.123456Z' }, erro: null
    }] };
  }

  async function quotePage(id = '1') {
    query.next(convertToParamMap({ carteiraId: id }));
    await TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [{ provide: CARTEIRA_STORAGE, useValue: null }, provideRouter([]), provideApiConfig(),
        provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { queryParamMap: query } },
        { provide: CarteirasService, useValue: { listar: () => of([A, B]) } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
        { provide: SuccessToastService, useValue: { show: vi.fn() } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(DashboardPageComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    const page = fixture.componentInstance as any;
    const button = () => Array.from(fixture.nativeElement.querySelectorAll('button'))
      .find((item: any) => /Atualiza(?:r|ndo) cotações/.test(item.textContent)) as HTMLButtonElement | undefined;
    function load(portfolioId: number, positions = DATA.posicoes) {
      http.expectOne(`/api/carteiras/${portfolioId}/resumo`).flush(JSON.stringify({ ...DATA.resumo, carteiraId: portfolioId }));
      http.expectOne(`/api/carteiras/${portfolioId}/posicoes`).flush(JSON.stringify(positions));
      http.expectOne(`/api/carteiras/${portfolioId}/resultados-realizados`).flush('[]');
      http.expectOne(`/api/carteiras/${portfolioId}/evolucao-patrimonial`).flush(JSON.stringify({ carteiraId: portfolioId, pontos: [] }));
      fixture.detectChanges();
    }
    return { fixture, http, page, button, load };
  }

  it('bloqueia chamada direta sem carteira válida e não envia POST', async () => {
    const result = await quotePage('99');
    expect(result.button()).toBeUndefined();
    result.page.atualizarCotacoes();
    expect(result.page.quoteAttempt()).toBeNull();
    result.http.expectNone(() => true); result.http.verify();
  });

  it('mantém botão visível e disabled com explicação acessível enquanto conjunto é desconhecido ou vazio', async () => {
    const result = await quotePage();
    expect(result.button()!.type).toBe('button');
    expect(result.button()!.disabled).toBe(true);
    expect(result.button()!.getAttribute('aria-describedby')).toBe('quote-update-status');
    expect(result.fixture.nativeElement.querySelector('#quote-update-status').textContent).toContain('carregamento das posições');
    result.page.atualizarCotacoes(); result.http.expectNone(request => request.method === 'POST');
    result.load(1, []);
    expect(result.button()!.disabled).toBe(true);
    expect(result.fixture.nativeElement.querySelector('#quote-update-status').textContent).toContain('não possui posições abertas');
    result.page.atualizarCotacoes(); result.http.expectNone(() => true); result.http.verify();
  });

  it('guarda dois disparos síncronos antes da renderização, anuncia busy e permite nova tentativa após sucesso', async () => {
    const result = await quotePage(); result.load(1);
    expect(result.button()!.disabled).toBe(false);
    result.button()!.click(); result.button()!.click(); result.page.atualizarCotacoes();
    const post = result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes');
    expect(post.request.method).toBe('POST'); expect(post.request.serializeBody()).toBeNull();
    expect(result.page.quoteUpdating()).toBe(true);
    expect(result.page.financialLoading()).toBe(false);
    expect(result.page.quoteAttempt().origem).toEqual({ carteiraId: 1, nome: 'Principal' });
    expect(Object.isFrozen(result.page.quoteAttempt().origem)).toBe(true);
    result.fixture.detectChanges();
    expect(result.button()!.disabled).toBe(true); expect(result.button()!.getAttribute('aria-busy')).toBe('true');
    expect(result.button()!.textContent).toContain('Atualizando cotações');
    expect(result.fixture.nativeElement.querySelector('#quote-update-status').getAttribute('aria-live')).toBe('polite');
    const response = quoteResponse(1);
    post.flush(JSON.stringify(response)); result.fixture.detectChanges();
    expect(result.page.quoteUpdating()).toBe(false); expect(result.button()!.disabled).toBe(true);
    expect(result.page.quoteAttempt().resultado).toEqual(response);
    expect(result.page.data()).toBeNull();
    result.load(1);
    expect(result.button()!.disabled).toBe(false);
    expect(result.page.data().posicoes).toEqual(DATA.posicoes);
    result.http.expectNone(() => true);
    result.page.atualizarCotacoes();
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').flush(JSON.stringify(response));
    result.fixture.detectChanges(); result.load(1);
    result.http.verify();
  });

  it.each([400, 404, 409, 422, 500])('libera busy após erro HTTP %s, preserva erro da F1 e permite nova ação manual sem retry', async status => {
    const result = await quotePage(); result.load(1);
    result.page.atualizarCotacoes();
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').flush(JSON.stringify({
      timeStamp: 1, status, error: 'Erro', message: 'Mensagem pública do lote', path: '/carteiras/1/atualizacoes-cotacoes',
      code: status === 500 ? 'FALHA_ATUALIZACAO_COTACOES' : `ERRO_${status}`, details: {}
    }), { status, statusText: 'Erro' });
    result.fixture.detectChanges();
    expect(result.page.quoteUpdating()).toBe(false); expect(result.button()!.disabled).toBe(false);
    expect(result.page.quoteAttempt().estado).toBe('falha');
    expect(result.page.quoteAttempt().erro).toMatchObject({ kind: 'standard', status, message: 'Mensagem pública do lote' });
    expect(result.page.financialError()).toBeNull();
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent).toContain('Mensagem pública do lote');
    result.http.expectNone(() => true);
    result.page.atualizarCotacoes();
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').flush(JSON.stringify(quoteResponse(1)));
    result.fixture.detectChanges(); result.load(1);
    result.http.verify();
  });

  it.each(['sucesso', 'erro'])('troca A → B mantém POST de A e isola sua conclusão por %s', async outcome => {
    const result = await quotePage(); result.load(1);
    result.page.atualizarCotacoes();
    const post = result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes');
    TestBed.inject(CarteiraContextService).upsert({ ...A, nome: 'Nome posterior' });
    const positionsB: DashboardFinancialData['posicoes'] = [{ ...DATA.posicoes[0],
      acaoId: 88, ticker: 'AAPL', mercado: 'EUA', moeda: 'USD', cotacaoAtual: '77.123456' }];
    query.next(convertToParamMap({ carteiraId: '2' })); result.fixture.detectChanges(); result.load(2, positionsB);
    const dataB = result.page.data();
    expect(post.cancelled).toBe(false); expect(result.button()!.disabled).toBe(true);
    expect(result.page.quoteAttempt().origem).toEqual({ carteiraId: 1, nome: 'Principal' });
    result.page.atualizarCotacoes(); result.http.expectNone(request => request.method === 'POST');
    if (outcome === 'sucesso') post.flush(JSON.stringify(quoteResponse(1)));
    else post.flush('Falha de comunicação', { status: 500, statusText: 'Erro' });
    result.fixture.detectChanges();
    expect(result.page.quoteUpdating()).toBe(false);
    if (outcome === 'sucesso') {
      expect(result.button()!.disabled).toBe(true);
      expect(result.page.data()).toBeNull();
      result.http.expectNone(request => request.method === 'GET' && request.url.startsWith('/api/carteiras/1/'));
      result.load(2, positionsB);
    } else expect(result.page.data()).toBe(dataB);
    expect(result.button()!.disabled).toBe(false);
    expect(result.page.quoteAttempt().origem.carteiraId).toBe(1);
    expect(result.page.quoteAttempt().estado).toBe(outcome === 'sucesso' ? 'concluida' : 'falha');
    expect(result.page.data().resumo.carteiraId).toBe(2);
    expect(result.page.data().posicoes).toEqual(positionsB);
    expect(result.page.financialError()).toBeNull();
    expect(result.fixture.nativeElement.textContent).toContain('Exterior');
    expect(result.fixture.nativeElement.textContent).not.toContain('Falha de comunicação');
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent).toContain('Principal (ID 1)');
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent).not.toContain('Nome posterior');
    result.http.expectNone(() => true);
    result.page.atualizarCotacoes();
    result.http.expectOne('/api/carteiras/2/atualizacoes-cotacoes').flush(JSON.stringify(quoteResponse(2)));
    result.fixture.detectChanges(); result.load(2);
    result.http.verify();
  });

  it('reload preserva os quatro GETs independentes sem cancelar nem repetir o POST', async () => {
    const result = await quotePage(); result.load(1);
    result.page.atualizarCotacoes();
    const post = result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes');
    const reload = Array.from(result.fixture.nativeElement.querySelectorAll('button'))
      .find((item: any) => item.textContent.includes('Atualizar dados')) as HTMLButtonElement;
    expect(reload.disabled).toBe(false); reload.click(); result.fixture.detectChanges();
    expect(post.cancelled).toBe(false); expect(result.page.quoteUpdating()).toBe(true);
    result.load(1);
    expect(result.page.quoteUpdating()).toBe(true);
    post.flush(JSON.stringify(quoteResponse(1))); result.fixture.detectChanges();
    result.load(1);
    result.http.expectNone(() => true); result.http.verify();
  });

  it('troca durante GET de reload cancela apresentação antiga mas conserva POST de A', async () => {
    const result = await quotePage(); result.load(1);
    result.page.atualizarCotacoes();
    const post = result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes');
    result.page.reload(); result.fixture.detectChanges();
    const oldGets = result.http.match(request => request.method === 'GET');
    expect(oldGets).toHaveLength(4);
    query.next(convertToParamMap({ carteiraId: '2' })); result.fixture.detectChanges();
    expect(oldGets.every(request => request.cancelled)).toBe(true);
    expect(post.cancelled).toBe(false);
    result.load(2);
    post.flush(JSON.stringify(quoteResponse(1)));
    result.fixture.detectChanges();
    expect(result.page.data()).toBeNull(); expect(result.page.quoteAttempt().origem.carteiraId).toBe(1);
    result.load(2);
    expect(result.page.data().resumo.carteiraId).toBe(2);
    result.http.expectNone(() => true); result.http.verify();
  });

  it('falha de conexão libera busy sem sucesso artificial, retry ou erro nos indicadores', async () => {
    const result = await quotePage(); result.load(1);
    result.page.atualizarCotacoes();
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').error(new ProgressEvent('error'));
    result.fixture.detectChanges();
    expect(result.page.quoteUpdating()).toBe(false); expect(result.button()!.disabled).toBe(false);
    expect(result.page.quoteAttempt()).toMatchObject({ estado: 'falha', origem: { carteiraId: 1 }, erro: { kind: 'technical', status: 0 } });
    expect(result.page.financialError()).toBeNull();
    result.http.expectNone(() => true);
    result.page.atualizarCotacoes();
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').flush(JSON.stringify(quoteResponse(1)));
    result.fixture.detectChanges(); result.load(1);
    result.http.verify();
  });

  it('erro de GET mantém conjunto desconhecido e bloqueia POST até recarga válida', async () => {
    const result = await quotePage();
    result.http.expectOne('/api/carteiras/1/resumo').flush('Falha', { status: 500, statusText: 'Erro' });
    result.http.expectOne('/api/carteiras/1/posicoes');
    result.http.expectOne('/api/carteiras/1/resultados-realizados');
    result.http.expectOne('/api/carteiras/1/evolucao-patrimonial').flush('{"carteiraId":1,"pontos":[]}');
    result.fixture.detectChanges();
    expect(result.button()!.disabled).toBe(true);
    expect(result.fixture.nativeElement.querySelector('#quote-update-status').textContent).toContain('Use Atualizar dados');
    expect(result.fixture.nativeElement.querySelector('#quote-update-status').textContent).not.toContain('Aguarde');
    result.page.atualizarCotacoes(); result.http.expectNone(request => request.method === 'POST');
    result.page.reload(); result.fixture.detectChanges(); result.load(1);
    expect(result.button()!.disabled).toBe(false); result.http.verify();
  });

  it('destruição da página encerra subscription própria sem alterar dados financeiros', async () => {
    const result = await quotePage(); result.load(1);
    result.page.atualizarCotacoes();
    const post = result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes');
    result.fixture.destroy();
    expect(post.cancelled).toBe(true); result.http.verify();
  });

  function batchResponse(categories: ('ATUALIZADA' | 'SEM_NOVA_COTACAO' | 'FALHA')[]): AtualizacaoCotacoesCarteiraResponse {
    const sample = quoteResponse(1).resultados[0];
    return {
      carteiraId: 1,
      resumo: {
        atualizadas: categories.filter(value => value === 'ATUALIZADA').length,
        semNovaCotacao: categories.filter(value => value === 'SEM_NOVA_COTACAO').length,
        falhas: categories.filter(value => value === 'FALHA').length
      },
      resultados: categories.map((resultado, index) => ({
        ...sample, acaoId: index + 1, ticker: ['PETR4', 'AAPL', 'MSFT'][index], resultado,
        erro: resultado === 'FALHA' ? { code: 'FALHA_CONSULTA', message: 'Não foi possível obter a cotação.' } : null
      })) as AtualizacaoCotacoesCarteiraResponse['resultados']
    };
  }

  it.each([
    { categories: ['ATUALIZADA', 'ATUALIZADA'], variant: 'success', title: 'Cotações atualizadas com sucesso.', counts: '2 atualizadas; 0 sem nova cotação; 0 com falha.', failed: '' },
    { categories: ['SEM_NOVA_COTACAO', 'SEM_NOVA_COTACAO'], variant: 'info', title: 'Nenhuma nova cotação.', counts: '0 atualizadas; 2 sem nova cotação; 0 com falha.', failed: '' },
    { categories: ['ATUALIZADA', 'SEM_NOVA_COTACAO', 'FALHA'], variant: 'warning', title: 'Resultado parcial.', counts: '1 atualizadas; 1 sem nova cotação; 1 com falha.', failed: 'MSFT' },
    { categories: ['ATUALIZADA', 'SEM_NOVA_COTACAO'], variant: 'warning', title: 'Resultado parcial.', counts: '1 atualizadas; 1 sem nova cotação; 0 com falha.', failed: '' },
    { categories: ['FALHA', 'FALHA'], variant: 'error', title: 'Todas as Ações falharam individualmente.', counts: '0 atualizadas; 0 sem nova cotação; 2 com falha.', failed: 'PETR4, AAPL' },
    { categories: [], variant: 'info', title: 'Nenhuma posição aberta foi processada.', counts: '0 atualizadas; 0 sem nova cotação; 0 com falha.', failed: '' }
  ] as const)('apresenta $title com contadores do POST e recarrega quatro GETs sem apagar feedback', async scenario => {
    const result = await quotePage(); result.load(1);
    result.page.atualizarCotacoes();
    const response = batchResponse([...scenario.categories]);
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').flush(JSON.stringify(response));
    result.fixture.detectChanges();
    const feedback = result.fixture.nativeElement.querySelector('.quote-update-feedback');
    const message = feedback.textContent;
    expect(message).toContain('Principal (ID 1)');
    expect(message).toContain(scenario.title); expect(message).toContain(scenario.counts);
    if (scenario.failed) expect(message).toContain(`Ativos com falha: ${scenario.failed}.`);
    else expect(message).not.toContain('Ativos com falha');
    const announcement = feedback.querySelector('section');
    expect(announcement.classList.contains(`feedback-alert--${scenario.variant}`)).toBe(true);
    expect(announcement.getAttribute('role')).toBe('status');
    expect(announcement.getAttribute('aria-live')).toBe('polite');
    expect(result.fixture.nativeElement.querySelectorAll('.quote-update-feedback')).toHaveLength(1);
    expect(result.page.financialError()).toBeNull();
    expect(result.page.quoteUpdating()).toBe(false); expect(result.button()!.getAttribute('aria-busy')).toBe('false');
    expect(result.button()!.disabled).toBe(true);
    expect(result.fixture.nativeElement.querySelector('#quote-update-status').textContent).toContain('Aguarde o carregamento');
    result.http.expectNone(request => request.method === 'POST');
    result.load(1, scenario.categories.length === 0 ? [] : DATA.posicoes);
    expect(feedback.textContent).toBe(message);
    expect(result.page.quoteAttempt().resultado).toEqual(response);
    result.http.expectNone(() => true); result.http.verify();
  });

  it('preserva snapshot lossless congelado sem transformar preços, referências, offsets ou ordem no feedback', async () => {
    const result = await quotePage(); result.load(1); result.page.atualizarCotacoes();
    const response = batchResponse(['ATUALIZADA', 'SEM_NOVA_COTACAO', 'FALHA']);
    response.resultados[0].cotacao = { cotacaoAtual: '9999999999999.123456',
      referenciaMercadoCotacao: { tipo: 'DATA', data: '2026-10-02', instante: null },
      cotacaoObtidaEm: '2026-10-04T07:20:30.123456-03:00', dataHoraCotacao: '2026-10-04T07:20:30.123456-03:00' };
    response.resultados[1].cotacao = { ...quoteResponse(1).resultados[0].cotacao,
      referenciaMercadoCotacao: { tipo: 'INSTANTE', data: null, instante: '2026-10-04T07:20:30.999999-03:00' } };
    response.resultados[2].cotacao = { cotacaoAtual: '0.123456',
      referenciaMercadoCotacao: { tipo: 'DESCONHECIDA', data: null, instante: null },
      cotacaoObtidaEm: null, dataHoraCotacao: '2026-10-04T10:20:30Z' };
    // Números monetários reais do JSON HTTP, sem conversão para number no fixture.
    const json = JSON.stringify(response).replace(/"cotacaoAtual":"([0-9.]+)"/g, '"cotacaoAtual":$1');
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').flush(json);
    const stored = result.page.quoteAttempt().resultado;
    function freeze(value: any): void {
      if (value && typeof value === 'object') { Object.values(value).forEach(freeze); Object.freeze(value); }
    }
    freeze(stored);
    const before = JSON.stringify(stored);
    result.fixture.detectChanges(); result.load(1);
    expect(result.page.quoteAttempt().resultado).toBe(stored);
    expect(JSON.stringify(stored)).toBe(before); expect(stored).toEqual(response);
    expect(stored.resultados.map((item: any) => item.acaoId)).toEqual([1, 2, 3]);
    expect(stored.resultados.map((item: any) => typeof item.cotacao.cotacaoAtual)).toEqual(['string', 'string', 'string']);
    const feedback = result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent;
    expect(feedback).not.toContain('9999999999999'); expect(feedback).not.toContain('2026-10');
    result.http.verify();
  });

  it('mostra somente a mensagem pública do erro global sem detalhes internos nem recarga automática', async () => {
    const result = await quotePage(); result.load(1); result.page.atualizarCotacoes();
    const publicMessage = 'Não foi possível concluir o lote. Atualizações já confirmadas podem ter sido preservadas.';
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').flush(JSON.stringify({
      timeStamp: 1, status: 500, error: 'Internal Server Error', message: publicMessage,
      path: '/carteiras/1/atualizacoes-cotacoes', code: 'FALHA_ATUALIZACAO_COTACOES',
      details: { sql: 'SELECT segredo', stackTrace: 'java.internal.Causa', payload: 'TOKEN_EXTERNO' },
      cause: 'CAUSE_INTERNA'
    }), { status: 500, statusText: 'Erro' });
    result.fixture.detectChanges();
    const feedback = result.fixture.nativeElement.querySelector('.quote-update-feedback');
    expect(feedback.textContent).toContain(publicMessage); expect(feedback.textContent).toContain('Principal (ID 1)');
    expect(feedback.querySelector('dl')).toBeNull();
    for (const secret of ['SELECT segredo', 'java.internal.Causa', 'TOKEN_EXTERNO', 'CAUSE_INTERNA', 'originalError']) {
      expect(result.fixture.nativeElement.textContent).not.toContain(secret);
    }
    expect(result.page.quoteAttempt().erro.details.sql).toBe('SELECT segredo');
    expect(result.page.financialError()).toBeNull(); expect(result.button()!.disabled).toBe(false);
    result.http.expectNone(() => true); result.http.verify();
  });

  it.each(['conexao', 'abortada', 'resposta-invalida'])('informa resultado não confirmado em %s sem prometer rollback ou repetir POST', async failure => {
    const result = await quotePage(); result.load(1); result.page.atualizarCotacoes();
    const post = result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes');
    if (failure !== 'resposta-invalida') post.error(new ProgressEvent(failure === 'abortada' ? 'abort' : 'error'));
    else post.flush('{"payloadInterno":"SQL_PRIVADO"}');
    result.fixture.detectChanges();
    const message = result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent;
    expect(message).toContain('Resultado não confirmado');
    expect(message).toContain('Atualizações já confirmadas podem ter sido preservadas');
    expect(message).toContain('Use Atualizar dados'); expect(message).not.toContain('SQL_PRIVADO');
    expect(message).not.toContain('cancelado'); expect(message).not.toContain('rollback');
    expect(result.page.quoteAttempt().estado).toBe('falha');
    result.http.expectNone(() => true);
    result.page.reload(); result.fixture.detectChanges(); result.load(1);
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent).toBe(message);
    result.http.expectNone(request => request.method === 'POST'); result.http.verify();
  });

  it('distingue falha da recarga pós-lote e mantém resultado mesmo ao recuperar dados manualmente', async () => {
    const result = await quotePage(); result.load(1); result.page.atualizarCotacoes();
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').flush(JSON.stringify(batchResponse(['ATUALIZADA'])));
    result.fixture.detectChanges();
    const attempt = result.page.quoteAttempt();
    const message = result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent;
    result.http.expectOne('/api/carteiras/1/resumo').flush('Falha', { status: 500, statusText: 'Erro' });
    expect(result.http.expectOne('/api/carteiras/1/posicoes').cancelled).toBe(true);
    expect(result.http.expectOne('/api/carteiras/1/resultados-realizados').cancelled).toBe(true);
    result.http.expectOne('/api/carteiras/1/evolucao-patrimonial').flush('{"carteiraId":1,"pontos":[]}');
    result.fixture.detectChanges();
    expect(result.page.quoteAttempt()).toBe(attempt);
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent).toBe(message);
    expect(result.fixture.nativeElement.textContent).toContain('Não foi possível recarregar os indicadores');
    expect(result.fixture.nativeElement.textContent).toContain('Principal (ID 1) foi preservado');
    expect(result.button()!.disabled).toBe(true);
    expect(result.fixture.nativeElement.querySelector('#quote-update-status').textContent).toContain('Use Atualizar dados');
    result.http.expectNone(() => true);
    result.page.reload(); result.fixture.detectChanges(); result.load(1);
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent).toBe(message);
    expect(result.fixture.nativeElement.textContent).not.toContain('Não foi possível recarregar os indicadores');
    expect(result.button()!.disabled).toBe(false); result.http.verify();
  });

  it('mantém conclusão do lote quando apenas a evolução falha na recarga', async () => {
    const result = await quotePage(); result.load(1); result.page.atualizarCotacoes();
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').flush(JSON.stringify(quoteResponse(1)));
    result.fixture.detectChanges();
    result.http.expectOne('/api/carteiras/1/resumo').flush(JSON.stringify(DATA.resumo));
    result.http.expectOne('/api/carteiras/1/posicoes').flush(JSON.stringify(DATA.posicoes));
    result.http.expectOne('/api/carteiras/1/resultados-realizados').flush('[]');
    result.http.expectOne('/api/carteiras/1/evolucao-patrimonial').flush('Falha', { status: 500, statusText: 'Erro' });
    result.fixture.detectChanges();
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent).toContain('Nenhuma nova cotação');
    expect(result.fixture.nativeElement.querySelector('app-portfolio-evolution').textContent).toContain('Falha técnica na comunicação HTTP.');
    expect(result.fixture.nativeElement.querySelector('app-portfolio-evolution').textContent).toContain('Tentar novamente o histórico');
    expect(result.page.financialError()).toBeNull(); expect(result.page.data()).not.toBeNull();
    result.http.expectNone(() => true); result.http.verify();
  });

  it('conclusão de A com GETs de B pendentes recarrega somente B e nova seleção cancela somente GETs', async () => {
    const result = await quotePage(); result.load(1); result.page.atualizarCotacoes();
    const post = result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes');
    query.next(convertToParamMap({ carteiraId: '2' })); result.fixture.detectChanges();
    const pendingB = result.http.match(request => request.method === 'GET');
    expect(pendingB).toHaveLength(4); expect(post.cancelled).toBe(false);
    post.flush(JSON.stringify(quoteResponse(1))); result.fixture.detectChanges();
    expect(pendingB.every(request => request.cancelled)).toBe(true);
    const reloadB = result.http.match(request => request.method === 'GET');
    expect(reloadB.map(request => request.request.url).sort()).toEqual([
      '/api/carteiras/2/evolucao-patrimonial', '/api/carteiras/2/posicoes',
      '/api/carteiras/2/resultados-realizados', '/api/carteiras/2/resumo'
    ]);
    expect(result.page.selected().id).toBe(2);
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent).toContain('Principal (ID 1)');
    query.next(convertToParamMap({ carteiraId: '1' })); result.fixture.detectChanges();
    expect(reloadB.every(request => request.cancelled)).toBe(true);
    result.load(1); expect(result.page.data().resumo.carteiraId).toBe(1);
    expect(result.page.quoteAttempt().origem.carteiraId).toBe(1);
    result.http.expectNone(() => true); result.http.verify();
  });

  it('mantém feedback da origem mesmo sem seleção válida na conclusão e não reativa carteira antiga', async () => {
    const result = await quotePage(); result.load(1); result.page.atualizarCotacoes();
    const post = result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes');
    query.next(convertToParamMap({ carteiraId: '99' })); result.fixture.detectChanges();
    expect(post.cancelled).toBe(false);
    post.flush(JSON.stringify(quoteResponse(1))); result.fixture.detectChanges();
    expect(result.page.selected()).toBeNull(); expect(result.page.data()).toBeNull();
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent).toContain('Principal (ID 1)');
    expect(result.page.quoteUpdating()).toBe(false); result.http.expectNone(() => true); result.http.verify();
  });

  it('nova tentativa manual substitui feedback anterior sem apagá-lo em reload e sem POST automático', async () => {
    const result = await quotePage(); result.load(1); result.page.atualizarCotacoes();
    result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes').flush(JSON.stringify(quoteResponse(1)));
    result.fixture.detectChanges(); result.load(1);
    const attempt = result.page.quoteAttempt();
    result.page.reload(); result.fixture.detectChanges(); result.load(1);
    expect(result.page.quoteAttempt()).toBe(attempt);
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback')).not.toBeNull();
    result.http.expectNone(() => true);
    result.page.atualizarCotacoes(); result.page.atualizarCotacoes(); result.fixture.detectChanges();
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback')).toBeNull();
    const post = result.http.expectOne('/api/carteiras/1/atualizacoes-cotacoes');
    expect(post.request.serializeBody()).toBeNull();
    post.flush(JSON.stringify(batchResponse(['ATUALIZADA']))); result.fixture.detectChanges(); result.load(1);
    expect(result.fixture.nativeElement.querySelector('.quote-update-feedback').textContent).toContain('Cotações atualizadas com sucesso');
    result.http.expectNone(() => true); result.http.verify();
  });

  it('integra HttpErrorResponse, interceptor, service e mensagem da UI', async () => {
    query.next(convertToParamMap({ carteiraId: '1' }));
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [{ provide: CARTEIRA_STORAGE, useValue: null }, provideRouter([]), provideApiConfig(), provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting(),
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
    http.expectOne('/api/carteiras/1/evolucao-patrimonial').flush('{"carteiraId":1,"pontos":[]}');
    http.expectOne('/api/carteiras/1/posicoes').flush('{"timeStamp":1,"status":409,"error":"Conflict","message":"Histórico inconsistente","path":"/carteiras/1/posicoes","code":"HISTORICO_OPERACOES_INCONSISTENTE","details":{}}', { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Histórico inconsistente');
    http.verify();
  });
});
