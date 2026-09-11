import { parseLosslessJson } from '../../core/http/lossless-json';

const DECIMAL_FIELDS = new Set(['quantidade', 'precoUnitario', 'precoUnitarioSugerido', 'valorTotal']);

export function parseOperationJson<T>(text: string): T {
  try {
    return parseLosslessJson<T>(text, DECIMAL_FIELDS);
  } catch (error) {
    if (typeof text !== 'string' || !text.trim()) throw new Error('Resposta JSON de Operações vazia.');
    throw error;
  }
}
