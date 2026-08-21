package com.projeto.services;

import com.projeto.dto.AcaoCreateRequest;
import com.projeto.dto.AcaoResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.integrations.cotacao.CotacaoData;
import com.projeto.integrations.cotacao.CotacaoProvider;
import com.projeto.mappers.AcaoMapper;
import com.projeto.repositories.AcaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.validation.TickerNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcaoServiceTest {

    private static final Instant FALLBACK_INSTANT = Instant.parse("2026-08-20T15:30:00Z");

    @Mock
    private CotacaoProvider brapi;

    @Mock
    private CotacaoProvider alphaVantage;

    @Mock
    private AcaoPersistenceService persistenceService;

    @Mock
    private AcaoRepository repository;

    private AcaoService service;

    @BeforeEach
    void setUp() {
        when(brapi.mercado()).thenReturn(Mercado.BRASIL);
        when(alphaVantage.mercado()).thenReturn(Mercado.EUA);
        service = new AcaoService(
                new TickerNormalizer(),
                List.of(brapi, alphaVantage),
                persistenceService,
                repository,
                new AcaoMapper(),
                Clock.fixed(FALLBACK_INSTANT, ZoneOffset.UTC)
        );
        clearInvocations(brapi, alphaVantage, persistenceService, repository);
    }

    @Test
    void registersBrazilianActionWithCanonicalTickerBrlAndProviderTimestampInUtc() {
        OffsetDateTime providerTime = OffsetDateTime.parse("2026-08-20T12:15:00-03:00");
        when(brapi.consultar("OLD3")).thenReturn(new CotacaoData(
                "NEW3", "Empresa Brasileira", "BRL", new BigDecimal("12.3456"), providerTime, true
        ));
        when(persistenceService.saveUnique(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AcaoResponse response = service.cadastrar(new AcaoCreateRequest(" old3 ", Mercado.BRASIL));

        assertEquals("NEW3", response.ticker());
        assertEquals(Moeda.BRL, response.moeda());
        assertEquals(new BigDecimal("12.345600"), response.cotacaoAtual());
        assertEquals(OffsetDateTime.parse("2026-08-20T15:15:00Z"), response.dataHoraCotacao());
        verify(brapi).consultar("OLD3");
        verifyNoInteractions(alphaVantage);

        ArgumentCaptor<Acao> captor = ArgumentCaptor.forClass(Acao.class);
        verify(persistenceService).saveUnique(captor.capture());
        assertEquals("NEW3", captor.getValue().getTicker());
    }

    @Test
    void registersAmericanActionWithUsdLatestAvailablePriceAndClockFallback() {
        when(alphaVantage.consultar("AAPL")).thenReturn(new CotacaoData(
                "AAPL", "Apple Inc.", "USD", new BigDecimal("224.41"), null, false
        ));
        when(persistenceService.saveUnique(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AcaoResponse response = service.cadastrar(new AcaoCreateRequest(" aapl ", Mercado.EUA));

        assertEquals("AAPL", response.ticker());
        assertEquals(Moeda.USD, response.moeda());
        assertEquals(new BigDecimal("224.410000"), response.cotacaoAtual());
        assertEquals(OffsetDateTime.parse("2026-08-20T15:30:00Z"), response.dataHoraCotacao());
        verify(alphaVantage).consultar("AAPL");
        verifyNoInteractions(brapi);
    }

    @Test
    void usesUtcClockFallbackForBrazilWhenProviderTimestampIsUnusable() {
        when(brapi.consultar("PETR4")).thenReturn(new CotacaoData(
                "PETR4", "Empresa Brasileira", "BRL", new BigDecimal("32.10"), null, false
        ));
        when(persistenceService.saveUnique(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AcaoResponse response = service.cadastrar(new AcaoCreateRequest("PETR4", Mercado.BRASIL));

        assertEquals(OffsetDateTime.parse("2026-08-20T15:30:00Z"), response.dataHoraCotacao());
    }

    @Test
    void invalidLocalTickerDoesNotCallProvidersOrPersistence() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cadastrar(new AcaoCreateRequest("   ", Mercado.EUA))
        );

        assertEquals(ErrorCodes.TICKER_INVALIDO, exception.getCode());
        verifyNoInteractions(brapi, alphaVantage, persistenceService, repository);
    }

    @Test
    void propagatesTickerNotFoundFromSelectedProviderWithoutSaving() {
        ApiException notFound = new ApiException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                ErrorCodes.TICKER_INEXISTENTE,
                "Ticker inexistente"
        );
        when(alphaVantage.consultar("INVALID")).thenThrow(notFound);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cadastrar(new AcaoCreateRequest("invalid", Mercado.EUA))
        );

        assertEquals(ErrorCodes.TICKER_INEXISTENTE, exception.getCode());
        verify(persistenceService, never()).saveUnique(any());
        verifyNoInteractions(brapi);
    }

    @Test
    void rejectsMissingNameWrongCurrencyAndDifferentUnconfirmedTicker() {
        assertExternalFailure(
                new CotacaoData("PETR4", " ", "BRL", BigDecimal.TEN, null, false),
                ErrorCodes.DADOS_EXTERNOS_INCOMPLETOS
        );
        assertExternalFailure(
                new CotacaoData("PETR4", "Empresa", "USD", BigDecimal.TEN, null, false),
                ErrorCodes.RESPOSTA_EXTERNA_INVALIDA
        );
        assertExternalFailure(
                new CotacaoData("VALE3", "Empresa", "BRL", BigDecimal.TEN, null, false),
                ErrorCodes.RESPOSTA_EXTERNA_INVALIDA
        );
    }

    @Test
    void rejectsUnavailableNonPositiveAndNonRepresentableQuotesWithoutSaving() {
        assertQuoteFailure(null, ErrorCodes.COTACAO_INDISPONIVEL);
        assertQuoteFailure(BigDecimal.ZERO, ErrorCodes.COTACAO_INDISPONIVEL);
        assertQuoteFailure(new BigDecimal("-1"), ErrorCodes.COTACAO_INDISPONIVEL);
        assertQuoteFailure(new BigDecimal("1.1234567"), ErrorCodes.COTACAO_FORA_DA_PRECISAO);
        assertQuoteFailure(new BigDecimal("10000000000000.000000"), ErrorCodes.COTACAO_FORA_DA_PRECISAO);
    }

    @Test
    void knownDuplicateStopsBeforeProviderAndCanonicalDuplicateStopsAtFinalPersistence() {
        ApiException duplicate = new ApiException(
                org.springframework.http.HttpStatus.CONFLICT,
                ErrorCodes.ACAO_DUPLICADA,
                "duplicada"
        );
        org.mockito.Mockito.doThrow(duplicate)
                .when(persistenceService).ensureAvailable("AAPL", Mercado.EUA);

        ApiException early = assertThrows(
                ApiException.class,
                () -> service.cadastrar(new AcaoCreateRequest("aapl", Mercado.EUA))
        );
        assertEquals(ErrorCodes.ACAO_DUPLICADA, early.getCode());
        verify(alphaVantage, never()).consultar(any());

        clearInvocations(persistenceService, brapi, alphaVantage);
        when(brapi.consultar("OLD3")).thenReturn(new CotacaoData(
                "NEW3", "Empresa", "BRL", BigDecimal.TEN, null, true
        ));
        when(persistenceService.saveUnique(any())).thenThrow(duplicate);

        ApiException canonical = assertThrows(
                ApiException.class,
                () -> service.cadastrar(new AcaoCreateRequest("OLD3", Mercado.BRASIL))
        );
        assertEquals(ErrorCodes.ACAO_DUPLICADA, canonical.getCode());
    }

    @Test
    void listsPersistedActionsUsingAscendingIdSortAndPreservesEveryField() {
        OffsetDateTime firstTimestamp = OffsetDateTime.parse("2026-08-19T18:45:00Z");
        OffsetDateTime secondTimestamp = OffsetDateTime.parse("2026-08-20T15:30:00Z");
        Acao first = action(
                1L,
                "PETR4",
                "Petróleo Brasileiro S.A.",
                Mercado.BRASIL,
                Moeda.BRL,
                new BigDecimal("32.123456"),
                firstTimestamp
        );
        Acao second = action(
                2L,
                "AAPL",
                "Apple Inc.",
                Mercado.EUA,
                Moeda.USD,
                new BigDecimal("224.410000"),
                secondTimestamp
        );
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(first, second));

        List<AcaoResponse> response = service.listar();

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(sortCaptor.capture());
        assertEquals(Sort.Direction.ASC, sortCaptor.getValue().getOrderFor("id").getDirection());
        assertEquals(List.of(1L, 2L), response.stream().map(AcaoResponse::id).toList());
        assertEquals("PETR4", response.get(0).ticker());
        assertEquals("Petróleo Brasileiro S.A.", response.get(0).nomeEmpresa());
        assertEquals(Mercado.BRASIL, response.get(0).mercado());
        assertEquals(Moeda.BRL, response.get(0).moeda());
        assertEquals(new BigDecimal("32.123456"), response.get(0).cotacaoAtual());
        assertEquals(firstTimestamp, response.get(0).dataHoraCotacao());
        assertEquals("AAPL", response.get(1).ticker());
        assertEquals(secondTimestamp, response.get(1).dataHoraCotacao());
        verifyNoInteractions(brapi, alphaVantage, persistenceService);
    }

    @Test
    void returnsEmptyListWhenNoActionIsPersistedWithoutExternalCalls() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of());

        List<AcaoResponse> response = service.listar();

        assertTrue(response.isEmpty());
        verifyNoInteractions(brapi, alphaVantage, persistenceService);
    }

    @Test
    void findsPersistedActionByIdWithoutExternalCallsOrPersistence() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-20T15:30:00Z");
        Acao persisted = action(
                7L,
                "MSFT",
                "Microsoft Corporation",
                Mercado.EUA,
                Moeda.USD,
                new BigDecimal("501.250000"),
                timestamp
        );
        when(repository.findById(7L)).thenReturn(Optional.of(persisted));

        AcaoResponse response = service.buscarPorId(7L);

        assertEquals(7L, response.id());
        assertEquals("MSFT", response.ticker());
        assertEquals("Microsoft Corporation", response.nomeEmpresa());
        assertEquals(Mercado.EUA, response.mercado());
        assertEquals(Moeda.USD, response.moeda());
        assertEquals(new BigDecimal("501.250000"), response.cotacaoAtual());
        assertEquals(timestamp, response.dataHoraCotacao());
        verify(repository).findById(7L);
        verifyNoInteractions(brapi, alphaVantage, persistenceService);
    }

    @Test
    void throwsObjectNotFoundForMissingActionIdWithoutExternalCallsOrPersistence() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> service.buscarPorId(99L)
        );

        assertEquals("Ação não encontrada para o id: 99", exception.getMessage());
        verify(repository).findById(99L);
        verifyNoInteractions(brapi, alphaVantage, persistenceService);
    }

    private void assertExternalFailure(CotacaoData data, String expectedCode) {
        clearInvocations(persistenceService, brapi, alphaVantage);
        when(brapi.consultar("PETR4")).thenReturn(data);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cadastrar(new AcaoCreateRequest("PETR4", Mercado.BRASIL))
        );

        assertEquals(expectedCode, exception.getCode());
        verify(persistenceService, never()).saveUnique(any());
    }

    private void assertQuoteFailure(BigDecimal quote, String expectedCode) {
        assertExternalFailure(
                new CotacaoData("PETR4", "Empresa", "BRL", quote, null, false),
                expectedCode
        );
    }

    private Acao action(
            Long id,
            String ticker,
            String companyName,
            Mercado market,
            Moeda currency,
            BigDecimal quote,
            OffsetDateTime timestamp
    ) {
        Acao action = new Acao(ticker, companyName, market, currency, quote, timestamp);
        ReflectionTestUtils.setField(action, "id", id);
        return action;
    }
}
