import { Mercado, Moeda } from './models/acao';

export function formatMercado(mercado: Mercado): string {
  return mercado === 'BRASIL' ? 'Brasil' : 'EUA';
}

export function formatCotacao(value: number, moeda: Moeda): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: moeda }).format(value);
}

export function formatDataHora(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(date);
}
