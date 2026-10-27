package com.projeto.services;

import com.projeto.entities.Mercado;
import com.projeto.entities.TipoOperacao;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.validation.TickerNormalizer;
import java.math.*;
import java.time.*;
import java.util.Map;
import org.springframework.http.HttpStatus;

public record OperacaoPersistenceCommand(Long carteiraId, String ticker, Mercado mercado, Long corretoraId,
        TipoOperacao tipo, BigDecimal quantidade, BigDecimal precoUnitario, LocalDate dataOperacao) {
    OperacaoPersistenceCommand validada(Clock clock, TickerNormalizer tickers) {
        if (carteiraId == null || carteiraId <= 0) throw invalida("carteiraId", "Carteira deve possuir ID positivo");
        if (corretoraId != null && corretoraId <= 0) throw invalida("corretoraId", "Corretora deve possuir ID positivo");
        if (mercado == null) throw invalida("mercado", "Mercado é obrigatório");
        if (tipo == null) throw invalida("tipo", "Tipo é obrigatório");
        String normalizado = tickers.normalizeAndValidate(ticker);
        BigDecimal q = operando(quantidade, "quantidade", "Quantidade");
        if (mercado == Mercado.BRASIL && q.stripTrailingZeros().scale() > 0)
            throw invalida("quantidade", "Quantidade deve ser matematicamente inteira para o mercado BRASIL");
        if (dataOperacao == null) throw invalida("dataOperacao", "Data da operação é obrigatória");
        var zona = ZoneId.of(mercado == Mercado.BRASIL ? "America/Sao_Paulo" : "America/New_York");
        if (dataOperacao.isAfter(LocalDate.now(clock.withZone(zona))))
            throw invalida("dataOperacao", "Data da operação não pode ser futura");
        BigDecimal preco = operando(precoUnitario, "precoUnitario", "Preço unitário");
        return new OperacaoPersistenceCommand(carteiraId, normalizado, mercado, corretoraId, tipo, q, preco, dataOperacao);
    }
    private static BigDecimal operando(BigDecimal valor, String campo, String nome) {
        if (valor == null) throw invalida(campo, nome + " é obrigatório");
        if (valor.signum() <= 0) throw invalida(campo, nome + " deve ser maior que zero");
        if (valor.scale() > 6) throw invalida(campo, nome + " deve possuir no máximo 6 casas decimais");
        BigDecimal normalizado = valor.setScale(6, RoundingMode.UNNECESSARY);
        if (normalizado.precision() > 19) throw invalida(campo, nome + " excede a precisão máxima 19");
        return normalizado;
    }
    static ApiException invalida(String campo, String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.REQUEST_INVALIDO, "Dados da requisição inválidos", Map.of(campo, mensagem));
    }
}
