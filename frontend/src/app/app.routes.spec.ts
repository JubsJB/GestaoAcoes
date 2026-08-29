import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { routes } from './app.routes';

describe('application routes', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
  });

  it('resolves the technical initial route', async () => {
    const harness = await RouterTestingHarness.create('/');

    expect(harness.routeNativeElement?.textContent).toContain('Frontend inicializado.');
  });

  it('redirects an unknown route to the technical initial route', async () => {
    const harness = await RouterTestingHarness.create('/nao-existe');

    expect(harness.routeNativeElement?.textContent).toContain('Frontend inicializado.');
  });
});
