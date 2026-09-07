import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { CARTEIRA_STORAGE, CarteiraContextService } from '../../core/carteira/carteira-context.service';
import { provideApiConfig } from '../../core/config/api.config';
import { CarteiraDeleteConfirmDialogComponent } from './carteira-delete-confirm-dialog.component';
import { CarteiraFormPageComponent } from './pages/carteira-form-page.component';

const A = { id: 1, nome: 'A', dataCriacao: '2026-01-01T00:00:00Z' };
const B = { ...A, id: 2, nome: 'B' };
describe('Confirmed HTTP mutations and carteira context', () => {
  let http: HttpTestingController;
  let context: CarteiraContextService;
  let storage: { getItem: ReturnType<typeof vi.fn>; setItem: ReturnType<typeof vi.fn>; removeItem: ReturnType<typeof vi.fn> };
  function setup(data: unknown) {
    storage = { getItem: vi.fn().mockReturnValue('1'), setItem: vi.fn(), removeItem: vi.fn() };
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideApiConfig(),
      { provide: CARTEIRA_STORAGE, useValue: storage }, { provide: MAT_DIALOG_DATA, useValue: data }, { provide: MatDialogRef, useValue: { close: vi.fn() } }
    ] });
    http = TestBed.inject(HttpTestingController); context = TestBed.inject(CarteiraContextService);
    context.initialize().subscribe(); http.expectOne('/api/carteiras').flush([A, B]);
  }
  afterEach(() => http.verify());
  it('incorporates only the confirmed POST response and preserves active selection', () => {
    setup({ mode: 'create' });
    const fixture = TestBed.createComponent(CarteiraFormPageComponent); fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input'); input.value = 'Nova'; input.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    const post = http.expectOne('/api/carteiras'); expect(post.request.method).toBe('POST'); expect(post.request.body).toEqual({ nome: 'Nova' });
    expect(context.carteiras()).toHaveLength(2);
    post.flush({ ...A, id: 3, nome: 'Nova' }, { status: 201, statusText: 'Created' });
    expect(context.carteiras()).toHaveLength(3); expect(context.activeId()).toBe(1);
  });
  it('updates the active name only after PATCH response', () => {
    setup({ mode: 'edit', carteira: A });
    const fixture = TestBed.createComponent(CarteiraFormPageComponent); fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input'); input.value = 'Edited'; input.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    const patch = http.expectOne('/api/carteiras/1'); expect(patch.request.method).toBe('PATCH'); expect(patch.request.body).toEqual({ nome: 'Edited' });
    expect(context.active()?.nome).toBe('A'); patch.flush({ ...A, nome: 'Edited' });
    expect(context.active()?.nome).toBe('Edited'); expect(context.activeId()).toBe(1);
  });
  it('reconciles active deletion only after DELETE 204', () => {
    setup(A); const fixture = TestBed.createComponent(CarteiraDeleteConfirmDialogComponent); fixture.detectChanges();
    fixture.nativeElement.querySelectorAll('button')[1].click();
    const request = http.expectOne('/api/carteiras/1'); expect(request.request.method).toBe('DELETE');
    expect(context.activeId()).toBe(1); request.flush(null, { status: 204, statusText: 'No Content' });
    expect(context.activeId()).toBe(2); expect(context.carteiras()).toEqual([B]); expect(storage.removeItem).toHaveBeenCalled();
  });
  it.each([409, 500])('keeps selection, collection and preference on DELETE %s', status => {
    setup(A); const fixture = TestBed.createComponent(CarteiraDeleteConfirmDialogComponent); fixture.detectChanges();
    fixture.nativeElement.querySelectorAll('button')[1].click(); http.expectOne('/api/carteiras/1').flush({}, { status, statusText: 'Failed' });
    expect(context.activeId()).toBe(1); expect(context.carteiras()).toEqual([A, B]); expect(storage.removeItem).not.toHaveBeenCalled();
  });
});
