export function onlyDigits(value: string): string {
  return value.replace(/\D/g, '');
}

export function formatCnpj(value: string): string {
  const digits = onlyDigits(value).slice(0, 14);
  return digits
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d)/, '$1-$2');
}

export function formatCep(value: string): string {
  const digits = onlyDigits(value).slice(0, 8);
  return digits.replace(/^(\d{5})(\d)/, '$1-$2');
}

export function displayOptional(value: string | null): string {
  return value ?? 'Não informado';
}

export type CorretoraStatusVariant = 'positive' | 'warning' | 'error' | 'neutral';

const WARNING_STATUSES = new Set(['SUSPENSA', 'PENDENTE', 'NÃO ATIVA', 'NAO ATIVA']);
const ERROR_STATUSES = new Set(['BAIXADA', 'INAPTA', 'INATIVA', 'NULA', 'CANCELADA']);

export function corretoraStatusVariant(value: string): CorretoraStatusVariant {
  const normalized = value.trim().toLocaleUpperCase('pt-BR');
  if (normalized === 'ATIVA') return 'positive';
  if (WARNING_STATUSES.has(normalized)) return 'warning';
  if (ERROR_STATUSES.has(normalized)) return 'error';
  return 'neutral';
}
