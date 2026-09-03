package com.projeto.services;

import com.projeto.dto.PreviaPrecoCompraResponse;
import com.projeto.dto.SugestaoPrecoVendaResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.validation.TickerNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PrecoOperacaoServiceTest {

    private final CarteiraRepository carteiras = mock(CarteiraRepository.class);
    private final AcaoRepository acoes = mock(AcaoRepository.class);
    private final OperacaoRepository operacoes = mock(OperacaoRepository.class);
    private final FechamentoHistoricoService historico = mock(FechamentoHistoricoService.class);
    private final PrecoOperacaoService service = new PrecoOperacaoService(
            carteiras, acoes, operacoes, new TickerNormalizer(), historico,
            Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC)
    );

    private Acao acao;

    @BeforeEach
    void setUp() {
        acao = mock(Acao.class);
        when(acao.getId()).thenReturn(2L);
    }

    @Test
    void previewNormalizesTickerReturnsActionCurrencyAndExactPrice() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        when(acao.getMoeda()).thenReturn(Moeda.BRL);
        when(acoes.findByTickerAndMercado("PETR4", Mercado.BRASIL)).thenReturn(Optional.of(acao));
        when(historico.consultar("PETR4", Mercado.BRASIL, date)).thenReturn(new BigDecimal("42.123456"));

        PreviaPrecoCompraResponse response = service.consultarPreviaCompra(" petr4 ", Mercado.BRASIL, date);

        assertEquals("PETR4", response.ticker());
        assertEquals(Mercado.BRASIL, response.mercado());
        assertEquals(Moeda.BRL, response.moeda());
        assertEquals(date, response.dataCotacao());
        assertEquals(new BigDecimal("42.123456"), response.precoUnitario());
        verifyNoInteractions(carteiras, operacoes);
    }

    @Test
    void previewFailsBeforeProviderWhenActionDoesNotExist() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        when(acoes.findByTickerAndMercado("PETR4", Mercado.BRASIL)).thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundException.class,
                () -> service.consultarPreviaCompra("PETR4", Mercado.BRASIL, date));
        verifyNoInteractions(historico, operacoes);
    }

    @Test
    void previewPropagatesHistoricalErrorAndDoesNotTouchRepositoriesAfterResolution() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        when(acoes.findByTickerAndMercado("PETR4", Mercado.BRASIL)).thenReturn(Optional.of(acao));
        ApiException expected = new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                "LIMITE_REQUISICOES_EXCEDIDO", "limite");
        when(historico.consultar("PETR4", Mercado.BRASIL, date)).thenThrow(expected);

        assertEquals(expected, assertThrows(ApiException.class,
                () -> service.consultarPreviaCompra("PETR4", Mercado.BRASIL, date)));
        verifyNoInteractions(carteiras, operacoes);
    }

    @Test
    void suggestionUsesCanonicalActionAndExactChronologicalQuery() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        Operacao compra = mock(Operacao.class);
        when(compra.getPrecoUnitario()).thenReturn(new BigDecimal("25.123456"));
        when(carteiras.findById(1L)).thenReturn(Optional.of(mock(com.projeto.entities.Carteira.class)));
        when(acoes.findByTickerAndMercado("PETR4", Mercado.BRASIL)).thenReturn(Optional.of(acao));
        when(operacoes.findFirstByCarteiraIdAndAcaoIdAndTipoAndDataOperacaoLessThanEqualOrderByDataOperacaoDescOrdemNoDiaDescIdDesc(
                1L, 2L, TipoOperacao.COMPRA, date)).thenReturn(Optional.of(compra));

        SugestaoPrecoVendaResponse response = service.consultarSugestaoVenda(
                1L, " petr4 ", Mercado.BRASIL, date
        );

        assertEquals(new BigDecimal("25.123456"), response.precoUnitarioSugerido());
        verifyNoInteractions(historico);
        verify(carteiras, never()).findByIdForUpdate(1L);
    }

    @Test
    void suggestionReturnsExplicitNullWhenNoApplicablePurchaseExists() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        when(carteiras.findById(1L)).thenReturn(Optional.of(mock(com.projeto.entities.Carteira.class)));
        when(acoes.findByTickerAndMercado("PETR4", Mercado.BRASIL)).thenReturn(Optional.of(acao));
        when(operacoes.findFirstByCarteiraIdAndAcaoIdAndTipoAndDataOperacaoLessThanEqualOrderByDataOperacaoDescOrdemNoDiaDescIdDesc(
                1L, 2L, TipoOperacao.COMPRA, date)).thenReturn(Optional.empty());

        assertNull(service.consultarSugestaoVenda(1L, "PETR4", Mercado.BRASIL, date)
                .precoUnitarioSugerido());
        verifyNoInteractions(historico);
    }

    @Test
    void distinguishesMissingPortfolioAndActionFromAbsentPurchase() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        when(carteiras.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ObjectNotFoundException.class,
                () -> service.consultarSugestaoVenda(404L, "PETR4", Mercado.BRASIL, date));
        verifyNoInteractions(acoes, operacoes, historico);
    }

    @Test
    void rejectsFutureDateBeforeAnyHistoricalOrOperationQuery() {
        assertThrows(ApiException.class,
                () -> service.consultarPreviaCompra("PETR4", Mercado.BRASIL, LocalDate.of(2026, 9, 1)));
        verifyNoInteractions(acoes, operacoes, historico);
    }
}
