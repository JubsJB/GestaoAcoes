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

const CARTEIRA: CarteiraResponse = { id: 3, nome: 'Carteira detalhada', dataCriacao: '2026-08-31T12:00:00Z' };
const httpError = (status: number, message: string) => ({ status, code: null, message, details: {} }) as unknown as NormalizedHttpError;

describe('CarteiraDetailPageComponent', () => {
  async function create(info?: CarteiraResponse, failure?: NormalizedHttpError) {
    const service = { buscarPorId: vi.fn().mockReturnValue(failure ? throwError(() => failure) : of(CARTEIRA)), excluir: vi.fn(), atualizar: vi.fn(), cadastrar: vi.fn(), listar: vi.fn().mockReturnValue(of([CARTEIRA])) };
    const params = new BehaviorSubject(convertToParamMap({ id: 3 }));
    const dashboard = { listarPosicoes: vi.fn().mockReturnValue(of([])) };
    const operations = { listarPorCarteira: vi.fn().mockReturnValue(of([])), cadastrar: vi.fn() };
    const closed = new Subject<CarteiraResponse | OperacaoResponse | boolean | undefined>(); const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => closed, close: vi.fn() }) }; const toast = { show: vi.fn() };
    await TestBed.configureTestingModule({ imports: [CarteiraDetailPageComponent], providers: [{ provide: CARTEIRA_STORAGE, useValue: null }, { provide: DashboardService, useValue: dashboard }, provideRouter([]), { provide: ActivatedRoute, useValue: { paramMap: params } }, { provide: CarteirasService, useValue: service }, { provide: OperacoesService, useValue: operations }, { provide: MatDialog, useValue: dialog }, { provide: SuccessToastService, useValue: toast }] }).compileComponents();
    const router = TestBed.inject(Router); vi.spyOn(router, 'currentNavigation').mockReturnValue(info ? ({ extras: { info: { carteira: info } } } as never) : null); vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(CarteiraDetailPageComponent); fixture.detectChanges(); return { fixture, service, closed, dialog, toast, router, params, dashboard, operations };
  }
  afterEach(() => TestBed.resetTestingModule());

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
    expect(fixture.nativeElement.textContent).toContain('12,340000');
    expect(fixture.nativeElement.textContent).toContain('24,680000000000');
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
    newPositions.next([{ acaoId: 7, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL', moeda: 'BRL', quantidadeAtual: '5', precoMedio: '40', custoPosicao: '200', cotacaoAtual: '48.2', dataHoraCotacao: '2026-01-01T00:00:00Z', valorAtualPosicao: '241', resultadoNaoRealizado: '41', rentabilidadePercentual: '20.5' }]); newPositions.complete();
    result.fixture.detectChanges();
    expect(result.fixture.nativeElement.textContent).toContain('Outra');
    expect(result.fixture.nativeElement.textContent).toContain('PETR4');
    expect(result.fixture.nativeElement.textContent).toContain('R$ 241,00');
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
