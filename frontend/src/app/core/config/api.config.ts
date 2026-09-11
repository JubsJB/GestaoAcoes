import { Provider } from '@angular/core';

import { API_BASE_URL } from './api-base-url.token';

export const DEFAULT_API_BASE_URL = '/api';

export function provideApiConfig(apiBaseUrl = DEFAULT_API_BASE_URL): Provider {
  return { provide: API_BASE_URL, useValue: apiBaseUrl };
}
