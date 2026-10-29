import { CorretorasService } from '../../corretoras/corretoras.service';
import { CARTEIRA_STORAGE } from '../../../core/carteira/carteira-context.service';
import { DashboardService } from '../../dashboard/dashboard.service';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { OperacoesService } from '../../operacoes/operacoes.service';
import { OperacaoResponse } from '../../operacoes/models/operacao';
import { OperacaoFormPageComponent } from '../../operacoes/pages/operacao-form-page.component';
import { CarteiraDeleteConfirmDialogComponent } from '../carteira-delete-confirm-dialog.component';
import { CarteirasService } from '../carteiras.service';
import { CarteiraResponse } from '../models/carteira';
import { CarteiraDetailPageComponent } from './carteira-detail-page.component';
import { CarteiraFormPageComponent } from './carteira-form-page.component';
import { DistribuicaoCorretorasService } from '../distribuicao-corretoras.service';
import { parseDistribuicaoCorretoras } from '../distribuicao-corretoras.contract';
import { DISTRIBUICAO_EXAMPLES } from '../testing/distribuicao-corretoras.fixtures';
import { DistribuicaoCarteiraResponse } from '../models/distribuicao-corretoras';
import { CustodiaFormDialogComponent } from '../custodia-form-dialog.component';
import { originalCustodia } from '../testing/custodia.fixtures';
import { MovimentacaoCustodia } from '../models/custodia';
import { CustodiaService } from '../custodia.service';
import { CustodiaHistoricoComponent } from '../custodia-historico.component';
import { By } from '@angular/platform-browser';
import { parseDetalheCustodia } from '../custodia.contract';
import { estornoCustodia } from '../testing/custodia.fixtures';

const CARTEIRA: CarteiraResponse = { id: 3, nome: 'Carteira detalhada', dataCriacao: '2026-08-31T12:00:00Z' };
const httpError = (status: number, message: string) => ({ status, code: null, message, details: {} }) as unknown as NormalizedHttpError;

describe('CarteiraDetailPageComponent', () => {
  async function create(info?: CarteiraResponse, failure?: NormalizedHttpError) {
    const service = { buscarPorId: vi.fn().mockReturnValue(failure ? throwError(() => failure) : of(CARTEIRA)), excluir: vi.fn(), atualizar: vi.fn(), cadastrar: vi.fn(), listar: vi.fn().mockReturnValue(of([CARTEIRA])) };
    const params = new BehaviorSubject(convertToParamMap({ id: 3 }));
    const dashboard = { listarPosicoes: vi.fn().mockReturnValue(of([])) };
    const distribution = { consultar: vi.fn((id: number) => of<DistribuicaoCarteiraResponse>({ carteiraId: id, posicoes: [] })) };
    const operations = { listarPorCarteira: vi.fn().mockReturnValue(of([])), cadastrar: vi.fn() };
    const custodia = { historico: vi.fn().mockReturnValue(of([])) };
    const closed = new Subject<CarteiraResponse | OperacaoResponse | MovimentacaoCustodia | boolean | undefined>(); const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => closed, close: vi.fn() }) }; const toast = { show: vi.fn() };
    await TestBed.configureTestingModule({ imports: [CarteiraDetailPageComponent], providers: [{provide:CustodiaService,useValue:custodia},{provide:DistribuicaoCorretorasService,useValue:distribution},{provide:CorretorasService,useValue:{listar:vi.fn().mockReturnValue(of([]))}},{ provide: CARTEIRA_STORAGE, useValue: null }, { provide: DashboardService, useValue: dashboard }, provideRouter([]), { provide: ActivatedRoute, useValue: { paramMap: params } }, { provide: CarteirasService, useValue: service }, { provide: OperacoesService, useValue: operations }, { provide: MatDialog, useValue: dialog }, { provide: SuccessToastService, useValue: toast }] }).compileComponents();
    const router = TestBed.inject(Router); vi.spyOn(router, 'currentNavigation').mockReturnValue(info ? ({ extras: { info: { carteira: info } } } as never) : null); vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(CarteiraDetailPageComponent); fixture.detectChanges(); return { fixture, service, closed, dialog, toast, router, params, dashboard, operations, distribution, custodia };
  }
  afterEach(() => TestBed.resetTestingModule());

  it('cadastro confirmado recarrega histórico e distribuição sem repetir escrita ou apagar financeiro', async () => {
    const r = await create(CARTEIRA);
    const b = [...r.fixture.nativeElement.querySelectorAll('button')].find((v: HTMLButtonElement) => v.textContent?.includes('Movimentar custódia'));
    b.click(); const antesFinanceiro = r.operations.listarPorCarteira.mock.calls.length;
    r.closed.next(originalCustodia({ carteiraId: 3, quantidade: '40' }) as unknown as MovimentacaoCustodia);
    r.fixture.detectChanges();
    expect(r.custodia.historico).toHaveBeenCalledTimes(2); expect(r.distribution.consultar).toHaveBeenCalledTimes(2);
    expect(r.operations.listarPorCarteira).toHaveBeenCalledTimes(antesFinanceiro);
    expect(r.fixture.nativeElement.textContent).toContain('Movimentação #10 registrada com sucesso');
  });

  it('mantém histórico de custódia separado e acessível sem posição aberta', async () => {
    const r = await create(CARTEIRA);
    expect(r.fixture.nativeElement.querySelector('app-custodia-historico')).not.toBeNull();
    expect(r.custodia.historico).toHaveBeenCalledExactlyOnceWith(3, {});
    expect(r.fixture.nativeElement.textContent).toContain('Histórico de operações');
    expect(r.fixture.nativeElement.textContent).toContain('Histórico de custódia');
  });
  it('falha de histórico não remove posições consolidadas válidas', async () => {
    const r = await create(CARTEIRA);
    const position = { acaoId: 1, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL', moeda: 'BRL',
      quantidadeAtual: '5', precoMedio: '40', custoPosicao: '200', cotacaoAtual: '48.2',
      dataHoraCotacao: '2026-01-01T00:00:00Z', referenciaMercadoCotacao: { tipo: 'DESCONHECIDA', data: null, instante: null },
      cotacaoObtidaEm: null, valorAtualPosicao: '241', resultadoNaoRealizado: '41', rentabilidadePercentual: '20.5' };
    r.dashboard.listarPosicoes.mockReturnValue(of([position]));
    (r.fixture.componentInstance as any).loadPositions(); r.fixture.detectChanges();
    r.custodia.historico.mockReturnValue(throwError(() => httpError(500, 'Falha no histórico')));
    r.fixture.nativeElement.querySelector('app-custodia-historico button').click(); r.fixture.detectChanges();
    expect(r.fixture.nativeElement.textContent).toContain('Falha no histórico');
    expect(r.fixture.nativeElement.querySelector('app-portfolio-positions table')?.textContent).toContain('R$ 241,00');
    expect(r.fixture.nativeElement.textContent).toContain('Histórico de operações');
  });
  it('recebe sucesso contextual de estorno com recarga independente das consultas', async () => {
    const r = await create(CARTEIRA);
    const child = r.fixture.debugElement.query(By.directive(CustodiaHistoricoComponent)).componentInstance as CustodiaHistoricoComponent;
    const d = parseDetalheCustodia(JSON.stringify({ movimentacao: originalCustodia({ carteiraId: 3, situacao: 'ESTORNADA', estornoId: 20 }),
      estorno: estornoCustodia({ carteiraId: 3 }) }), 3, 10);
    const consultas = r.distribution.consultar.mock.calls.length;
    child.estornoConfirmado.emit(d); r.fixture.detectChanges();
    expect(r.toast.show).toHaveBeenCalledExactlyOnceWith('Estorno de custódia confirmado com sucesso.');
    expect(r.distribution.consultar).toHaveBeenCalledTimes(consultas + 1); expect(r.custodia.historico).toHaveBeenCalledTimes(2);
    child.estornoConfirmado.emit({ ...d, movimentacao: { ...d.movimentacao, carteiraId: 9 } });
    expect(r.toast.show).toHaveBeenCalledOnce();
  });

  it('abre custódia na carteira fixa e recebe sucesso sem alterar histórico financeiro', async () => {
    const r = await create(CARTEIRA);
    const b = [...r.fixture.nativeElement.querySelectorAll('button')].find((v: HTMLButtonElement) => v.textContent?.includes('Movimentar custódia'));
    expect(b).toBeDefined(); b!.click();
    expect(r.dialog.open).toHaveBeenCalledWith(CustodiaFormDialogComponent, expect.objectContaining({
      data: { carteira: CARTEIRA }, restoreFocus: true, ariaLabelledBy: 'custodia-form-title', ariaDescribedBy: 'custodia-form-description' }));
    const antes = r.operations.listarPorCarteira.mock.calls.length;
    const distribuicaoAntes = r.distribution.consultar.mock.calls.length;
    r.closed.next(originalCustodia({ carteiraId: 3, quantidade: '40' }) as unknown as MovimentacaoCustodia); r.fixture.detectChanges();
    expect(r.toast.show).toHaveBeenCalledWith('Movimentação de custódia registrada com sucesso.');
    expect(r.operations.listarPorCarteira).toHaveBeenCalledTimes(antes);
    expect(r.distribution.consultar).toHaveBeenCalledTimes(distribuicaoAntes + 1);
  });

  it('descarta resultado tardio de custódia depois da troca de carteira', async () => {
    const r = await create(CARTEIRA);
    const b = [...r.fixture.nativeElement.querySelectorAll('button')].find((v: HTMLButtonElement) => v.textContent?.includes('Movimentar custódia'));
    expect(b).toBeDefined(); b!.click();
    r.params.next(convertToParamMap({ id: '4' }));
    r.closed.next(originalCustodia({ carteiraId: 3, quantidade: '40' }) as unknown as MovimentacaoCustodia);
    expect(r.toast.show).not.toHaveBeenCalled();
  });

  it('recarrega distribuicao apos operacao confirmada sem alterar payload financeiro', async () => {
    const result = await create(CARTEIRA);
    expect(result.distribution.consultar).toHaveBeenCalledTimes(1);
    (result.fixture.componentInstance as any).openOperationDialog();
    result.closed.next({ id: 20, carteiraId: 3, ticker: 'AAPL', mercado: 'EUA', corretoraId: null,
      tipo: 'COMPRA', quantidade: '2', precoUnitario: '12.34', dataOperacao: '2026-08-31',
      ordemNoDia: 1, valorTotal: '24.68' } as OperacaoResponse);
    result.fixture.detectChanges();
    expect(result.distribution.consultar).toHaveBeenCalledTimes(2);
    expect(result.distribution.consultar).toHaveBeenLastCalledWith(3);
  });

  it('preserva posicoes quando distribuicao falha e nao compara snapshots independentes', async () => {
    const result = await create(CARTEIRA);
    const position = { acaoId: 1, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL', moeda: 'BRL',
      quantidadeAtual: '5', precoMedio: '40', custoPosicao: '200', cotacaoAtual: '48.2',
      dataHoraCotacao: '2026-01-01T00:00:00Z', referenciaMercadoCotacao: { tipo: 'DESCONHECIDA', data: null, instante: null },
      cotacaoObtidaEm: null, valorAtualPosicao: '241', resultadoNaoRealizado: '41', rentabilidadePercentual: '20.5' };
    result.dashboard.listarPosicoes.mockReturnValue(of([position]));
    (result.fixture.componentInstance as any).loadPositions();
    const distribution = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]); distribution.carteiraId = 3;
    result.distribution.consultar.mockReturnValue(of(distribution));
    (result.fixture.componentInstance as any).distributionVersion.update((v: number) => v + 1);
    result.fixture.detectChanges();
    const root: HTMLElement = result.fixture.nativeElement;
    expect(root.querySelector('app-portfolio-positions')?.textContent).toContain('R$ 241,00');
    expect(root.querySelector('app-distribuicao-corretoras .reconciliation')).toBeNull();
    result.distribution.consultar.mockReturnValue(throwError(() => httpError(500, 'Falha na distribuição')));
    (result.fixture.componentInstance as any).distributionVersion.update((v: number) => v + 1);
    result.fixture.detectChanges();
    expect(root.querySelector('app-portfolio-positions table')).not.toBeNull();
    expect(root.querySelector('app-portfolio-positions')?.textContent).toContain('R$ 241,00');
    expect(root.textContent).toContain('Não foi possível carregar a distribuição');
  });

  it('resolve corretoras para todo o histórico com uma consulta e preserva fallback e navegação', async () => {
    const result = await create(CARTEIRA);
    const brokers = vi.mocked(TestBed.inject(CorretorasService).listar);
    brokers.mockReturnValue(of([{ id: 4, nomeFantasia: 'XP Investimentos', razaoSocial: 'XP SA' }] as any));
    const operation: OperacaoResponse = { id: 20, carteiraId: 3, ticker: 'AAPL', mercado: 'EUA', corretoraId: 4, tipo: 'COMPRA', quantidade: '2', precoUnitario: '12.34', dataOperacao: '2026-08-31', ordemNoDia: 2, valorTotal: '24.68' };
    result.operations.listarPorCarteira.mockReturnValue(of([operation, { ...operation, id: 21, corretoraId: 99 }, { ...operation, id: 22, corretoraId: null }]));
    (result.fixture.componentInstance as any).loadHistory();
    (result.fixture.componentInstance as any).loadHistory();
    result.fixture.detectChanges();
    expect(brokers).toHaveBeenCalledTimes(1);
    const root: HTMLElement = result.fixture.nativeElement;
    expect(root.textContent).toContain('XP Investimentos');
    expect(root.textContent).toContain('Corretora #99');
    expect(root.textContent).toContain('Sem corretora');
    expect(root.textContent).toContain('Quantidade2');
    expect(root.textContent).toContain('US$ 12,34');
    expect(root.textContent).toContain('US$ 24,68');
    expect(operation.precoUnitario).toBe('12.34');
    expect(root.querySelector('a.history-item')?.getAttribute('href')).toContain('/operacoes/20?carteiraId=3&origem=carteira');
  });

  it('usa DTO transitório compatível sem GET e mostra dados básicos e histórico', async () => { const { fixture, service } = await create(CARTEIRA); expect(service.buscarPorId).not.toHaveBeenCalled(); expect(fixture.nativeElement.textContent).toContain('Carteira detalhada'); expect(fixture.nativeElement.textContent).toContain('Histórico de operações'); expect(fixture.nativeElement.textContent).not.toMatch(/patrimônio|resumo por moeda/i); });
  it('ignora DTO incompatível e faz GET no acesso direto', async () => { const { service } = await create({ ...CARTEIRA, id: 99 }); expect(service.buscarPorId).toHaveBeenCalledOnce(); expect(service.buscarPorId).toHaveBeenCalledWith(3); });
  it('apresenta 404 dedicado e retry em erro recuperável', async () => { const notFound = await create(undefined, httpError(404, 'Ausente')); expect(notFound.fixture.nativeElement.textContent).toContain('Carteira não encontrada'); expect(notFound.fixture.nativeElement.textContent).toContain('Voltar para a listagem'); notFound.fixture.destroy(); TestBed.resetTestingModule(); const temporary = await create(undefined, httpError(500, 'Falha temporária')); temporary.service.buscarPorId.mockReturnValueOnce(of(CARTEIRA)); (temporary.fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); temporary.fixture.detectChanges(); expect(temporary.service.buscarPorId).toHaveBeenCalledTimes(2); });
  it('edita por dialog e substitui DTO sem GET redundante', async () => { const { fixture, service, dialog, closed, toast } = await create(CARTEIRA); (fixture.nativeElement.querySelectorAll('button')[0] as HTMLButtonElement).click(); expect(dialog.open).toHaveBeenCalledWith(CarteiraFormPageComponent, expect.objectContaining({ data: { mode: 'edit', carteira: CARTEIRA }, restoreFocus: true })); closed.next({ ...CARTEIRA, nome: 'Atualizada' }); closed.complete(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Atualizada'); expect(service.buscarPorId).not.toHaveBeenCalled(); expect(toast.show).toHaveBeenCalledWith('Carteira atualizada com sucesso.'); });
  it('só navega e mostra sucesso após confirmação de exclusão', async () => { const { fixture, dialog, closed, router, toast } = await create(CARTEIRA); (fixture.nativeElement.querySelectorAll('button')[1] as HTMLButtonElement).click(); expect(dialog.open).toHaveBeenCalledWith(CarteiraDeleteConfirmDialogComponent, expect.objectContaining({ data: CARTEIRA })); closed.next(false); expect(router.navigate).not.toHaveBeenCalled(); closed.next(true); closed.complete(); await Promise.resolve(); expect(router.navigate).toHaveBeenCalledWith(['/carteiras']); expect(toast.show).toHaveBeenCalledWith('Carteira excluída com sucesso.'); });
  it('incorpora no histórico o preço, ordem e total retornados pelo cadastro contextual', async () => {
    const { fixture, dialog, closed, toast } = await create(CARTEIRA);
    const register = [...fixture.nativeElement.querySelectorAll('button')].find((button: HTMLButtonElement) => button.textContent?.includes('Registrar operação')) as HTMLButtonElement;
    register.click();
    expect(dialog.open).toHaveBeenCalledWith(OperacaoFormPageComponent, expect.objectContaining({ data: { carteira: CARTEIRA }, restoreFocus: true }));
    closed.next({ id: 20, carteiraId: 3, ticker: 'AAPL', mercado: 'EUA', corretoraId: null, tipo: 'COMPRA', quantidade: '2', precoUnitario: '12.340000', dataOperacao: '2026-08-31', ordemNoDia: 4, valorTotal: '24.680000000000' });
    closed.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('ordem 4');
    expect(fixture.nativeElement.textContent).toContain('US$ 12,34');
    expect(fixture.nativeElement.textContent).toContain('US$ 24,68');
    expect(toast.show).toHaveBeenCalledWith('Operação registrada com sucesso.');
  });
  it('reage ao paramMap, limpa posições antigas e isola histórico e respostas tardias', async () => {
    const result = await create(CARTEIRA);
    const oldPositions = new Subject<any[]>();
    const newPositions = new Subject<any[]>();
    result.dashboard.listarPosicoes.mockReturnValueOnce(oldPositions).mockReturnValueOnce(newPositions);
    (result.fixture.componentInstance as any).loadPositions();
    result.service.buscarPorId.mockReturnValueOnce(of({ ...CARTEIRA, id: 4, nome: 'Outra' }));
    result.operations.listarPorCarteira.mockReturnValueOnce(throwError(() => httpError(500, 'Falha no histórico')));
    result.params.next(convertToParamMap({ id: 4 })); result.fixture.detectChanges();
    oldPositions.next([{ ticker: 'STALE' }]); oldPositions.complete();
    expect(result.fixture.nativeElement.textContent).not.toContain('STALE');
    newPositions.next([{ acaoId: 7, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL', moeda: 'BRL', quantidadeAtual: '5', precoMedio: '40', custoPosicao: '200', cotacaoAtual: '48.2', dataHoraCotacao: '2026-01-01T00:00:00Z', referenciaMercadoCotacao: { tipo: 'DESCONHECIDA', data: null, instante: null }, cotacaoObtidaEm: null, valorAtualPosicao: '241', resultadoNaoRealizado: '41', rentabilidadePercentual: '20.5' }]); newPositions.complete();
    result.fixture.detectChanges();
    expect(result.fixture.nativeElement.textContent).toContain('Outra');
    expect(result.fixture.nativeElement.textContent).toContain('PETR4');
    expect(result.fixture.nativeElement.textContent).toContain('R$ 241,00');
    expect(result.fixture.nativeElement.querySelector('app-portfolio-positions table')).toBeTruthy();
    expect(result.fixture.nativeElement.textContent).toContain('Falha no histórico');
    expect(result.dashboard.listarPosicoes).toHaveBeenLastCalledWith(4);
  });
  it('isola erro de posições sem esconder dados básicos ou histórico', async () => {
    const result = await create(CARTEIRA);
    result.dashboard.listarPosicoes.mockReturnValueOnce(throwError(() => httpError(500, 'Falha nas posições')));
    (result.fixture.componentInstance as any).loadPositions(); result.fixture.detectChanges();
    expect(result.fixture.nativeElement.textContent).toContain('Falha nas posições');
    expect(result.fixture.nativeElement.textContent).toContain('Carteira detalhada');
    expect(result.fixture.nativeElement.textContent).toContain('Nenhuma operação nesta carteira');
  });
  it('fecha dialog e não incorpora sucesso da carteira anterior após trocar rota', async () => {
    const result = await create(CARTEIRA);
    (result.fixture.componentInstance as any).openOperationDialog();
    result.service.buscarPorId.mockReturnValueOnce(of({ ...CARTEIRA, id: 4, nome: 'Outra' }));
    result.params.next(convertToParamMap({ id: 4 }));
    expect(result.dialog.open.mock.results[0].value.close).toHaveBeenCalled();
    result.closed.next({ id: 20, carteiraId: 3, ticker: 'STALE' } as OperacaoResponse);
    result.fixture.detectChanges();
    expect(result.fixture.nativeElement.textContent).not.toContain('STALE'); expect(result.toast.show).not.toHaveBeenCalled();
  });

  it('preserva registros do GET pendente ao incorporar operação confirmada', async () => {
    const result = await create(CARTEIRA);
    const pending = new Subject<OperacaoResponse[]>();
    result.operations.listarPorCarteira.mockReturnValueOnce(pending);
    (result.fixture.componentInstance as any).loadHistory();
    (result.fixture.componentInstance as any).openOperationDialog();
    const created: OperacaoResponse = { id: 20, carteiraId: 3, ticker: 'NOVA', mercado: 'EUA', corretoraId: null, tipo: 'COMPRA', quantidade: '2', precoUnitario: '12.34', dataOperacao: '2026-08-31', ordemNoDia: 2, valorTotal: '24.68' };
    result.closed.next(created);
    pending.next([{ ...created, id: 10, ticker: 'ANTIGA', ordemNoDia: 1 }]); pending.complete();
    result.fixture.detectChanges();
    expect(result.fixture.nativeElement.textContent).toContain('ANTIGA');
    expect(result.fixture.nativeElement.textContent).toContain('NOVA');
    expect(result.dashboard.listarPosicoes).toHaveBeenCalledTimes(2);
  });

});
