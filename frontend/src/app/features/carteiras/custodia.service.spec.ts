import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { Observable } from 'rxjs';
import { provideApiConfig } from '../../core/config/api.config';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';
import { CustodiaService } from './custodia.service';
import { CadastroCustodia } from './models/custodia';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { estornoCustodia, originalCustodia } from './testing/custodia.fixtures';

describe('CustodiaService', () => {
  let service: CustodiaService;
  let http: HttpTestingController;
  const path = '/test-api/carteiras/1/movimentacoes-custodia';
  const entrada: CadastroCustodia = { natureza: 'TRANSFERENCIA', ticker: 'PETR4', mercado: 'BRASIL',
    corretoraOrigemId: 3, corretoraDestinoId: 4, quantidade: '40.000000', dataEfetiva: '2026-10-01' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideApiConfig('/test-api'),
      provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting()] });
    service = TestBed.inject(CustodiaService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('registra transferência com corpo mínimo e resposta lossless, sem GET posterior', () => {
    let value: unknown, failure: unknown;
    service.cadastrar(1, entrada).subscribe({ next: v => value = v, error: e => failure = e });
    const req = http.expectOne(path);
    expect(req.request.method).toBe('POST'); expect(req.request.body).toEqual(entrada);
    expect(req.request.responseType).toBe('text');
    req.flush(JSON.stringify(originalCustodia()), { status: 201, statusText: 'Created' });
    expect(failure).toBeUndefined(); expect(value).toEqual({ ...originalCustodia(), quantidade: '40' });
    http.expectNone(path);
  });
  it.each([undefined, null])('atribuição envia origem %s e quantidade EUA exata', origem => {
    const body: CadastroCustodia = { natureza: 'ATRIBUICAO', ticker: 'AAPL', mercado: 'EUA',
      corretoraDestinoId: 4, quantidade: '9999999999999.123456', dataEfetiva: '2026-10-01',
      ...(origem === null ? { corretoraOrigemId: null } : {}) };
    let value: unknown;
    service.cadastrar(1, body).subscribe(v => value = v);
    const req = http.expectOne(path);
    expect(req.request.body).toEqual(body); expect(req.request.body).not.toHaveProperty('carteiraId');
    const json = JSON.stringify(originalCustodia({ natureza: 'ATRIBUICAO', corretoraOrigemId: null,
      ticker: 'AAPL', mercado: 'EUA' })).replace('"quantidade":40', '"quantidade":9999999999999.123456');
    req.flush(json, { status: 201, statusText: 'Created' });
    expect(value).toHaveProperty('quantidade', '9999999999999.123456');
  });
  it.each(['id', 'carteiraId', 'posicaoCronologica', 'precoUnitario', 'dataRegistro', 'situacao'])
    ('rejeita campo proibido %s sem request', key => {
      let error: NormalizedHttpError | undefined;
      service.cadastrar(1, { ...entrada, [key]: null } as CadastroCustodia).subscribe({ error: e => error = e });
      expect(error?.kind).toBe('technical'); http.expectNone(path);
    });
  it.each([
    { natureza: 'ESTORNO' }, { corretoraOrigemId: null }, { corretoraOrigemId: 4 },
    { natureza: 'ATRIBUICAO' }, { quantidade: '0' }, { quantidade: '0.000001' },
    { quantidade: '9007199254740993' }, { quantidade: 40 }, { dataEfetiva: '2026-02-30' },
    { mercado: 'OUTRO' }
  ])('rejeita request estruturalmente inválido %# sem HTTP', extra => {
    let error: unknown;
    service.cadastrar(1, { ...entrada, ...extra } as CadastroCustodia).subscribe({ error: e => error = e });
    expect(error).toBeDefined(); http.expectNone(path);
  });
  it('consulta filtros do backend, conservando estorno com situação null', () => {
    let value: unknown;
    service.historico(1, { natureza: 'ESTORNO', situacao: 'ATIVA' }).subscribe(v => value = v);
    const req = http.expectOne(r => r.url === path);
    expect(req.request.method).toBe('GET'); expect(req.request.params.keys()).toEqual(['natureza', 'situacao']);
    expect(req.request.params.get('natureza')).toBe('ESTORNO'); expect(req.request.params.get('situacao')).toBe('ATIVA');
    req.flush(JSON.stringify([estornoCustodia()])); expect(value).toHaveLength(1);
    expect(value).toEqual([{ ...estornoCustodia(), quantidade: '40' }]);
  });
  it('consulta detalhe original e vínculo sem endpoint individual de estorno', () => {
    let value: unknown;
    service.detalhe(1, 10).subscribe(v => value = v);
    const body = { movimentacao: originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 }), estorno: estornoCustodia() };
    const req = http.expectOne(`${path}/10`); expect(req.request.method).toBe('GET');
    req.flush(JSON.stringify(body)); expect(value).toHaveProperty('estorno.movimentacaoOriginalId', 10);
    http.expectNone(`${path}/20`);
  });
  it.each([undefined, {}, { motivo: null }, { motivo: '  ' }, { motivo: ' Texto preservado ' }])
    ('estorna com corpo opcional %#, sem GET ou retry', body => {
      let value: unknown;
      service.estornar(1, 10, body).subscribe(v => value = v);
      const req = http.expectOne(`${path}/10/estornos`);
      expect(req.request.method).toBe('POST'); expect(req.request.body).toEqual(body ?? null);
      req.flush(JSON.stringify({ movimentacao: originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 }),
        estorno: estornoCustodia({ motivo: typeof body?.motivo === 'string' && body.motivo.trim() ? body.motivo : null }) }),
        { status: 201, statusText: 'Created' });
      expect(value).toHaveProperty('estorno.id', 20); http.expectNone(`${path}/10`);
    });
  it.each([{ motivo: 1 }, { quantidade: 1 }, { motivo: 'x'.repeat(1001) }])('rejeita motivo/campos inválidos %#', body => {
    let error: unknown;
    service.estornar(1, 10, body as never).subscribe({ error: e => error = e });
    expect(error).toBeDefined(); http.expectNone(`${path}/10/estornos`);
  });
  it.each([400, 404, 409])('preserva HTTP %s, detalhes nulos e decimais sem perda', status => {
    let error: NormalizedHttpError | undefined;
    service.cadastrar(1, entrada).subscribe({ error: e => error = e });
    http.expectOne(path).flush(`{"timeStamp":1,"status":${status},"error":"Conflict","message":"Conflito",
      "path":"/custodia","code":${status === 404 ? 'null' : '"POSICAO_CUSTODIA_INDISPONIVEL"'},
      "details":{"evento":null,"origem":{"corretoraId":null,"categoria":"SEM_CORRETORA_INFORMADA"},
      "quantidadeSolicitada":9999999999999.123456,"quantidadeDisponivel":9007199254740993,
      "saldoAntes":-0.000001,"saldoDepois":-9999999999999.123456,"deficitAntes":null,"deficitDepois":1E-6}}`,
      { status, statusText: 'Conflict' });
    expect(error?.kind).toBe('standard'); expect(error?.status).toBe(status);
    expect(error?.details).toMatchObject({ evento: null, quantidadeDisponivel: '9007199254740993',
      quantidadeSolicitada: '9999999999999.123456', saldoAntes: '-0.000001',
      saldoDepois: '-9999999999999.123456', deficitAntes: null, deficitDepois: '1E-6' });
    http.expectNone(path);
  });
  it('preserva evento CUSTODIA com identidade segura e fração sem converter', () => {
    let error: NormalizedHttpError | undefined;
    service.estornar(1, 10).subscribe({ error: e => error = e });
    http.expectOne(`${path}/10/estornos`).flush(JSON.stringify({ timeStamp: 1, status: 409,
      error: 'Conflict', message: 'Histórico', path, code: 'MOVIMENTACAO_CUSTODIA_INVALIDADA',
      details: { evento: { dominio: 'CUSTODIA', id: 10, natureza: 'TRANSFERENCIA', dataEfetiva: '2026-10-01',
        posicaoCronologica: { ordemFinanceiraAncora: 0, numerador: '9007199254740993', denominador: '9007199254740994' } } } }),
      { status: 409, statusText: 'Conflict' });
    expect(error?.details).toHaveProperty('evento.posicaoCronologica.numerador', '9007199254740993');
    expect(error?.details).toHaveProperty('evento.id', 10);
  });
  it('falha segura em resposta fora da carteira, sem GET ou repetição da escrita', () => {
    let error: NormalizedHttpError | undefined, success = false;
    service.cadastrar(1, entrada).subscribe({ next: () => success = true, error: e => error = e });
    http.expectOne(path).flush(JSON.stringify(originalCustodia({ carteiraId: 2 })), { status: 201, statusText: 'Created' });
    expect(success).toBe(false); expect(error?.kind).toBe('technical'); http.expectNone(path);
  });
  it('mantém falha técnica sem fabricar código de domínio', () => {
    let error: NormalizedHttpError | undefined;
    service.historico(1).subscribe({ error: e => error = e });
    http.expectOne(path).flush('erro não JSON', { status: 500, statusText: 'Server Error' });
    expect(error?.kind).toBe('technical'); expect(error?.status).toBe(500); expect(error?.code).toBeNull();
  });
  it('rejeita IDs/filtros inválidos antes do transporte', () => {
    const requests: Observable<unknown>[] = [service.detalhe(1, 0), service.detalhe(0, 10), service.historico(9007199254740992),
      service.historico(1, { natureza: 'OUTRO' } as never), service.historico(1, { situacao: 'ativa' } as never)];
    for (const request of requests) {
      let error: unknown; request.subscribe({ error: e => error = e }); expect(error).toBeDefined();
    }
    http.expectNone(r => r.url.startsWith(path));
  });
});
