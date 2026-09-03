import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideApiConfig } from '../../core/config/api.config';
import { OperacaoCompraCreateRequest, OperacaoVendaCreateRequest } from './models/operacao';
import { OperacoesService } from './operacoes.service';

const PURCHASE: OperacaoCompraCreateRequest = {
  carteiraId: 2, ticker: 'AAPL', mercado: 'EUA', corretoraId: null,
  tipo: 'COMPRA', quantidade: '0.123456', dataOperacao: '2026-09-01'
};
const SALE: OperacaoVendaCreateRequest = {
  carteiraId: 2, ticker: 'AAPL', mercado: 'EUA', tipo: 'VENDA',
  quantidade: '0.123456', precoUnitario: '9999999999999.123456', dataOperacao: '2026-09-01'
};
const RESPONSE = '{"id":7,"carteiraId":2,"ticker":"AAPL","mercado":"EUA","corretoraId":null,"tipo":"COMPRA","quantidade":0.123456,"precoUnitario":9999999999999.123456,"dataOperacao":"2026-09-01","ordemNoDia":1,"valorTotal":12345678901234567890123456.123456789012}';

describe('OperacoesService', () => {
  let service: OperacoesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    service = TestBed.inject(OperacoesService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('envia payload exato de COMPRA sem preço, ordem ou campos derivados', () => {
    let result: unknown;
    service.cadastrar(PURCHASE).subscribe(value => result = value);
    const request = http.expectOne('/api/operacoes');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(PURCHASE);
    expect(request.request.body).not.toHaveProperty('precoUnitario');
    expect(request.request.body).not.toHaveProperty('ordemNoDia');
    expect(request.request.body).not.toHaveProperty('valorTotal');
    expect(request.request.body).not.toHaveProperty('id');
    expect(request.request.body).not.toHaveProperty('acaoId');
    request.flush(RESPONSE);
    expect((result as { precoUnitario: string }).precoUnitario).toBe('9999999999999.123456');
    expect((result as { valorTotal: string }).valorTotal).toBe('12345678901234567890123456.123456789012');
  });

  it('envia payload exato de VENDA com preço e sem ordem', () => {
    service.cadastrar(SALE).subscribe();
    const request = http.expectOne('/api/operacoes');
    expect(request.request.body).toEqual(SALE);
    expect(request.request.body.precoUnitario).toBe('9999999999999.123456');
    expect(request.request.body).not.toHaveProperty('ordemNoDia');
    request.flush(RESPONSE);
  });

  it('aceita corretora nula ou omitida nos dois tipos', () => {
    service.cadastrar(PURCHASE).subscribe();
    let request = http.expectOne('/api/operacoes');
    expect(request.request.body.corretoraId).toBeNull();
    request.flush(RESPONSE);
    service.cadastrar(SALE).subscribe();
    request = http.expectOne('/api/operacoes');
    expect(request.request.body).not.toHaveProperty('corretoraId');
    request.flush(RESPONSE);
  });

  it('não repete automaticamente POST que falhou', () => {
    service.cadastrar(PURCHASE).subscribe({ error: () => undefined });
    const request = http.expectOne('/api/operacoes');
    request.flush('{"status":503,"code":"SERVICO_EXTERNO_INDISPONIVEL","message":"Indisponível","details":{}}', { status: 503, statusText: 'Unavailable' });
    http.expectNone('/api/operacoes');
  });

  it('preserva os três GETs de leitura existentes', () => {
    service.listar().subscribe();
    http.expectOne('/api/operacoes').flush('[]');
    service.buscarPorId(9).subscribe();
    http.expectOne('/api/operacoes/9').flush(RESPONSE);
    service.listarPorCarteira(2).subscribe();
    http.expectOne('/api/carteiras/2/operacoes').flush('[]');
  });

  it.each([
    ['BRASIL', 'BRL', 'PETR4'],
    ['EUA', 'USD', 'AAPL']
  ] as const)('consulta prévia de COMPRA com query explícita e preço lossless em %s', (mercado, moeda, ticker) => {
    let result: unknown;
    service.obterPreviaCompra(ticker, mercado, '2026-08-20').subscribe(value => result = value);
    const request = http.expectOne(req => req.url === '/api/operacoes/previa-compra');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys().sort()).toEqual(['dataOperacao', 'mercado', 'ticker']);
    expect(request.request.params.get('ticker')).toBe(ticker);
    expect(request.request.params.get('mercado')).toBe(mercado);
    expect(request.request.params.get('dataOperacao')).toBe('2026-08-20');
    request.flush(`{"ticker":"${ticker}","mercado":"${mercado}","moeda":"${moeda}","dataCotacao":"2026-08-20","precoUnitario":9999999999999.123456}`);
    expect(result).toEqual(expect.objectContaining({ moeda, precoUnitario: '9999999999999.123456' }));
  });

  it.each(['42.30', null])('consulta sugestão de VENDA presente/nula sem retry (%s)', suggested => {
    let result: unknown;
    service.obterSugestaoPrecoVenda(2, 'PETR4', 'BRASIL', '2026-08-20').subscribe(value => result = value);
    const request = http.expectOne(req => req.url === '/api/carteiras/2/operacoes/sugestao-preco-venda');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('ticker')).toBe('PETR4');
    expect(request.request.params.get('mercado')).toBe('BRASIL');
    expect(request.request.params.get('dataOperacao')).toBe('2026-08-20');
    request.flush(`{"precoUnitarioSugerido":${suggested === null ? 'null' : suggested}}`);
    expect(result).toEqual({ precoUnitarioSugerido: suggested });
    http.expectNone('/api/carteiras/2/operacoes/sugestao-preco-venda');
  });
});
