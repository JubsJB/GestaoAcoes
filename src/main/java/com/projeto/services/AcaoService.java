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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AcaoService {

    private static final int MAX_TICKER_LENGTH = 30;
    private static final int MAX_NAME_LENGTH = 255;

    private final TickerNormalizer tickerNormalizer;
    private final Map<Mercado, CotacaoProvider> providers;
    private final AcaoPersistenceService persistenceService;
    private final AcaoCotacaoPersistenceService cotacaoPersistenceService;
    private final AcaoRepository repository;
    private final AcaoMapper mapper;
    private final Clock clock;

    @Value("${integration.alpha-vantage.quote-reuse-interval:15m}")
    private Duration quoteReuseInterval = Duration.ofMinutes(15);
    @Value("${integration.alpha-vantage.refresh-cooldown:15m}")
    private Duration refreshCooldown = Duration.ofMinutes(15);
    private final Map<String, RefreshAttempt> refreshAttempts = new ConcurrentHashMap<>();
    private final Object[] refreshLocks = java.util.stream.IntStream.range(0, 64)
            .mapToObj(i -> new Object()).toArray();

    @PostConstruct
    void validateRefreshIntervals() {
        if (quoteReuseInterval.isNegative() || quoteReuseInterval.isZero()
                || refreshCooldown.isNegative() || refreshCooldown.isZero()) {
            throw new IllegalArgumentException("Alpha Vantage refresh intervals must be positive");
        }
    }

    private record RefreshAttempt(Instant completedAt, AcaoResponse result, ApiException failure) { }


    public AcaoService(
            TickerNormalizer tickerNormalizer,
            List<CotacaoProvider> providers,
            AcaoPersistenceService persistenceService,
            AcaoCotacaoPersistenceService cotacaoPersistenceService,
            AcaoRepository repository,
            AcaoMapper mapper,
            Clock clock
    ) {
        this.tickerNormalizer = tickerNormalizer;
        this.providers = indexProviders(providers);
        this.persistenceService = persistenceService;
        this.cotacaoPersistenceService = cotacaoPersistenceService;
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    public AcaoResponse cadastrar(AcaoCreateRequest request) {
        if (request == null || request.getMercado() == null) {
            throw invalidRequest();
        }

        String requestedTicker = tickerNormalizer.normalizeAndValidate(request.getTicker());
        Mercado mercado = request.getMercado();
        persistenceService.ensureAvailable(requestedTicker, mercado);

        CotacaoProvider provider = providerFor(mercado);

        CotacaoData externalData = provider.consultar(requestedTicker);
        ValidatedQuote validated = validateExternalData(externalData, requestedTicker, mercado);
        OffsetDateTime quoteTime = externalData.dataHoraCotacao() == null
                ? OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                : externalData.dataHoraCotacao().withOffsetSameInstant(ZoneOffset.UTC);

        Acao acao = new Acao(
                validated.ticker(),
                validated.companyName(),
                mercado,
                validated.currency(),
                validated.quote(),
                quoteTime
        );
        return mapper.toResponse(persistenceService.saveUnique(acao));
    }

    @Transactional(readOnly = true)
    public List<AcaoResponse> listar() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcaoResponse buscarPorId(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Ação não encontrada para o id: " + id
                ));

        return mapper.toResponse(acao);
    }

    @Transactional(readOnly = true)
    public AcaoResponse buscarPorTickerEMercado(String ticker, Mercado mercado) {
        String tickerNormalizado = tickerNormalizer.normalizeAndValidate(ticker);
        if (mercado == null) {
            throw invalidRequest();
        }

        Acao acao = repository.findByTickerAndMercado(tickerNormalizado, mercado)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Ação não encontrada para o ticker e mercado: "
                                + tickerNormalizado + " / " + mercado
                ));

        return mapper.toResponse(acao);
    }

    public AcaoResponse atualizarCotacao(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Ação não encontrada para o id: " + id
                ));

        if (acao.getMercado() != Mercado.EUA) return atualizarCotacaoPersistida(acao);
        // Local coordination includes the database reread and final persistence.
        synchronized (refreshLocks[Math.floorMod(acao.getTicker().hashCode(), refreshLocks.length)]) {
            Acao current = repository.findById(id).orElseThrow(() ->
                    new ObjectNotFoundException("Acao nao encontrada para o id: " + id));
            Instant now = clock.instant();
            RefreshAttempt previous = refreshAttempts.get(current.getTicker());
            if (previous != null && now.isBefore(previous.completedAt().plus(refreshCooldown))) {
                if (previous.failure() != null) throw withPreservedQuote(previous.failure(), current);
                // A concurrent request may still hold an older JPA first-level-cache entity.
                if (current.getDataHoraCotacao() == null || previous.result().dataHoraCotacao()
                        .isAfter(current.getDataHoraCotacao())) return previous.result();
                return mapper.toResponse(current);
            }
            if (current.getCotacaoAtual() != null && current.getCotacaoAtual().signum() > 0
                    && current.getDataHoraCotacao() != null
                    && !current.getDataHoraCotacao().toInstant().isAfter(now)
                    && now.isBefore(current.getDataHoraCotacao().toInstant().plus(quoteReuseInterval))) {
                return mapper.toResponse(current);
            }
            try {
                AcaoResponse result = atualizarCotacaoPersistida(current);
                refreshAttempts.put(current.getTicker(), new RefreshAttempt(clock.instant(), result, null));
                return result;
            } catch (ApiException failure) {
                refreshAttempts.put(current.getTicker(), new RefreshAttempt(clock.instant(), null, failure));
                throw failure;
            }
        }
    }

    private AcaoResponse atualizarCotacaoPersistida(Acao acao) {
        try {
            CotacaoProvider provider = providerFor(acao.getMercado());
            CotacaoData externalData = acao.getMercado() == Mercado.EUA
                    ? provider.consultarAtualizacao(acao.getTicker(), acao.getNomeEmpresa(), acao.getMoeda().name())
                    : provider.consultar(acao.getTicker());
            ValidatedQuote validated = validateUpdateExternalData(externalData, acao);
            OffsetDateTime quoteTime = externalData.dataHoraCotacao() == null
                    ? OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    : externalData.dataHoraCotacao().withOffsetSameInstant(ZoneOffset.UTC);

            Acao persisted = cotacaoPersistenceService.atualizarSePosterior(
                    acao.getId(),
                    validated.quote(),
                    quoteTime
            );
            return mapper.toResponse(persisted);
        } catch (ApiException exception) {
            throw withPreservedQuote(exception, acao);
        }
    }

    private Map<Mercado, CotacaoProvider> indexProviders(List<CotacaoProvider> providerList) {
        Map<Mercado, CotacaoProvider> indexed = new EnumMap<>(Mercado.class);
        for (CotacaoProvider provider : providerList) {
            Mercado supportedMarket = provider.mercado();
            if (supportedMarket == null || indexed.putIfAbsent(supportedMarket, provider) != null) {
                throw new IllegalStateException("Configuração ambígua de provider de cotação");
            }
        }
        return Map.copyOf(indexed);
    }

    private CotacaoProvider providerFor(Mercado mercado) {
        CotacaoProvider provider = providers.get(mercado);
        if (provider == null) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,
                    "Serviço externo indisponível para o mercado informado"
            );
        }
        return provider;
    }

    private ValidatedQuote validateUpdateExternalData(CotacaoData data, Acao acao) {
        if (data != null && !isBlank(data.ticker()) && data.tickerAlteradoExplicitamente()) {
            String returnedTicker = data.ticker().trim().toUpperCase(Locale.ROOT);
            if (returnedTicker.length() <= MAX_TICKER_LENGTH
                    && !acao.getTicker().equals(returnedTicker)) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("tickerPersistido", acao.getTicker());
                details.put("tickerCanonicoRetornado", returnedTicker);
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        ErrorCodes.TICKER_CANONICO_DIVERGENTE,
                        "O provider informou um ticker canônico diferente do persistido",
                        details
                );
            }
        }

        return validateExternalData(data, acao.getTicker(), acao.getMercado());
    }

    private ValidatedQuote validateExternalData(
            CotacaoData data,
            String requestedTicker,
            Mercado market
    ) {
        if (data == null || isBlank(data.ticker()) || isBlank(data.nomeEmpresa())) {
            throw incompleteExternalData();
        }

        String returnedTicker = data.ticker().trim().toUpperCase(Locale.ROOT);
        if (returnedTicker.length() > MAX_TICKER_LENGTH) {
            throw invalidExternalResponse();
        }

        String finalTicker;
        if (market == Mercado.BRASIL && data.tickerAlteradoExplicitamente()) {
            finalTicker = returnedTicker;
        } else {
            if (!requestedTicker.equals(returnedTicker)) {
                throw invalidExternalResponse();
            }
            finalTicker = requestedTicker;
        }

        String companyName = data.nomeEmpresa().trim();
        if (companyName.length() > MAX_NAME_LENGTH) {
            throw invalidExternalResponse();
        }

        Moeda expectedCurrency = market == Mercado.BRASIL ? Moeda.BRL : Moeda.USD;
        if (isBlank(data.moeda()) || !expectedCurrency.name().equals(data.moeda().trim().toUpperCase(Locale.ROOT))) {
            throw invalidExternalResponse();
        }

        BigDecimal quote = validateQuote(data.cotacao());
        return new ValidatedQuote(finalTicker, companyName, expectedCurrency, quote);
    }

    private BigDecimal validateQuote(BigDecimal quote) {
        if (quote == null || quote.signum() <= 0) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    ErrorCodes.COTACAO_INDISPONIVEL,
                    "Cotação indisponível"
            );
        }

        try {
            BigDecimal exact = quote.setScale(6, RoundingMode.UNNECESSARY);
            if (exact.precision() > 19) {
                throw precisionError();
            }
            return exact;
        } catch (ArithmeticException exception) {
            throw precisionError();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ApiException invalidRequest() {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_INVALIDO, "Dados da requisição inválidos");
    }

    private ApiException incompleteExternalData() {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCodes.DADOS_EXTERNOS_INCOMPLETOS,
                "Dados obrigatórios ausentes na resposta do provider de cotação"
        );
    }

    private ApiException invalidExternalResponse() {
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,
                "Resposta inválida do provider de cotação"
        );
    }

    private ApiException precisionError() {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCodes.COTACAO_FORA_DA_PRECISAO,
                "Cotação fora da precisão suportada"
        );
    }

    private ApiException withPreservedQuote(ApiException exception, Acao acao) {
        Map<String, Object> details = new LinkedHashMap<>(exception.getDetails());
        details.put("acaoId", acao.getId());
        details.put("cotacaoPreservada", true);
        details.put("ultimaCotacaoValida", acao.getCotacaoAtual());
        details.put("dataHoraUltimaCotacao", acao.getDataHoraCotacao());
        return new ApiException(
                exception.getStatus(),
                exception.getCode(),
                exception.getMessage(),
                details
        );
    }

    private record ValidatedQuote(String ticker, String companyName, Moeda currency, BigDecimal quote) {
    }
}
