import { OverlayContainer } from '@angular/cdk/overlay';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { AcoesService } from '../../acoes/acoes.service';
import { CorretorasService } from '../../corretoras/corretoras.service';
import { OperacaoResponse } from '../../operacoes/models/operacao';
import { OperacoesService } from '../../operacoes/operacoes.service';
import { OperacaoFormPageComponent } from '../../operacoes/pages/operacao-form-page.component';
import { CarteirasService } from '../carteiras.service';
import { CarteiraResponse } from '../models/carteira';
import { CarteiraDetailPageComponent } from './carteira-detail-page.component';
import { CarteirasListPageComponent } from './carteiras-list-page.component';

const CARTEIRA: CarteiraResponse = { id: 6, nome: 'Inicial', dataCriacao: '2026-08-31T12:00:00Z' };
const waitForDialogClose = () => new Promise((resolve) => setTimeout(resolve, 250));

describe('Carteiras com MatDialog real', () => {
  let overlay: OverlayContainer;
  afterEach(() => { overlay?.getContainerElement().replaceChildren(); TestBed.resetTestingModule(); });

  it('cadastra pelo CTA e afterClosed real com POST único, sem novo GET', async () => {
    const service = { listar: vi.fn().mockReturnValue(of([])), cadastrar: vi.fn().mockReturnValue(of(CARTEIRA)), buscarPorId: vi.fn(), atualizar: vi.fn(), excluir: vi.fn() };
    await TestBed.configureTestingModule({ imports: [CarteirasListPageComponent], providers: [provideRouter([]), { provide: CarteirasService, useValue: service }] }).compileComponents();
    overlay = TestBed.inject(OverlayContainer); const fixture = TestBed.createComponent(CarteirasListPageComponent); fixture.detectChanges();
    const create = [...fixture.nativeElement.querySelectorAll('button')].find((button: HTMLButtonElement) => button.textContent?.includes('Nova carteira')) as HTMLButtonElement; create.click(); fixture.detectChanges();
    const input = overlay.getContainerElement().querySelector('input') as HTMLInputElement; input.value = 'Inicial'; input.dispatchEvent(new Event('input')); (overlay.getContainerElement().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit')); await waitForDialogClose(); fixture.detectChanges();
    expect(service.cadastrar).toHaveBeenCalledOnce(); expect(service.cadastrar).toHaveBeenCalledWith({ nome: 'Inicial' }); expect(service.listar).toHaveBeenCalledTimes(1); expect(fixture.nativeElement.textContent).toContain('Inicial');
  });

  it('cancela edição real sem PATCH e confirma exclusão real com um DELETE', async () => {
    const service = { listar: vi.fn(), cadastrar: vi.fn(), buscarPorId: vi.fn(), atualizar: vi.fn().mockReturnValue(of(CARTEIRA)), excluir: vi.fn().mockReturnValue(of(undefined)) }; const toast = { show: vi.fn() };
    await TestBed.configureTestingModule({ imports: [CarteiraDetailPageComponent], providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '6' } } } }, { provide: CarteirasService, useValue: service }, { provide: OperacoesService, useValue: { listarPorCarteira: vi.fn().mockReturnValue(of([])), cadastrar: vi.fn() } }, { provide: SuccessToastService, useValue: toast }] }).compileComponents();
    overlay = TestBed.inject(OverlayContainer); const router = TestBed.inject(Router); vi.spyOn(router, 'currentNavigation').mockReturnValue({ extras: { info: { carteira: CARTEIRA } } } as never); vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture: ComponentFixture<CarteiraDetailPageComponent> = TestBed.createComponent(CarteiraDetailPageComponent); fixture.detectChanges();
    (fixture.nativeElement.querySelectorAll('button')[0] as HTMLButtonElement).click(); fixture.detectChanges(); const cancel = [...overlay.getContainerElement().querySelectorAll('button')].find((button) => button.textContent?.trim() === 'Cancelar') as HTMLButtonElement; cancel.click(); await waitForDialogClose(); expect(service.atualizar).not.toHaveBeenCalled();
    (fixture.nativeElement.querySelectorAll('button')[1] as HTMLButtonElement).click(); fixture.detectChanges(); const exclude = [...overlay.getContainerElement().querySelectorAll('button')].find((button) => button.textContent?.trim() === 'Excluir') as HTMLButtonElement; exclude.click(); await waitForDialogClose();
    expect(service.excluir).toHaveBeenCalledOnce(); expect(service.excluir).toHaveBeenCalledWith(6); expect(router.navigate).toHaveBeenCalledWith(['/carteiras']);
  });

  it('usa o mesmo dialog contextual para payloads discriminados de COMPRA e VENDA', async () => {
    const purchaseResponse: OperacaoResponse = { id: 10, carteiraId: 6, ticker: 'AAPL', mercado: 'EUA', corretoraId: null, tipo: 'COMPRA', quantidade: '1', precoUnitario: '12.34', dataOperacao: '2026-08-31', ordemNoDia: 1, valorTotal: '12.34' };
    const saleResponse: OperacaoResponse = { ...purchaseResponse, id: 11, tipo: 'VENDA', precoUnitario: '15', ordemNoDia: 2, valorTotal: '15' };
    const operations = {
      cadastrar: vi.fn().mockReturnValueOnce(of(purchaseResponse)).mockReturnValueOnce(of(saleResponse)),
      obterPreviaCompra: vi.fn().mockReturnValue(of({ ticker: 'AAPL', mercado: 'EUA', moeda: 'USD', dataCotacao: '2026-08-31', precoUnitario: '12.34' })),
      obterSugestaoPrecoVenda: vi.fn().mockReturnValue(of({ precoUnitarioSugerido: '15' }))
    };
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: OperacoesService, useValue: operations },
        { provide: CarteirasService, useValue: { listar: vi.fn() } },
        { provide: AcoesService, useValue: { listar: () => of([{ id: 2, ticker: 'AAPL', nomeEmpresa: 'Apple', mercado: 'EUA', moeda: 'USD', cotacaoAtual: 1, dataHoraCotacao: '' }]) } },
        { provide: CorretorasService, useValue: { listar: () => of([]) } },
        { provide: SuccessToastService, useValue: { show: vi.fn() } }
      ]
    }).compileComponents();
    overlay = TestBed.inject(OverlayContainer);
    const dialog = TestBed.inject(MatDialog);

    const purchase = dialog.open(OperacaoFormPageComponent, { data: { carteira: CARTEIRA } });
    purchase.componentInstance['form'].setValue({ carteiraId: 6, acaoKey: 'AAPL|EUA', corretoraId: null, tipo: 'COMPRA', quantidade: '1', precoUnitario: '', dataOperacao: '2026-08-31' });
    expect(operations.obterPreviaCompra).toHaveBeenCalledWith('AAPL', 'EUA', '2026-08-31');
    purchase.componentInstance['submit']();
    expect(operations.cadastrar.mock.calls[0][0]).toEqual({ carteiraId: 6, ticker: 'AAPL', mercado: 'EUA', corretoraId: null, tipo: 'COMPRA', quantidade: '1', dataOperacao: '2026-08-31' });
    expect(operations.cadastrar.mock.calls[0][0]).not.toHaveProperty('precoUnitario');
    expect(operations.cadastrar.mock.calls[0][0]).not.toHaveProperty('ordemNoDia');
    await waitForDialogClose();

    const sale = dialog.open(OperacaoFormPageComponent, { data: { carteira: CARTEIRA } });
    sale.componentInstance['form'].setValue({ carteiraId: 6, acaoKey: 'AAPL|EUA', corretoraId: null, tipo: 'VENDA', quantidade: '1', precoUnitario: '15', dataOperacao: '2026-08-31' });
    sale.componentInstance['form'].controls.precoUnitario.setValue('15');
    expect(operations.obterSugestaoPrecoVenda).toHaveBeenCalledWith(6, 'AAPL', 'EUA', '2026-08-31');
    sale.componentInstance['submit']();
    expect(operations.cadastrar.mock.calls[1][0]).toEqual({ carteiraId: 6, ticker: 'AAPL', mercado: 'EUA', corretoraId: null, tipo: 'VENDA', quantidade: '1', precoUnitario: '15', dataOperacao: '2026-08-31' });
    expect(operations.cadastrar.mock.calls[1][0]).not.toHaveProperty('ordemNoDia');
  });
});
