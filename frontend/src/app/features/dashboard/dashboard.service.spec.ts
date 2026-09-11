import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach, afterEach } from 'vitest';

import { provideApiConfig } from '../../core/config/api.config';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideApiConfig(), provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting()] });
    service = TestBed.inject(DashboardService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('consulta resumo e preserva BRL, USD e decimais longos', () => {
    let response: any;
    service.obterResumo(3).subscribe(value => response = value);
    const request = http.expectOne('/api/carteiras/3/resumo');
    expect(request.request.method).toBe('GET');
    request.flush('{"carteiraId":3,"resumos":[{"moeda":"BRL","custoTotalPosicoes":9999999999999.123456,"patrimonioAtual":123456789012345678.123456,"resultadoNaoRealizadoTotal":-50.123456,"rentabilidadePercentual":48.123400},{"moeda":"USD","custoTotalPosicoes":40.000000,"patrimonioAtual":48.200000,"resultadoNaoRealizadoTotal":0.000001,"rentabilidadePercentual":0}]}');
    expect(response.resumos[0].patrimonioAtual).toBe('123456789012345678.123456');
    expect(response.resumos[1].custoTotalPosicoes).toBe('40.000000');
  });

  it('consulta posições e resultados realizados sem endpoints extras', () => {
    let position: any;
    let result: any;
    service.listarPosicoes(7).subscribe(value => position = value[0]);
    service.listarResultadosRealizados(7).subscribe(value => result = value[0]);
    http.expectOne('/api/carteiras/7/posicoes').flush('[{"acaoId":1,"ticker":"PETR4","nomeEmpresa":"Petrobras","mercado":"BRASIL","moeda":"BRL","quantidadeAtual":5.000000,"precoMedio":40.000000,"custoPosicao":200.000000,"cotacaoAtual":48.200000,"dataHoraCotacao":"2026-09-03T10:00:00-03:00","valorAtualPosicao":241.000000,"resultadoNaoRealizado":41.000000,"rentabilidadePercentual":20.500000}]');
    http.expectOne('/api/carteiras/7/resultados-realizados').flush('[{"acaoId":2,"ticker":"AAPL","nomeEmpresa":"Apple","mercado":"EUA","moeda":"USD","resultadoRealizado":-50.123456}]');
    expect(position.cotacaoAtual).toBe('48.200000');
    expect(result.resultadoRealizado).toBe('-50.123456');
    http.expectNone('/api/carteiras/7/patrimonio');
    http.expectNone('/api/carteiras/7/evolucao-patrimonial');
  });

  it('falha seguramente diante de resposta estruturalmente inválida', () => {
    let failure: any;
    service.obterResumo(1).subscribe({ error: error => failure = error });
    http.expectOne('/api/carteiras/1/resumo').flush('{"carteiraId":1,"resumos":[{"moeda":"BRL"}]}');
    expect(failure.kind).toBe('technical');
    expect(failure.message).toContain('Resposta financeira inválida');
  });

  it('preserva StandardError textual normalizado sem retry', () => {
    let failure: any;
    service.listarPosicoes(9).subscribe({ error: error => failure = error });
    const request = http.expectOne('/api/carteiras/9/posicoes');
    request.flush('{"timeStamp":1,"status":422,"error":"Unprocessable Entity","message":"Cálculo fora da precisão","path":"/carteiras/9/posicoes","code":"CALCULO_POSICAO_FORA_DA_PRECISAO","details":{}}', { status: 422, statusText: 'Unprocessable Entity' });
    expect(failure.code).toBe('CALCULO_POSICAO_FORA_DA_PRECISAO');
    http.expectNone('/api/carteiras/9/posicoes');
  });
});
