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
@ContextConfiguration(classes=OperacaoCustodiaTransacaoH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class OperacaoCustodiaTransacaoH2Test {
    static final OffsetDateTime REGISTRO=OffsetDateTime.parse("2026-10-24T12:00:00Z");
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    @TestConfiguration(proxyBeanMethods=false)
    @Import({TransferenciaCustodiaTransacaoH2Test.Config.class, OperacaoService.class, OperacaoPersistenceService.class,
            HistoricoQuantitativoCustodia.class, CalculadoraPosicao.class, com.projeto.mappers.OperacaoMapper.class, com.projeto.services.exceptions.ConstraintNameExtractor.class})
    static class Config { }
    @Autowired OperacaoService service;
    @Autowired OperacaoPersistenceService writer;
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
        reset(movimentos, operacoes, carteiras);
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
    com.projeto.dto.OperacaoResponse registrar(TipoOperacao tipo, Long corretora, String quantidade, LocalDate data) {
        var q = new BigDecimal(quantidade);
        return service.cadastrar(tipo == TipoOperacao.COMPRA
                ? new com.projeto.dto.OperacaoCompraCreateRequest(f.carteira(), "PETR4", Mercado.BRASIL, corretora, q, data, BigDecimal.TEN)
                : new com.projeto.dto.OperacaoVendaCreateRequest(f.carteira(), "PETR4", Mercado.BRASIL, corretora, q, data, BigDecimal.TEN));
    }
    List<List<Object>> financeiro(){return tx(e->{
        @SuppressWarnings("unchecked") List<Object[]> rows=e.createNativeQuery("SELECT id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total FROM operacao ORDER BY id").getResultList();
        return rows.stream().map(Arrays::asList).toList();
    });}
    @ParameterizedTest @ValueSource(booleans={false,true})
    void compraComOuSemCorretoraMantemPrecoManualEOrdem(boolean semCorretora) {
        var salvo = registrar(TipoOperacao.COMPRA, semCorretora ? null : f.origem(), "20", DIA);
        assertEquals(2, salvo.ordemNoDia());
        assertEquals(new BigDecimal("200.000000000000"), salvo.valorTotal());
        assertEquals(semCorretora ? null : f.origem(), salvo.corretoraId());
    }
    @Test void compraEmOutroGrupoNaoRegularizaCustodiaHistoricaInvalida() {
        tx(e -> { e.persist(new MovimentacaoCustodia(null,e.find(Carteira.class,f.carteira()),e.find(Acao.class,f.acao()),
                NaturezaMovimentacaoCustodia.TRANSFERENCIA,e.find(Corretora.class,f.origem()),e.find(Corretora.class,f.destino()),
                new BigDecimal("110"),DIA,1,PosicaoRacionalCustodia.deTexto("1","2"),Clock.fixed(REGISTRO.toInstant(),ZoneOffset.UTC))); return null; });
        var antes = financeiro();
        var erro = assertThrows(ApiException.class, () -> registrar(TipoOperacao.COMPRA, f.terceira(), "20", DIA));
        assertEquals("MOVIMENTACAO_CUSTODIA_INVALIDADA", erro.getCode());
        assertEquals(antes, financeiro()); verify(operacoes, never()).saveAndFlush(any());
    }
    @Test void compraRetroativaReduzDeficitLegadoSemEditarVenda() {
        op(f.carteira(), f.acao(), f.destino(), TipoOperacao.VENDA, "20", "15", DIA.plusDays(2));
        var vendaAntes = financeiro().get(1);
        var salvo = registrar(TipoOperacao.COMPRA, f.destino(), "20", DIA.plusDays(1));
        assertEquals(DIA.plusDays(1), salvo.dataOperacao());
        assertTrue(financeiro().contains(vendaAntes));
    }

    @Test void vendaParcialEIntegralConsomemSomenteGrupoInformado() {
        registrar(TipoOperacao.VENDA, f.origem(), "40", DIA);
        var ultima = registrar(TipoOperacao.VENDA, f.origem(), "60", DIA);
        assertEquals(3, ultima.ordemNoDia());
        assertEquals(3, financeiro().size());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void vendaEmGrupoSemSaldoNaoUsaConsolidadoDeOutraCorretora(boolean nulo) {
        var antes = financeiro();
        var erro = assertThrows(ApiException.class, () -> registrar(TipoOperacao.VENDA, nulo ? null : f.destino(), "1", DIA));
        assertEquals("SALDO_CORRETORA_INSUFICIENTE", erro.getCode());
        assertNull(erro.getDetails().get("evento"));
        assertEquals(antes, financeiro());
    }
    @Test void vendaNulaComSaldoProprioEhAceita() {
        op(f.carteira(), f.acao(), null, TipoOperacao.COMPRA, "40", "10", DIA);
        assertNull(registrar(TipoOperacao.VENDA, null, "40", DIA).corretoraId());
    }
    @Test void consolidadoInsuficientePrecedeFalhaLocal() {
        var erro = assertThrows(ApiException.class, () -> registrar(TipoOperacao.VENDA, f.destino(), "101", DIA));
        assertEquals("POSICAO_INSUFICIENTE", erro.getCode()); verify(operacoes, never()).saveAndFlush(any());
    }
    @Test void vendaRetroativaProtegeCustodiaPosterior() {
        custodia(f.origem(), f.destino(), "80", DIA.plusDays(1), 0);
        var antes = financeiro();
        var erro = assertThrows(ApiException.class, () -> registrar(TipoOperacao.VENDA, f.origem(), "30", DIA));
        assertEquals("MOVIMENTACAO_CUSTODIA_INVALIDADA", erro.getCode());
        assertEquals("CUSTODIA", ((Map<?,?>)erro.getDetails().get("evento")).get("dominio"));
        assertEquals(antes, financeiro());
    }
    @Test void vendaRetroativaNaoPodeCriarDeficitIntermediarioMesmoComFinalPositivo() {
        op(f.carteira(), f.acao(), f.destino(), TipoOperacao.COMPRA, "100", "10", DIA.plusDays(1));
        op(f.carteira(), f.acao(), f.origem(), TipoOperacao.VENDA, "90", "10", DIA.plusDays(2));
        custodia(f.destino(), f.origem(), "20", DIA.plusDays(3), 0);
        var erro = assertThrows(ApiException.class, () -> registrar(TipoOperacao.VENDA, f.origem(), "20", DIA));
        assertEquals("CONFLITO_HISTORICO_QUANTITATIVO", erro.getCode());
        assertEquals(0, new BigDecimal("10").compareTo((BigDecimal)erro.getDetails().get("deficitDepois")));
    }
    @Test void vendaNovaPreservaVendaLegadaDeficitariaIndependente() {
        op(f.carteira(), f.acao(), f.destino(), TipoOperacao.COMPRA, "10", "10", DIA.plusDays(1));
        op(f.carteira(), f.acao(), f.destino(), TipoOperacao.VENDA, "20", "10", DIA.plusDays(2));
        op(f.carteira(), f.acao(), f.destino(), TipoOperacao.COMPRA, "10", "10", DIA.plusDays(3));
        var anteriores = financeiro();
        registrar(TipoOperacao.VENDA, f.origem(), "5", DIA);
        assertTrue(financeiro().containsAll(anteriores));
    }
    @Test void saldoDeOutraCarteiraOuAcaoNaoFinanciaVenda() {
        op(f.outraCarteira(), f.acao(), f.destino(), TipoOperacao.COMPRA, "1000", "10", DIA);
        op(f.carteira(), f.outraAcao(), f.destino(), TipoOperacao.COMPRA, "1000", "10", DIA);
        var erro = assertThrows(ApiException.class, () -> registrar(TipoOperacao.VENDA, f.destino(), "1", DIA));
        assertEquals("SALDO_CORRETORA_INSUFICIENTE", erro.getCode());
        verify(operacoes).listarParaReplayQuantitativo(f.carteira(), f.acao());
    }
    @Test void compraPosteriorNaDataNaoReposicionaCustodia() {
        Long id = custodia(f.origem(), f.destino(), "40", DIA, 1);
        assertEquals(2, registrar(TipoOperacao.COMPRA, f.origem(), "10", DIA).ordemNoDia());
        tx(e -> { var m=e.find(MovimentacaoCustodia.class,id);assertEquals(1,m.getOrdemFinanceiraAncora());
            assertEquals("1",m.getPosicaoRacional().getNumeradorTexto());assertEquals("2",m.getPosicaoRacional().getDenominadorTexto());return null; });
    }
    @Test void falhaReconhecidaNaConfirmacaoForaDoMetodoEhTraduzidaERevertida() {
        var antes = financeiro();
        var real = mockingDetails(operacoes).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv -> { var salvo=real.answer(inv);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) {
                    throw new RuntimeException("senha=sentinela",
                            new java.sql.SQLException("SQL sentinela", "40001"));
                }
            }); return salvo;
        }).when(operacoes).saveAndFlush(any());
        var erro=assertThrows(ApiException.class,()->registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA));
        assertEquals("CONFLITO_TRANSACIONAL_TRANSITORIO",erro.getCode());assertNull(erro.getCause());
        assertFalse((erro.getMessage()+erro.getDetails()).contains("sentinela"));
        assertEquals(antes,financeiro());verify(operacoes,times(1)).saveAndFlush(any());
    }
    @Test void flushComIntegridadeDesconhecidaNaoExpoeCausaNemConsultaDepois() {
        var causa = new org.hibernate.exception.ConstraintViolationException("senha=sentinela",
                new java.sql.SQLException("SQL sentinela"), "constraint_desconhecida");
        doThrow(new DataIntegrityViolationException("sentinela",causa)).when(operacoes).saveAndFlush(any());
        var antes=financeiro();
        var erro=assertThrows(ApiException.class,()->registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA));
        assertEquals("INTEGRIDADE_DADOS_VIOLADA",erro.getCode());assertNull(erro.getCause());
        assertFalse((erro.getMessage()+erro.getDetails()).contains("sentinela"));
        var ordem=inOrder(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
        ordem.verify(operacoes).saveAndFlush(any());ordem.verifyNoMoreInteractions();
        assertEquals(antes,financeiro());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void falhaDeFlushReverteLancamentoSemRetry(boolean depoisDoFlush) {
        var antes=financeiro();var real=mockingDetails(operacoes).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv->{if(depoisDoFlush)real.answer(inv);throw new DataIntegrityViolationException("sentinela");}).when(operacoes).saveAndFlush(any());
        var erro=assertThrows(ApiException.class,()->registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA));
        assertEquals("INTEGRIDADE_DADOS_VIOLADA",erro.getCode());assertEquals(antes,financeiro());verify(operacoes,times(1)).saveAndFlush(any());
    }
    @Test void beforeCommitDesconhecidoNaoEhRotuladoComoTransitorioEReverte() {
        var antes=financeiro();var falha=new IllegalStateException("falha controlada");
        var real=mockingDetails(operacoes).getMockCreationSettings().getDefaultAnswer();
        doAnswer(inv->{var salvo=real.answer(inv);TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void beforeCommit(boolean readOnly){throw falha;}
        });return salvo;}).when(operacoes).saveAndFlush(any());
        assertSame(falha,assertThrows(IllegalStateException.class,()->registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA)));
        assertEquals(antes,financeiro());verify(operacoes,times(1)).saveAndFlush(any());
    }
    @ParameterizedTest @ValueSource(strings={"40001","40P01","55P03"})
    void conflitoReconhecidoAoAdquirirLockNaoRepeteEscrita(String estado) {
        doThrow(new RuntimeException("sentinela",new java.sql.SQLException("segredo",estado))).when(carteiras).findByIdForUpdate(f.carteira());
        var erro=assertThrows(ApiException.class,()->registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA));
        assertEquals("CONFLITO_TRANSACIONAL_TRANSITORIO",erro.getCode());assertNull(erro.getCause());
        verify(carteiras,times(1)).findByIdForUpdate(f.carteira());verifyNoInteractions(acoes,corretoras,operacoes,movimentos,estornos);
    }
    @ParameterizedTest @ValueSource(strings={"spring","jpa","timeout","hibernate"})
    void tipoDeFalhaDeLockConhecidoEhTraduzido(String tipo) {
        RuntimeException falha=switch(tipo){
            case "spring" -> new org.springframework.dao.CannotAcquireLockException("sentinela");
            case "jpa" -> new jakarta.persistence.PessimisticLockException("sentinela");
            case "timeout" -> new jakarta.persistence.LockTimeoutException("sentinela");
            default -> new org.hibernate.exception.LockAcquisitionException("sentinela",new java.sql.SQLException("sentinela"));
        };
        doThrow(falha).when(carteiras).findByIdForUpdate(f.carteira());
        var erro=assertThrows(ApiException.class,()->registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA));
        assertEquals("CONFLITO_TRANSACIONAL_TRANSITORIO",erro.getCode());assertNull(erro.getCause());
    }
    @Test void sqlStateDesconhecidoNaoEhPresumidoTransitorio() {
        var falha=new RuntimeException("falha controlada",new java.sql.SQLException("controlada","ZZZZZ"));
        doThrow(falha).when(carteiras).findByIdForUpdate(f.carteira());
        assertSame(falha,assertThrows(RuntimeException.class,()->registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA)));
    }
    @Test void entradaInternaInvalidaNaoConsultaReferencias() {
        var c=new OperacaoPersistenceCommand(f.carteira(),"PETR4",Mercado.BRASIL,f.origem(),TipoOperacao.COMPRA,BigDecimal.ZERO,BigDecimal.TEN,DIA);
        assertEquals("REQUEST_INVALIDO",assertThrows(ApiException.class,()->writer.persistir(c)).getCode());
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
    }
    @Test void lockEhPrimeiraConsultaMesmoPelaFachada() {
        registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA);
        var primeira=java.util.stream.Stream.of(carteiras,acoes,corretoras,operacoes,movimentos,estornos)
                .flatMap(r->mockingDetails(r).getInvocations().stream()).min(Comparator.comparingInt(org.mockito.invocation.Invocation::getSequenceNumber)).orElseThrow();
        assertSame(carteiras,primeira.getMock());assertEquals("findByIdForUpdate",primeira.getMethod().getName());
        verify(operacoes,times(1)).listarParaReplayQuantitativo(f.carteira(),f.acao());
    }
    @Test void formulasFinanceirasPermanecemCorretasComCustodia() {
        custodia(f.origem(),f.destino(),"40",DIA,1);
        service.cadastrar(new com.projeto.dto.OperacaoCompraCreateRequest(f.carteira(),"PETR4",Mercado.BRASIL,f.origem(),new BigDecimal("100"),DIA,new BigDecimal("20")));
        service.cadastrar(new com.projeto.dto.OperacaoVendaCreateRequest(f.carteira(),"PETR4",Mercado.BRASIL,f.origem(),new BigDecimal("40"),DIA.plusDays(1),new BigDecimal("25")));
        var r=calculos();
        assertEquals(new BigDecimal("160.000000"),r.get(0));assertEquals(new BigDecimal("15.000000000000"),r.get(1));
        assertEquals(new BigDecimal("2400.000000000000"),r.get(2));assertEquals(new BigDecimal("400.000000000000"),r.get(3));
        assertEquals(new BigDecimal("-480.000000000000"),r.get(4));assertEquals(new BigDecimal("-20.000000"),r.get(5));
    }
    @Test void vinculoDeEstornoNeutralizaOriginalNaValidacaoDaVenda() {
        Long id=custodia(f.origem(),f.destino(),"40",DIA,1);
        tx(e->{e.persist(new EstornoMovimentacaoCustodia(null,e.find(MovimentacaoCustodia.class,id),null,Clock.fixed(REGISTRO.toInstant(),ZoneOffset.UTC)));return null;});
        registrar(TipoOperacao.VENDA,f.origem(),"100",DIA);verify(estornos).listarPorCarteiraEOriginais(f.carteira(),List.of(id));
    }
    @Test void escritaNaoDependeDeProviders() {
        for(String nome:context.getBeanDefinitionNames()){var t=context.getType(nome);if(t!=null)assertFalse(t.getName().startsWith("com.projeto.integrations."));}
        registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA);verify(operacoes,times(1)).saveAndFlush(any());
    }
    List<BigDecimal> calculos() {
        return tx(e -> {
            var calc = new CalculadoraPosicao();
            var resultado = calc.reproduzir(operacoes.listarParaReplayQuantitativo(f.carteira(), f.acao()));
            assertTrue(resultado.valido());
            var p = resultado.posicao();
            var valorAtual = calc.calcularValorAtual(p.quantidadeAtual(), new BigDecimal("12"));
            var naoRealizado = calc.calcularResultadoNaoRealizado(valorAtual, p.custoPosicao());
            var rentabilidade = new CalculadoraRentabilidade().calcularPercentual(naoRealizado, p.custoPosicao());
            return List.of(p.quantidadeAtual(), p.precoMedio(), p.custoPosicao(), resultado.resultadoRealizado(), naoRealizado, rentabilidade);
        });
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void participaDeTransacaoExternaValidaSemConfirmacaoIndependente(boolean rollback) {
        var externa=new TransactionTemplate(manager);externa.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        externa.execute(status->{assertNotNull(registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA).id());if(rollback)status.setRollbackOnly();return null;});
        assertEquals(rollback?1L:2L,contarOperacoes());
    }
    @ParameterizedTest @ValueSource(ints={TransactionDefinition.ISOLATION_READ_UNCOMMITTED,TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionDefinition.ISOLATION_SERIALIZABLE})
    void isolamentoExternoIncompativelEhRecusadoAntesDeConsulta(int isolamento) {
        var externa=new TransactionTemplate(manager);externa.setIsolationLevel(isolamento);
        assertThrows(IllegalTransactionStateException.class,()->externa.execute(status->registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA)));
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);assertEquals(1L,contarOperacoes());
    }
    @Test void contextoReadOnlyExternoEhRecusadoAntesDeConsulta() {
        var externa=new TransactionTemplate(manager);externa.setReadOnly(true);
        assertThrows(IllegalTransactionStateException.class,()->externa.execute(status->registrar(TipoOperacao.COMPRA,f.origem(),"10",DIA)));
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);assertEquals(1L,contarOperacoes());
    }
    long contarOperacoes() { return financeiro().size(); }
    Long custodia(Long origem, Long destino, String q, LocalDate d, int ancora) {
        return tx(e -> { var m = new MovimentacaoCustodia(null,e.find(Carteira.class,f.carteira()),e.find(Acao.class,f.acao()),
                NaturezaMovimentacaoCustodia.TRANSFERENCIA,e.find(Corretora.class,origem),e.find(Corretora.class,destino),
                new BigDecimal(q),d,ancora,PosicaoRacionalCustodia.deTexto("1","2"),Clock.fixed(REGISTRO.toInstant(),ZoneOffset.UTC));
            e.persist(m);return m.getId(); });
    }
    void op(Long carteira, Long acao, Long corretora, TipoOperacao tipo, String quantidade, String preco, LocalDate dia) {
        tx(e -> {
            var q = new BigDecimal(quantidade);
            var valor = new BigDecimal(preco);
            int ordem = e.createQuery("select coalesce(max(o.ordemNoDia),0) from Operacao o where o.carteira.id=:c and o.acao.id=:a and o.dataOperacao=:d", Integer.class)
                    .setParameter("c", carteira).setParameter("a", acao).setParameter("d", dia).getSingleResult() + 1;
            e.persist(new Operacao(e.find(Carteira.class, carteira), e.find(Acao.class, acao),
                    corretora == null ? null : e.find(Corretora.class, corretora), tipo, q, valor, dia, ordem, q.multiply(valor)));
            return null;
        });
    }

    static Corretora corretora(String cnpj) {return new Corretora(cnpj,"Sintética",null,null,null,"00000000","Rua","1",null,"Bairro","Cidade","SP","ATIVA",REGISTRO);}
}
