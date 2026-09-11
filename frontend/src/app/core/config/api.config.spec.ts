import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from './api-base-url.token';
import { DEFAULT_API_BASE_URL, provideApiConfig } from './api.config';

describe('API configuration', () => {
  it('provides the relative API prefix by default', () => {
    TestBed.configureTestingModule({ providers: [provideApiConfig()] });

    expect(TestBed.inject(API_BASE_URL)).toBe(DEFAULT_API_BASE_URL);
  });

  it('allows tests and deployments to override the API base URL', () => {
    TestBed.configureTestingModule({ providers: [provideApiConfig('/test-api')] });

    expect(TestBed.inject(API_BASE_URL)).toBe('/test-api');
  });
});
