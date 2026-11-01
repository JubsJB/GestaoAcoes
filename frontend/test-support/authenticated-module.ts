import { TestBed, TestModuleMetadata } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideApiConfig } from '../src/app/core/config/api.config';
import { API_BASE_URL } from '../src/app/core/config/api-base-url.token';
import { SessionService } from '../src/app/core/auth/session.service';
import { inject, provideEnvironmentInitializer } from '@angular/core';

/** Fixture de autorização de UI. Integração de cookie/CSRF tem testes próprios. */
export function authenticatedModule(metadata: TestModuleMetadata, papel: 'ADMIN' | 'USER' = 'ADMIN'): TestBed {
  const configured = TestBed.configureTestingModule({ ...metadata, providers: [provideHttpClient(), provideApiConfig(), ...(metadata.providers ?? []), provideHttpClientTesting(),
    provideEnvironmentInitializer(() => {
      const session = inject(SessionService);
      session.restore().subscribe();
      inject(HttpTestingController).expectOne(`${inject(API_BASE_URL)}/auth/me`).flush({
        id: 919, username: 'fixture-ui', papel,
        horarioServidor: '2026-10-31T12:00:00Z', ultimaAtividadeEm: '2026-10-31T12:00:00Z',
        inativaEm: '2026-10-31T12:10:00Z', expiraEm: '2026-10-31T16:00:00Z'
      });
      inject(HttpTestingController).expectOne(`${inject(API_BASE_URL)}/auth/csrf`).flush(null);
    })
  ] });
  return configured;
}
