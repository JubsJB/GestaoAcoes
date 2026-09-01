import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { CarteirasService } from '../carteiras.service';
import { CarteiraResponse } from '../models/carteira';
import { CarteiraFormPageComponent } from './carteira-form-page.component';

const CARTEIRA: CarteiraResponse = { id: 4, nome: 'Principal', dataCriacao: '2026-08-31T12:00:00Z' };
const error = { status: 409, code: 'REGRA_BACKEND', message: 'Conflito', details: { motivo: 'backend' } } as unknown as NormalizedHttpError;

describe('CarteiraFormPageComponent em rota direta', () => {
  let fixture: ComponentFixture<CarteiraFormPageComponent>;
  let service: { buscarPorId: ReturnType<typeof vi.fn>; cadastrar: ReturnType<typeof vi.fn>; atualizar: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  async function create(mode: 'create' | 'edit'): Promise<void> {
    service = { buscarPorId: vi.fn().mockReturnValue(of(CARTEIRA)), cadastrar: vi.fn().mockReturnValue(of(CARTEIRA)), atualizar: vi.fn().mockReturnValue(of({ ...CARTEIRA, nome: 'Atualizada' })) }; toast = { show: vi.fn() };
    await TestBed.configureTestingModule({ imports: [CarteiraFormPageComponent], providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { snapshot: { data: { mode }, paramMap: { get: () => '4' } } } }, { provide: CarteirasService, useValue: service }, { provide: SuccessToastService, useValue: toast }] }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true); fixture = TestBed.createComponent(CarteiraFormPageComponent); fixture.detectChanges();
  }
  afterEach(() => TestBed.resetTestingModule());

  it('cadastro inicia vazio sem HTTP, valida branco e envia POST único com nome aparado', async () => { await create('create'); expect(service.buscarPorId).not.toHaveBeenCalled(); expect(service.cadastrar).not.toHaveBeenCalled(); const input = fixture.nativeElement.querySelector('input') as HTMLInputElement; input.value = '   '; input.dispatchEvent(new Event('input')); (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit')); expect(service.cadastrar).not.toHaveBeenCalled(); input.value = '  Principal  '; input.dispatchEvent(new Event('input')); (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit')); expect(service.cadastrar).toHaveBeenCalledOnce(); expect(service.cadastrar).toHaveBeenCalledWith({ nome: 'Principal' }); expect(service.atualizar).not.toHaveBeenCalled(); });
  it('bloqueia POST concorrente', async () => { await create('create'); service.cadastrar.mockReturnValue(new Subject<CarteiraResponse>()); const input = fixture.nativeElement.querySelector('input') as HTMLInputElement; input.value = 'Nome'; input.dispatchEvent(new Event('input')); const form = fixture.nativeElement.querySelector('form') as HTMLFormElement; form.dispatchEvent(new Event('submit')); form.dispatchEvent(new Event('submit')); expect(service.cadastrar).toHaveBeenCalledTimes(1); });
  it('edição direta carrega, preenche e envia somente nome no PATCH', async () => { await create('edit'); expect(service.buscarPorId).toHaveBeenCalledWith(4); expect((fixture.nativeElement.querySelector('input') as HTMLInputElement).value).toBe('Principal'); (fixture.nativeElement.querySelector('input') as HTMLInputElement).value = 'Atualizada'; fixture.nativeElement.querySelector('input').dispatchEvent(new Event('input')); (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit')); expect(service.atualizar).toHaveBeenCalledWith(4, { nome: 'Atualizada' }); expect(service.atualizar).toHaveBeenCalledTimes(1); expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/carteiras', 4], { info: { carteira: { ...CARTEIRA, nome: 'Atualizada' } } }); });
  it('preserva entrada e apresenta StandardError sem retry automático', async () => { await create('edit'); service.atualizar.mockReturnValueOnce(throwError(() => error)); const input = fixture.nativeElement.querySelector('input') as HTMLInputElement; input.value = 'Tentativa'; input.dispatchEvent(new Event('input')); (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit')); fixture.detectChanges(); expect(input.value).toBe('Tentativa'); expect(fixture.nativeElement.textContent).toContain('Conflito'); expect(service.atualizar).toHaveBeenCalledTimes(1); });
});
