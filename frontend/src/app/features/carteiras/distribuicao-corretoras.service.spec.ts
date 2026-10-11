import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, afterEach, describe, expect, it } from 'vitest';
import { Subject, switchMap } from 'rxjs';
import { provideApiConfig } from '../../core/config/api.config';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';
import { DistribuicaoCorretorasService } from './distribuicao-corretoras.service';
import { DISTRIBUICAO_EXAMPLES } from './testing/distribuicao-corretoras.fixtures';

describe('DistribuicaoCorretorasService', () => {
  let service: DistribuicaoCorretorasService;
  let http: HttpTestingController;
  const path = '/test-api/carteiras/1/posicoes/distribuicao-corretoras';
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideApiConfig('/test-api'),
      provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting()] });
    service = TestBed.inject(DistribuicaoCorretorasService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  for (const [index, example] of DISTRIBUICAO_EXAMPLES.entries()) {
    it(`consome exemplo ${index + 1} como sucesso HTTP, sem endpoints adicionais`, () => {
      let result: any;
      service.consultar(1).subscribe(value => result = value);
      const req = http.expectOne(path);
      expect(req.request.method).toBe('GET'); expect(req.request.responseType).toBe('text');
      expect(req.request.body).toBeNull(); req.flush(example);
      expect(result.posicoes[0].reconciliada).toBe(index !== 4);
      expect(typeof result.posicoes[0].quantidadeConsolidada).toBe('string');
      http.expectNone('/test-api/carteiras/1/posicoes');
    });
  }
  it('aceita vazio', () => {
    service.consultar(1).subscribe(value => expect(value.posicoes).toEqual([]));
    http.expectOne(path).flush('{"carteiraId":1,"posicoes":[]}');
  });
  it('preserva token decimal de grande magnitude no transporte textual', () => {
    service.consultar(1).subscribe(v => expect(v.posicoes[0].quantidadeConsolidada)
      .toBe('999999999999999999999.123457'));
    http.expectOne(path).flush(DISTRIBUICAO_EXAMPLES[0].replace(':100', ':999999999999999999999.123457'));
  });
  it('falha de rede ou body nao JSON mantem status e classificacao tecnica', () => {
    let error: any;
    service.consultar(1).subscribe({ error: e => error = e });
    http.expectOne(path).flush('indisponivel', { status: 503, statusText: 'Service Unavailable' });
    expect(error.kind).toBe('technical'); expect(error.status).toBe(503);
    service.consultar(1).subscribe({ error: e => error = e });
    http.expectOne(path).error(new ProgressEvent('error'));
    expect(error.kind).toBe('technical'); expect(error.status).toBe(0);
  });
  it('erro HTTP textual preserva StandardError e permite nova tentativa manual sem retry automatico', () => {
    let failure: any;
    service.consultar(1).subscribe({ error: e => failure = e });
    http.expectOne(path).flush('{"timeStamp":1,"status":500,"error":"Internal Server Error","message":"Nao foi possivel consultar a distribuicao por corretora.","path":"/carteiras/1/posicoes/distribuicao-corretoras","code":"FALHA_CONSULTA_DISTRIBUICAO_CORRETORAS","details":{}}', { status: 500, statusText: 'Internal Server Error' });
    expect(failure.kind).toBe('standard'); expect(failure.status).toBe(500);
    expect(failure.code).toBe('FALHA_CONSULTA_DISTRIBUICAO_CORRETORAS'); http.expectNone(path);
    service.consultar(1).subscribe(value => expect(value.posicoes).toEqual([]));
    http.expectOne(path).flush('{"carteiraId":1,"posicoes":[]}');
  });
  it('resposta invalida ou carteira diferente gera erro tecnico, nunca sucesso vazio', () => {
    for (const body of ['{}', '{"carteiraId":2,"posicoes":[]}', '{']) {
      let failure: any;
      service.consultar(1).subscribe({ error: e => failure = e }); http.expectOne(path).flush(body);
      expect(failure.kind).toBe('technical'); expect(failure.standardError).toBeNull();
    }
  });
  it('unsubscribe cancela transporte', () => {
    const sub = service.consultar(1).subscribe(); const req = http.expectOne(path);
    sub.unsubscribe(); expect(req.cancelled).toBe(true);
  });
  it('switchMap substitui contexto e nao entrega resposta anterior', () => {
    const context = new Subject<number>(); const received: number[] = [];
    const sub = context.pipe(switchMap(id => service.consultar(id))).subscribe(v => received.push(v.carteiraId));
    context.next(1); const old = http.expectOne(path);
    context.next(2); expect(old.cancelled).toBe(true);
    http.expectOne('/test-api/carteiras/2/posicoes/distribuicao-corretoras').flush('{"carteiraId":2,"posicoes":[]}');
    expect(received).toEqual([2]); sub.unsubscribe();
  });
});
