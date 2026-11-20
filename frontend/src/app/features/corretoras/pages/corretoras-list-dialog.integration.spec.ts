import { authenticatedModule } from '../../../../../test-support/authenticated-module';
import { OverlayContainer } from '@angular/cdk/overlay';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { CorretorasService } from '../corretoras.service';
import { CorretorasListPageComponent } from './corretoras-list-page.component';

describe('CorretorasListPageComponent com MatDialog real', () => {
  let fixture: ComponentFixture<CorretorasListPageComponent>;
  let overlay: OverlayContainer;
  let service: { listar: ReturnType<typeof vi.fn>; buscarPorCnpj: ReturnType<typeof vi.fn>; cadastrar: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = {
      listar: vi.fn().mockReturnValue(of([])),
      cadastrar: vi.fn(),
      buscarPorCnpj: vi.fn().mockReturnValue(throwError(() => ({ status: 404, code: null, message: 'Ausente', details: {} } as unknown as NormalizedHttpError)))
    };
    await authenticatedModule({ imports: [CorretorasListPageComponent], providers: [provideRouter([]), { provide: CorretorasService, useValue: service }] }).compileComponents();
    overlay = TestBed.inject(OverlayContainer);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(CorretorasListPageComponent);
    fixture.detectChanges();
  });

  afterEach(() => { overlay.getContainerElement().replaceChildren(); vi.restoreAllMocks(); });

  it('returns USER from absence with Escape and restores the query focus without registration', async () => {
    fixture.destroy();
    TestBed.resetTestingModule();
    service.listar.mockClear();
    await authenticatedModule({ imports: [CorretorasListPageComponent], providers: [provideRouter([]), {provide:CorretorasService,useValue:service}] }, 'USER').compileComponents();
    overlay = TestBed.inject(OverlayContainer);
    fixture = TestBed.createComponent(CorretorasListPageComponent);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = '11.222.333/0001-81';
    input.dispatchEvent(new Event('input'));
    input.focus();
    // jsdom has no layout; visibility geometry lets CDK exercise its real focus trap.
    vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(100);
    vi.spyOn(HTMLElement.prototype, 'offsetHeight', 'get').mockReturnValue(44);
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}));
    fixture.detectChanges(); await fixture.whenStable();
    const buttons = [...overlay.getContainerElement().querySelectorAll('button')] as HTMLButtonElement[];
    expect(buttons.map(button => button.textContent?.trim())).toEqual(['Voltar à listagem']);
    await vi.waitFor(() => expect(document.activeElement).toBe(buttons[0]));
    overlay.getContainerElement().querySelector('mat-dialog-container')!
      .dispatchEvent(new KeyboardEvent('keydown', {key:'Escape',keyCode:27,bubbles:true}));
    fixture.detectChanges(); await fixture.whenStable();
    await vi.waitFor(() => expect(overlay.getContainerElement().querySelector('mat-dialog-container')).toBeNull());
    await vi.waitFor(() => expect(document.activeElement).toBe(input));
    expect(service.cadastrar).not.toHaveBeenCalled();
    expect(service.buscarPorCnpj).toHaveBeenCalledTimes(1);
    expect(service.listar).toHaveBeenCalledTimes(1);
  });

  it('encadeia o CTA real para o cadastro preenchido sem POST ou overlay órfão', async () => {
    const open = vi.spyOn(TestBed.inject(MatDialog), 'open');
    const component = fixture.componentInstance as unknown as { searchControl: { setValue(value: string): void }; search(): void };
    component.searchControl.setValue('11.222.333/0001-81');
    component.search();
    fixture.detectChanges();
    const closed = firstValueFrom(open.mock.results[0].value.afterClosed());
    const register = [...overlay.getContainerElement().querySelectorAll('button')].find((button) => button.textContent?.trim() === 'Cadastrar corretora') as HTMLButtonElement;
    register.click();
    expect(await closed).toBe(true);
    await fixture.whenStable();
    expect((overlay.getContainerElement().querySelector('input') as HTMLInputElement).value).toBe('11.222.333/0001-81');
    expect(service.cadastrar).not.toHaveBeenCalled();
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
    expect(overlay.getContainerElement().querySelectorAll('.cdk-overlay-pane')).toHaveLength(1);
  });
});
