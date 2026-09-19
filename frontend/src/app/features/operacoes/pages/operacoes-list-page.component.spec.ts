import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject, throwError } from 'rxjs';

import { CarteiraContextService } from '../../../core/carteira/carteira-context.service';
import { CarteiraNavigationService } from '../../../core/carteira/carteira-navigation.service';
import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { CorretorasService } from '../../corretoras/corretoras.service';
import { OperacaoResponse } from '../models/operacao';
import { OperacoesService } from '../operacoes.service';
import { OperacoesListPageComponent } from './operacoes-list-page.component';

const A = { id: 1, nome: 'Carteira A', dataCriacao: '' };
const B = { id: 2, nome: 'Carteira B', dataCriacao: '' };
const first: OperacaoResponse = { id: 2, carteiraId: 1, ticker: 'AAPL', mercado: 'EUA', corretoraId: null, tipo: 'VENDA', quantidade: '0.100000', precoUnitario: '200.123456', dataOperacao: '2026-01-02', ordemNoDia: 2, valorTotal: '20.012345600000' };
const second: OperacaoResponse = { ...first, id: 1, ticker: 'PETR4', mercado: 'BRASIL', tipo: 'COMPRA', quantidade: '10', dataOperacao: '2026-01-01', ordemNoDia: 1 };

type Status = 'idle' | 'loading' | 'ready' | 'error';
interface TestState { items: readonly (typeof A)[]; activeId: number | null; status: Status; error: NormalizedHttpError | null; invalidSelection: boolean; }

describe('OperacoesListPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  async function create(initial: TestState, response: (id: number) => Observable<OperacaoResponse[]> = () => of([first, second])) {
    const state$ = new BehaviorSubject<TestState>(initial);
    const current = signal(initial);
    state$.subscribe(value => current.set(value));
    const context = {
      state$,
      carteiras: () => current().items,
      activeId: () => current().activeId,
      active: () => current().items.find(item => item.id === current().activeId) ?? null,
      loading: () => current().status === 'idle' || current().status === 'loading',
      error: () => current().error,
      invalidSelection: () => current().invalidSelection,
      initialize: vi.fn().mockReturnValue(of(true))
    };
    const service = { listarPorCarteira: vi.fn().mockImplementation(response), listar: vi.fn() };
    const navigation = { bindOperations: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [OperacoesListPageComponent],
      providers: [
        provideRouter([]),
        { provide: CarteiraContextService, useValue: context },
        { provide: CarteiraNavigationService, useValue: navigation },
        { provide: OperacoesService, useValue: service },
        { provide: CorretorasService, useValue: { listar: vi.fn().mockReturnValue(of([])) } }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(OperacoesListPageComponent);
    fixture.detectChanges();
    return { fixture, context, service, state$ };
  }

  it.each([
    [{ items: [A], activeId: null, status: 'loading', error: null, invalidSelection: false }, 'Carregando carteiras'],
    [{ items: [], activeId: null, status: 'ready', error: null, invalidSelection: false }, 'Nenhuma carteira cadastrada'],
    [{ items: [A], activeId: null, status: 'ready', error: null, invalidSelection: true }, 'carteira informada não está disponível']
  ] as const)('distingue estados de contexto sem consultar operações', async (state, text) => {
    const { fixture, service } = await create(state as TestState);
    expect(fixture.nativeElement.textContent).toContain(text);
    expect(service.listarPorCarteira).not.toHaveBeenCalled();
    expect(service.listar).not.toHaveBeenCalled();
  });

  it('distingue erro de carteiras e permite retry manual', async () => {
    const failure = { status: 500, message: 'Falha nas carteiras', details: {}, code: null } as unknown as NormalizedHttpError;
    const { fixture, context, service } = await create({ items: [], activeId: null, status: 'error', error: failure, invalidSelection: false });
    expect(fixture.nativeElement.textContent).toContain('Falha nas carteiras');
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    expect(context.initialize).toHaveBeenCalledWith(true);
    expect(service.listarPorCarteira).not.toHaveBeenCalled();
  });

  it('mostra loading, nome da carteira e preserva ordem, precisão e links contextuais', async () => {
    const pending = new Subject<OperacaoResponse[]>();
    const { fixture, service } = await create({ items: [A, B], activeId: 1, status: 'ready', error: null, invalidSelection: false }, () => pending);
    expect(fixture.nativeElement.textContent).toContain('Carregando operações de Carteira A');
    pending.next([first, second]); pending.complete(); fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text.indexOf('AAPL')).toBeLessThan(text.indexOf('PETR4'));
    expect(text).toContain('US$ 200,12');
    expect(text).toContain('Sem corretora');
    const links = [...fixture.nativeElement.querySelectorAll('a')] as HTMLAnchorElement[];
    expect(links.find(link => link.textContent?.includes('Nova operação'))?.href).toContain('carteiraId=1');
    expect(links.find(link => link.textContent?.includes('Ver detalhes'))?.href).toContain('origem=operacoes');
    expect(service.listarPorCarteira).toHaveBeenCalledOnce();
    expect(service.listarPorCarteira).toHaveBeenCalledWith(1);
    expect(service.listar).not.toHaveBeenCalled();
  });

  it('mostra vazio contextual', async () => {
    const { fixture } = await create({ items: [A], activeId: 1, status: 'ready', error: null, invalidSelection: false }, () => of([]));
    expect(fixture.nativeElement.textContent).toContain('Nenhuma operação nesta carteira');
    expect(fixture.nativeElement.textContent).toContain('Carteira A');
  });

  it.each([500, 404])('mostra erro de operações %s e refaz somente a carteira atual', async status => {
    const failure = { status, message: 'Falha no histórico', details: {}, code: null } as unknown as NormalizedHttpError;
    let attempt = 0;
    const { fixture, service } = await create(
      { items: [A], activeId: 1, status: 'ready', error: null, invalidSelection: false },
      () => ++attempt === 1 ? throwError(() => failure) : of([])
    );
    expect(fixture.nativeElement.textContent).toContain(status === 404 ? 'não está mais disponível' : 'Falha no histórico');
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); fixture.detectChanges();
    expect(service.listarPorCarteira).toHaveBeenCalledTimes(2);
    expect(service.listarPorCarteira).toHaveBeenLastCalledWith(1);
  });

  it('invalida A ao trocar para B e ignora dados, erro e finalize tardios de A', async () => {
    const requests = new Map<number, Subject<OperacaoResponse[]>>([[1, new Subject()], [2, new Subject()]]);
    const { fixture, service, state$ } = await create(
      { items: [A, B], activeId: 1, status: 'ready', error: null, invalidSelection: false },
      id => requests.get(id)!
    );
    state$.next({ items: [A, B], activeId: 2, status: 'ready', error: null, invalidSelection: false }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('AAPL');
    expect(fixture.nativeElement.textContent).toContain('Carregando operações de Carteira B');
    requests.get(1)!.next([first]); requests.get(1)!.error({ status: 500, message: 'Erro tardio' }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Erro tardio');
    expect(fixture.nativeElement.textContent).toContain('Carregando operações de Carteira B');
    requests.get(2)!.next([{ ...second, carteiraId: 2 }]); requests.get(2)!.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('PETR4');
    expect(service.listarPorCarteira).toHaveBeenCalledTimes(2);
  });

  it('não consulta novamente ao repetir o mesmo estado e invalida ao perder contexto', async () => {
    const pending = new Subject<OperacaoResponse[]>();
    const state = { items: [A], activeId: 1, status: 'ready', error: null, invalidSelection: false } satisfies TestState;
    const { fixture, service, state$ } = await create(state, () => pending);
    state$.next({ ...state }); fixture.detectChanges();
    expect(service.listarPorCarteira).toHaveBeenCalledOnce();
    state$.next({ ...state, activeId: null, invalidSelection: true }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('carteira informada não está disponível');
    expect(fixture.nativeElement.textContent).not.toContain('Carregando operações');
  });
});
