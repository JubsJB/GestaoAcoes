package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2IsolationInitializer;
import com.projeto.entities.*;
import com.projeto.mappers.DistribuicaoCorretoraMapper;
import com.projeto.resources.DistribuicaoCorretoraResource;
import com.projeto.resources.exceptions.ResourceExceptionHandler;
import com.projeto.services.*;
import com.projeto.repositories.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(value="custodia-h2",inheritProfiles=false)
@ContextConfiguration(classes=DistribuicaoCustodiaH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class DistribuicaoCustodiaH2Test {
    static final OffsetDateTime AGORA=OffsetDateTime.parse("2026-10-24T12:00:00Z");
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    @TestConfiguration(proxyBeanMethods=false)
    @Import({TransferenciaCustodiaTransacaoH2Test.Config.class,DistribuicaoPosicaoCorretoraService.class,
        CalculadoraDistribuicaoCorretora.class,CalculadoraPosicao.class,AtribuicaoCustodiaService.class,EstornoCustodiaService.class,
        ConsultaMovimentacaoCustodiaService.class,com.projeto.mappers.CustodiaMapper.class,
        PosicaoService.class,CalculadoraRentabilidade.class,com.projeto.mappers.PosicaoMapper.class})
    static class Config { }
    @Autowired DistribuicaoPosicaoCorretoraService consulta;
    @Autowired TransferenciaCustodiaService transferencias;
    @Autowired AtribuicaoCustodiaService atribuicoes;
    @Autowired EstornoCustodiaService estornos;
    @Autowired PlatformTransactionManager manager;
    @Autowired EntityManagerFactory emf;
    @Autowired ConsultaMovimentacaoCustodiaService historico;
    @Autowired PosicaoService posicoesFinanceiras;
    @MockitoSpyBean OperacaoRepository operacoes;
    @PersistenceContext EntityManager em;
    Long carteira,outra,acao,a,b;
    <T>T tx(Function<EntityManager,T> trabalho){return new TransactionTemplate(manager).execute(s->trabalho.apply(em));}
    @BeforeEach void preparar(){reset(operacoes);tx(e->{
        for(String tipo:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","Acao","Corretora","Carteira"))e.createQuery("delete from "+tipo).executeUpdate();
        var c=new Carteira("Alvo",AGORA);var o=new Carteira("Outra",AGORA);
        var ac=new Acao("AAPL","Sintética",Mercado.EUA,Moeda.USD,BigDecimal.TEN,AGORA);
        var ca=ConsultaCustodiaRestH2Test.corretora("11111111000111");var cb=ConsultaCustodiaRestH2Test.corretora("22222222000122");
        for(Object item:List.of(c,o,ac,ca,cb))e.persist(item);
        carteira=c.getId();outra=o.getId();acao=ac.getId();a=ca.getId();b=cb.getId();
        e.persist(new Operacao(c,ac,ca,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));
        e.flush();return null;
    });}
    MovimentacaoCustodia transferir(String quantidade){return transferencias.transferir(new TransferenciaCustodiaCommand(carteira,"AAPL",Mercado.EUA,a,b,new BigDecimal(quantidade),DIA));}
    CalculadoraDistribuicaoCorretora.Posicao posicao(){return consulta.consultar(carteira).posicoes().get(0);}
    static void decimal(String esperado,BigDecimal atual){assertEquals(0,new BigDecimal(esperado).compareTo(atual));}
    List<List<Object>> financeiro(){return tx(e->{
        @SuppressWarnings("unchecked") List<Object[]> rows=e.createNativeQuery("select * from operacao order by id").getResultList();return rows.stream().map(Arrays::asList).toList();
    });}
    CalculadoraPosicao.ResultadoReplay valores(){return tx(e->new CalculadoraPosicao().reproduzir(operacoes.listarParaDistribuicao(carteira)));}
    com.fasterxml.jackson.databind.JsonNode json()throws Exception{
        var mvc=MockMvcBuilders.standaloneSetup(new DistribuicaoCorretoraResource(consulta,new DistribuicaoCorretoraMapper())).setControllerAdvice(new ResourceExceptionHandler()).build();
        return new ObjectMapper().readTree(mvc.perform(get("/carteiras/"+carteira+"/posicoes/distribuicao-corretoras")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    @Test void atribuicaoEEstornoConservamGrupoNuloEFinanceiro(){
        tx(e->{e.createNativeQuery("update operacao set corretora_id=null where carteira_id=?1").setParameter(1,carteira).executeUpdate();return null;});
        var antes=financeiro();var resultados=valores();
        var m=atribuicoes.atribuir(new AtribuicaoCustodiaCommand(carteira,"AAPL",Mercado.EUA,b,new BigDecimal("40"),DIA));
        var p=posicao();decimal("100",p.quantidadeConsolidada());assertTrue(p.reconciliada());
        decimal("60",p.corretoras().stream().filter(g->g.corretoraId()==null).findFirst().orElseThrow().quantidadeLiquida());
        estornos.estornar(new EstornoCustodiaCommand(carteira,m.getId(),null));
        p=posicao();assertEquals(1,p.corretoras().size());assertNull(p.corretoras().get(0).corretoraId());decimal("100",p.corretoras().get(0).quantidadeLiquida());
        assertEquals(antes,financeiro());assertEquals(resultados,valores());
    }
    @Test void transferenciaEEstornoNaoMudamPrecoCustoResultadosOuCotacao(){
        var antes=financeiro();var resultados=valores();var m=transferir("40");
        assertEquals(resultados,valores());assertTrue(posicao().reconciliada());
        estornos.estornar(new EstornoCustodiaCommand(carteira,m.getId(),"Correção"));
        decimal("100",posicao().corretoras().get(0).quantidadeLiquida());assertEquals(1,posicao().corretoras().size());
        assertEquals(resultados,valores());assertEquals(antes,financeiro());
        tx(e->{decimal("10",e.find(Acao.class,acao).getCotacaoAtual());return null;});
    }
    @Test void invarianciaFinanceiraIncluiLucroRealizadoENaoRealizadoNaoZero(){
        tx(e->{e.createNativeQuery("update acao set cotacao_atual=12 where id=?1").setParameter(1,acao).executeUpdate();
            e.persist(new Operacao(e.find(Carteira.class,carteira),e.find(Acao.class,acao),e.find(Corretora.class,a),TipoOperacao.VENDA,new BigDecimal("20"),new BigDecimal("15"),DIA.plusDays(1),1,new BigDecimal("300")));return null;});
        var antes=posicoesFinanceiras.listarPorCarteira(carteira);var replay=valores();
        decimal("100",replay.resultadoRealizado());decimal("10",antes.get(0).precoMedio());decimal("800",antes.get(0).custoPosicao());decimal("160",antes.get(0).resultadoNaoRealizado());
        var m=transferir("40");consulta.consultar(carteira);assertEquals(antes,posicoesFinanceiras.listarPorCarteira(carteira));assertEquals(replay,valores());
        estornos.estornar(new EstornoCustodiaCommand(carteira,m.getId(),null));consulta.consultar(carteira);
        assertEquals(antes,posicoesFinanceiras.listarPorCarteira(carteira));assertEquals(replay,valores());
    }
    Long op(Long corretora,TipoOperacao tipo,String quantidade,LocalDate data,int ordem){return tx(e->{
        var o=new Operacao(e.find(Carteira.class,carteira),e.find(Acao.class,acao),corretora==null?null:e.find(Corretora.class,corretora),tipo,new BigDecimal(quantidade),BigDecimal.TEN,data,ordem,new BigDecimal(quantidade).multiply(BigDecimal.TEN));e.persist(o);e.flush();return o.getId();
    });}
    @Test void financeiroGenericoPrimeiroNegativoNaoEhPiorSaldoEZeroMantemDiagnostico()throws Exception{
        Long primeira=op(b,TipoOperacao.VENDA,"10",DIA.plusDays(1),1);
        op(b,TipoOperacao.VENDA,"20",DIA.plusDays(2),1);op(b,TipoOperacao.COMPRA,"30",DIA.plusDays(3),1);
        var p=posicao();assertEquals(1,p.corretoras().size());var d=p.diagnosticosCorretoras().stream().filter(g->b.equals(g.corretoraId())).findFirst().orElseThrow();
        assertTrue(d.possuiDeficitHistorico());assertFalse(d.saldoNegativoAtual());decimal("0",d.quantidadeFinal());decimal("-10",d.primeiroDeficit().saldoAposEvento());
        var linhas=json().at("/posicoes/0/diagnosticosCorretoras");com.fasterxml.jackson.databind.JsonNode evidencia=null;
        for(var linha:linhas)if(linha.path("corretoraId").asLong()==b)evidencia=linha.get("primeiroDeficit");
        assertNotNull(evidencia);assertEquals(6,evidencia.size());assertEquals("FINANCEIRO",evidencia.get("dominio").asText());assertEquals(primeira.longValue(),evidencia.get("id").asLong());
        assertEquals("VENDA",evidencia.get("natureza").asText());assertEquals(1,evidencia.at("/posicaoCronologica/ordemNoDia").asInt());
        assertFalse(evidencia.has("operacaoId"));assertFalse(evidencia.has("dataOperacao"));assertFalse(evidencia.has("saldoAposOperacao"));
    }
    @Test void custodiaInconsistenteEstruturalIdentificaFracaoEPrimeiroEventoSemIdFinanceiroFalso()throws Exception{
        Long primeiro=tx(e->{var c=e.find(Carteira.class,carteira);var ac=e.find(Acao.class,acao);var ca=e.find(Corretora.class,a);var cb=e.find(Corretora.class,b);
            var posterior=new MovimentacaoCustodia(null,c,ac,NaturezaMovimentacaoCustodia.TRANSFERENCIA,ca,cb,BigDecimal.TEN,DIA,1,PosicaoRacionalCustodia.deTexto("11","12"),Clock.fixed(AGORA.toInstant(),ZoneOffset.UTC));
            var inicial=new MovimentacaoCustodia(null,c,ac,NaturezaMovimentacaoCustodia.TRANSFERENCIA,ca,cb,new BigDecimal("110"),DIA,1,PosicaoRacionalCustodia.deTexto("9","10"),Clock.fixed(AGORA.toInstant(),ZoneOffset.UTC));
            e.persist(posterior);e.persist(inicial);e.flush();return inicial.getId();});
        // Fixture histórica estrutural; o fluxo de escrita não permite registrar esse novo déficit.
        var evidencia=json().at("/posicoes/0/diagnosticosCorretoras/0/primeiroDeficit");
        assertEquals("CUSTODIA",evidencia.get("dominio").asText());assertEquals(primeiro.longValue(),evidencia.get("id").asLong());assertEquals("TRANSFERENCIA",evidencia.get("natureza").asText());
        assertEquals("9",evidencia.at("/posicaoCronologica/numerador").asText());assertEquals("10",evidencia.at("/posicaoCronologica/denominador").asText());
        assertFalse(evidencia.has("operacaoId"));assertFalse(evidencia.at("/posicaoCronologica").has("ordemNoDia"));decimal("-10",evidencia.get("saldoAposEvento").decimalValue());
    }
    @Test void posicaoEncerradaNaoApareceMasHistoricoContinuaCompleto(){
        transferir("40");op(a,TipoOperacao.VENDA,"60",DIA.plusDays(1),1);op(b,TipoOperacao.VENDA,"40",DIA.plusDays(1),2);
        assertTrue(consulta.consultar(carteira).posicoes().isEmpty());assertEquals(1,historico.historico(carteira,null,null).size());
    }
    @Test void retroativaValidaRemoveDeficitDerivadoSemAlterarLegado(){
        op(b,TipoOperacao.VENDA,"10",DIA.plusDays(1),1);assertTrue(posicao().diagnosticosCorretoras().stream().anyMatch(g->g.possuiDeficitHistorico()));
        var antes=financeiro();transferir("10");assertTrue(posicao().diagnosticosCorretoras().stream().noneMatch(g->g.possuiDeficitHistorico()));assertEquals(antes,financeiro());
    }
    @Test void carteiraIndependenteNaoRecebeMovimentacaoOuSaldo(){
        transferir("40");assertTrue(consulta.consultar(outra).posicoes().isEmpty());decimal("100",posicao().quantidadeConsolidada());
    }
    @Test void quatroConsultasConstantesParaMuitasCorretorasSemEscrita(){
        var stats=emf.unwrap(SessionFactory.class).getStatistics();stats.setStatisticsEnabled(true);stats.clear();
        consulta.consultar(carteira);assertEquals(4,stats.getPrepareStatementCount());
        tx(e->{for(int i=1;i<=30;i++){var broker=ConsultaCustodiaRestH2Test.corretora("%014d".formatted(30000000000000L+i));e.persist(broker);
            e.persist(new MovimentacaoCustodia(null,e.find(Carteira.class,carteira),e.find(Acao.class,acao),NaturezaMovimentacaoCustodia.TRANSFERENCIA,e.find(Corretora.class,a),broker,BigDecimal.ONE,DIA,1,
                PosicaoRacionalCustodia.deTexto(Integer.toString(i),Integer.toString(i+1)),Clock.fixed(AGORA.toInstant(),ZoneOffset.UTC)));}e.flush();return null;});
        var antes=financeiro();stats.clear();var p=posicao();assertEquals(31,p.corretoras().size());decimal("100",p.quantidadeAtribuidaTotal());
        assertEquals(4,stats.getPrepareStatementCount());assertEquals(0,stats.getEntityInsertCount());assertEquals(0,stats.getEntityUpdateCount());assertEquals(0,stats.getEntityDeleteCount());assertEquals(antes,financeiro());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void snapshotNaoMisturaCustodiaOuEstornoConfirmadoEntreLeituras(boolean estornar)throws Exception{
        Long original=estornar?transferir("40").getId():null;
        var real=mockingDetails(operacoes).getMockCreationSettings().getDefaultAnswer();var executou=new AtomicBoolean();
        doAnswer(inv->{var dados=real.answer(inv);assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(java.sql.Connection.TRANSACTION_SERIALIZABLE,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            if(executou.compareAndSet(false,true)){
                var executor=Executors.newSingleThreadExecutor();try{
                    executor.submit(()->{if(estornar)estornos.estornar(new EstornoCustodiaCommand(carteira,original,null));else transferir("40");}).get(10,TimeUnit.SECONDS);
                }finally{executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
            }return dados;
        }).when(operacoes).listarParaDistribuicao(carteira);
        var primeiro=posicao();assertTrue(executou.get());assertEquals(estornar?2:1,primeiro.corretoras().size());
        var depois=posicao();assertEquals(estornar?1:2,depois.corretoras().size());decimal("100",depois.quantidadeConsolidada());
    }
    @Test void transferenciaAfetaDistribuicaoSemAlterarConsolidado(){
        transferencias.transferir(new TransferenciaCustodiaCommand(carteira,"AAPL",Mercado.EUA,a,b,new BigDecimal("40"),DIA));
        var p=consulta.consultar(carteira).posicoes().get(0);
        assertEquals(0,new BigDecimal("100").compareTo(p.quantidadeConsolidada()));
        assertEquals(2,p.corretoras().size());
        assertEquals(0,new BigDecimal("60").compareTo(p.corretoras().stream().filter(g->a.equals(g.corretoraId())).findFirst().orElseThrow().quantidadeLiquida()));
        assertEquals(0,new BigDecimal("40").compareTo(p.corretoras().stream().filter(g->b.equals(g.corretoraId())).findFirst().orElseThrow().quantidadeLiquida()));
    }
}
