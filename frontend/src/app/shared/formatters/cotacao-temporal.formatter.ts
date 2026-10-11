import { ReferenciaMercadoCotacaoResponse } from '../models/cotacao-temporal';
import { formatOffsetDateTime } from './offset-date-time.formatter';

/** Data civil: apenas reorganiza o texto, sem relógio, Date ou timezone. */
export function formatCivilDate(value: string): string {
  const parts = /^([+-]?\d{4,})-(\d{2})-(\d{2})$/.exec(value);
  return parts ? `${parts[3]}/${parts[2]}/${parts[1]}` : value;
}

export function formatReferenciaCotacao(reference: ReferenciaMercadoCotacaoResponse): string {
  switch (reference.tipo) {
    case 'DATA': return `Cotação de ${formatCivilDate(reference.data)}`;
    case 'INSTANTE': return `Cotação de ${formatOffsetDateTime(reference.instante)}`;
    case 'DESCONHECIDA': return 'Referência da cotação indisponível';
  }
}

export function formatCotacaoObtidaEm(value: string | null): string {
  return value === null ? 'Não informada' : formatOffsetDateTime(value);
}
