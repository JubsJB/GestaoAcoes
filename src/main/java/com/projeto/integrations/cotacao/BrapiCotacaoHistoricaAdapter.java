package com.projeto.integrations.cotacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.entities.Mercado;
import com.projeto.integrations.ExternalApiErrorMapper;
import com.projeto.services.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Component
@Profile("!test")
public class BrapiCotacaoHistoricaAdapter implements CotacaoHistoricaProvider {
    private static final String PROVIDER = "BRAPI";
    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public BrapiCotacaoHistoricaAdapter(@Qualifier("brapiRestClient") RestClient restClient,
                                        @Value("${integration.brapi.api-key:}") String apiKey) {
        this.restClient = restClient; this.apiKey = apiKey;
    }
    public Mercado mercado() { return Mercado.BRASIL; }
    public CotacaoHistoricaData consultarFechamento(String ticker, LocalDate data) {
        ensureConfigured();
        try {
            String payload = restClient.get().uri(b -> b.path("/api/v2/stocks/historical")
                    .queryParam("symbols", ticker).queryParam("startDate", data)
                    .queryParam("endDate", data).queryParam("interval", "1d").build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).retrieve().body(String.class);
            return parse(objectMapper.readTree(payload), ticker, data);
        } catch (ApiException e) { throw e;
        } catch (HttpClientErrorException.NotFound e) { throw tickerNotFound();
        } catch (HttpClientErrorException.TooManyRequests e) { throw ExternalApiErrorMapper.rateLimit(PROVIDER, e);
        } catch (HttpServerErrorException e) { throw ExternalApiErrorMapper.unavailable(PROVIDER, e);
        } catch (ResourceAccessException e) { throw ExternalApiErrorMapper.accessFailure(PROVIDER, e);
        } catch (RestClientException e) { throw ExternalApiErrorMapper.invalidResponse(PROVIDER, e);
        } catch (Exception e) { throw ExternalApiErrorMapper.invalidResponse(PROVIDER); }
    }
    CotacaoHistoricaData parse(JsonNode root, String expectedTicker, LocalDate expectedDate) {
        if (root == null || !root.isObject()) throw invalid();
        classifyProviderMessage(root.get("message"));
        JsonNode results = root.get("results");
        if (results == null || !results.isArray() || results.size() != 1) throw invalid();
        JsonNode result = results.get(0);
        String symbol = text(result, "symbol");
        if (symbol == null || !expectedTicker.equals(symbol.trim().toUpperCase(Locale.ROOT))) throw invalid();
        JsonNode prices = result.get("historicalDataPrice");
        if (prices == null || !prices.isArray() || prices.size() != 1) throw invalid();
        JsonNode candle = prices.get(0);
        String rawDate = text(candle, "date");
        if (rawDate == null) throw invalid();
        LocalDate date;
        try { date = LocalDate.parse(rawDate); } catch (DateTimeParseException e) { throw invalid(); }
        if (!expectedDate.equals(date)) throw invalid();
        BigDecimal close = decimal(candle.get("close"));
        if (close == null || close.signum() <= 0) throw invalid();
        return new CotacaoHistoricaData(expectedTicker, date, close);
    }
    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }
    private BigDecimal decimal(JsonNode node) {
        if (node == null || node.isNull() || (!node.isNumber() && !node.isTextual())) return null;
        try { return new BigDecimal(node.asText().trim()); } catch (RuntimeException e) { return null; }
    }
    private ApiException invalid() { return ExternalApiErrorMapper.invalidResponse(PROVIDER); }
    private ApiException tickerNotFound() {
        return new ApiException(org.springframework.http.HttpStatus.NOT_FOUND,
                com.projeto.services.exceptions.ErrorCodes.TICKER_INEXISTENTE, "Ticker inexistente");
    }
    private void classifyProviderMessage(JsonNode node) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) return;
        String message = node.asText().trim().toUpperCase(Locale.ROOT);
        boolean rateLimit = message.contains("RATE LIMIT")
                || message.contains("TOO MANY REQUESTS")
                || message.contains("REQUESTS PER")
                || message.contains("CALL FREQUENCY")
                || message.contains("LIMITE DE REQUISIÇÕES")
                || message.contains("LIMITE DE REQUISICOES");
        if (rateLimit) throw ExternalApiErrorMapper.rateLimit(PROVIDER);
        boolean mentionsTicker = message.contains("TICKER") || message.contains("SYMBOL")
                || message.contains("SÍMBOLO") || message.contains("SIMBOLO");
        boolean explicitlyMissing = message.contains("NOT FOUND") || message.contains("DOES NOT EXIST")
                || message.contains("INEXISTENTE") || message.contains("INVALID TICKER")
                || message.contains("INVALID SYMBOL") || message.contains("TICKER INVÁLIDO")
                || message.contains("TICKER INVALIDO") || message.contains("SÍMBOLO INVÁLIDO")
                || message.contains("SIMBOLO INVALIDO");
        if (mentionsTicker && explicitlyMissing) throw tickerNotFound();
        throw invalid();
    }
    private void ensureConfigured() { if (apiKey == null || apiKey.isBlank()) throw ExternalApiErrorMapper.unavailable(PROVIDER); }
}
