package com.projeto.integrations.cotacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.entities.Mercado;
import com.projeto.integrations.ExternalApiErrorMapper;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.*;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
@Profile("!test")
public class BrapiCotacaoHistoricaAdapter implements CotacaoHistoricaProvider {
    private static final String PROVIDER = "BRAPI";
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/Sao_Paulo");
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
        JsonNode data = result.get("data");
        if (data == null || !data.isObject()) throw invalid();
        JsonNode prices = data.get("historicalDataPrice");
        if (prices == null || !prices.isArray()) throw invalid();
        if (prices.isEmpty()) throw historical(ErrorCodes.COTACAO_HISTORICA_INDISPONIVEL,
                "CotaÃ§Ã£o histÃ³rica indisponÃ­vel para a data solicitada");

        Map<LocalDate, BigDecimal> candles = new HashMap<>();
        for (JsonNode candle : prices) {
            LocalDate date = marketDate(candle == null ? null : candle.get("date"));
            if (candles.containsKey(date)) throw invalid();
            BigDecimal close = decimal(candle.get("close"));
            if (close == null || close.signum() <= 0) throw invalid();
            candles.put(date, close);
        }

        BigDecimal exact = candles.get(expectedDate);
        if (exact != null) return new CotacaoHistoricaData(expectedTicker, expectedDate, exact);
        LocalDate min = Collections.min(candles.keySet());
        LocalDate max = Collections.max(candles.keySet());
        if (!expectedDate.isBefore(min) && !expectedDate.isAfter(max)) {
            throw historical(ErrorCodes.COTACAO_HISTORICA_INDISPONIVEL,
                    "CotaÃ§Ã£o histÃ³rica indisponÃ­vel para a data solicitada");
        }
        throw historical(ErrorCodes.HISTORICO_COTACAO_FORA_DO_ALCANCE,
                "Data fora do alcance do histÃ³rico de cotaÃ§Ã£o disponÃ­vel");
    }
    private LocalDate marketDate(JsonNode node) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToLong()) throw invalid();
        try { return Instant.ofEpochSecond(node.longValue()).atZone(MARKET_ZONE).toLocalDate(); }
        catch (DateTimeException | ArithmeticException e) { throw invalid(); }
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
    private ApiException historical(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }
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
