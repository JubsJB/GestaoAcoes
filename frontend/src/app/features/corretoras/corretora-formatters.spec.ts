import { corretoraStatusVariant, displayOptional, formatCep, formatCnpj, onlyDigits } from './corretora-formatters';

describe('corretora formatters', () => {
  it('normaliza e formata CNPJ sem validar regra de negócio', () => {
    expect(onlyDigits('11.222.333/0001-81')).toBe('11222333000181');
    expect(formatCnpj('11222333000181')).toBe('11.222.333/0001-81');
  });

  it('formata CEP e representa apenas null como ausente', () => {
    expect(formatCep('01001000')).toBe('01001-000');
    expect(displayOptional(null)).toBe('Não informado');
    expect(displayOptional('0')).toBe('0');
  });

  it.each([
    ['ATIVA', 'positive'],
    ['SUSPENSA', 'warning'],
    ['INATIVA', 'error'],
    ['EM ANÁLISE', 'neutral']
  ] as const)('mapeia visualmente %s sem criar outro valor de status', (status, variant) => {
    expect(corretoraStatusVariant(status)).toBe(variant);
    expect(status).toBe(status);
  });
});
