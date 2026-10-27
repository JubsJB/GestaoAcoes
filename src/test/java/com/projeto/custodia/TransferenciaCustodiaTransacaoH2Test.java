package com.projeto.custodia;

import com.projeto.custodia.support.*;
import com.projeto.entities.*;
import com.projeto.repositories.*;
import com.projeto.services.*;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.validation.TickerNormalizer;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Fixtures confirmadas e chamadas pelo proxy; nenhuma transação de teste envolvendo o método. */
@ExtendWith(SpringExtension.class)
@ActiveProfiles(value="custodia-h2",inheritProfiles=false)
@ContextConfiguration(classes=TransferenciaCustodiaTransacaoH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class TransferenciaCustodiaTransacaoH2Test {
    static final OffsetDateTime REGISTRO=OffsetDateTime.parse("2026-10-24T12:00:00Z");
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    @TestConfiguration(proxyBeanMethods=false)
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories(
            basePackages="com.projeto.repositories",
            excludeFilters=@ComponentScan.Filter(type=FilterType.CUSTOM, classes=ReferenciasOnly.class))
    @Import({CustodiaH2TestConfiguration.class,TransferenciaCustodiaService.class,TransferenciaCustodiaPersistenceService.class,TickerNormalizer.class})
    static class Config {
        @Bean Clock clock(){return Clock.fixed(REGISTRO.toInstant(),ZoneOffset.UTC);}
    }
    public static class ReferenciasOnly implements org.springframework.core.type.filter.TypeFilter {
        @Override public boolean match(org.springframework.core.type.classreading.MetadataReader reader,
                org.springframework.core.type.classreading.MetadataReaderFactory factory) {
            return !Set.of(CarteiraRepository.class.getName(), AcaoRepository.class.getName(),
                    CorretoraRepository.class.getName()).contains(reader.getClassMetadata().getClassName());
        }
    }
    @Autowired TransferenciaCustodiaService service;
    @Autowired PlatformTransactionManager manager;
    @Autowired org.springframework.context.ApplicationContext context;
    @PersistenceContext EntityManager em;
    @MockitoSpyBean CarteiraRepository carteiras;
    @MockitoSpyBean AcaoRepository acoes;
    @MockitoSpyBean CorretoraRepository corretoras;
    @MockitoSpyBean OperacaoRepository operacoes;
    @MockitoSpyBean MovimentacaoCustodiaRepository movimentos;
    @MockitoSpyBean EstornoMovimentacaoCustodiaRepository estornos;
    Fixture f;
    record Fixture(Long carteira,Long outraCarteira,Long acao,Long outraAcao,Long origem,Long destino,Long terceira) { }
    <T>T tx(Function<EntityManager,T> trabalho){return new TransactionTemplate(manager).execute(status->trabalho.apply(em));}
    @BeforeEach void preparar() {
        reset(movimentos);
        f=tx(e->{
            for(String tabela:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","Acao","Corretora","Carteira"))
                e.createQuery("delete from "+tabela).executeUpdate();
            var c=new Carteira("Transferência",REGISTRO);var outra=new Carteira("Outra",REGISTRO);
            var a=new Acao("PETR4","Sintética",Mercado.BRASIL,Moeda.BRL,BigDecimal.TEN,REGISTRO);
            var outraAcao=new Acao("AAPL","Sintética",Mercado.EUA,Moeda.USD,BigDecimal.TEN,REGISTRO);
            var origem=corretora("11111111000111");var destino=corretora("22222222000122");var terceira=corretora("33333333000133");
            for(Object item:List.of(c,outra,a,outraAcao,origem,destino,terceira))e.persist(item);
            e.persist(new Operacao(c,a,origem,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));
            e.flush();return new Fixture(c.getId(),outra.getId(),a.getId(),outraAcao.getId(),origem.getId(),destino.getId(),terceira.getId());
        });
        clearInvocations(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
    }
    TransferenciaCustodiaCommand command(String quantidade) {
        return new TransferenciaCustodiaCommand(f.carteira()," petr4 ",Mercado.BRASIL,f.origem(),f.destino(),new BigDecimal(quantidade),DIA);
    }
    List<List<Object>> financeiro(){return tx(e->{
        @SuppressWarnings("unchecked") List<Object[]> rows=e.createNativeQuery("SELECT id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total FROM operacao ORDER BY id").getResultList();
        return rows.stream().map(Arrays::asList).toList();
    });}
    @Test void escritaFuncionaSemProvidersOuClientesHttp() {
        for (String nome : context.getBeanDefinitionNames()) {
            Class<?> tipo = context.getType(nome);
            if (tipo != null) {
                assertFalse(tipo.getName().startsWith("com.projeto.integrations."), tipo.getName());
                assertFalse(tipo.getName().contains("ServletWebServer"), tipo.getName());
            }
        }
        assertNotNull(service.transferir(command("40")).getId());
        verify(movimentos, times(1)).saveAndFlush(any());
    }
    @Test void transferencia40Preserva100TotaisCompraEValoresFinanceirosComLockPrimeiro() {
        var antes=financeiro();
        var m=service.transferir(command("40"));
        assertNotNull(m.getId());assertEquals(NaturezaMovimentacaoCustodia.TRANSFERENCIA,m.getNatureza());
        assertEquals(new BigDecimal("40.000000"),m.getQuantidade());assertEquals(DIA,m.getDataEfetiva());
        assertEquals(REGISTRO.toInstant(),m.getDataRegistro().toInstant());
        assertEquals(antes,financeiro());
        var ordem=inOrder(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
        ordem.verify(carteiras).findByIdForUpdate(f.carteira());ordem.verify(acoes).findByTickerAndMercado("PETR4",Mercado.BRASIL);
        ordem.verify(corretoras).findById(f.origem());ordem.verify(corretoras).findById(f.destino());
        tx(e->{
            var os=operacoes.listarParaReplayQuantitativo(f.carteira(),f.acao());
            var ms=movimentos.listarPorCarteiraEAcao(f.carteira(),f.acao());
            assertEquals(1,ms.size());
            var fontes=new AdaptadorEventosQuantitativos().adaptar(f.carteira(),f.acao(),os,ms,
                    ms.stream().map(x->VinculoEstornoConsultado.ausenciaVerificada(x.getId())).toList());
            var eventos=new NormalizadorEventosQuantitativos().normalizar(f.carteira(),f.acao(),fontes.financeiros(),fontes.custodias());
            var estado=new MotorReplayQuantitativo().reproduzir(eventos).estadoFinal();
            assertEquals(0,new BigDecimal("100").compareTo(estado.quantidadeConsolidada()));
            assertEquals(0,new BigDecimal("60").compareTo(estado.saldo(EventoQuantitativo.Grupo.corretora(f.origem()))));
            assertEquals(0,new BigDecimal("40").compareTo(estado.saldo(EventoQuantitativo.Grupo.corretora(f.destino()))));
            return null;
        });
    }
    @Test void saldoInsuficienteRetornaDiagnosticoDaCandidataSemGravar() {
        var antes=financeiro();
        var erro=assertThrows(ApiException.class,()->service.transferir(command("101")));
        assertEquals("SALDO_ORIGEM_INSUFICIENTE",erro.getCode());
        assertEquals(409,erro.getStatus().value());assertTrue(erro.getDetails().containsKey("evento"));
        assertNull(erro.getDetails().get("evento"));
        // A busca informa o primeiro espaço sem saldo, anterior à compra, e não o saldo máximo do dia.
        assertEquals(0,BigDecimal.ZERO.compareTo((BigDecimal)erro.getDetails().get("quantidadeDisponivel")));
        verify(movimentos,never()).saveAndFlush(any());assertEquals(antes,financeiro());
        assertEquals(0L,tx(e->e.createQuery("select count(m) from MovimentacaoCustodia m",Long.class).getSingleResult()).longValue());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void falhaNaFronteiraSaveAndFlushReverteIntegralmente(boolean depoisDeFlushReal) {
        var antes=financeiro();
        var repositorioReal=mockingDetails(movimentos).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocacao->{
            if(depoisDeFlushReal)repositorioReal.answer(invocacao);
            throw new DataIntegrityViolationException("Falha injetada na fronteira saveAndFlush");
        }).when(movimentos).saveAndFlush(any(MovimentacaoCustodia.class));
        var erro = assertThrows(ApiException.class,()->service.transferir(command("40")));
        assertEquals("INTEGRIDADE_DADOS_VIOLADA", erro.getCode()); assertNull(erro.getCause());
        assertEquals(0L,contarMovimentos());assertEquals(antes,financeiro());
        verify(movimentos,times(1)).saveAndFlush(any(MovimentacaoCustodia.class));
    }
    @Test void falhaInjetadaEmBeforeCommitReverteFlushJaRealizado() {
        var antes=financeiro();var confirmou=new AtomicBoolean();
        var repositorioReal=mockingDetails(movimentos).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocacao->{
            Object salvo=repositorioReal.answer(invocacao);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void beforeCommit(boolean readOnly) {
                    confirmou.set(true);throw new IllegalStateException("Falha injetada em beforeCommit");
                }
            });return salvo;
        }).when(movimentos).saveAndFlush(any(MovimentacaoCustodia.class));
        assertThrows(IllegalStateException.class,()->service.transferir(command("40")));
        assertTrue(confirmou.get());assertEquals(0L,contarMovimentos());assertEquals(antes,financeiro());
        verify(movimentos,times(1)).saveAndFlush(any(MovimentacaoCustodia.class));
    }
    long contarMovimentos(){return tx(e->e.createQuery("select count(m) from MovimentacaoCustodia m",Long.class).getSingleResult());}
    @ParameterizedTest @ValueSource(strings={"carteira","acao","origem","destino"})
    void referenciasInexistentesMantem404SemEscrita(String referencia) {
        var c=new TransferenciaCustodiaCommand(referencia.equals("carteira")?Long.MAX_VALUE:f.carteira(),
                referencia.equals("acao")?"AUSENTE":"PETR4",Mercado.BRASIL,
                referencia.equals("origem")?Long.MAX_VALUE:f.origem(),referencia.equals("destino")?Long.MAX_VALUE:f.destino(),
                BigDecimal.ONE,DIA);
        assertThrows(ObjectNotFoundException.class,()->service.transferir(c));
        verify(movimentos,never()).saveAndFlush(any());assertEquals(0L,contarMovimentos());
    }
    @Test void entradaInvalidaPrevaleceSobreReferenciaAusenteSemConsultarBanco() {
        var c=new TransferenciaCustodiaCommand(f.carteira(),"PETR4",Mercado.BRASIL,f.origem(),Long.MAX_VALUE,BigDecimal.ZERO,DIA);
        assertEquals("REQUEST_INVALIDO",assertThrows(ApiException.class,()->service.transferir(c)).getCode());
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
    }
    @Test void origemIgualAoDestinoNaoAdquireLockNemConsultaReferencias() {
        var c=new TransferenciaCustodiaCommand(f.carteira(),"PETR4",Mercado.BRASIL,f.origem(),f.origem(),BigDecimal.ONE,DIA);
        assertEquals("REQUEST_INVALIDO",assertThrows(ApiException.class,()->service.transferir(c)).getCode());
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
    }
    @Test void vendaPosteriorInviabilizaTransferenciaSemAlterarFinanceiro() {
        op(f.carteira(),f.acao(),f.origem(),TipoOperacao.VENDA,"80",DIA.plusDays(1));var antes=financeiro();
        var erro=assertThrows(ApiException.class,()->service.transferir(command("50")));
        assertEquals("POSICAO_CUSTODIA_INDISPONIVEL",erro.getCode());
        assertEquals("CONFLITO_HISTORICO_QUANTITATIVO",erro.getDetails().get("motivo"));
        assertEquals(antes,financeiro());assertEquals(0L,contarMovimentos());
    }
    @Test void custodiaPosteriorAtivaEhProtegidaEIdentificada() {
        Long original=tx(e->{var m=mov(e,f.origem(),f.destino(),"80",DIA.plusDays(1),0,"1","2");e.persist(m);return m.getId();});
        var c=new TransferenciaCustodiaCommand(f.carteira(),"PETR4",Mercado.BRASIL,f.origem(),f.terceira(),new BigDecimal("30"),DIA);
        var erro=assertThrows(ApiException.class,()->service.transferir(c));
        assertEquals("POSICAO_CUSTODIA_INDISPONIVEL",erro.getCode());assertEquals("MOVIMENTACAO_CUSTODIA_INVALIDADA",erro.getDetails().get("motivo"));
        var evento=(Map<?,?>)erro.getDetails().get("evento");assertEquals("CUSTODIA",evento.get("dominio"));assertEquals(original,evento.get("id"));
        assertEquals(1L,contarMovimentos());
        tx(e->{assertEquals("1",e.find(MovimentacaoCustodia.class,original).getPosicaoRacional().getNumeradorTexto());return null;});
    }
    @Test void estornoConsultadoNeutralizaOriginalMasPreservaSuaPosicaoReservada() {
        Long original=tx(e->{var m=mov(e,f.origem(),f.destino(),"80",DIA,1,"1","2");e.persist(m);
            e.persist(new EstornoMovimentacaoCustodia(null,m,null,Clock.fixed(REGISTRO.toInstant(),ZoneOffset.UTC)));return m.getId();});
        var nova=service.transferir(command("40"));
        assertEquals("1",nova.getPosicaoRacional().getNumeradorTexto());assertEquals("3",nova.getPosicaoRacional().getDenominadorTexto());
        verify(estornos).listarPorCarteiraEOriginais(f.carteira(),List.of(original));assertEquals(2L,contarMovimentos());
        assertEquals(1L,tx(e->e.createQuery("select count(e) from EstornoMovimentacaoCustodia e",Long.class).getSingleResult()).longValue());
    }
    @Test void limiteDeChaveRetornaDiagnosticoSemGravarOuTentarOutroEspaco() {
        String denominador = "9".repeat(512);
        tx(e -> { e.persist(mov(e, f.origem(), f.destino(), "50", DIA, 1, "1", denominador)); return null; });
        var antes = financeiro();
        var erro = assertThrows(ApiException.class, () -> service.transferir(command("10")));
        assertEquals("LIMITE_POSICAO_CUSTODIA", erro.getCode());
        assertEquals("LIMITE_POSICAO_CUSTODIA", erro.getDetails().get("motivo"));
        assertTrue(erro.getDetails().containsKey("evento"));
        assertNull(erro.getDetails().get("evento"));
        assertEquals(1L, contarMovimentos());
        assertEquals(antes, financeiro());
        verify(movimentos, never()).saveAndFlush(any());
    }
    @Test void saldoDeOutraCarteiraOuAcaoNaoFinanciaCandidata() {
        op(f.outraCarteira(),f.acao(),f.origem(),TipoOperacao.COMPRA,"1000",DIA);
        op(f.carteira(),f.outraAcao(),f.origem(),TipoOperacao.COMPRA,"1000",DIA);
        var erro=assertThrows(ApiException.class,()->service.transferir(command("101")));
        assertEquals("SALDO_ORIGEM_INSUFICIENTE",erro.getCode());assertEquals(0L,contarMovimentos());
        verify(operacoes).listarParaReplayQuantitativo(f.carteira(),f.acao());verify(movimentos).listarPorCarteiraEAcao(f.carteira(),f.acao());
    }
    @Test void precisaoEuaPersisteUltimaCasaSemArredondamento() {
        op(f.carteira(),f.outraAcao(),f.origem(),TipoOperacao.COMPRA,"1",DIA);
        var antes=financeiro();var c=new TransferenciaCustodiaCommand(f.carteira()," aapl ",Mercado.EUA,f.origem(),f.destino(),new BigDecimal("0.000001"),DIA);
        var m=service.transferir(c);assertEquals(new BigDecimal("0.000001"),m.getQuantidade());assertEquals(antes,financeiro());
        tx(e->{assertEquals(new BigDecimal("0.000001"),e.find(MovimentacaoCustodia.class,m.getId()).getQuantidade());return null;});
    }
    @Test void consolidadoInvalidoPreservaCodigoFinanceiroExistente() {
        op(f.carteira(),f.acao(),f.origem(),TipoOperacao.VENDA,"110",DIA.plusDays(1));var antes=financeiro();
        var erro=assertThrows(ApiException.class,()->service.transferir(command("1")));
        assertEquals("POSICAO_INSUFICIENTE",erro.getCode());assertEquals("FINANCEIRO",((Map<?,?>)erro.getDetails().get("evento")).get("dominio"));
        assertEquals(0L,contarMovimentos());assertEquals(antes,financeiro());
    }
    @Test void deficitLegadoIndependenteNaoBloqueiaTransferenciaValida() {
        op(f.carteira(),f.acao(),f.destino(),TipoOperacao.COMPRA,"10",DIA.plusDays(1));
        op(f.carteira(),f.acao(),f.destino(),TipoOperacao.VENDA,"20",DIA.plusDays(2));
        op(f.carteira(),f.acao(),f.destino(),TipoOperacao.COMPRA,"10",DIA.plusDays(3));var antes=financeiro();
        var c=new TransferenciaCustodiaCommand(f.carteira(),"PETR4",Mercado.BRASIL,f.origem(),f.terceira(),new BigDecimal("5"),DIA);
        assertNotNull(service.transferir(c).getId());assertEquals(antes,financeiro());assertEquals(1L,contarMovimentos());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void participaDeTransacaoExternaValidaSemConfirmacaoIndependente(boolean rollback) {
        var externa=new TransactionTemplate(manager);externa.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        externa.execute(status->{assertNotNull(service.transferir(command("40")).getId());if(rollback)status.setRollbackOnly();return null;});
        assertEquals(rollback?0L:1L,contarMovimentos());
    }
    @ParameterizedTest @ValueSource(ints={TransactionDefinition.ISOLATION_READ_UNCOMMITTED,TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionDefinition.ISOLATION_SERIALIZABLE})
    void isolamentoExternoIncompativelEhRecusadoAntesDeConsulta(int isolamento) {
        var externa=new TransactionTemplate(manager);externa.setIsolationLevel(isolamento);
        assertThrows(IllegalTransactionStateException.class,()->externa.execute(status->service.transferir(command("40"))));
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);assertEquals(0L,contarMovimentos());
    }
    @Test void contextoReadOnlyExternoEhRecusadoAntesDeConsulta() {
        var externa=new TransactionTemplate(manager);externa.setReadOnly(true);
        assertThrows(IllegalTransactionStateException.class,()->externa.execute(status->service.transferir(command("40"))));
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);assertEquals(0L,contarMovimentos());
    }
    void op(Long carteira,Long acao,Long corretora,TipoOperacao tipo,String quantidade,LocalDate dia) {
        tx(e->{var q=new BigDecimal(quantidade);e.persist(new Operacao(e.find(Carteira.class,carteira),e.find(Acao.class,acao),
                e.find(Corretora.class,corretora),tipo,q,BigDecimal.TEN,dia,1,q.multiply(BigDecimal.TEN)));return null;});
    }
    MovimentacaoCustodia mov(EntityManager e,Long origem,Long destino,String quantidade,LocalDate dia,int ancora,String n,String d) {
        return new MovimentacaoCustodia(null,e.find(Carteira.class,f.carteira()),e.find(Acao.class,f.acao()),NaturezaMovimentacaoCustodia.TRANSFERENCIA,
                e.find(Corretora.class,origem),e.find(Corretora.class,destino),new BigDecimal(quantidade),dia,ancora,
                PosicaoRacionalCustodia.deTexto(n,d),Clock.fixed(REGISTRO.toInstant(),ZoneOffset.UTC));
    }
    static Corretora corretora(String cnpj){return new Corretora(cnpj,"Sintética",null,null,null,"00000000","Rua","1",null,"Bairro","Cidade","SP","ATIVA",REGISTRO);}
}
