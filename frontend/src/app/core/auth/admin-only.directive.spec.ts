import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { provideApiConfig } from '../config/api.config';
import { SessionService } from './session.service';
import { AdminOnlyDirective } from './admin-only.directive';

@Component({ imports: [AdminOnlyDirective], template: '<p>Consulta financeira</p><button *appAdmin>Gravar</button>' })
class FinancialActions {}

describe('Ações exclusivas de ADMIN', () => {
  it.each(['USER', 'ADMIN'])('%s mantém consultas e só ADMIN possui ação de escrita', async papel => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApiConfig()] });
    const http = TestBed.inject(HttpTestingController);
    const result = firstValueFrom(TestBed.inject(SessionService).restore());
    http.expectOne('/api/auth/me').flush({ id: 7, username: 'fixture', papel }); http.expectOne('/api/auth/csrf').flush(null);
    await result;
    const fixture = TestBed.createComponent(FinancialActions);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Consulta financeira');
    expect(fixture.nativeElement.querySelector('button') !== null).toBe(papel === 'ADMIN');
    http.verify();
  });
});
