import { Mercado, Moeda } from './models/acao';
import { formatOffsetDateTime } from '../../shared/formatters/offset-date-time.formatter';

export function formatMercado(mercado: Mercado): string {
  return mercado === 'BRASIL' ? 'Brasil' : 'EUA';
}

export function formatCotacao(value: number, moeda: Moeda): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: moeda }).format(value);
}

export function formatDataHora(value: string): string {
  return formatOffsetDateTime(value);
}
