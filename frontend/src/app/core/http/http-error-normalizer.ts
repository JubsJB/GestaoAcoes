import { HttpErrorResponse } from '@angular/common/http';

import { NormalizedHttpError } from '../errors/normalized-http-error';
import { StandardError } from '../errors/standard-error';

const TECHNICAL_ERROR_MESSAGE = 'Falha técnica na comunicação HTTP.';

export function isStandardError(value: unknown): value is StandardError {
  if (!isRecord(value)) {
    return false;
  }

  return typeof value['timeStamp'] === 'number'
    && typeof value['status'] === 'number'
    && typeof value['error'] === 'string'
    && typeof value['message'] === 'string'
    && typeof value['path'] === 'string'
    && (typeof value['code'] === 'string' || value['code'] === null)
    && isRecord(value['details']);
}

export function normalizeHttpError(error: HttpErrorResponse): NormalizedHttpError {
  if (isStandardError(error.error)) {
    return {
      kind: 'standard',
      status: error.error.status,
      code: error.error.code,
      message: error.error.message,
      details: error.error.details,
      standardError: error.error,
      originalError: error
    };
  }

  return {
    kind: 'technical',
    status: error.status,
    code: null,
    message: TECHNICAL_ERROR_MESSAGE,
    details: {},
    standardError: null,
    originalError: error
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
