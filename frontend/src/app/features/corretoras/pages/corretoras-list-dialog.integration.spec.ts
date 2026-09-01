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
    await TestBed.configureTestingModule({ imports: [CorretorasListPageComponent], providers: [provideRouter([]), { provide: CorretorasService, useValue: service }] }).compileComponents();
    overlay = TestBed.inject(OverlayContainer);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(CorretorasListPageComponent);
    fixture.detectChanges();
  });

  afterEach(() => overlay.getContainerElement().replaceChildren());

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
