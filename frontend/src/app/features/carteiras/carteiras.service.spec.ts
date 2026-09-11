import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { provideApiConfig } from '../../core/config/api.config';
import { CarteirasService } from './carteiras.service';
import { CarteiraResponse } from './models/carteira';

const CARTEIRA: CarteiraResponse = { id: 7, nome: 'Longo prazo', dataCriacao: '2026-08-31T12:00:00Z' };

describe('CarteirasService', () => {
  let service: CarteirasService;
  let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig('/test-api')] }); service = TestBed.inject(CarteirasService); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());

  it('lista carteiras', () => { service.listar().subscribe((value) => expect(value).toEqual([CARTEIRA])); const req = http.expectOne('/test-api/carteiras'); expect(req.request.method).toBe('GET'); req.flush([CARTEIRA]); });
  it('busca por id codificado', () => { service.buscarPorId(7).subscribe(); const req = http.expectOne('/test-api/carteiras/7'); expect(req.request.method).toBe('GET'); req.flush(CARTEIRA); });
  it('cadastra com payload exato', () => { service.cadastrar({ nome: 'Longo prazo' }).subscribe(); const req = http.expectOne('/test-api/carteiras'); expect(req.request.method).toBe('POST'); expect(req.request.body).toEqual({ nome: 'Longo prazo' }); req.flush(CARTEIRA); });
  it('atualiza com payload exato', () => { service.atualizar(7, { nome: 'Novo nome' }).subscribe(); const req = http.expectOne('/test-api/carteiras/7'); expect(req.request.method).toBe('PATCH'); expect(req.request.body).toEqual({ nome: 'Novo nome' }); req.flush({ ...CARTEIRA, nome: 'Novo nome' }); });
  it('exclui e aceita resposta sem corpo', () => { service.excluir(7).subscribe((value) => expect(value).toBeNull()); const req = http.expectOne('/test-api/carteiras/7'); expect(req.request.method).toBe('DELETE'); req.flush(null, { status: 204, statusText: 'No Content' }); });
});
