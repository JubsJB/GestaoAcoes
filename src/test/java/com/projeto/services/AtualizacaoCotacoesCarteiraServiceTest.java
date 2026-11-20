package com.projeto.services;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.*;
import com.projeto.integrations.cotacao.*;
import com.projeto.mappers.AcaoMapper;
import com.projeto.repositories.*;
import com.projeto.services.exceptions.*;
import com.projeto.validation.TickerNormalizer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.projeto.entities.ReferenciaTipoCotacao.*;
import static com.projeto.services.RegraDecisaoCotacao.Resultado.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
class AtualizacaoCotacoesCarteiraServiceTest {
    static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-04T16:00:00Z");
    static final LocalDate DAY = LocalDate.of(2026,10,2);
    static final AtomicInteger IDS = new AtomicInteger();
    @MockitoSpyBean AcaoRepository acoes;
    @Autowired CarteiraRepository carteiras;
    @Autowired OperacaoRepository operacoes;
    @MockitoSpyBean HistoricoCotacaoRepository historico;
    @Autowired PosicaoService posicoes;
    @MockitoSpyBean AplicacaoCotacaoService aplicacao;
    @Autowired EntityManager em;
    @Autowired jakarta.persistence.EntityManagerFactory emf;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    CotacaoProvider br, us;
    AcaoService individual;
    AtualizacaoCotacoesCarteiraService lote;
    Carteira carteira;

    @BeforeEach void preparar() {
        br=mock(CotacaoProvider.class); us=mock(CotacaoProvider.class);
        when(br.mercado()).thenReturn(Mercado.BRASIL); when(us.mercado()).thenReturn(Mercado.EUA);
        individual=novaInstancia();
        lote=new AtualizacaoCotacoesCarteiraService(posicoes,individual,acoes);
        carteira=carteiras.saveAndFlush(new Carteira("Lote"+IDS.incrementAndGet(),NOW));
        clearInvocations(br,us,historico,aplicacao);
    }
    @AfterEach void limpar() { reset(historico,aplicacao); }
    AcaoService novaInstancia() {
        return new AcaoService(new TickerNormalizer(),List.of(br,us),mock(AcaoPersistenceService.class),
                aplicacao,acoes,new AcaoMapper(),Clock.fixed(NOW.toInstant(),ZoneOffset.UTC));
    }
    Acao acao(Mercado mercado, ReferenciaTipoCotacao tipo, OffsetDateTime obtencao) {
        return acoes.saveAndFlush(new Acao("LT"+IDS.incrementAndGet(),"Empresa",mercado,
                mercado==Mercado.EUA?Moeda.USD:Moeda.BRL,new BigDecimal("100.000000"),NOW.plusDays(3),
                tipo,tipo==INSTANTE?NOW.minusDays(2):null,tipo==DATA?DAY:null,obtencao));
    }
    void operacao(Carteira c, Acao a, TipoOperacao tipo, int quantidade, int ordem) {
        operacoes.saveAndFlush(new Operacao(c,a,null,tipo,new BigDecimal(quantidade),BigDecimal.TEN,
                DAY,ordem,new BigDecimal(quantidade*10)));
    }
    Acao aberta(Mercado mercado, ReferenciaTipoCotacao tipo, OffsetDateTime obtencao) {
        var a=acao(mercado,tipo,obtencao); operacao(carteira,a,TipoOperacao.COMPRA,2,1); return a;
    }
    CotacaoCandidata candidata(Acao a,int ordem,String preco,OffsetDateTime obtencao) {
        var tipo=a.getMercado()==Mercado.EUA?DATA:INSTANTE;
        return new CotacaoCandidata(a.getTicker(),"Empresa",a.getMoeda(),new BigDecimal(preco),tipo,
                tipo==INSTANTE?NOW.minusDays(2).plusDays(ordem):null,
                tipo==DATA?DAY.plusDays(ordem):null,obtencao);
    }
    CotacaoProvider provider(Acao a) { return a.getMercado()==Mercado.EUA?us:br; }
    void responder(Acao a,int ordem,String preco) {
        when(provider(a).consultarCandidataAtualizacao(eq(a.getTicker()),any(),any(),any())).thenAnswer(i->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return candidata(a,ordem,preco,NOW);
        });
    }
    ResultadoAtualizacaoCotacao unico() { return lote.atualizar(carteira.getId()).resultados().get(0); }
    long historicos(Acao a) { return historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId()).size(); }
    Acao reler(Acao a) { return acoes.findById(a.getId()).orElseThrow(); }

    @Test void inexistenteVaziaEZeradaNaoConsultamNemAplicam() {
        assertThrows(ObjectNotFoundException.class,()->lote.atualizar(-1L));
        assertEquals(new AtualizacaoCotacoesCarteiraService.Resumo(0,0,0),lote.atualizar(carteira.getId()).resumo());
        var a=aberta(Mercado.BRASIL,DESCONHECIDA,null);
        operacao(carteira,a,TipoOperacao.VENDA,2,2);
        acao(Mercado.EUA,DESCONHECIDA,null); // Apenas cadastrada.
        assertTrue(lote.atualizar(carteira.getId()).resultados().isEmpty());
        verifyNoInteractions(br,us,historico);
    }

    @Test void replayInconsistenteFalhaAntesDeQualquerItem() {
        var a=acao(Mercado.BRASIL,DESCONHECIDA,null);
        operacao(carteira,a,TipoOperacao.VENDA,1,1);
        assertEquals(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE,
                assertThrows(ApiException.class,()->lote.atualizar(carteira.getId())).getCode());
        verifyNoInteractions(br,us,historico);
    }

    @Test void misturaDeduplicadaOrdenadaComSucessoParcial() {
        var a=aberta(Mercado.BRASIL,DESCONHECIDA,null);
        var b=aberta(Mercado.EUA,DATA,NOW.minusMinutes(1));
        var c=aberta(Mercado.EUA,DATA,NOW.minusHours(1));
        operacao(carteira,a,TipoOperacao.COMPRA,1,2);
        responder(a,0,"101"); responder(c,0,"102");
        var r=lote.atualizar(carteira.getId());
        assertEquals(List.of(a.getId(),b.getId(),c.getId()),r.resultados().stream().map(ResultadoAtualizacaoCotacao::acaoId).toList());
        assertEquals(List.of(ATUALIZADA,SEM_NOVA_COTACAO,FALHA),r.resultados().stream().map(ResultadoAtualizacaoCotacao::resultado).toList());
        assertEquals(new AtualizacaoCotacoesCarteiraService.Resumo(1,1,1),r.resumo());
        assertEquals("REUSO",r.resultados().get(1).motivo());
        assertEquals(1,historicos(a)); assertEquals(0,historicos(b)); assertEquals(0,historicos(c));
        verify(br,times(1)).consultarCandidataAtualizacao(any(),any(),any(),any());
        verify(us,times(1)).consultarCandidataAtualizacao(any(),any(),any(),any());
        assertThrows(UnsupportedOperationException.class,()->r.resultados().clear());
    }

    @ParameterizedTest @EnumSource(value=Mercado.class,names={"BRASIL","EUA"})
    void referenciaPosteriorUsaObtencaoPropriaAnterior(Mercado mercado) {
        var a=aberta(mercado,mercado==Mercado.EUA?DATA:INSTANTE,NOW.plusMinutes(1));
        responder(a,1,"102"); var r=unico();
        assertEquals(ATUALIZADA,r.resultado()); assertEquals(NOW,r.cotacao().obtencao());
        assertEquals(NOW,reler(a).getCotacaoObtidaEm()); assertEquals(1,historicos(a));
    }

    @Test void igualdadeAvancaObtencaoSemHistoricoEAnteriorPreserva() {
        var a=aberta(Mercado.BRASIL,INSTANTE,NOW.minusHours(1)); responder(a,0,"100");
        var r=unico(); assertEquals(SEM_NOVA_COTACAO,r.resultado());
        assertEquals("MESMA_OBSERVACAO",r.motivo()); assertEquals(NOW,r.cotacao().obtencao());
        assertEquals(a.getDataHoraCotacao(),r.cotacao().legado()); assertEquals(0,historicos(a));
        responder(a,-1,"101"); r=unico(); assertEquals("REFERENCIA_ANTERIOR",r.motivo());
        assertEquals(new BigDecimal("100.000000"),r.cotacao().preco()); assertEquals(0,historicos(a));
    }

    @Test void todasAtualizadasTodasSemNovidadeETodasFalhando() {
        var a=aberta(Mercado.BRASIL,DESCONHECIDA,null); var b=aberta(Mercado.EUA,DESCONHECIDA,null);
        responder(a,0,"101"); responder(b,0,"101");
        assertEquals(new AtualizacaoCotacoesCarteiraService.Resumo(2,0,0),lote.atualizar(carteira.getId()).resumo());
        assertEquals(new AtualizacaoCotacoesCarteiraService.Resumo(0,2,0),lote.atualizar(carteira.getId()).resumo());
        when(br.consultarCandidataAtualizacao(any(),any(),any(),any())).thenThrow(new ApiException(HttpStatus.BAD_GATEWAY,"PAYLOAD_INVALIDO","Resposta inválida"));
        // Restart loses cooldown; use an expired obtained time by consulting in a later instance.
        var later=new AcaoService(new TickerNormalizer(),List.of(br,us),mock(AcaoPersistenceService.class),
                aplicacao,acoes,new AcaoMapper(),Clock.fixed(NOW.plusHours(1).toInstant(),ZoneOffset.UTC));
        when(us.consultarCandidataAtualizacao(any(),any(),any(),any())).thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE,ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,"Indisponível"));
        var failed=new AtualizacaoCotacoesCarteiraService(posicoes,later,acoes).atualizar(carteira.getId());
        assertEquals(new AtualizacaoCotacoesCarteiraService.Resumo(0,0,2),failed.resumo());
        assertEquals(1,historicos(a)); assertEquals(1,historicos(b));
    }

    @Test void cooldownCompartilhaSnapshotDoPatchMesmoComNovaObservacaoNoBanco() {
        var a=aberta(Mercado.EUA,DATA,NOW.minusHours(1)); responder(a,1,"101");
        var primeira=individual.atualizarCotacaoCompartilhada(a.getId());
        aplicacao.aplicar(a.getId(),candidata(a,2,"102",NOW.plusMinutes(1)));
        clearInvocations(us,historico,aplicacao);
        var r=unico(); assertEquals(SEM_NOVA_COTACAO,r.resultado()); assertEquals("COOLDOWN_SUCESSO",r.motivo());
        assertSame(primeira.cotacao(),r.cotacao()); assertEquals(new BigDecimal("101.000000"),r.cotacao().preco());
        assertEquals(new BigDecimal("102.000000"),reler(a).getCotacaoAtual());
        verifyNoInteractions(us,historico,aplicacao);
        assertEquals(new BigDecimal("101.000000"),individual.atualizarCotacao(a.getId()).cotacaoAtual());
    }

    @Test void cooldownFalhaPrecedeObtencaoRecenteDeOutraExecucao() {
        var a=aberta(Mercado.EUA,DATA,NOW.minusHours(1));
        when(us.consultarCandidataAtualizacao(any(),any(),any(),any())).thenThrow(new ApiException(HttpStatus.GATEWAY_TIMEOUT,ErrorCodes.SERVICO_EXTERNO_TIMEOUT,"Timeout"));
        assertEquals(FALHA,unico().resultado());
        aplicacao.aplicar(a.getId(),candidata(a,1,"101",NOW)); clearInvocations(us,historico,aplicacao);
        var r=unico(); assertEquals(FALHA,r.resultado()); assertEquals("COOLDOWN_FALHA",r.motivo());
        assertEquals(ErrorCodes.SERVICO_EXTERNO_TIMEOUT,r.falha().getCode()); verifyNoInteractions(us,historico,aplicacao);
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void rollbackIsoladoNaoDesfazAnteriorNemImpedeProximo(boolean noCommit) {
        var a=aberta(Mercado.BRASIL,DESCONHECIDA,null);
        var b=aberta(Mercado.BRASIL,DESCONHECIDA,null);
        var c=aberta(Mercado.BRASIL,DESCONHECIDA,null);
        responder(a,0,"101"); responder(b,0,"102"); responder(c,0,"103");
        doAnswer(i->{
            HistoricoCotacao h=i.getArgument(0);
            em.persist(h); em.flush();
            if(h.getAcao().getId().equals(b.getId())) {
                Runnable violar=()->jdbc.update("insert into historico_cotacao (acao_id,cotacao,data_hora_cotacao,referencia_tipo,referencia_instante,referencia_data,cotacao_obtida_em) select acao_id,cotacao,data_hora_cotacao,referencia_tipo,referencia_instante,referencia_data,cotacao_obtida_em from historico_cotacao where id = ?",h.getId());
                if(noCommit) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) { violar.run(); }
                });
                else violar.run();
            }
            return h;
        }).when(historico).saveAndFlush(any(HistoricoCotacao.class));
        var r=lote.atualizar(carteira.getId());
        assertEquals(List.of(ATUALIZADA,FALHA,ATUALIZADA),r.resultados().stream().map(ResultadoAtualizacaoCotacao::resultado).toList());
        assertEquals(new BigDecimal("100.000000"),reler(b).getCotacaoAtual()); assertEquals(DESCONHECIDA,reler(b).getReferenciaTipo());
        assertEquals(1,historicos(a)); assertEquals(0,historicos(b)); assertEquals(1,historicos(c));
        assertFalse(r.resultados().get(1).falha().getMessage().contains("SQL"));
    }

    @Test void erroGlobalTardioNaoReverteSucessoConfirmado() {
        var a=aberta(Mercado.BRASIL,DESCONHECIDA,null); var b=aberta(Mercado.BRASIL,DESCONHECIDA,null);
        responder(a,0,"101");
        when(br.consultarCandidataAtualizacao(eq(b.getTicker()),any(),any(),any())).thenThrow(new IllegalStateException("Defeito"));
        assertThrows(IllegalStateException.class,()->lote.atualizar(carteira.getId()));
        assertEquals(1,historicos(a)); assertEquals(new BigDecimal("101.000000"),reler(a).getCotacaoAtual());
    }

    @Test void selecaoInicialMantidaMesmoComVendaTotalENovaCompraDuranteRede() {
        var a=aberta(Mercado.BRASIL,DESCONHECIDA,null); var b=aberta(Mercado.BRASIL,DESCONHECIDA,null);
        var nova=acao(Mercado.BRASIL,DESCONHECIDA,null); responder(b,0,"102");
        when(br.consultarCandidataAtualizacao(eq(a.getTicker()),any(),any(),any())).thenAnswer(i->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            operacao(carteira,b,TipoOperacao.VENDA,2,2);
            operacao(carteira,nova,TipoOperacao.COMPRA,1,1);
            return candidata(a,0,"101",NOW);
        });
        var r=lote.atualizar(carteira.getId());
        assertEquals(List.of(a.getId(),b.getId()),r.resultados().stream().map(ResultadoAtualizacaoCotacao::acaoId).toList());
        assertEquals(1,historicos(b)); assertEquals(0,historicos(nova));
    }

    @Test void rejeitaTransacaoEnvolventeAntesDaRede() {
        new TransactionTemplate(transactions).executeWithoutResult(tx->{
            assertThrows(IllegalStateException.class,()->lote.atualizar(carteira.getId()));
            assertThrows(IllegalStateException.class,()->individual.atualizarCotacaoCompartilhada(1L));
        });
        verifyNoInteractions(br,us,historico);
    }

    @Test void loteEPatchConcorrentesCompartilhamTentativaSemDuplaContagem() throws Exception {
        var a=aberta(Mercado.EUA,DESCONHECIDA,null);
        var entrou=new CountDownLatch(1); var liberar=new CountDownLatch(1);
        when(us.consultarCandidataAtualizacao(any(),any(),any(),any())).thenAnswer(i->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            entrou.countDown(); assertTrue(liberar.await(5,TimeUnit.SECONDS)); return candidata(a,0,"101",NOW);
        });
        var segunda=new java.util.concurrent.atomic.AtomicReference<Thread>();
        var executor=poolComSegundaThread(segunda);
        try {
            var first=executor.submit(()->lote.atualizar(carteira.getId()));
            assertTrue(entrou.await(5,TimeUnit.SECONDS));
            var patch=executor.submit(()->individual.atualizarCotacaoCompartilhada(a.getId()));
            confirmarContencaoLocal(segunda,a); liberar.countDown();
            assertEquals(ATUALIZADA,first.get(10,TimeUnit.SECONDS).resultados().get(0).resultado());
            assertEquals("COOLDOWN_SUCESSO",patch.get(10,TimeUnit.SECONDS).motivo());
            verify(us,times(1)).consultarCandidataAtualizacao(any(),any(),any(),any()); assertEquals(1,historicos(a));
        } finally { liberar.countDown(); executor.shutdownNow(); }
    }
    @Test void processamentoSequencialConcluiCommitAntesDoProximoProvider() {
        var a=aberta(Mercado.BRASIL,DESCONHECIDA,null);
        var b=aberta(Mercado.BRASIL,DESCONHECIDA,null);
        responder(a,0,"101");
        when(br.consultarCandidataAtualizacao(eq(b.getTicker()),any(),any(),any())).thenAnswer(i->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(1,historicos(a));
            assertEquals(new BigDecimal("101.000000"),reler(a).getCotacaoAtual());
            return candidata(b,0,"102",NOW);
        });
        assertEquals(new AtualizacaoCotacoesCarteiraService.Resumo(2,0,0),lote.atualizar(carteira.getId()).resumo());
    }

    @Test void loteAtualizaETentativaIndividualSeguinteUsaCooldownSemAplicacao() {
        var a=aberta(Mercado.EUA,DESCONHECIDA,null); responder(a,0,"101");
        assertEquals(ATUALIZADA,unico().resultado()); clearInvocations(us,historico,aplicacao);
        assertEquals("COOLDOWN_SUCESSO",individual.atualizarCotacaoCompartilhada(a.getId()).motivo());
        verifyNoInteractions(us,historico,aplicacao);
        var restart=novaInstancia(); clearInvocations(us,historico,aplicacao);
        assertEquals("REUSO",restart.atualizarCotacaoCompartilhada(a.getId()).motivo());
        verifyNoInteractions(us,historico,aplicacao);
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void lotesConcorrentesMesmaOuDiferentesCarteirasCompartilhamAcao(boolean outraCarteira) throws Exception {
        var a=aberta(Mercado.EUA,DESCONHECIDA,null);
        var outra=outraCarteira?carteiras.saveAndFlush(new Carteira("Outra",NOW)):carteira;
        if(outraCarteira) operacao(outra,a,TipoOperacao.COMPRA,1,1);
        var entrou=new CountDownLatch(1); var liberar=new CountDownLatch(1);
        when(us.consultarCandidataAtualizacao(any(),any(),any(),any())).thenAnswer(i->{
            entrou.countDown(); assertTrue(liberar.await(5,TimeUnit.SECONDS)); return candidata(a,0,"101",NOW);
        });
        var segunda=new java.util.concurrent.atomic.AtomicReference<Thread>();
        var pool=poolComSegundaThread(segunda);
        try {
            var first=pool.submit(()->lote.atualizar(carteira.getId())); assertTrue(entrou.await(5,TimeUnit.SECONDS));
            var second=pool.submit(()->lote.atualizar(outra.getId()));
            confirmarContencaoLocal(segunda,a); liberar.countDown();
            assertEquals(new AtualizacaoCotacoesCarteiraService.Resumo(1,0,0),first.get(10,TimeUnit.SECONDS).resumo());
            assertEquals(new AtualizacaoCotacoesCarteiraService.Resumo(0,1,0),second.get(10,TimeUnit.SECONDS).resumo());
            verify(us,times(1)).consultarCandidataAtualizacao(any(),any(),any(),any()); assertEquals(1,historicos(a));
        } finally { liberar.countDown(); pool.shutdownNow(); }
    }

    @Test void respostaAntigaTardiaDoLoteNaoSubstituiPatchPosterior() throws Exception {
        var a=aberta(Mercado.BRASIL,INSTANTE,NOW.minusHours(1));
        var entrou=new CountDownLatch(1); var liberar=new CountDownLatch(1);
        when(br.consultarCandidataAtualizacao(any(),any(),any(),any())).thenAnswer(i->{
            if(Thread.currentThread().getName().equals("lote-atrasado")) {
                entrou.countDown(); assertTrue(liberar.await(5,TimeUnit.SECONDS)); return candidata(a,1,"101",NOW.plusMinutes(2));
            }
            return candidata(a,2,"102",NOW.plusMinutes(1));
        });
        var pool=Executors.newSingleThreadExecutor(r->new Thread(r,"lote-atrasado"));
        try {
            var delayed=pool.submit(()->lote.atualizar(carteira.getId())); assertTrue(entrou.await(5,TimeUnit.SECONDS));
            assertEquals(new BigDecimal("102.000000"),individual.atualizarCotacao(a.getId()).cotacaoAtual());
            liberar.countDown(); var r=delayed.get(10,TimeUnit.SECONDS).resultados().get(0);
            assertEquals("REFERENCIA_ANTERIOR",r.motivo()); assertEquals(new BigDecimal("102.000000"),r.cotacao().preco());
            assertEquals(NOW.plusMinutes(1),r.cotacao().obtencao()); assertEquals(1,historicos(a));
        } finally { liberar.countDown(); pool.shutdownNow(); }
    }

    @Test void restartPerdeCooldownEConsultaQuandoObtencaoPermaneceExpirada() {
        var a=aberta(Mercado.EUA,DATA,NOW.minusHours(2));
        when(us.consultarCandidataAtualizacao(any(),any(),any(),any()))
                .thenReturn(candidata(a,0,"100",NOW.minusHours(1)));
        assertEquals("MESMA_OBSERVACAO",unico().motivo());
        assertEquals("COOLDOWN_SUCESSO",unico().motivo());
        var restart=new AtualizacaoCotacoesCarteiraService(posicoes,novaInstancia(),acoes);
        clearInvocations(us);
        assertEquals("MESMA_OBSERVACAO",restart.atualizar(carteira.getId()).resultados().get(0).motivo());
        verify(us,times(1)).consultarCandidataAtualizacao(any(),any(),any(),any()); assertEquals(0,historicos(a));
    }

    @Test void mesmaReferenciaPrecoExecucaoAtrasadaNaoRegrideObtencao() throws Exception {
        var a=aberta(Mercado.BRASIL,INSTANTE,NOW.minusHours(2));
        var entrou=new CountDownLatch(1); var liberar=new CountDownLatch(1);
        when(br.consultarCandidataAtualizacao(any(),any(),any(),any())).thenAnswer(i->{
            if(Thread.currentThread().getName().equals("obtencao-atrasada")) {
                entrou.countDown(); assertTrue(liberar.await(5,TimeUnit.SECONDS));
                return candidata(a,0,"100",NOW.minusMinutes(1));
            }
            return candidata(a,0,"100",NOW);
        });
        var pool=Executors.newSingleThreadExecutor(r->new Thread(r,"obtencao-atrasada"));
        try {
            var delayed=pool.submit(()->lote.atualizar(carteira.getId())); assertTrue(entrou.await(5,TimeUnit.SECONDS));
            individual.atualizarCotacao(a.getId()); liberar.countDown();
            var r=delayed.get(10,TimeUnit.SECONDS).resultados().get(0);
            assertEquals(SEM_NOVA_COTACAO,r.resultado()); assertEquals(NOW,r.cotacao().obtencao());
            assertEquals(a.getDataHoraCotacao(),r.cotacao().legado()); assertEquals(0,historicos(a));
        } finally { liberar.countDown(); pool.shutdownNow(); }
    }

    ExecutorService poolComSegundaThread(java.util.concurrent.atomic.AtomicReference<Thread> segunda) {
        var ordem=new AtomicInteger();
        return Executors.newFixedThreadPool(2,r->{
            var thread=new Thread(r,"controle-local-"+ordem.incrementAndGet());
            if(ordem.get()==2) segunda.set(thread);
            return thread;
        });
    }
    void confirmarContencaoLocal(java.util.concurrent.atomic.AtomicReference<Thread> segunda,Acao a) {
        Object[] locks=(Object[])org.springframework.test.util.ReflectionTestUtils.getField(individual,"refreshLocks");
        Object lock=locks[Math.floorMod(a.getTicker().hashCode(),locks.length)];
        var mx=java.lang.management.ManagementFactory.getThreadMXBean();
        long limite=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<limite) {
            Thread thread=segunda.get();
            var info=thread==null?null:mx.getThreadInfo(thread.getId());
            if(info!=null && info.getThreadState()==Thread.State.BLOCKED && info.getLockInfo()!=null
                    && info.getLockInfo().getIdentityHashCode()==System.identityHashCode(lock)) return;
            Thread.onSpinWait();
        }
        fail("Segunda execução deve bloquear no mesmo monitor local antes de liberar a primeira");
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void falhaExternaConcorrenteApresentaSnapshotPosteriorSemEfeitosMesmoComJpaStale(boolean contextoAntigo) throws Exception {
        var a=aberta(Mercado.EUA,DATA,NOW.minusHours(1));
        var entrou=new CountDownLatch(1); var liberar=new CountDownLatch(1);
        var failure=new ApiException(HttpStatus.GATEWAY_TIMEOUT,ErrorCodes.SERVICO_EXTERNO_TIMEOUT,"Timeout do provider");
        when(us.consultarCandidataAtualizacao(any(),any(),any(),any())).thenAnswer(i->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            entrou.countDown(); assertTrue(liberar.await(10,TimeUnit.SECONDS)); throw failure;
        });
        var pool=Executors.newSingleThreadExecutor();
        try {
            var first=pool.submit(()->{
                var contexto=contextoAntigo?emf.createEntityManager():null;
                if(contexto!=null) org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(
                        emf,new org.springframework.orm.jpa.EntityManagerHolder(contexto));
                try {
                    var r=lote.atualizar(carteira.getId());
                    if(contexto!=null) assertEquals(new BigDecimal("100.000000"),contexto.find(Acao.class,a.getId()).getCotacaoAtual());
                    return r;
                } finally {
                    if(contexto!=null) {
                        org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource(emf);
                        contexto.close();
                    }
                }
            });
            assertTrue(entrou.await(5,TimeUnit.SECONDS));
            var nova=candidata(a,1,"102",NOW);
            aplicacao.aplicar(a.getId(),nova);
            clearInvocations(aplicacao,historico,acoes);
            liberar.countDown();
            var r=first.get(10,TimeUnit.SECONDS).resultados().get(0);
            assertEquals(FALHA,r.resultado()); assertEquals(ResultadoAtualizacaoCotacao.Origem.CONSULTA,r.origem());
            assertEquals(new BigDecimal("102.000000"),r.cotacao().preco());
            assertEquals(DAY.plusDays(1),r.cotacao().data()); assertEquals(NOW,r.cotacao().obtencao());
            assertEquals(failure.getCode(),r.falha().getCode()); assertEquals(failure.getMessage(),r.falha().getMessage());
            verify(us,times(1)).consultarCandidataAtualizacao(any(),any(),any(),any());
            verifyNoInteractions(aplicacao,historico);
            verify(acoes,times(1)).lerSnapshotCotacao(a.getId());
            verify(acoes,never()).save(any()); verify(acoes,never()).saveAndFlush(any());
            assertEquals(1,historicos(a)); assertEquals(new BigDecimal("102.000000"),reler(a).getCotacaoAtual());
        } finally { liberar.countDown(); pool.shutdownNow(); }
    }

}
