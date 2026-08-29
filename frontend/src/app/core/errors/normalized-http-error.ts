import { HttpErrorResponse } from '@angular/common/http';

import { StandardError } from './standard-error';

export interface NormalizedHttpError {
  kind: 'standard' | 'technical';
  status: number;
  code: string | null;
  message: string;
  details: Record<string, unknown>;
  standardError: StandardError | null;
  originalError: HttpErrorResponse;
}
