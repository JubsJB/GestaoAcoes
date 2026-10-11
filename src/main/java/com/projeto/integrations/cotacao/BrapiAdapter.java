package com.projeto.integrations.cotacao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projeto.entities.Mercado;
import com.projeto.integrations.ExternalApiErrorMapper;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Component
public class BrapiAdapter implements CotacaoProvider {

    private static final String PROVIDER = "BRAPI";

    private final RestClient restClient;
    private final String apiKey;

    public BrapiAdapter(
            @Qualifier("brapiRestClient") RestClient restClient,
            @Value("${integration.brapi.api-key:}") String apiKey
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    public Mercado mercado() {
        return Mercado.BRASIL;
    }

    @Override
    public CotacaoData consultar(String ticker) {
        ensureConfigured();
        try {
            BrapiResponse response = restClient.get()
                    .uri("/api/quote/{ticker}", ticker)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve()
                    .body(BrapiResponse.class);

            if (response != null && isRateLimitMessage(response.message())) {
                throw ExternalApiErrorMapper.rateLimit(PROVIDER);
            }
            if (response != null && Boolean.TRUE.equals(response.error())) {
                throw ExternalApiErrorMapper.invalidResponse(PROVIDER);
            }
            if (response == null || response.results() == null || response.results().isEmpty()) {
                throw tickerNotFound();
            }

            BrapiResult result = response.results().get(0);
            if (result == null) throw ExternalApiErrorMapper.invalidResponse(PROVIDER);
            OffsetDateTime referencia = parseTimestamp(result.regularMarketTime());
            return new CotacaoData(
                    result.symbol(),
                    firstUsable(result.longName(), result.shortName()),
                    result.currency(),
                    result.regularMarketPrice(),
                    referencia,
                    Boolean.TRUE.equals(result.changed()),
                    null,
                    referenciaInvalida(result.regularMarketTime(), referencia)
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (HttpClientErrorException.NotFound exception) {
            throw tickerNotFound();
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

    private void ensureConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            throw ExternalApiErrorMapper.unavailable(PROVIDER);
        }
    }

    private String firstUsable(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private OffsetDateTime parseTimestamp(Object rawTimestamp) {
        if (rawTimestamp instanceof Number number) {
            try {
                return Instant.ofEpochSecond(number.longValue()).atOffset(ZoneOffset.UTC);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        if (!(rawTimestamp instanceof String value) || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return Instant.parse(value).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private boolean referenciaInvalida(Object raw, OffsetDateTime referencia) {
        if (raw == null) return false;
        if (referencia == null) return true;
        if (raw instanceof Number number) {
            try {
                // A candidata não aceita truncamento de segundos ou overflow do campo externo.
                new BigDecimal(number.toString()).longValueExact();
            } catch (RuntimeException exception) {
                return true;
            }
        }
        return false;
    }

    /** Preserva o decimal do JSON antes da validação monetária, sem ponto flutuante. */
    public static final class PrecoDecimalDeserializer extends ValueDeserializer<BigDecimal> {
        @Override
        public BigDecimal deserialize(JsonParser parser, DeserializationContext context) {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                return parser.isNaN() ? null : parser.getDecimalValue();
            }
            if (token == JsonToken.VALUE_STRING) {
                try {
                    return new BigDecimal(parser.getString().trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            parser.skipChildren();
            return null;
        }
    }

    private ApiException tickerNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.TICKER_INEXISTENTE, "Ticker inexistente");
    }

    private boolean isRateLimitMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toUpperCase(Locale.ROOT);
        return normalized.contains("RATE LIMIT")
                || normalized.contains("LIMITE")
                || normalized.contains("REQUESTS PER")
                || normalized.contains("TOO MANY REQUESTS");
    }

    private record BrapiResponse(List<BrapiResult> results, Boolean error, String message) {
    }

    private record BrapiResult(
            String symbol,
            String longName,
            String shortName,
            String currency,
            @JsonDeserialize(using = PrecoDecimalDeserializer.class) BigDecimal regularMarketPrice,
            Object regularMarketTime,
            @JsonProperty("changed") Boolean changed
    ) {
    }
}
