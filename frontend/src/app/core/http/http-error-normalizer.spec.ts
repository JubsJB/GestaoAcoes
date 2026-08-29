import { HttpErrorResponse } from '@angular/common/http';

import { StandardError } from '../errors/standard-error';
import { isStandardError, normalizeHttpError } from './http-error-normalizer';

const standardError: StandardError = {
  timeStamp: 1788004800000,
  status: 409,
  error: 'Conflict',
  message: 'Falha pública.',
  path: '/recurso/1',
  code: null,
  details: { field: 'value' }
};

describe('HTTP error normalizer', () => {
  it('recognizes the complete StandardError contract', () => {
    expect(isStandardError(standardError)).toBe(true);
    expect(isStandardError({ status: 409 })).toBe(false);
  });

  it('preserves a valid StandardError and all public information', () => {
    const originalError = new HttpErrorResponse({ error: standardError, status: 409 });
    const normalized = normalizeHttpError(originalError);

    expect(normalized.kind).toBe('standard');
    expect(normalized.standardError).toBe(standardError);
    expect(normalized.status).toBe(409);
    expect(normalized.code).toBeNull();
    expect(normalized.message).toBe('Falha pública.');
    expect(normalized.details).toEqual({ field: 'value' });
  });

  it('normalizes an unknown technical failure predictably', () => {
    const originalError = new HttpErrorResponse({ error: new ProgressEvent('error'), status: 0 });
    const normalized = normalizeHttpError(originalError);

    expect(normalized).toMatchObject({
      kind: 'technical',
      status: 0,
      code: null,
      message: 'Falha técnica na comunicação HTTP.',
      details: {},
      standardError: null
    });
    expect(normalized.originalError).toBe(originalError);
  });
});
