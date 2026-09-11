package com.projeto.services;

import com.projeto.entities.Mercado;
import com.projeto.integrations.cotacao.CotacaoHistoricaData;
import com.projeto.integrations.cotacao.CotacaoHistoricaProvider;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FechamentoHistoricoServiceTest {

    private final CotacaoHistoricaProvider brasil = mock(CotacaoHistoricaProvider.class);
    private final CotacaoHistoricaProvider eua = mock(CotacaoHistoricaProvider.class);
    private FechamentoHistoricoService service;

    @BeforeEach
    void setUp() {
        when(brasil.mercado()).thenReturn(Mercado.BRASIL);
        when(eua.mercado()).thenReturn(Mercado.EUA);
        service = new FechamentoHistoricoService(List.of(brasil, eua));
    }

    @Test
    void selectsOnlyBrazilProviderAndPreservesRawClose() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        when(brasil.consultarFechamento("PETR4", date))
                .thenReturn(new CotacaoHistoricaData("PETR4", date, new BigDecimal("42.123456")));

        assertEquals(new BigDecimal("42.123456"), service.consultar("PETR4", Mercado.BRASIL, date));
        verify(eua, never()).consultarFechamento("PETR4", date);
    }

    @Test
    void selectsOnlyUsProvider() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        when(eua.consultarFechamento("AAPL", date))
                .thenReturn(new CotacaoHistoricaData("AAPL", date, new BigDecimal("230.25")));

        assertEquals(new BigDecimal("230.250000"), service.consultar("AAPL", Mercado.EUA, date));
        verify(brasil, never()).consultarFechamento("AAPL", date);
    }

    @Test
    void rejectsMismatchedTickerDateAndInvalidDecimalAsExternalResponse() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        for (CotacaoHistoricaData invalid : List.of(
                new CotacaoHistoricaData("VALE3", date, BigDecimal.TEN),
                new CotacaoHistoricaData("PETR4", date.minusDays(1), BigDecimal.TEN),
                new CotacaoHistoricaData("PETR4", date, BigDecimal.ZERO),
                new CotacaoHistoricaData("PETR4", date, new BigDecimal("1.1234567"))
        )) {
            when(brasil.consultarFechamento("PETR4", date)).thenReturn(invalid);
            ApiException error = assertThrows(ApiException.class,
                    () -> service.consultar("PETR4", Mercado.BRASIL, date));
            assertEquals(ErrorCodes.RESPOSTA_EXTERNA_INVALIDA, error.getCode());
        }
    }

    @Test
    void propagatesProviderClassificationWithoutTranslation() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        for (String code : List.of(
                ErrorCodes.COTACAO_HISTORICA_INDISPONIVEL,
                ErrorCodes.HISTORICO_COTACAO_FORA_DO_ALCANCE,
                ErrorCodes.TICKER_INEXISTENTE,
                ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO,
                ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,
                ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,
                ErrorCodes.SERVICO_EXTERNO_TIMEOUT
        )) {
            ApiException expected = new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, code, code);
            doThrow(expected).when(brasil).consultarFechamento("PETR4", date);
            assertEquals(expected, assertThrows(ApiException.class,
                    () -> service.consultar("PETR4", Mercado.BRASIL, date)));
        }
    }
}
