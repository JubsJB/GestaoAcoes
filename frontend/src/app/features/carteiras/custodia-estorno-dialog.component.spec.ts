import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Subject } from 'rxjs';
import { CustodiaEstornoDialogComponent } from './custodia-estorno-dialog.component';
import { CustodiaService } from './custodia.service';
import { DetalheCustodia, MovimentacaoCustodia } from './models/custodia';
import { parseDetalheCustodia, parseRegistroCustodia } from './custodia.contract';
import { estornoCustodia, originalCustodia } from './testing/custodia.fixtures';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideApiConfig } from '../../core/config/api.config';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';

function enviar(f: ComponentFixture<CustodiaEstornoDialogComponent>) {
  f.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })); f.detectChanges();
}
function motivo(f: ComponentFixture<CustodiaEstornoDialogComponent>, value: string) {
  const campo = f.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
  campo.value = value; campo.dispatchEvent(new Event('input', { bubbles: true })); f.detectChanges();
}
function confirmado(): DetalheCustodia {
  return parseDetalheCustodia(JSON.stringify({ movimentacao: originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 }),
    estorno: estornoCustodia() }), 1, 10);
}
describe('CustodiaEstornoDialogComponent', () => {
  afterEach(() => TestBed.resetTestingModule());
  async function criar(options: { realHttp?: boolean; alteracoes?: Record<string, unknown>; carteiraId?: number } = {}) {
    const post = new Subject<DetalheCustodia>(); const service = { estornar: vi.fn().mockReturnValue(post) };
    const dialog = { close: vi.fn(), disableClose: false };
    const data = { carteiraId: options.carteiraId ?? 1,
      movimentacao: { ...parseRegistroCustodia(JSON.stringify(originalCustodia()), 1), ...options.alteracoes } as MovimentacaoCustodia };
    await TestBed.configureTestingModule({ imports: [CustodiaEstornoDialogComponent], providers: [
      provideApiConfig('/test-api'), provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting(),
      ...(options.realHttp ? [] : [{ provide: CustodiaService, useValue: service }]), { provide: MAT_DIALOG_DATA, useValue: data },
      { provide: MatDialogRef, useValue: dialog }
    ] }).compileComponents();
    const fixture = TestBed.createComponent(CustodiaEstornoDialogComponent); fixture.detectChanges();
    return { fixture, service, dialog, post, data };
  }
  it('somente confirmação explícita envia estorno da original contextual, sem motivo obrigatório', async () => {
    const r = await criar(); expect(r.service.estornar).not.toHaveBeenCalled();
    enviar(r.fixture); expect(r.service.estornar).toHaveBeenCalledExactlyOnceWith(1, 10);
    expect(r.dialog.close).not.toHaveBeenCalled();
  });
  it('cancelar e abrir não executam POST e mantêm a original imutável', async () => {
    const r = await criar();
    const cancelar = [...r.fixture.nativeElement.querySelectorAll('button')].find((b: HTMLButtonElement) => b.textContent?.trim() === 'Cancelar');
    cancelar.click(); expect(r.dialog.close).toHaveBeenCalledWith(); expect(r.service.estornar).not.toHaveBeenCalled();
    expect(r.data.movimentacao.situacao).toBe('ATIVA'); expect(r.data.movimentacao.estornoId).toBeNull();
  });
  it.each([' Texto preservado ', '  ', 'x'.repeat(1000)])('envia somente motivo opcional sem alterar seu conteúdo %#', async text => {
    const r = await criar(); motivo(r.fixture, text); enviar(r.fixture);
    expect(r.service.estornar).toHaveBeenCalledExactlyOnceWith(1, 10, { motivo: text });
    expect(r.dialog.close).not.toHaveBeenCalled();
  });
  it('rejeita motivo acima de 1000 caracteres sem truncar ou enviar', async () => {
    const r = await criar(); const text = 'a'.repeat(1001); motivo(r.fixture, text); enviar(r.fixture);
    expect(r.fixture.nativeElement.querySelector('textarea').value).toBe(text);
    expect(r.fixture.nativeElement.textContent).toContain('no máximo 1.000 caracteres');
    expect(r.service.estornar).not.toHaveBeenCalled();
  });
  it('explica neutralização e auditoria, mantendo posição exata e identificação', async () => {
    const n = '1' + '0'.repeat(511), d = '1' + '0'.repeat(510) + '1';
    const r = await criar({ alteracoes: { posicaoCronologica: { ordemFinanceiraAncora: 0, numerador: n, denominador: d } } });
    const root: HTMLElement = r.fixture.nativeElement;
    for (const text of ['Movimentação #10', 'carteira #1', 'neutraliza retroativamente', 'permanece no histórico',
      'Não cria transferência inversa', `${n}/${d}`]) expect(root.textContent).toContain(text);
    expect(root.querySelector('form')?.getAttribute('aria-describedby')).toBe('custodia-estorno-description');
    expect(root.querySelector('textarea')?.getAttribute('maxlength')).toBe('1000');
  });
  it('bloqueia dupla confirmação/cancelamento e só retorna sucesso após resposta', async () => {
    const r = await criar(); enviar(r.fixture); enviar(r.fixture);
    expect(r.service.estornar).toHaveBeenCalledOnce(); expect(r.dialog.disableClose).toBe(true);
    expect(r.fixture.nativeElement.querySelector('textarea').disabled).toBe(true);
    expect(r.fixture.nativeElement.textContent).toContain('Confirmando estorno'); expect(r.dialog.close).not.toHaveBeenCalled();
    const resposta = confirmado(); r.post.next(resposta); r.post.next(resposta);
    expect(r.dialog.close).toHaveBeenCalledExactlyOnceWith(resposta);
    expect(r.data.movimentacao.situacao).toBe('ATIVA');
  });
  it.each([
    ['ESTORNO_DUPLICADO', 'já possui estorno'], ['MOVIMENTACAO_CUSTODIA_INVALIDADA', 'custódia posterior dependente'],
    ['CONFLITO_HISTORICO_QUANTITATIVO', 'déficit histórico'], ['POSICAO_INSUFICIENTE', 'posição disponível'],
    ['CONFLITO_TRANSACIONAL_TRANSITORIO', 'conflito temporário']
  ])('preserva motivo e detalhes na falha %s, sem sucesso fictício ou retry', async (code, message) => {
    const r = await criar(); motivo(r.fixture, 'Preservar'); enviar(r.fixture);
    r.post.error({ kind: 'standard', status: 409, code, message: 'Rejeição', details: { evento: null, saldoAntes: '-9999999999999.123456' } });
    r.fixture.detectChanges();
    expect(r.fixture.nativeElement.textContent).toContain(message); expect(r.fixture.nativeElement.textContent).toContain('-9999999999999.123456');
    expect(r.fixture.nativeElement.querySelector('textarea').value).toBe('Preservar');
    expect(r.fixture.nativeElement.querySelector('textarea').disabled).toBe(false);
    expect(r.dialog.disableClose).toBe(false); expect(r.service.estornar).toHaveBeenCalledOnce(); expect(r.dialog.close).not.toHaveBeenCalled();
  });
  it.each([404, 500])('falha HTTP %s não fecha dialog nem perde motivo', async status => {
    const r = await criar(); motivo(r.fixture, 'Motivo'); enviar(r.fixture);
    r.post.error({ kind: status === 404 ? 'standard' : 'technical', status, code: null, message: 'Falha segura', details: {} });
    r.fixture.detectChanges(); expect(r.fixture.nativeElement.textContent).toContain(status === 404 ? 'não encontrada nesta carteira' : 'Falha segura');
    expect(r.dialog.close).not.toHaveBeenCalled(); expect(r.fixture.nativeElement.querySelector('textarea').value).toBe('Motivo');
  });
  it.each([
    { situacao: 'ESTORNADA', estornoId: 20 }, { tipoRegistro: 'ESTORNO', natureza: 'ESTORNO', situacao: null }, { carteiraId: 2 }
  ])('não permite estornar registro não elegível %#', async alteracoes => {
    const r = await criar({ alteracoes }); enviar(r.fixture); expect(r.service.estornar).not.toHaveBeenCalled();
    expect(r.fixture.nativeElement.textContent).toContain('Somente uma movimentação original ATIVA');
  });
  it('contexto e original são capturados; respostas tardias após destruição não fecham dialog', async () => {
    const r = await criar(); r.data.carteiraId = 9; r.data.movimentacao.id = 999; enviar(r.fixture);
    expect(r.service.estornar).toHaveBeenCalledExactlyOnceWith(1, 10); r.fixture.destroy(); r.post.next(confirmado());
    expect(r.dialog.close).not.toHaveBeenCalled();
  });
  it('resposta fora do contexto não é apresentada como sucesso', async () => {
    const r = await criar(); enviar(r.fixture); const v = confirmado();
    r.post.next({ ...v, movimentacao: { ...v.movimentacao, carteiraId: 2 } }); r.fixture.detectChanges();
    expect(r.dialog.close).not.toHaveBeenCalled(); expect(r.fixture.nativeElement.querySelector('textarea').disabled).toBe(false);
  });
  it('serviço real envia apenas motivo e retorna vínculo persistido após HTTP 201 sem GET', async () => {
    const r = await criar({ realHttp: true }); const http = TestBed.inject(HttpTestingController);
    motivo(r.fixture, ' Correção '); enviar(r.fixture);
    const req = http.expectOne('/test-api/carteiras/1/movimentacoes-custodia/10/estornos');
    expect(req.request.method).toBe('POST'); expect(req.request.body).toEqual({ motivo: ' Correção ' });
    req.flush(JSON.stringify({ movimentacao: originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 }), estorno: estornoCustodia() }),
      { status: 201, statusText: 'Created' }); expect(r.dialog.close).toHaveBeenCalledWith(confirmado()); http.verify();
  });
  it('HTTP 409 real preserva erro de duplicidade e detalhes nulos sem POST repetido', async () => {
    const r = await criar({ realHttp: true }); const http = TestBed.inject(HttpTestingController);
    enviar(r.fixture); http.expectOne('/test-api/carteiras/1/movimentacoes-custodia/10/estornos').flush(
      '{"timeStamp":1,"status":409,"error":"Conflict","message":"Já estornada","path":"/custodia","code":"ESTORNO_DUPLICADO","details":{"evento":null}}',
      { status: 409, statusText: 'Conflict' }); r.fixture.detectChanges();
    expect(r.fixture.nativeElement.textContent).toContain('já possui estorno'); expect(r.dialog.close).not.toHaveBeenCalled(); http.verify();
  });
});
