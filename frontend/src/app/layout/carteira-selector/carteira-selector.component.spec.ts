import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { CARTEIRA_STORAGE, CarteiraContextService } from '../../core/carteira/carteira-context.service';
import { CarteirasService } from '../../features/carteiras/carteiras.service';
import { CarteiraSelectorComponent } from './carteira-selector.component';

describe('CarteiraSelectorComponent', () => {
  it('reflects minimum id despite collection order and supports explicit selection', () => {
    const listar = vi.fn().mockReturnValue(of([{ id: 2, nome: 'B', dataCriacao: '' }, { id: 1, nome: 'A', dataCriacao: '' }]));
    const storage = { getItem: vi.fn().mockReturnValue(null), setItem: vi.fn(), removeItem: vi.fn() };
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: CarteirasService, useValue: { listar } }, { provide: CARTEIRA_STORAGE, useValue: storage }] });
    const fixture = TestBed.createComponent(CarteiraSelectorComponent); fixture.detectChanges();
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    expect(select.value).toBe('1'); expect(select.disabled).toBe(false);
    expect(fixture.nativeElement.querySelector('label')?.htmlFor).toBe(select.id);
    select.value = '2'; select.dispatchEvent(new Event('change')); fixture.detectChanges();
    expect(TestBed.inject(CarteiraContextService).activeId()).toBe(2);
    expect(storage.setItem).toHaveBeenCalledWith('gestaoacoes.carteira-ativa.v1', '2');
    expect(listar).toHaveBeenCalledTimes(1);
  });
  it('announces loading and error, then allows a manual retry to empty', () => {
    const pending = new Subject<never[]>(); const listar = vi.fn().mockReturnValue(pending);
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: CarteirasService, useValue: { listar } }, { provide: CARTEIRA_STORAGE, useValue: null }] });
    const fixture = TestBed.createComponent(CarteiraSelectorComponent); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Carregando carteiras');
    pending.error({ message: 'Falha na lista' }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Falha na lista');
    expect(fixture.nativeElement.querySelector('select').disabled).toBe(true);
    listar.mockReturnValue(of([])); fixture.nativeElement.querySelector('button').click(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/carteiras/nova');
  });
});
