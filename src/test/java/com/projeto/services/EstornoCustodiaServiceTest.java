package com.projeto.services;

import com.projeto.repositories.*;
import com.projeto.services.exceptions.ApiException;
import com.projeto.validation.TickerNormalizer;
import java.time.Clock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstornoCustodiaServiceTest {
    final CarteiraRepository carteiras = mock(CarteiraRepository.class);
    final AcaoRepository acoes = mock(AcaoRepository.class);
    final CorretoraRepository corretoras = mock(CorretoraRepository.class);
    final OperacaoRepository operacoes = mock(OperacaoRepository.class);
    final MovimentacaoCustodiaRepository movimentos = mock(MovimentacaoCustodiaRepository.class);
    final EstornoMovimentacaoCustodiaRepository estornos = mock(EstornoMovimentacaoCustodiaRepository.class);

    @ParameterizedTest @ValueSource(strings={"ausente", "carteiraNula", "carteiraZero", "originalNula", "originalNegativa", "motivo", "brancoLongo"})
    void entradaInvalidaNaoConsultaBanco(String caso) {
        var writer = new TransferenciaCustodiaPersistenceService(carteiras, acoes, corretoras,
                operacoes, movimentos, estornos, Clock.systemUTC(), new TickerNormalizer());
        var c = caso.equals("ausente") ? null : new EstornoCustodiaCommand(
                caso.equals("carteiraNula") ? null : caso.equals("carteiraZero") ? 0L : 1L,
                caso.equals("originalNula") ? null : caso.equals("originalNegativa") ? -1L : 2L,
                caso.equals("motivo") ? "x".repeat(1001) : caso.equals("brancoLongo") ? " ".repeat(1001) : null);
        var erro = assertThrows(ApiException.class, () -> new EstornoCustodiaService(writer).estornar(c));
        assertEquals("REQUEST_INVALIDO", erro.getCode());
        verifyNoInteractions(carteiras, acoes, corretoras, operacoes, movimentos, estornos);
    }
}
