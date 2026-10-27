import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject, throwError } from 'rxjs';
import { describe, it, expect, afterEach, vi } from 'vitest';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { DistribuicaoCorretorasComponent } from './distribuicao-corretoras.component';
import { DistribuicaoCorretorasService } from './distribuicao-corretoras.service';
import { parseDistribuicaoCorretoras } from './distribuicao-corretoras.contract';
import { DistribuicaoCarteiraResponse } from './models/distribuicao-corretoras';
import { DISTRIBUICAO_EXAMPLES, DISTRIBUICAO_CUSTODIA_EXAMPLE } from './testing/distribuicao-corretoras.fixtures';

describe('DistribuicaoCorretorasComponent', () => {
  afterEach(() => TestBed.resetTestingModule());
  async function create(source = of(parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]))) {
    const service = { consultar: vi.fn().mockReturnValue(source) };
    await TestBed.configureTestingModule({ imports: [DistribuicaoCorretorasComponent],
      providers: [{ provide: DistribuicaoCorretorasService, useValue: service }] }).compileComponents();
    const fixture = TestBed.createComponent(DistribuicaoCorretorasComponent);
    fixture.componentRef.setInput('carteiraId', 1); fixture.detectChanges();
    return { fixture, service, root: fixture.nativeElement as HTMLElement };
  }
  function open(fixture: ComponentFixture<DistribuicaoCorretorasComponent>) {
    (fixture.nativeElement.querySelector('details') as HTMLDetailsElement).open = true;
    fixture.detectChanges();
  }

  it.each(['TRANSFERENCIA', 'ATRIBUICAO'])('apresenta %s como custodia, com ancora e fracao exatas', async natureza => {
    let wire = DISTRIBUICAO_CUSTODIA_EXAMPLE.replace('TRANSFERENCIA', natureza);
    if (natureza === 'ATRIBUICAO') wire = wire.replaceAll('"corretoraId":2,"nome":"Corretora B"', '"corretoraId":null,"nome":"Sem corretora informada"');
    const value = parseDistribuicaoCorretoras(wire), original = structuredClone(value);
    const { fixture, root } = await create(of(value));open(fixture);
    const diagnostic = root.querySelector('.broker-diagnostic')!;
    expect(diagnostic.textContent).toContain(natureza === 'TRANSFERENCIA' ? 'transferência de custódia 20' : 'atribuição de custódia 20');
    expect(diagnostic.textContent).toContain('02/10/2026');expect(diagnostic.textContent).toContain('âncora financeira 0');
    expect(diagnostic.textContent).toContain('9007199254740993/9007199254740994');
    expect(diagnostic.textContent).toContain('Saldo após o evento: -40');expect(diagnostic.textContent).not.toContain('operação 20');
    expect(root.querySelectorAll('.broker-row')).toHaveLength(2);expect(diagnostic.querySelector('[role=alert]')).toBeNull();
    expect(root.querySelector('summary')?.getAttribute('aria-label')).toContain('Distribuição de PETR4');expect(value).toEqual(original);
  });
  it('renderiza fracao de 512 digitos sem abreviar ou converter para Number', async () => {
    const n = '1' + '0'.repeat(510) + '1', d = '2' + '0'.repeat(510) + '1';
    const wire = DISTRIBUICAO_CUSTODIA_EXAMPLE.replace('9007199254740993', n).replace('9007199254740994', d);
    const { fixture, root } = await create(of(parseDistribuicaoCorretoras(wire)));open(fixture);
    expect(root.querySelector('.broker-diagnostic')?.textContent).toContain(n + '/' + d);
  });
  it('sem primeiro deficit nao fabrica evidencia nem altera avisos saudaveis', async () => {
    const { fixture, root } = await create();open(fixture);
    expect(root.querySelector('.broker-diagnostic')).toBeNull();expect(root.textContent).not.toContain('Primeiro déficit');
    expect(root.querySelectorAll('.broker-row')).toHaveLength(2);
  });

  for (const [index, example] of DISTRIBUICAO_EXAMPLES.entries()) {
    it(`renderiza exemplo ${index + 1} com uma advertencia por grupo e sem recalculo`, async () => {
      const value = parseDistribuicaoCorretoras(example);
      const original = structuredClone(value);
      const { fixture, root } = await create(of(value)); open(fixture);
      expect(root.textContent).toContain('PETR4');
      expect(root.textContent).toContain('Não representa custódia comprovada');
      expect(root.querySelectorAll('.broker-row')).toHaveLength(value.posicoes[0].corretoras.length);
      expect(root.querySelectorAll('.broker-diagnostic')).toHaveLength(index >= 1 && index <= 3 ? 1 : 0);
      if (index === 0) expect(root.textContent).toContain('Sem corretora informada');
      if (index === 1) { expect(root.textContent).toContain('-40'); expect(root.textContent).toContain('Saldo negativo atual'); }
      if (index === 2 || index === 3) expect(root.textContent).toContain('Déficit histórico regularizado');
      if (index === 3) {
        expect(root.querySelector('tbody')?.textContent).not.toContain('Corretora B');
        expect(root.querySelector('.broker-diagnostic')?.textContent).toContain('Corretora B');
        expect(root.querySelector('.broker-diagnostic')?.textContent).toContain('Saldo atual atribuído: 0');
      }
      if (index >= 1 && index <= 3) {
        expect(root.textContent).toContain('02/10/2026');
        expect(root.textContent).toContain('ordem 2, operação 20');
      }
      expect(root.querySelectorAll('.reconciliation')).toHaveLength(index === 4 ? 1 : 0);
      expect(root.querySelector('canvas,svg.chart')).toBeNull();
      expect(root.textContent).not.toContain('%');
      expect(value).toEqual(original);
    });
  }

  it('mantem quantidades extensas exatas e ordem recebida', async () => {
    const value = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]);
    value.posicoes[0].mercado = 'EUA'; value.posicoes[0].moeda = 'USD';
    value.posicoes[0].corretoras[0].quantidadeLiquida = '999999999999999999999.123457';
    value.posicoes[0].corretoras.reverse();
    const { fixture, root } = await create(of(value)); open(fixture);
    expect(root.querySelector('tbody tr')?.textContent).toContain('Sem corretora informada');
    expect(root.textContent).toContain('999.999.999.999.999.999.999,123457');
  });
  it('nao compara totais para criar diagnostico', async () => {
    const value = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]);
    value.posicoes[0].quantidadeAtribuidaTotal = '999';
    const { fixture, root } = await create(of(value)); open(fixture);
    expect(root.querySelector('.reconciliation')).toBeNull();
    expect(root.querySelector('.broker-diagnostic')).toBeNull();
  });
  it('loading e vazio sao independentes e anunciados em uma regiao', async () => {
    const source = new Subject<DistribuicaoCarteiraResponse>();
    const { fixture, root } = await create(source);
    expect(root.querySelector('section')?.getAttribute('aria-busy')).toBe('true');
    expect(root.querySelectorAll('[role=status]')).toHaveLength(1);
    expect(root.textContent).toContain('Carregando distribuição');
    source.next({ carteiraId: 1, posicoes: [] }); fixture.detectChanges();
    expect(root.textContent).toContain('Nenhuma distribuição de posições abertas disponível');
    expect(root.querySelector('section')?.getAttribute('aria-busy')).toBe('false');
    expect(root.querySelector('details')).toBeNull();
  });
  it('agrupamentos vazios sao apresentados explicitamente', async () => {
    const value = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]);
    value.posicoes[0].corretoras = [];
    const { fixture, root } = await create(of(value)); open(fixture);
    expect(root.textContent).toContain('Nenhum agrupamento quantitativo disponível');
  });
  it('erro tem um alerta seguro,sem detalhes internos e retry manual', async () => {
    const error: NormalizedHttpError = { kind: 'standard', status: 500, code: null,
      message: 'Falha na consulta.', details: { senha: 'SEGREDO' }, standardError: null,
      originalError: new HttpErrorResponse({ status: 500 }) };
    const { fixture, service, root } = await create(throwError(() => error));
    expect(root.querySelectorAll('[role=alert]')).toHaveLength(1);
    expect(root.textContent).not.toContain('SEGREDO');
    expect(root.querySelector('[role=status]')?.textContent).toBe('');
    service.consultar.mockReturnValue(of(parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0])));
    root.querySelector('button')!.click(); fixture.detectChanges();
    expect(service.consultar).toHaveBeenCalledTimes(2);
    expect(root.querySelector('details')).not.toBeNull();
  });
  it('diferencia erro de contrato de falha HTTP', async () => {
    const failure = { kind: 'technical', status: 0,
      originalError: new HttpErrorResponse({ status: 0, statusText: 'Resposta de distribuicao invalida.' }) } as NormalizedHttpError;
    const { root } = await create(throwError(() => failure));
    expect(root.textContent).toContain('Não foi possível validar os dados');
  });
  it('troca de carteira cancela pedido e limpa snapshot anterior', async () => {
    const old = new Subject<DistribuicaoCarteiraResponse>();
    const current = new Subject<DistribuicaoCarteiraResponse>();
    const { fixture, service, root } = await create(old);
    service.consultar.mockReturnValue(current);
    fixture.componentRef.setInput('carteiraId', 2); fixture.detectChanges();
    expect(old.observed).toBe(false);
    old.next(parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]));
    expect(root.querySelector('details')).toBeNull();
    current.next({ carteiraId: 2, posicoes: [] }); fixture.detectChanges();
    expect(service.consultar).toHaveBeenLastCalledWith(2);
    expect(root.textContent).not.toContain('PETR4');
  });
  it('refresh apos operacao e destruicao cancelam transporte', async () => {
    const old = new Subject<DistribuicaoCarteiraResponse>();
    const current = new Subject<DistribuicaoCarteiraResponse>();
    const { fixture, service } = await create(old);
    service.consultar.mockReturnValue(current);
    fixture.componentRef.setInput('refreshVersion', 1); fixture.detectChanges();
    expect(old.observed).toBe(false); expect(service.consultar).toHaveBeenCalledTimes(2);
    fixture.destroy(); expect(current.observed).toBe(false);
  });
  it('usa expansivel nativo,rotulos e foco sem alerts concorrentes nos diagnosticos', async () => {
    const { fixture, root } = await create(of(parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[3]))); open(fixture);
    expect(root.querySelector('details > summary')?.textContent).toContain('PETR4');
    expect(root.querySelector('details > summary')?.getAttribute('aria-label')).toContain('Distribuição de PETR4, BRASIL, BRL');
    expect(root.querySelector('caption')?.textContent).toContain('Distribuição de PETR4 por corretora');
    expect(root.querySelector('th[scope=col]')).not.toBeNull();
    expect(root.querySelector('.broker-diagnostic [role=alert]')).toBeNull();
    const id = root.querySelector('section')!.getAttribute('aria-labelledby');
    expect(root.querySelector('#' + id)?.textContent).toBe('Distribuição por corretora');
  });
});
