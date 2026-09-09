import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { EvolucaoPatrimonialResponse, SnapshotCarteiraResponse } from './evolution.models';
import { EvolutionService } from './evolution.service';
import { buildEvolutionGeometry, segmentPath } from './evolution-geometry';
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
    expect(fixture.nativeElement.textContent).toContain('Nenhum registro de patrimônio');
  });

  it('renderiza moedas separadas, gaps e histórico lossless incluindo snapshot vazio', async () => {
    const fixture = await create(DATA);
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Histórico do patrimônio — BRL');
    expect(text).toContain('Histórico do patrimônio — USD');
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
    expect(fixture.nativeElement.querySelector('svg').getAttribute('role')).toBe('group');
    expect(fixture.nativeElement.querySelector('#evolution-history-title')).toBeTruthy();
    expect(point.getAttribute('tabindex')).toBe('0');
    expect(point.getAttribute('aria-label')).toContain('R$ 100,12');
    point.dispatchEvent(new FocusEvent('focus')); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.tooltip')?.textContent).toContain('R$ 100,12');
  });

  it('mantém semântica, nomes não cromáticos e breakpoints responsivos estruturais', async () => {
    const fixture = await create(DATA);
    expect(fixture.nativeElement.querySelector('h2')?.textContent).toContain('Histórico do patrimônio');
    expect(fixture.nativeElement.textContent).toContain('Os registros são realizados manualmente e preservados separadamente por moeda.');
    expect(fixture.nativeElement.querySelector('.evolution__timezone')?.textContent).toContain('horário local');
    expect(fixture.nativeElement.querySelector('button[mat-stroked-button]')?.textContent).toContain('Registrar patrimônio atual');
    expect(fixture.nativeElement.textContent).not.toMatch(/snapshot/i);
    expect(fixture.nativeElement.textContent).toContain('R$ · linha contínua');
    expect(fixture.nativeElement.textContent).toContain('US$ · linha tracejada');
    expect(fixture.nativeElement.querySelector('ol')).toBeTruthy();
    const styles = ((PortfolioEvolutionComponent as any).ɵcmp.styles as string[]).join('');
    expect(styles).toContain('@media (max-width: 70rem)');
    expect(styles).toContain('@media (max-width: 48rem)');
    expect(styles).toContain('@media (max-width: 36rem)');
    expect(styles).toContain('white-space: nowrap');
    expect(styles).toContain('width: max-content');
    expect(styles).toMatch(/@container \(max-width:\s*36rem\)/);
    expect(styles).toMatch(/@container[^}]+\.charts[^}]+grid-template-columns: 1fr/);
    expect(styles.split('@media')[0]).toContain('grid-template-columns: repeat(auto-fit, minmax(min(100%, 22rem), 1fr))');
    expect(fixture.nativeElement.querySelectorAll('.charts > .chart')).toHaveLength(2);
    expect(fixture.nativeElement.querySelectorAll('.history')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('svg').getAttribute('viewBox')).toBe('0 0 720 260');
  });

  it('apresenta somente minutos locais e preserva fallback inválido', () => {
    expect(formatLocalDateTime('2026-09-07T18:17:26.957')).toBe('07/09/2026, 18:17 (horário local)');
    expect(formatLocalDateTime('2026-09-04T10:00:00.001Z')).toBe(formatLocalDateTime('2026-09-04T10:00:00.002Z'));
    expect(formatLocalDateTime('timestamp-inválido')).toBe('timestamp-inválido');
  });

  it('compartilha horário local entre tooltip, registro e descrição sem alterar timestamps ou eixo compacto', async () => {
    const data = structuredClone(DATA);
    data.pontos[0].dataHoraSnapshot = '2026-09-07T18:17:26.957';
    const original = JSON.stringify(data);
    const fixture = await create(data);
    const root = fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('.history-point')!.click(); fixture.detectChanges();
    const expected = '07/09/2026, 18:17 (horário local)';
    expect(root.querySelector('.tooltip time')?.textContent).toBe(expected);
    expect(root.querySelector('.history time')?.textContent).toBe(expected);
    expect(root.querySelector('.history-inspection')?.textContent).toContain(expected);
    expect(root.querySelector('g.point')?.getAttribute('aria-label')).toContain(expected);
    expect(root.querySelector('.history-point')?.getAttribute('aria-label')).toContain(expected);
    expect(root.textContent).not.toMatch(/18:17:26|957/);
    expect(root.querySelector('.axis-labels span')?.getAttribute('data-full')).toBe('07/09, 18:17');
    expect(root.querySelector('.tooltip time')?.getAttribute('datetime')).toBe(data.pontos[0].dataHoraSnapshot);
    expect(root.querySelector('.history time')?.getAttribute('datetime')).toBe(data.pontos[0].dataHoraSnapshot);
    expect(JSON.stringify(data)).toBe(original);
    expect(root.querySelectorAll('svg')).toHaveLength(2);
    expect(root.querySelectorAll('g.point')).toHaveLength(3);
    expect(root.querySelectorAll('.history li')).toHaveLength(4);
  });

  it.each([1200, 768, 390, 320, 160])('mede extremos em %i px, abrevia sem quebra e restaura ao ampliar', async width => {
    const callbacks: (() => void)[] = [];
    const resize = () => callbacks.forEach(callback => callback());
    vi.stubGlobal('ResizeObserver', class {
      constructor(callback: () => void) { callbacks.push(callback); }
      observe() {} disconnect() {}
    });
    try {
      const fixture = await create(DATA);
      const root = fixture.nativeElement as HTMLElement;
      const axis = root.querySelector<HTMLElement>('.axis-labels')!;
      const spans = Array.from(axis.querySelectorAll('span'));
      const rect = (left: number, size: number) => ({ left, right: left + size, width: size, height: 24 } as DOMRect);
      vi.spyOn(axis, 'getBoundingClientRect').mockImplementation(() => rect(0, width));
      spans.forEach((span, index) => {
        span.dataset['full'] = '07/09/2026, 18:17:00.001';
        span.dataset['compact'] = '07/09';
        vi.spyOn(span, 'getBoundingClientRect').mockImplementation(() =>
          rect(index === 0 ? -8 : width - 4, span.textContent === '07/09' ? 50 : 240));
      });
      resize();
      expect(spans.map(span => span.style.visibility)).toEqual(['visible', 'visible']);
      expect(spans[0].style.translate).toBe('8px');
      expect(spans[1].textContent).toBe(width < 488 ? '07/09' : '07/09/2026, 18:17:00.001');
      expect(root.querySelectorAll('.history')).toHaveLength(1);
      expect(root.querySelectorAll('.history li')).toHaveLength(DATA.pontos.length);
      expect(root.querySelectorAll('g.point')).toHaveLength(3);
      width = 1200;
      resize();
      expect(spans[1].textContent).toBe('07/09/2026, 18:17:00.001');
      expect(service.consultar).toHaveBeenCalledTimes(1);
      expect(service.registrarSnapshot).not.toHaveBeenCalled();
    } finally { vi.unstubAllGlobals(); }
  });

  it('suprime somente labels sobrepostos após resize e mantém pontos, registros e consulta', async () => {
    let resize = () => {};
    vi.stubGlobal('ResizeObserver', class {
      constructor(callback: () => void) { resize = callback; }
      observe() {} disconnect() {}
    });
    try {
      const data: EvolucaoPatrimonialResponse = { carteiraId: 1, pontos: [1, 2, 3].map(snapshotId => ({
        snapshotId, dataHoraSnapshot: `2026-09-0${snapshotId === 3 ? 7 : 5}T14:${snapshotId === 2 ? '25' : '09'}:00Z`,
        patrimonios: [{ moeda: 'BRL', patrimonioAtual: '100' }]
      })) };
      const fixture = await create(data);
      const root = fixture.nativeElement as HTMLElement;
      const axis = root.querySelector<HTMLElement>('.axis-labels')!;
      const spans = axis.querySelectorAll('span');
      const rect = (left: number, right: number) => ({ left, right, width: right - left, height: 24 } as DOMRect);
      vi.spyOn(axis, 'getBoundingClientRect').mockReturnValue(rect(0, 1000));
      [[60, 200], [10, 150], [830, 970]].forEach(([left, right], i) => vi.spyOn(spans[i], 'getBoundingClientRect').mockReturnValue(rect(left, right)));
      resize();
      expect(Array.from(spans).map(span => span.style.visibility)).toEqual(['visible', 'hidden', 'visible']);
      expect(root.querySelectorAll('g.point')).toHaveLength(3);
      expect(root.querySelectorAll('.history li')).toHaveLength(3);
      expect(root.querySelectorAll('.history-point')).toHaveLength(3);
      root.querySelectorAll<HTMLButtonElement>('.history-point')[1].click(); fixture.detectChanges();
      expect(root.querySelector('[role="tooltip"]')?.textContent).toContain('Registro #2');
      expect(service.consultar).toHaveBeenCalledTimes(1);
      expect(service.registrarSnapshot).not.toHaveBeenCalled();
      vi.mocked(spans[1].getBoundingClientRect).mockReturnValue(rect(420, 560));
      resize();
      expect(spans[1].style.visibility).toBe('visible');
    } finally { vi.unstubAllGlobals(); }
  });

  it('mantém consulta por foco ao sair o ponteiro, dispensa com Escape e reabre por Enter/Espaço', async () => {
    const fixture = await create(DATA);
    const root = fixture.nativeElement as HTMLElement;
    const point = root.querySelector('g.point') as SVGGElement;
    point.dispatchEvent(new FocusEvent('focus')); fixture.detectChanges();
    const label = point.getAttribute('aria-label');
    expect(label).toContain('BRL'); expect(label).toContain(formatLocalDateTime(DATA.pontos[0].dataHoraSnapshot));
    root.querySelector('.chart')!.dispatchEvent(new MouseEvent('mouseleave')); fixture.detectChanges();
    expect(root.querySelector('[role="tooltip"]')?.textContent).toContain('R$ 100,12');
    point.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true })); fixture.detectChanges();
    expect(root.querySelector('[role="tooltip"]')).toBeNull();
    for (const key of ['Enter', ' ']) {
      const event = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true });
      point.dispatchEvent(event); fixture.detectChanges();
      expect(event.defaultPrevented).toBe(true);
      expect(root.querySelector('[role="tooltip"]')?.textContent).toContain('R$ 100,12');
      expect(point.getAttribute('aria-describedby')).toBe('tooltip-BRL');
      point.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true })); fixture.detectChanges();
      expect(root.querySelector('[role="tooltip"]')).toBeNull();
    }
    point.dispatchEvent(new MouseEvent('click', { bubbles: true })); fixture.detectChanges();
    expect(root.querySelector('[role="tooltip"]')?.textContent).toContain('Registro #1');
    expect(service.consultar).toHaveBeenCalledTimes(1);
    expect(service.registrarSnapshot).not.toHaveBeenCalled();
  });

  it('permite alcançar o tooltip por ponteiro e não fecha enquanto o foco sustenta a consulta', async () => {
    const fixture = await create(DATA);
    const root = fixture.nativeElement as HTMLElement;
    const point = root.querySelector('g.point')!;
    point.dispatchEvent(new MouseEvent('mouseenter')); fixture.detectChanges();
    point.dispatchEvent(new MouseEvent('mouseleave')); fixture.detectChanges();
    expect(root.querySelector('[role="tooltip"]')).toBeTruthy();
    root.querySelector('.chart')!.dispatchEvent(new MouseEvent('mouseleave')); fixture.detectChanges();
    expect(root.querySelector('[role="tooltip"]')).toBeNull();
  });

  it('preserva pontos coincidentes e oferece controles individuais no histórico sem perder valores extensos', async () => {
    const data: EvolucaoPatrimonialResponse = { carteiraId: 1, pontos: [1, 2, 3].map(snapshotId => ({
      snapshotId, dataHoraSnapshot: snapshotId === 3 ? '2026-09-04T10:00:00.002Z' : '2026-09-04T10:00:00.001Z',
      patrimonios: [{ moeda: 'USD', patrimonioAtual: '9999999999999.123456789012' }]
    })) };
    const original = JSON.stringify(data);
    const fixture = await create(data);
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelectorAll('svg')).toHaveLength(1);
    expect(root.querySelectorAll('g.point')).toHaveLength(3);
    const markers = root.querySelectorAll('g.point circle');
    expect(markers[0].getAttribute('cx')).toBe(markers[1].getAttribute('cx'));
    expect(markers[0].getAttribute('cy')).toBe(markers[1].getAttribute('cy'));
    const controls = root.querySelectorAll<HTMLButtonElement>('.history-point');
    expect(controls).toHaveLength(3);
    controls[1].click(); fixture.detectChanges();
    expect(root.querySelector('.history-inspection')?.textContent).toContain('Registro #2');
    expect(root.querySelector('[role="tooltip"]')?.textContent).toContain('US$ 9.999.999.999.999,12');
    expect(JSON.stringify(data)).toBe(original);
    expect(root.querySelectorAll('path.series-line--usd')).toHaveLength(1);
    expect(service.registrarSnapshot).not.toHaveBeenCalled();
  });

  it('registra somente por ação explícita, bloqueia double-submit e recarrega após sucesso', async () => {
    const post = new Subject<SnapshotCarteiraResponse>();
    service.registrarSnapshot.mockReturnValue(post);
    const fixture = await create();
    expect(service.registrarSnapshot).not.toHaveBeenCalled();
    const button = Array.from(fixture.nativeElement.querySelectorAll('button')).find((item: any) => item.textContent.includes('Registrar patrimônio atual')) as HTMLButtonElement;
    button.click(); button.click(); fixture.detectChanges();
    expect(service.registrarSnapshot).toHaveBeenCalledTimes(1);
    expect(button.disabled).toBe(true);
    post.next(SNAPSHOT); post.complete(); fixture.detectChanges();
    expect(toast.show).toHaveBeenCalledWith(expect.stringContaining('Principal'));
    expect(toast.show).toHaveBeenCalledWith('Patrimônio de Principal registrado com sucesso.');
    expect(service.consultar).toHaveBeenCalledTimes(2);
  });

  it('não contamina novo contexto após POST da carteira anterior', async () => {
    const post = new Subject<SnapshotCarteiraResponse>(); service.registrarSnapshot.mockReturnValue(post);
    const fixture = await create();
    (Array.from(fixture.nativeElement.querySelectorAll('button')).find((item: any) => item.textContent.includes('Registrar patrimônio atual')) as HTMLButtonElement).click();
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
    expect(fixture.nativeElement.textContent).toContain('Nenhum registro de patrimônio');
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
    (Array.from(fixture.nativeElement.querySelectorAll('button')).find((item: any) => item.textContent.includes('Registrar patrimônio atual')) as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Snapshot duplicado');
    expect(service.consultar).toHaveBeenCalledTimes(2);
    expect((Array.from(fixture.nativeElement.querySelectorAll('button')).find((item: any) => item.textContent.includes('Registrar patrimônio atual')) as HTMLButtonElement).disabled).toBe(false);
  });
  it('refina o grid sem mudar coordenadas, segmentos ou criar séries e chamadas', async () => {
    const data = { carteiraId: 1, pontos: [DATA.pontos[0],
      { ...DATA.pontos[2], snapshotId: 5, dataHoraSnapshot: '2026-09-05T10:00:00Z' },
      DATA.pontos[3],
      { ...DATA.pontos[2], snapshotId: 6, dataHoraSnapshot: '2026-09-06T10:00:00Z' }
    ] };
    const fixture = await create(data);
    const root = fixture.nativeElement as HTMLElement;
    const series = buildEvolutionGeometry(data.pontos, 'BRL')!;
    expect(root.querySelectorAll('.chart-grid[aria-hidden="true"]')).toHaveLength(1);
    expect(root.querySelectorAll('.series-line')).toHaveLength(1);
    expect(root.querySelector('.series-line')?.getAttribute('d')).toBe(segmentPath(series.segments[0]));
    expect(Array.from(root.querySelectorAll('.point circle')).map(p => [p.getAttribute('cx'), p.getAttribute('cy')]))
      .toEqual(series.points.map(p => [String(p.x), String(p.y)]));
    expect(root.querySelectorAll('.history')).toHaveLength(1);
    expect(root.querySelectorAll('.history li')).toHaveLength(4);
    expect(root.querySelectorAll('svg path')).toHaveLength(1);
    expect(service.consultar).toHaveBeenCalledTimes(1);
    expect(service.registrarSnapshot).not.toHaveBeenCalled();
  });

});
