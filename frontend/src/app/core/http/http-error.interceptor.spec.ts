import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { StandardError } from '../errors/standard-error';
import { httpErrorInterceptor } from './http-error.interceptor';

describe('httpErrorInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting()
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('preserves a StandardError returned by the backend', async () => {
    const response: StandardError = {
      timeStamp: 1788004800000,
      status: 422,
      error: 'Unprocessable Entity',
      message: 'Falha pública.',
      path: '/recurso',
      code: 'PUBLIC_ERROR',
      details: { quantity: 'invalid' }
    };
    const result = firstValueFrom(httpClient.get('/api/test'));

    httpTesting.expectOne('/api/test').flush(response, {
      status: 422,
      statusText: 'Unprocessable Entity'
    });

    await expect(result).rejects.toMatchObject({
      kind: 'standard',
      status: 422,
      code: 'PUBLIC_ERROR',
      message: 'Falha pública.',
      details: { quantity: 'invalid' },
      standardError: response
    });
  });

  it('normalizes a network failure without a domain message', async () => {
    const result = firstValueFrom(httpClient.get('/api/test'));

    httpTesting.expectOne('/api/test').error(new ProgressEvent('network'));

    await expect(result).rejects.toMatchObject({
      kind: 'technical',
      status: 0,
      code: null,
      message: 'Falha técnica na comunicação HTTP.',
      details: {},
      standardError: null
    });
  });
});
