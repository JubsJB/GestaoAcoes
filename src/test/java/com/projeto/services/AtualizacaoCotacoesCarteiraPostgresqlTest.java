package com.projeto.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.projeto.entities.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.projeto.entities.ReferenciaTipoCotacao.*;
import static com.projeto.services.RegraDecisaoCotacao.Resultado.*;

/** Todos os cenários de lote, snapshots pós-rollback e concorrência local executados com persistência PostgreSQL. */
@EnabledIfSystemProperty(named="temporal.test.postgresql.enabled", matches="true")
class AtualizacaoCotacoesCarteiraPostgresqlTest extends AtualizacaoCotacoesCarteiraServiceTest {
    @DynamicPropertySource static void banco(DynamicPropertyRegistry r) { PostgresqlTemporalSupport.configurar(r); }
    @BeforeEach void conferirPostgresql() { PostgresqlTemporalSupport.conferirBanco(jdbc); }

    @Override
    @ParameterizedTest @ValueSource(booleans={false,true})
    void rollbackIsoladoNaoDesfazAnteriorNemImpedeProximo(boolean noCommit) {
        var a = aberta(Mercado.BRASIL, DESCONHECIDA, null);
        var b = aberta(Mercado.BRASIL, DESCONHECIDA, null);
        var c = aberta(Mercado.BRASIL, DESCONHECIDA, null);
        var estadoAnterior = ResultadoAtualizacaoCotacao.estado(b);
        var hAnterior = historico.saveAndFlush(new HistoricoCotacao(b, b.getCotacaoAtual(), b.getDataHoraCotacao()));
        responder(a, 0, "101"); responder(b, 0, "102"); responder(c, 0, "103");
        doAnswer(i -> {
            HistoricoCotacao h = i.getArgument(0); em.persist(h); em.flush();
            if (h.getAcao().getId().equals(b.getId())) {
                Runnable violar = () -> jdbc.update("insert into historico_cotacao "
                        + "(acao_id,cotacao,data_hora_cotacao,referencia_tipo,referencia_instante,referencia_data,cotacao_obtida_em) "
                        + "select acao_id,cotacao,data_hora_cotacao,referencia_tipo,referencia_instante,referencia_data,cotacao_obtida_em "
                        + "from historico_cotacao where id=?", h.getId());
                if (noCommit) org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override public void beforeCommit(boolean readOnly) { violar.run(); }
                        });
                else violar.run();
            }
            return h;
        }).when(historico).saveAndFlush(any(HistoricoCotacao.class));
        var r = lote.atualizar(carteira.getId());
        assertEquals(java.util.List.of(ATUALIZADA, FALHA, ATUALIZADA),
                r.resultados().stream().map(ResultadoAtualizacaoCotacao::resultado).toList());
        assertEquals(estadoAnterior, r.resultados().get(1).cotacao());
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(s -> {
            assertEquals(estadoAnterior, ResultadoAtualizacaoCotacao.estado(acoes.findById(b.getId()).orElseThrow()));
            var hs = historico.findByAcaoIdOrderByDataHoraCotacaoAsc(b.getId()); assertEquals(1, hs.size());
            var h = hs.get(0); assertEquals(hAnterior.getId(), h.getId()); assertEquals(b.getId(), h.getAcao().getId());
            assertEquals(hAnterior.getCotacao(), h.getCotacao()); assertEquals(hAnterior.getReferenciaTipo(), h.getReferenciaTipo());
            assertEquals(hAnterior.getReferenciaData(), h.getReferenciaData()); assertEquals(hAnterior.getReferenciaInstante(), h.getReferenciaInstante());
            assertEquals(hAnterior.getCotacaoObtidaEm(), h.getCotacaoObtidaEm()); assertEquals(hAnterior.getDataHoraCotacao(), h.getDataHoraCotacao());
            assertEquals(1, historicos(a)); assertEquals(1, historicos(c));
        });
        verify(br, times(1)).consultarCandidataAtualizacao(eq(b.getTicker()), any(), any(), any());
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void lockRealLoteVersusPatchOuOutroLoteDeInstanciaIndependente(boolean segundoLote) throws Exception {
        var a = aberta(Mercado.BRASIL, INSTANTE, NOW.minusHours(1));
        var outraInstancia = novaInstancia();
        var outroLote = new AtualizacaoCotacoesCarteiraService(posicoes, outraInstancia, acoes);
        var primeira = candidata(a, 1, "101", NOW.plusMinutes(2));
        var segunda = candidata(a, 2, "102", NOW.plusMinutes(1));
        when(br.consultarCandidataAtualizacao(any(), any(), any(), any())).thenAnswer(i -> {
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            return Thread.currentThread().getName().equals("pg-lote-primeiro") ? primeira : segunda;
        });
        var pronta = new CountDownLatch(1); var liberar = new CountDownLatch(1); var iniciou = new CountDownLatch(1);
        var pidA = new AtomicInteger(); var pidB = new AtomicInteger();
        doAnswer(i -> {
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            int pid = jdbc.queryForObject("select pg_backend_pid()", Integer.class);
            if (Thread.currentThread().getName().equals("pg-lote-primeiro")) {
                pidA.set(pid); acoes.findByIdForUpdate(a.getId()).orElseThrow();
                pronta.countDown(); assertTrue(liberar.await(10, TimeUnit.SECONDS));
            } else {
                pidB.set(pid);
                assertEquals(new BigDecimal("100.000000"), em.find(Acao.class, a.getId()).getCotacaoAtual());
                iniciou.countDown();
            }
            return i.callRealMethod();
        }).when(aplicacao).aplicar(eq(a.getId()), any());
        var nomes = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(2, r -> new Thread(r,
                nomes.incrementAndGet() == 1 ? "pg-lote-primeiro" : "pg-segundo"));
        try {
            var f1 = pool.submit(() -> lote.atualizar(carteira.getId()));
            assertTrue(pronta.await(10, TimeUnit.SECONDS));
            var f2 = pool.submit(() -> segundoLote ? outroLote.atualizar(carteira.getId()).resultados().get(0)
                    : outraInstancia.atualizarCotacaoCompartilhada(a.getId()));
            assertTrue(iniciou.await(10, TimeUnit.SECONDS));
            PostgresqlTemporalSupport.esperarBloqueio(jdbc, pidA.get(), pidB.get());
            assertFalse(f2.isDone()); liberar.countDown();
            assertEquals(ATUALIZADA, f1.get(10, TimeUnit.SECONDS).resultados().get(0).resultado());
            assertEquals(ATUALIZADA, f2.get(10, TimeUnit.SECONDS).resultado());
            var atual = new org.springframework.transaction.support.TransactionTemplate(transactions)
                    .execute(s -> acoes.findById(a.getId()).orElseThrow());
            assertEquals(segunda.cotacao(), atual.getCotacaoAtual());
            assertEquals(segunda.referenciaInstante(), atual.getReferenciaInstante());
            assertEquals(segunda.cotacaoObtidaEm(), atual.getCotacaoObtidaEm());
            var hs = historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId());
            assertEquals(2, hs.size()); assertEquals(2, hs.stream().map(HistoricoCotacao::getReferenciaInstante).distinct().count());
            verify(br, times(2)).consultarCandidataAtualizacao(any(), any(), any(), any());
        } finally { liberar.countDown(); pool.shutdownNow(); }
    }

    @ParameterizedTest @ValueSource(strings={"desconhecida", "validaLegadoAntigo", "expiradaLegadoRecente", "microssegundoDentro", "limiteExato"})
    void reusoAposRoundTripDependeSomenteDeObtencaoEReferencia(String caso) {
        boolean desconhecida = caso.equals("desconhecida");
        boolean reutilizavel = caso.equals("validaLegadoAntigo") || caso.equals("microssegundoDentro");
        var obt = caso.equals("expiradaLegadoRecente") ? NOW.minusHours(1)
                : caso.equals("microssegundoDentro") ? NOW.minusMinutes(15).plusNanos(1000)
                : caso.equals("limiteExato") ? NOW.minusMinutes(15) : NOW.minusMinutes(5);
        var a = aberta(Mercado.EUA, desconhecida ? DESCONHECIDA : DATA, obt);
        var legado = caso.equals("validaLegadoAntigo") ? NOW.minusYears(10) : NOW;
        jdbc.update("update acao set data_hora_cotacao=? where id=?", legado, a.getId());
        responder(a, 1, "101"); clearInvocations(us, aplicacao, historico);
        var resultado = unico();
        if (reutilizavel) {
            assertEquals("REUSO", resultado.motivo()); assertEquals(obt, resultado.cotacao().obtencao());
            verifyNoInteractions(us, aplicacao, historico); assertEquals(0, historicos(a));
        } else {
            assertEquals(ATUALIZADA, resultado.resultado());
            verify(us, times(1)).consultarCandidataAtualizacao(any(), any(), any(), any());
            assertEquals(1, historicos(a));
        }
    }
}
