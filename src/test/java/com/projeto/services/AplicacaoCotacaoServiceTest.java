package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.*;
import com.projeto.integrations.cotacao.CotacaoCandidata;
import com.projeto.integrations.cotacao.CotacaoProvider;
import com.projeto.repositories.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static com.projeto.entities.ReferenciaTipoCotacao.*;
import static com.projeto.services.RegraDecisaoCotacao.Resultado.*;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
class AplicacaoCotacaoServiceTest {
    static final OffsetDateTime O = OffsetDateTime.parse("2026-10-03T15:05:00Z");
    static final LocalDate D = LocalDate.of(2026, 10, 1);
    static final AtomicInteger IDS = new AtomicInteger();
    @Autowired AplicacaoCotacaoService service;
    @MockitoSpyBean AcaoRepository acoes;
    @MockitoSpyBean HistoricoCotacaoRepository historico;
    @MockitoSpyBean(name="alphaVantageAdapter") CotacaoProvider provider;
    @MockitoSpyBean(name="brapiAdapter") CotacaoProvider brapi;
    @Autowired PlatformTransactionManager manager;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void limparInteracoes() { clearInvocations(acoes, historico, provider, brapi); }
    @AfterEach void limparStubs() { reset(acoes, historico, provider, brapi); }

    Acao inicial(ReferenciaTipoCotacao tipo) {
        return acoes.saveAndFlush(new Acao("T" + IDS.incrementAndGet(), "Empresa", Mercado.EUA,
                Moeda.USD, new BigDecimal("100.000000"), O, tipo, tipo == INSTANTE ? O.minusDays(2) : null,
                tipo == DATA ? D : null, tipo == DESCONHECIDA ? null : O));
    }
    CotacaoCandidata candidata(Acao a, ReferenciaTipoCotacao tipo, int ordem, String preco, OffsetDateTime obt) {
        return new CotacaoCandidata(a.getTicker(), "Empresa", Moeda.USD, new BigDecimal(preco), tipo,
                tipo == INSTANTE ? O.minusDays(2).plusDays(ordem) : null,
                tipo == DATA ? D.plusDays(ordem) : null, obt);
    }
    Acao reler(Acao a) { return acoes.findById(a.getId()).orElseThrow(); }
    void semHistorico(Acao a) { assertTrue(historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId()).isEmpty()); }
    void conferirHistorico(Acao a, CotacaoCandidata c) {
        var lista = historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId());
        assertEquals(1, lista.size());
        var h = lista.get(0);
        assertEquals(a.getId(), h.getAcao().getId());
        assertEquals(0, c.cotacao().compareTo(h.getCotacao()));
        assertEquals(c.referenciaTipo(), h.getReferenciaTipo());
        assertEquals(c.referenciaData(), h.getReferenciaData());
        assertEquals(c.referenciaInstante(), h.getReferenciaInstante());
        assertEquals(c.cotacaoObtidaEm(), h.getCotacaoObtidaEm());
        assertEquals(c.referenciaTipo() == DATA ? c.cotacaoObtidaEm() : c.referenciaInstante(), h.getDataHoraCotacao());
    }
    java.util.List<Object> observacao(Acao a) {
        return java.util.Arrays.asList(a.getCotacaoAtual(),a.getReferenciaTipo(),a.getReferenciaData(),
                a.getReferenciaInstante(),a.getCotacaoObtidaEm(),a.getDataHoraCotacao());
    }
    java.util.List<Object> observacao(HistoricoCotacao h) {
        return java.util.Arrays.asList(h.getId(),h.getAcao().getId(),h.getCotacao(),h.getReferenciaTipo(),
                h.getReferenciaData(),h.getReferenciaInstante(),h.getCotacaoObtidaEm(),h.getDataHoraCotacao());
    }
    HistoricoCotacao prepararHistorico(Acao a) {
        return historico.saveAndFlush(new HistoricoCotacao(a,a.getCotacaoAtual(),a.getDataHoraCotacao(),
                a.getReferenciaTipo(),a.getReferenciaInstante(),a.getReferenciaData(),a.getCotacaoObtidaEm()));
    }
    void conferirRollback(Acao a, HistoricoCotacao anterior) {
        assertEquals(observacao(a),observacao(reler(a)));
        var hs=historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId());
        assertEquals(1,hs.size()); assertEquals(observacao(anterior),observacao(hs.get(0)));
    }

    @ParameterizedTest @EnumSource(value=ReferenciaTipoCotacao.class, names={"DATA","INSTANTE"})
    void posteriorSubstituiIntegralmenteComObtencaoPropria(ReferenciaTipoCotacao tipo) {
        var a=inicial(tipo); var c=candidata(a,tipo,1,"102",O.minusMinutes(1));
        var r=service.aplicar(a.getId(),c); var atual=reler(a);
        assertEquals(ATUALIZADA,r.decisao().resultado());
        assertSame(c,r.decisao().candidataParaSubstituicao());
        assertEquals(c.cotacao(),atual.getCotacaoAtual());
        assertEquals(c.referenciaData(),atual.getReferenciaData());
        assertEquals(c.referenciaInstante(),atual.getReferenciaInstante());
        assertEquals(c.cotacaoObtidaEm(),atual.getCotacaoObtidaEm());
        assertEquals(tipo==DATA?c.cotacaoObtidaEm():c.referenciaInstante(),atual.getDataHoraCotacao());
        conferirHistorico(a,c); verifyNoInteractions(provider,brapi);
    }
    @ParameterizedTest @EnumSource(value=ReferenciaTipoCotacao.class, names={"DATA","INSTANTE"})
    void primeiraBaselineCriaObservacao(ReferenciaTipoCotacao tipo) {
        var a=inicial(DESCONHECIDA); var c=candidata(a,tipo,0,"102",O.minusMinutes(1));
        assertEquals(ATUALIZADA,service.aplicar(a.getId(),c).decisao().resultado());
        assertEquals(tipo,reler(a).getReferenciaTipo()); conferirHistorico(a,c);
    }
    @ParameterizedTest @EnumSource(value=ReferenciaTipoCotacao.class, names={"DATA","INSTANTE"})
    void mesmaObservacaoAvancaOuPreservaSemHistorico(ReferenciaTipoCotacao tipo) {
        var a=inicial(tipo);
        for(int ordem:new int[]{-1,0,1}) {
            var r=service.aplicar(a.getId(),candidata(a,tipo,0,"100",O.plusMinutes(ordem)));
            assertEquals(SEM_NOVA_COTACAO,r.decisao().resultado());
            var atual=reler(a);
            assertEquals(ordem>0?O.plusMinutes(1):O,atual.getCotacaoObtidaEm());
            assertEquals(O,atual.getDataHoraCotacao());
            assertEquals(a.getCotacaoAtual(),atual.getCotacaoAtual());
            assertEquals(a.getReferenciaData(),atual.getReferenciaData());
            assertEquals(a.getReferenciaInstante(),atual.getReferenciaInstante()); semHistorico(a);
        }
    }
    @ParameterizedTest @EnumSource(value=ReferenciaTipoCotacao.class, names={"DATA","INSTANTE"})
    void anteriorEConflitosPreservamSemEscrita(ReferenciaTipoCotacao tipo) {
        var a=inicial(tipo); clearInvocations(acoes,historico);
        var antiga=service.aplicar(a.getId(),candidata(a,tipo,-1,"99",O.plusDays(1)));
        assertEquals(SEM_NOVA_COTACAO,antiga.decisao().resultado());
        assertEquals(FALHA,service.aplicar(a.getId(),candidata(a,tipo,0,"101",O)).decisao().resultado());
        assertEquals(FALHA,service.aplicar(a.getId(),candidata(a,tipo==DATA?INSTANTE:DATA,1,"101",O)).decisao().resultado());
        assertEquals(FALHA,service.aplicar(a.getId(),candidata(a,DESCONHECIDA,0,"101",O)).decisao().resultado());
        assertEquals(a.getCotacaoAtual(),reler(a).getCotacaoAtual());
        assertEquals(O,reler(a).getCotacaoObtidaEm()); semHistorico(a);
        verify(acoes,never()).saveAndFlush(any()); verify(historico,never()).saveAndFlush(any());
    }
    @ParameterizedTest @EnumSource(value=ReferenciaTipoCotacao.class, names={"DATA","INSTANTE"})
    void unicidadeResidualReverteTudoEPropagaSemRetry(ReferenciaTipoCotacao tipo) {
        var a=inicial(tipo); var c=candidata(a,tipo,1,"102",O.minusMinutes(1));
        // Histórico previamente existente força a constraint real, não uma falha simulada.
        var anterior=historico.saveAndFlush(new HistoricoCotacao(a,c.cotacao(),O,tipo,c.referenciaInstante(),c.referenciaData(),c.cotacaoObtidaEm()));
        clearInvocations(acoes,historico,provider);
        assertThrows(DataIntegrityViolationException.class,()->service.aplicar(a.getId(),c));
        conferirRollback(a,anterior);
        var atual=reler(a);
        assertEquals(a.getCotacaoAtual(),atual.getCotacaoAtual());
        assertEquals(a.getReferenciaData(),atual.getReferenciaData());
        assertEquals(a.getReferenciaInstante(),atual.getReferenciaInstante());
        assertEquals(O,atual.getCotacaoObtidaEm()); assertEquals(O,atual.getDataHoraCotacao());
        assertEquals(1,historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId()).size());
        verify(acoes,times(1)).findByIdForUpdate(a.getId());
        verify(historico,times(1)).saveAndFlush(any()); verifyNoInteractions(provider,brapi);
    }
    @Test void falhaDaAcaoNaoDeixaHistorico() {
        var a=inicial(DATA); var anterior=prepararHistorico(a); clearInvocations(historico);
        doThrow(new DataIntegrityViolationException("acao")).when(acoes).saveAndFlush(any());
        assertThrows(DataIntegrityViolationException.class,()->service.aplicar(a.getId(),candidata(a,DATA,1,"102",O)));
        conferirRollback(a,anterior);
        verify(historico,never()).saveAndFlush(any());
    }

    @ParameterizedTest @EnumSource(value=ReferenciaTipoCotacao.class, names={"DATA","INSTANTE"})
    void concorrenciaSerializaNovaAntesDaAntigaEOrdemInversa(ReferenciaTipoCotacao tipo) throws Exception {
        for(boolean novaPrimeiro:new boolean[]{true,false}) {
            var a=inicial(tipo);
            var antiga=candidata(a,tipo,1,"101",O.plusMinutes(2)); var nova=candidata(a,tipo,2,"102",O.plusMinutes(1));
            executarSobLock(a,novaPrimeiro?nova:antiga,novaPrimeiro?antiga:nova);
            var atual=reler(a); assertEquals(nova.cotacao(),atual.getCotacaoAtual());
            assertEquals(nova.referenciaData(),atual.getReferenciaData());
            assertEquals(nova.referenciaInstante(),atual.getReferenciaInstante());
            assertEquals(nova.cotacaoObtidaEm(),atual.getCotacaoObtidaEm());
            var hs=historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId());
            assertEquals(novaPrimeiro?1:2,hs.size());
            assertEquals(hs.size(),hs.stream().map(h->tipo==DATA?h.getReferenciaData():h.getReferenciaInstante()).distinct().count());
        }
    }
    @Test void concorrenciaMesmaObservacaoNaoRegrideObtencao() throws Exception {
        var a=inicial(DATA); var nova=candidata(a,DATA,1,"102",O.plusMinutes(1));
        executarSobLock(a,nova,candidata(a,DATA,1,"102",O));
        assertEquals(nova.cotacaoObtidaEm(),reler(a).getCotacaoObtidaEm()); conferirHistorico(a,nova);
    }
    @Test void falhaDepoisDeInserirHistoricoReverteAmbasAsGravacoes() {
        var a=inicial(DATA); var anterior=prepararHistorico(a);
        doAnswer(inv->{em.persist(inv.getArgument(0)); em.flush();
            throw new DataIntegrityViolationException("apos insercao");})
                .when(historico).saveAndFlush(any());
        assertThrows(DataIntegrityViolationException.class,()->service.aplicar(a.getId(),candidata(a,DATA,1,"102",O)));
        conferirRollback(a,anterior);
    }
    @Test void mesmaObservacaoPreencheObtencaoNulaSemHistorico() {
        var a=acoes.saveAndFlush(new Acao("T"+IDS.incrementAndGet(),"Empresa",Mercado.EUA,Moeda.USD,
                new BigDecimal("100"),O,DATA,null,D,null));
        assertEquals(SEM_NOVA_COTACAO,service.aplicar(a.getId(),candidata(a,DATA,0,"100",O)).decisao().resultado());
        assertEquals(O,reler(a).getCotacaoObtidaEm()); assertEquals(O,reler(a).getDataHoraCotacao()); semHistorico(a);
    }
    @Test void falhaRealNaConclusaoTransacionalImpedeRetornoDeSucesso() {
        var a=inicial(DATA); var anterior=prepararHistorico(a);
        var candidata=candidata(a,DATA,1,"102",O);
        var faseCommit=new java.util.concurrent.atomic.AtomicBoolean();
        var rollback=new java.util.concurrent.atomic.AtomicBoolean();
        var retorno=new java.util.concurrent.atomic.AtomicReference<AplicacaoCotacaoService.Aplicacao>();
        doAnswer(inv->{
            HistoricoCotacao h=inv.getArgument(0); em.persist(h); em.flush();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) {
                    faseCommit.set(true);
                    assertEquals(0,candidata.cotacao().compareTo(acoes.findById(a.getId()).orElseThrow().getCotacaoAtual()));
                    // Falha real do banco apenas na conclusão do proxy, após retorno do corpo.
                    jdbc.update("insert into historico_cotacao (acao_id,cotacao,data_hora_cotacao,referencia_tipo,referencia_instante,referencia_data,cotacao_obtida_em) select acao_id,cotacao,data_hora_cotacao,referencia_tipo,referencia_instante,referencia_data,cotacao_obtida_em from historico_cotacao where id = ?",h.getId());
                }
                @Override public void afterCompletion(int status) {
                    rollback.set(status==STATUS_ROLLED_BACK);
                }
            });
            return h;
        }).when(historico).saveAndFlush(any());
        assertThrows(DataIntegrityViolationException.class,()->retorno.set(service.aplicar(a.getId(),candidata)));
        assertTrue(faseCommit.get()); assertTrue(rollback.get()); assertNull(retorno.get());
        conferirRollback(a,anterior); verifyNoInteractions(provider,brapi);
    }
    @Test void concorrenciaConflitoDePrecoPreservaObservacaoAceita() throws Exception {
        var a=inicial(DATA); var primeira=candidata(a,DATA,1,"102",O);
        var segunda=candidata(a,DATA,1,"103",O.plusMinutes(1));
        assertEquals(FALHA,executarSobLock(a,primeira,segunda).decisao().resultado());
        assertEquals(primeira.cotacao(),reler(a).getCotacaoAtual()); conferirHistorico(a,primeira);
    }
    @Test void concorrenciaTiposIncompativeisPreservaObservacaoAceita() throws Exception {
        var a=inicial(DATA); var primeira=candidata(a,DATA,1,"102",O);
        assertEquals(FALHA,executarSobLock(a,primeira,candidata(a,INSTANTE,2,"103",O)).decisao().resultado());
        assertEquals(DATA,reler(a).getReferenciaTipo()); conferirHistorico(a,primeira);
    }
    AplicacaoCotacaoService.Aplicacao executarSobLock(Acao a,CotacaoCandidata primeira,CotacaoCandidata segunda) throws Exception {
        var bloqueada=new CountDownLatch(1); var liberar=new CountDownLatch(1); var iniciada=new CountDownLatch(1);
        var sessaoA=new AtomicInteger(); var sessaoB=new AtomicInteger();
        var pool=Executors.newFixedThreadPool(2);
        try {
            var f1=pool.submit(()->new TransactionTemplate(manager).execute(s->{
                sessaoA.set(jdbc.queryForObject("select session_id()",Integer.class));
                acoes.findByIdForUpdate(a.getId()).orElseThrow(); bloqueada.countDown(); aguardar(liberar);
                return service.aplicar(a.getId(),primeira);
            }));
            assertTrue(bloqueada.await(10,TimeUnit.SECONDS));
            var f2=pool.submit(()->new TransactionTemplate(manager).execute(s->{
                sessaoB.set(jdbc.queryForObject("select session_id()",Integer.class));
                iniciada.countDown(); return service.aplicar(a.getId(),segunda);
            }));
            assertTrue(iniciada.await(10,TimeUnit.SECONDS));
            assertNotEquals(sessaoA.get(),sessaoB.get());
            long limite=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            boolean contencao=false;
            while(System.nanoTime()<limite && !f2.isDone()) {
                contencao=jdbc.queryForObject("select count(*) from information_schema.sessions where session_id = ? and blocker_id = ? and upper(executing_statement) like '%FOR UPDATE%'",
                        Integer.class,sessaoB.get(),sessaoA.get())==1;
                if(contencao)break;
                Thread.onSpinWait();
            }
            assertTrue(contencao,"H2 deve mostrar B esperando o FOR UPDATE bloqueado pela sessão A");
            assertFalse(f2.isDone()); liberar.countDown();
            f1.get(10,TimeUnit.SECONDS); return f2.get(10,TimeUnit.SECONDS);
        } finally { liberar.countDown(); pool.shutdownNow(); }
    }
    static void aguardar(CountDownLatch l) {
        try { if(!l.await(10,TimeUnit.SECONDS))throw new IllegalStateException("timeout"); }
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
    }
    @Test void lockDeUmaAcaoNaoBloqueiaOutra() throws Exception {
        var a=inicial(DATA); var b=inicial(DATA); var pronta=new CountDownLatch(1); var liberar=new CountDownLatch(1);
        var pool=Executors.newSingleThreadExecutor();
        try {
            var f=pool.submit(()->new TransactionTemplate(manager).execute(s->{
                acoes.findByIdForUpdate(a.getId()); pronta.countDown(); aguardar(liberar); return null;
            }));
            assertTrue(pronta.await(10,TimeUnit.SECONDS));
            assertEquals(ATUALIZADA,service.aplicar(b.getId(),candidata(b,DATA,1,"102",O)).decisao().resultado());
            semHistorico(a); liberar.countDown(); f.get(10,TimeUnit.SECONDS);
        } finally { liberar.countDown(); pool.shutdownNow(); }
    }
    @Test void refreshDescartaEstadoAntigoJaGerenciado() {
        var a=inicial(DATA);
        new TransactionTemplate(manager).execute(s->{
            var gerenciada=acoes.findById(a.getId()).orElseThrow();
            em.createNativeQuery("update acao set referencia_data = :data, cotacao_atual = 103 where id = :id")
                    .setParameter("data",D.plusDays(3)).setParameter("id",a.getId()).executeUpdate();
            assertEquals(D,gerenciada.getReferenciaData());
            assertEquals(SEM_NOVA_COTACAO,service.aplicar(a.getId(),candidata(a,DATA,1,"102",O)).decisao().resultado());
            return null;
        });
        assertEquals(0,new BigDecimal("103").compareTo(reler(a).getCotacaoAtual())); semHistorico(a);
    }
    @ParameterizedTest @EnumSource(value=ReferenciaTipoCotacao.class, names={"DATA","INSTANTE"})
    void referenciaPosteriorComMesmoPrecoCriaNovaObservacao(ReferenciaTipoCotacao tipo) {
        var a=inicial(tipo); var c=candidata(a,tipo,1,"100",O.minusMinutes(1));
        assertEquals(ATUALIZADA,service.aplicar(a.getId(),c).decisao().resultado());
        conferirHistorico(a,c); assertEquals(c.cotacaoObtidaEm(),reler(a).getCotacaoObtidaEm());
    }
    @Test void acaoAusenteFalhaAntesDeEscritaOuHistorico() {
        var a=inicial(DATA); var c=candidata(a,DATA,1,"102",O);
        clearInvocations(acoes,historico);
        assertThrows(com.projeto.services.exceptions.ObjectNotFoundException.class,
                ()->service.aplicar(Long.MAX_VALUE,c));
        verify(acoes,never()).saveAndFlush(any()); verifyNoInteractions(historico);
    }

}
