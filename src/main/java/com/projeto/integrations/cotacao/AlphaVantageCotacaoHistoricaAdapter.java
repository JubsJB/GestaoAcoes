package com.projeto.integrations.cotacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.projeto.entities.Mercado;
import com.projeto.integrations.ExternalApiErrorMapper;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
@Profile("!test")
public class AlphaVantageCotacaoHistoricaAdapter implements CotacaoHistoricaProvider {
    private static final String PROVIDER = "Alpha Vantage";
    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build());
    public AlphaVantageCotacaoHistoricaAdapter(@Qualifier("alphaVantageRestClient") RestClient restClient,
                                               @Value("${integration.alpha-vantage.api-key:}") String apiKey) {
        this.restClient = restClient; this.apiKey = apiKey;
    }
    public Mercado mercado() { return Mercado.EUA; }
    public CotacaoHistoricaData consultarFechamento(String ticker, LocalDate data) {
        ensureConfigured();
        try {
            String payload = restClient.get().uri(b -> b.path("/query")
                    .queryParam("function", "TIME_SERIES_DAILY").queryParam("symbol", ticker)
                    .queryParam("outputsize", "compact").queryParam("apikey", apiKey).build())
                    .retrieve().body(String.class);
            return parse(objectMapper.readTree(payload), ticker, data);
        } catch (ApiException e) { throw e;
        } catch (HttpClientErrorException.TooManyRequests e) { throw ExternalApiErrorMapper.rateLimit(PROVIDER, e);
        } catch (HttpServerErrorException e) { throw ExternalApiErrorMapper.unavailable(PROVIDER, e);
        } catch (ResourceAccessException e) { throw ExternalApiErrorMapper.accessFailure(PROVIDER, e);
        } catch (RestClientException e) { throw ExternalApiErrorMapper.invalidResponse(PROVIDER, e);
        } catch (Exception e) { throw ExternalApiErrorMapper.invalidResponse(PROVIDER); }
    }
    CotacaoHistoricaData parse(JsonNode root, String ticker, LocalDate requested) {
        if (root == null || !root.isObject()) throw invalid();
        classifyMessage(root.get("Note"), false);
        classifyMessage(root.get("Information"), false);
        classifyMessage(root.get("Error Message"), true);
        JsonNode series = root.get("Time Series (Daily)");
        if (series == null || !series.isObject() || series.isEmpty()) throw invalid();
        Map<LocalDate, BigDecimal> candles = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = series.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            LocalDate date;
            try { date = LocalDate.parse(entry.getKey()); } catch (DateTimeParseException e) { throw invalid(); }
            if (candles.containsKey(date)) throw invalid();
            JsonNode closeNode = entry.getValue() == null ? null : entry.getValue().get("4. close");
            BigDecimal close = decimal(closeNode);
            if (close == null || close.signum() <= 0) throw invalid();
            candles.put(date, close);
        }
        BigDecimal exact = candles.get(requested);
        if (exact != null) return new CotacaoHistoricaData(ticker, requested, exact);
        LocalDate min = Collections.min(candles.keySet());
        LocalDate max = Collections.max(candles.keySet());
        if (!requested.isBefore(min) && !requested.isAfter(max))
            throw historical(HttpStatus.UNPROCESSABLE_CONTENT, ErrorCodes.COTACAO_HISTORICA_INDISPONIVEL,
                    "Cotação histórica indisponível para a data solicitada");
        if (requested.isBefore(min) && candles.size() >= 100)
            throw historical(HttpStatus.UNPROCESSABLE_CONTENT, ErrorCodes.HISTORICO_COTACAO_FORA_DO_ALCANCE,
                    "Data fora do alcance do histórico de cotação disponível");
        throw invalid();
    }
    private void classifyMessage(JsonNode node, boolean errorMessage) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) return;
        String value = node.asText().toUpperCase(Locale.ROOT);
        boolean rate = value.contains("RATE LIMIT") || value.contains("CALL FREQUENCY")
                || value.contains("REQUESTS PER") || value.contains("TOO MANY REQUESTS")
                || value.contains("MAXIMUM") && (value.contains("CALLS") || value.contains("REQUESTS"))
                || value.contains("CALL LIMIT") || value.contains("REQUEST LIMIT");
        if (rate) throw ExternalApiErrorMapper.rateLimit(PROVIDER);
        boolean mentionsSymbol = value.contains("SYMBOL") || value.contains("TICKER");
        boolean invalidSymbol = errorMessage && mentionsSymbol
                && (value.contains("INVALID") || value.contains("NOT FOUND")
                || value.contains("DOES NOT EXIST") || value.contains("UNKNOWN"));
        if (invalidSymbol) throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.TICKER_INEXISTENTE, "Ticker inexistente");
        throw invalid();
    }
    private BigDecimal decimal(JsonNode node) {
        if (node == null || node.isNull() || (!node.isTextual() && !node.isNumber())) return null;
        try { return new BigDecimal(node.asText().trim()); } catch (RuntimeException e) { return null; }
    }
    private ApiException historical(HttpStatus status, String code, String message) {
        return new ApiException(status, code, message);
    }
    private ApiException invalid() { return ExternalApiErrorMapper.invalidResponse(PROVIDER); }
    private void ensureConfigured() { if (apiKey == null || apiKey.isBlank()) throw ExternalApiErrorMapper.unavailable(PROVIDER); }
}
