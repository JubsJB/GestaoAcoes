import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatFormField } from '@angular/material/form-field';
import { By } from '@angular/platform-browser';
import { FormGroupDirective } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, of, Subject, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AcoesService } from '../acoes/acoes.service';
import { CorretorasService } from '../corretoras/corretoras.service';
import { CustodiaService } from './custodia.service';
import { DistribuicaoCorretorasService } from './distribuicao-corretoras.service';
import { CustodiaFormDialogComponent } from './custodia-form-dialog.component';
import { MovimentacaoCustodia } from './models/custodia';
import { parseRegistroCustodia } from './custodia.contract';
import { originalCustodia } from './testing/custodia.fixtures';
import { DistribuicaoCarteiraResponse } from './models/distribuicao-corretoras';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideApiConfig } from '../../core/config/api.config';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';

const carteira = { id: 1, nome: 'Principal', dataCriacao: '2026-10-01T12:00:00Z' };
const acoes = [{ id: 2, ticker: 'PETR4', mercado: 'BRASIL', moeda: 'BRL' },
  { id: 5, ticker: 'AAPL', mercado: 'EUA', moeda: 'USD' }];
function form(f: ComponentFixture<CustodiaFormDialogComponent>) {
  return f.debugElement.query(By.directive(FormGroupDirective)).injector.get(FormGroupDirective).form;
}
function preencher(f: ComponentFixture<CustodiaFormDialogComponent>, extra = {}) {
  form(f).patchValue({ natureza: 'TRANSFERENCIA', acaoKey: 'PETR4|BRASIL', corretoraOrigemId: 3,
    corretoraDestinoId: 4, quantidade: '40', dataEfetiva: '2026-10-01', ...extra }); f.detectChanges();
}
function enviar(f: ComponentFixture<CustodiaFormDialogComponent>) {
  f.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })); f.detectChanges();
}
describe('CustodiaFormDialogComponent', () => {
  it.each(['TRANSFERENCIA', 'ATRIBUICAO'])('%s acomoda mensagens curtas e multilinha no fluxo dinâmico, sem enviar', async natureza => {
    const r = await criar();
    form(r.fixture).controls['natureza'].setValue(natureza);
    form(r.fixture).markAllAsTouched(); r.fixture.detectChanges();
    const campos = r.fixture.debugElement.queryAll(By.directive(MatFormField));
    expect(campos).toHaveLength(natureza === 'TRANSFERENCIA' ? 6 : 5);
    for (const campo of campos) expect(campo.injector.get(MatFormField).subscriptSizing).toBe('dynamic');
    const quantidade = campos.find(c => c.nativeElement.querySelector('[formControlName="quantidade"]'))!;
    const data = campos.find(c => c.nativeElement.querySelector('[formControlName="dataEfetiva"]'))!;
    expect(quantidade.nativeElement.querySelector('mat-error')?.textContent).toContain('13 inteiros e 6 decimais');
    expect(data.nativeElement.querySelector('mat-error')?.textContent).toContain('data civil válida');
    expect(campos.find(c => c.nativeElement.querySelector('[formControlName="acaoKey"]'))!.nativeElement.textContent).toContain('Selecione uma ação cadastrada.');
    expect(quantidade.nativeElement.querySelector('.mat-mdc-form-field-subscript-dynamic-size')).not.toBeNull();
    expect(data.nativeElement.querySelector('.mat-mdc-form-field-subscript-dynamic-size')).not.toBeNull();
    expect(r.service.cadastrar).not.toHaveBeenCalled();
  });
  it('anuncia carregamentos textuais sem expor um segundo anúncio do spinner decorativo', async () => {
    const referencias = new Subject<typeof acoes>();
    const r = await criar({ referencias });
    const status: HTMLElement = r.fixture.nativeElement.querySelector('[role="status"]');
    expect(status.textContent).toContain('Carregando ações e corretoras');
    expect(status.getAttribute('aria-live')).toBe('polite');
    expect(status.querySelector('mat-spinner')?.getAttribute('aria-hidden')).toBe('true');
    referencias.next(acoes); referencias.complete(); r.fixture.detectChanges();
    preencher(r.fixture); enviar(r.fixture);
    expect(r.fixture.nativeElement.querySelector('[role="status"]')?.textContent).toContain('Registrando custódia');
  });
  afterEach(() => { TestBed.resetTestingModule(); vi.useRealTimers(); });
  async function criar(options: { distribuicao?: Observable<DistribuicaoCarteiraResponse>;
    referencias?: Observable<typeof acoes>; corretoras?: Observable<unknown[]>; realHttp?: boolean } = {}) {
    const post = new Subject<MovimentacaoCustodia>();
    const service = { cadastrar: vi.fn().mockReturnValue(post) };
    const refs = { listar: vi.fn().mockReturnValue(options.referencias ?? of(acoes)) };
    const brokers = { listar: vi.fn().mockReturnValue(options.corretoras ?? of([{ id: 3, nomeFantasia: 'Origem' }, { id: 4, razaoSocial: 'Destino' }])) };
    const distribution = { consultar: vi.fn().mockReturnValue(options.distribuicao ?? of({ carteiraId: 1, posicoes: [] })) };
    const dialog = { close: vi.fn(), disableClose: false };
    const data = { carteira: { ...carteira } };
    await TestBed.configureTestingModule({ imports: [CustodiaFormDialogComponent], providers: [
      provideApiConfig('/test-api'), provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting(),
      { provide: MAT_DIALOG_DATA, useValue: data }, { provide: MatDialogRef, useValue: dialog },
      ...(options.realHttp ? [] : [{ provide: CustodiaService, useValue: service }]), { provide: AcoesService, useValue: refs },
      { provide: CorretorasService, useValue: brokers }, { provide: DistribuicaoCorretorasService, useValue: distribution }
    ] }).compileComponents();
    const fixture = TestBed.createComponent(CustodiaFormDialogComponent); fixture.detectChanges();
    return { fixture, service, refs, brokers, distribution, dialog, post, data };
  }
  it('registra transferência na carteira capturada sem campos financeiros ou de auditoria', async () => {
    const r = await criar(); preencher(r.fixture); enviar(r.fixture);
    expect(r.service.cadastrar).toHaveBeenCalledExactlyOnceWith(1, { natureza: 'TRANSFERENCIA', ticker: 'PETR4',
      mercado: 'BRASIL', corretoraOrigemId: 3, corretoraDestinoId: 4, quantidade: '40', dataEfetiva: '2026-10-01' });
  });
  it('mostra saldo autoritativo atual com aviso, sem usá-lo como bloqueio de retroatividade', async () => {
    const distribuicao = { carteiraId: 1, posicoes: [{ acaoId: 2, ticker: 'PETR4', mercado: 'BRASIL',
      corretoras: [{ corretoraId: 3, nome: 'Origem', quantidadeLiquida: '10' }], diagnosticosCorretoras: [] }] } as unknown as DistribuicaoCarteiraResponse;
    const r = await criar({ distribuicao: of(distribuicao) }); preencher(r.fixture, { quantidade: '40' });
    expect(r.fixture.nativeElement.textContent).toContain('Saldo atual da origem: 10');
    expect(r.fixture.nativeElement.textContent).toContain('não garante disponibilidade na data efetiva');
    enviar(r.fixture); expect(r.service.cadastrar).toHaveBeenCalledOnce();
    expect(r.distribution.consultar).toHaveBeenCalledExactlyOnceWith(1);
  });
  it('alterna para atribuição com origem null fixa e volta exigindo origem cadastrada', async () => {
    const r = await criar(); preencher(r.fixture);
    form(r.fixture).controls['natureza'].setValue('ATRIBUICAO'); r.fixture.detectChanges();
    expect(form(r.fixture).controls['corretoraOrigemId'].value).toBeNull();
    expect(r.fixture.nativeElement.textContent).toContain('Sem corretora informada');
    expect(r.fixture.nativeElement.querySelector('mat-select[formControlName="corretoraOrigemId"]')).toBeNull();
    enviar(r.fixture);
    expect(r.service.cadastrar).toHaveBeenCalledExactlyOnceWith(1, expect.objectContaining({ natureza: 'ATRIBUICAO', corretoraOrigemId: null }));
    r.post.error({ status: 409, kind: 'standard', code: 'SALDO_ORIGEM_INSUFICIENTE', message: 'Saldo', details: {} });
    form(r.fixture).controls['natureza'].setValue('TRANSFERENCIA'); r.fixture.detectChanges();
    expect(form(r.fixture).controls['corretoraOrigemId'].invalid).toBe(true);
    enviar(r.fixture); expect(r.service.cadastrar).toHaveBeenCalledOnce();
  });
  it.each([
    { corretoraDestinoId: 3 }, { corretoraOrigemId: null }, { corretoraOrigemId: 999 }, { corretoraDestinoId: 999 },
    { acaoKey: 'INEXISTENTE|BRASIL' }, { natureza: 'ESTORNO' }, { quantidade: '' }, { quantidade: '0' },
    { quantidade: '-1' }, { quantidade: '0,5' }, { quantidade: '1.0000001' }, { quantidade: '10000000000000' },
    { quantidade: '9007199254740993' }, { dataEfetiva: '' }, { dataEfetiva: '2026-02-30' }, { dataEfetiva: '2999-01-01' }
  ])('bloqueia entrada inválida %# antes do POST', async extra => {
    const r = await criar(); preencher(r.fixture, extra); enviar(r.fixture);
    expect(r.service.cadastrar).not.toHaveBeenCalled(); expect(r.dialog.close).not.toHaveBeenCalled();
  });
  it('identifica corretoras iguais e campos obrigatórios sem criar referências', async () => {
    const r = await criar(); preencher(r.fixture, { corretoraDestinoId: 3 }); enviar(r.fixture);
    expect(r.fixture.nativeElement.textContent).toContain('Origem e destino devem ser corretoras distintas');
    form(r.fixture).reset(); enviar(r.fixture); expect(r.service.cadastrar).not.toHaveBeenCalled();
    expect(r.refs.listar).toHaveBeenCalledOnce(); expect(r.brokers.listar).toHaveBeenCalledOnce();
  });
  it.each(['0,000001', '9999999999999,123456'])('envia EUA %s como string sem arredondar', async quantidade => {
    const r = await criar(); preencher(r.fixture, { acaoKey: 'AAPL|EUA', quantidade }); enviar(r.fixture);
    expect(r.service.cadastrar).toHaveBeenCalledWith(1, expect.objectContaining({ ticker: 'AAPL', mercado: 'EUA',
      quantidade: quantidade.replace(',', '.') }));
  });
  it('envia BRASIL inteiro matemático com escala e data civil sem UTC', async () => {
    const r = await criar(); preencher(r.fixture, { quantidade: '100,000000', dataEfetiva: '2026-10-01' }); enviar(r.fixture);
    expect(r.service.cadastrar).toHaveBeenCalledWith(1, expect.objectContaining({ quantidade: '100.000000', dataEfetiva: '2026-10-01' }));
  });
  it('valida futuro pela zona do mercado selecionado, não pela data UTC', async () => {
    const r = await criar(); vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date('2026-10-02T03:30:00Z'));
    preencher(r.fixture, { acaoKey: 'AAPL|EUA', dataEfetiva: '2026-10-02' }); enviar(r.fixture);
    expect(r.service.cadastrar).not.toHaveBeenCalled();
    form(r.fixture).controls['acaoKey'].setValue('PETR4|BRASIL'); enviar(r.fixture);
    expect(r.service.cadastrar).toHaveBeenCalledWith(1, expect.objectContaining({ mercado: 'BRASIL', dataEfetiva: '2026-10-02' }));
  });
  it('seleciona ticker e mercado, mesmo quando existem tickers iguais', async () => {
    const r = await criar({ referencias: of([...acoes, { id: 6, ticker: 'PETR4', mercado: 'EUA', moeda: 'USD' }]) });
    preencher(r.fixture, { acaoKey: 'PETR4|EUA', quantidade: '0,5' }); enviar(r.fixture);
    expect(r.service.cadastrar).toHaveBeenCalledWith(1, expect.objectContaining({ ticker: 'PETR4', mercado: 'EUA', quantidade: '0.5' }));
  });
  it('conserva o contexto capturado mesmo se o objeto externo for alterado', async () => {
    const r = await criar(); r.data.carteira.id = 9; r.data.carteira.nome = 'Outra'; preencher(r.fixture); enviar(r.fixture);
    expect(r.service.cadastrar).toHaveBeenCalledWith(1, expect.anything());
    expect(r.fixture.nativeElement.textContent).toContain('Principal · #1');
  });
  it('cancelamento e abertura não cadastram, e o formulário não possui campos proibidos', async () => {
    const r = await criar();
    expect(Object.keys(form(r.fixture).controls)).toEqual(['natureza', 'acaoKey', 'corretoraOrigemId', 'corretoraDestinoId', 'quantidade', 'dataEfetiva']);
    const cancelar = [...r.fixture.nativeElement.querySelectorAll('button')].find((b: HTMLButtonElement) => b.textContent?.trim() === 'Cancelar');
    cancelar.click(); expect(r.dialog.close).toHaveBeenCalledWith(); expect(r.service.cadastrar).not.toHaveBeenCalled();
  });
  it('envio pendente bloqueia controles, cancelamento e submissão repetida; sucesso devolve DTO', async () => {
    const r = await criar(); preencher(r.fixture); enviar(r.fixture); enviar(r.fixture);
    expect(r.service.cadastrar).toHaveBeenCalledOnce(); expect(form(r.fixture).disabled).toBe(true);
    expect(r.dialog.disableClose).toBe(true); expect(r.fixture.nativeElement.textContent).toContain('Registrando custódia');
    const cancelar = [...r.fixture.nativeElement.querySelectorAll('button')].find((b: HTMLButtonElement) => b.textContent?.trim() === 'Cancelar');
    expect(cancelar.disabled).toBe(true); cancelar.click(); expect(r.dialog.close).not.toHaveBeenCalled();
    const original = parseRegistroCustodia(JSON.stringify(originalCustodia()), 1) as MovimentacaoCustodia;
    r.post.next(original); r.post.complete(); expect(r.dialog.close).toHaveBeenCalledExactlyOnceWith(original);
    expect(r.distribution.consultar).toHaveBeenCalledOnce();
  });
  it.each([
    ['SALDO_ORIGEM_INSUFICIENTE', 'origem não tem saldo'], ['DATA_EFETIVA_FUTURA', 'não pode ser futura'],
    ['POSICAO_CUSTODIA_INDISPONIVEL', 'posição cronológica disponível'],
    ['CONFLITO_HISTORICO_QUANTITATIVO', 'déficit histórico'], ['MOVIMENTACAO_CUSTODIA_INVALIDADA', 'custódia já registrada'],
    ['CONFLITO_TRANSACIONAL_TRANSITORIO', 'conflito temporário']
  ])('preserva formulário e detalhes exatos na rejeição %s sem retry', async (code, trecho) => {
    const r = await criar(); preencher(r.fixture); const antes = form(r.fixture).getRawValue(); enviar(r.fixture);
    r.post.error({ status: 409, kind: 'standard', code, message: 'Rejeição', details: {
      quantidadeDisponivel: '9999999999999.123456', evento: null } } satisfies Partial<NormalizedHttpError>);
    r.fixture.detectChanges(); expect(form(r.fixture).getRawValue()).toEqual(antes);
    expect(form(r.fixture).enabled).toBe(true); expect(r.dialog.disableClose).toBe(false);
    expect(r.fixture.nativeElement.textContent).toContain(trecho);
    expect(r.fixture.nativeElement.textContent).toContain('9999999999999.123456');
    expect(r.service.cadastrar).toHaveBeenCalledOnce(); expect(r.dialog.close).not.toHaveBeenCalled();
  });
  it('referência inexistente e falha técnica não geram sucesso nem perdem dados', async () => {
    for (const status of [404, 500]) {
      const r = await criar(); preencher(r.fixture); enviar(r.fixture);
      r.post.error({ status, kind: status === 404 ? 'standard' : 'technical', code: null, message: 'Falha segura', details: {} });
      r.fixture.detectChanges(); expect(form(r.fixture).controls['quantidade'].value).toBe('40');
      expect(r.fixture.nativeElement.textContent).toContain(status === 404 ? 'não está mais disponível' : 'Falha segura');
      expect(r.dialog.close).not.toHaveBeenCalled(); r.fixture.destroy(); TestBed.resetTestingModule();
    }
  });
  it('distribuição indisponível não bloqueia formulário nem repete consulta automaticamente', async () => {
    const r = await criar({ distribuicao: throwError(() => new Error('indisponível')) }); preencher(r.fixture); enviar(r.fixture);
    expect(r.fixture.nativeElement.textContent).toContain('Saldo atual indisponível');
    expect(r.service.cadastrar).toHaveBeenCalledOnce(); expect(r.distribution.consultar).toHaveBeenCalledOnce();
  });
  it('grupo sem corretora e grupo zerado usam valores retornados, sem somas locais', async () => {
    const d = { carteiraId: 1, posicoes: [{ acaoId: 2, ticker: 'PETR4', mercado: 'BRASIL', corretoras: [],
      diagnosticosCorretoras: [{ corretoraId: null, quantidadeFinal: '60' }, { corretoraId: 3, quantidadeFinal: '0' }] }] } as unknown as DistribuicaoCarteiraResponse;
    const r = await criar({ distribuicao: of(d) }); preencher(r.fixture, { natureza: 'ATRIBUICAO' });
    expect(r.fixture.nativeElement.textContent).toContain('Saldo atual da origem: 60');
    form(r.fixture).controls['natureza'].setValue('TRANSFERENCIA'); form(r.fixture).controls['corretoraOrigemId'].setValue(3); r.fixture.detectChanges();
    expect(r.fixture.nativeElement.textContent).toContain('Saldo atual da origem: 0');
    enviar(r.fixture); expect(r.service.cadastrar).toHaveBeenCalledOnce();
  });
  it('ausência de posição aberta não fabrica zero nem impede retroatividade', async () => {
    const r = await criar(); preencher(r.fixture); expect(r.fixture.nativeElement.textContent).toContain('Não há saldo atual informado');
    enviar(r.fixture); expect(r.service.cadastrar).toHaveBeenCalledOnce();
  });
  it('não apresenta saldo recebido para outra carteira', async () => {
    const r = await criar({ distribuicao: of({ carteiraId: 9, posicoes: [] }) }); preencher(r.fixture);
    expect(r.fixture.nativeElement.textContent).toContain('Saldo atual indisponível'); enviar(r.fixture);
    expect(r.service.cadastrar).toHaveBeenCalledWith(1, expect.anything());
  });
  it('ignora respostas de referências, saldo e POST após destruição do contexto', async () => {
    const referencias = new Subject<typeof acoes>(), saldo = new Subject<DistribuicaoCarteiraResponse>();
    const r = await criar({ referencias, distribuicao: saldo }); r.fixture.destroy();
    referencias.next(acoes); referencias.complete(); saldo.next({ carteiraId: 1, posicoes: [] });
    expect(r.service.cadastrar).not.toHaveBeenCalled(); expect(r.dialog.close).not.toHaveBeenCalled();
    TestBed.resetTestingModule(); const s = await criar(); preencher(s.fixture); enviar(s.fixture); s.fixture.destroy();
    s.post.next(parseRegistroCustodia(JSON.stringify(originalCustodia()), 1) as MovimentacaoCustodia);
    expect(s.dialog.close).not.toHaveBeenCalled(); expect(s.service.cadastrar).toHaveBeenCalledOnce();
  });
  it('referências carregando, ausentes ou com falha bloqueiam somente cadastro, com cancelamento disponível', async () => {
    for (const referencias of [new Subject<typeof acoes>(), of([]), throwError(() => new Error('falha'))]) {
      const r = await criar({ referencias }); preencher(r.fixture); enviar(r.fixture);
      expect(r.service.cadastrar).not.toHaveBeenCalled();
      const b = [...r.fixture.nativeElement.querySelectorAll('button')].find((v: HTMLButtonElement) => v.textContent?.trim() === 'Cancelar');
      expect(b.disabled).toBe(false); r.fixture.destroy(); TestBed.resetTestingModule();
    }
  });
  it('expõe título, descrição, labels e validações identificáveis sem novos links de cadastro', async () => {
    const r = await criar(); const root: HTMLElement = r.fixture.nativeElement;
    expect(root.querySelector('#custodia-form-title')?.textContent).toContain('custódia');
    expect(root.querySelector('form')?.getAttribute('aria-describedby')).toBe('custodia-form-description');
    for (const label of ['Natureza', 'Ação', 'Corretora de origem', 'Corretora de destino', 'Quantidade', 'Data efetiva'])
      expect([...root.querySelectorAll('mat-label')].some(n => n.textContent === label)).toBe(true);
    expect(root.querySelector('input[formControlName="quantidade"]')?.getAttribute('inputmode')).toBe('decimal');
    expect(root.querySelectorAll('a')).toHaveLength(0); enviar(r.fixture); expect(root.querySelectorAll('mat-error').length).toBeGreaterThan(0);
  });
  it('percorre formulário e serviço real até POST 201, devolvendo resposta persistida sem GET', async () => {
    const r = await criar({ realHttp: true }); const http = TestBed.inject(HttpTestingController);
    preencher(r.fixture, { natureza: 'ATRIBUICAO', acaoKey: 'AAPL|EUA', quantidade: '9999999999999,123456' }); enviar(r.fixture);
    const req = http.expectOne('/test-api/carteiras/1/movimentacoes-custodia');
    expect(req.request.body).toEqual({ natureza: 'ATRIBUICAO', ticker: 'AAPL', mercado: 'EUA', corretoraOrigemId: null,
      corretoraDestinoId: 4, quantidade: '9999999999999.123456', dataEfetiva: '2026-10-01' });
    req.flush(JSON.stringify(originalCustodia({ natureza: 'ATRIBUICAO', corretoraOrigemId: null,
      ticker: 'AAPL', mercado: 'EUA' })).replace('"quantidade":40', '"quantidade":9999999999999.123456'),
      { status: 201, statusText: 'Created' });
    expect(r.dialog.close).toHaveBeenCalledWith(expect.objectContaining({ quantidade: '9999999999999.123456', carteiraId: 1 }));
    http.verify();
  });
  it('percorre HTTP 409 e interceptor preservando detalhes nulos e formulário, sem POST repetido', async () => {
    const r = await criar({ realHttp: true }); const http = TestBed.inject(HttpTestingController);
    preencher(r.fixture); const antes = form(r.fixture).getRawValue(); enviar(r.fixture);
    http.expectOne('/test-api/carteiras/1/movimentacoes-custodia').flush(
      '{"timeStamp":1,"status":409,"error":"Conflict","message":"Sem posição","path":"/custodia","code":"POSICAO_CUSTODIA_INDISPONIVEL","details":{"evento":null,"quantidadeDisponivel":9999999999999.123456,"motivo":"MOVIMENTACAO_CUSTODIA_INVALIDADA"}}',
      { status: 409, statusText: 'Conflict' }); r.fixture.detectChanges();
    expect(form(r.fixture).getRawValue()).toEqual(antes); expect(r.dialog.close).not.toHaveBeenCalled();
    expect(r.fixture.nativeElement.textContent).toContain('9999999999999.123456');
    expect(r.fixture.nativeElement.textContent).toContain('posição cronológica disponível'); http.verify();
  });
});
