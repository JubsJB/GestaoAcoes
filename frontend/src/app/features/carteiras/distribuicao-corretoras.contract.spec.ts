import { describe, expect, it } from 'vitest';
import { isDistribuicaoCarteiraResponse, parseDistribuicaoCorretoras } from './distribuicao-corretoras.contract';
import { DISTRIBUICAO_EXAMPLES } from './testing/distribuicao-corretoras.fixtures';

describe('Contrato de distribuicao por corretora', () => {
  for (const [index, example] of DISTRIBUICAO_EXAMPLES.entries()) {
    it(`valida exemplo normativo ${index + 1} sem modificar diagnosticos`, () => {
      const value = parseDistribuicaoCorretoras(example);
      expect(isDistribuicaoCarteiraResponse(value)).toBe(true);
      expect(value.posicoes[0].quantidadeConsolidada).toBe(['100', '60', '110', '100', '101'][index]);
      expect(value.posicoes[0].reconciliada).toBe(index !== 4);
      if (index === 3) {
        expect(value.posicoes[0].corretoras).toHaveLength(1);
        expect(value.posicoes[0].diagnosticosCorretoras[1].quantidadeFinal).toBe('0');
        expect(value.posicoes[0].diagnosticosCorretoras[1].primeiroDeficit?.saldoAposOperacao).toBe('-10');
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
    negative.posicoes[0].diagnosticosCorretoras[1].primeiroDeficit!.saldoAposOperacao = '0';
    expect(isDistribuicaoCarteiraResponse(negative)).toBe(false);
  });
  it('data da operacao e civil, inclusive legado, sem Date ou piso financeiro', () => {
    for (const data of ['0000-02-29', '-0001-01-01', '+10000-01-01', '1582-10-10', '2024-02-29']) {
      expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1].replace('2026-10-02', data))).not.toThrow();
    }
    expect(() => parseDistribuicaoCorretoras(DISTRIBUICAO_EXAMPLES[1].replace('2026-10-02', '2025-02-29'))).toThrow();
  });
});
