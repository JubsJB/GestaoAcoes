package com.projeto.services;

import com.projeto.dto.PatrimonioResponse;
import com.projeto.dto.PosicaoResponse;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.mappers.PatrimonioMapper;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatrimonioServiceTest {

    @Mock
    private PosicaoService posicaoService;

    private PatrimonioService service;

    @BeforeEach
    void setUp() {
        service = new PatrimonioService(
                posicaoService,
                new AgregadorPosicoesPorMoeda(),
                new PatrimonioMapper()
        );
    }

    @Test
    void returnsPortfolioIdAndEmptyCurrenciesForEmptyOpenPositions() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of());

        PatrimonioResponse response = service.consultar(1L);

        assertEquals(1L, response.carteiraId());
        assertTrue(response.patrimonios().isEmpty());
        verify(posicaoService, times(1)).listarPorCarteira(1L);
        verifyNoMoreInteractions(posicaoService);
    }

    @Test
    void propagatesMissingPortfolioFromOfficialPositionFlow() {
        when(posicaoService.listarPorCarteira(404L))
                .thenThrow(new ObjectNotFoundException("Carteira não encontrada para o id: 404"));

        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> service.consultar(404L)
        );

        assertEquals("Carteira não encontrada para o id: 404", exception.getMessage());
        verify(posicaoService, times(1)).listarPorCarteira(404L);
    }

    @Test
    void sumsExactCurrentValuesByCurrencyAndSortsByCurrencyName() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                position(3L, Mercado.EUA, Moeda.USD, "112.205000000000", "999999"),
                position(2L, Mercado.BRASIL, Moeda.BRL, "700.000000000000", "-999999"),
                position(1L, Mercado.BRASIL, Moeda.BRL, "9912.345600000000", "999999")
        ));

        PatrimonioResponse response = service.consultar(1L);

        assertEquals(2, response.patrimonios().size());
        assertEquals(Moeda.BRL, response.patrimonios().get(0).moeda());
        assertEquals(new BigDecimal("10612.345600000000"),
                response.patrimonios().get(0).patrimonioAtual());
        assertEquals(Moeda.USD, response.patrimonios().get(1).moeda());
        assertEquals(new BigDecimal("112.205000000000"),
                response.patrimonios().get(1).patrimonioAtual());
        verify(posicaoService, times(1)).listarPorCarteira(1L);
        verifyNoMoreInteractions(posicaoService);
    }

    @Test
    void usesOnlyCurrentValueAndDoesNotAddCostResultsOrProfitability() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                position(1L, Mercado.BRASIL, Moeda.BRL, "3550.000000000000", "350")
        ));

        PatrimonioResponse response = service.consultar(1L);

        assertEquals(new BigDecimal("3550.000000000000"),
                response.patrimonios().get(0).patrimonioAtual());
    }

    @Test
    void normalizesOnlyFinalExactSumToScaleTwelve() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                position(1L, Mercado.BRASIL, Moeda.BRL, "0.100000000000", "0"),
                position(2L, Mercado.BRASIL, Moeda.BRL, "0.200000000000", "0")
        ));

        BigDecimal total = service.consultar(1L).patrimonios().get(0).patrimonioAtual();

        assertEquals(new BigDecimal("0.300000000000"), total);
        assertEquals(12, total.scale());
    }

    @Test
    void rejectsPrecisionOverflowWithoutReturningPartialCurrencies() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                position(1L, Mercado.BRASIL, Moeda.BRL,
                        "99999999999999999999999999.999999999999", "0"),
                position(2L, Mercado.BRASIL, Moeda.BRL,
                        "0.000000000001", "0"),
                position(3L, Mercado.EUA, Moeda.USD, "1.000000000000", "0")
        ));

        ApiException exception = assertThrows(ApiException.class, () -> service.consultar(1L));

        assertEquals(422, exception.getStatus().value());
        assertEquals(ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO, exception.getCode());
        assertEquals(1L, exception.getDetails().get("carteiraId"));
        assertEquals(Moeda.BRL, exception.getDetails().get("moeda"));
    }

    @Test
    void rejectsScaleLossWithUnnecessaryRounding() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                position(1L, Mercado.BRASIL, Moeda.BRL, "1.0000000000001", "0")
        ));

        ApiException exception = assertThrows(ApiException.class, () -> service.consultar(1L));

        assertEquals(ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO, exception.getCode());
        assertEquals(Moeda.BRL, exception.getDetails().get("moeda"));
    }

    @Test
    void keepsApprovedTransactionAndHasNoInfrastructureDependencies() throws Exception {
        Transactional transactional = PatrimonioService.class
                .getMethod("consultar", Long.class)
                .getAnnotation(Transactional.class);

        assertTrue(transactional.readOnly());
        assertEquals(Isolation.REPEATABLE_READ, transactional.isolation());
        assertEquals(
                List.of("posicaoService", "agregador", "mapper"),
                Arrays.stream(PatrimonioService.class.getDeclaredFields())
                        .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                        .map(field -> field.getName())
                        .toList()
        );
    }

    private PosicaoResponse position(
            Long actionId,
            Mercado market,
            Moeda currency,
            String currentValue,
            String unrealized
    ) {
        return new PosicaoResponse(
                actionId,
                actionId + "TICKER",
                "Empresa " + actionId,
                market,
                currency,
                new BigDecimal("1.000000"),
                new BigDecimal("10.000000000000"),
                new BigDecimal("10.000000000000"),
                new BigDecimal("20.000000"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z"),
                new BigDecimal(currentValue),
                new BigDecimal(unrealized).setScale(12),
                new BigDecimal("1.000000")
        );
    }
}
