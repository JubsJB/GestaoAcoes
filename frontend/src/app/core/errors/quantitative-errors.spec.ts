import { describe, expect, it } from 'vitest';
import { detalhesQuantitativos, mensagemQuantitativa } from './quantitative-errors';
import { NormalizedHttpError } from './normalized-http-error';

describe('erros quantitativos públicos', () => {
  it.each(['POSICAO_INSUFICIENTE', 'SALDO_CORRETORA_INSUFICIENTE', 'SALDO_ORIGEM_INSUFICIENTE',
    'POSICAO_CUSTODIA_INDISPONIVEL', 'CONFLITO_HISTORICO_QUANTITATIVO', 'MOVIMENTACAO_CUSTODIA_INVALIDADA',
    'DATA_EFETIVA_FUTURA', 'ESTORNO_DUPLICADO', 'LIMITE_POSICAO_CUSTODIA', 'CONFLITO_TRANSACIONAL_TRANSITORIO'])(
    'explica %s nos três formulários com detalhes ausentes ou nulos', code => {
      for (const details of [null, undefined]) {
        const erro = { kind: 'standard', status: 409, code, message: 'SQL segredo', details } as unknown as NormalizedHttpError;
        for (const contexto of ['operacao', 'cadastro', 'estorno'] as const) {
          expect(mensagemQuantitativa(erro, contexto)).toBeTruthy();
          expect(mensagemQuantitativa(erro, contexto)).not.toMatch(/SQL|segredo/);
        }
        expect(detalhesQuantitativos(erro)).toEqual({});
      }
    });
  it('não substitui contratos de erros não quantitativos nem exibe detalhes técnicos', () => {
    expect(mensagemQuantitativa(null)).toBeNull();
    const erro = { kind: 'standard', status: 400, code: 'REQUEST_INVALIDO', details: { campo: 'ticker' } } as unknown as NormalizedHttpError;
    expect(mensagemQuantitativa(erro)).toBeNull(); expect(detalhesQuantitativos(erro)).toEqual({ campo: 'ticker' });
    expect(detalhesQuantitativos({ ...erro, status: 500 })).toEqual({});
  });
  it('explica insuficiência local sem expor a mensagem técnica recebida', () => {
    const erro = { kind: 'standard', status: 409, code: 'SALDO_CORRETORA_INSUFICIENTE',
      message: 'SQL SELECT interno', details: {} } as unknown as NormalizedHttpError;
    expect(mensagemQuantitativa(erro)).toContain('saldo da corretora');
    expect(mensagemQuantitativa(erro)).not.toContain('SQL');
  });
  it('preserva evento/grupo/valores nulos e exatos, excluindo campos internos', () => {
    const erro = { kind: 'standard', status: 409, code: 'MOVIMENTACAO_CUSTODIA_INVALIDADA', message: 'Conflito', details: {
      carteiraId: 1, saldoAntes: '-9999999999999.123456', origem: { corretoraId: null, categoria: 'SEM_CORRETORA_INFORMADA', sql: 'interno' },
      evento: { dominio: 'CUSTODIA', id: 3, natureza: 'TRANSFERENCIA', dataEfetiva: '2026-10-01',
        posicaoCronologica: { ordemFinanceiraAncora: 1, numerador: '9007199254740993', denominador: '9007199254740994' }, stackTrace: 'interno' },
      deficitAntes: null, sql: 'select segredo', cause: { password: 'segredo' }
    } } as unknown as NormalizedHttpError;
    expect(detalhesQuantitativos(erro)).toEqual({ carteiraId: 1, saldoAntes: '-9999999999999.123456',
      origem: { corretoraId: null, categoria: 'SEM_CORRETORA_INFORMADA' }, deficitAntes: null,
      evento: { dominio: 'CUSTODIA', id: 3, natureza: 'TRANSFERENCIA', dataEfetiva: '2026-10-01',
        posicaoCronologica: { ordemFinanceiraAncora: 1, numerador: '9007199254740993', denominador: '9007199254740994' } } });
  });
});
