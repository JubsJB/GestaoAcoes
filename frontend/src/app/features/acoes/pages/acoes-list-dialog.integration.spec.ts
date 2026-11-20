import { authenticatedModule } from '../../../../../test-support/authenticated-module';
import { OverlayContainer } from '@angular/cdk/overlay';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { AcoesService } from '../acoes.service';
import { Mercado } from '../models/acao';
import { AcoesListPageComponent } from './acoes-list-page.component';

describe('AcoesListPageComponent com MatDialog real', () => {
  let fixture: ComponentFixture<AcoesListPageComponent>;
  let overlay: OverlayContainer;
  let service: { listar: ReturnType<typeof vi.fn>; buscarPorTickerEMercado: ReturnType<typeof vi.fn>; criar: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = {
      listar: vi.fn().mockReturnValue(of([])),
      criar: vi.fn(),
      buscarPorTickerEMercado: vi.fn().mockReturnValue(throwError(() => ({
        status: 404,
        code: null,
        message: 'Ausente',
        details: {}
      } as NormalizedHttpError)))
    };
    await authenticatedModule({
      imports: [AcoesListPageComponent],
      providers: [provideRouter([]), { provide: AcoesService, useValue: service }]
    }).compileComponents();
    overlay = TestBed.inject(OverlayContainer);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(AcoesListPageComponent);
    fixture.detectChanges();
  });

  afterEach(() => { overlay.getContainerElement().replaceChildren(); vi.restoreAllMocks(); });

  it('returns USER from absence with Escape, restoring search focus without opening registration', async () => {
    fixture.destroy();
    TestBed.resetTestingModule();
    await authenticatedModule({
      imports: [AcoesListPageComponent],
      providers: [provideRouter([]), { provide: AcoesService, useValue: service }]
    }, 'USER').compileComponents();
    overlay = TestBed.inject(OverlayContainer);
    fixture = TestBed.createComponent(AcoesListPageComponent);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'XPTO';
    input.dispatchEvent(new Event('input'));
    const component = fixture.componentInstance as unknown as {
      searchForm: { controls: { mercado: { setValue(value: Mercado): void } } };
    };
    component.searchForm.controls.mercado.setValue('EUA');
    input.focus();
    // jsdom has no layout; supply visibility geometry for CDK's real focus trap.
    vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(100);
    vi.spyOn(HTMLElement.prototype, 'offsetHeight', 'get').mockReturnValue(44);
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit', {bubbles:true, cancelable:true}));
    fixture.detectChanges(); await fixture.whenStable();
    const buttons = [...overlay.getContainerElement().querySelectorAll('button')] as HTMLButtonElement[];
    expect(buttons.map(button => button.textContent?.trim())).toEqual(['Voltar à listagem']);
    await vi.waitFor(() => expect(document.activeElement).toBe(buttons[0]));
    overlay.getContainerElement().querySelector('mat-dialog-container')!
      .dispatchEvent(new KeyboardEvent('keydown', {key:'Escape', keyCode:27, bubbles:true}));
    fixture.detectChanges(); await fixture.whenStable();
    await vi.waitFor(() => expect(overlay.getContainerElement().querySelector('mat-dialog-container')).toBeNull());
    await vi.waitFor(() => expect(document.activeElement).toBe(input));
    expect(service.criar).not.toHaveBeenCalled();
    expect(service.buscarPorTickerEMercado).toHaveBeenCalledTimes(1);
  });

  it('propaga o clique real do CTA e abre o cadastro preenchido sem POST automático', async () => {
    const open = vi.spyOn(TestBed.inject(MatDialog), 'open');
    const component = fixture.componentInstance as unknown as {
      searchForm: { controls: { ticker: { setValue(value: string): void }; mercado: { setValue(value: Mercado): void } } };
      search(): void;
    };
    component.searchForm.controls.ticker.setValue('XPTO');
    component.searchForm.controls.mercado.setValue('EUA');
    component.search();
    fixture.detectChanges();
    const closed = firstValueFrom(open.mock.results[0].value.afterClosed());

    const register = [...overlay.getContainerElement().querySelectorAll('button')]
      .find((button) => button.textContent?.trim() === 'Cadastrar ação') as HTMLButtonElement;
    expect(register).toBeTruthy();
    register.click();
    expect(await closed).toBe(true);
    await fixture.whenStable();

    const inputs = overlay.getContainerElement().querySelectorAll('input');
    expect(inputs).toHaveLength(1);
    expect(overlay.getContainerElement().querySelector('mat-dialog-container')?.getAttribute('aria-labelledby')).toBe('create-title');
    expect(overlay.getContainerElement().querySelector('#create-title')?.textContent).toBe('Cadastrar ação');
    expect((inputs[0] as HTMLInputElement).value).toBe('XPTO');
    expect(overlay.getContainerElement().querySelector('mat-select')?.textContent).toContain('EUA');
    expect(service.criar).not.toHaveBeenCalled();
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
    expect(overlay.getContainerElement().querySelectorAll('.cdk-overlay-pane')).toHaveLength(1);
  });
});
