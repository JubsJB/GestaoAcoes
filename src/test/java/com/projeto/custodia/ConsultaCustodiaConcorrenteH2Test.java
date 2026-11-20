package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2IsolationInitializer;
import com.projeto.entities.*;
import com.projeto.repositories.*;
import com.projeto.resources.MovimentacaoCustodiaResource;
import com.projeto.resources.exceptions.ResourceExceptionHandler;
import com.projeto.services.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.Session;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(value="custodia-h2",inheritProfiles=false)
@ContextConfiguration(classes=CadastroCustodiaRestH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ConsultaCustodiaConcorrenteH2Test {
    @Autowired ConsultaMovimentacaoCustodiaService consulta;
    @Autowired CadastroCustodiaService cadastro;
    @Autowired EstornoCustodiaService reversoes;
    @Autowired PlatformTransactionManager manager;
    @PersistenceContext EntityManager em;
    @MockitoSpyBean MovimentacaoCustodiaRepository movimentos;
    @MockitoSpyBean EstornoMovimentacaoCustodiaRepository estornos;
    Long carteira,original;
    <T>T tx(Function<EntityManager,T> f){return new TransactionTemplate(manager).execute(s->f.apply(em));}
    @BeforeEach void preparar(){reset(movimentos,estornos);tx(e->{
        for(String tipo:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","Acao","Corretora","Carteira"))e.createQuery("delete from "+tipo).executeUpdate();
        var agora=OffsetDateTime.parse("2026-10-24T12:00:00Z");var dia=LocalDate.of(2026,10,1);
        var c=new Carteira("Snapshot",agora);var ac=new Acao("AAPL","Sintética",Mercado.EUA,Moeda.USD,BigDecimal.TEN,agora);
        var a=ConsultaCustodiaRestH2Test.corretora("11111111000111");var b=ConsultaCustodiaRestH2Test.corretora("22222222000122");
        for(Object item:List.of(c,ac,a,b))e.persist(item);
        e.persist(new Operacao(c,ac,a,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,dia,1,new BigDecimal("1000")));
        var m=new MovimentacaoCustodia(null,c,ac,NaturezaMovimentacaoCustodia.TRANSFERENCIA,a,b,new BigDecimal("40"),dia,1,PosicaoRacionalCustodia.deTexto("1","2"),Clock.fixed(agora.toInstant(),ZoneOffset.UTC));
        e.persist(m);e.flush();carteira=c.getId();original=m.getId();return null;
    });}
    List<List<Object>> originais(){return tx(e->{
        @SuppressWarnings("unchecked") List<Object[]> rows=e.createNativeQuery("select * from movimentacao_custodia order by id").getResultList();return rows.stream().map(Arrays::asList).toList();
    });}
    @Test void historicoConservaSnapshotQuandoEstornoConfirmaEntreOriginaisEVinculos()throws Exception{verificar(false);}
    @Test void detalheConservaSnapshotQuandoEstornoConfirmaEntreOriginalEVinculo()throws Exception{verificar(true);}
    private void verificar(boolean detalhe)throws Exception{
        var antes=originais();var leitor=Thread.currentThread();var encontrouOriginal=new AtomicBoolean();
        var confirmou=new AtomicBoolean();var leuVinculoDepois=new AtomicBoolean();var conexaoLeitor=new AtomicReference<Connection>();var conexaoEscritor=new AtomicReference<Connection>();
        var repoMov=mockingDetails(movimentos).getMockCreationSettings().getDefaultAnswer();
        var repoEst=mockingDetails(estornos).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv->{conexaoEscritor.set(em.unwrap(Session.class).doReturningWork(c->c));return repoEst.answer(inv);}).when(estornos).saveAndFlush(any());
        org.mockito.stubbing.Answer<Object> depoisDaOriginal=inv->{
            Object carregado=repoMov.answer(inv);
            if(Thread.currentThread()==leitor && encontrouOriginal.compareAndSet(false,true)){
                assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
                conexaoLeitor.set(em.unwrap(Session.class).doReturningWork(c->{assertEquals(Connection.TRANSACTION_SERIALIZABLE,c.getTransactionIsolation());return c;}));
                var executor=Executors.newSingleThreadExecutor();
                try{
                    executor.submit(()->{
                        reversoes.estornar(new EstornoCustodiaCommand(carteira,original,"Concorrente"));
                        // Observação em nova transação depois do retorno do proxy de escrita.
                        assertEquals(1L,tx(e->e.createQuery("select count(e) from EstornoMovimentacaoCustodia e",Long.class).getSingleResult()).longValue());
                        confirmou.set(true);
                    }).get(10,TimeUnit.SECONDS);
                }finally{executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
                assertTrue(confirmou.get());assertNotSame(conexaoLeitor.get(),conexaoEscritor.get());
            }return carregado;
        };
        org.mockito.stubbing.Answer<Object> aoLerVinculo=inv->{
            if(Thread.currentThread()==leitor){assertTrue(confirmou.get());leuVinculoDepois.set(true);}
            return repoEst.answer(inv);
        };
        if(detalhe){
            doAnswer(depoisDaOriginal).when(movimentos).buscarPorCarteiraEId(carteira,original);
            doAnswer(aoLerVinculo).when(estornos).listarPorCarteiraEOriginais(eq(carteira),anyCollection());
        }else{
            doAnswer(depoisDaOriginal).when(movimentos).listarPorCarteira(carteira);
            doAnswer(aoLerVinculo).when(estornos).listarPorCarteira(carteira);
        }
        var mvc=MockMvcBuilders.standaloneSetup(new MovimentacaoCustodiaResource(consulta,cadastro)).setControllerAdvice(new ResourceExceptionHandler()).build();
        String path="/carteiras/"+carteira+"/movimentacoes-custodia"+(detalhe?"/"+original:"");
        var json=new ObjectMapper();var primeira=json.readTree(mvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertTrue(encontrouOriginal.get());assertTrue(leuVinculoDepois.get());
        var m=detalhe?primeira.get("movimentacao"):primeira.get(0);
        assertEquals("ATIVA",m.get("situacao").asText());assertTrue(m.get("estornoId").isNull());
        if(detalhe)assertTrue(primeira.get("estorno").isNull());else assertEquals(1,primeira.size());
        var segunda=json.readTree(mvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var atual=detalhe?segunda.get("movimentacao"):java.util.stream.StreamSupport.stream(segunda.spliterator(),false)
                .filter(r->r.get("tipoRegistro").asText().equals("MOVIMENTACAO")).findFirst().orElseThrow();
        assertEquals("ESTORNADA",atual.get("situacao").asText());assertFalse(atual.get("estornoId").isNull());
        if(detalhe)assertEquals(atual.get("estornoId"),segunda.get("estorno").get("id"));else{
            assertEquals(2,segunda.size());assertTrue(java.util.stream.StreamSupport.stream(segunda.spliterator(),false)
                .anyMatch(r->r.get("tipoRegistro").asText().equals("ESTORNO") && r.get("movimentacaoOriginalId").asLong()==original));
        }
        assertEquals(antes,originais());verify(estornos,times(1)).saveAndFlush(any());verify(movimentos,never()).saveAndFlush(any());
        System.out.println("CUSTODIA_SNAPSHOT tipo="+(detalhe?"DETALHE":"HISTORICO")+" commitEntreConsultas=true conexoesIndependentes=true primeiro=ATIVA segundo=ESTORNADA");
    }
}
