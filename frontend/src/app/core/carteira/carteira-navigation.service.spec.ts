import { Component, DestroyRef, inject } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { CarteirasService } from '../../features/carteiras/carteiras.service';
import { CARTEIRA_STORAGE, CarteiraContextService } from './carteira-context.service';
import { CarteiraNavigationService } from './carteira-navigation.service';

@Component({ template: '' })
class DashboardStub {
  constructor() { inject(CarteiraNavigationService).bindDashboard(inject(ActivatedRoute), inject(DestroyRef)); }
}
@Component({ template: '' })
class PageStub {}

describe('CarteiraNavigationService', () => {
  let storage: { getItem: ReturnType<typeof vi.fn>; setItem: ReturnType<typeof vi.fn>; removeItem: ReturnType<typeof vi.fn> };
  beforeEach(() => {
    storage = { getItem: vi.fn().mockReturnValue(null), setItem: vi.fn(), removeItem: vi.fn() };
    TestBed.configureTestingModule({ providers: [
      provideRouter([{ path: 'dashboard', component: DashboardStub }, { path: 'carteiras/:id', component: PageStub }, { path: 'operacoes/nova', component: PageStub }]),
      { provide: CARTEIRA_STORAGE, useValue: storage },
      { provide: CarteirasService, useValue: { listar: () => of([{ id: 2, nome: 'B', dataCriacao: '' }, { id: 1, nome: 'A', dataCriacao: '' }]) } }
    ] });
  });
  it('normalizes fallback without persisting it and honors explicit URL and subsequent navigation', async () => {
    const harness = await RouterTestingHarness.create('/dashboard?extra=kept');
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/dashboard?extra=kept&carteiraId=1');
    expect(storage.setItem).not.toHaveBeenCalled();
    await harness.navigateByUrl('/dashboard?carteiraId=2');
    expect(TestBed.inject(CarteiraContextService).activeId()).toBe(2);
    await harness.navigateByUrl('/dashboard?carteiraId=1');
    expect(TestBed.inject(CarteiraContextService).activeId()).toBe(1);
    await harness.navigateByUrl('/dashboard?carteiraId=bad');
    expect(TestBed.inject(CarteiraContextService).invalidSelection()).toBe(true);
    TestBed.inject(CarteiraNavigationService).select(2);
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/dashboard?carteiraId=2');
  });
  it('navigates portfolio detail but never rewrites an operation form origin', async () => {
    const navigation = TestBed.inject(CarteiraNavigationService); navigation.start();
    const harness = await RouterTestingHarness.create('/carteiras/1');
    navigation.select(2); await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/carteiras/2');
    await harness.navigateByUrl('/operacoes/nova?carteiraId=1&origem=carteira');
    navigation.select(1); navigation.select(2); await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/operacoes/nova?carteiraId=1&origem=carteira');
  });
});
