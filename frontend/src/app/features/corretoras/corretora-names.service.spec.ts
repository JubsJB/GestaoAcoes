import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CorretoraNamesService } from './corretora-names.service';
import { API_BASE_URL } from '../../core/config/api-base-url.token';

describe('CorretoraNamesService', () => {
  let lookup: CorretoraNamesService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_BASE_URL, useValue: '/api' }, CorretoraNamesService] });
    lookup = TestBed.inject(CorretoraNamesService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });

  it('compartilha uma consulta entre linhas, chamadas concorrentes e novos históricos', () => {
    lookup.loadFor([{ corretoraId: null }]);
    http.expectNone(() => true);
    lookup.loadFor([{ corretoraId: 4 }, { corretoraId: 4 }, { corretoraId: 5 }]);
    lookup.loadFor([{ corretoraId: 6 }]);
    expect(lookup.name(4)).toBe('Corretora #4');
    const request = http.expectOne(req => req.url.endsWith('/corretoras'));
    expect(request.request.method).toBe('GET');
    request.flush([{ id: 4, nomeFantasia: 'XP Investimentos', razaoSocial: 'XP SA' }, { id: 5, nomeFantasia: null, razaoSocial: 'Outra SA' }]);
    expect(lookup.name(4)).toBe('XP Investimentos');
    expect(lookup.name(5)).toBe('Outra SA');
    expect(lookup.name(6)).toBe('Corretora #6');
    expect(lookup.name(null)).toBe('Sem corretora');
    lookup.loadFor([{ corretoraId: 4 }]);
    http.expectNone(() => true);
  });

  it('mantém fallback em falha sem bloquear histórico ou repetir por linha', () => {
    lookup.loadFor([{ corretoraId: 4 }]);
    http.expectOne(req => req.url.endsWith('/corretoras')).flush({}, { status: 503, statusText: 'Unavailable' });
    expect(lookup.name(4)).toBe('Corretora #4');
    lookup.loadFor([{ corretoraId: 5 }]);
    http.expectNone(() => true);
  });
});
