import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { CarteirasService } from '../../features/carteiras/carteiras.service';
import { CarteiraResponse } from '../../features/carteiras/models/carteira';
import { CARTEIRA_STORAGE, CARTEIRA_STORAGE_KEY, CarteiraContextService } from './carteira-context.service';

const A = { id: 1, nome: 'A', dataCriacao: '2026-01-01T00:00:00Z' };
const B = { ...A, id: 2, nome: 'B' };

describe('CarteiraContextService', () => {
  let storage: { getItem: ReturnType<typeof vi.fn>; setItem: ReturnType<typeof vi.fn>; removeItem: ReturnType<typeof vi.fn> };
  let service: { listar: ReturnType<typeof vi.fn> };
  let context: CarteiraContextService;
  beforeEach(() => {
    storage = { getItem: vi.fn().mockReturnValue(null), setItem: vi.fn(), removeItem: vi.fn() };
    service = { listar: vi.fn().mockReturnValue(of([B, A])) };
    TestBed.configureTestingModule({ providers: [{ provide: CarteirasService, useValue: service }, { provide: CARTEIRA_STORAGE, useValue: storage }] });
    context = TestBed.inject(CarteiraContextService);
  });

  it('shares an in-flight collection and chooses minimum id without persisting fallback', () => {
    const pending = new Subject<CarteiraResponse[]>(); service.listar.mockReturnValue(pending);
    context.initialize().subscribe(); context.initialize().subscribe();
    expect(context.loading()).toBe(true); expect(service.listar).toHaveBeenCalledTimes(1);
    pending.next([B, A]); pending.complete();
    expect(context.active()).toEqual(A); expect(storage.setItem).not.toHaveBeenCalled();
    context.initialize().subscribe(); expect(service.listar).toHaveBeenCalledTimes(1);
  });
  it('restores valid preference, then memory wins, while explicit URL wins over both', () => {
    storage.getItem.mockReturnValue('2'); context.initialize().subscribe();
    expect(context.activeId()).toBe(2);
    context.select(1); context.resolveUrl(null); expect(context.activeId()).toBe(1);
    context.resolveUrl('2'); expect(context.activeId()).toBe(2);
    expect(storage.setItem).toHaveBeenLastCalledWith(CARTEIRA_STORAGE_KEY, '2');
  });
  it.each(['99', '{broken', '', '0', '-1', '9007199254740992'])('ignores invalid preference %s', value => {
    storage.getItem.mockReturnValue(value); context.initialize().subscribe();
    expect(context.activeId()).toBe(1); expect(storage.setItem).not.toHaveBeenCalled();
  });
  it.each(['99', '', 'abc', '0', '-1', '9007199254740992'])('does not silently fallback for explicit URL %s', value => {
    storage.getItem.mockReturnValue('2'); context.resolveUrl(value); context.initialize().subscribe();
    expect(context.activeId()).toBeNull(); expect(context.invalidSelection()).toBe(true);
    expect(storage.setItem).not.toHaveBeenCalled();
    context.select(1); expect(context.invalidSelection()).toBe(false);
  });
  it('works in memory when every storage operation throws', () => {
    for (const method of Object.values(storage)) method.mockImplementation(() => { throw new Error('blocked'); });
    context.initialize().subscribe(); context.select(2); context.resolveUrl(null);
    expect(context.activeId()).toBe(2); context.remove(2); expect(context.activeId()).toBe(1);
  });
  it('keeps empty collection distinct from an error and supports explicit retry', () => {
    const error = { status: 500, message: 'Unavailable' };
    service.listar.mockReturnValueOnce(throwError(() => error)); context.initialize().subscribe();
    expect(context.error()).toBe(error); expect(context.activeId()).toBeNull();
    service.listar.mockReturnValueOnce(of([])); context.initialize(true).subscribe();
    expect(context.error()).toBeNull(); expect(context.carteiras()).toEqual([]); expect(context.activeId()).toBeNull();
  });
  it('does not let an old collection undo confirmed mutations', () => {
    const pending = new Subject<CarteiraResponse[]>(); service.listar.mockReturnValue(pending);
    context.initialize().subscribe(); context.upsert({ ...A, nome: 'Edited' }); context.remove(2);
    pending.next([A, B]); pending.complete();
    expect(context.carteiras()).toEqual([{ ...A, nome: 'Edited' }]);
  });
  it('updates names, preserves selection on create, removes active and last portfolio', () => {
    context.initialize().subscribe(); context.select(2);
    context.upsert({ ...B, nome: 'Edited' }); context.upsert({ ...A, id: 3 });
    expect(context.active()?.nome).toBe('Edited');
    storage.getItem.mockReturnValue('2'); context.remove(2);
    expect(storage.removeItem).toHaveBeenCalledWith(CARTEIRA_STORAGE_KEY); expect(context.activeId()).toBe(1);
    context.remove(3); expect(context.activeId()).toBe(1);
    context.remove(1); expect(context.activeId()).toBeNull();
  });
  it('rejects malformed collection instead of treating it as empty', () => {
    service.listar.mockReturnValue(of([{ ...A, id: -1 }])); context.initialize().subscribe();
    expect(context.error()?.kind).toBe('technical'); expect(context.activeId()).toBeNull();
  });
});
