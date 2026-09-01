import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { CarteirasService } from '../carteiras.service';
import { CarteiraResponse } from '../models/carteira';
import { CarteiraFormPageComponent } from './carteira-form-page.component';
import { CarteirasListPageComponent } from './carteiras-list-page.component';

const CARTEIRA: CarteiraResponse = { id: 2, nome: 'Carteira principal', dataCriacao: '2026-08-31T12:00:00Z' };
const failure = { status: 500, code: null, message: 'Falha ao listar', details: {} } as NormalizedHttpError;

describe('CarteirasListPageComponent', () => {
  let fixture: ComponentFixture<CarteirasListPageComponent>;
  let service: { listar: ReturnType<typeof vi.fn> };
  let closed: Subject<CarteiraResponse | undefined>;
  let dialog: { open: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = { listar: vi.fn().mockReturnValue(of([CARTEIRA])) }; closed = new Subject(); dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => closed }) }; toast = { show: vi.fn() };
    await TestBed.configureTestingModule({ imports: [CarteirasListPageComponent], providers: [provideRouter([]), { provide: CarteirasService, useValue: service }, { provide: MatDialog, useValue: dialog }, { provide: SuccessToastService, useValue: toast }] }).compileComponents();
    fixture = TestBed.createComponent(CarteirasListPageComponent); fixture.detectChanges();
  });

  it('faz GET único, mantém ordem e apresenta nome, data e detalhe acessível', () => { expect(service.listar).toHaveBeenCalledTimes(1); expect(fixture.nativeElement.textContent).toContain('Carteira principal'); expect(fixture.nativeElement.textContent).toMatch(/31\/08\/2026 às \d{2}:\d{2}/); expect(fixture.nativeElement.querySelector('[aria-label="Ver detalhes da carteira Carteira principal"]')).toBeTruthy(); });
  it('diferencia loading, empty e erro com retry', () => { const pending = new Subject<CarteiraResponse[]>(); service.listar.mockReturnValueOnce(pending); (fixture.componentInstance as any).load(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Carregando carteiras'); pending.next([]); pending.complete(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Você ainda não possui carteiras'); service.listar.mockReturnValueOnce(throwError(() => failure)); (fixture.componentInstance as any).load(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Falha ao listar'); (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); expect(service.listar).toHaveBeenCalledTimes(3); });
  it('abre cadastro sem HTTP e incorpora DTO sem GET redundante', () => { const button = [...fixture.nativeElement.querySelectorAll('button')].find((item: HTMLButtonElement) => item.textContent?.includes('Nova carteira')) as HTMLButtonElement; button.click(); expect(dialog.open).toHaveBeenCalledWith(CarteiraFormPageComponent, expect.objectContaining({ data: { mode: 'create' }, restoreFocus: true })); const created = { ...CARTEIRA, id: 5, nome: 'Nova' }; closed.next(created); closed.complete(); fixture.detectChanges(); expect(service.listar).toHaveBeenCalledTimes(1); expect(fixture.nativeElement.textContent).toContain('Nova'); expect(toast.show).toHaveBeenCalledWith('Carteira cadastrada com sucesso.'); });
  it('cancelar o cadastro não muda lista nem dispara novo GET', () => { (fixture.componentInstance as any).openCreateDialog(); closed.next(undefined); closed.complete(); expect(service.listar).toHaveBeenCalledTimes(1); expect(toast.show).not.toHaveBeenCalled(); });
});
