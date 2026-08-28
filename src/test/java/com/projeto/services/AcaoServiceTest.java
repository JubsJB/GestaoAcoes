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
import org.springframework.data.jpa.repository.Lock;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
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
    private AcaoCotacaoPersistenceService cotacaoPersistenceService;

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
                cotacaoPersistenceService,
                repository,
                new AcaoMapper(),
                Clock.fixed(FALLBACK_INSTANT, ZoneOffset.UTC)
        );
        clearInvocations(brapi, alphaVantage, persistenceService, cotacaoPersistenceService, repository);
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
    void atualizaCotacaoBrasilForaDeTransacaoComTimestampDoProvider() {
        Acao existente = action(10L, "PETR4", "Nome persistido", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("30.000000"), OffsetDateTime.parse("2026-08-19T15:30:00Z"));
        Acao persistida = action(10L, "PETR4", "Nome persistido", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("32.123456"), OffsetDateTime.parse("2026-08-20T15:15:00Z"));
        when(repository.findById(10L)).thenReturn(Optional.of(existente));
        when(brapi.consultar("PETR4")).thenAnswer(invocation -> {
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive());
            return new CotacaoData("PETR4", "Nome externo", "BRL", new BigDecimal("32.123456"),
                    OffsetDateTime.parse("2026-08-20T12:15:00-03:00"), false);
        });
        when(cotacaoPersistenceService.atualizarSePosterior(
                10L, new BigDecimal("32.123456"), OffsetDateTime.parse("2026-08-20T15:15:00Z")))
                .thenReturn(persistida);

        AcaoResponse resposta = service.atualizarCotacao(10L);

        assertEquals("Nome persistido", resposta.nomeEmpresa());
        assertEquals(new BigDecimal("32.123456"), resposta.cotacaoAtual());
        verify(brapi).consultar("PETR4");
        verifyNoInteractions(alphaVantage);
    }

    @Test
    void atualizaCotacaoEuaComFallbackDoClock() {
        Acao existente = action(11L, "AAPL", "Apple", Mercado.EUA, Moeda.USD,
                new BigDecimal("220.000000"), OffsetDateTime.parse("2026-08-19T15:30:00Z"));
        Acao persistida = action(11L, "AAPL", "Apple", Mercado.EUA, Moeda.USD,
                new BigDecimal("224.410000"), OffsetDateTime.parse("2026-08-20T15:30:00Z"));
        when(repository.findById(11L)).thenReturn(Optional.of(existente));
        when(alphaVantage.consultar("AAPL")).thenReturn(
                new CotacaoData("AAPL", "Apple Inc.", "USD", new BigDecimal("224.41"), null, false));
        when(cotacaoPersistenceService.atualizarSePosterior(
                11L, new BigDecimal("224.410000"), OffsetDateTime.parse("2026-08-20T15:30:00Z")))
                .thenReturn(persistida);

        AcaoResponse resposta = service.atualizarCotacao(11L);

        assertEquals(Moeda.USD, resposta.moeda());
        assertEquals(new BigDecimal("224.410000"), resposta.cotacaoAtual());
        verify(alphaVantage).consultar("AAPL");
        verifyNoInteractions(brapi);
    }

    @Test
    void idInexistenteNaoConsultaProvidersNemPersiste() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundException.class, () -> service.atualizarCotacao(404L));

        verifyNoInteractions(brapi, alphaVantage, cotacaoPersistenceService);
    }

    @Test
    void rejeitaTickerCanonicoDivergenteEPreservaCotacao() {
        Acao existente = action(12L, "PETR4", "Empresa", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("30.000000"), OffsetDateTime.parse("2026-08-19T15:30:00Z"));
        when(repository.findById(12L)).thenReturn(Optional.of(existente));
        when(brapi.consultar("PETR4")).thenReturn(
                new CotacaoData("NEW3", "Empresa", "BRL", new BigDecimal("36"), null, true));

        ApiException erro = assertThrows(ApiException.class, () -> service.atualizarCotacao(12L));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, erro.getStatus());
        assertEquals(ErrorCodes.TICKER_CANONICO_DIVERGENTE, erro.getCode());
        assertEquals("PETR4", erro.getDetails().get("tickerPersistido"));
        assertEquals("NEW3", erro.getDetails().get("tickerCanonicoRetornado"));
        assertEquals(true, erro.getDetails().get("cotacaoPreservada"));
        assertEquals(new BigDecimal("30.000000"), erro.getDetails().get("ultimaCotacaoValida"));
        verifyNoInteractions(cotacaoPersistenceService, alphaVantage);
    }

    @Test
    void preservaCotacaoNosErrosExternosEDeValidacaoSemEscrita() {
        Acao existente = action(12L, "PETR4", "Empresa", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("30.000000"), OffsetDateTime.parse("2026-08-19T15:30:00Z"));
        List<ApiException> falhas = List.of(
                new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, ErrorCodes.TICKER_INEXISTENTE, "Ticker inexistente"),
                new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO, "Limite"),
                new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL, "Indisponível"),
                new ApiException(org.springframework.http.HttpStatus.GATEWAY_TIMEOUT, ErrorCodes.SERVICO_EXTERNO_TIMEOUT, "Timeout")
        );
        for (ApiException falha : falhas) {
            reset(brapi);
            clearInvocations(alphaVantage, cotacaoPersistenceService, repository);
            when(repository.findById(12L)).thenReturn(Optional.of(existente));
            org.mockito.Mockito.doThrow(falha).when(brapi).consultar("PETR4");
            ApiException erro = assertThrows(ApiException.class, () -> service.atualizarCotacao(12L));
            assertEquals(falha.getCode(), erro.getCode());
            assertEquals(12L, erro.getDetails().get("acaoId"));
            assertEquals(true, erro.getDetails().get("cotacaoPreservada"));
            verifyNoInteractions(cotacaoPersistenceService, alphaVantage);
        }

        reset(brapi);
        clearInvocations(alphaVantage, cotacaoPersistenceService, repository);
        when(repository.findById(12L)).thenReturn(Optional.of(existente));
        when(brapi.consultar("PETR4")).thenReturn(
                new CotacaoData("PETR4", "Empresa", "BRL", new BigDecimal("1.1234567"), null, false));
        ApiException precisao = assertThrows(ApiException.class, () -> service.atualizarCotacao(12L));
        assertEquals(ErrorCodes.COTACAO_FORA_DA_PRECISAO, precisao.getCode());
        verifyNoInteractions(cotacaoPersistenceService, alphaVantage);
    }

    @Test
    void retornaEstadoPersistidoParaTimestampIgualOuAnterior() {
        Acao existente = action(12L, "PETR4", "Empresa", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("30.000000"), OffsetDateTime.parse("2026-08-19T15:30:00Z"));
        when(repository.findById(12L)).thenReturn(Optional.of(existente));
        when(brapi.consultar("PETR4")).thenReturn(new CotacaoData(
                "PETR4", "Empresa", "BRL", new BigDecimal("30"),
                OffsetDateTime.parse("2026-08-19T15:30:00Z"), false));
        when(cotacaoPersistenceService.atualizarSePosterior(
                eq(12L), eq(new BigDecimal("30.000000")), eq(OffsetDateTime.parse("2026-08-19T15:30:00Z"))))
                .thenReturn(existente);

        AcaoResponse resposta = service.atualizarCotacao(12L);

        assertEquals(new BigDecimal("30.000000"), resposta.cotacaoAtual());
        assertEquals(OffsetDateTime.parse("2026-08-19T15:30:00Z"), resposta.dataHoraCotacao());
    }

    @Test
    void rejeitaPayloadIncompletoCotacaoInvalidaEIdentidadeIncompativelPreservandoEstado() {
        List<CotacaoData> dados = List.of(
                new CotacaoData("PETR4", " ", "BRL", BigDecimal.TEN, null, false),
                new CotacaoData("VALE3", "Empresa", "BRL", BigDecimal.TEN, null, false),
                new CotacaoData("PETR4", "Empresa", "USD", BigDecimal.TEN, null, false),
                new CotacaoData("PETR4", "Empresa", "BRL", BigDecimal.ZERO, null, false),
                new CotacaoData("PETR4", "Empresa", "BRL", BigDecimal.ONE.negate(), null, false)
        );
        List<String> codigos = List.of(
                ErrorCodes.DADOS_EXTERNOS_INCOMPLETOS,
                ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,
                ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,
                ErrorCodes.COTACAO_INDISPONIVEL,
                ErrorCodes.COTACAO_INDISPONIVEL
        );

        for (int indice = 0; indice < dados.size(); indice++) {
            reset(brapi);
            clearInvocations(alphaVantage, cotacaoPersistenceService, repository);
            Acao existente = action(12L, "PETR4", "Empresa", Mercado.BRASIL, Moeda.BRL,
                    new BigDecimal("30.000000"), OffsetDateTime.parse("2026-08-19T15:30:00Z"));
            when(repository.findById(12L)).thenReturn(Optional.of(existente));
            when(brapi.consultar("PETR4")).thenReturn(dados.get(indice));

            ApiException erro = assertThrows(ApiException.class, () -> service.atualizarCotacao(12L));

            assertEquals(codigos.get(indice), erro.getCode());
            assertEquals(true, erro.getDetails().get("cotacaoPreservada"));
            assertEquals(OffsetDateTime.parse("2026-08-19T15:30:00Z"),
                    erro.getDetails().get("dataHoraUltimaCotacao"));
            verifyNoInteractions(cotacaoPersistenceService, alphaVantage);
        }
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

    @Test
    void findsBrazilianAndAmericanActionsByNormalizedTickerAndMarketWithCompleteResponse() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-20T15:30:00Z");
        Acao brazil = action(10L, "ABC", "Empresa Brasil", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("10.000000"), timestamp);
        Acao usa = action(20L, "ABC", "Empresa EUA", Mercado.EUA, Moeda.USD,
                new BigDecimal("11.000000"), timestamp);
        when(repository.findByTickerAndMercado("ABC", Mercado.BRASIL)).thenReturn(Optional.of(brazil));
        when(repository.findByTickerAndMercado("ABC", Mercado.EUA)).thenReturn(Optional.of(usa));

        AcaoResponse brazilResponse = service.buscarPorTickerEMercado(" abc ", Mercado.BRASIL);
        AcaoResponse usaResponse = service.buscarPorTickerEMercado("ABC", Mercado.EUA);

        assertEquals(10L, brazilResponse.id());
        assertEquals("ABC", brazilResponse.ticker());
        assertEquals("Empresa Brasil", brazilResponse.nomeEmpresa());
        assertEquals(Mercado.BRASIL, brazilResponse.mercado());
        assertEquals(Moeda.BRL, brazilResponse.moeda());
        assertEquals(new BigDecimal("10.000000"), brazilResponse.cotacaoAtual());
        assertEquals(timestamp, brazilResponse.dataHoraCotacao());
        assertEquals(20L, usaResponse.id());
        assertEquals(Mercado.EUA, usaResponse.mercado());
        assertEquals(Moeda.USD, usaResponse.moeda());
        verify(repository).findByTickerAndMercado("ABC", Mercado.BRASIL);
        verify(repository).findByTickerAndMercado("ABC", Mercado.EUA);
        verifyNoInteractions(brapi, alphaVantage, persistenceService, cotacaoPersistenceService);
    }

    @Test
    void rejectsInvalidTickersBeforeRepositoryOrExternalDependencies() {
        List<String> invalidTickers = java.util.Arrays.asList(
                null,
                "",
                "   ",
                "A".repeat(31)
        );

        for (String ticker : invalidTickers) {
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> service.buscarPorTickerEMercado(ticker, Mercado.BRASIL)
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertEquals(ErrorCodes.TICKER_INVALIDO, exception.getCode());
        }

        verifyNoInteractions(repository, brapi, alphaVantage, persistenceService, cotacaoPersistenceService);
    }

    @Test
    void rejectsMissingMarketBeforeRepositoryOrExternalDependencies() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.buscarPorTickerEMercado("PETR4", null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(ErrorCodes.REQUEST_INVALIDO, exception.getCode());
        verifyNoInteractions(repository, brapi, alphaVantage, persistenceService, cotacaoPersistenceService);
    }

    @Test
    void throwsObjectNotFoundForMissingTickerAndMarketUsingOneRepositoryQuery() {
        when(repository.findByTickerAndMercado("MISSING", Mercado.EUA)).thenReturn(Optional.empty());

        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> service.buscarPorTickerEMercado(" missing ", Mercado.EUA)
        );

        assertEquals("Ação não encontrada para o ticker e mercado: MISSING / EUA", exception.getMessage());
        verify(repository, times(1)).findByTickerAndMercado("MISSING", Mercado.EUA);
        verifyNoInteractions(brapi, alphaVantage, persistenceService, cotacaoPersistenceService);
    }

    @Test
    void tickerAndMarketLookupIsReadOnlyWithDefaultIsolationAndNoLock() throws Exception {
        Method method = AcaoService.class.getMethod(
                "buscarPorTickerEMercado",
                String.class,
                Mercado.class
        );
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertTrue(transactional.readOnly());
        assertEquals(Isolation.DEFAULT, transactional.isolation());
        assertNull(method.getAnnotation(Lock.class));
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
