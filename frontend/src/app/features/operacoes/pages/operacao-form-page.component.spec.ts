import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { provideApiConfig } from '../../../core/config/api.config';
import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { httpErrorInterceptor } from '../../../core/http/http-error.interceptor';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { AcoesService } from '../../acoes/acoes.service';
import { CarteirasService } from '../../carteiras/carteiras.service';
import { CarteiraResponse } from '../../carteiras/models/carteira';
import { CorretorasService } from '../../corretoras/corretoras.service';
import { OperacaoResponse, PreviaPrecoCompraResponse, SugestaoPrecoVendaResponse } from '../models/operacao';
import { OperacoesService } from '../operacoes.service';
import { OperacaoFormPageComponent } from './operacao-form-page.component';

const CARTEIRA: CarteiraResponse = { id: 1, nome: 'Principal', dataCriacao: '' };
const PREVIEW: PreviaPrecoCompraResponse = { ticker: 'AAPL', mercado: 'EUA', moeda: 'USD', dataCotacao: '2026-08-31', precoUnitario: '42.30' };
const RESPONSE: OperacaoResponse = { id: 9, carteiraId: 1, ticker: 'AAPL', mercado: 'EUA', corretoraId: null, tipo: 'VENDA', quantidade: '0.5', precoUnitario: '10', dataOperacao: '2026-08-31', ordemNoDia: 2, valorTotal: '5' };
type TestComponent = { form: FormGroup; submit(): void; submitting(): boolean; submitBlocked(): boolean };
function setContext(component: TestComponent, tipo: 'COMPRA' | 'VENDA', action = 'AAPL|EUA', date = '2026-08-31') { component.form.patchValue({ carteiraId: 1, acaoKey: action, tipo, quantidade: action.includes('BRASIL') ? '1' : '0,5', dataOperacao: date }); }

describe('OperacaoFormPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());
  async function create(options: { post?: Subject<OperacaoResponse>; context?: CarteiraResponse; preview?: Subject<PreviaPrecoCompraResponse>; suggestion?: Subject<SugestaoPrecoVendaResponse> } = {}) {
    const operations = { cadastrar: vi.fn().mockReturnValue(options.post ?? new Subject<OperacaoResponse>()), obterPreviaCompra: vi.fn().mockReturnValue(options.preview ?? of(PREVIEW)), obterSugestaoPrecoVenda: vi.fn().mockReturnValue(options.suggestion ?? of({ precoUnitarioSugerido: '10' })) };
    const providers: object[] = [provideRouter([]), { provide: OperacoesService, useValue: operations }, { provide: CarteirasService, useValue: { listar: () => of([CARTEIRA]) } }, { provide: AcoesService, useValue: { listar: () => of([{ id: 2, ticker: 'AAPL', nomeEmpresa: 'Apple', mercado: 'EUA', moeda: 'USD' }, { id: 3, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL', moeda: 'BRL' }]) } }, { provide: CorretorasService, useValue: { listar: () => of([]) } }, { provide: SuccessToastService, useValue: { show: vi.fn() } }];
    if (options.context) providers.push({ provide: MAT_DIALOG_DATA, useValue: { carteira: options.context } });
    await TestBed.configureTestingModule({ imports: [OperacaoFormPageComponent], providers }).compileComponents();
    const fixture = TestBed.createComponent(OperacaoFormPageComponent); fixture.detectChanges();
    return { fixture, component: fixture.componentInstance as unknown as TestComponent, operations };
  }

  it.each([['AAPL|EUA', 'USD', 'US$'], ['PETR4|BRASIL', 'BRL', 'R$']] as const)('COMPRA mostra preço readonly em %s/%s e omite preço/ordem no POST', async (action, moeda, prefix) => {
    const preview = of({ ...PREVIEW, ticker: action.split('|')[0], mercado: action.split('|')[1], moeda }); const { fixture, component, operations } = await create({ preview: preview as never }); setContext(component, 'COMPRA', action); fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('[formcontrolname="precoUnitario"]') as HTMLInputElement; expect(input.readOnly).toBe(true); expect(input.getAttribute('aria-readonly')).toBe('true'); expect(input.value).toBe('42.30'); expect(fixture.nativeElement.textContent).toContain(prefix);
    component.submit(); const payload = operations.cadastrar.mock.calls[0][0]; expect(payload.tipo).toBe('COMPRA'); expect(payload).not.toHaveProperty('precoUnitario'); expect(payload).not.toHaveProperty('ordemNoDia');
  });

  it('COMPRA invalida no loading/data e ignora resposta atrasada', async () => {
    const first = new Subject<PreviaPrecoCompraResponse>(); const second = new Subject<PreviaPrecoCompraResponse>(); const { component, operations } = await create(); operations.obterPreviaCompra.mockReturnValueOnce(first).mockReturnValueOnce(second); setContext(component, 'COMPRA'); expect(component.submitBlocked()).toBe(true); component.form.controls['dataOperacao'].setValue('2026-08-30'); first.next({ ...PREVIEW, precoUnitario: '1' }); expect(component.form.controls['precoUnitario'].value).toBe(''); second.next({ ...PREVIEW, precoUnitario: '2' }); second.complete(); expect(component.form.controls['precoUnitario'].value).toBe('2'); expect(component.submitBlocked()).toBe(false);
  });

  it.each([['REQUEST_INVALIDO', 'Backend'], ['COTACAO_HISTORICA_INDISPONIVEL', 'Não foi encontrado fechamento'], ['HISTORICO_COTACAO_FORA_DO_ALCANCE', 'fora do histórico disponível'], ['TICKER_INEXISTENTE', 'ticker informado'], ['LIMITE_REQUISICOES_EXCEDIDO', 'limite de requisições']] as const)('erro %s bloqueia COMPRA e preserva formulário', async (code, message) => {
    const error = { status: 422, code, message: 'Backend', details: {} } as NormalizedHttpError; const { fixture, component, operations } = await create(); operations.obterPreviaCompra.mockReturnValueOnce(throwError(() => error)); setContext(component, 'COMPRA'); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain(message); expect(component.form.controls['quantidade'].value).toBe('0,5'); expect(component.submitBlocked()).toBe(true);
  });

  it.each([502, 503, 504])('mantém erro técnico %i e bloqueia COMPRA', async status => {
    const error = { status, code: null, message: 'Falha técnica na comunicação HTTP.', details: {} } as NormalizedHttpError; const { fixture, component, operations } = await create(); operations.obterPreviaCompra.mockReturnValueOnce(throwError(() => error)); setContext(component, 'COMPRA'); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Falha técnica'); expect(component.submitBlocked()).toBe(true);
  });

  it('VENDA aplica sugestão editável, aceita maior/menor e envia valor final', async () => {
    const { fixture, component, operations } = await create(); setContext(component, 'VENDA'); fixture.detectChanges(); const input = fixture.nativeElement.querySelector('[formcontrolname="precoUnitario"]') as HTMLInputElement; expect(input.readOnly).toBe(false); expect(input.value).toBe('10'); component.form.controls['precoUnitario'].setValue('12'); component.submit(); expect(operations.cadastrar.mock.calls[0][0].precoUnitario).toBe('12');
  });

  it('sugestão nula deixa VENDA vazia, editável e required', async () => {
    const { fixture, component } = await create({ suggestion: of({ precoUnitarioSugerido: null }) as never }); setContext(component, 'VENDA'); fixture.detectChanges(); const input = fixture.nativeElement.querySelector('[formcontrolname="precoUnitario"]') as HTMLInputElement; expect(input.value).toBe(''); expect(input.readOnly).toBe(false); expect(component.form.controls['precoUnitario'].hasError('required')).toBe(true); expect(fixture.nativeElement.querySelector('.price-status app-feedback-alert')).toBeNull();
  });

  it('sugestão tardia não sobrescreve edição manual', async () => {
    const suggestion = new Subject<SugestaoPrecoVendaResponse>(); const { component } = await create({ suggestion }); setContext(component, 'VENDA'); component.form.controls['precoUnitario'].setValue('77'); suggestion.next({ precoUnitarioSugerido: '10' }); suggestion.complete(); expect(component.form.controls['precoUnitario'].value).toBe('77');
  });

  it('mudanças de carteira, ação e data limpam sugestão e descartam resposta antiga', async () => {
    const old = new Subject<SugestaoPrecoVendaResponse>(); const current = new Subject<SugestaoPrecoVendaResponse>(); const { component, operations } = await create(); operations.obterSugestaoPrecoVenda.mockReturnValueOnce(old).mockReturnValue(current); setContext(component, 'VENDA'); component.form.controls['precoUnitario'].setValue('33'); component.form.controls['carteiraId'].setValue(2); expect(component.form.controls['precoUnitario'].value).toBe(''); old.next({ precoUnitarioSugerido: '99' }); expect(component.form.controls['precoUnitario'].value).toBe(''); component.form.controls['acaoKey'].setValue('PETR4|BRASIL'); expect(component.form.controls['precoUnitario'].value).toBe(''); component.form.controls['dataOperacao'].setValue('2026-08-30'); expect(component.form.controls['precoUnitario'].value).toBe('');
  });

  it('troca tipos sem reutilizar preço e preserva validators/double-submit', async () => {
    const post = new Subject<OperacaoResponse>(); const { fixture, component, operations } = await create({ post }); setContext(component, 'COMPRA'); expect(component.form.controls['precoUnitario'].value).toBe('42.30'); component.form.controls['tipo'].setValue('VENDA'); expect(component.form.controls['precoUnitario'].value).toBe('10'); component.form.controls['precoUnitario'].setValue('12345678901234'); expect(component.form.controls['precoUnitario'].hasError('integerDigits')).toBe(true); component.form.controls['precoUnitario'].setValue('10'); component.submit(); component.submit(); expect(operations.cadastrar).toHaveBeenCalledTimes(1); post.error({}); component.form.controls['tipo'].setValue('COMPRA'); fixture.detectChanges(); expect(component.form.controls['precoUnitario'].value).toBe('42.30'); expect((fixture.nativeElement.querySelector('[formcontrolname="precoUnitario"]') as HTMLInputElement).readOnly).toBe(true);
  });

  it('reutiliza carteira contextual em sugestão e payloads', async () => {
    const fixed = { ...CARTEIRA, id: 77 }; const purchase = await create({ context: fixed }); setContext(purchase.component, 'COMPRA'); purchase.component.submit(); expect(purchase.operations.cadastrar.mock.calls[0][0].carteiraId).toBe(77); purchase.fixture.destroy(); TestBed.resetTestingModule(); const sale = await create({ context: fixed }); setContext(sale.component, 'VENDA'); sale.component.form.controls['precoUnitario'].setValue('15'); sale.component.submit(); expect(sale.operations.obterSugestaoPrecoVenda).toHaveBeenCalledWith(77, 'AAPL', 'EUA', '2026-08-31'); expect(sale.operations.cadastrar.mock.calls[0][0]).toEqual(expect.objectContaining({ carteiraId: 77, tipo: 'VENDA', precoUnitario: '15' }));
  });
});

describe('OperacaoForm integração de erro HTTP', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('percorre HttpErrorResponse, interceptor, parsing lossless, service e UI', async () => {
    await TestBed.configureTestingModule({ imports: [OperacaoFormPageComponent], providers: [provideRouter([]), provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting(), provideApiConfig(), { provide: CarteirasService, useValue: { listar: () => of([CARTEIRA]) } }, { provide: AcoesService, useValue: { listar: () => of([{ id: 2, ticker: 'AAPL', nomeEmpresa: 'Apple', mercado: 'EUA', moeda: 'USD' }]) } }, { provide: CorretorasService, useValue: { listar: () => of([]) } }, { provide: SuccessToastService, useValue: { show: vi.fn() } }] }).compileComponents();
    const http = TestBed.inject(HttpTestingController); const fixture = TestBed.createComponent(OperacaoFormPageComponent); fixture.detectChanges(); const component = fixture.componentInstance as unknown as TestComponent; setContext(component, 'COMPRA'); const request = http.expectOne(req => req.url === '/api/operacoes/previa-compra'); request.flush('{"timeStamp":1,"status":422,"error":"Unprocessable Entity","code":"HISTORICO_COTACAO_FORA_DO_ALCANCE","message":"Data fora do alcance","path":"/operacoes/previa-compra","details":{"dataOperacao":"2026-08-31"}}', { status: 422, statusText: 'Unprocessable Entity' }); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('fora do histórico disponível'); expect(fixture.nativeElement.textContent).toContain('Data fora do alcance'); expect(component.submitBlocked()).toBe(true); http.verify();
  });
});
