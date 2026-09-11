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
    private final AcaoCotacaoPersistenceService persistence = mock(AcaoCotacaoPersistenceService.class);
    private final Instant now = Instant.parse("2026-09-09T16:00:00Z");
    private final AcaoService service;

    AlphaVantageConsumptionTest() {
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(alpha.mercado()).thenReturn(Mercado.EUA);
        when(brapi.mercado()).thenReturn(Mercado.BRASIL);
        service = new AcaoService(new TickerNormalizer(), List.of(alpha, brapi),
                mock(AcaoPersistenceService.class), persistence, repository, new AcaoMapper(), clock);
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
                new CotacaoData("AAPL", "Apple", "USD", new BigDecimal("101.123456"), null, false));
        when(persistence.atualizarSePosterior(eq(1L), any(), any())).thenAnswer(i -> {
            a.atualizarCotacao(i.getArgument(1), i.getArgument(2));
            return a;
        });
    }

    @Test void recentQuoteDoesNotCallOrPersist() {
        Acao a = action(now.minusSeconds(899), Mercado.EUA);
        assertEquals(a.getCotacaoAtual(), service.atualizarCotacao(1L).cotacaoAtual());
        verifyNoInteractions(alpha, persistence);
    }

    @Test void exactExpiryCallsOnceAndPersistsExactValue() {
        Acao a = action(now.minusSeconds(900), Mercado.EUA); success(a);
        assertEquals(new BigDecimal("101.123456"), service.atualizarCotacao(1L).cotacaoAtual());
        service.atualizarCotacao(1L);
        verify(alpha, times(1)).consultarAtualizacao("AAPL", "Apple", "USD");
        verify(persistence, times(1)).atualizarSePosterior(1L, new BigDecimal("101.123456"), now.atOffset(ZoneOffset.UTC));
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
                new CotacaoData("AAPL", "Apple", "USD", persisted.getCotacaoAtual(), null, false));
        when(persistence.atualizarSePosterior(eq(1L), any(), any())).thenReturn(persisted);
        service.atualizarCotacao(1L);
        assertEquals(persisted.getCotacaoAtual(), service.atualizarCotacao(1L).cotacaoAtual());
        verify(alpha, times(1)).consultarAtualizacao("AAPL", "Apple", "USD");
    }

    @Test void failureCooldownPreservesQuoteAndRetriesOnlyAfterExpiry() {
        for (String code : List.of(ErrorCodes.SERVICO_EXTERNO_TIMEOUT,
                ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL)) {
            ReflectionTestUtils.setField(service, "refreshAttempts", new ConcurrentHashMap<>());
            reset(alpha); when(clock.instant()).thenReturn(now);
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
                new BigDecimal("102"), null, false));
        when(persistence.atualizarSePosterior(eq(1L), any(), any())).thenReturn(a);
        service.atualizarCotacao(1L); service.atualizarCotacao(1L);
        verify(brapi, times(2)).consultar("AAPL"); verifyNoInteractions(alpha);
    }

    @Test void intervalsMustBePositive() {
        ReflectionTestUtils.setField(service, "refreshCooldown", Duration.ZERO);
        assertThrows(IllegalArgumentException.class, service::validateRefreshIntervals);
    }
}
