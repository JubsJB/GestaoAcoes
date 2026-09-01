import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, Subject, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { CarteiraDeleteConfirmDialogComponent } from './carteira-delete-confirm-dialog.component';
import { CarteirasService } from './carteiras.service';
import { CarteiraResponse } from './models/carteira';

const CARTEIRA: CarteiraResponse = { id: 8, nome: 'Reserva', dataCriacao: '2026-08-31T12:00:00Z' };

describe('CarteiraDeleteConfirmDialogComponent', () => {
  let fixture: ComponentFixture<CarteiraDeleteConfirmDialogComponent>;
  let service: { excluir: ReturnType<typeof vi.fn> };
  let ref: { close: ReturnType<typeof vi.fn> };
  beforeEach(async () => { service = { excluir: vi.fn().mockReturnValue(of(undefined)) }; ref = { close: vi.fn() }; await TestBed.configureTestingModule({ imports: [CarteiraDeleteConfirmDialogComponent], providers: [{ provide: CarteirasService, useValue: service }, { provide: MAT_DIALOG_DATA, useValue: CARTEIRA }, { provide: MatDialogRef, useValue: ref }] }).compileComponents(); fixture = TestBed.createComponent(CarteiraDeleteConfirmDialogComponent); fixture.detectChanges(); });

  it('identifica a carteira e não exclui na abertura ou cancelamento', () => { expect(fixture.nativeElement.textContent).toContain('Reserva'); expect(service.excluir).not.toHaveBeenCalled(); (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); expect(ref.close).toHaveBeenCalledWith(false); expect(service.excluir).not.toHaveBeenCalled(); });
  it('envia um único DELETE após confirmação e bloqueia concorrência', () => { const pending = new Subject<void>(); service.excluir.mockReturnValueOnce(pending); const buttons = fixture.nativeElement.querySelectorAll('button'); buttons[1].click(); buttons[1].click(); expect(service.excluir).toHaveBeenCalledTimes(1); expect(service.excluir).toHaveBeenCalledWith(8); pending.next(); pending.complete(); expect(ref.close).toHaveBeenCalledWith(true); });
  it('preserva StandardError e permite nova decisão após falha', () => { const conflict = { status: 409, code: 'CARTEIRA_POSSUI_OPERACOES', message: 'Carteira possui operações', details: { operacoes: 2 } } as unknown as NormalizedHttpError; service.excluir.mockReturnValueOnce(throwError(() => conflict)); fixture.nativeElement.querySelectorAll('button')[1].click(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Carteira possui operações'); expect(ref.close).not.toHaveBeenCalledWith(true); });
});
