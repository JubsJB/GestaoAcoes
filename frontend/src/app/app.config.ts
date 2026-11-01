import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { provideApiConfig } from './core/config/api.config';
import { httpErrorInterceptor } from './core/http/http-error.interceptor';
import { routes } from './app.routes';
import { sessionInterceptor } from './core/auth/session.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([httpErrorInterceptor, sessionInterceptor])),
    provideApiConfig()
  ]
};
