package com.projeto.integrations.cotacao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projeto.entities.Mercado;
import com.projeto.integrations.ExternalApiErrorMapper;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AlphaVantageAdapter implements CotacaoProvider {

    private static final String PROVIDER = "Alpha Vantage";
    private static final Set<String> US_REGIONS = Set.of("UNITED STATES", "USA", "US");

    private final RestClient restClient;
    private final String apiKey;

    public AlphaVantageAdapter(
            @Qualifier("alphaVantageRestClient") RestClient restClient,
            @Value("${integration.alpha-vantage.api-key:}") String apiKey
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    public Mercado mercado() {
        return Mercado.EUA;
    }

    @Override
    public CotacaoData consultar(String ticker) {
        ensureConfigured();
        try {
            SearchResponse search = getSearch(ticker);
            inspectPayload(search.note(), search.information(), search.errorMessage());
            SearchMatch exact = findExactUsMatch(search.bestMatches(), ticker);
            if (exact == null) {
                throw tickerNotFound();
            }
            if (!"USD".equals(normalize(exact.currency()))) {
                throw ExternalApiErrorMapper.invalidResponse(PROVIDER);
            }

            String companyName = usable(exact.name());
            if (companyName == null) {
                OverviewResponse overview = getOverview(ticker);
                inspectPayload(overview.note(), overview.information(), overview.errorMessage());
                companyName = usable(overview.name());
                if (companyName == null) {
                    throw incompleteData();
                }
            }

            QuoteEnvelope envelope = getQuote(ticker);
            inspectPayload(envelope.note(), envelope.information(), envelope.errorMessage());
            GlobalQuote quote = envelope.globalQuote();
            if (quote == null || usable(quote.symbol()) == null) {
                throw quoteUnavailable();
            }
            if (!ticker.equals(normalize(quote.symbol()))) {
                throw ExternalApiErrorMapper.invalidResponse(PROVIDER);
            }

            return new CotacaoData(
                    ticker,
                    companyName,
                    exact.currency(),
                    parsePrice(quote.price()),
                    parseTimestamp(quote.timestamp()),
                    false
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (HttpClientErrorException.TooManyRequests exception) {
            throw ExternalApiErrorMapper.rateLimit(PROVIDER, exception);
        } catch (HttpServerErrorException exception) {
            throw ExternalApiErrorMapper.unavailable(PROVIDER, exception);
        } catch (ResourceAccessException exception) {
            throw ExternalApiErrorMapper.accessFailure(PROVIDER, exception);
        } catch (RestClientException exception) {
            throw ExternalApiErrorMapper.invalidResponse(PROVIDER, exception);
        }
    }

    private SearchResponse getSearch(String ticker) {
        SearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/query")
                        .queryParam("function", "SYMBOL_SEARCH")
                        .queryParam("keywords", ticker)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(SearchResponse.class);
        if (response == null) {
            throw ExternalApiErrorMapper.invalidResponse(PROVIDER);
        }
        return response;
    }

    private OverviewResponse getOverview(String ticker) {
        OverviewResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/query")
                        .queryParam("function", "OVERVIEW")
                        .queryParam("symbol", ticker)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(OverviewResponse.class);
        if (response == null) {
            throw ExternalApiErrorMapper.invalidResponse(PROVIDER);
        }
        return response;
    }

    private QuoteEnvelope getQuote(String ticker) {
        QuoteEnvelope response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/query")
                        .queryParam("function", "GLOBAL_QUOTE")
                        .queryParam("symbol", ticker)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(QuoteEnvelope.class);
        if (response == null) {
            throw ExternalApiErrorMapper.invalidResponse(PROVIDER);
        }
        return response;
    }

    private SearchMatch findExactUsMatch(List<SearchMatch> matches, String ticker) {
        if (matches == null) {
            return null;
        }
        return matches.stream()
                .filter(match -> ticker.equals(normalize(match.symbol())))
                .filter(match -> US_REGIONS.contains(normalize(match.region())))
                .findFirst()
                .orElse(null);
    }

    private void inspectPayload(String note, String information, String errorMessage) {
        if (usable(note) != null || isRateLimitInformation(information)) {
            throw ExternalApiErrorMapper.rateLimit(PROVIDER);
        }
        if (usable(errorMessage) != null) {
            throw tickerNotFound();
        }
        if (usable(information) != null) {
            throw ExternalApiErrorMapper.invalidResponse(PROVIDER);
        }
    }

    private boolean isRateLimitInformation(String information) {
        String normalized = normalize(information);
        return normalized != null && (normalized.contains("RATE LIMIT")
                || normalized.contains("CALL FREQUENCY")
                || normalized.contains("REQUESTS PER")
                || normalized.contains("API CALL"));
    }

    private BigDecimal parsePrice(String price) {
        try {
            return usable(price) == null ? null : new BigDecimal(price.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private OffsetDateTime parseTimestamp(String timestamp) {
        if (usable(timestamp) == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(timestamp.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String usable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private void ensureConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            throw ExternalApiErrorMapper.unavailable(PROVIDER);
        }
    }

    private ApiException tickerNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.TICKER_INEXISTENTE, "Ticker inexistente");
    }

    private ApiException incompleteData() {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCodes.DADOS_EXTERNOS_INCOMPLETOS,
                "Dados obrigatórios ausentes na resposta de " + PROVIDER
        );
    }

    private ApiException quoteUnavailable() {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCodes.COTACAO_INDISPONIVEL,
                "Cotação indisponível"
        );
    }

    private record SearchResponse(
            @JsonProperty("bestMatches") List<SearchMatch> bestMatches,
            @JsonProperty("Note") String note,
            @JsonProperty("Information") String information,
            @JsonProperty("Error Message") String errorMessage
    ) {
    }

    private record SearchMatch(
            @JsonProperty("1. symbol") String symbol,
            @JsonProperty("2. name") String name,
            @JsonProperty("4. region") String region,
            @JsonProperty("8. currency") String currency
    ) {
    }

    private record OverviewResponse(
            @JsonProperty("Name") String name,
            @JsonProperty("Note") String note,
            @JsonProperty("Information") String information,
            @JsonProperty("Error Message") String errorMessage
    ) {
    }

    private record QuoteEnvelope(
            @JsonProperty("Global Quote") GlobalQuote globalQuote,
            @JsonProperty("Note") String note,
            @JsonProperty("Information") String information,
            @JsonProperty("Error Message") String errorMessage
    ) {
    }

    private record GlobalQuote(
            @JsonProperty("01. symbol") String symbol,
            @JsonProperty("05. price") String price,
            @JsonProperty("07. latest trading day") String timestamp
    ) {
    }
}
