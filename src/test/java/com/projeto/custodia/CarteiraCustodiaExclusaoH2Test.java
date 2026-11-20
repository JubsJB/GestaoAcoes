package com.projeto.custodia;

import com.projeto.custodia.support.CustodiaH2IsolationInitializer;
import com.projeto.entities.*;
import com.projeto.repositories.*;
import com.projeto.services.CarteiraService;
import com.projeto.services.exceptions.ApiException;
import com.projeto.mappers.CarteiraMapper;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.core.type.classreading.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(value="custodia-h2",inheritProfiles=false)
@ContextConfiguration(classes=CarteiraCustodiaExclusaoH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class CarteiraCustodiaExclusaoH2Test {
    static final OffsetDateTime AGORA=OffsetDateTime.parse("2026-10-24T12:00Z");
    @TestConfiguration(proxyBeanMethods=false)
    @Import({TransferenciaCustodiaTransacaoH2Test.Config.class,CarteiraService.class,com.projeto.services.CarteiraExclusaoPersistenceService.class,CarteiraMapper.class})
    @EnableJpaRepositories(basePackages="com.projeto.repositories",excludeFilters=@ComponentScan.Filter(type=FilterType.CUSTOM,classes=SomenteSnapshot.class))
    static class Config { }
    public static class SomenteSnapshot implements TypeFilter {
        public boolean match(MetadataReader r,MetadataReaderFactory f) {return !r.getClassMetadata().getClassName().equals(SnapshotCarteiraRepository.class.getName());}
    }
    @Autowired CarteiraService service;
    @Autowired PlatformTransactionManager manager;
    @PersistenceContext EntityManager em;
    @MockitoSpyBean CarteiraRepository carteiras;
    @MockitoSpyBean OperacaoRepository operacoes;
    @MockitoSpyBean SnapshotCarteiraRepository snapshots;
    @MockitoSpyBean MovimentacaoCustodiaRepository movimentos;
    @MockitoSpyBean EstornoMovimentacaoCustodiaRepository estornos;
    Long carteira,outra,acao,origem,destino;
    <T>T tx(Function<EntityManager,T> trabalho){return new TransactionTemplate(manager).execute(s->trabalho.apply(em));}
    @BeforeEach void preparar() {
        reset(carteiras);
        tx(e->{
            for(String tabela:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","SnapshotCarteira","Acao","Corretora","Carteira"))
                e.createQuery("delete from "+tabela).executeUpdate();
            var c=new Carteira("Alvo",AGORA);var o=new Carteira("Outra",AGORA);
            var a=new Acao("PETR4","Sintética",Mercado.BRASIL,Moeda.BRL,BigDecimal.TEN,AGORA);
            var br1=corretora("11111111000111");var br2=corretora("22222222000122");
            for(Object v:List.of(c,o,a,br1,br2))e.persist(v);
            e.flush();carteira=c.getId();outra=o.getId();acao=a.getId();origem=br1.getId();destino=br2.getId();return null;
        });
        clearInvocations(carteiras,operacoes,snapshots,movimentos,estornos);
    }
    @Test void transacaoExternaReadOnlyEhRejeitadaAntesDeConsultarBanco() {
        var externa=new TransactionTemplate(manager);
        externa.setReadOnly(true);
        externa.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                ()->externa.execute(status->{service.excluir(carteira);return null;}));
        verifyNoInteractions(carteiras,operacoes,snapshots,movimentos,estornos);
        assertTrue(existe(carteira));
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void participaDeTransacaoExternaValidaSemConfirmarIndependente(boolean rollback) {
        var externa=new TransactionTemplate(manager);
        externa.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        externa.execute(status->{service.excluir(carteira);if(rollback)status.setRollbackOnly();return null;});
        assertEquals(rollback,existe(carteira));assertTrue(existe(outra));
    }

    @ParameterizedTest @ValueSource(ints={1,4,8})
    void isolamentoExternoIncompativelEhRejeitadoAntesDoLock(int isolamento) {
        var externa=new TransactionTemplate(manager);externa.setIsolationLevel(isolamento);
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                ()->externa.execute(status->{service.excluir(carteira);return null;}));
        verifyNoInteractions(carteiras,operacoes,snapshots,movimentos,estornos);assertTrue(existe(carteira));
    }

    @Test void transacaoPropriaDeclaraProtocoloAntesDaPrimeiraConsulta() {
        var real=mockingDetails(carteiras).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv->{
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Integer.valueOf(2),org.springframework.transaction.support.TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            return real.answer(inv);
        }).when(carteiras).findByIdForUpdate(carteira);
        service.excluir(carteira);assertFalse(existe(carteira));verify(carteiras,times(1)).delete(any());
    }

    @Test void transferenciaBloqueiaExclusaoSemApagarAuditoria() {
        movimentacao(NaturezaMovimentacaoCustodia.TRANSFERENCIA,false);
        var erro=assertThrows(ApiException.class,()->service.excluir(carteira));
        assertEquals("CARTEIRA_POSSUI_MOVIMENTACOES_CUSTODIA",erro.getCode());
        assertEquals(409,erro.getStatus().value());assertEquals(Map.of("carteiraId",carteira),erro.getDetails());
        assertTrue(existe(carteira));assertEquals(1L,contar("MovimentacaoCustodia"));
    }
    @Test void carteiraVaziaEhExcluidaComLockPrimeiroEPreservaReferencias() {
        var outraAntes=tx(e->{var c=e.find(Carteira.class,outra);return List.of(c.getId(),c.getNome(),c.getDataCriacao());});
        service.excluir(carteira);
        var ordem=inOrder(carteiras,operacoes,snapshots,movimentos,estornos);
        ordem.verify(carteiras).findByIdForUpdate(carteira);
        ordem.verify(operacoes).existsByCarteiraId(carteira);
        ordem.verify(snapshots).existsByCarteiraId(carteira);
        ordem.verify(movimentos).existsByCarteira_Id(carteira);
        ordem.verify(estornos).existsByMovimentacaoOriginal_Carteira_Id(carteira);
        ordem.verify(carteiras).delete(any(Carteira.class));ordem.verifyNoMoreInteractions();
        var primeira=java.util.stream.Stream.of(carteiras,operacoes,snapshots,movimentos,estornos)
                .flatMap(r->mockingDetails(r).getInvocations().stream())
                .min(Comparator.comparingInt(org.mockito.invocation.Invocation::getSequenceNumber)).orElseThrow();
        assertSame(carteiras,primeira.getMock());assertEquals("findByIdForUpdate",primeira.getMethod().getName());
        assertFalse(existe(carteira));
        assertThrows(com.projeto.services.exceptions.ObjectNotFoundException.class,()->service.buscarPorId(carteira));
        assertEquals(outraAntes,tx(e->{var c=e.find(Carteira.class,outra);return List.of(c.getId(),c.getNome(),c.getDataCriacao());}));
        assertEquals(1L,contar("Acao"));assertEquals(2L,contar("Corretora"));
    }

    @ParameterizedTest @ValueSource(strings={"ATRIBUICAO","TRANSFERENCIA_ESTORNADA","ATRIBUICAO_ESTORNADA"})
    void qualquerOriginalInclusiveEstornadaPreservaAuditoria(String caso) {
        boolean estornada=caso.endsWith("_ESTORNADA");
        movimentacao(caso.startsWith("ATRIBUICAO")?NaturezaMovimentacaoCustodia.ATRIBUICAO:NaturezaMovimentacaoCustodia.TRANSFERENCIA,estornada);
        var antes=auditoria();
        var erro=assertThrows(ApiException.class,()->service.excluir(carteira));
        assertEquals("CARTEIRA_POSSUI_MOVIMENTACOES_CUSTODIA",erro.getCode());
        assertEquals(antes,auditoria());assertTrue(existe(carteira));
        verify(movimentos).existsByCarteira_Id(carteira);
        verify(movimentos,never()).listarPorCarteira(any());verify(estornos,never()).listarPorCarteira(any());
        verify(carteiras,never()).delete(any());
        assertEquals(estornada,tx(e->estornos.existsByMovimentacaoOriginal_Carteira_Id(carteira)).booleanValue());
        assertFalse(tx(e->estornos.existsByMovimentacaoOriginal_Carteira_Id(outra)).booleanValue());
    }

    @ParameterizedTest @ValueSource(strings={"operacao","snapshot"})
    void financeiroESnapshotPrecedemCustodiaSemConsultarSeuHistorico(String caso) {
        movimentacao(NaturezaMovimentacaoCustodia.TRANSFERENCIA,true);
        tx(e->{
            e.persist(new SnapshotCarteira(e.find(Carteira.class,carteira),AGORA));
            if(caso.equals("operacao"))e.persist(new Operacao(e.find(Carteira.class,carteira),e.find(Acao.class,acao),
                    e.find(Corretora.class,origem),TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,
                    LocalDate.of(2026,10,1),1,new BigDecimal("1000")));
            return null;
        });
        var antes=auditoria();
        var erro=assertThrows(ApiException.class,()->service.excluir(carteira));
        assertEquals(caso.equals("operacao")?"CARTEIRA_POSSUI_OPERACOES":"CARTEIRA_POSSUI_SNAPSHOTS",erro.getCode());
        assertEquals(Map.of("carteiraId",carteira),erro.getDetails());
        verifyNoInteractions(movimentos,estornos);
        if(caso.equals("operacao"))verifyNoInteractions(snapshots);
        assertTrue(existe(carteira));assertEquals(antes,auditoria());assertEquals(1L,contar("SnapshotCarteira"));
    }

    @Test void historicoDeOutraCarteiraNaoImpedeExclusaoDaVazia() {
        movimentacao(NaturezaMovimentacaoCustodia.ATRIBUICAO,true);
        var antes=auditoria();service.excluir(outra);
        assertFalse(existe(outra));assertTrue(existe(carteira));assertEquals(antes,auditoria());
        verify(movimentos).existsByCarteira_Id(outra);verify(estornos).existsByMovimentacaoOriginal_Carteira_Id(outra);
    }

    @Test void carteiraInexistenteNaoConsultaOutrosRepositorios() {
        assertThrows(com.projeto.services.exceptions.ObjectNotFoundException.class,()->service.excluir(Long.MAX_VALUE));
        verifyNoInteractions(operacoes,snapshots,movimentos,estornos);verify(carteiras,never()).delete(any());
    }

    @ParameterizedTest @ValueSource(strings={"antes","aposFlush","beforeCommit"})
    void falhaDePersistenciaReverteExclusaoIntegralmente(String etapa) {
        var antes=tx(e->{var c=e.find(Carteira.class,carteira);return List.of(c.getId(),c.getNome(),c.getDataCriacao());});
        var real=mockingDetails(carteiras).getMockCreationSettings().getDefaultAnswer();
        var falha=new IllegalStateException("Falha controlada de exclusão");
        doAnswer(inv->{
            if(etapa.equals("antes"))throw falha;
            real.answer(inv);em.flush();
            if(etapa.equals("aposFlush"))throw falha;
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization(){
                        @Override public void beforeCommit(boolean readOnly){throw falha;}
                    });return null;
        }).when(carteiras).delete(any(Carteira.class));
        assertSame(falha,assertThrows(IllegalStateException.class,()->service.excluir(carteira)));
        assertTrue(existe(carteira));assertTrue(existe(outra));
        assertEquals(antes,tx(e->{var c=e.find(Carteira.class,carteira);return List.of(c.getId(),c.getNome(),c.getDataCriacao());}));
        verify(carteiras,times(1)).delete(any(Carteira.class));
    }

    @Test void fkRestritivaImpedeRemocaoMesmoSeConsultasDeExistenciaForemInconclusivas() {
        movimentacao(NaturezaMovimentacaoCustodia.TRANSFERENCIA,true);
        var antes=auditoria();
        doReturn(false).when(movimentos).existsByCarteira_Id(carteira);
        doReturn(false).when(estornos).existsByMovimentacaoOriginal_Carteira_Id(carteira);
        var erro = assertThrows(ApiException.class,()->service.excluir(carteira));
        assertEquals("INTEGRIDADE_DADOS_VIOLADA", erro.getCode()); assertNull(erro.getCause());
        assertTrue(existe(carteira));assertEquals(antes,auditoria());
    }

    Map<String,List<List<Object>>> auditoria() {
        return tx(e->{
            @SuppressWarnings("unchecked") List<Object[]> ms=e.createNativeQuery("select id,carteira_id,acao_id,natureza,corretora_origem_id,corretora_destino_id,quantidade,data_efetiva,ordem_financeira_ancora,posicao_numerador,posicao_denominador,data_registro from movimentacao_custodia order by id").getResultList();
            @SuppressWarnings("unchecked") List<Object[]> es=e.createNativeQuery("select id,movimentacao_original_id,motivo,data_registro from estorno_movimentacao_custodia order by id").getResultList();
            return Map.of("originais",ms.stream().map(Arrays::asList).toList(),"estornos",es.stream().map(Arrays::asList).toList());
        });
    }
    Long movimentacao(NaturezaMovimentacaoCustodia natureza,boolean estornada) {
        return tx(e->{var m=new MovimentacaoCustodia(null,e.find(Carteira.class,carteira),e.find(Acao.class,acao),natureza,
                natureza==NaturezaMovimentacaoCustodia.ATRIBUICAO?null:e.find(Corretora.class,origem),e.find(Corretora.class,destino),
                new BigDecimal("40"),LocalDate.of(2026,10,1),0,PosicaoRacionalCustodia.deTexto("1","2"),Clock.fixed(AGORA.toInstant(),ZoneOffset.UTC));
            e.persist(m);if(estornada)e.persist(new EstornoMovimentacaoCustodia(null,m,"Auditoria",Clock.fixed(AGORA.toInstant(),ZoneOffset.UTC)));return m.getId();});
    }
    boolean existe(Long id){return tx(e->e.find(Carteira.class,id)!=null);}
    long contar(String entidade){return tx(e->e.createQuery("select count(x) from "+entidade+" x",Long.class).getSingleResult());}
    static Corretora corretora(String cnpj){return new Corretora(cnpj,"Sintética",null,null,null,"00000000","Rua","1",null,"Bairro","Cidade","SP","ATIVA",AGORA);}
}
