import { describe, expect, it } from 'vitest';
import { isDistribuicaoCarteiraResponse, parseDistribuicaoCorretoras } from './distribuicao-corretoras.contract';
import { DISTRIBUICAO_EXAMPLES, DISTRIBUICAO_CUSTODIA_EXAMPLE } from './testing/distribuicao-corretoras.fixtures';

describe('Contrato de distribuicao por corretora', () => {
  it('aceita evidencia financeira generica do backend sem aliases legados', () => {
    const value = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1]);
    expect(value.posicoes[0].diagnosticosCorretoras[1].primeiroDeficit).toEqual({
      dominio: 'FINANCEIRO', id: 20, natureza: 'VENDA', dataEfetiva: '2026-10-02',
      posicaoCronologica: { ordemNoDia: 2 }, saldoAposEvento: '-40'
    });
  });
  it('preserva evidencia de custodia, ancora zero e fracao acima de 2^53', () => {
    const d = parseDistribuicaoCorretoras(DISTRIBUICAO_CUSTODIA_EXAMPLE).posicoes[0].diagnosticosCorretoras[1].primeiroDeficit;
    expect(d).toEqual({ dominio: 'CUSTODIA', id: 20, natureza: 'TRANSFERENCIA', dataEfetiva: '2026-10-02',
      posicaoCronologica: { ordemFinanceiraAncora: 0, numerador: '9007199254740993', denominador: '9007199254740994' }, saldoAposEvento: '-40' });
  });
  it('aceita atribuicao do grupo nulo e nao troca dominio por operacao financeira', () => {
    const wire = DISTRIBUICAO_CUSTODIA_EXAMPLE.replace('TRANSFERENCIA', 'ATRIBUICAO')
      .replaceAll('"corretoraId":2,"nome":"Corretora B"', '"corretoraId":null,"nome":"Sem corretora informada"');
    expect(parseDistribuicaoCorretoras(wire).posicoes[0].diagnosticosCorretoras[1].primeiroDeficit?.natureza).toBe('ATRIBUICAO');
  });
  it.each(['999999999999999999999.123456', '1.2345600e+25', '0.000000000000000000000001'])
    ('preserva saldoAposEvento negativo exato %s', token => {
      const wire = DISTRIBUICAO_CUSTODIA_EXAMPLE.replace('"saldoAposEvento":-40', '"saldoAposEvento":-' + token);
      expect(parseDistribuicaoCorretoras(wire).posicoes[0].diagnosticosCorretoras[1].primeiroDeficit?.saldoAposEvento).toBe('-' + token);
    });
  it('aceita componentes de 512 digitos sem arredondar', () => {
    const numerator = '1' + '0'.repeat(510) + '1', denominator = '2' + '0'.repeat(510) + '1';
    const wire = DISTRIBUICAO_CUSTODIA_EXAMPLE.replace('9007199254740993', numerator).replace('9007199254740994', denominator);
    const d = parseDistribuicaoCorretoras(wire).posicoes[0].diagnosticosCorretoras[1].primeiroDeficit;
    expect(d?.dominio).toBe('CUSTODIA');
    if (d?.dominio !== 'CUSTODIA') throw new Error('Domínio incorreto');
    expect(d.posicaoCronologica.numerador).toBe(numerator);expect(d.posicaoCronologica.denominador).toBe(denominator);
  });
  it.each([
    ['9007199254740993', '9007199254740993'], ['9007199254740995', '9007199254740994'],
    ['2', '4'], ['0', '2'], ['01', '2'], ['-1', '2'], ['1e2', '300'], ['1', '2'.repeat(513)]
  ])('rejeita fracao nao canonica, impropria ou fora do limite %s/%s', (n, d) => {
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_CUSTODIA_EXAMPLE.replace('9007199254740993', n).replace('9007199254740994', d))).toThrow();
  });
  it.each(['0', '-1', '20.1', '9007199254740993', '"20"', 'null'])('rejeita id generico invalido %s', token => {
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_CUSTODIA_EXAMPLE.replace('"id":20', '"id":' + token))).toThrow();
  });
  it.each(['-1', '0.1', '2147483648', '9007199254740993', '"0"', 'null'])('rejeita ancora invalida %s', token => {
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_CUSTODIA_EXAMPLE.replace('"ordemFinanceiraAncora":0', '"ordemFinanceiraAncora":' + token))).toThrow();
  });
  it('aceita limite int32 das posicoes e distingue ordem positiva de ancora zero', () => {
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_CUSTODIA_EXAMPLE.replace('"ordemFinanceiraAncora":0', '"ordemFinanceiraAncora":2147483647'))).not.toThrow();
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1].replace('"ordemNoDia":2', '"ordemNoDia":2147483647'))).not.toThrow();
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1].replace('"ordemNoDia":2', '"ordemNoDia":0'))).toThrow();
  });
  it.each(['"-40"', '0', '-0.0000', '40', 'null', 'true', '{}'])('rejeita saldo sem evidencia negativa numerica no wire %s', token => {
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1].replace('"saldoAposEvento":-40', '"saldoAposEvento":' + token))).toThrow();
  });
  it.each([
    ['CUSTODIA', 'COMPRA'], ['FINANCEIRO', 'ATRIBUICAO'], ['ESTORNO', 'ESTORNO'], ['CUSTODIA', 'ESTORNO']
  ])('rejeita dominio e natureza incoerentes %s/%s', (dominio, natureza) => {
    const wire = DISTRIBUICAO_CUSTODIA_EXAMPLE.replace('"CUSTODIA"', JSON.stringify(dominio)).replace('"TRANSFERENCIA"', JSON.stringify(natureza));
    expect(() => parseDistribuicaoCorretoras(wire)).toThrow();
  });
  it('rejeita posicao financeira em custodia, custodia em financeiro e componentes numericos', () => {
    const financeira = '{"ordemNoDia":2}';
    const custodia = '{"ordemFinanceiraAncora":0,"numerador":"9007199254740993","denominador":"9007199254740994"}';
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_CUSTODIA_EXAMPLE.replace(custodia, financeira))).toThrow();
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1].replace(financeira, custodia))).toThrow();
    for (const campo of ['numerador', 'denominador']) {
      const wire = DISTRIBUICAO_CUSTODIA_EXAMPLE.replace(new RegExp('"' + campo + '":"(\\d+)"'), '"' + campo + '":$1');
      expect(() => parseDistribuicaoCorretoras(wire)).toThrow();
    }
  });
  it('exige campos exatos nas duas posicoes e rejeita aliases antigos', () => {
    for (const wire of [DISTRIBUICAO_EXAMPLES[1], DISTRIBUICAO_CUSTODIA_EXAMPLE]) {
      const original = parseDistribuicaoCorretoras(wire);
      const position = (v: typeof original) => v.posicoes[0].diagnosticosCorretoras[1].primeiroDeficit!.posicaoCronologica as unknown as Record<string, unknown>;
      for (const key of Object.keys(position(original))) {
        const clone = structuredClone(original);delete position(clone)[key];expect(isDistribuicaoCarteiraResponse(clone)).toBe(false);
      }
      position(original)['extra'] = null;expect(isDistribuicaoCarteiraResponse(original)).toBe(false);
    }
    const legacy = DISTRIBUICAO_EXAMPLES[1].replace(
      '{"dominio":"FINANCEIRO","id":20,"natureza":"VENDA","dataEfetiva":"2026-10-02","posicaoCronologica":{"ordemNoDia":2},"saldoAposEvento":-40}',
      '{"operacaoId":20,"dataOperacao":"2026-10-02","ordemNoDia":2,"saldoAposOperacao":-40}');
    expect(() => parseDistribuicaoCorretoras(legacy)).toThrow();
  });
  it('identidades iguais em dominios diferentes nao sao confundidas nem deduplicadas', () => {
    const value = parseDistribuicaoCorretoras(DISTRIBUICAO_CUSTODIA_EXAMPLE);
    const diagnostic = value.posicoes[0].diagnosticosCorretoras[0];diagnostic.possuiDeficitHistorico = true;
    diagnostic.primeiroDeficit = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1]).posicoes[0].diagnosticosCorretoras[1].primeiroDeficit;
    expect(isDistribuicaoCarteiraResponse(value)).toBe(true);
    expect(value.posicoes[0].diagnosticosCorretoras.map(d => d.primeiroDeficit?.id)).toEqual([20, 20]);
  });
  for (const [index, example] of DISTRIBUICAO_EXAMPLES.entries()) {
    it(`valida exemplo normativo ${index + 1} sem modificar diagnosticos`, () => {
      const value = parseDistribuicaoCorretoras(example);
      expect(isDistribuicaoCarteiraResponse(value)).toBe(true);
      expect(value.posicoes[0].quantidadeConsolidada).toBe(['100', '60', '110', '100', '101'][index]);
      expect(value.posicoes[0].reconciliada).toBe(index !== 4);
      if (index === 3) {
        expect(value.posicoes[0].corretoras).toHaveLength(1);
        expect(value.posicoes[0].diagnosticosCorretoras[1].quantidadeFinal).toBe('0');
        expect(value.posicoes[0].diagnosticosCorretoras[1].primeiroDeficit?.saldoAposEvento).toBe('-10');
      }
    });
  }
  it('aceita vazio', () => expect(parseDistribuicaoCorretoras('{"carteiraId":1,"posicoes":[]}').posicoes).toEqual([]));

  it('preserva magnitude, escala e notacao cientifica sem Number', () => {
    for (const token of ['999999999999999999999.123457', '1.2345700e+25', '0.000000000000000000000001']) {
      const value = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0].replace(/:100/g, ':' + token));
      expect(value.posicoes[0].quantidadeConsolidada).toBe(token);
    }
  });
  it('rejeita strings decimais no wire, NaN, Infinity e JSON malformado', () => {
    for (const token of ['"100"', 'null', 'NaN', 'Infinity', 'true', '{}', '01']) {
      expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0].replace('"quantidadeConsolidada":100', '"quantidadeConsolidada":' + token))).toThrow();
    }
    expect(() => parseDistribuicaoCorretoras('')).toThrow();
    expect(() => parseDistribuicaoCorretoras('{')).toThrow();
  });

  it('exige cada campo e rejeita propriedades extras em todos os objetos', () => {
    const original = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1]);
    const objects = (v: typeof original) => [v, v.posicoes[0], v.posicoes[0].corretoras[0],
      v.posicoes[0].diagnosticosCorretoras[1], v.posicoes[0].diagnosticosCorretoras[1].primeiroDeficit];
    for (let i = 0; i < objects(original).length; i++) {
      for (const key of Object.keys(objects(original)[i]!)) {
        const clone = structuredClone(original);
        delete (objects(clone)[i] as unknown as Record<string, unknown>)[key];
        expect(isDistribuicaoCarteiraResponse(clone), `${i}:${key}`).toBe(false);
      }
      const clone = structuredClone(original);
      (objects(clone)[i] as unknown as Record<string, unknown>)['extra'] = true;
      expect(isDistribuicaoCarteiraResponse(clone)).toBe(false);
    }
    const divergent = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[4]);
    (divergent.posicoes[0].diagnosticoReconciliacao as unknown as Record<string, unknown>)['corretoraId'] = null;
    expect(isDistribuicaoCarteiraResponse(divergent)).toBe(false);
  });
  it('valida nulabilidade condicional sem inventar diagnostico', () => {
    const value = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1]);
    value.posicoes[0].diagnosticosCorretoras[1].primeiroDeficit = null;
    expect(isDistribuicaoCarteiraResponse(value)).toBe(false);
    value.posicoes[0].corretoras = null as never;
    expect(isDistribuicaoCarteiraResponse(value)).toBe(false);
    const healthy = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]);
    healthy.posicoes[0].diagnosticoReconciliacao = { codigo: 'DIVERGENCIA_QUANTIDADES' };
    expect(isDistribuicaoCarteiraResponse(healthy)).toBe(false);
  });
  it('rejeita colecoes null, tipos invalidos e mercado/moeda incoerentes', () => {
    const base = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]);
    for (const [key, bad] of [['corretoras', null], ['diagnosticosCorretoras', null], ['ticker', ' '],
      ['moeda', 'USD'], ['reconciliada', 'true'], ['quantidadeConsolidada', '-1']] as const) {
      const value = structuredClone(base);
      (value.posicoes[0] as unknown as Record<string, unknown>)[key] = bad;
      expect(isDistribuicaoCarteiraResponse(value)).toBe(false);
    }
    const divergent = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[4]);
    divergent.posicoes[0].diagnosticoReconciliacao = null;
    expect(isDistribuicaoCarteiraResponse(divergent)).toBe(false);
  });
  it('valida IDs sem arredondar tokens fracionarios ou int64 acima da faixa segura do frontend', () => {
    for (const token of ['9007199254740993', '9007199254740991.1', '0', '-1', '"1"']) {
      expect(() => parseDistribuicaoCorretoras('{"carteiraId":' + token + ',"posicoes":[]}')).toThrow();
    }
  });
  it('mantem ordem recebida e nao compara totais para recalcular reconciliacao', () => {
    const value = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]);
    value.posicoes[0].corretoras.reverse();
    value.posicoes[0].quantidadeAtribuidaTotal = '999';
    expect(isDistribuicaoCarteiraResponse(value)).toBe(true);
    expect(value.posicoes[0].corretoras[0].corretoraId).toBeNull();
    expect(value.posicoes[0].reconciliada).toBe(true);
  });
  it('rejeita IDs duplicados, corretora null mal identificada e evidencia invalida', () => {
    const value = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]);
    value.posicoes[0].corretoras.push(value.posicoes[0].corretoras[0]);
    expect(isDistribuicaoCarteiraResponse(value)).toBe(false);
    const nullGroup = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[0]);
    nullGroup.posicoes[0].corretoras[1].nome = 'Outra';
    expect(isDistribuicaoCarteiraResponse(nullGroup)).toBe(false);
    const negative = parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1]);
    negative.posicoes[0].diagnosticosCorretoras[1].primeiroDeficit!.saldoAposEvento = '0';
    expect(isDistribuicaoCarteiraResponse(negative)).toBe(false);
  });
  it('data da operacao e civil, inclusive legado, sem Date ou piso financeiro', () => {
    for (const data of ['0000-02-29', '-0001-01-01', '+10000-01-01', '1582-10-10', '2024-02-29']) {
      expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1].replace('2026-10-02', data))).not.toThrow();
    }
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1].replace('2026-10-02', '2025-02-29'))).toThrow();
  });
});
