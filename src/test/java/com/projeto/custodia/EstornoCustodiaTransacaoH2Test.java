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
@ContextConfiguration(classes=EstornoCustodiaTransacaoH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class EstornoCustodiaTransacaoH2Test {
    static final OffsetDateTime REGISTRO=OffsetDateTime.parse("2026-10-24T12:00:00Z");
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    @TestConfiguration(proxyBeanMethods=false)
    @Import({TransferenciaCustodiaTransacaoH2Test.Config.class, AtribuicaoCustodiaService.class, EstornoCustodiaService.class})
    static class Config { }
    @Autowired EstornoCustodiaService service;
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
    Long original;
    record Fixture(Long carteira,Long outraCarteira,Long acao,Long outraAcao,Long origem,Long destino,Long terceira) { }
    <T>T tx(Function<EntityManager,T> trabalho){return new TransactionTemplate(manager).execute(status->trabalho.apply(em));}
    @BeforeEach void preparar() {
        reset(estornos);
        f=tx(e->{
            for(String tabela:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","Acao","Corretora","Carteira"))
                e.createQuery("delete from "+tabela).executeUpdate();
            var c=new Carteira("Atribuição",REGISTRO);var outra=new Carteira("Outra",REGISTRO);
            var a=new Acao("PETR4","Sintética",Mercado.BRASIL,Moeda.BRL,BigDecimal.TEN,REGISTRO);
            var outraAcao=new Acao("AAPL","Sintética",Mercado.EUA,Moeda.USD,BigDecimal.TEN,REGISTRO);
            var origem=corretora("11111111000111");var destino=corretora("22222222000122");var terceira=corretora("33333333000133");
            for(Object item:List.of(c,outra,a,outraAcao,origem,destino,terceira))e.persist(item);
            e.persist(new Operacao(c,a,origem,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));
            e.flush();return new Fixture(c.getId(),outra.getId(),a.getId(),outraAcao.getId(),origem.getId(),destino.getId(),terceira.getId());
        });
        original = tx(e -> {
            var m = new MovimentacaoCustodia(null, e.find(Carteira.class, f.carteira()), e.find(Acao.class, f.acao()),
                    NaturezaMovimentacaoCustodia.TRANSFERENCIA, e.find(Corretora.class, f.origem()),
                    e.find(Corretora.class, f.destino()), new BigDecimal("40"), DIA, 1,
                    PosicaoRacionalCustodia.deTexto("1", "2"), Clock.fixed(REGISTRO.minusDays(2).toInstant(), ZoneOffset.UTC));
            e.persist(m); return m.getId();
        });
        clearInvocations(carteiras,acoes,corretoras,operacoes,movimentos,estornos);
    }
    EstornoCustodiaCommand command(String motivo) { return new EstornoCustodiaCommand(f.carteira(), original, motivo); }
    List<List<Object>> financeiro(){return tx(e->{
        @SuppressWarnings("unchecked") List<Object[]> rows=e.createNativeQuery("SELECT id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total FROM operacao ORDER BY id").getResultList();
        return rows.stream().map(Arrays::asList).toList();
    });}
    @Test void estornoDeTransferenciaNeutralizaDesdeOriginalEPreservaAuditoria() {
        var financeiroAntes = financeiro();
        var originalAntes = originais();
        var estorno = service.estornar(command("  acerto interno  "));
        assertNotNull(estorno.getId());
        assertEquals(original, estorno.getMovimentacaoOriginalId());
        assertEquals("  acerto interno  ", estorno.getMotivo());
        assertEquals(REGISTRO.toInstant(), estorno.getDataRegistro().toInstant());
        assertNotEquals(REGISTRO.minusDays(2).toInstant(), estorno.getDataRegistro().toInstant());
        var primeira = java.util.stream.Stream.of(carteiras, acoes, corretoras, operacoes, movimentos, estornos)
                .flatMap(repo -> mockingDetails(repo).getInvocations().stream())
                .min(Comparator.comparingInt(org.mockito.invocation.Invocation::getSequenceNumber)).orElseThrow();
        assertSame(carteiras, primeira.getMock());
        assertEquals("findByIdForUpdate", primeira.getMethod().getName());
        assertEquals(originalAntes, originais());
        assertEquals(financeiroAntes, financeiro());
        var ordem = inOrder(carteiras, movimentos, estornos, operacoes);
        ordem.verify(carteiras).findByIdForUpdate(f.carteira());
        ordem.verify(movimentos).buscarPorCarteiraEId(f.carteira(), original);
        saldos("100", "0");
        assertEquals(1L, contarEstornos());
    }
    @Test void duplicidadeEhRecusadaAntesDeLerOuSimularFinanceiro() {
        service.estornar(command(null));
        clearInvocations(carteiras, movimentos, estornos, operacoes);
        var erro = assertThrows(ApiException.class, () -> service.estornar(command("segunda")));
        assertEquals("ESTORNO_DUPLICADO", erro.getCode());
        verifyNoInteractions(operacoes);
        verify(movimentos, never()).listarPorCarteiraEAcao(anyLong(), anyLong());
        verify(estornos, never()).saveAndFlush(any());
        assertEquals(1L, contarEstornos());
    }

    @Test void vendaPosteriorFinanciadaPelaOriginalImpedeNeutralizacao() {
        tx(e -> {
            e.persist(new Operacao(e.find(Carteira.class, f.carteira()), e.find(Acao.class, f.acao()),
                    e.find(Corretora.class, f.destino()), TipoOperacao.VENDA, new BigDecimal("30"),
                    new BigDecimal("15"), DIA.plusDays(1), 1, new BigDecimal("450")));
            return null;
        });
        var antes = financeiro();
        var auditoria = originais();
        var erro = assertThrows(ApiException.class, () -> service.estornar(command(null)));
        assertEquals("CONFLITO_HISTORICO_QUANTITATIVO", erro.getCode());
        assertEquals("FINANCEIRO", ((Map<?, ?>) erro.getDetails().get("evento")).get("dominio"));
        assertEquals(0, new BigDecimal("30").compareTo((BigDecimal) erro.getDetails().get("deficitDepois")));
        assertEquals(0L, contarEstornos());
        assertEquals(antes, financeiro());
        assertEquals(auditoria, originais());
    }

    @Test void unicidadeConhecidaEhTraduzidaSemExporSqlOuCausa() {
        var sql = new java.sql.SQLException("jdbc:segredo senha=sentinela", "23505");
        var hibernate = new org.hibernate.exception.ConstraintViolationException("SQL sentinela", sql,
                "uk_estorno_movimentacao_custodia_original");
        doThrow(new DataIntegrityViolationException("sentinela", hibernate)).when(estornos).saveAndFlush(any());
        var erro = assertThrows(ApiException.class, () -> service.estornar(command(null)));
        assertEquals("ESTORNO_DUPLICADO", erro.getCode());
        assertNull(erro.getCause());
        assertFalse((erro.getMessage() + erro.getDetails()).contains("sentinela"));
        assertFalse(erro.getDetails().toString().contains("uk_estorno"));
        var ordem = inOrder(carteiras, acoes, corretoras, operacoes, movimentos, estornos);
        ordem.verify(estornos).saveAndFlush(any());
        ordem.verifyNoMoreInteractions();
        assertEquals(0L, contarEstornos());
    }

    @ParameterizedTest @ValueSource(strings={"nulo", "vazio", "branco", "limite"})
    void motivoOpcionalEhNormalizadoSemAlterarTextoValido(String caso) {
        String motivo = switch(caso) { case "nulo" -> null; case "vazio" -> ""; case "branco" -> " \t\n "; default -> " x".repeat(500); };
        var salvo = service.estornar(command(motivo));
        assertEquals(caso.equals("limite") ? motivo : null, salvo.getMotivo());
        tx(e -> { assertEquals(salvo.getMotivo(), e.find(EstornoMovimentacaoCustodia.class, salvo.getId()).getMotivo()); return null; });
    }

    @Test void originalAusenteOuDeOutraCarteiraTemMesmo404SemRevelarDuplicidade() {
        service.estornar(command(null));
        clearInvocations(estornos, operacoes);
        var ausente = assertThrows(ObjectNotFoundException.class, () -> service.estornar(
                new EstornoCustodiaCommand(f.outraCarteira(), Long.MAX_VALUE, null)));
        var fora = assertThrows(ObjectNotFoundException.class, () -> service.estornar(
                new EstornoCustodiaCommand(f.outraCarteira(), original, null)));
        assertEquals(ausente.getMessage(), fora.getMessage());
        assertEquals("Movimentação não encontrada", fora.getMessage());
        verifyNoInteractions(estornos, operacoes);
    }

    @Test void carteiraAusenteFalhaNoPrimeiroLockSemConsultarOriginal() {
        assertThrows(ObjectNotFoundException.class, () -> service.estornar(new EstornoCustodiaCommand(Long.MAX_VALUE, original, null)));
        verify(carteiras).findByIdForUpdate(Long.MAX_VALUE);
        verifyNoInteractions(movimentos, estornos, operacoes);
    }

    @Test void consolidadoNegativoPreservaPrioridadeECodigoFinanceiro() {
        op(f.carteira(), f.acao(), f.origem(), TipoOperacao.VENDA, "101", "10", DIA.plusDays(1));
        var erro = assertThrows(ApiException.class, () -> service.estornar(command(null)));
        assertEquals("POSICAO_INSUFICIENTE", erro.getCode());
        assertEquals("FINANCEIRO", ((Map<?, ?>) erro.getDetails().get("evento")).get("dominio"));
        assertEquals(0L, contarEstornos()); verify(estornos, never()).saveAndFlush(any());
    }

    @Test void duplicidadeTemPrecedenciaSobreHistoricoFinanceiroInvalido() {
        service.estornar(command(null));
        op(f.carteira(), f.acao(), f.origem(), TipoOperacao.VENDA, "101", "10", DIA.plusDays(1));
        clearInvocations(operacoes);
        var erro = assertThrows(ApiException.class, () -> service.estornar(command(null)));
        assertEquals("ESTORNO_DUPLICADO", erro.getCode());
        verifyNoInteractions(operacoes);
        assertEquals(1L, contarEstornos());
    }

    @Test void estornoDeAtribuicaoRecuperaGrupoNuloSemAlterarCompra() {
        original = tx(e -> {
            e.remove(e.find(MovimentacaoCustodia.class, original)); e.flush();
            e.createQuery("delete from Operacao").executeUpdate();
            var carteira = e.find(Carteira.class, f.carteira()); var acao = e.find(Acao.class, f.acao());
            e.persist(new Operacao(carteira, acao, null, TipoOperacao.COMPRA, new BigDecimal("100"), BigDecimal.TEN, DIA, 1, new BigDecimal("1000")));
            var m = new MovimentacaoCustodia(null, carteira, acao, NaturezaMovimentacaoCustodia.ATRIBUICAO,
                    null, e.find(Corretora.class, f.destino()), new BigDecimal("40"), DIA, 1,
                    PosicaoRacionalCustodia.deTexto("1", "2"), Clock.fixed(REGISTRO.minusDays(2).toInstant(), ZoneOffset.UTC));
            e.persist(m); return m.getId();
        });
        var antes = financeiro(); var auditoria = originais();
        service.estornar(command(null));
        tx(e -> {
            var ms = movimentos.listarPorCarteiraEAcao(f.carteira(), f.acao());
            var es = estornos.listarPorCarteiraEAcao(f.carteira(), f.acao());
            var fontes = new AdaptadorEventosQuantitativos().adaptar(f.carteira(), f.acao(),
                    operacoes.listarParaReplayQuantitativo(f.carteira(), f.acao()), ms,
                    List.of(VinculoEstornoConsultado.vinculoPresente(original, es.get(0))));
            var eventos = new NormalizadorEventosQuantitativos().normalizar(f.carteira(), f.acao(), fontes.financeiros(), fontes.custodias());
            var estado = new MotorReplayQuantitativo().reproduzir(eventos).estadoFinal();
            assertEquals(0, new BigDecimal("100").compareTo(estado.saldo(EventoQuantitativo.Grupo.semCorretora())));
            assertEquals(0, BigDecimal.ZERO.compareTo(estado.saldo(EventoQuantitativo.Grupo.corretora(f.destino()))));
            assertEquals(0, new BigDecimal("100").compareTo(estado.quantidadeConsolidada())); return null;
        });
        assertEquals(antes, financeiro()); assertEquals(auditoria, originais());
    }

    @Test void custodiaPosteriorDependenteImpedeEstornoSemCascata() {
        var dependente = context.getBean(TransferenciaCustodiaService.class).transferir(new TransferenciaCustodiaCommand(
                f.carteira(), "PETR4", Mercado.BRASIL, f.destino(), f.terceira(), new BigDecimal("30"), DIA.plusDays(1)));
        var antes = originais();
        var erro = assertThrows(ApiException.class, () -> service.estornar(command(null)));
        assertEquals("MOVIMENTACAO_CUSTODIA_INVALIDADA", erro.getCode());
        assertEquals(dependente.getId(), ((Map<?, ?>) erro.getDetails().get("evento")).get("id"));
        assertEquals(0L, contarEstornos()); assertEquals(antes, originais());
    }

    @Test void deficitLegadoAgravadoEhRecusado() {
        op(f.carteira(), f.acao(), f.destino(), TipoOperacao.VENDA, "50", "10", DIA.plusDays(1));
        var erro = assertThrows(ApiException.class, () -> service.estornar(command(null)));
        assertEquals("CONFLITO_HISTORICO_QUANTITATIVO", erro.getCode());
        assertEquals(0, new BigDecimal("10").compareTo((BigDecimal) erro.getDetails().get("deficitAntes")));
        assertEquals(0, new BigDecimal("50").compareTo((BigDecimal) erro.getDetails().get("deficitDepois")));
        assertEquals(0L, contarEstornos());
    }

    @Test void deficitProlongadoEhDetectadoNoCheckpointOriginalPreservado() {
        original = tx(e -> {
            e.remove(e.find(MovimentacaoCustodia.class, original)); e.flush();
            var m = new MovimentacaoCustodia(null, e.find(Carteira.class, f.carteira()), e.find(Acao.class, f.acao()),
                    NaturezaMovimentacaoCustodia.TRANSFERENCIA, e.find(Corretora.class, f.origem()), e.find(Corretora.class, f.destino()),
                    new BigDecimal("40"), DIA.plusDays(1), 0, PosicaoRacionalCustodia.deTexto("1", "2"),
                    Clock.fixed(REGISTRO.minusDays(2).toInstant(), ZoneOffset.UTC));
            e.persist(m); return m.getId();
        });
        op(f.carteira(), f.acao(), f.destino(), TipoOperacao.VENDA, "10", "10", DIA);
        op(f.carteira(), f.acao(), f.destino(), TipoOperacao.COMPRA, "20", "10", DIA.plusDays(2));
        var erro = assertThrows(ApiException.class, () -> service.estornar(command(null)));
        assertEquals("CONFLITO_HISTORICO_QUANTITATIVO", erro.getCode());
        assertEquals(original, ((Map<?, ?>) erro.getDetails().get("evento")).get("id"));
        assertEquals("CUSTODIA", ((Map<?, ?>) erro.getDetails().get("evento")).get("dominio"));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) erro.getDetails().get("deficitAntes")));
        assertEquals(0, new BigDecimal("10").compareTo((BigDecimal) erro.getDetails().get("deficitDepois")));
        assertEquals(0L, contarEstornos());
    }

    @Test void legadoIndependenteEOutrosContextosNaoBloqueiamEstorno() {
        op(f.carteira(), f.acao(), f.terceira(), TipoOperacao.VENDA, "10", "10", DIA.plusDays(1));
        op(f.outraCarteira(), f.acao(), f.origem(), TipoOperacao.VENDA, "1000", "10", DIA);
        op(f.carteira(), f.outraAcao(), null, TipoOperacao.VENDA, "1000", "10", DIA);
        var antes = financeiro();
        clearInvocations(operacoes, movimentos);
        service.estornar(command(null));
        verify(operacoes).listarParaReplayQuantitativo(f.carteira(), f.acao());
        verify(movimentos).listarPorCarteiraEAcao(f.carteira(), f.acao());
        assertEquals(1L, contarEstornos()); assertEquals(antes, financeiro());
    }

    @Test void quantidadeECalculosFinanceirosComVendaPermanecemIguais() {
        op(f.carteira(), f.acao(), f.origem(), TipoOperacao.VENDA, "20", "15", DIA.plusDays(1));
        var antes = financeiro(); var resultado = calculos(); var auditoria = originais();
        assertEquals(new BigDecimal("80.000000"), resultado.get(0));
        assertEquals(new BigDecimal("10.000000000000"), resultado.get(1));
        assertEquals(new BigDecimal("800.000000000000"), resultado.get(2));
        assertEquals(new BigDecimal("100.000000000000"), resultado.get(3));
        assertEquals(new BigDecimal("160.000000000000"), resultado.get(4));
        assertEquals(new BigDecimal("20.000000"), resultado.get(5));
        service.estornar(command(null));
        assertEquals(antes, financeiro()); assertEquals(resultado, calculos()); assertEquals(auditoria, originais());
    }

    @ParameterizedTest @ValueSource(strings={"outra", "parecida", "semNome", "semHibernate", "somenteSqlState"})
    void integridadeDesconhecidaNaoEhClassificadaComoDuplicidade(String caso) {
        String constraint = switch(caso) { case "parecida" -> "uk_estorno_movimentacao_custodia_original_extra"; case "semNome" -> null; default -> "outra_constraint"; };
        Throwable causa = caso.equals("semHibernate") ? new IllegalStateException("sentinela")
                : caso.equals("somenteSqlState") ? new java.sql.SQLException("sentinela", "23505")
                : new org.hibernate.exception.ConstraintViolationException("sentinela", new java.sql.SQLException("senha=sentinela", "23505"), constraint);
        doThrow(new DataIntegrityViolationException("sentinela", causa)).when(estornos).saveAndFlush(any());
        var erro = assertThrows(ApiException.class, () -> service.estornar(command(null)));
        assertEquals("INTEGRIDADE_DADOS_VIOLADA", erro.getCode()); assertNull(erro.getCause());
        assertFalse((erro.getMessage() + erro.getDetails()).contains("sentinela"));
        assertTrue(erro.getDetails().isEmpty());
        var ordem = inOrder(carteiras, acoes, corretoras, operacoes, movimentos, estornos);
        ordem.verify(estornos).saveAndFlush(any()); ordem.verifyNoMoreInteractions();
        assertEquals(0L, contarEstornos());
    }

    @Test void escritaNaoDependeDeProvidersNemRepetePersistencia() {
        for (String nome : context.getBeanDefinitionNames()) {
            Class<?> tipo = context.getType(nome);
            if (tipo != null) assertFalse(tipo.getName().startsWith("com.projeto.integrations."), tipo.getName());
        }
        service.estornar(command(null)); verify(estornos, times(1)).saveAndFlush(any());
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

    @ParameterizedTest @ValueSource(booleans={false,true})
    void falhaNaFronteiraSaveAndFlushReverteIntegralmente(boolean depoisDeFlushReal) {
        var antes=financeiro(); var auditoria=originais();
        var repositorioReal=mockingDetails(estornos).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocacao->{
            if(depoisDeFlushReal)repositorioReal.answer(invocacao);
            throw new DataIntegrityViolationException("Falha injetada na fronteira saveAndFlush");
        }).when(estornos).saveAndFlush(any(EstornoMovimentacaoCustodia.class));
        assertThrows(ApiException.class,()->service.estornar(command(null)));
        assertEquals(0L,contarEstornos());assertEquals(antes,financeiro()); assertEquals(auditoria, originais());
        verify(estornos,times(1)).saveAndFlush(any(EstornoMovimentacaoCustodia.class));
    }
    @Test void falhaInjetadaEmBeforeCommitReverteFlushJaRealizado() {
        var antes=financeiro(); var auditoria=originais();var confirmou=new AtomicBoolean();
        var repositorioReal=mockingDetails(estornos).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocacao->{
            Object salvo=repositorioReal.answer(invocacao);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void beforeCommit(boolean readOnly) {
                    confirmou.set(true);throw new IllegalStateException("Falha injetada em beforeCommit");
                }
            });return salvo;
        }).when(estornos).saveAndFlush(any(EstornoMovimentacaoCustodia.class));
        assertThrows(IllegalStateException.class,()->service.estornar(command(null)));
        assertTrue(confirmou.get());assertEquals(0L,contarEstornos());assertEquals(antes,financeiro()); assertEquals(auditoria, originais());
        verify(estornos,times(1)).saveAndFlush(any(EstornoMovimentacaoCustodia.class));
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void participaDeTransacaoExternaValidaSemConfirmacaoIndependente(boolean rollback) {
        var externa=new TransactionTemplate(manager);externa.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        externa.execute(status->{assertNotNull(service.estornar(command(null)).getId());if(rollback)status.setRollbackOnly();return null;});
        assertEquals(rollback?0L:1L,contarEstornos());
    }
    @ParameterizedTest @ValueSource(ints={TransactionDefinition.ISOLATION_READ_UNCOMMITTED,TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionDefinition.ISOLATION_SERIALIZABLE})
    void isolamentoExternoIncompativelEhRecusadoAntesDeConsulta(int isolamento) {
        var externa=new TransactionTemplate(manager);externa.setIsolationLevel(isolamento);
        assertThrows(IllegalTransactionStateException.class,()->externa.execute(status->service.estornar(command(null))));
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);assertEquals(0L,contarEstornos());
    }
    @Test void contextoReadOnlyExternoEhRecusadoAntesDeConsulta() {
        var externa=new TransactionTemplate(manager);externa.setReadOnly(true);
        assertThrows(IllegalTransactionStateException.class,()->externa.execute(status->service.estornar(command(null))));
        verifyNoInteractions(carteiras,acoes,corretoras,operacoes,movimentos,estornos);assertEquals(0L,contarEstornos());
    }
    List<List<Object>> originais() { return tx(e -> {
        @SuppressWarnings("unchecked") List<Object[]> rows = e.createNativeQuery("SELECT id,carteira_id,acao_id,natureza,corretora_origem_id,corretora_destino_id,quantidade,data_efetiva,ordem_financeira_ancora,posicao_numerador,posicao_denominador,data_registro FROM movimentacao_custodia ORDER BY id").getResultList();
        return rows.stream().map(Arrays::asList).toList();
    }); }
    void saldos(String origem, String destino) {
        tx(e -> {
            var ms = movimentos.listarPorCarteiraEAcao(f.carteira(), f.acao());
            var es = estornos.listarPorCarteiraEAcao(f.carteira(), f.acao());
            var vinculos = ms.stream().map(m -> es.stream().filter(x -> x.getMovimentacaoOriginalId().equals(m.getId())).findFirst()
                    .map(x -> VinculoEstornoConsultado.vinculoPresente(m.getId(), x))
                    .orElseGet(() -> VinculoEstornoConsultado.ausenciaVerificada(m.getId()))).toList();
            var fontes = new AdaptadorEventosQuantitativos().adaptar(f.carteira(), f.acao(),
                    operacoes.listarParaReplayQuantitativo(f.carteira(), f.acao()), ms, vinculos);
            var eventos = new NormalizadorEventosQuantitativos().normalizar(f.carteira(), f.acao(), fontes.financeiros(), fontes.custodias());
            assertEquals(1, eventos.stream().filter(x -> x.identidade().equals(EventoQuantitativo.Identidade.persistida(EventoQuantitativo.Dominio.CUSTODIA, original)) && x.neutralizado()).count());
            var estado = new MotorReplayQuantitativo().reproduzir(eventos).estadoFinal();
            assertEquals(0, new BigDecimal("100").compareTo(estado.quantidadeConsolidada()));
            assertEquals(0, new BigDecimal(origem).compareTo(estado.saldo(EventoQuantitativo.Grupo.corretora(f.origem()))));
            assertEquals(0, new BigDecimal(destino).compareTo(estado.saldo(EventoQuantitativo.Grupo.corretora(f.destino()))));
            return null;
        });
    }
    long contarEstornos() { return tx(e -> e.createQuery("select count(e) from EstornoMovimentacaoCustodia e", Long.class).getSingleResult()); }
    static Corretora corretora(String cnpj) { return new Corretora(cnpj,"Sintética",null,null,null,"00000000","Rua","1",null,"Bairro","Cidade","SP","ATIVA",REGISTRO); }
}
