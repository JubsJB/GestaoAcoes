import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { provideApiConfig } from '../../core/config/api.config';
import { AcoesService } from './acoes.service';
import { AcaoResponse } from './models/acao';

const acao: AcaoResponse={id:7,ticker:'PETR4',nomeEmpresa:'Petrobras',mercado:'BRASIL',moeda:'BRL',cotacaoAtual:32.12,dataHoraCotacao:'2026-08-29T12:00:00Z'};

describe('AcoesService',()=>{
  let service:AcoesService;let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideApiConfig()]});service=TestBed.inject(AcoesService);http=TestBed.inject(HttpTestingController);});
  afterEach(()=>http.verify());
  it('POSTs only ticker and mercado',()=>{service.criar({ticker:'PETR4',mercado:'BRASIL'}).subscribe(value=>expect(value).toEqual(acao));const request=http.expectOne('/api/acoes');expect(request.request.method).toBe('POST');expect(request.request.body).toEqual({ticker:'PETR4',mercado:'BRASIL'});request.flush(acao);});
  it('lists actions',()=>{service.listar().subscribe(value=>expect(value).toEqual([acao]));const request=http.expectOne('/api/acoes');expect(request.request.method).toBe('GET');request.flush([acao]);});
  it('gets by encoded id',()=>{service.buscarPorId(7).subscribe(value=>expect(value).toEqual(acao));const request=http.expectOne('/api/acoes/7');expect(request.request.method).toBe('GET');request.flush(acao);});
  it('gets by ticker and market query params',()=>{service.buscarPorTickerEMercado('BRK.B','EUA').subscribe(value=>expect(value).toEqual(acao));const request=http.expectOne(r=>r.url==='/api/acoes/por-ticker');expect(request.request.method).toBe('GET');expect(request.request.params.get('ticker')).toBe('BRK.B');expect(request.request.params.get('mercado')).toBe('EUA');request.flush(acao);});
  it('PATCHes the quote endpoint with no functional body',()=>{service.atualizarCotacao(7).subscribe(value=>expect(value).toEqual(acao));const request=http.expectOne('/api/acoes/7/cotacao');expect(request.request.method).toBe('PATCH');expect(request.request.body).toBeNull();request.flush(acao);});
});
