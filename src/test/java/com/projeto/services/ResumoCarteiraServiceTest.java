package com.projeto.services;

import com.projeto.dto.PosicaoResponse;
import com.projeto.dto.ResumoCarteiraResponse;
import com.projeto.dto.PatrimonioResponse;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.mappers.ResumoCarteiraMapper;
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
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ResumoCarteiraServiceTest {

    @Mock
    private PosicaoService posicaoService;

    private ResumoCarteiraService service;

    @BeforeEach
    void setUp() {
        service = new ResumoCarteiraService(
                posicaoService,
                new AgregadorPosicoesPorMoeda(),
                new CalculadoraRentabilidade(),
                new ResumoCarteiraMapper()
        );
    }

    @Test
    void returnsEmptySummaryForExistingPortfolioWithoutOpenPositions() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of());

        ResumoCarteiraResponse response = service.consultar(1L);

        assertEquals(1L, response.carteiraId());
        assertTrue(response.resumos().isEmpty());
        verify(posicaoService, times(1)).listarPorCarteira(1L);
        verifyNoMoreInteractions(posicaoService);
    }

    @Test
    void propagatesMissingPortfolioFromPositionService() {
        when(posicaoService.listarPorCarteira(404L))
                .thenThrow(new ObjectNotFoundException("Carteira não encontrada para o id: 404"));

        assertThrows(ObjectNotFoundException.class, () -> service.consultar(404L));
        verify(posicaoService, times(1)).listarPorCarteira(404L);
    }

    @Test
    void summarizesBrlAndUsdWithoutMixingCurrencies() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                posicao(1L, Mercado.EUA, Moeda.USD, "100", "112.205", "12.205", "999"),
                posicao(2L, Mercado.BRASIL, Moeda.BRL, "3200", "3550", "350", "999"),
                posicao(3L, Mercado.BRASIL, Moeda.BRL, "600", "700", "100", "-999")
        ));

        ResumoCarteiraResponse response = service.consultar(1L);

        assertEquals(2, response.resumos().size());
        assertEquals(Moeda.BRL, response.resumos().get(0).moeda());
        assertEquals(new BigDecimal("3800.000000000000"),
                response.resumos().get(0).custoTotalPosicoes());
        assertEquals(new BigDecimal("4250.000000000000"),
                response.resumos().get(0).patrimonioAtual());
        assertEquals(new BigDecimal("450.000000000000"),
                response.resumos().get(0).resultadoNaoRealizadoTotal());
        assertEquals(new BigDecimal("11.842105"),
                response.resumos().get(0).rentabilidadePercentual());
        assertEquals(Moeda.USD, response.resumos().get(1).moeda());
        verify(posicaoService, times(1)).listarPorCarteira(1L);
        verifyNoMoreInteractions(posicaoService);
    }

    @Test
    void returnsOnlyTheCurrencyPresentInTheOpenPositions() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                posicao(1L, Mercado.BRASIL, Moeda.BRL, "10", "12", "2", "20")
        ));
        when(posicaoService.listarPorCarteira(2L)).thenReturn(List.of(
                posicao(2L, Mercado.EUA, Moeda.USD, "10", "11", "1", "10")
        ));

        ResumoCarteiraResponse somenteBrl = service.consultar(1L);
        ResumoCarteiraResponse somenteUsd = service.consultar(2L);

        assertEquals(List.of(Moeda.BRL),
                somenteBrl.resumos().stream().map(item -> item.moeda()).toList());
        assertEquals(List.of(Moeda.USD),
                somenteUsd.resumos().stream().map(item -> item.moeda()).toList());
        verify(posicaoService, times(1)).listarPorCarteira(1L);
        verify(posicaoService, times(1)).listarPorCarteira(2L);
        verifyNoMoreInteractions(posicaoService);
    }

    @Test
    void calculatesFromCurrencyTotalsInsteadOfAveragingPositionPercentages() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                posicao(1L, Mercado.BRASIL, Moeda.BRL, "1000", "1100", "100", "10"),
                posicao(2L, Mercado.BRASIL, Moeda.BRL, "3000", "3300", "300", "90")
        ));

        ResumoCarteiraResponse response = service.consultar(1L);

        assertEquals(new BigDecimal("4000.000000000000"),
                response.resumos().get(0).custoTotalPosicoes());
        assertEquals(new BigDecimal("400.000000000000"),
                response.resumos().get(0).resultadoNaoRealizadoTotal());
        assertEquals(new BigDecimal("10.000000"),
                response.resumos().get(0).rentabilidadePercentual());
    }

    @Test
    void supportsNegativeZeroAndAboveOneHundredWithoutArtificialLimits() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                posicao(1L, Mercado.BRASIL, Moeda.BRL, "100", "250", "150", "0"),
                posicao(2L, Mercado.EUA, Moeda.USD, "100", "85", "-15", "0")
        ));

        ResumoCarteiraResponse response = service.consultar(1L);

        assertEquals(new BigDecimal("150.000000"),
                response.resumos().get(0).rentabilidadePercentual());
        assertEquals(new BigDecimal("-15.000000"),
                response.resumos().get(1).rentabilidadePercentual());

        when(posicaoService.listarPorCarteira(2L)).thenReturn(List.of(
                posicao(3L, Mercado.BRASIL, Moeda.BRL, "100", "100", "0", "999")
        ));
        assertEquals(new BigDecimal("0.000000"),
                service.consultar(2L).resumos().get(0).rentabilidadePercentual());
    }

    @Test
    void rejectsNonPositiveAggregateCostAsInconsistentBeforeCalculatingPercentage() {
        AgregadorPosicoesPorMoeda agregador = mock(AgregadorPosicoesPorMoeda.class);
        CalculadoraRentabilidade calculadora = mock(CalculadoraRentabilidade.class);
        ResumoCarteiraService isolatedService = new ResumoCarteiraService(
                posicaoService, agregador, calculadora, new ResumoCarteiraMapper());
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                posicao(1L, Mercado.BRASIL, Moeda.BRL, "1", "1", "0", "0")));
        when(agregador.agregar(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                new AgregadorPosicoesPorMoeda.TotaisPorMoeda(
                        Moeda.BRL, BigDecimal.ZERO.setScale(12), BigDecimal.ONE.setScale(12),
                        BigDecimal.ONE.setScale(12))));

        ApiException zero = assertThrows(ApiException.class, () -> isolatedService.consultar(1L));
        assertEquals(409, zero.getStatus().value());
        assertEquals(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE, zero.getCode());
        verifyNoMoreInteractions(calculadora);

        when(agregador.agregar(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                new AgregadorPosicoesPorMoeda.TotaisPorMoeda(
                        Moeda.BRL, BigDecimal.ONE.negate().setScale(12), BigDecimal.ZERO.setScale(12),
                        BigDecimal.ONE.setScale(12))));
        ApiException negative = assertThrows(
                ApiException.class, () -> isolatedService.consultar(1L));
        assertEquals(409, negative.getStatus().value());
        assertEquals(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE, negative.getCode());
        verifyNoMoreInteractions(calculadora);
    }

    @Test
    void translatesPercentagePrecisionFailureToUnprocessableWithoutPartialSummary() {
        AgregadorPosicoesPorMoeda agregador = mock(AgregadorPosicoesPorMoeda.class);
        ResumoCarteiraService isolatedService = new ResumoCarteiraService(
                posicaoService, agregador, new CalculadoraRentabilidade(),
                new ResumoCarteiraMapper());
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                posicao(1L, Mercado.BRASIL, Moeda.BRL, "1", "1", "0", "0")));
        when(agregador.agregar(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                new AgregadorPosicoesPorMoeda.TotaisPorMoeda(
                        Moeda.BRL,
                        new BigDecimal("0.000000000001"),
                        new BigDecimal("99999999999999999999999999.000000000000"),
                        new BigDecimal("99999999999999999999999999.000000000000"))));

        ApiException exception = assertThrows(ApiException.class,
                () -> isolatedService.consultar(1L));

        assertEquals(422, exception.getStatus().value());
        assertEquals(ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO, exception.getCode());
        assertEquals(Moeda.BRL, exception.getDetails().get("moeda"));
        assertEquals("rentabilidadePercentual", exception.getDetails().get("indicador"));
    }

    @Test
    void matchesPatrimonioServiceExactlyForTheSameConsolidatedPositions() {
        List<PosicaoResponse> posicoes = List.of(
                posicao(1L, Mercado.EUA, Moeda.USD, "100", "112.205", "12.205", "12.205"),
                posicao(2L, Mercado.BRASIL, Moeda.BRL, "3200", "3550", "350", "10.9375")
        );
        when(posicaoService.listarPorCarteira(1L)).thenReturn(posicoes);
        AgregadorPosicoesPorMoeda agregador = new AgregadorPosicoesPorMoeda();
        ResumoCarteiraService resumoService = new ResumoCarteiraService(
                posicaoService, agregador, new CalculadoraRentabilidade(),
                new ResumoCarteiraMapper());
        PatrimonioService patrimonioService = new PatrimonioService(
                posicaoService, agregador, new PatrimonioMapper());

        ResumoCarteiraResponse resumo = resumoService.consultar(1L);
        PatrimonioResponse patrimonio = patrimonioService.consultar(1L);

        assertEquals(
                patrimonio.patrimonios().stream()
                        .map(item -> item.moeda() + ":" + item.patrimonioAtual().toPlainString())
                        .toList(),
                resumo.resumos().stream()
                        .map(item -> item.moeda() + ":" + item.patrimonioAtual().toPlainString())
                        .toList()
        );
        verify(posicaoService, times(2)).listarPorCarteira(1L);
    }

    @Test
    void translatesOverflowWithoutPartialResponse() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of(
                posicao(1L, Mercado.BRASIL, Moeda.BRL, "1", "1", "1.0000000000001", "0"),
                posicao(2L, Mercado.EUA, Moeda.USD, "1", "1", "0", "0")
        ));

        ApiException exception = assertThrows(ApiException.class, () -> service.consultar(1L));

        assertEquals(422, exception.getStatus().value());
        assertEquals(ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO, exception.getCode());
        assertEquals(Moeda.BRL, exception.getDetails().get("moeda"));
        assertEquals(AgregadorPosicoesPorMoeda.Indicador.RESULTADO_NAO_REALIZADO_TOTAL,
                exception.getDetails().get("indicador"));
    }

    @Test
    void propagatesInconsistentHistoryWithoutReturningPartialSummary() {
        ApiException inconsistency = new ApiException(
                org.springframework.http.HttpStatus.CONFLICT,
                ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE,
                "Histórico de Operações inconsistente",
                new LinkedHashMap<>()
        );
        when(posicaoService.listarPorCarteira(1L)).thenThrow(inconsistency);

        ApiException thrown = assertThrows(ApiException.class, () -> service.consultar(1L));

        assertEquals(inconsistency, thrown);
        assertEquals(409, thrown.getStatus().value());
        assertEquals(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE, thrown.getCode());
    }

    @Test
    void keepsApprovedTransactionAndDoesNotDependOnPatrimonioService() throws Exception {
        Transactional transactional = ResumoCarteiraService.class
                .getMethod("consultar", Long.class)
                .getAnnotation(Transactional.class);

        assertTrue(transactional.readOnly());
        assertEquals(Isolation.REPEATABLE_READ, transactional.isolation());
        assertEquals(
                List.of("posicaoService", "agregador", "calculadoraRentabilidade", "mapper"),
                Arrays.stream(ResumoCarteiraService.class.getDeclaredFields())
                        .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                        .map(field -> field.getName())
                        .toList()
        );
    }

    private PosicaoResponse posicao(
            Long id,
            Mercado mercado,
            Moeda moeda,
            String custo,
            String atual,
            String resultado,
            String rentabilidade
    ) {
        return new PosicaoResponse(
                id, "T" + id, "Empresa " + id, mercado, moeda,
                new BigDecimal("1.000000"), new BigDecimal("10.000000000000"),
                new BigDecimal(custo), new BigDecimal("20.000000"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z"), new BigDecimal(atual),
                new BigDecimal(resultado), new BigDecimal(rentabilidade)
        );
    }
}
