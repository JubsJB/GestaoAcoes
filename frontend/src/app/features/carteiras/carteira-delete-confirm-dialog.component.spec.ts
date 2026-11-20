import { CARTEIRA_STORAGE } from '../../core/carteira/carteira-context.service';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef, MatDialogState } from '@angular/material/dialog';
import { OverlayContainer } from '@angular/cdk/overlay';
import { of, Subject, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { normalizeHttpError } from '../../core/http/http-error-normalizer';

import { CarteiraDeleteConfirmDialogComponent } from './carteira-delete-confirm-dialog.component';
import { CarteirasService } from './carteiras.service';
import { CarteiraResponse } from './models/carteira';

const CARTEIRA: CarteiraResponse = { id: 8, nome: 'Reserva', dataCriacao: '2026-08-31T12:00:00Z' };
const failure = (status: number, code: string | null) => normalizeHttpError(new HttpErrorResponse({ status, error: {
  timeStamp: 1, status, error: 'Erro', message: 'Mensagem original do backend', path: '/carteiras/8', code, details: { carteiraId: 8 }
} }));

describe('CarteiraDeleteConfirmDialogComponent', () => {
  let fixture: ComponentFixture<CarteiraDeleteConfirmDialogComponent>;
  let service: { excluir: ReturnType<typeof vi.fn> };
  let ref: { close: ReturnType<typeof vi.fn>; getState: ReturnType<typeof vi.fn> };
  beforeEach(async () => { service = { excluir: vi.fn().mockReturnValue(of(undefined)) }; ref = { close: vi.fn(), getState: vi.fn(() => MatDialogState.OPEN) }; await TestBed.configureTestingModule({ imports: [CarteiraDeleteConfirmDialogComponent], providers: [{ provide: CARTEIRA_STORAGE, useValue: null },{ provide: CarteirasService, useValue: service }, { provide: MAT_DIALOG_DATA, useValue: { ...CARTEIRA } }, { provide: MatDialogRef, useValue: ref }] }).compileComponents(); fixture = TestBed.createComponent(CarteiraDeleteConfirmDialogComponent); fixture.detectChanges(); });

  it('apresenta a confirmação aprovada com nome integral e ação destrutiva explícita', () => {
    const name = 'Planejamento patrimonial familiar — investimentos no Brasil e no exterior';
    fixture.destroy();
    TestBed.inject(MAT_DIALOG_DATA).nome = name;
    fixture = TestBed.createComponent(CarteiraDeleteConfirmDialogComponent);
    fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('h2')?.textContent?.trim()).toBe('Excluir carteira');
    expect(root.textContent).toContain('Confirme a exclusão');
    expect(root.textContent).toContain('Deseja excluir esta carteira?');
    expect(root.querySelector('dd')?.textContent?.trim()).toBe(name);
    expect(Array.from(root.querySelectorAll('button')).map(button => button.textContent?.trim())).toEqual(['Cancelar', 'Excluir carteira']);
    expect(service.excluir).not.toHaveBeenCalled();
  });

  it('identifica a carteira e não exclui na abertura ou cancelamento', () => { expect(fixture.nativeElement.textContent).toContain('Reserva'); expect(service.excluir).not.toHaveBeenCalled(); (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); expect(ref.close).toHaveBeenCalledWith(false); expect(service.excluir).not.toHaveBeenCalled(); });
  it('envia um único DELETE após confirmação e bloqueia concorrência', () => { const pending = new Subject<void>(); service.excluir.mockReturnValueOnce(pending); const buttons = fixture.nativeElement.querySelectorAll('button'); buttons[1].click(); buttons[1].click(); expect(service.excluir).toHaveBeenCalledTimes(1); expect(service.excluir).toHaveBeenCalledWith(8); pending.next(); pending.complete(); expect(ref.close).toHaveBeenCalledWith(true); });
  it.each([
    ['CARTEIRA_POSSUI_OPERACOES', 'Operações vinculadas', 'Esta carteira possui operações registradas e não pode ser excluída.'],
    ['CARTEIRA_POSSUI_SNAPSHOTS', 'Snapshots vinculados', 'Esta carteira possui registros de evolução patrimonial (snapshots) e não pode ser excluída.'],
    ['CARTEIRA_POSSUI_MOVIMENTACOES_CUSTODIA', 'Histórico de custódia', 'Esta carteira possui histórico de custódia e não pode ser excluída.']
  ])('apresenta %s como impedimento, sem detalhes técnicos ou nova exclusão', (code, subtitle, message) => {
    const error = failure(409, code);
    Object.freeze(error.details); Object.freeze(error);
    service.excluir.mockReturnValueOnce(throwError(() => error));
    fixture.nativeElement.querySelectorAll('button')[1].click(); fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('h2')?.textContent?.trim()).toBe('Não é possível excluir');
    expect(root.textContent).toContain(subtitle); expect(root.textContent).toContain(message);
    expect(root.querySelector('dd')?.textContent?.trim()).toBe('Reserva');
    expect(root.textContent).not.toContain('carteiraId'); expect(root.textContent).not.toContain('Mensagem original do backend');
    expect(Array.from(root.querySelectorAll('button')).map(button => button.textContent?.trim())).toEqual(['Entendi']);
    root.querySelector('button')!.click();
    expect(ref.close).toHaveBeenCalledWith(false); expect(service.excluir).toHaveBeenCalledTimes(1);
    expect(error.details).toEqual({ carteiraId: 8 });
  });

  it.each([
    [409, 'CONFLITO_TRANSACIONAL_TRANSITORIO', 'Um conflito temporário impediu a exclusão. Você pode tentar novamente.'],
    [403, 'CSRF_INVALIDO', 'Não foi possível validar a solicitação. Tente novamente.']
  ])('permite repetir %s %s somente por nova confirmação explícita', (status, code, message) => {
    service.excluir.mockReturnValueOnce(throwError(() => failure(Number(status), String(code))));
    fixture.nativeElement.querySelectorAll('button')[1].click(); fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain(message);
    expect(root.textContent).not.toContain('carteiraId');
    expect(service.excluir).toHaveBeenCalledTimes(1);
    const retry = Array.from(root.querySelectorAll('button')).find(button => button.textContent?.trim() === 'Tentar excluir novamente');
    expect(retry).toBeTruthy();
    retry!.click();
    expect(service.excluir).toHaveBeenCalledTimes(2); expect(ref.close).toHaveBeenCalledWith(true);
  });

  it.each([
    new HttpErrorResponse({ status: 0, error: 'Falha de rede ou timeout' }),
    new HttpErrorResponse({ status: 502, error: 'Gateway indisponível' }),
    new HttpErrorResponse({ status: 500, error: { timeStamp: 1, status: 500, error: 'Erro', message: 'Falha interna', path: '/carteiras/8', code: null, details: { carteiraId: 8 } } })
  ])('não permite repetir resultado incerto de HTTP $status sem verificar a carteira', response => {
    service.excluir.mockReturnValueOnce(throwError(() => normalizeHttpError(response)));
    fixture.nativeElement.querySelectorAll('button')[1].click(); fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('Não foi possível confirmar o resultado da exclusão.');
    expect(root.textContent).toContain('verifique o estado atual da carteira antes de tentar novamente');
    expect(root.textContent).not.toContain('carteiraId'); expect(root.textContent).not.toContain('Falha interna');
    expect(Array.from(root.querySelectorAll('button')).map(button => button.textContent?.trim())).toEqual(['Entendi']);
    root.querySelector('button')!.click();
    expect(ref.close).toHaveBeenCalledWith(false); expect(service.excluir).toHaveBeenCalledTimes(1);
  });

  it.each([
    [401, null, 'Sessão não confirmada'],
    [403, 'ACESSO_NEGADO', 'Exclusão não autorizada'],
    [503, 'SEGURANCA_INDISPONIVEL', 'Verificação de sessão indisponível'],
    [404, null, 'Carteira não encontrada'],
    [400, 'REQUEST_INVALIDO', 'Solicitação não processada'],
    [400, 'CARTEIRA_POSSUI_OPERACOES', 'Solicitação não processada'],
    [409, 'INTEGRIDADE_DADOS_VIOLADA', 'Não foi possível excluir'],
    [409, 'CODIGO_DESCONHECIDO', 'Não foi possível excluir']
  ])('preserva o fechamento seguro para %s %s sem inventar impedimento ou expor detalhes', (status, code, title) => {
    service.excluir.mockReturnValueOnce(throwError(() => failure(Number(status), code as string | null)));
    fixture.nativeElement.querySelectorAll('button')[1].click(); fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('h2')?.textContent?.trim()).toBe(title);
    expect(root.textContent).not.toContain('carteiraId');
    expect(root.textContent).not.toContain('Mensagem original do backend');
    expect(root.querySelectorAll('button')).toHaveLength(1);
    root.querySelector('button')!.click();
    expect(ref.close).toHaveBeenCalledWith(false); expect(service.excluir).toHaveBeenCalledTimes(1);
  });

  it.each([
    [failure(409, 'CARTEIRA_POSSUI_OPERACOES'), 'Entendi'],
    [failure(409, 'CONFLITO_TRANSACIONAL_TRANSITORIO'), 'Cancelar'],
    [normalizeHttpError(new HttpErrorResponse({ status: 0 })), 'Entendi']
  ])('move o foco para uma ação segura ao apresentar o erro $code', async (error, action) => {
    service.excluir.mockReturnValueOnce(throwError(() => error));
    const submit: HTMLButtonElement = fixture.nativeElement.querySelectorAll('button')[1];
    submit.focus(); submit.click(); fixture.detectChanges(); TestBed.tick(); await fixture.whenStable();
    expect(ref.getState).toHaveBeenCalled();
    expect(document.activeElement?.textContent?.trim()).toBe(action);
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent?.trim()).toBeTruthy();
  });

  it('não disputa o foco quando o diálogo está fechando', async () => {
    ref.getState.mockReturnValue(MatDialogState.CLOSING);
    const outside = document.createElement('button'); document.body.append(outside); outside.focus();
    try {
      service.excluir.mockReturnValueOnce(throwError(() => failure(409, 'CARTEIRA_POSSUI_OPERACOES')));
      fixture.nativeElement.querySelectorAll('button')[1].click(); fixture.detectChanges(); TestBed.tick(); await fixture.whenStable();
      expect(document.activeElement).toBe(outside);
    } finally { outside.remove(); }
  });

  it('mantém Escape e restauração ao acionador após impedimento com MatDialog real', async () => {
    fixture.destroy();
    const trigger = document.createElement('button'); trigger.textContent = 'Abrir confirmação'; document.body.append(trigger); trigger.focus();
    const dialog = TestBed.inject(MatDialog);
    const overlay = TestBed.inject(OverlayContainer).getContainerElement();
    service.excluir.mockReturnValueOnce(throwError(() => failure(409, 'CARTEIRA_POSSUI_OPERACOES')));
    const opened = dialog.open(CarteiraDeleteConfirmDialogComponent, { data: { ...CARTEIRA }, autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'app-dialog--compact' });
    const closed = vi.fn(); opened.afterClosed().subscribe(closed);
    try {
      TestBed.tick();
      overlay.querySelectorAll('button')[1].click(); TestBed.tick();
      expect(document.activeElement?.textContent?.trim()).toBe('Entendi');
      document.activeElement!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', keyCode: 27, bubbles: true }));
      await vi.waitFor(() => { expect(closed).toHaveBeenCalledWith(undefined); expect(document.activeElement).toBe(trigger); });
      expect(service.excluir).toHaveBeenCalledTimes(1);
    } finally { opened.close(); trigger.remove(); }
  });
});
