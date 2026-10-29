import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { TestKey } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, firstValueFrom, of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { provideApiConfig } from '../../core/config/api.config';
import { CARTEIRA_STORAGE } from '../../core/carteira/carteira-context.service';
import { httpErrorInterceptor } from '../../core/http/http-error.interceptor';
import { SuccessToastService } from '../../shared/success-toast/success-toast.service';
import { AcoesService } from '../acoes/acoes.service';
import { CorretorasService } from '../corretoras/corretoras.service';
import { DashboardService } from '../dashboard/dashboard.service';
import { OperacoesService } from '../operacoes/operacoes.service';
import { CarteirasService } from './carteiras.service';
import { CarteiraDetailPageComponent } from './pages/carteira-detail-page.component';
import { originalCustodia, estornoCustodia } from './testing/custodia.fixtures';

const HISTORICO = '/test-api/carteiras/1/movimentacoes-custodia';
const DISTRIBUICAO = '/test-api/carteiras/1/posicoes/distribuicao-corretoras';
const carteira = { id: 1, nome: 'Principal', dataCriacao: '2026-10-01T12:00:00Z' };

describe('custódia: POST confirmado e recargas independentes com dialogs reais', () => {
  afterEach(() => { try { TestBed.inject(HttpTestingController).verify(); } finally { TestBed.resetTestingModule(); } });
  async function criar() {
    const params = new BehaviorSubject(convertToParamMap({ id: '1' }));
    const operacoes = { listarPorCarteira: vi.fn().mockReturnValue(of([])) };
    await TestBed.configureTestingModule({ imports: [CarteiraDetailPageComponent], providers: [
      provideHttpClient(withInterceptors([httpErrorInterceptor])), provideHttpClientTesting(), provideApiConfig('/test-api'), provideRouter([]),
      { provide: CARTEIRA_STORAGE, useValue: null }, { provide: ActivatedRoute, useValue: { paramMap: params } },
      { provide: CarteirasService, useValue: { buscarPorId: (id: number) => of({ ...carteira, id }), listar: () => of([carteira]) } },
      { provide: OperacoesService, useValue: operacoes }, { provide: DashboardService, useValue: { listarPosicoes: () => of([]) } },
      { provide: AcoesService, useValue: { listar: () => of([{ id: 2, ticker: 'PETR4', mercado: 'BRASIL', moeda: 'BRL' }]) } },
      { provide: CorretorasService, useValue: { listar: () => of([{ id: 3, nomeFantasia: 'Corretora A' }, { id: 4, nomeFantasia: 'Corretora B' }]) } },
      { provide: SuccessToastService, useValue: { show: vi.fn() } }
    ] }).compileComponents();
    const fixture = TestBed.createComponent(CarteiraDetailPageComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(DISTRIBUICAO).flush('{"carteiraId":1,"posicoes":[]}');
    http.expectOne(HISTORICO).flush(JSON.stringify([originalCustodia()])); fixture.detectChanges();
    return { fixture, http, params, operacoes, overlay: TestBed.inject(OverlayContainer).getContainerElement() };
  }
  async function confirmar(r: Awaited<ReturnType<typeof criar>>, tipo: 'TRANSFERENCIA' | 'ATRIBUICAO' | 'ESTORNO') {
    if (tipo === 'ESTORNO') r.fixture.nativeElement.querySelector('button[data-estornar="10"]').click();
    else {
      [...r.fixture.nativeElement.querySelectorAll('button')].find((b: HTMLButtonElement) => b.textContent?.includes('Movimentar custódia')).click();
      r.fixture.detectChanges();
      r.http.expectOne(DISTRIBUICAO).flush('{"carteiraId":1,"posicoes":[]}');
      const loader = TestbedHarnessEnvironment.documentRootLoader(r.fixture);
      if (tipo === 'ATRIBUICAO') await (await loader.getHarness(MatSelectHarness.with({ selector: 'mat-dialog-container [formControlName="natureza"]' }))).clickOptions({ text: 'Atribuição' });
      await (await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="acaoKey"]' }))).clickOptions({ text: 'PETR4 · BRASIL' });
      if (tipo === 'TRANSFERENCIA') await (await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="corretoraOrigemId"]' }))).clickOptions({ text: 'Corretora A' });
      await (await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="corretoraDestinoId"]' }))).clickOptions({ text: 'Corretora B' });
      for (const [name, value] of [['quantidade', '40'], ['dataEfetiva', '2026-10-01']]) {
        const input = r.overlay.querySelector(`[formControlName="${name}"]`) as HTMLInputElement;
        input.value = value; input.dispatchEvent(new Event('input', { bubbles: true }));
      }
    }
    r.fixture.detectChanges(); const ref = TestBed.inject(MatDialog).openDialogs[0];
    expect(ref).toBeDefined(); const fechado = firstValueFrom(ref.afterClosed());
    r.overlay.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const req = r.http.expectOne(tipo === 'ESTORNO' ? `${HISTORICO}/10/estornos` : HISTORICO);
    expect(req.request.method).toBe('POST');
    if (tipo === 'ESTORNO') { expect(req.request.body).toBeNull(); req.flush(JSON.stringify({
      movimentacao: originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 }), estorno: estornoCustodia() }), { status: 201, statusText: 'Created' }); }
    else { expect(req.request.body).not.toHaveProperty('carteiraId'); expect(req.request.body.quantidade).toBe('40');
      req.flush(JSON.stringify(originalCustodia({ natureza: tipo, ...(tipo === 'ATRIBUICAO' ? { corretoraOrigemId: null } : {}) })),
        { status: 201, statusText: 'Created' }); }
    await fechado; r.fixture.detectChanges();
    expect(r.fixture.nativeElement.querySelector('.feedback-alert--success')).not.toBeNull();
    expect(r.fixture.nativeElement.querySelector('.feedback-alert--success')?.getAttribute('role')).toBe('status');
    expect(r.fixture.nativeElement.querySelector('.feedback-alert--success')?.getAttribute('aria-live')).toBe('polite');
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(0);
  }
  it.each(['cadastro', 'estorno'] as const)('%s recebe foco, tem nome e descrição e Escape devolve foco sem POST', async tipo => {
    const r = await criar();
    const botao = tipo === 'estorno' ? r.fixture.nativeElement.querySelector('[data-estornar="10"]')
      : [...r.fixture.nativeElement.querySelectorAll('button')].find((b: HTMLButtonElement) => b.textContent?.includes('Movimentar custódia'));
    botao.focus(); botao.click(); r.fixture.detectChanges();
    const aberto = firstValueFrom(TestBed.inject(MatDialog).openDialogs[0].afterOpened());
    if (tipo === 'cadastro') r.http.expectOne(DISTRIBUICAO).flush('{"carteiraId":1,"posicoes":[]}');
    const loader = TestbedHarnessEnvironment.documentRootLoader(r.fixture);
    const dialog = await loader.getHarness(MatDialogHarness);
    expect(await dialog.getRole()).toBe('dialog');
    const prefixo = tipo === 'cadastro' ? 'custodia-form' : 'custodia-estorno';
    expect(await dialog.getAriaLabelledby()).toBe(`${prefixo}-title`);
    expect(await dialog.getAriaDescribedby()).toBe(`${prefixo}-description`);
    expect(r.overlay.querySelector(`#${prefixo}-title`)?.textContent?.trim()).toBeTruthy();
    expect(r.overlay.querySelector(`#${prefixo}-description`)?.textContent?.trim()).toBeTruthy();
    for (const campo of r.overlay.querySelectorAll<HTMLElement>('mat-select,input,textarea')) {
      const nomes = (campo.getAttribute('aria-labelledby') ?? '').split(/\s+/)
        .map(id => document.getElementById(id)?.textContent ?? '').join('')
        + (campo.getAttribute('aria-label') ?? '')
        + [...((campo as HTMLInputElement).labels ?? [])].map(label => label.textContent).join('');
      expect(nomes.trim(), `nome acessível de ${campo.getAttribute('formControlName')}`).toBeTruthy();
    }
    await aberto; r.fixture.detectChanges(); await r.fixture.whenStable();
    expect(r.overlay.contains(document.activeElement)).toBe(true);
    // jsdom não calcula visibilidade/layout: CDK pode focar o próprio dialog como fallback.
    // Foco inicial no primeiro controle e ciclo Tab real permanecem na homologação de navegador.
    const controle = r.overlay.querySelector<HTMLElement>(tipo === 'cadastro' ? '[formControlName="natureza"]' : 'textarea[formControlName="motivo"]')!;
    controle.focus(); expect(document.activeElement).toBe(controle);
    const fechado = firstValueFrom(TestBed.inject(MatDialog).openDialogs[0].afterClosed());
    await dialog.close(); await fechado; r.fixture.detectChanges();
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(0);
    expect(document.activeElement).toBe(botao);
    r.http.expectNone(req => req.method === 'POST');
  });
  it('filtro abre com Enter e Escape fecha o painel sem executar escrita', async () => {
    const r = await criar();
    const select = await TestbedHarnessEnvironment.loader(r.fixture).getHarness(MatSelectHarness.with({ selector: '[formControlName="natureza"]' }));
    const campo = await select.host(); await campo.focus(); await campo.sendKeys(TestKey.ENTER);
    expect(await select.isOpen()).toBe(true);
    await campo.sendKeys(TestKey.ESCAPE); r.fixture.detectChanges();
    // O harness verifica a presença do painel: aguardar também sua animação de saída.
    await vi.waitFor(async () => expect(await select.isOpen()).toBe(false));
    expect(await campo.isFocused()).toBe(true);
    r.http.expectNone(req => req.method === 'POST');
  });
  it('estorno confirmado mantém foco no título estável após remover acionador e concluir recargas atrasadas', async () => {
    const r = await criar();
    const acionador = r.fixture.nativeElement.querySelector('[data-estornar="10"]') as HTMLButtonElement;
    acionador.focus();
    await confirmar(r, 'ESTORNO');
    const historico = r.http.expectOne(HISTORICO), distribuicao = r.http.expectOne(DISTRIBUICAO);
    r.fixture.detectChanges(); TestBed.tick();
    expect(acionador.isConnected).toBe(false);
    const titulo = r.fixture.nativeElement.querySelector('app-custodia-historico h2');
    expect(document.activeElement).toBe(titulo);
    expect(document.activeElement).not.toBe(document.body);
    historico.flush(JSON.stringify([estornoCustodia(), originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 })]));
    distribuicao.flush('{"carteiraId":1,"posicoes":[]}');
    r.fixture.detectChanges(); await r.fixture.whenStable();
    expect(document.activeElement).toBe(titulo);
    expect(r.fixture.nativeElement.querySelector('[data-estornar="10"]')).toBeNull();
    r.http.expectNone(req => req.method === 'POST');
  });
  it('cancelar estorno pelo botão mantém o acionador existente como destino do foco, sem POST', async () => {
    const r = await criar();
    const acionador = r.fixture.nativeElement.querySelector('[data-estornar="10"]') as HTMLButtonElement;
    acionador.focus(); acionador.click(); r.fixture.detectChanges();
    const ref = TestBed.inject(MatDialog).openDialogs[0];
    const fechado = firstValueFrom(ref.afterClosed());
    (r.overlay.querySelector('mat-dialog-actions button') as HTMLButtonElement).click();
    await fechado; r.fixture.detectChanges(); await r.fixture.whenStable();
    expect(acionador.isConnected).toBe(true);
    expect(document.activeElement).toBe(acionador);
    r.http.expectNone(req => req.method === 'POST');
  });
  it('estorno rejeitado mantém diálogo e foco interno sem direcionar ao histórico ou recarregar dados', async () => {
    const r = await criar();
    r.fixture.nativeElement.querySelector('[data-estornar="10"]').click(); r.fixture.detectChanges();
    const ref = TestBed.inject(MatDialog).openDialogs[0];
    await firstValueFrom(ref.afterOpened());
    const dialog = r.overlay.querySelector('[role="dialog"]') as HTMLElement;
    dialog.focus();
    r.overlay.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const req = r.http.expectOne(`${HISTORICO}/10/estornos`);
    req.flush(JSON.stringify({ code: 'MOVIMENTACAO_CUSTODIA_INVALIDADA', message: 'Dependência histórica', details: { evento: null } }),
      { status: 409, statusText: 'Conflict' });
    r.fixture.detectChanges(); await r.fixture.whenStable();
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    expect(document.activeElement).toBe(dialog);
    expect(r.overlay.querySelector('.feedback-alert--error')).not.toBeNull();
    expect(r.fixture.nativeElement.querySelector('[data-estornar="10"]')).not.toBeNull();
    r.http.expectNone(HISTORICO); r.http.expectNone(DISTRIBUICAO);
  });
  it('troca de carteira cancela ambas as recargas antigas e remove a confirmação do contexto anterior', async () => {
    const r = await criar(); await confirmar(r, 'TRANSFERENCIA');
    const antigoHistorico = r.http.expectOne(HISTORICO), antigaDistribuicao = r.http.expectOne(DISTRIBUICAO);
    r.params.next(convertToParamMap({ id: '2' })); r.fixture.detectChanges();
    expect(antigoHistorico.cancelled).toBe(true); expect(antigaDistribuicao.cancelled).toBe(true);
    r.http.expectOne('/test-api/carteiras/2/movimentacoes-custodia').flush('[]');
    r.http.expectOne('/test-api/carteiras/2/posicoes/distribuicao-corretoras').flush('{"carteiraId":2,"posicoes":[]}');
    r.fixture.detectChanges();
    expect(r.fixture.nativeElement.querySelector('.feedback-alert--success')).toBeNull();
    expect(r.operacoes.listarPorCarteira).toHaveBeenLastCalledWith(2);
    r.http.expectNone(req => req.method === 'POST');
  });
  it.each([
    ['TRANSFERENCIA', false, false], ['ATRIBUICAO', false, false], ['ESTORNO', false, false],
    ['ATRIBUICAO', true, false], ['TRANSFERENCIA', false, true], ['ESTORNO', true, true]
  ] as const)('%s mantém confirmação com falha de histórico=%s e distribuição=%s', async (tipo, falhaHistorico, falhaDistribuicao) => {
    const r = await criar(); await confirmar(r, tipo);
    const historico = r.http.expectOne(HISTORICO), distribuicao = r.http.expectOne(DISTRIBUICAO);
    expect(historico.request.method).toBe('GET'); expect(distribuicao.request.method).toBe('GET');
    if (falhaHistorico) historico.flush('Falha de leitura', { status: 500, statusText: 'Error' });
    else historico.flush(JSON.stringify(tipo === 'ESTORNO' ? [estornoCustodia(), originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 })]
      : [originalCustodia({ natureza: tipo, ...(tipo === 'ATRIBUICAO' ? { corretoraOrigemId: null } : {}) })]));
    if (falhaDistribuicao) distribuicao.flush('Falha de leitura', { status: 500, statusText: 'Error' });
    else distribuicao.flush('{"carteiraId":1,"posicoes":[]}');
    r.fixture.detectChanges();
    expect(r.fixture.nativeElement.querySelector('.feedback-alert--success')?.textContent).toContain('sucesso');
    expect(r.operacoes.listarPorCarteira).toHaveBeenCalledOnce();
    if (falhaHistorico) {
      [...r.fixture.nativeElement.querySelectorAll('app-custodia-historico button')]
        .find((b: HTMLButtonElement) => b.textContent?.includes('Tentar novamente')).click();
      r.http.expectOne(HISTORICO).flush(JSON.stringify(tipo === 'ESTORNO' ? [estornoCustodia(), originalCustodia({ situacao: 'ESTORNADA', estornoId: 20 })] : []));
      r.http.expectNone(DISTRIBUICAO);
    }
    if (falhaDistribuicao) {
      r.fixture.nativeElement.querySelector('app-distribuicao-corretoras button').click();
      r.http.expectOne(DISTRIBUICAO).flush('{"carteiraId":1,"posicoes":[]}'); r.http.expectNone(HISTORICO);
    }
    r.fixture.detectChanges(); r.http.expectNone(req => req.method === 'POST');
    expect(r.fixture.nativeElement.querySelector('.feedback-alert--success')).not.toBeNull();
    if (tipo === 'ESTORNO') {
      expect(r.fixture.nativeElement.querySelector('app-custodia-historico')?.textContent).toContain('ESTORNADA');
      expect(r.fixture.nativeElement.querySelector('[data-registro="ESTORNO:20"]')).not.toBeNull();
    }
  });
});
