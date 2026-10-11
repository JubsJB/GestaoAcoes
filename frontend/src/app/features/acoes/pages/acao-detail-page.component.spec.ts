import { temporalPresentationCases, localMinuteExpectation } from '../../../shared/formatters/cotacao-temporal.fixtures';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { AcoesService } from '../acoes.service';
import { AcaoResponse } from '../models/acao';
import { AcaoDetailPageComponent } from './acao-detail-page.component';
import { formatCotacaoObtidaEm, formatReferenciaCotacao } from '../../../shared/formatters/cotacao-temporal.formatter';

const OLD:AcaoResponse={id:3,ticker:'AAPL',nomeEmpresa:'Apple Inc.',mercado:'EUA',moeda:'USD',cotacaoAtual:190,dataHoraCotacao:'2026-08-28T12:00:00Z', referenciaMercadoCotacao: { tipo: 'DESCONHECIDA', data: null, instante: null }, cotacaoObtidaEm: null};
const UPDATED:AcaoResponse={...OLD,cotacaoAtual:200,dataHoraCotacao:'2026-08-29T12:00:00Z', referenciaMercadoCotacao: { tipo: 'DESCONHECIDA', data: null, instante: null }, cotacaoObtidaEm: null};
const failure=(status:number,code:string|null,message='Falha',details={})=>({status,code,message,details}) as NormalizedHttpError;
describe('AcaoDetailPageComponent',()=>{
 async function create(info?:AcaoResponse,error?:NormalizedHttpError,routeId=3){const service={buscarPorId:vi.fn().mockReturnValue(error?throwError(()=>error):of(OLD)),atualizarCotacao:vi.fn().mockReturnValue(of(UPDATED))};const toast={show:vi.fn()};await TestBed.configureTestingModule({imports:[AcaoDetailPageComponent],providers:[provideRouter([]),{provide:ActivatedRoute,useValue:{snapshot:{paramMap:{get:()=>String(routeId)}}}},{provide:AcoesService,useValue:service},{provide:SuccessToastService,useValue:toast}]}).compileComponents();const router=TestBed.inject(Router);vi.spyOn(router,'currentNavigation').mockReturnValue(info?({extras:{info:{acao:info}}} as never):null);const fixture=TestBed.createComponent(AcaoDetailPageComponent);fixture.detectChanges();return{fixture,service,toast};}
 afterEach(()=>TestBed.resetTestingModule());
 it('does not promise a new quote or provider call for an unchanged returned state', async () => {
   const {fixture, service, toast} = await create(OLD);
   const returned = Object.freeze({...OLD});
   service.atualizarCotacao.mockReturnValueOnce(of(returned));
   (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
   fixture.detectChanges();
   expect(toast.show).toHaveBeenCalledWith('Solicitação processada. Cotação retornada sem alteração.');
   expect(toast.show).not.toHaveBeenCalledWith('Cotação atualizada com sucesso.');
   expect(service.atualizarCotacao).toHaveBeenCalledTimes(1);
   expect(service.buscarPorId).not.toHaveBeenCalled();
   const component = fixture.componentInstance as unknown as {acao(): AcaoResponse};
   expect(component.acao()).toBe(returned);
 });
 it.each([
   {referenciaMercadoCotacao: {tipo: 'DATA' as const, data: '2026-10-02', instante: null}},
   {referenciaMercadoCotacao: {tipo: 'INSTANTE' as const, data: null, instante: '2026-10-02T10:00:00Z'}},
   {cotacaoObtidaEm: '2026-10-02T10:00:00.123456Z'}
 ])('reports changed returned metadata without requiring a price change: %j', async patch => {
   const {fixture, service, toast} = await create(OLD);
   const returned: AcaoResponse = Object.freeze({...OLD, ...patch});
   service.atualizarCotacao.mockReturnValueOnce(of(returned));
   (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
   fixture.detectChanges();
   expect(toast.show).toHaveBeenCalledWith('Solicitação processada. Dados da cotação atualizados na tela.');
   expect(service.buscarPorId).not.toHaveBeenCalled();
   expect(returned.cotacaoAtual).toBe(OLD.cotacaoAtual);
 });
 it('does not use the legacy timestamp as evidence of a changed market quote', async () => {
   const {fixture, service, toast} = await create(OLD);
   service.atualizarCotacao.mockReturnValueOnce(of({...OLD, dataHoraCotacao: UPDATED.dataHoraCotacao}));
   (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
   fixture.detectChanges();
   expect(toast.show).toHaveBeenCalledWith('Solicitação processada. Cotação retornada sem alteração.');
 });
 it.each(temporalPresentationCases())('separa referência $tipo de obtenção sem usar legado', async (reference, expected) => {
   const stock = Object.freeze({ ...OLD, referenciaMercadoCotacao: Object.freeze(reference),
     cotacaoObtidaEm: '2026-10-03T02:30:59.123456+00:00:30', dataHoraCotacao: '1999-01-01T00:00:00Z' });
   const {fixture, service} = await create(stock);
   const entries = Array.from(fixture.nativeElement.querySelectorAll('dl > div')) as HTMLElement[];
   const entry = (label: string) => entries.find(item => item.querySelector('dt')?.textContent === label)?.querySelector('dd')?.textContent;
   expect(entry('Referência da cotação')).toBe(expected);
   expect(entry('Cotação obtida em')).toBe(localMinuteExpectation());
   expect(fixture.nativeElement.textContent).not.toContain('1999');
   expect(service.buscarPorId).not.toHaveBeenCalled();
   expect(stock.referenciaMercadoCotacao).toBe(reference);
 });
 it('obtenção legada nula não usa dataHoraCotacao e PATCH substitui metadados retornados', async () => {
   const {fixture,service} = await create(OLD);
   expect(fixture.nativeElement.textContent).toContain('Não informada');
   expect(fixture.nativeElement.textContent).not.toContain('28/08/2026');
   const updated: AcaoResponse = { ...UPDATED, referenciaMercadoCotacao: { tipo: 'DATA', data: '2026-10-02', instante: null },
     cotacaoObtidaEm: '2026-10-04T10:20:30.123456Z' };
   service.atualizarCotacao.mockReturnValueOnce(of(updated));
   (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); fixture.detectChanges();
   expect(fixture.nativeElement.textContent).toContain('Cotação de 02/10/2026');
   expect(fixture.nativeElement.textContent).toContain(formatCotacaoObtidaEm(updated.cotacaoObtidaEm));
   expect(service.buscarPorId).not.toHaveBeenCalled(); expect(service.atualizarCotacao).toHaveBeenCalledTimes(1);
 });
 it('does not announce a preserved quote before an update failure or when loading fails', async () => {
   const initial = await create(OLD);
   expect(initial.fixture.nativeElement.textContent).not.toContain('Estamos usando a última cotação válida');
   TestBed.resetTestingModule();
   const failed = await create(undefined, failure(503, null));
   expect(failed.fixture.nativeElement.textContent).not.toContain('Estamos usando a última cotação válida');
 });
 it.each([429, 502, 503, 504])('announces confirmed preservation once after %s and clears it on manual retry', async status => {
   const { fixture, service } = await create(OLD);
   const pending = new Subject<AcaoResponse>();
   service.atualizarCotacao.mockReturnValueOnce(throwError(() => failure(status, null, 'Falha do serviço', {cotacaoPreservada: true}))).mockReturnValueOnce(pending);
   const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
   button.click();
   fixture.detectChanges();
   const alert = () => fixture.nativeElement.querySelector('.feedback-alert--error') as HTMLElement | null;
   expect(alert()?.textContent).toContain('A última cotação válida foi preservada');
   expect(alert()?.getAttribute('role')).toBe('alert');
   expect(alert()?.getAttribute('aria-live')).toBe('assertive');
   expect(fixture.nativeElement.querySelectorAll('.feedback-alert')).toHaveLength(1);
   expect(fixture.nativeElement.textContent).not.toContain('Falha do serviço');
   expect(service.atualizarCotacao).toHaveBeenCalledTimes(1);
   button.click();
   fixture.detectChanges();
   expect(alert()).toBeNull();
   expect(fixture.nativeElement.textContent).toContain('190');
   expect(button.disabled).toBe(true);
   pending.next(UPDATED);
   pending.complete();
   fixture.detectChanges();
   expect(alert()).toBeNull();
   expect(fixture.nativeElement.textContent).toContain('200');
   expect(fixture.nativeElement.textContent).not.toContain('Falha do serviço');
   expect(service.atualizarCotacao).toHaveBeenCalledTimes(2);
   expect(service.buscarPorId).not.toHaveBeenCalled();
 });
 it('uses a matching transient DTO without GET and presents the complete contract',async()=>{const{fixture,service}=await create(OLD);const text=fixture.nativeElement.textContent;expect(service.buscarPorId).not.toHaveBeenCalled();expect(text).toContain('AAPL');expect(text).toContain('Apple Inc.');expect(text).toContain('EUA');expect(text).toContain('USD');expect(text).toContain('US$');expect(text).toContain('Última cotação registrada');expect(text).toContain('pode não refletir o preço em tempo real');expect(fixture.nativeElement.querySelector('h1')?.textContent).toBe('AAPL');expect(fixture.nativeElement.querySelector('table')).toBeNull();});
 it('falls back to GET for missing or mismatched transient DTO',async()=>{const direct=await create();expect(direct.service.buscarPorId).toHaveBeenCalledWith(3);TestBed.resetTestingModule();const mismatch=await create({...OLD,id:99});expect(mismatch.service.buscarPorId).toHaveBeenCalledWith(3);});
 it('renders a dedicated local 404 without provider classification',async()=>{const{fixture}=await create(undefined,failure(404,null,'Ação não existe'));expect(fixture.nativeElement.textContent).toContain('Ação não encontrada');expect(fixture.nativeElement.textContent).toContain('Voltar para a listagem de ações');});
 it('renders non-404 error and retries with a real GET',async()=>{const{fixture,service}=await create(undefined,failure(503,'SERVICO_EXTERNO_INDISPONIVEL','Indisponível'));service.buscarPorId.mockReturnValueOnce(of(OLD));expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar a ação');(fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();fixture.detectChanges();expect(service.buscarPorId).toHaveBeenCalledTimes(2);expect(fixture.nativeElement.textContent).toContain('Apple Inc.');});
 it('PATCHes once while busy, replaces DTO, announces the returned state and performs no GET',async()=>{const{fixture,service,toast}=await create(OLD);const pending=new Subject<AcaoResponse>();service.atualizarCotacao.mockReturnValueOnce(pending);const button=fixture.nativeElement.querySelector('button') as HTMLButtonElement;button.click();button.click();fixture.detectChanges();expect(service.atualizarCotacao).toHaveBeenCalledTimes(1);expect(service.atualizarCotacao).toHaveBeenCalledWith(3);expect(button.disabled).toBe(true);expect(button.getAttribute('aria-busy')).toBe('true');expect(fixture.nativeElement.textContent).toContain('Atualizando cotação');pending.next(UPDATED);pending.complete();fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('200');expect(toast.show).toHaveBeenCalledWith('Solicitação processada. Dados da cotação atualizados na tela.');expect(service.buscarPorId).not.toHaveBeenCalled();});
 it('keeps the back action navigable in the sticky workspace structure',async()=>{const{fixture}=await create(OLD);const back=fixture.nativeElement.querySelector('a.app-back-action') as HTMLAnchorElement;expect(back).toBeTruthy();expect(back.getAttribute('href')).toBe('/acoes');});
 it.each([[409,'TICKER_CANONICO_DIVERGENTE'],[422,'COTACAO_INDISPONIVEL'],[422,'COTACAO_FORA_DA_PRECISAO'],[429,'LIMITE_REQUISICOES_EXCEDIDO'],[502,'RESPOSTA_EXTERNA_INVALIDA'],[503,'SERVICO_EXTERNO_INDISPONIVEL'],[504,'SERVICO_EXTERNO_TIMEOUT']] as const)('preserves the prior DTO and structured error without rendering internal fields after %s/%s',async(status,code)=>{const{fixture,service}=await create(OLD);const expectedError=failure(status,code,`Erro ${code}`,{cotacaoPreservada:true});service.atualizarCotacao.mockReturnValueOnce(throwError(()=>expectedError));(fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();fixture.detectChanges();const component=fixture.componentInstance as unknown as {acao():AcaoResponse|null;updateError():NormalizedHttpError|null};const currentError=component.updateError();const text=fixture.nativeElement.textContent;expect(currentError?.status).toBe(expectedError.status);expect(currentError?.code).toBe(expectedError.code);expect(currentError?.message).toBe(expectedError.message);expect(currentError?.details).toEqual(expectedError.details);expect(component.acao()).toEqual(OLD);expect(text).toContain('190');expect(text).toContain('AAPL');expect(text).toContain('A atualização da cotação não foi concluída');expect(text).toContain('A última cotação válida foi preservada');expect(text).not.toContain(`Erro ${code}`);expect(text).not.toContain('cotacaoPreservada');expect(service.atualizarCotacao).toHaveBeenCalledTimes(1);expect((fixture.nativeElement.querySelector('button') as HTMLButtonElement).disabled).toBe(false);expect(service.buscarPorId).not.toHaveBeenCalled();if(code==='TICKER_CANONICO_DIVERGENTE')expect(document.querySelector('[role="dialog"]')).toBeNull();});
 it('hides the PETR4 technical payload while retaining it internally and announces one error', async () => {
   const {fixture, service} = await create(OLD);
   const error = failure(422, 'REFERENCIA_TEMPORAL_INVALIDA', 'Instante não exatamente representável em microssegundos',
     {acaoId: 3, cotacaoPreservada: true, ultimaCotacaoValida: 49.26, dataHoraUltimaCotacao: '2026-09-25T00:27:52Z'});
   service.atualizarCotacao.mockReturnValueOnce(throwError(() => error));
   (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); fixture.detectChanges();
   const alerts = fixture.nativeElement.querySelectorAll('.feedback-alert') as NodeListOf<HTMLElement>;
   expect(alerts).toHaveLength(1);
   expect(alerts[0].textContent).toContain('A atualização da cotação não foi concluída');
   expect(alerts[0].textContent).toContain('A última cotação válida foi preservada');
   expect(alerts[0].getAttribute('role')).toBe('alert');
   expect(alerts[0].getAttribute('aria-live')).toBe('assertive');
   expect(alerts[0].querySelector('dl')).toBeNull();
   for (const internal of ['acaoId', 'cotacaoPreservada', 'ultimaCotacaoValida', 'dataHoraUltimaCotacao', 'microssegundos', '49.26', '2026-09-25']) {
     expect(alerts[0].textContent).not.toContain(internal);
   }
   const component = fixture.componentInstance as unknown as {updateError(): NormalizedHttpError};
   expect(component.updateError()).toBe(error);
 });
 it.each([undefined, false, 'true'])('does not assert preservation without explicit boolean confirmation: %s', async preserved => {
   const {fixture, service} = await create(OLD);
   service.atualizarCotacao.mockReturnValueOnce(throwError(() => failure(422, 'REFERENCIA_TEMPORAL_INVALIDA', 'Interno',
     preserved === undefined ? {} : {cotacaoPreservada: preserved})));
   (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); fixture.detectChanges();
   expect(fixture.nativeElement.textContent).toContain('A atualização da cotação não foi concluída');
   expect(fixture.nativeElement.textContent).not.toContain('foi preservada');
 });
 it('does not fabricate a previous quote when the initial load failed', async () => {
   const {fixture, service} = await create(undefined, failure(404, null, 'Ação não existe'));
   expect(fixture.nativeElement.textContent).not.toContain('foi preservada');
   expect(fixture.nativeElement.textContent).not.toContain('Última cotação registrada');
   expect(service.atualizarCotacao).not.toHaveBeenCalled();
 });
 it('does not claim no server writes when a transport failure has an unknown result', async () => {
   const {fixture, service} = await create(OLD);
   service.atualizarCotacao.mockReturnValueOnce(throwError(() => failure(0, null, 'Interno')));
   (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); fixture.detectChanges();
   expect(fixture.nativeElement.textContent).toContain('Não foi possível confirmar a atualização');
   expect(fixture.nativeElement.textContent).not.toContain('foi preservada');
 });
});
