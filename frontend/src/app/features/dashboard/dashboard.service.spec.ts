import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach, afterEach } from 'vitest';

import { provideApiConfig } from '../../core/config/api.config';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';
import { DashboardService } from './dashboard.service';
import { AtualizacaoCotacoesCarteiraResponse } from './models/atualizacao-cotacoes';

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
    http.expectOne('/api/carteiras/7/posicoes').flush('[{"acaoId":1,"ticker":"PETR4","nomeEmpresa":"Petrobras","mercado":"BRASIL","moeda":"BRL","quantidadeAtual":5.000000,"precoMedio":40.000000,"custoPosicao":200.000000,"cotacaoAtual":48.200000,"dataHoraCotacao":"2026-09-03T10:00:00-03:00","referenciaMercadoCotacao":{"tipo":"DESCONHECIDA","data":null,"instante":null},"cotacaoObtidaEm":null,"valorAtualPosicao":241.000000,"resultadoNaoRealizado":41.000000,"rentabilidadePercentual":20.500000}]');
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

  function lote(): any {
    return { carteiraId: 3, resumo: { atualizadas: 1, semNovaCotacao: 0, falhas: 0 }, resultados: [{
      acaoId: 8, ticker: 'AAPL', mercado: 'EUA', resultado: 'ATUALIZADA', motivo: 'REFERENCIA_POSTERIOR',
      cotacao: { cotacaoAtual: '1234567890123.123456',
        referenciaMercadoCotacao: { tipo: 'DATA', data: '2026-10-02', instante: null },
        cotacaoObtidaEm: '2026-10-04T15:30:00.123456000Z', dataHoraCotacao: '2026-10-04T15:30:00.123456Z' },
      erro: null
    }] };
  }

  function receber(payload: unknown): AtualizacaoCotacoesCarteiraResponse {
    let result: AtualizacaoCotacoesCarteiraResponse | undefined;
    service.atualizarCotacoes(3).subscribe(value => result = value);
    http.expectOne('/api/carteiras/3/atualizacoes-cotacoes').flush(JSON.stringify(payload));
    expect(result).toBeDefined();
    return result!;
  }

  it('envia exatamente um POST sem conteúdo e preserva token BigDecimal numérico lossless', () => {
    let result: AtualizacaoCotacoesCarteiraResponse | undefined;
    service.atualizarCotacoes(3).subscribe(value => result = value);
    const request = http.expectOne('/api/carteiras/3/atualizacoes-cotacoes');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    expect(request.request.serializeBody()).toBeNull();
    expect(request.request.detectContentTypeHeader()).toBeNull();
    expect(request.request.responseType).toBe('text');
    request.flush(JSON.stringify(lote()).replace('"1234567890123.123456"', '1234567890123.123456'));
    expect(result?.resultados[0].cotacao.cotacaoAtual).toBe('1234567890123.123456');
    expect(result?.resultados[0].cotacao.cotacaoObtidaEm).toBe('2026-10-04T15:30:00.123456000Z');
    http.expectNone(() => true);
  });

  it.each([
    { tipo: 'DATA', data: '2026-10-02', instante: null },
    { tipo: 'INSTANTE', data: null, instante: '2026-10-02T12:15:00.999999000-03:00' },
    { tipo: 'DESCONHECIDA', data: null, instante: null }
  ])('preserva referência $tipo e obtenção legada nula sem fallback', referencia => {
    const payload = lote(); payload.resultados[0].cotacao.referenciaMercadoCotacao = referencia;
    payload.resultados[0].cotacao.cotacaoObtidaEm = null;
    const result = receber(payload);
    expect(result.resultados[0].cotacao.referenciaMercadoCotacao).toEqual(referencia);
    expect(result.resultados[0].cotacao.cotacaoObtidaEm).toBeNull();
    expect(result.resultados[0].cotacao.dataHoraCotacao).toBe(payload.resultados[0].cotacao.dataHoraCotacao);
  });

  it('preserva sucesso parcial, ordem recebida, motivo aberto e erro público individual no 200', () => {
    const payload = lote();
    const primeira = payload.resultados[0];
    payload.resultados.push({ ...primeira, acaoId: 9, resultado: 'SEM_NOVA_COTACAO', motivo: 'COOLDOWN_SUCESSO' });
    payload.resultados.push({ ...primeira, acaoId: 10, resultado: 'FALHA', motivo: 'CODIGO_PUBLICO_FUTURO',
      erro: { code: 'CODIGO_PUBLICO_FUTURO', message: 'Mensagem pública' } });
    payload.resumo = { atualizadas: 1, semNovaCotacao: 1, falhas: 1 };
    const result = receber(payload);
    expect(result.resultados.map(item => item.acaoId)).toEqual([8, 9, 10]);
    expect(result.resumo).toEqual(payload.resumo);
    expect(result.resultados[0].erro).toBeNull(); expect(result.resultados[1].erro).toBeNull();
    expect(result.resultados[2].erro).toEqual({ code: 'CODIGO_PUBLICO_FUTURO', message: 'Mensagem pública' });
  });

  it.each(['ATUALIZADA', 'SEM_NOVA_COTACAO', 'FALHA'])('aceita lote integralmente %s como resposta normal', resultado => {
    const payload = lote(); payload.resultados[0].resultado = resultado;
    payload.resultados[0].erro = resultado === 'FALHA' ? { code: 'SERVICO_EXTERNO_TIMEOUT', message: 'Timeout' } : null;
    payload.resumo = { atualizadas: resultado === 'ATUALIZADA' ? 1 : 0,
      semNovaCotacao: resultado === 'SEM_NOVA_COTACAO' ? 1 : 0, falhas: resultado === 'FALHA' ? 1 : 0 };
    expect(receber(payload).resultados[0].resultado).toBe(resultado);
  });

  it('aceita lote vazio com três contadores zero', () => {
    const payload = { carteiraId: 3, resumo: { atualizadas: 0, semNovaCotacao: 0, falhas: 0 }, resultados: [] };
    expect(receber(payload)).toEqual(payload);
  });

  const invalidos: [string, (payload: any) => void][] = [
    ['carteira ausente', p => { delete p.carteiraId; }],
    ['resumo ausente', p => { delete p.resumo; }],
    ['resultados ausentes', p => { delete p.resultados; }],
    ['contador negativo', p => { p.resumo.atualizadas = -1; }],
    ['contador fracionário', p => { p.resumo.atualizadas = 0.5; }],
    ['contador divergente', p => { p.resumo.semNovaCotacao = 1; }],
    ['contador ausente', p => { delete p.resumo.falhas; }],
    ['classificação inválida', p => { p.resultados[0].resultado = 'SUCESSO'; }],
    ['motivo ausente', p => { delete p.resultados[0].motivo; }],
    ['cotação ausente', p => { delete p.resultados[0].cotacao; }],
    ['preço inválido', p => { p.resultados[0].cotacao.cotacaoAtual = 'não decimal'; }],
    ['tipo inválido', p => { p.resultados[0].cotacao.referenciaMercadoCotacao.tipo = 'HORA'; }],
    ['DATA com instante', p => { p.resultados[0].cotacao.referenciaMercadoCotacao.instante = '2026-10-02T15:00:00Z'; }],
    ['DATA sem data', p => { p.resultados[0].cotacao.referenciaMercadoCotacao.data = null; }],
    ['DATA com timestamp', p => { p.resultados[0].cotacao.referenciaMercadoCotacao.data = '2026-10-02T15:00:00Z'; }],
    ['INSTANTE sem instante', p => { p.resultados[0].cotacao.referenciaMercadoCotacao = { tipo: 'INSTANTE', data: null, instante: null }; }],
    ['INSTANTE com data', p => { p.resultados[0].cotacao.referenciaMercadoCotacao = { tipo: 'INSTANTE', data: '2026-10-02', instante: '2026-10-02T15:00:00Z' }; }],
    ['DESCONHECIDA com referência', p => { p.resultados[0].cotacao.referenciaMercadoCotacao.tipo = 'DESCONHECIDA'; }],
    ['referência ausente', p => { delete p.resultados[0].cotacao.referenciaMercadoCotacao; }],
    ['obtenção ausente', p => { delete p.resultados[0].cotacao.cotacaoObtidaEm; }],
    ['obtenção inválida', p => { p.resultados[0].cotacao.cotacaoObtidaEm = '2026-10-04'; }],
    ['legado ausente', p => { delete p.resultados[0].cotacao.dataHoraCotacao; }],
    ['FALHA sem erro', p => { p.resultados[0].resultado = 'FALHA'; p.resumo = { atualizadas: 0, semNovaCotacao: 0, falhas: 1 }; delete p.resultados[0].erro; }],
    ['FALHA com erro nulo', p => { p.resultados[0].resultado = 'FALHA'; p.resumo = { atualizadas: 0, semNovaCotacao: 0, falhas: 1 }; }],
    ['FALHA com erro incompleto', p => { p.resultados[0].resultado = 'FALHA'; p.resumo = { atualizadas: 0, semNovaCotacao: 0, falhas: 1 }; p.resultados[0].erro = { code: 'FALHA' }; }],
    ['ATUALIZADA com erro', p => { p.resultados[0].erro = { code: 'FALHA', message: 'Falha' }; }],
    ['SEM_NOVA_COTACAO com erro', p => { p.resultados[0].resultado = 'SEM_NOVA_COTACAO'; p.resumo = { atualizadas: 0, semNovaCotacao: 1, falhas: 0 }; p.resultados[0].erro = { code: 'FALHA', message: 'Falha' }; }],
    ['erro ausente fora de FALHA', p => { delete p.resultados[0].erro; }],
    ['Ação duplicada', p => { p.resultados.push(p.resultados[0]); p.resumo.atualizadas = 2; }]
  ];
  it.each(invalidos)('rejeita payload inconsistente: %s', (_, alterar) => {
    const payload = lote(); alterar(payload); let failure: any;
    service.atualizarCotacoes(3).subscribe({ next: () => { throw new Error('Payload inválido aceito'); }, error: error => failure = error });
    http.expectOne('/api/carteiras/3/atualizacoes-cotacoes').flush(JSON.stringify(payload));
    expect(failure.kind).toBe('technical'); http.expectNone(() => true);
  });

  it.each([400, 404, 409, 422, 500])('preserva StandardError global HTTP %s sem retry', status => {
    let failure: any;
    const code = status === 500 ? 'FALHA_ATUALIZACAO_COTACOES' : `ERRO_${status}`;
    const message = 'Não foi possível concluir o lote. Atualizações já confirmadas podem ter sido preservadas.';
    service.atualizarCotacoes(3).subscribe({ error: error => failure = error });
    http.expectOne('/api/carteiras/3/atualizacoes-cotacoes').flush(JSON.stringify({
      timeStamp: 1, status, error: 'Erro', message, path: '/carteiras/3/atualizacoes-cotacoes', code, details: {}
    }), { status, statusText: 'Erro' });
    expect(failure.status).toBe(status); expect(failure.code).toBe(code); expect(failure.message).toBe(message);
    expect(failure.details).toEqual({}); http.expectNone(() => true);
  });
});
