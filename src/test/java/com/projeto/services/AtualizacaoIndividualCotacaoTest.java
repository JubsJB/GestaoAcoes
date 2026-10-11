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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.projeto.entities.ReferenciaTipoCotacao.*;

@SpringBootTest(classes=GestaoacoesApplication.class)
@ActiveProfiles("test")
class AtualizacaoIndividualCotacaoTest {
    static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-04T16:00:00Z");
    static final LocalDate DAY = LocalDate.of(2026,10,2);
    static final AtomicInteger IDS = new AtomicInteger();
    @Autowired AcaoRepository acoes;
    @Autowired HistoricoCotacaoRepository historico;
    @Autowired AplicacaoCotacaoService aplicacao;
    CotacaoProvider provider;
    AcaoService service;

    @BeforeEach void preparar() {
        provider = mock(CotacaoProvider.class);
        when(provider.mercado()).thenReturn(Mercado.EUA);
        service = novaInstancia();
        clearInvocations(provider);
    }
    AcaoService novaInstancia() {
        return new AcaoService(new TickerNormalizer(), List.of(provider), mock(AcaoPersistenceService.class),
                aplicacao, acoes, new AcaoMapper(), Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
    }
    Acao criar(ReferenciaTipoCotacao tipo, OffsetDateTime obtencao) {
        return acoes.saveAndFlush(new Acao("IT"+IDS.incrementAndGet(), "Empresa", Mercado.EUA,
                Moeda.USD, new BigDecimal("100.000000"), NOW.plusDays(5), tipo,
                tipo == INSTANTE ? NOW.minusDays(2) : null, tipo == DATA ? DAY : null, obtencao));
    }
    CotacaoCandidata candidata(Acao a, ReferenciaTipoCotacao tipo, int ordem, String preco, OffsetDateTime obt) {
        return new CotacaoCandidata(a.getTicker(), "Empresa", Moeda.USD, new BigDecimal(preco), tipo,
                tipo == INSTANTE ? NOW.minusDays(2).plusDays(ordem) : null,
                tipo == DATA ? DAY.plusDays(ordem) : null, obt);
    }
    void responder(Acao a, CotacaoCandidata c) {
        when(provider.consultarCandidataAtualizacao(eq(a.getTicker()), any(), any(), any())).thenAnswer(i -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return c;
        });
    }
    List<Object> estado(Acao a) {
        return Arrays.asList(a.getCotacaoAtual(),a.getReferenciaTipo(),a.getReferenciaInstante(),
                a.getReferenciaData(),a.getCotacaoObtidaEm(),a.getDataHoraCotacao());
    }
    Acao reler(Acao a) { return acoes.findById(a.getId()).orElseThrow(); }
    void semHistorico(Acao a) { assertTrue(historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId()).isEmpty()); }

    @ParameterizedTest @EnumSource(value=ReferenciaTipoCotacao.class,names={"DATA","INSTANTE"})
    void posteriorComObtencaoPropriaAnteriorELegadoMaiorEhAceita(ReferenciaTipoCotacao tipo) {
        Acao a=criar(tipo,NOW.plusMinutes(1));
        var c=candidata(a,tipo,1,"102",NOW);
        responder(a,c);
        assertEquals(new BigDecimal("102.000000"),service.atualizarCotacao(a.getId()).cotacaoAtual());
        assertEquals(NOW,reler(a).getCotacaoObtidaEm());
        assertEquals(c.referenciaData(),reler(a).getReferenciaData());
        assertEquals(c.referenciaInstante(),reler(a).getReferenciaInstante());
        var h=historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId());
        assertEquals(1,h.size()); assertEquals(NOW,h.get(0).getCotacaoObtidaEm());
        var accepted=estado(reler(a));
        clearInvocations(provider);
        service.atualizarCotacao(a.getId());
        verifyNoInteractions(provider);
        assertEquals(accepted,estado(reler(a)));
        assertEquals(1,historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId()).size());
    }
    @Test void baselineConfiavelCriaHistoricoApesarDoLegadoFuturo() {
        Acao a=criar(DESCONHECIDA,null); responder(a,candidata(a,DATA,0,"100",NOW));
        service.atualizarCotacao(a.getId());
        assertEquals(DATA,reler(a).getReferenciaTipo());
        assertEquals(1,historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId()).size());
    }
    @Test void mesmaObservacaoAvancaSomenteObtencao() {
        Acao a=criar(DATA,NOW.minusHours(1)); responder(a,candidata(a,DATA,0,"100",NOW));
        service.atualizarCotacao(a.getId());
        assertEquals(NOW,reler(a).getCotacaoObtidaEm());
        assertEquals(a.getDataHoraCotacao(),reler(a).getDataHoraCotacao()); semHistorico(a);
    }
    @Test void referenciaAnteriorPreservaTudo() {
        Acao a=criar(DATA,NOW.minusHours(1)); var before=estado(a);
        responder(a,candidata(a,DATA,-1,"101",NOW)); service.atualizarCotacao(a.getId());
        assertEquals(before,estado(reler(a))); semHistorico(a);
    }
    @Test void conflitoTemporalRetorna422ECooldownPreservaErroSemHistorico() {
        Acao a=criar(DATA,NOW.minusHours(1)); var before=estado(a);
        responder(a,candidata(a,DATA,0,"101",NOW));
        var failure=assertThrows(ApiException.class,()->service.atualizarCotacao(a.getId()));
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT,failure.getStatus());
        assertEquals("CONFLITO_NA_MESMA_REFERENCIA",failure.getCode());
        clearInvocations(provider);
        assertEquals(failure.getCode(),assertThrows(ApiException.class,()->service.atualizarCotacao(a.getId())).getCode());
        verifyNoInteractions(provider); assertEquals(before,estado(reler(a))); semHistorico(a);
    }
    @Test void tiposIncompativeisRetornam422SemEscrita() {
        Acao a=criar(DATA,NOW.minusHours(1)); var before=estado(a);
        responder(a,candidata(a,INSTANTE,1,"102",NOW));
        var failure=assertThrows(ApiException.class,()->service.atualizarCotacao(a.getId()));
        assertEquals("TIPO_REFERENCIA_INCOMPATIVEL",failure.getCode());
        assertEquals(before,estado(reler(a))); semHistorico(a);
    }
    @Test void restartPerdeCooldownMasReutilizaObtencaoPersistidaSemHistorico() {
        Acao a=criar(DATA,NOW.minusMinutes(1)); var before=estado(a);
        service=novaInstancia(); clearInvocations(provider);
        service.atualizarCotacao(a.getId());
        verifyNoInteractions(provider); assertEquals(before,estado(reler(a))); semHistorico(a);
    }
}
