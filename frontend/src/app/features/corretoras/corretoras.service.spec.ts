import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { provideApiConfig } from '../../core/config/api.config';
import { Corretora } from './models/corretora';
import { CorretorasService } from './corretoras.service';

const BROKER: Corretora = {id:1,cnpj:'11222333000181',razaoSocial:'Corretora Teste',nomeFantasia:null,email:null,telefone:null,cep:'01001000',logradouro:'Praça da Sé',numero:null,complemento:null,bairro:'Sé',cidade:'São Paulo',uf:'SP',situacaoCadastral:'ATIVA',validadaMercadoFinanceiro:false,dataCadastro:'2026-08-29T12:00:00Z'};

describe('CorretorasService', () => {
  let service: CorretorasService; let http: HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideApiConfig('/test-api')]});service=TestBed.inject(CorretorasService);http=TestBed.inject(HttpTestingController);});
  afterEach(()=>http.verify());
  it('lista corretoras',()=>{service.listar().subscribe(value=>expect(value).toEqual([BROKER]));const req=http.expectOne('/test-api/corretoras');expect(req.request.method).toBe('GET');req.flush([BROKER]);});
  it('busca por id',()=>{service.buscarPorId(1).subscribe(value=>expect(value).toEqual(BROKER));const req=http.expectOne('/test-api/corretoras/1');expect(req.request.method).toBe('GET');req.flush(BROKER);});
  it('busca por CNPJ no query parameter correto',()=>{service.buscarPorCnpj('11222333000181').subscribe();const req=http.expectOne(r=>r.url==='/test-api/corretoras/por-cnpj'&&r.params.get('cnpj')==='11222333000181');expect(req.request.method).toBe('GET');req.flush(BROKER);});
  it('envia somente CNPJ no cadastro inicial',()=>{service.cadastrar({cnpj:'11222333000181'}).subscribe();const req=http.expectOne('/test-api/corretoras');expect(req.request.method).toBe('POST');expect(req.request.body).toEqual({cnpj:'11222333000181'});req.flush(BROKER);});
  it('envia confirmação somente no cadastro confirmado',()=>{service.cadastrar({cnpj:'11222333000181',confirmarSituacaoCadastralNaoAtiva:true}).subscribe();const req=http.expectOne('/test-api/corretoras');expect(req.request.body).toEqual({cnpj:'11222333000181',confirmarSituacaoCadastralNaoAtiva:true});req.flush(BROKER);});
});
