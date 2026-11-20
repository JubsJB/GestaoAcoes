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
@ContextConfiguration(classes=AtribuicaoCustodiaTransacaoH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class AtribuicaoCustodiaTransacaoH2Test {
    static final OffsetDateTime REGISTRO=OffsetDateTime.parse("2026-10-24T12:00:00Z");
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    @TestConfiguration(proxyBeanMethods=false)
    @Import({TransferenciaCustodiaTransacaoH2Test.Config.class, AtribuicaoCustodiaService.class})
    static class Config { }
    @Autowired AtribuicaoCustodiaService service;
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
            var c=new Carteira("Atribuição",REGISTRO);var outra=new Carteira("Outra",REGISTRO);
            var a=new Acao("PETR4","Sintética",Mercado.BRASIL,Moeda.BRL,BigDecimal.TEN,REGISTRO);
            var outraAcao=new Acao("AAPL","Sintética",Mercado.EUA,Moeda.USD,BigDecimal.TEN,REGISTRO);
            var origem=corretora("11111111000111");var destino=corretora("22222222000122");var terceira=corretora("33333333000133");
            for(Object item:List.of(c,outra,a,outraAcao,origem,destino,terceira))e.persist(item);
            e.persist(new Operacao(c,a,null,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));
            e.flush();return new Fixture(c.getId(),outra.getId(),a.getId(),outraAcao.getId(),origem.getId(),destino.getId(),terceira.getId());
        });
        clearInvocations(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
    }
    AtribuicaoCustodiaCommand command(String quantidade) {
        return new AtribuicaoCustodiaCommand(f.carteira()," petr4 ",Mercado.BRASIL,f.destino(),new BigDecimal(quantidade),DIA);
    }
    List<List<Object>> financeiro(){return tx(e->{
        @SuppressWarnings("unchecked") List<Object[]> rows=e.createNativeQuery("SELECT id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total FROM operacao ORDER BY id").getResultList();
        return rows.stream().map(Arrays::asList).toList();
    });}
    @Test void atribui40DoGrupoNuloPreservandoCompraEConsolidado100() {
        var antes = financeiro();
        var m = service.atribuir(command("40"));
        assertNotNull(m.getId());
        assertEquals(NaturezaMovimentacaoCustodia.ATRIBUICAO, m.getNatureza());
        assertNull(m.getCorretoraOrigem());
        assertEquals(f.destino(), m.getCorretoraDestino().getId());
        assertEquals(new BigDecimal("40.000000"), m.getQuantidade());
        assertEquals(DIA, m.getDataEfetiva());
        assertEquals(REGISTRO.toInstant(), m.getDataRegistro().toInstant());
        assertEquals(antes, financeiro());
        var ordem = inOrder(carteiras, acoes, corretoras, operacoes, movimentos, estornos);
        ordem.verify(carteiras).findByIdForUpdate(f.carteira());
        ordem.verify(acoes).findByTickerAndMercado("PETR4", Mercado.BRASIL);
        ordem.verify(corretoras).findById(f.destino());
        saldos("60", "40");
        verify(movimentos, times(1)).saveAndFlush(any());
    }
    @Test void atribuiIntegralmente100SemCriarOuAlterarCorretoras() {
        var antes = financeiro();
        service.atribuir(command("100.000000"));
        saldos("0", "100");
        assertEquals(antes, financeiro());
        assertEquals(3L, tx(e -> e.createQuery("select count(c) from Corretora c", Long.class).getSingleResult()).longValue());
        assertEquals(1L, contarMovimentos());
    }

    @Test void saldoDeCorretoraCarteiraOuAcaoDistintaNaoFinanciaGrupoNulo() {
        op(f.carteira(), f.acao(), f.origem(), TipoOperacao.COMPRA, "1000", "10", DIA);
        op(f.outraCarteira(), f.acao(), null, TipoOperacao.COMPRA, "1000", "10", DIA);
        op(f.carteira(), f.outraAcao(), null, TipoOperacao.COMPRA, "1000", "10", DIA);
        var antes = financeiro();
        var erro = assertThrows(ApiException.class, () -> service.atribuir(command("101")));
        assertEquals("SALDO_ORIGEM_INSUFICIENTE", erro.getCode());
        var origem = (Map<?, ?>) erro.getDetails().get("origem");
        assertEquals("SEM_CORRETORA_INFORMADA", origem.get("categoria"));
        assertTrue(origem.containsKey("corretoraId"));
        assertNull(origem.get("corretoraId"));
        assertNull(erro.getDetails().get("evento"));
        assertEquals(0L, contarMovimentos());
        assertEquals(antes, financeiro());
        verify(movimentos, never()).saveAndFlush(any());
        verify(operacoes).listarParaReplayQuantitativo(f.carteira(), f.acao());
        verify(movimentos).listarPorCarteiraEAcao(f.carteira(), f.acao());
    }

    @ParameterizedTest @ValueSource(strings={"carteira", "acao", "destino"})
    void referenciaInexistenteNaoGravaNemAlteraOperacoes(String referencia) {
        var antes = financeiro();
        var c = new AtribuicaoCustodiaCommand(referencia.equals("carteira") ? Long.MAX_VALUE : f.carteira(),
                referencia.equals("acao") ? "AUSENTE" : "PETR4", Mercado.BRASIL,
                referencia.equals("destino") ? Long.MAX_VALUE : f.destino(), BigDecimal.ONE, DIA);
        assertThrows(ObjectNotFoundException.class, () -> service.atribuir(c));
        assertEquals(0L, contarMovimentos());
        assertEquals(antes, financeiro());
        verify(movimentos, never()).saveAndFlush(any());
    }

    @Test void entradaInvalidaNaoConsultaReferenciasNemGrava() {
        var erro = assertThrows(ApiException.class, () -> service.atribuir(command("0")));
        assertEquals("REQUEST_INVALIDO", erro.getCode());
        verifyNoInteractions(carteiras, acoes, corretoras, operacoes, movimentos, estornos);
        assertEquals(0L, contarMovimentos());
    }

    @Test void vendaPosteriorSemCorretoraInviabilizaAtribuicaoRetroativa() {
        op(f.carteira(), f.acao(), null, TipoOperacao.VENDA, "80", "15", DIA.plusDays(1));
        var antes = financeiro();
        var erro = assertThrows(ApiException.class, () -> service.atribuir(command("50")));
        assertEquals("POSICAO_CUSTODIA_INDISPONIVEL", erro.getCode());
        assertEquals("CONFLITO_HISTORICO_QUANTITATIVO", erro.getDetails().get("motivo"));
        assertEquals("FINANCEIRO", ((Map<?, ?>) erro.getDetails().get("evento")).get("dominio"));
        assertEquals(0L, contarMovimentos());
        assertEquals(antes, financeiro());
    }

    @Test void atribuicaoPosteriorAtivaEhProtegidaSemReposicionarOriginal() {
        Long id = tx(e -> {
            var original = atribuicao(e, "80", DIA.plusDays(1));
            e.persist(original);
            return original.getId();
        });
        var erro = assertThrows(ApiException.class, () -> service.atribuir(command("30")));
        assertEquals("POSICAO_CUSTODIA_INDISPONIVEL", erro.getCode());
        assertEquals("MOVIMENTACAO_CUSTODIA_INVALIDADA", erro.getDetails().get("motivo"));
        assertEquals(id, ((Map<?, ?>) erro.getDetails().get("evento")).get("id"));
        assertEquals(1L, contarMovimentos());
        verify(estornos).listarPorCarteiraEOriginais(f.carteira(), List.of(id));
        tx(e -> {
            var original = e.find(MovimentacaoCustodia.class, id);
            assertEquals("1", original.getPosicaoRacional().getNumeradorTexto());
            assertEquals("2", original.getPosicaoRacional().getDenominadorTexto());
            return null;
        });
    }

    @Test void vinculoDeEstornoExistenteEhReutilizadoSemImplementarNovoEstorno() {
        Long id = tx(e -> {
            var original = atribuicao(e, "80", DIA);
            e.persist(original);
            e.persist(new EstornoMovimentacaoCustodia(null, original, null, Clock.fixed(REGISTRO.toInstant(), ZoneOffset.UTC)));
            return original.getId();
        });
        var antes = financeiro();
        var m = service.atribuir(command("40"));
        assertEquals("1", m.getPosicaoRacional().getNumeradorTexto());
        assertEquals("3", m.getPosicaoRacional().getDenominadorTexto());
        verify(estornos).listarPorCarteiraEOriginais(f.carteira(), List.of(id));
        assertEquals(2L, contarMovimentos());
        assertEquals(1L, tx(e -> e.createQuery("select count(e) from EstornoMovimentacaoCustodia e", Long.class).getSingleResult()).longValue());
        assertEquals(antes, financeiro());
    }

    @Test void fracaoEuaEhPersistidaSemArredondamento() {
        op(f.carteira(), f.outraAcao(), null, TipoOperacao.COMPRA, "1", "10", DIA);
        var antes = financeiro();
        var c = new AtribuicaoCustodiaCommand(f.carteira(), " aapl ", Mercado.EUA, f.destino(), new BigDecimal("0.000001"), DIA);
        var m = service.atribuir(c);
        assertEquals(new BigDecimal("0.000001"), m.getQuantidade());
        tx(e -> {
            var salvo = e.find(MovimentacaoCustodia.class, m.getId());
            assertNull(salvo.getCorretoraOrigem());
            assertEquals(new BigDecimal("0.000001"), salvo.getQuantidade());
            return null;
        });
        assertEquals(antes, financeiro());
    }

    @Test void precoMedioCustoRealizadoNaoRealizadoERentabilidadePermanecemIguais() {
        op(f.carteira(), f.acao(), null, TipoOperacao.VENDA, "20", "15", DIA.plusDays(1));
        var antes = financeiro();
        var calculosAntes = calculos();
        assertEquals(new BigDecimal("80.000000"), calculosAntes.get(0));
        assertEquals(new BigDecimal("10.000000000000"), calculosAntes.get(1));
        assertEquals(new BigDecimal("800.000000000000"), calculosAntes.get(2));
        assertEquals(new BigDecimal("100.000000000000"), calculosAntes.get(3));
        assertEquals(new BigDecimal("160.000000000000"), calculosAntes.get(4));
        assertEquals(new BigDecimal("20.000000"), calculosAntes.get(5));
        service.atribuir(command("40"));
        assertEquals(antes, financeiro());
        assertEquals(calculosAntes, calculos());
    }

    @Test void atribuicaoSeguidaDeTransferenciaMantemCadeiaValidaSemAlterarCompra() {
        var antes = financeiro();
        service.atribuir(command("80"));
        context.getBean(TransferenciaCustodiaService.class).transferir(new TransferenciaCustodiaCommand(
                f.carteira(), "PETR4", Mercado.BRASIL, f.destino(), f.origem(), new BigDecimal("50"), DIA));
        tx(e -> {
            var os = operacoes.listarParaReplayQuantitativo(f.carteira(), f.acao());
            var ms = movimentos.listarPorCarteiraEAcao(f.carteira(), f.acao());
            assertEquals(2, ms.size());
            var fontes = new AdaptadorEventosQuantitativos().adaptar(f.carteira(), f.acao(), os, ms,
                    ms.stream().map(x -> VinculoEstornoConsultado.ausenciaVerificada(x.getId())).toList());
            var eventos = new NormalizadorEventosQuantitativos().normalizar(f.carteira(), f.acao(), fontes.financeiros(), fontes.custodias());
            var estado = new MotorReplayQuantitativo().reproduzir(eventos).estadoFinal();
            assertEquals(0, new BigDecimal("100").compareTo(estado.quantidadeConsolidada()));
            assertEquals(0, new BigDecimal("20").compareTo(estado.saldo(EventoQuantitativo.Grupo.semCorretora())));
            assertEquals(0, new BigDecimal("30").compareTo(estado.saldo(EventoQuantitativo.Grupo.corretora(f.destino()))));
            assertEquals(0, new BigDecimal("50").compareTo(estado.saldo(EventoQuantitativo.Grupo.corretora(f.origem()))));
            return null;
        });
        assertEquals(antes, financeiro());
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

    MovimentacaoCustodia atribuicao(EntityManager e, String quantidade, LocalDate dia) {
        return new MovimentacaoCustodia(null, e.find(Carteira.class, f.carteira()), e.find(Acao.class, f.acao()),
                NaturezaMovimentacaoCustodia.ATRIBUICAO, null, e.find(Corretora.class, f.destino()), new BigDecimal(quantidade),
                dia, dia.equals(DIA) ? 1 : 0, PosicaoRacionalCustodia.deTexto("1", "2"), Clock.fixed(REGISTRO.toInstant(), ZoneOffset.UTC));
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void falhaNaFronteiraSaveAndFlushReverteIntegralmente(boolean depoisDeFlushReal) {
        var antes=financeiro();
        var repositorioReal=mockingDetails(movimentos).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocacao->{
            if(depoisDeFlushReal)repositorioReal.answer(invocacao);
            throw new DataIntegrityViolationException("Falha injetada na fronteira saveAndFlush");
        }).when(movimentos).saveAndFlush(any(MovimentacaoCustodia.class));
        var erro = assertThrows(ApiException.class,()->service.atribuir(command("40")));
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
        assertThrows(IllegalStateException.class,()->service.atribuir(command("40")));
        assertTrue(confirmou.get());assertEquals(0L,contarMovimentos());assertEquals(antes,financeiro());
        verify(movimentos,times(1)).saveAndFlush(any(MovimentacaoCustodia.class));
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void participaDeTransacaoExternaValidaSemConfirmacaoIndependente(boolean rollback) {
        var externa=new TransactionTemplate(manager);externa.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        externa.execute(status->{assertNotNull(service.atribuir(command("40")).getId());if(rollback)status.setRollbackOnly();return null;});
        assertEquals(rollback?0L:1L,contarMovimentos());
    }
    @ParameterizedTest @ValueSource(ints={TransactionDefinition.ISOLATION_READ_UNCOMMITTED,TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionDefinition.ISOLATION_SERIALIZABLE})
    void isolamentoExternoIncompativelEhRecusadoAntesDeConsulta(int isolamento) {
        var externa=new TransactionTemplate(manager);externa.setIsolationLevel(isolamento);
        assertThrows(IllegalTransactionStateException.class,()->externa.execute(status->service.atribuir(command("40"))));
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);assertEquals(0L,contarMovimentos());
    }
    @Test void contextoReadOnlyExternoEhRecusadoAntesDeConsulta() {
        var externa=new TransactionTemplate(manager);externa.setReadOnly(true);
        assertThrows(IllegalTransactionStateException.class,()->externa.execute(status->service.atribuir(command("40"))));
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);assertEquals(0L,contarMovimentos());
    }
    @Test void escritaFuncionaSemProvidersOuClientesHttp() {
        for (String nome : context.getBeanDefinitionNames()) {
            Class<?> tipo = context.getType(nome);
            if (tipo != null) {
                assertFalse(tipo.getName().startsWith("com.projeto.integrations."), tipo.getName());
                assertFalse(tipo.getName().contains("ServletWebServer"), tipo.getName());
            }
        }
        assertNotNull(service.atribuir(command("40")).getId());
        verify(movimentos, times(1)).saveAndFlush(any());
    }
    void saldos(String nulo, String destino) {
        tx(e -> {
            var os = operacoes.listarParaReplayQuantitativo(f.carteira(), f.acao());
            var ms = movimentos.listarPorCarteiraEAcao(f.carteira(), f.acao());
            var fontes = new AdaptadorEventosQuantitativos().adaptar(f.carteira(), f.acao(), os, ms,
                    ms.stream().map(x -> VinculoEstornoConsultado.ausenciaVerificada(x.getId())).toList());
            var eventos = new NormalizadorEventosQuantitativos().normalizar(f.carteira(), f.acao(), fontes.financeiros(), fontes.custodias());
            var estado = new MotorReplayQuantitativo().reproduzir(eventos).estadoFinal();
            assertEquals(0, new BigDecimal("100").compareTo(estado.quantidadeConsolidada()));
            assertEquals(0, new BigDecimal(nulo).compareTo(estado.saldo(EventoQuantitativo.Grupo.semCorretora())));
            assertEquals(0, new BigDecimal(destino).compareTo(estado.saldo(EventoQuantitativo.Grupo.corretora(f.destino()))));
            return null;
        });
    }
    long contarMovimentos() {
        return tx(e -> e.createQuery("select count(m) from MovimentacaoCustodia m", Long.class).getSingleResult());
    }
    static Corretora corretora(String cnpj) { return new Corretora(cnpj,"Sintética",null,null,null,"00000000","Rua","1",null,"Bairro","Cidade","SP","ATIVA",REGISTRO); }
}
