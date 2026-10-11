package com.projeto.integrations.cotacao;

import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.ReferenciaTipoCotacao;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CotacaoCandidataTest {
    private final Clock clock = spy(Clock.fixed(Instant.parse("2026-10-03T15:00:00Z"), ZoneOffset.ofHours(-3)));

    @Test
    void cadastroSemReferenciaNaoInventaMercadoMasTemObtencaoUtc() {
        CotacaoCandidata candidata = CotacaoCandidata.obtidaDe(dados(null), "AAPL", Mercado.EUA, true, clock);
        assertEquals(ReferenciaTipoCotacao.DESCONHECIDA, candidata.referenciaTipo());
        assertNull(candidata.referenciaData());
        assertNull(candidata.referenciaInstante());
        assertEquals(OffsetDateTime.parse("2026-10-03T15:00:00Z"), candidata.cotacaoObtidaEm());
        assertEquals(new BigDecimal("250.123456"), candidata.cotacao());
        assertEquals(Moeda.USD, candidata.moeda());
    }

    @Test
    void atualizacaoSemReferenciaNaoProduzObtencaoValida() {
        ApiException erro = assertThrows(ApiException.class,
                () -> CotacaoCandidata.obtidaDe(dados(null), "AAPL", Mercado.EUA, false, clock));
        assertEquals(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA, erro.getCode());
        verify(clock, never()).instant();
    }

    @Test
    void instantePreservaInstanteComUtcEObtencaoIndependente() {
        OffsetDateTime mercado = OffsetDateTime.parse("2026-10-02T12:00:00-03:00");
        CotacaoCandidata c = CotacaoCandidata.obtidaDe(dados(mercado), "AAPL", Mercado.EUA, false, clock);
        assertEquals(ReferenciaTipoCotacao.INSTANTE, c.referenciaTipo());
        assertEquals(mercado.toInstant(), c.referenciaInstante().toInstant());
        assertEquals(ZoneOffset.UTC, c.referenciaInstante().getOffset());
        assertNull(c.referenciaData());
        assertNotEquals(c.referenciaInstante(), c.cotacaoObtidaEm());
    }

    @Test
    void construtorRejeitaEstadosTemporaisInvalidosEObtencaoAusente() {
        OffsetDateTime obtida = OffsetDateTime.now(clock);
        LocalDate data = LocalDate.of(2026, 10, 2);
        for (ReferenciaTipoCotacao tipo : ReferenciaTipoCotacao.values()) {
            for (LocalDate referenciaData : new LocalDate[]{null, data}) {
                for (OffsetDateTime instante : new OffsetDateTime[]{null, obtida}) {
                    boolean valido = switch (tipo) {
                        case DATA -> referenciaData != null && instante == null;
                        case INSTANTE -> instante != null && referenciaData == null;
                        case DESCONHECIDA -> referenciaData == null && instante == null;
                    };
                    if (valido) continue;
                    assertThrows(IllegalArgumentException.class, () -> new CotacaoCandidata("AAPL", "Apple",
                            Moeda.USD, BigDecimal.TEN, tipo, instante, referenciaData, obtida));
                }
            }
        }
        assertThrows(IllegalArgumentException.class, () -> new CotacaoCandidata("AAPL", "Apple",
                Moeda.USD, BigDecimal.TEN, ReferenciaTipoCotacao.DATA, null, data, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.1234567", "10000000000000"})
    void precoInvalidoNaoProduzObtencao(String preco) {
        CotacaoData dados = new CotacaoData("AAPL", "Apple", "USD", new BigDecimal(preco), null, false);
        assertThrows(ApiException.class, () -> CotacaoCandidata.obtidaDe(dados, "AAPL", Mercado.EUA, true, clock));
        verify(clock, never()).instant();
    }

    @Test
    void identidadeMoedaOuPayloadInvalidosNaoProduzemCandidata() {
        for (CotacaoData dados : new CotacaoData[]{null,
                new CotacaoData("MSFT", "Apple", "USD", BigDecimal.TEN, null, false),
                new CotacaoData("AAPL", "Apple", "BRL", BigDecimal.TEN, null, false),
                new CotacaoData("AAPL", null, "USD", BigDecimal.TEN, null, false),
                new CotacaoData("AAPL", "Apple", "USD", null, null, false)}) {
            assertThrows(ApiException.class, () -> CotacaoCandidata.obtidaDe(dados, "AAPL", Mercado.EUA, true, clock));
        }
        verify(clock, never()).instant();
    }

    @Test
    void renomeacaoExplicitaSomenteCadastroAceitaNovaIdentidade() {
        CotacaoData dados = new CotacaoData("NEW3", "Empresa", "BRL", BigDecimal.TEN, null, true);
        ApiException erro = assertThrows(ApiException.class,
                () -> CotacaoCandidata.obtidaDe(dados, "OLD3", Mercado.BRASIL, false, clock));
        assertEquals(ErrorCodes.TICKER_CANONICO_DIVERGENTE, erro.getCode());
        assertEquals(409, erro.getStatus().value());
        verify(clock, never()).instant();
        assertEquals("NEW3", CotacaoCandidata.obtidaDe(dados, "OLD3", Mercado.BRASIL, true, clock).ticker());
    }

    private CotacaoData dados(OffsetDateTime instante) {
        return new CotacaoData("AAPL", "Apple", "USD", new BigDecimal("250.123456"), instante, false);
    }
}
