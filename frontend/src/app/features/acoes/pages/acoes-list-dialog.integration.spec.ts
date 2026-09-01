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
    await TestBed.configureTestingModule({
      imports: [AcoesListPageComponent],
      providers: [provideRouter([]), { provide: AcoesService, useValue: service }]
    }).compileComponents();
    overlay = TestBed.inject(OverlayContainer);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(AcoesListPageComponent);
    fixture.detectChanges();
  });

  afterEach(() => overlay.getContainerElement().replaceChildren());

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
    expect((inputs[0] as HTMLInputElement).value).toBe('XPTO');
    expect(overlay.getContainerElement().querySelector('mat-select')?.textContent).toContain('EUA');
    expect(service.criar).not.toHaveBeenCalled();
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
    expect(overlay.getContainerElement().querySelectorAll('.cdk-overlay-pane')).toHaveLength(1);
  });
});
