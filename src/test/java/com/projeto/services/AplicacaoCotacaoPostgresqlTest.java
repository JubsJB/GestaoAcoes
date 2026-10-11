package com.projeto.services;

import com.projeto.entities.*;
import com.projeto.integrations.cotacao.CotacaoCandidata;
import com.projeto.services.exceptions.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.projeto.entities.ReferenciaTipoCotacao.*;

/** Repete atomicidade/semântica H2 em PostgreSQL real e substitui a prova de lock por pg_blocking_pids. */
@EnabledIfSystemProperty(named="temporal.test.postgresql.enabled", matches="true")
class AplicacaoCotacaoPostgresqlTest extends AplicacaoCotacaoServiceTest {
    @DynamicPropertySource static void banco(DynamicPropertyRegistry r) { PostgresqlTemporalSupport.configurar(r); }
    @BeforeEach void conferirPostgresql() { PostgresqlTemporalSupport.conferirBanco(jdbc); }

    @Override Acao reler(Acao a) {
        return new TransactionTemplate(manager).execute(s -> acoes.findById(a.getId()).orElseThrow());
    }

    @Override void conferirRollback(Acao a, HistoricoCotacao anterior) {
        new TransactionTemplate(manager).executeWithoutResult(s -> {
            assertEquals(observacao(a), observacao(acoes.findById(a.getId()).orElseThrow()));
            var hs = historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId());
            assertEquals(1, hs.size()); assertEquals(observacao(anterior), observacao(hs.get(0)));
        });
    }

    @Override AplicacaoCotacaoService.Aplicacao executarSobLock(Acao a, CotacaoCandidata primeira,
            CotacaoCandidata segunda) throws Exception {
        var pronta = new CountDownLatch(1); var liberar = new CountDownLatch(1); var iniciada = new CountDownLatch(1);
        var pidA = new AtomicInteger(); var pidB = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(2);
        try {
            var f1 = pool.submit(() -> new TransactionTemplate(manager).execute(s -> {
                pidA.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                acoes.findByIdForUpdate(a.getId()).orElseThrow(); pronta.countDown(); aguardar(liberar);
                return service.aplicar(a.getId(), primeira);
            }));
            assertTrue(pronta.await(10, TimeUnit.SECONDS));
            var f2 = pool.submit(() -> new TransactionTemplate(manager).execute(s -> {
                pidB.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                // Carrega deliberadamente estado antigo ANTES de aguardar o lock.
                assertEquals(observacao(a), observacao(acoes.findById(a.getId()).orElseThrow()));
                iniciada.countDown(); return service.aplicar(a.getId(), segunda);
            }));
            assertTrue(iniciada.await(10, TimeUnit.SECONDS)); assertNotEquals(pidA.get(), pidB.get());
            PostgresqlTemporalSupport.esperarBloqueio(jdbc, pidA.get(), pidB.get());
            assertFalse(f2.isDone()); liberar.countDown(); f1.get(10, TimeUnit.SECONDS);
            return f2.get(10, TimeUnit.SECONDS);
        } finally { liberar.countDown(); pool.shutdownNow(); }
    }

    @ParameterizedTest @ValueSource(strings={"00", "00.1", "00.123456", "00.123456000", "00.999999000"})
    void roundTripExatoAcaoHistoricoESqlComOffsetEquivalente(String segundos) {
        var a = inicial(DESCONHECIDA);
        var referencia = OffsetDateTime.parse("2026-10-02T12:15:" + segundos + "-03:00");
        var obtencao = OffsetDateTime.parse("2026-10-04T15:30:" + segundos + "Z");
        var c = new CotacaoCandidata(a.getTicker(), "Empresa", Moeda.USD, new BigDecimal("1234567890123.123456"),
                INSTANTE, referencia, null, obtencao);
        service.aplicar(a.getId(), c);
        var atual = reler(a);
        assertEquals(referencia.toInstant(), atual.getReferenciaInstante().toInstant());
        assertEquals(obtencao.toInstant(), atual.getCotacaoObtidaEm().toInstant());
        conferirHistorico(a, c);
        for (String tabela : new String[]{"acao", "historico_cotacao"}) {
            var valores = jdbc.queryForMap("select referencia_instante,cotacao_obtida_em from " + tabela
                    + " where " + (tabela.equals("acao") ? "id" : "acao_id") + "=?", a.getId());
            assertEquals(referencia.toInstant(), ((java.sql.Timestamp) valores.get("referencia_instante")).toInstant());
            assertEquals(obtencao.toInstant(), ((java.sql.Timestamp) valores.get("cotacao_obtida_em")).toInstant());
        }
        System.out.println("POSTGRESQL_ROUND_TRIP " + segundos + " referencia=" + atual.getReferenciaInstante()
                + " obtencao=" + atual.getCotacaoObtidaEm());
    }

    @ParameterizedTest @ValueSource(strings={"123456001", "123456789", "999999001"})
    void nanosNaoRepresentaveisRejeitadosAntesDeQualquerPersistencia(String nanos) {
        var a = inicial(DESCONHECIDA); clearInvocations(acoes, historico);
        var invalido = OffsetDateTime.parse("2026-10-02T15:00:00." + nanos + "Z");
        for (boolean referencia : new boolean[]{true, false}) {
            var erro = assertThrows(ApiException.class, () -> service.aplicar(a.getId(), new CotacaoCandidata(
                    a.getTicker(), "Empresa", Moeda.USD, BigDecimal.TEN, INSTANTE,
                    referencia ? invalido : O, null, referencia ? O : invalido)));
            assertEquals("REFERENCIA_TEMPORAL_INVALIDA", erro.getCode());
        }
        verifyNoInteractions(acoes, historico); assertEquals(observacao(a), observacao(reler(a))); semHistorico(a);
    }

    @Test void contratoFisicoEfetivo() {
        assertEquals(7, jdbc.queryForObject("select count(*) from databasechangelog", Integer.class));
        for (String tabela : new String[]{"acao", "historico_cotacao"}) {
            for (String coluna : new String[]{"referencia_instante", "cotacao_obtida_em"}) {
                var info = jdbc.queryForMap("select data_type,datetime_precision,is_nullable from information_schema.columns "
                        + "where table_schema='public' and table_name=? and column_name=?", tabela, coluna);
                assertEquals("timestamp with time zone", info.get("data_type"));
                assertEquals(6, info.get("datetime_precision")); assertEquals("YES", info.get("is_nullable"));
            }
            assertEquals("date", jdbc.queryForObject("select data_type from information_schema.columns "
                    + "where table_schema='public' and table_name=? and column_name='referencia_data'", String.class, tabela));
        }
        assertEquals(1, jdbc.queryForObject("select count(*) from pg_constraint where conname='fk_historico_cotacao_acao' "
                + "and contype='f' and confdeltype='a'", Integer.class));
        System.out.println("POSTGRESQL_CONSTRAINTS " + jdbc.queryForList("select conname,pg_get_constraintdef(oid) as definition "
                + "from pg_constraint where conrelid='historico_cotacao'::regclass order by conname"));
    }

    @ParameterizedTest @ValueSource(strings={"00", "00.1", "00.123456", "00.123456000", "00.999999000"})
    void dataBaselineEPosteriorPreservamObtencaoExataSemFabricarHorario(String segundos) {
        var a = inicial(DESCONHECIDA);
        var obt = OffsetDateTime.parse("2026-10-04T15:30:" + segundos + "Z");
        var primeira = candidata(a, DATA, 0, "101", obt);
        service.aplicar(a.getId(), primeira); conferirHistorico(a, primeira);
        var posterior = candidata(a, DATA, 1, "102", obt.minusMinutes(1));
        service.aplicar(a.getId(), posterior);
        new TransactionTemplate(manager).executeWithoutResult(s -> {
            var atual = acoes.findById(a.getId()).orElseThrow();
            assertEquals(DATA, atual.getReferenciaTipo()); assertNull(atual.getReferenciaInstante());
            assertEquals(posterior.referenciaData(), atual.getReferenciaData());
            assertEquals(posterior.cotacaoObtidaEm(), atual.getCotacaoObtidaEm());
            assertEquals(posterior.cotacaoObtidaEm(), atual.getDataHoraCotacao());
            var hs = historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId());
            assertEquals(2, hs.size());
            for (var h : hs) { assertEquals(DATA, h.getReferenciaTipo()); assertNull(h.getReferenciaInstante()); }
            assertTrue(hs.stream().anyMatch(h -> h.getReferenciaData().equals(primeira.referenciaData())
                    && h.getCotacaoObtidaEm().equals(primeira.cotacaoObtidaEm())));
            assertTrue(hs.stream().anyMatch(h -> h.getReferenciaData().equals(posterior.referenciaData())
                    && h.getCotacaoObtidaEm().equals(posterior.cotacaoObtidaEm())));
        });
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void desconhecidaSemOuComObtencaoPermaneceFielAoLegado(boolean obtencaoConhecida) {
        var a = acoes.saveAndFlush(new Acao("LEG" + IDS.incrementAndGet(), "Empresa", Mercado.EUA,
                Moeda.USD, new BigDecimal("10.000000"), O, DESCONHECIDA, null, null, obtencaoConhecida ? O : null));
        var h = prepararHistorico(a);
        conferirRollback(a, h);
        var atual = reler(a);
        assertEquals(DESCONHECIDA, atual.getReferenciaTipo()); assertNull(atual.getReferenciaInstante());
        assertNull(atual.getReferenciaData()); assertEquals(obtencaoConhecida ? O : null, atual.getCotacaoObtidaEm());
    }

    @Test void falhaRealNoUpdateDaAcaoReverteTodosOsCampos() {
        var a = inicial(DATA); var anterior = prepararHistorico(a); clearInvocations(historico);
        doAnswer(i -> {
            // Executa UPDATE realmente inválido no banco na fronteira de gravação da Ação.
            jdbc.update("update acao set referencia_tipo='INSTANTE', referencia_instante=null where id=?", a.getId());
            return i.callRealMethod();
        }).when(acoes).saveAndFlush(any());
        var erro = assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> service.aplicar(a.getId(), candidata(a, DATA, 1, "102", O)));
        Throwable causa = erro; while (causa.getCause() != null) causa = causa.getCause();
        assertEquals("23514", assertInstanceOf(org.postgresql.util.PSQLException.class, causa).getSQLState());
        conferirRollback(a, anterior); verify(historico, never()).saveAndFlush(any());
    }

    @Test void foreignKeyRealImpedeOrfaoEDeleteEmCascata() {
        var a = inicial(DATA); var anterior = prepararHistorico(a);
        var orfao = assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("insert into historico_cotacao(acao_id,cotacao,data_hora_cotacao) values(?,10,?)", Long.MAX_VALUE, O));
        assertEquals("23503", ((java.sql.SQLException) orfao.getMostSpecificCause()).getSQLState());
        var delete = assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("delete from acao where id=?", a.getId()));
        assertEquals("23503", ((java.sql.SQLException) delete.getMostSpecificCause()).getSQLState());
        conferirRollback(a, anterior);
    }

    @ParameterizedTest @ValueSource(strings={"DATA", "INSTANTE"})
    void causaRealDeUnicidadeResidual(String tipoTexto) {
        var tipo = ReferenciaTipoCotacao.valueOf(tipoTexto); var a = inicial(tipo);
        var c = candidata(a, tipo, 1, "102", O.minusMinutes(1));
        var anterior = historico.saveAndFlush(new HistoricoCotacao(a, c.cotacao(), O, tipo,
                c.referenciaInstante(), c.referenciaData(), c.cotacaoObtidaEm()));
        clearInvocations(acoes, historico, provider, brapi);
        var erro = assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> service.aplicar(a.getId(), c));
        Throwable causa = erro; while (causa.getCause() != null) causa = causa.getCause();
        var pg = assertInstanceOf(org.postgresql.util.PSQLException.class, causa);
        assertEquals("23505", pg.getSQLState());
        assertEquals("uk_historico_cotacao_acao_" + (tipo == DATA ? "data" : "instante"), pg.getServerErrorMessage().getConstraint());
        System.out.println("POSTGRESQL_RESIDUAL " + erro.getClass().getName() + " -> " + pg.getClass().getName()
                + " SQLSTATE=" + pg.getSQLState() + " constraint=" + pg.getServerErrorMessage().getConstraint());
        conferirRollback(a, anterior); verify(historico, times(1)).saveAndFlush(any());
        verify(acoes, times(1)).findByIdForUpdate(a.getId()); verifyNoInteractions(provider, brapi);
    }
}
