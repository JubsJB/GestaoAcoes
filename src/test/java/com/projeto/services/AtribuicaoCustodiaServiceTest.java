package com.projeto.services;

import com.projeto.repositories.*;
import com.projeto.services.exceptions.ApiException;
import com.projeto.validation.TickerNormalizer;
import java.time.*;
import java.math.BigDecimal;
import com.projeto.entities.Mercado;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtribuicaoCustodiaServiceTest {
    final CarteiraRepository carteiras = mock(CarteiraRepository.class);
    final AcaoRepository acoes = mock(AcaoRepository.class);
    final CorretoraRepository corretoras = mock(CorretoraRepository.class);
    final OperacaoRepository operacoes = mock(OperacaoRepository.class);
    final MovimentacaoCustodiaRepository movimentos = mock(MovimentacaoCustodiaRepository.class);
    final EstornoMovimentacaoCustodiaRepository estornos = mock(EstornoMovimentacaoCustodiaRepository.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);

    AtribuicaoCustodiaService service(Clock agora) {
        var writer = new TransferenciaCustodiaPersistenceService(carteiras, acoes, corretoras,
                operacoes, movimentos, estornos, agora, new TickerNormalizer());
        return new AtribuicaoCustodiaService(writer, new TickerNormalizer(), agora);
    }

    @Test void entradaAusenteEhRecusadaAntesDoBanco() {
        var erro = assertThrows(ApiException.class, () -> service(clock).atribuir(null));
        assertEquals("REQUEST_INVALIDO", erro.getCode());
        verifyNoInteractions(carteiras, acoes, corretoras, operacoes, movimentos, estornos);
    }
    @ParameterizedTest
    @ValueSource(strings={"carteira", "ticker", "mercado", "destino", "quantidade", "zero", "negativa", "casas", "precisao", "fracaoBR", "data"})
    void estruturaInvalidaNaoConsultaBanco(String caso) {
        var c = new AtribuicaoCustodiaCommand(caso.equals("carteira") ? 0L : 1L,
                caso.equals("ticker") ? " " : "PETR4", caso.equals("mercado") ? null : Mercado.BRASIL,
                caso.equals("destino") ? null : 20L,
                switch (caso) {
                    case "quantidade" -> null;
                    case "zero" -> BigDecimal.ZERO;
                    case "negativa" -> BigDecimal.ONE.negate();
                    case "casas" -> new BigDecimal("1.0000001");
                    case "precisao" -> new BigDecimal("10000000000000");
                    case "fracaoBR" -> new BigDecimal("0.1");
                    default -> new BigDecimal("40");
                }, caso.equals("data") ? null : LocalDate.of(2026, 10, 1));
        var erro = assertThrows(ApiException.class, () -> service(clock).atribuir(c));
        assertEquals("REQUEST_INVALIDO", erro.getCode());
        verifyNoInteractions(carteiras, acoes, corretoras, operacoes, movimentos, estornos);
    }
    @ParameterizedTest
    @CsvSource({"BRASIL,2026-10-24T02:30:00Z", "EUA,2026-10-24T03:30:00Z"})
    void dataFuturaNaZonaDoMercadoNaoConsultaBanco(Mercado mercado, String instante) {
        var c = new AtribuicaoCustodiaCommand(1L, "PETR4", mercado, 20L, BigDecimal.ONE, LocalDate.of(2026,10,24));
        var erro = assertThrows(ApiException.class, () -> service(Clock.fixed(Instant.parse(instante), ZoneOffset.UTC)).atribuir(c));
        assertEquals("DATA_EFETIVA_FUTURA", erro.getCode());
        verifyNoInteractions(carteiras, acoes, corretoras, operacoes, movimentos, estornos);
    }

    @Test void seteCasasNosEuaNaoSaoArredondadas() {
        var c = new AtribuicaoCustodiaCommand(1L, "AAPL", Mercado.EUA, 20L, new BigDecimal("0.0000001"), LocalDate.of(2026,10,1));
        var erro = assertThrows(ApiException.class, () -> service(clock).atribuir(c));
        assertEquals("REQUEST_INVALIDO", erro.getCode());
        assertTrue(erro.getDetails().containsKey("quantidade"));
        verifyNoInteractions(carteiras, acoes, corretoras, operacoes, movimentos, estornos);
    }
}
