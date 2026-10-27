package com.projeto.services;

import com.projeto.entities.Mercado;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.validation.TickerNormalizer;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Regras puras compartilhadas; não consulta referências nem calcula posições. */
final class ValidacaoEntradaCustodia {
    private ValidacaoEntradaCustodia() { }

    static String validar(Long carteiraId, String ticker, Mercado mercado, Long destinoId,
            BigDecimal quantidade, LocalDate dataEfetiva, TickerNormalizer tickers, Clock clock) {
        id(carteiraId, "carteiraId");
        id(destinoId, "corretoraDestinoId");
        if (mercado == null) throw invalida("mercado", "Mercado obrigatório");
        if (quantidade == null || quantidade.signum() <= 0 || quantidade.scale() > 6
                || (long) quantidade.precision() + 6L - quantidade.scale() > 19
                || (mercado == Mercado.BRASIL && quantidade.stripTrailingZeros().scale() > 0)) {
            throw invalida("quantidade", "Quantidade deve respeitar o mercado e NUMERIC(19,6), sem arredondamento");
        }
        if (dataEfetiva == null) throw invalida("dataEfetiva", "Data efetiva obrigatória");
        var zona = ZoneId.of(mercado == Mercado.BRASIL ? "America/Sao_Paulo" : "America/New_York");
        if (dataEfetiva.isAfter(LocalDate.now(clock.withZone(zona)))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.DATA_EFETIVA_FUTURA,
                    "Data efetiva futura no mercado", Map.of("dataEfetiva", "Data efetiva não pode ser futura"));
        }
        try {
            return tickers.normalizeAndValidate(ticker);
        } catch (ApiException erro) {
            throw invalida("ticker", "Ticker inválido");
        }
    }

    static void id(Long valor, String campo) {
        if (valor == null || valor <= 0) throw invalida(campo, "Identidade positiva obrigatória");
    }

    static ApiException invalida(String campo, String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_INVALIDO,
                "Dados da custódia inválidos", Map.of(campo, mensagem));
    }
}
