import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of, Subject, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { CustodiaHistoricoComponent } from './custodia-historico.component';
import { CustodiaService } from './custodia.service';
import { CorretoraNamesService } from '../corretoras/corretora-names.service';
import { parseHistoricoCustodia } from './custodia.contract';
import { DetalheCustodia, RegistroCustodia } from './models/custodia';
import { estornoCustodia, originalCustodia } from './testing/custodia.fixtures';
import { CustodiaEstornoDialogComponent } from './custodia-estorno-dialog.component';
import { By } from '@angular/platform-browser';
import { FormGroupDirective } from '@angular/forms';
import { parseDetalheCustodia, parseRegistroCustodia } from './custodia.contract';

function registros(): RegistroCustodia[] {
  return parseHistoricoCustodia(JSON.stringify([
    originalCustodia({ id: 30, natureza: 'ATRIBUICAO', corretoraOrigemId: null, dataRegistro: '2026-10-25T12:00:00Z',
      posicaoCronologica: { ordemFinanceiraAncora: 1, numerador: '2', denominador: '3' } }),
    estornoCustodia({ id: 10, estornoId: 10 }), originalCustodia({ situacao: 'ESTORNADA', estornoId: 10 })]), 1);
}
function filtros(f: ComponentFixture<CustodiaHistoricoComponent>) {
  return f.debugElement.query(By.directive(FormGroupDirective)).injector.get(FormGroupDirective).form;
}
describe('CustodiaHistoricoComponent', () => {
  it('atualização de escrita conserva filtros e invalida respostas de gerações anteriores', async () => {
    const r = await criar();
    const antigo = new Subject<RegistroCustodia[]>(), atual = new Subject<RegistroCustodia[]>();
    r.service.historico.mockReturnValueOnce(antigo).mockReturnValueOnce(atual);
    filtros(r.fixture).patchValue({ natureza: 'ATRIBUICAO' }); r.fixture.detectChanges();
    r.fixture.componentRef.setInput('refreshVersion', 1); r.fixture.detectChanges();
    expect(r.service.historico).toHaveBeenLastCalledWith(1, { natureza: 'ATRIBUICAO' });
    atual.next([]); antigo.next(registros()); r.fixture.detectChanges();
    expect(r.fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(0);
    expect(filtros(r.fixture).value.natureza).toBe('ATRIBUICAO');
    expect(r.service.estornar).not.toHaveBeenCalled();
  });
  afterEach(() => TestBed.resetTestingModule());
  async function criar(source: Observable<RegistroCustodia[]> = of(registros())) {
    const service = { historico: vi.fn().mockReturnValue(source), estornar: vi.fn() };
    const closed = new Subject<DetalheCustodia | undefined>();
    const ref = { afterClosed: () => closed, close: vi.fn() };
    const dialog = { open: vi.fn().mockReturnValue(ref) };
    const names = { loadFor: vi.fn(), name: vi.fn((id: number) => `Corretora #${id}`) };
    await TestBed.configureTestingModule({ imports: [CustodiaHistoricoComponent], providers: [
      { provide: CustodiaService, useValue: service }, { provide: MatDialog, useValue: dialog },
      { provide: CorretoraNamesService, useValue: names }
    ] }).compileComponents();
    const fixture = TestBed.createComponent(CustodiaHistoricoComponent);
    fixture.componentRef.setInput('carteiraId', 1); fixture.detectChanges();
    return { fixture, service, closed, ref, dialog, names };
  }
  it('renderiza conjunto completo e ordem recebida com identidades discriminadas e vínculos', async () => {
    const r = await criar(); const root: HTMLElement = r.fixture.nativeElement;
    expect(r.service.historico).toHaveBeenCalledExactlyOnceWith(1, {});
    expect([...root.querySelectorAll('tbody tr')].map(v => v.getAttribute('data-registro')))
      .toEqual(['MOVIMENTACAO:30', 'ESTORNO:10', 'MOVIMENTACAO:10']);
    expect(root.textContent).toContain('ATRIBUICAO'); expect(root.textContent).toContain('ESTORNADA');
    expect(root.textContent).toContain('Movimentação #10'); expect(root.textContent).toContain('Estorno #10');
  });
  it('oferece confirmação apenas à original ativa e expõe sucesso sem GET automático', async () => {
    const r = await criar(); const root: HTMLElement = r.fixture.nativeElement;
    const buttons = root.querySelectorAll<HTMLButtonElement>('button[data-estornar]');
    expect(buttons).toHaveLength(1); expect(buttons[0].getAttribute('data-estornar')).toBe('30');
    const sucesso = vi.fn(); r.fixture.componentInstance.estornoConfirmado.subscribe(sucesso);
    buttons[0].click(); expect(r.dialog.open).toHaveBeenCalledWith(CustodiaEstornoDialogComponent, expect.objectContaining({
      data: { carteiraId: 1, movimentacao: registros()[0] }, restoreFocus: true,
      ariaLabelledBy: 'custodia-estorno-title', ariaDescribedBy: 'custodia-estorno-description' }));
    expect(r.service.estornar).not.toHaveBeenCalled(); expect(sucesso).not.toHaveBeenCalled();
    const detalhe = { movimentacao: { ...registros()[0], situacao: 'ESTORNADA', estornoId: 40 },
      estorno: { ...registros()[1], id: 40, estornoId: 40, movimentacaoOriginalId: 30, corretoraOrigemId: null,
        posicaoCronologica: registros()[0].posicaoCronologica } } as DetalheCustodia;
    r.closed.next(detalhe); r.fixture.detectChanges();
    expect(sucesso).toHaveBeenCalledExactlyOnceWith(detalhe); expect(r.service.historico).toHaveBeenCalledOnce();
    expect(root.querySelectorAll('button[data-estornar]')).toHaveLength(0);
    expect(root.textContent).toContain('Atualize o histórico');
  });
  it('mantém loading e vazio independentes, sem POST ou confirmação', async () => {
    const source = new Subject<RegistroCustodia[]>(); const r = await criar(source);
    expect(r.fixture.nativeElement.textContent).toContain('Carregando histórico de custódia');
    source.next([]); r.fixture.detectChanges();
    expect(r.fixture.nativeElement.textContent).toContain('Nenhuma movimentação de custódia');
    expect(r.dialog.open).not.toHaveBeenCalled(); expect(r.service.estornar).not.toHaveBeenCalled();
  });
  it('erro não fabrica vazio; retry repete somente GET e conserva filtros', async () => {
    const r = await criar(throwError(() => ({ kind: 'standard', status: 500, message: 'Falha segura', details: {} })));
    expect(r.fixture.nativeElement.textContent).toContain('Falha segura');
    expect(r.fixture.nativeElement.textContent).not.toContain('Nenhuma movimentação');
    r.service.historico.mockReturnValue(of(registros()));
    const b = [...r.fixture.nativeElement.querySelectorAll('button')].find((v: HTMLButtonElement) => v.textContent?.includes('Tentar novamente'));
    b.click(); r.fixture.detectChanges(); expect(r.service.historico).toHaveBeenCalledTimes(2);
    expect(r.fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(3); expect(r.service.estornar).not.toHaveBeenCalled();
  });
  it.each([
    ['', '', {}], ['TRANSFERENCIA', 'ATIVA', { natureza: 'TRANSFERENCIA', situacao: 'ATIVA' }],
    ['ATRIBUICAO', '', { natureza: 'ATRIBUICAO' }], ['ESTORNO', 'ATIVA', { natureza: 'ESTORNO', situacao: 'ATIVA' }],
    ['ESTORNO', 'ESTORNADA', { natureza: 'ESTORNO', situacao: 'ESTORNADA' }], ['', 'ESTORNADA', { situacao: 'ESTORNADA' }]
  ])('envia filtros sem reinterpretar a política do backend %#', async (natureza, situacao, esperado) => {
    const r = await criar(); filtros(r.fixture).patchValue({ natureza, situacao }); r.fixture.detectChanges();
    expect(r.service.historico).toHaveBeenLastCalledWith(1, esperado);
    expect(r.fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(3);
    expect(r.fixture.nativeElement.querySelector('[data-registro="ESTORNO:10"]')?.textContent).not.toContain('ATIVA');
  });
  it('preserva conjunto completo sem limitar, paginar ou ordenar novamente', async () => {
    const fonte = Array.from({ length: 120 }, (_, i) => parseRegistroCustodia(JSON.stringify(originalCustodia({
      id: 500 - i, posicaoCronologica: { ordemFinanceiraAncora: 1, numerador: '1', denominador: String(i + 3) } })), 1));
    const r = await criar(of(fonte));
    expect([...r.fixture.nativeElement.querySelectorAll('tbody tr')].map((v: Element) => v.getAttribute('data-registro')))
      .toEqual(fonte.map(v => `MOVIMENTACAO:${v.id}`));
    expect(r.service.historico).toHaveBeenCalledOnce(); expect(r.names.loadFor).toHaveBeenCalledOnce();
    expect(r.fixture.nativeElement.textContent).not.toMatch(/próxima página|carregar mais/i);
  });
  it('preserva decimal EUA, fração de 512 dígitos e timestamp com nanos no DOM', async () => {
    const n = '1' + '0'.repeat(511), d = '1' + '0'.repeat(510) + '1';
    const json = JSON.stringify(originalCustodia({ mercado: 'EUA', ticker: 'AAPL', dataRegistro: '2026-10-24T12:00:00.123456789-03:00',
      posicaoCronologica: { ordemFinanceiraAncora: 0, numerador: n, denominador: d } }))
      .replace('"quantidade":40', '"quantidade":9999999999999.123456');
    const r = await criar(of([parseRegistroCustodia(json, 1)])); const root: HTMLElement = r.fixture.nativeElement;
    expect(root.textContent).toContain('9.999.999.999.999,123456'); expect(root.textContent).toContain(`${n}/${d}`);
    expect(root.querySelector('time[datetime="2026-10-24T12:00:00.123456789-03:00"]')?.textContent)
      .toBe('2026-10-24T12:00:00.123456789-03:00');
  });
  it('não depende de posição aberta e conserva origem sem corretora e situações próprias', async () => {
    const r = await criar(); const root: HTMLElement = r.fixture.nativeElement;
    expect(root.textContent).toContain('Sem corretora informada'); expect(root.textContent).toContain('ATIVA');
    expect(root.querySelector('[data-registro="ESTORNO:10"] [aria-label="Situação não se aplica ao estorno"]')).not.toBeNull();
    expect(root.querySelectorAll('[data-estornar="10"]')).toHaveLength(0);
    expect(r.names.loadFor).toHaveBeenCalledWith(expect.arrayContaining([{ corretoraId: null }, { corretoraId: 4 }]));
  });
  it('abertura/cancelamento e clique repetido não emitem sucesso ou POST', async () => {
    const r = await criar(); const output = vi.fn(); r.fixture.componentInstance.estornoConfirmado.subscribe(output);
    const b = r.fixture.nativeElement.querySelector('button[data-estornar="30"]'); b.click(); b.click();
    expect(r.dialog.open).toHaveBeenCalledOnce(); r.closed.next(undefined);
    expect(output).not.toHaveBeenCalled(); expect(r.service.estornar).not.toHaveBeenCalled();
    expect(r.service.historico).toHaveBeenCalledOnce();
  });
  it('troca de carteira cancela consulta/dialog e rejeita resultados antigos', async () => {
    const source = new Subject<RegistroCustodia[]>(); const r = await criar();
    const output = vi.fn(); r.fixture.componentInstance.estornoConfirmado.subscribe(output);
    r.fixture.nativeElement.querySelector('button[data-estornar="30"]').click();
    r.service.historico.mockReturnValue(source);
    r.fixture.componentRef.setInput('carteiraId', 2); r.fixture.detectChanges();
    expect(r.ref.close).toHaveBeenCalled(); expect(r.service.historico).toHaveBeenLastCalledWith(2, {});
    source.next(registros()); r.fixture.detectChanges(); expect(r.fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(0);
    r.closed.next(parseDetalheCustodia(JSON.stringify({ movimentacao: originalCustodia({ id: 30, situacao: 'ESTORNADA', estornoId: 40 }),
      estorno: estornoCustodia({ id: 40, estornoId: 40, movimentacaoOriginalId: 30 }) }), 1, 30));
    expect(output).not.toHaveBeenCalled();
  });
  it('consulta antiga não sobrescreve filtros nem contexto novo', async () => {
    const anterior = new Subject<RegistroCustodia[]>(); const atual = new Subject<RegistroCustodia[]>(); const r = await criar(anterior);
    r.service.historico.mockReturnValue(atual); filtros(r.fixture).patchValue({ natureza: 'ESTORNO' });
    anterior.next(registros()); atual.next([registros()[1]]); r.fixture.detectChanges();
    expect(r.fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(r.fixture.nativeElement.querySelector('tbody tr')?.getAttribute('data-registro')).toBe('ESTORNO:10');
  });
  it('cancelamento usa título focável quando recarga assíncrona já removeu o acionador', async () => {
    const r = await criar();
    const acionador = r.fixture.nativeElement.querySelector('[data-estornar="30"]') as HTMLButtonElement;
    acionador.focus(); acionador.click();
    r.service.historico.mockReturnValue(of([]));
    r.fixture.componentRef.setInput('refreshVersion', 1); r.fixture.detectChanges();
    expect(acionador.isConnected).toBe(false);
    r.closed.next(undefined); r.fixture.detectChanges(); TestBed.tick();
    const titulo = r.fixture.nativeElement.querySelector('h2') as HTMLElement;
    expect(titulo.isConnected).toBe(true); expect(titulo.tabIndex).toBe(-1);
    expect(document.activeElement).toBe(titulo);
    expect(r.service.estornar).not.toHaveBeenCalled();
  });
  it.each([true, false])('troca de carteira impede foco de fechamento antigo, resposta antes da troca=%s', async antes => {
    const r = await criar();
    const acionador = r.fixture.nativeElement.querySelector('[data-estornar="30"]') as HTMLButtonElement;
    acionador.focus(); acionador.click();
    const titulo = r.fixture.nativeElement.querySelector('h2') as HTMLElement;
    const focar = vi.spyOn(titulo, 'focus');
    const detalhe = parseDetalheCustodia(JSON.stringify({
      movimentacao: originalCustodia({ id: 30, situacao: 'ESTORNADA', estornoId: 40 }),
      estorno: estornoCustodia({ id: 40, estornoId: 40, movimentacaoOriginalId: 30 }) }), 1, 30);
    if (antes) r.closed.next(detalhe);
    r.fixture.componentRef.setInput('carteiraId', 2); r.fixture.detectChanges();
    if (!antes) r.closed.next(detalhe);
    r.fixture.detectChanges(); TestBed.tick();
    expect(focar).not.toHaveBeenCalled();
    expect(document.activeElement).not.toBe(titulo);
    expect(r.service.estornar).not.toHaveBeenCalled();
  });
  it('títulos, filtros, tabela e ação possuem nomes acessíveis e identidade explícita', async () => {
    const r = await criar(); const root: HTMLElement = r.fixture.nativeElement;
    expect(root.querySelector('section')?.getAttribute('aria-labelledby')).toBe('custodia-historico-title');
    expect(root.querySelector('caption')?.textContent).toContain('ordem recebida');
    expect(root.querySelector('[data-estornar="30"]')?.getAttribute('aria-label')).toContain('30 PETR4');
    expect([...root.querySelectorAll('mat-label')].map(v => v.textContent)).toEqual(['Natureza da custódia', 'Situação da movimentação']);
  });
});
