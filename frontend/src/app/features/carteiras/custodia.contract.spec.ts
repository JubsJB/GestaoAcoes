import { describe, expect, it } from 'vitest';
import { parseDetalheCustodia, parseHistoricoCustodia, parseRegistroCustodia } from './custodia.contract';
import { estornoCustodia, originalCustodia } from './testing/custodia.fixtures';

describe('contrato de custódia', () => {
  it('preserva uma original completa e seus nulos, decimal e fração textuais', () => {
    expect(parseRegistroCustodia(JSON.stringify(originalCustodia()), 1)).toEqual(
      { ...originalCustodia(), quantidade: '40' });
  });
  it('rejeita resposta incompleta em vez de produzir uma movimentação parcial', () => {
    const value = originalCustodia(); delete value['posicaoCronologica'];
    expect(() => parseRegistroCustodia(JSON.stringify(value), 1)).toThrow();
  });
  it.each(['9999999999999.123456', '0.000001', '1E-6'])('preserva quantidade EUA %s sem passagem binária', q => {
    const json = JSON.stringify(originalCustodia({ mercado: 'EUA' })).replace('"quantidade":40', `"quantidade":${q}`);
    expect(parseRegistroCustodia(json, 1).quantidade).toBe(q);
  });
  it('preserva BRASIL matematicamente inteiro com escala e instante original', () => {
    const json = JSON.stringify(originalCustodia()).replace('"quantidade":40', '"quantidade":100.000000');
    const value = parseRegistroCustodia(json, 1);
    expect(value.quantidade).toBe('100.000000'); expect(value.dataRegistro).toBe('2026-10-24T12:00:00.123456Z');
  });
  it.each([
    { mercado: 'BRASIL', quantidade: 0.1 }, { quantidade: 0 }, { quantidade: -1 },
    { quantidade: '40' }, { mercado: 'EUA', quantidade: 0.0000001 }, { quantidade: null },
    { id: 0 }, { id: 1.5 }, { carteiraId: 2 }, { tipoRegistro: 'FINANCEIRO' },
    { situacao: 'ESTORNADA' }, { estornoId: 20 }, { natureza: 'ATRIBUICAO' },
    { corretoraOrigemId: 4 }, { corretoraDestinoId: null }, { motivo: 'indevido' },
    { dataEfetiva: '2026-02-30' }, { dataRegistro: '2026-10-01T24:00:00Z' },
    { dataRegistro: '2026-10-01T12:00:00' }, { dataRegistro: '2026-10-01T12:00:00+18:01' },
    { preco: 10 }
  ])('rejeita contrato incoerente %#', extra => {
    expect(() => parseRegistroCustodia(JSON.stringify(originalCustodia(extra)), 1)).toThrow();
  });
  it.each(['9007199254740993', '9007199254740992', '1e100'])('rejeita ID inseguro %s antes de converter', id => {
    expect(() => parseRegistroCustodia(JSON.stringify(originalCustodia()).replace('"id":10', `"id":${id}`), 1)).toThrow();
  });
  it('rejeita quantidade maior que 2^53, incompatível com NUMERIC(19,6), sem arredondar', () => {
    const json = JSON.stringify(originalCustodia()).replace('"quantidade":40', '"quantidade":9007199254740993');
    expect(() => parseRegistroCustodia(json, 1)).toThrow();
  });
  it('aceita o limite seguro dos IDs', () => {
    expect(parseRegistroCustodia(JSON.stringify(originalCustodia({ id: 9007199254740991 })), 1).id).toBe(9007199254740991);
  });
  it.each([
    { numerador: '0', denominador: '2' }, { numerador: '01', denominador: '2' },
    { numerador: '2', denominador: '4' }, { numerador: '2', denominador: '2' },
    { numerador: 1, denominador: 2 }, { numerador: '1', denominador: '9'.repeat(513) }
  ])('rejeita fração não canônica ou fora do limite %#', fracao => {
    expect(() => parseRegistroCustodia(JSON.stringify(originalCustodia({
      posicaoCronologica: { ordemFinanceiraAncora: 0, ...fracao } })), 1)).toThrow();
  });
  it.each([
    ['9007199254740993', '9007199254740994'], ['1' + '0'.repeat(511), '1' + '0'.repeat(510) + '1']
  ])('conserva integralmente fração textual de precisão exata %#', (numerador, denominador) => {
    const posicaoCronologica = { ordemFinanceiraAncora: 0, numerador, denominador };
    expect(parseRegistroCustodia(JSON.stringify(originalCustodia({ posicaoCronologica })), 1).posicaoCronologica)
      .toEqual(posicaoCronologica);
  });
  it('preserva identidades coincidentes entre original e estorno, conjunto e ordem recebida', () => {
    const m = originalCustodia({ situacao: 'ESTORNADA', estornoId: 10 });
    const e = estornoCustodia({ id: 10, estornoId: 10 });
    const v = parseHistoricoCustodia(JSON.stringify([e, m]), 1);
    expect(v.map(r => `${r.tipoRegistro}:${r.id}`)).toEqual(['ESTORNO:10', 'MOVIMENTACAO:10']);
    expect(parseDetalheCustodia(JSON.stringify({ movimentacao: m, estorno: e }), 1, 10).estorno?.id).toBe(10);
  });
  it('aceita coleção vazia, original ativa e resultado filtrado somente de estornos', () => {
    expect(parseHistoricoCustodia('[]', 1)).toEqual([]);
    expect(parseHistoricoCustodia(JSON.stringify([estornoCustodia()]), 1)).toHaveLength(1);
    expect(parseDetalheCustodia(JSON.stringify({ movimentacao: originalCustodia(), estorno: null }), 1, 10).estorno).toBeNull();
  });
  it('compara quantidade do vínculo matematicamente, preservando cada token original', () => {
    const json = JSON.stringify({ movimentacao: originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 }),
      estorno: estornoCustodia() }).replace('"quantidade":40', '"quantidade":40.000000')
      .replace('"quantidade":40,', '"quantidade":4E1,');
    const detalhe = parseDetalheCustodia(json, 1, 10);
    expect(detalhe.movimentacao.quantidade).toBe('40.000000'); expect(detalhe.estorno?.quantidade).toBe('4E1');
  });
  it.each([
    { movimentacao: originalCustodia(), estorno: estornoCustodia() },
    { movimentacao: originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 }), estorno: null },
    { movimentacao: originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 }), estorno: estornoCustodia({ quantidade: 39 }) },
    { movimentacao: originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 }), estorno: estornoCustodia({ movimentacaoOriginalId: 11 }) }
  ])('rejeita detalhe misturado ou vínculo divergente %#', v => {
    expect(() => parseDetalheCustodia(JSON.stringify(v), 1, 10)).toThrow();
  });
  it('não retorna histórico parcial quando há linha inválida, duplicada ou original contraditória', () => {
    for (const v of [[originalCustodia(), {}], [originalCustodia(), originalCustodia()], [originalCustodia(), estornoCustodia()]])
      expect(() => parseHistoricoCustodia(JSON.stringify(v), 1)).toThrow();
    expect(() => parseDetalheCustodia(JSON.stringify({ movimentacao: originalCustodia(), estorno: null }), 1, 11)).toThrow();
    expect(() => parseHistoricoCustodia('[]', 0)).toThrow();
  });
});
