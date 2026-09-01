import { OverlayContainer } from '@angular/cdk/overlay';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { CarteirasService } from '../carteiras.service';
import { CarteiraResponse } from '../models/carteira';
import { CarteiraDetailPageComponent } from './carteira-detail-page.component';
import { CarteirasListPageComponent } from './carteiras-list-page.component';

const CARTEIRA: CarteiraResponse = { id: 6, nome: 'Inicial', dataCriacao: '2026-08-31T12:00:00Z' };
const waitForDialogClose = () => new Promise((resolve) => setTimeout(resolve, 250));

describe('Carteiras com MatDialog real', () => {
  let overlay: OverlayContainer;
  afterEach(() => { overlay?.getContainerElement().replaceChildren(); TestBed.resetTestingModule(); });

  it('cadastra pelo CTA e afterClosed real com POST único, sem novo GET', async () => {
    const service = { listar: vi.fn().mockReturnValue(of([])), cadastrar: vi.fn().mockReturnValue(of(CARTEIRA)), buscarPorId: vi.fn(), atualizar: vi.fn(), excluir: vi.fn() };
    await TestBed.configureTestingModule({ imports: [CarteirasListPageComponent], providers: [provideRouter([]), { provide: CarteirasService, useValue: service }] }).compileComponents();
    overlay = TestBed.inject(OverlayContainer); const fixture = TestBed.createComponent(CarteirasListPageComponent); fixture.detectChanges();
    const create = [...fixture.nativeElement.querySelectorAll('button')].find((button: HTMLButtonElement) => button.textContent?.includes('Nova carteira')) as HTMLButtonElement; create.click(); fixture.detectChanges();
    const input = overlay.getContainerElement().querySelector('input') as HTMLInputElement; input.value = 'Inicial'; input.dispatchEvent(new Event('input')); (overlay.getContainerElement().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit')); await waitForDialogClose(); fixture.detectChanges();
    expect(service.cadastrar).toHaveBeenCalledOnce(); expect(service.cadastrar).toHaveBeenCalledWith({ nome: 'Inicial' }); expect(service.listar).toHaveBeenCalledTimes(1); expect(fixture.nativeElement.textContent).toContain('Inicial');
  });

  it('cancela edição real sem PATCH e confirma exclusão real com um DELETE', async () => {
    const service = { listar: vi.fn(), cadastrar: vi.fn(), buscarPorId: vi.fn(), atualizar: vi.fn().mockReturnValue(of(CARTEIRA)), excluir: vi.fn().mockReturnValue(of(undefined)) }; const toast = { show: vi.fn() };
    await TestBed.configureTestingModule({ imports: [CarteiraDetailPageComponent], providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '6' } } } }, { provide: CarteirasService, useValue: service }, { provide: SuccessToastService, useValue: toast }] }).compileComponents();
    overlay = TestBed.inject(OverlayContainer); const router = TestBed.inject(Router); vi.spyOn(router, 'currentNavigation').mockReturnValue({ extras: { info: { carteira: CARTEIRA } } } as never); vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture: ComponentFixture<CarteiraDetailPageComponent> = TestBed.createComponent(CarteiraDetailPageComponent); fixture.detectChanges();
    (fixture.nativeElement.querySelectorAll('button')[0] as HTMLButtonElement).click(); fixture.detectChanges(); const cancel = [...overlay.getContainerElement().querySelectorAll('button')].find((button) => button.textContent?.trim() === 'Cancelar') as HTMLButtonElement; cancel.click(); await waitForDialogClose(); expect(service.atualizar).not.toHaveBeenCalled();
    (fixture.nativeElement.querySelectorAll('button')[1] as HTMLButtonElement).click(); fixture.detectChanges(); const exclude = [...overlay.getContainerElement().querySelectorAll('button')].find((button) => button.textContent?.trim() === 'Excluir') as HTMLButtonElement; exclude.click(); await waitForDialogClose();
    expect(service.excluir).toHaveBeenCalledOnce(); expect(service.excluir).toHaveBeenCalledWith(6); expect(router.navigate).toHaveBeenCalledWith(['/carteiras']);
  });
});
