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
@ContextConfiguration(classes=CustodiaConflitosTransacionaisH2Test.Config.class,initializers=CustodiaH2IsolationInitializer.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class CustodiaConflitosTransacionaisH2Test {
    static final OffsetDateTime REGISTRO=OffsetDateTime.parse("2026-10-24T12:00:00Z");
    static final LocalDate DIA=LocalDate.of(2026,10,1);
    @TestConfiguration(proxyBeanMethods=false)
    @Import({CarteiraCustodiaExclusaoH2Test.Config.class,EstornoCustodiaTransacaoH2Test.Config.class})
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
    @MockitoSpyBean SnapshotCarteiraRepository snapshots;
    Fixture f;
    Long original;
    record Fixture(Long carteira,Long outraCarteira,Long acao,Long outraAcao,Long origem,Long destino,Long terceira) { }
    <T>T tx(Function<EntityManager,T> trabalho){return new TransactionTemplate(manager).execute(status->trabalho.apply(em));}
    @BeforeEach void preparar() {
        reset(estornos,movimentos,carteiras);
        f=tx(e->{
            for(String tabela:List.of("EstornoMovimentacaoCustodia","MovimentacaoCustodia","Operacao","SnapshotCarteira","Acao","Corretora","Carteira"))
                e.createQuery("delete from "+tabela).executeUpdate();
            var c=new Carteira("Atribuição",REGISTRO);var outra=new Carteira("Outra",REGISTRO);
            var a=new Acao("PETR4","Sintética",Mercado.BRASIL,Moeda.BRL,BigDecimal.TEN,REGISTRO);
            var outraAcao=new Acao("AAPL","Sintética",Mercado.EUA,Moeda.USD,BigDecimal.TEN,REGISTRO);
            var origem=corretora("11111111000111");var destino=corretora("22222222000122");var terceira=corretora("33333333000133");
            for(Object item:List.of(c,outra,a,outraAcao,origem,destino,terceira))e.persist(item);
            e.persist(new Operacao(c,a,origem,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,1,new BigDecimal("1000")));
            e.persist(new Operacao(c,a,null,TipoOperacao.COMPRA,new BigDecimal("100"),BigDecimal.TEN,DIA,2,new BigDecimal("1000")));
            e.flush();return new Fixture(c.getId(),outra.getId(),a.getId(),outraAcao.getId(),origem.getId(),destino.getId(),terceira.getId());
        });
        original = tx(e -> {
            var m = new MovimentacaoCustodia(null, e.find(Carteira.class, f.carteira()), e.find(Acao.class, f.acao()),
                    NaturezaMovimentacaoCustodia.TRANSFERENCIA, e.find(Corretora.class, f.origem()),
                    e.find(Corretora.class, f.destino()), new BigDecimal("40"), DIA, 1,
                    PosicaoRacionalCustodia.deTexto("1", "2"), Clock.fixed(REGISTRO.minusDays(2).toInstant(), ZoneOffset.UTC));
            e.persist(m); return m.getId();
        });
        clearInvocations(carteiras,acoes,corretoras,operacoes,movimentos,estornos,snapshots);
    }
    EstornoCustodiaCommand command(String motivo) { return new EstornoCustodiaCommand(f.carteira(), original, motivo); }
    List<List<Object>> financeiro(){return tx(e->{
        @SuppressWarnings("unchecked") List<Object[]> rows=e.createNativeQuery("SELECT id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total FROM operacao ORDER BY id").getResultList();
        return rows.stream().map(Arrays::asList).toList();
    });}
    @ParameterizedTest @ValueSource(strings={"TRANSFERENCIA","ATRIBUICAO","ESTORNO","EXCLUSAO"})
    void lockReconhecidoEhTraduzidoEmTodasAsFachadas(String tipo) {
        doThrow(new org.springframework.dao.CannotAcquireLockException("senha=sentinela"))
                .when(carteiras).findByIdForUpdate(anyLong());
        var erro=assertThrows(ApiException.class,()->executar(tipo));
        assertEquals("CONFLITO_TRANSACIONAL_TRANSITORIO",erro.getCode());
        assertEquals(409,erro.getStatus().value());assertNull(erro.getCause());
        assertFalse((erro.getMessage()+erro.getDetails()).contains("sentinela"));
        verify(carteiras,times(1)).findByIdForUpdate(anyLong());
        verifyNoInteractions(acoes,corretoras,operacoes,movimentos,estornos);
    }
    @ParameterizedTest @ValueSource(strings={"TRANSFERENCIA","ATRIBUICAO","ESTORNO","EXCLUSAO"})
    void erroDeDominioNaoEhSubstituidoPorCausaTransitoria(String tipo) {
        var dominio = new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                "CODIGO_DOMINIO", "Erro de domínio", Map.of("carteiraId", f.carteira()));
        dominio.initCause(new java.sql.SQLException("causa controlada", "40001"));
        doThrow(dominio).when(carteiras).findByIdForUpdate(anyLong());
        assertSame(dominio, assertThrows(ApiException.class, () -> executar(tipo)));
        verify(carteiras,times(1)).findByIdForUpdate(anyLong());
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> estadosPorFachada() {
        return java.util.stream.Stream.of("TRANSFERENCIA","ATRIBUICAO","ESTORNO","EXCLUSAO")
                .flatMap(t->java.util.stream.Stream.of("40001","40P01","55P03")
                        .map(e->org.junit.jupiter.params.provider.Arguments.of(t,e)));
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.MethodSource("estadosPorFachada")
    void estadosReconhecidosNaoSaoExpostosNemRepetidos(String tipo,String estado) {
        doThrow(new RuntimeException("senha=sentinela",new java.sql.SQLException("SQL sentinela",estado)))
                .when(carteiras).findByIdForUpdate(anyLong());
        var erro=assertThrows(ApiException.class,()->executar(tipo));
        assertEquals("CONFLITO_TRANSACIONAL_TRANSITORIO",erro.getCode());assertNull(erro.getCause());
        assertFalse((erro.getMessage()+erro.getDetails()).contains("sentinela"));
        verify(carteiras,times(1)).findByIdForUpdate(anyLong());
        verifyNoInteractions(acoes,corretoras,operacoes,movimentos,estornos,snapshots);
    }

    @ParameterizedTest @ValueSource(strings={"TRANSFERENCIA","ATRIBUICAO","ESTORNO","EXCLUSAO"})
    void erroDesconhecidoDeLockNaoEhPresumidoTransitorio(String tipo) {
        var falha=new RuntimeException("Falha controlada",new java.sql.SQLException("controlada","ZZZZZ"));
        doThrow(falha).when(carteiras).findByIdForUpdate(anyLong());
        assertSame(falha,assertThrows(RuntimeException.class,()->executar(tipo)));
        verify(carteiras,times(1)).findByIdForUpdate(anyLong());
        verifyNoInteractions(acoes,corretoras,operacoes,movimentos,estornos,snapshots);
    }

    @ParameterizedTest @ValueSource(strings={"TRANSFERENCIA","ATRIBUICAO","ESTORNO","EXCLUSAO"})
    void referenciaAusenteJaClassificadaEhPreservada(String tipo) {
        var erro=new ObjectNotFoundException("Referência não encontrada");
        erro.initCause(new java.sql.SQLException("causa controlada","40001"));
        doThrow(erro).when(carteiras).findByIdForUpdate(anyLong());
        assertSame(erro,assertThrows(ObjectNotFoundException.class,()->executar(tipo)));
    }

    @ParameterizedTest @ValueSource(strings={"TRANSFERENCIA","ATRIBUICAO","ESTORNO","EXCLUSAO"})
    void falhaReconhecidaNoEncerramentoDoProxyReverteTudo(String tipo) {
        var antes=estado();var chegou=new AtomicBoolean();
        injetarNaConfirmacao(tipo,new RuntimeException("senha=sentinela",new java.sql.SQLException("SQL sentinela","40001")),chegou);
        var erro=assertThrows(ApiException.class,()->executar(tipo));
        assertTrue(chegou.get());assertEquals("CONFLITO_TRANSACIONAL_TRANSITORIO",erro.getCode());
        assertNull(erro.getCause());assertFalse((erro.getMessage()+erro.getDetails()).contains("sentinela"));
        semChamadasAposEscrita(tipo);assertEquals(antes,estado());
    }

    @ParameterizedTest @ValueSource(strings={"TRANSFERENCIA","ATRIBUICAO","ESTORNO","EXCLUSAO"})
    void falhaDesconhecidaNaConfirmacaoNaoEhPresumidaTransitoria(String tipo) {
        var antes=estado();var chegou=new AtomicBoolean();var falha=new IllegalStateException("Falha controlada");
        injetarNaConfirmacao(tipo,falha,chegou);
        assertSame(falha,assertThrows(IllegalStateException.class,()->executar(tipo)));
        assertTrue(chegou.get());semChamadasAposEscrita(tipo);assertEquals(antes,estado());
    }

    @ParameterizedTest @ValueSource(strings={"TRANSFERENCIA","ATRIBUICAO","ESTORNO","EXCLUSAO"})
    void flushReconhecidoNaoProvocaConsultasOuRetries(String tipo) {
        var antes=estado();
        var falha=new RuntimeException("senha=sentinela",new java.sql.SQLException("SQL sentinela","40P01"));
        switch(tipo) {
            case "TRANSFERENCIA", "ATRIBUICAO" -> doThrow(falha).when(movimentos).saveAndFlush(any());
            case "ESTORNO" -> doThrow(falha).when(estornos).saveAndFlush(any());
            case "EXCLUSAO" -> doThrow(falha).when(carteiras).delete(any(Carteira.class));
        }
        var erro=assertThrows(ApiException.class,()->executar(tipo));
        assertEquals("CONFLITO_TRANSACIONAL_TRANSITORIO",erro.getCode());assertNull(erro.getCause());
        semChamadasAposEscrita(tipo);assertEquals(antes,estado());
    }

    @ParameterizedTest @ValueSource(strings={"consulta","constraint"})
    void duplicidadeDeEstornoPermanecePrioritariaESegura(String modo) {
        if(modo.equals("consulta")) {
            tx(e->{e.persist(new EstornoMovimentacaoCustodia(null,e.find(MovimentacaoCustodia.class,original),null,Clock.fixed(REGISTRO.toInstant(),ZoneOffset.UTC)));return null;});
            clearInvocations(carteiras,acoes,corretoras,operacoes,movimentos,estornos,snapshots);
        } else {
            var sql=new java.sql.SQLException("senha=sentinela","23505");
            var causa=new org.hibernate.exception.ConstraintViolationException("SQL sentinela",sql,"uk_estorno_movimentacao_custodia_original");
            doThrow(new DataIntegrityViolationException("sentinela",causa)).when(estornos).saveAndFlush(any());
        }
        var antes=estado();
        var erro=assertThrows(ApiException.class,()->executar("ESTORNO"));
        assertEquals("ESTORNO_DUPLICADO",erro.getCode());assertNull(erro.getCause());
        assertFalse((erro.getMessage()+erro.getDetails()).contains("sentinela"));
        assertFalse((erro.getMessage()+erro.getDetails()).contains("uk_estorno"));
        if(modo.equals("consulta")){verifyNoInteractions(operacoes);verify(estornos,never()).saveAndFlush(any());}
        else semChamadasAposEscrita("ESTORNO");
        assertEquals(antes,estado());
    }

    @ParameterizedTest @ValueSource(strings={"TRANSFERENCIA","ATRIBUICAO","ESTORNO","EXCLUSAO"})
    void confirmacaoExternaAposRetornoDaFachadaPertenceAoChamador(String tipo) {
        var antes=estado();var retornou=new AtomicBoolean();
        var falha=new RuntimeException("Falha externa controlada",new java.sql.SQLException("controlada","40001"));
        var externa=new TransactionTemplate(manager);
        externa.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        var observada=assertThrows(RuntimeException.class,()->externa.execute(status->{
            executar(tipo);retornou.set(true);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void beforeCommit(boolean readOnly){throw falha;}
            });return null;
        }));
        assertTrue(retornou.get());assertSame(falha,observada);assertFalse(observada instanceof ApiException);
        assertEquals(antes,estado());
    }

    void injetarNaConfirmacao(String tipo,RuntimeException falha,AtomicBoolean chegou) {
        Object repo=tipo.equals("EXCLUSAO")?carteiras:tipo.equals("ESTORNO")?estornos:movimentos;
        var real=mockingDetails(repo).getMockCreationSettings().getDefaultAnswer();
        org.mockito.stubbing.Answer<Object> resposta=inv->{
            var salvo=real.answer(inv);
            if(tipo.equals("EXCLUSAO"))em.flush();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void beforeCommit(boolean readOnly){chegou.set(true);throw falha;}
            });return salvo;
        };
        switch(tipo) {
            case "TRANSFERENCIA", "ATRIBUICAO" -> doAnswer(resposta).when(movimentos).saveAndFlush(any());
            case "ESTORNO" -> doAnswer(resposta).when(estornos).saveAndFlush(any());
            case "EXCLUSAO" -> doAnswer(resposta).when(carteiras).delete(any(Carteira.class));
        }
    }

    void semChamadasAposEscrita(String tipo) {
        var ordem=inOrder(carteiras,acoes,corretoras,operacoes,movimentos,estornos,snapshots);
        switch(tipo) {
            case "TRANSFERENCIA", "ATRIBUICAO" -> {ordem.verify(movimentos).saveAndFlush(any());verify(movimentos,times(1)).saveAndFlush(any());}
            case "ESTORNO" -> {ordem.verify(estornos).saveAndFlush(any());verify(estornos,times(1)).saveAndFlush(any());}
            case "EXCLUSAO" -> {ordem.verify(carteiras).delete(any(Carteira.class));verify(carteiras,times(1)).delete(any(Carteira.class));}
        }
        ordem.verifyNoMoreInteractions();
    }

    Map<String,List<List<Object>>> estado() {
        return tx(e->{
            var consultas=Map.of(
                    "carteiras","select id,nome,data_criacao from carteira order by id",
                    "financeiro","select id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total from operacao order by id",
                    "originais","select id,carteira_id,acao_id,natureza,corretora_origem_id,corretora_destino_id,quantidade,data_efetiva,ordem_financeira_ancora,posicao_numerador,posicao_denominador,data_registro from movimentacao_custodia order by id",
                    "estornos","select id,movimentacao_original_id,motivo,data_registro from estorno_movimentacao_custodia order by id");
            Map<String,List<List<Object>>> resultado=new HashMap<>();
            consultas.forEach((nome,sql)->{
                @SuppressWarnings("unchecked") List<Object[]> rows=e.createNativeQuery(sql).getResultList();
                resultado.put(nome,rows.stream().map(Arrays::asList).toList());
            });return resultado;
        });
    }

    void executar(String tipo) {
        switch(tipo) {
            case "TRANSFERENCIA" -> context.getBean(TransferenciaCustodiaService.class).transferir(
                    new TransferenciaCustodiaCommand(f.carteira(),"PETR4",Mercado.BRASIL,f.origem(),f.terceira(),BigDecimal.TEN,DIA));
            case "ATRIBUICAO" -> context.getBean(AtribuicaoCustodiaService.class).atribuir(
                    new AtribuicaoCustodiaCommand(f.carteira(),"PETR4",Mercado.BRASIL,f.terceira(),BigDecimal.TEN,DIA));
            case "ESTORNO" -> service.estornar(command(null));
            case "EXCLUSAO" -> context.getBean(CarteiraService.class).excluir(f.outraCarteira());
            default -> throw new IllegalArgumentException("Caso de teste desconhecido");
        }
    }
    static Corretora corretora(String cnpj){return new Corretora(cnpj,"Sintética",null,null,null,"00000000","Rua","1",null,"Bairro","Cidade","SP","ATIVA",REGISTRO);}
}
