import { CorretorasService } from '../../corretoras/corretoras.service';
import { TestBed } from '@angular/core/testing';import { provideRouter } from '@angular/router';import { of,Subject,throwError } from 'rxjs';import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';import { OperacaoResponse } from '../models/operacao';import { OperacoesService } from '../operacoes.service';import { OperacoesListPageComponent } from './operacoes-list-page.component';
const first:OperacaoResponse={id:2,carteiraId:1,ticker:'AAPL',mercado:'EUA',corretoraId:null,tipo:'VENDA',quantidade:'0.100000',precoUnitario:'200.123456',dataOperacao:'2026-01-02',ordemNoDia:2,valorTotal:'20.012345600000'};const second={...first,id:1,ticker:'PETR4',mercado:'BRASIL' as const,tipo:'COMPRA' as const,quantidade:'10',dataOperacao:'2026-01-01',ordemNoDia:1};
describe('OperacoesListPageComponent',()=>{afterEach(()=>TestBed.resetTestingModule());async function create(result=of([first,second])){const service={listar:vi.fn().mockReturnValue(result)};await TestBed.configureTestingModule({imports:[OperacoesListPageComponent],providers:[{provide:CorretorasService,useValue:{listar:vi.fn().mockReturnValue(of([]))}},provideRouter([]),{provide:OperacoesService,useValue:service}]}).compileComponents();const fixture=TestBed.createComponent(OperacoesListPageComponent);fixture.detectChanges();return{fixture,service};}
it('mostra loading e depois preserva a ordem recebida e Sem corretora',async()=>{const pending=new Subject<OperacaoResponse[]>();const{fixture}=await create(pending);expect(fixture.nativeElement.textContent).toContain('Carregando operações');pending.next([first,second]);pending.complete();fixture.detectChanges();const text=fixture.nativeElement.textContent;expect(text.indexOf('AAPL')).toBeLessThan(text.indexOf('PETR4'));expect(text).toContain('Sem corretora');expect(text).toContain('VENDA');});
it('resolve nomes sem consulta por operação e mantém histórico em falha de referência',async()=>{
 const result=await create(of([]));
 const brokers=vi.mocked(TestBed.inject(CorretorasService).listar);
 const pending=new Subject<any[]>();brokers.mockReturnValue(pending);
 result.service.listar.mockReturnValue(of([{...first,corretoraId:4},{...second,corretoraId:99}]));
 (result.fixture.componentInstance as any).load();(result.fixture.componentInstance as any).load();
 result.fixture.detectChanges();expect(brokers).toHaveBeenCalledTimes(1);
 expect(result.fixture.nativeElement.textContent).toContain('Corretora #4');
 pending.next([{id:4,nomeFantasia:'XP Investimentos',razaoSocial:'XP SA'}]);pending.complete();result.fixture.detectChanges();
 expect(result.fixture.nativeElement.textContent).toContain('XP Investimentos');expect(result.fixture.nativeElement.textContent).toContain('Corretora #99');
 expect(result.fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(2);
});
it('mostra vazio',async()=>{const{fixture}=await create(of([]));expect(fixture.nativeElement.textContent).toContain('Nenhuma operação registrada');});
it('apresenta quantidade enxuta e dinheiro sem modificar os registros recebidos',async()=>{
 const records=[Object.freeze({...first,quantidade:'0.000001',precoUnitario:'9999999999999.123456',valorTotal:'1566.000000000000'}),Object.freeze({...second,quantidade:'10.000000',precoUnitario:'30.000000',valorTotal:'300.000000000000'})];
 const original=structuredClone(records);const{fixture}=await create(of(records));
 const rows=fixture.nativeElement.querySelectorAll('tbody tr');
 expect(rows[0].textContent).toContain('Quantidade0,000001');
 expect(rows[0].textContent).toContain('US$ 9.999.999.999.999,12');
 expect(rows[0].textContent).toContain('US$ 1.566,00');
 expect(rows[1].textContent).toContain('Quantidade10');
 expect(rows[1].textContent).toContain('R$ 30,00');expect(rows[1].textContent).toContain('R$ 300,00');
 expect(records).toEqual(original);
});
it('mostra erro e faz retry apenas pelo botão',async()=>{const error={status:500,message:'Falha',details:{},code:null} as unknown as NormalizedHttpError;const{fixture,service}=await create(throwError(()=>error));service.listar.mockReturnValueOnce(of([]));(fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();fixture.detectChanges();expect(service.listar).toHaveBeenCalledTimes(2);});});
