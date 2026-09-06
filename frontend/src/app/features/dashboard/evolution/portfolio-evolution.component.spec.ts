import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { EvolucaoPatrimonialResponse, SnapshotCarteiraResponse } from './evolution.models';
import { EvolutionService } from './evolution.service';
import { formatLocalDateTime, PortfolioEvolutionComponent } from './portfolio-evolution.component';

const EMPTY: EvolucaoPatrimonialResponse = { carteiraId: 1, pontos: [] };
const DATA: EvolucaoPatrimonialResponse = { carteiraId: 1, pontos: [
  { snapshotId: 1, dataHoraSnapshot: '2026-09-04T10:00:00.001Z', patrimonios: [{ moeda: 'BRL', patrimonioAtual: '100.123456789012' }] },
  { snapshotId: 2, dataHoraSnapshot: '2026-09-04T10:00:00.002Z', patrimonios: [{ moeda: 'USD', patrimonioAtual: '20.500000000000' }] },
  { snapshotId: 3, dataHoraSnapshot: '2026-09-04T10:00:00.003Z', patrimonios: [{ moeda: 'BRL', patrimonioAtual: '110.000000000000' }] },
  { snapshotId: 4, dataHoraSnapshot: '2026-09-04T10:00:00.004Z', patrimonios: [] }
] };
const SNAPSHOT: SnapshotCarteiraResponse = { id: 5, carteiraId: 1, dataHoraSnapshot: '2026-09-04T10:00:00Z', patrimonios: [] };
const ERROR = { kind: 'standard', status: 409, code: 'SNAPSHOT_CARTEIRA_DUPLICADO', message: 'Snapshot duplicado', details: {}, standardError: null, originalError: null } as unknown as NormalizedHttpError;

describe('PortfolioEvolutionComponent', () => {
  let service: { consultar: ReturnType<typeof vi.fn>; registrarSnapshot: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    service = { consultar: vi.fn().mockReturnValue(of(EMPTY)), registrarSnapshot: vi.fn().mockReturnValue(of(SNAPSHOT)) };
    toast = { show: vi.fn() };
  });

  async function create(data: EvolucaoPatrimonialResponse = EMPTY) {
    if (!service.consultar.getMockImplementation()) service.consultar.mockReturnValue(of(data));
    else if (service.consultar.mock.results.length === 0) service.consultar.mockReturnValue(of(data));
    await TestBed.configureTestingModule({ imports: [PortfolioEvolutionComponent], providers: [
      { provide: EvolutionService, useValue: service }, { provide: SuccessToastService, useValue: toast }
    ] }).compileComponents();
    const fixture = TestBed.createComponent(PortfolioEvolutionComponent);
    fixture.componentRef.setInput('carteiraId', 1);
    fixture.componentRef.setInput('carteiraNome', 'Principal');
    service.consultar.mockReturnValueOnce(of(data));
    fixture.detectChanges();
    return fixture;
  }

  it('apresenta loading anunciado e empty state sem tratar zero snapshots como erro', async () => {
    const pending = new Subject<EvolucaoPatrimonialResponse>();
    service.consultar.mockReturnValueOnce(pending);
    const fixture = await create();
    expect(fixture.nativeElement.querySelector('[role="status"]')?.textContent).toContain('Carregando');
    pending.next(EMPTY); pending.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nenhum snapshot registrado');
  });

  it('renderiza moedas separadas, gaps e histórico lossless incluindo snapshot vazio', async () => {
    const fixture = await create(DATA);
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Evolução patrimonial — BRL');
    expect(text).toContain('Evolução patrimonial — USD');
    expect(fixture.nativeElement.querySelectorAll('svg')).toHaveLength(2);
    expect(fixture.nativeElement.querySelectorAll('path.series-line--brl')).toHaveLength(0);
    expect(text).toContain('R$ 100,12');
    expect(text).toContain('US$ 20,50');
    expect(text).toContain('Observação sem patrimônio por moeda');
    expect(text).not.toContain('Patrimônio total');
  });

  it('trata uma observação como marcador isolado sem tendência', async () => {
    const fixture = await create({ carteiraId: 1, pontos: [DATA.pontos[0]] });
    expect(fixture.nativeElement.textContent).toContain('somente uma observação histórica');
    expect(fixture.nativeElement.querySelectorAll('circle')).toHaveLength(1);
    expect(fixture.nativeElement.querySelectorAll('path.series-line')).toHaveLength(0);
  });

  it('expõe SVG complementar, histórico e ponto focável com valor autoritativo', async () => {
    const fixture = await create({ carteiraId: 1, pontos: [DATA.pontos[0]] });
    const point = fixture.nativeElement.querySelector('g.point') as SVGGElement;
    expect(fixture.nativeElement.querySelector('svg').getAttribute('role')).toBe('img');
    expect(fixture.nativeElement.querySelector('#evolution-history-title')).toBeTruthy();
    expect(point.getAttribute('tabindex')).toBe('0');
    expect(point.getAttribute('aria-label')).toContain('R$ 100,12');
    point.dispatchEvent(new FocusEvent('focus')); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.tooltip')?.textContent).toContain('R$ 100,12');
  });

  it('mantém semântica, nomes não cromáticos e breakpoints responsivos estruturais', async () => {
    const fixture = await create(DATA);
    expect(fixture.nativeElement.querySelector('h2')?.textContent).toContain('Evolução patrimonial');
    expect(fixture.nativeElement.textContent).toContain('R$ · linha contínua');
    expect(fixture.nativeElement.textContent).toContain('US$ · linha tracejada');
    expect(fixture.nativeElement.querySelector('ol')).toBeTruthy();
    const styles = ((PortfolioEvolutionComponent as any).ɵcmp.styles as string[]).join('');
    expect(styles).toContain('@media (max-width: 70rem)');
    expect(styles).toContain('@media (max-width: 48rem)');
    expect(styles).toContain('@media (max-width: 36rem)');
    expect(fixture.nativeElement.querySelector('svg').getAttribute('viewBox')).toBe('0 0 720 260');
  });

  it('formata instantes próximos com milissegundos e preserva fallback inválido', () => {
    expect(formatLocalDateTime('2026-09-04T10:00:00.001Z')).not.toBe(formatLocalDateTime('2026-09-04T10:00:00.002Z'));
    expect(formatLocalDateTime('timestamp-inválido')).toBe('timestamp-inválido');
  });

  it('registra somente por ação explícita, bloqueia double-submit e recarrega após sucesso', async () => {
    const post = new Subject<SnapshotCarteiraResponse>();
    service.registrarSnapshot.mockReturnValue(post);
    const fixture = await create();
    expect(service.registrarSnapshot).not.toHaveBeenCalled();
    const button = Array.from(fixture.nativeElement.querySelectorAll('button')).find((item: any) => item.textContent.includes('Registrar snapshot')) as HTMLButtonElement;
    button.click(); button.click(); fixture.detectChanges();
    expect(service.registrarSnapshot).toHaveBeenCalledTimes(1);
    expect(button.disabled).toBe(true);
    post.next(SNAPSHOT); post.complete(); fixture.detectChanges();
    expect(toast.show).toHaveBeenCalledWith(expect.stringContaining('Principal'));
    expect(service.consultar).toHaveBeenCalledTimes(2);
  });

  it('não contamina novo contexto após POST da carteira anterior', async () => {
    const post = new Subject<SnapshotCarteiraResponse>(); service.registrarSnapshot.mockReturnValue(post);
    const fixture = await create();
    (Array.from(fixture.nativeElement.querySelectorAll('button')).find((item: any) => item.textContent.includes('Registrar snapshot')) as HTMLButtonElement).click();
    fixture.componentRef.setInput('carteiraId', 2); fixture.componentRef.setInput('carteiraNome', 'Exterior'); fixture.detectChanges();
    post.next(SNAPSHOT); post.complete(); fixture.detectChanges();
    expect(toast.show).not.toHaveBeenCalled();
    expect(service.consultar).toHaveBeenCalledTimes(2);
  });

  it('invalida imediatamente e cancela GET stale na troca de carteira', async () => {
    const first = new Subject<EvolucaoPatrimonialResponse>();
    service.consultar.mockReturnValueOnce(first).mockReturnValueOnce(of({ carteiraId: 2, pontos: [] }));
    const fixture = await create();
    fixture.componentRef.setInput('carteiraId', 2); fixture.detectChanges();
    first.next(DATA); first.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('R$ 100,12');
    expect(fixture.nativeElement.textContent).toContain('Nenhum snapshot registrado');
  });

  it('oferece retry manual sem retry automático e apresenta erro POST', async () => {
    service.consultar.mockReturnValueOnce(throwError(() => ERROR));
    const fixture = await create(); fixture.detectChanges();
    expect(service.consultar).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Snapshot duplicado');
    service.consultar.mockReturnValueOnce(of(EMPTY));
    const retry = Array.from(fixture.nativeElement.querySelectorAll('button')).find((item: any) => item.textContent.includes('Tentar novamente')) as HTMLButtonElement;
    retry.click(); fixture.detectChanges();
    expect(service.consultar).toHaveBeenCalledTimes(2);
    service.registrarSnapshot.mockReturnValue(throwError(() => ERROR));
    (Array.from(fixture.nativeElement.querySelectorAll('button')).find((item: any) => item.textContent.includes('Registrar snapshot')) as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Snapshot duplicado');
    expect(service.consultar).toHaveBeenCalledTimes(2);
    expect((Array.from(fixture.nativeElement.querySelectorAll('button')).find((item: any) => item.textContent.includes('Registrar snapshot')) as HTMLButtonElement).disabled).toBe(false);
  });
});
