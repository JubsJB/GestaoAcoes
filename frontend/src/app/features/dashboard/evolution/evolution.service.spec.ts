import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { provideApiConfig } from '../../../core/config/api.config';
import { httpErrorInterceptor } from '../../../core/http/http-error.interceptor';
import { EvolutionService } from './evolution.service';

describe('EvolutionService', () => {
  function setup() {
    TestBed.configureTestingModule({ providers: [provideApiConfig(), provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting()] });
    return { service: TestBed.inject(EvolutionService), http: TestBed.inject(HttpTestingController) };
  }

  it('consulta URL exata como texto e preserva decimal longo', () => {
    const { service, http } = setup();
    let value = '';
    service.consultar(7).subscribe(response => value = response.pontos[0].patrimonios[0].patrimonioAtual);
    const request = http.expectOne('/api/carteiras/7/evolucao-patrimonial');
    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('text');
    request.flush('{"carteiraId":7,"pontos":[{"snapshotId":9,"dataHoraSnapshot":"2026-09-04T10:00:00Z","patrimonios":[{"moeda":"BRL","patrimonioAtual":99999999999999999999999999.123456789012}]}]}');
    expect(value).toBe('99999999999999999999999999.123456789012');
    http.verify();
  });

  it('registra snapshot por POST sem body e preserva notação científica', () => {
    const { service, http } = setup();
    let value = '';
    service.registrarSnapshot(3).subscribe(response => value = response.patrimonios[0].patrimonioAtual);
    const request = http.expectOne('/api/carteiras/3/snapshots');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    expect(request.request.responseType).toBe('text');
    request.flush('{"id":4,"carteiraId":3,"dataHoraSnapshot":"2026-09-04T10:00:00Z","patrimonios":[{"moeda":"USD","patrimonioAtual":1.20E+3}]}', { status: 201, statusText: 'Created' });
    expect(value).toBe('1.20E+3');
    http.verify();
  });

  it.each([
    [404, null, 'Carteira não encontrada'],
    [409, 'SNAPSHOT_CARTEIRA_DUPLICADO', 'Snapshot duplicado'],
    [409, 'INTEGRIDADE_DADOS_VIOLADA', 'Integridade'],
    [422, 'CALCULO_POSICAO_FORA_DA_PRECISAO', 'Precisão'],
    [422, 'HISTORICO_OPERACOES_INCONSISTENTE', 'Histórico']
  ])('propaga erro normalizado %i %s sem retry', (status, code, message) => {
    const { service, http } = setup();
    let received: any;
    service.registrarSnapshot(1).subscribe({ error: error => received = error });
    const request = http.expectOne('/api/carteiras/1/snapshots');
    request.flush(JSON.stringify({ timeStamp: 1, status, error: 'Error', message, path: '/carteiras/1/snapshots', code, details: {} }), { status, statusText: 'Error' });
    expect(received.status).toBe(status);
    expect(received.code).toBe(code);
    http.expectNone('/api/carteiras/1/snapshots');
    http.verify();
  });

  it('classifica resposta inválida e falha técnica sem retry', () => {
    const { service, http } = setup();
    let invalid: any;
    service.consultar(1).subscribe({ error: error => invalid = error });
    http.expectOne('/api/carteiras/1/evolucao-patrimonial').flush('{"carteiraId":1,"pontos":null}');
    expect(invalid.kind).toBe('technical');
    http.verify();
  });
  it('propaga 404 do GET sem retry automático', () => {
    const { service, http } = setup();
    let received: any;
    service.consultar(99).subscribe({ error: error => received = error });
    http.expectOne('/api/carteiras/99/evolucao-patrimonial').flush(
      '{"timeStamp":1,"status":404,"error":"Not Found","message":"Carteira não encontrada","path":"/carteiras/99/evolucao-patrimonial","code":null,"details":{}}',
      { status: 404, statusText: 'Not Found' }
    );
    expect(received.status).toBe(404);
    expect(received.message).toBe('Carteira não encontrada');
    http.expectNone('/api/carteiras/99/evolucao-patrimonial');
    http.verify();
  });
});
