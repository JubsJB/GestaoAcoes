import { describe, expect, it } from 'vitest';
import { parseErroQuantitativo } from './quantitative-error-json';

describe('JSON de erro quantitativo financeiro', () => {
  it.each(['FINANCEIRO', 'CUSTODIA'])('preserva valores exatos e identidade %s', dominio => {
    const fracao = '9'.repeat(512);
    const body = parseErroQuantitativo(`{"code":"CONFLITO_HISTORICO_QUANTITATIVO","details":{
      "quantidadeDisponivel":9007199254740993.123456,"evento":{"dominio":"${dominio}","id":12,
      "posicaoCronologica":{"ordemNoDia":2,"ordemFinanceiraAncora":0,"numerador":"${fracao}","denominador":"1"}},"grupo":{"corretoraId":null}}}`) as any;
    expect(body.details.quantidadeDisponivel).toBe('9007199254740993.123456');
    expect(body.details.evento.id).toBe(12); expect(body.details.evento.dominio).toBe(dominio);
    expect(body.details.evento.posicaoCronologica.numerador).toBe(fracao);
    expect(body.details.grupo.corretoraId).toBeNull();
  });
  it.each(['9007199254740993', '-1', '1.5'])('recusa identidade não representável %s', id => {
    expect(() => parseErroQuantitativo(`{"code":"POSICAO_INSUFICIENTE","details":{"evento":{"id":${id}}}}`)).toThrow();
  });
});
