package com.projeto.services;

import com.projeto.entities.*;
import com.projeto.integrations.cotacao.*;
import com.projeto.mappers.AcaoMapper;
import com.projeto.repositories.AcaoRepository;
import com.projeto.services.exceptions.*;
import com.projeto.validation.TickerNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AlphaVantageConsumptionTest {
    private final Clock clock = mock(Clock.class);
    private final CotacaoProvider alpha = mock(CotacaoProvider.class);
    private final CotacaoProvider brapi = mock(CotacaoProvider.class);
    private final AcaoRepository repository = mock(AcaoRepository.class);
    private final AplicacaoCotacaoService persistence = mock(AplicacaoCotacaoService.class);
    private final Instant now = Instant.parse("2026-09-09T16:00:00Z");
    private final AcaoService service;

    AlphaVantageConsumptionTest() {
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(alpha.mercado()).thenReturn(Mercado.EUA);
        when(brapi.mercado()).thenReturn(Mercado.BRASIL);
        service = new AcaoService(new TickerNormalizer(), List.of(alpha, brapi),
                mock(AcaoPersistenceService.class), persistence, repository, new AcaoMapper(), clock);
        doCallRealMethod().when(alpha).consultarCandidataAtualizacao(any(), any(), any(), any());
        doCallRealMethod().when(brapi).consultarCandidataAtualizacao(any(), any(), any(), any());
        doCallRealMethod().when(brapi).consultarAtualizacao(any(), any(), any());
        clearInvocations(alpha, brapi);
    }

    private Acao action(Instant date, Mercado market) {
        Acao a = new Acao("AAPL", "Apple", market, market == Mercado.EUA ? Moeda.USD : Moeda.BRL,
                new BigDecimal("100.000000"), date == null ? null : date.atOffset(ZoneOffset.UTC));
        ReflectionTestUtils.setField(a, "id", 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(a));
        return a;
    }

    private void success(Acao a) {
        when(alpha.consultarAtualizacao("AAPL", "Apple", "USD")).thenReturn(
                new CotacaoData("AAPL", "Apple", "USD", new BigDecimal("101.123456"), null, false, LocalDate.of(2026, 9, 9), false));
        when(persistence.aplicar(eq(1L), any())).thenAnswer(i -> applied(a, i.getArgument(1)));
    }

    @Test void recentQuoteDoesNotCallOrPersist() {
        Acao a = typedAction(now.minusSeconds(899));
        when(repository.findById(1L)).thenReturn(Optional.of(a));
        assertEquals(a.getCotacaoAtual(), service.atualizarCotacao(1L).cotacaoAtual());
        verifyNoInteractions(alpha, persistence);
    }

    @Test void exactExpiryCallsOnceAndPersistsExactValue() {
        Acao a = action(now.minusSeconds(900), Mercado.EUA); success(a);
        assertEquals(new BigDecimal("101.123456"), service.atualizarCotacao(1L).cotacaoAtual());
        service.atualizarCotacao(1L);
        verify(alpha, times(1)).consultarAtualizacao("AAPL", "Apple", "USD");
        verify(persistence, times(1)).aplicar(eq(1L), any());
        verify(alpha, never()).consultar(anyString());
    }

    @Test void noReusableTimestampCallsProvider() {
        Acao a = action(null, Mercado.EUA); success(a);
        service.atualizarCotacao(1L);
        verify(alpha).consultarAtualizacao("AAPL", "Apple", "USD");
    }

    @Test void cooldownReturnsPersistedSuccessEvenWhenRequestHoldsOlderEntity() {
        action(now.minusSeconds(3600), Mercado.EUA);
        Acao persisted = new Acao("AAPL", "Apple", Mercado.EUA, Moeda.USD,
                new BigDecimal("101.123456"), now.atOffset(ZoneOffset.UTC));
        ReflectionTestUtils.setField(persisted, "id", 1L);
        when(alpha.consultarAtualizacao("AAPL", "Apple", "USD")).thenReturn(
                new CotacaoData("AAPL", "Apple", "USD", persisted.getCotacaoAtual(), null, false, LocalDate.of(2026, 9, 9), false));
        when(persistence.aplicar(eq(1L), any())).thenAnswer(i -> applied(persisted, i.getArgument(1)));
        service.atualizarCotacao(1L);
        assertEquals(persisted.getCotacaoAtual(), service.atualizarCotacao(1L).cotacaoAtual());
        verify(alpha, times(1)).consultarAtualizacao("AAPL", "Apple", "USD");
    }

    @Test void failureCooldownPreservesQuoteAndRetriesOnlyAfterExpiry() {
        for (String code : List.of(ErrorCodes.SERVICO_EXTERNO_TIMEOUT,
                ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL)) {
            ReflectionTestUtils.setField(service, "refreshAttempts", new ConcurrentHashMap<>());
            reset(alpha); doCallRealMethod().when(alpha).consultarCandidataAtualizacao(any(), any(), any(), any()); when(clock.instant()).thenReturn(now);
            Acao a = action(now.minusSeconds(3600), Mercado.EUA);
            when(alpha.consultarAtualizacao("AAPL", "Apple", "USD")).thenThrow(
                    new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, "Provider unavailable"));
            for (int i = 0; i < 2; i++) {
                ApiException error = assertThrows(ApiException.class, () -> service.atualizarCotacao(1L));
                assertEquals(code, error.getCode());
                assertEquals(true, error.getDetails().get("cotacaoPreservada"));
                assertEquals(a.getCotacaoAtual(), error.getDetails().get("ultimaCotacaoValida"));
            }
            verify(alpha, times(1)).consultarAtualizacao("AAPL", "Apple", "USD");
            when(clock.instant()).thenReturn(now.plusSeconds(900));
            assertThrows(ApiException.class, () -> service.atualizarCotacao(1L));
            verify(alpha, times(2)).consultarAtualizacao("AAPL", "Apple", "USD");
            assertEquals(now.minusSeconds(3600).atOffset(ZoneOffset.UTC), a.getDataHoraCotacao());
        }
        verifyNoInteractions(persistence);
    }

    @Test void recoveredProviderIsUsedAtCooldownExpiry() {
        Acao a = action(now.minusSeconds(3600), Mercado.EUA);
        when(alpha.consultarAtualizacao("AAPL", "Apple", "USD")).thenThrow(
                new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, "Rate limit"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                assertThrows(ApiException.class, () -> service.atualizarCotacao(1L)).getStatus());
        reset(alpha); doCallRealMethod().when(alpha).consultarCandidataAtualizacao(any(), any(), any(), any());
        success(a);
        when(clock.instant()).thenReturn(now.plusSeconds(899));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                assertThrows(ApiException.class, () -> service.atualizarCotacao(1L)).getStatus());
        verifyNoInteractions(alpha, persistence);
        when(clock.instant()).thenReturn(now.plusSeconds(900));
        assertEquals(new BigDecimal("101.123456"), service.atualizarCotacao(1L).cotacaoAtual());
        verify(alpha, times(1)).consultarAtualizacao("AAPL", "Apple", "USD");
    }

    @Test void concurrentUpdatesShareOneRequest() throws Exception {
        Acao a = action(now.minusSeconds(3600), Mercado.EUA); success(a);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Object> update = () -> { start.await(); return service.atualizarCotacao(1L); };
            Future<Object> first = executor.submit(update), second = executor.submit(update);
            start.countDown(); first.get(5, TimeUnit.SECONDS); second.get(5, TimeUnit.SECONDS);
            verify(alpha, times(1)).consultarAtualizacao("AAPL", "Apple", "USD");
        } finally { executor.shutdownNow(); }
    }

    @Test void recentBrazilianQuoteStillCallsBrapi() {
        Acao a = action(now.minusSeconds(1), Mercado.BRASIL);
        when(brapi.consultar("AAPL")).thenReturn(new CotacaoData("AAPL", "Apple", "BRL",
                new BigDecimal("102"), now.atOffset(ZoneOffset.UTC), false));
        when(persistence.aplicar(eq(1L), any())).thenAnswer(i -> applied(a, i.getArgument(1)));
        service.atualizarCotacao(1L); service.atualizarCotacao(1L);
        verify(brapi, times(2)).consultar("AAPL"); verifyNoInteractions(alpha);
    }

    @Test void reusePreservesObtentionAndReferenceWithoutCandidateOrPersistence() {
        Acao a = typedAction(now.minusSeconds(300));
        when(repository.findById(1L)).thenReturn(Optional.of(a));
        service.atualizarCotacao(1L);
        service.atualizarCotacao(1L);
        assertTemporalMetadata(a, now.minusSeconds(300));
        verifyNoInteractions(alpha, persistence);
    }

    @Test void failureCooldownPrecedesRecentStateReuseAndDoesNotCreateObtention() {
        action(now.minusSeconds(3600), Mercado.EUA);
        when(alpha.consultarAtualizacao("AAPL", "Apple", "USD")).thenThrow(
                new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, "Rate limit"));
        assertThrows(ApiException.class, () -> service.atualizarCotacao(1L));
        Acao recent = typedAction(now.minusSeconds(300));
        when(repository.findById(1L)).thenReturn(Optional.of(recent));
        ApiException failure = assertThrows(ApiException.class, () -> service.atualizarCotacao(1L));
        assertEquals(ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, failure.getCode());
        assertTemporalMetadata(recent, now.minusSeconds(300));
        verify(alpha, times(1)).consultarAtualizacao("AAPL", "Apple", "USD");
        verify(alpha, times(1)).consultarCandidataAtualizacao(anyString(), anyString(), anyString(), any());
        verifyNoInteractions(persistence);
    }

    @Test void successCooldownDoesNotCreateNewObtentionOrCandidate() {
        action(now.minusSeconds(3600), Mercado.EUA);
        Acao accepted = typedAction(now);
        when(alpha.consultarAtualizacao("AAPL", "Apple", "USD")).thenReturn(
                new CotacaoData("AAPL", "Apple", "USD", accepted.getCotacaoAtual(), null, false, LocalDate.of(2026, 9, 8), false));
        when(persistence.aplicar(eq(1L), any())).thenAnswer(i -> applied(accepted, i.getArgument(1)));
        service.atualizarCotacao(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(accepted));
        service.atualizarCotacao(1L);
        assertTemporalMetadata(accepted, now);
        verify(alpha, times(1)).consultarAtualizacao("AAPL", "Apple", "USD");
        verify(alpha, times(1)).consultarCandidataAtualizacao(anyString(), anyString(), anyString(), any());
        verify(persistence, times(1)).aplicar(eq(1L), any());
    }

    private Acao typedAction(Instant obtainedAt) {
        Acao a = new Acao("AAPL", "Apple", Mercado.EUA, Moeda.USD, new BigDecimal("100.000000"),
                obtainedAt.atOffset(ZoneOffset.UTC), ReferenciaTipoCotacao.DATA, null,
                LocalDate.of(2026, 9, 8), obtainedAt.atOffset(ZoneOffset.UTC));
        ReflectionTestUtils.setField(a, "id", 1L);
        return a;
    }

    private void assertTemporalMetadata(Acao a, Instant obtainedAt) {
        assertEquals(new BigDecimal("100.000000"), a.getCotacaoAtual());
        assertEquals(ReferenciaTipoCotacao.DATA, a.getReferenciaTipo());
        assertEquals(LocalDate.of(2026, 9, 8), a.getReferenciaData());
        assertNull(a.getReferenciaInstante());
        assertEquals(obtainedAt.atOffset(ZoneOffset.UTC), a.getCotacaoObtidaEm());
    }

    @Test void intervalsMustBePositive() {
        ReflectionTestUtils.setField(service, "refreshCooldown", Duration.ZERO);
        assertThrows(IllegalArgumentException.class, service::validateRefreshIntervals);
    }
    private AplicacaoCotacaoService.Aplicacao applied(Acao a, CotacaoCandidata c) {
        var d = RegraDecisaoCotacao.decidir(a, c);
        if (d.efeito() == RegraDecisaoCotacao.Efeito.SUBSTITUIR_OBSERVACAO)
            a.substituirObservacao(c.cotacao(), c.referenciaTipo(), c.referenciaInstante(), c.referenciaData(), c.cotacaoObtidaEm());
        return new AplicacaoCotacaoService.Aplicacao(d, new AplicacaoCotacaoService.EstadoCotacao(
                a.getCotacaoAtual(), a.getReferenciaTipo(), a.getReferenciaInstante(), a.getReferenciaData(),
                a.getCotacaoObtidaEm(), a.getDataHoraCotacao()));
    }
    @Test void unknownWithRecentLegacyIsNotReusable() {
        Acao a = action(now.minusSeconds(1), Mercado.EUA); success(a);
        service.atualizarCotacao(1L);
        verify(alpha).consultarCandidataAtualizacao(any(), any(), any(), any());
    }
    @Test void recentLegacyWithExpiredObtentionIsNotReusable() {
        Acao a = typedAction(now.minusSeconds(900));
        ReflectionTestUtils.setField(a, "dataHoraCotacao", now.atOffset(ZoneOffset.UTC));
        when(repository.findById(1L)).thenReturn(Optional.of(a)); success(a);
        service.atualizarCotacao(1L);
        verify(alpha).consultarCandidataAtualizacao(any(), any(), any(), any());
    }
    @Test void oldLegacyWithValidObtentionIsReusable() {
        Acao a = typedAction(now.minusSeconds(1));
        ReflectionTestUtils.setField(a, "dataHoraCotacao", now.minusSeconds(86400).atOffset(ZoneOffset.UTC));
        when(repository.findById(1L)).thenReturn(Optional.of(a));
        service.atualizarCotacao(1L);
        verifyNoInteractions(alpha, persistence);
    }
    @Test void cooldownSharesCompletedResultRegardlessOfLegacyOrdering() {
        Acao a = action(now.minusSeconds(3600), Mercado.EUA); success(a);
        var completed = service.atualizarCotacao(1L);
        Acao other = typedAction(now.plusSeconds(10));
        ReflectionTestUtils.setField(other, "dataHoraCotacao", now.plusSeconds(86400).atOffset(ZoneOffset.UTC));
        when(repository.findById(1L)).thenReturn(Optional.of(other));
        clearInvocations(alpha, persistence);
        assertEquals(completed, service.atualizarCotacao(1L));
        verifyNoInteractions(alpha, persistence);
        assertTemporalMetadata(other, now.plusSeconds(10));
    }

    @Test void knownReferenceWithoutObtentionDoesNotReuse() {
        Acao a=typedAction(now); ReflectionTestUtils.setField(a,"cotacaoObtidaEm",null);
        when(repository.findById(1L)).thenReturn(Optional.of(a)); success(a);
        service.atualizarCotacao(1L); verify(alpha).consultarCandidataAtualizacao(any(),any(),any(),any());
    }
    @Test void futureObtentionDoesNotReuse() {
        Acao a=typedAction(now.plusSeconds(1));
        when(repository.findById(1L)).thenReturn(Optional.of(a)); success(a);
        service.atualizarCotacao(1L); verify(alpha).consultarCandidataAtualizacao(any(),any(),any(),any());
    }
    @Test void expiredCooldownStillChecksReusableState() {
        Acao a=action(now.minusSeconds(3600),Mercado.EUA); success(a);
        service.atualizarCotacao(1L);
        Acao recent=typedAction(now.plusSeconds(600));
        when(repository.findById(1L)).thenReturn(Optional.of(recent));
        when(clock.instant()).thenReturn(now.plusSeconds(900));
        clearInvocations(alpha,persistence);
        service.atualizarCotacao(1L); verifyNoInteractions(alpha,persistence);
    }
    @Test void integrityFailurePropagatesWithoutRetryOrSuccessfulCooldown() {
        Acao a=action(now.minusSeconds(3600),Mercado.EUA); success(a);
        var failure=new org.springframework.dao.DataIntegrityViolationException("rollback");
        when(persistence.aplicar(eq(1L),any())).thenThrow(failure);
        assertSame(failure,assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->service.atualizarCotacao(1L)));
        verify(alpha,times(1)).consultarCandidataAtualizacao(any(),any(),any(),any());
        verify(persistence,times(1)).aplicar(eq(1L),any());
    }

}
