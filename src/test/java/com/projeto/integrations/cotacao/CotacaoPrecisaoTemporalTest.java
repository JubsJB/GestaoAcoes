package com.projeto.integrations.cotacao;

import com.projeto.entities.*;
import com.projeto.services.exceptions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class CotacaoPrecisaoTemporalTest {
    private static final OffsetDateTime EXATO = OffsetDateTime.parse("2026-10-04T15:30:17.123456Z");

    @ParameterizedTest
    @CsvSource({"0,true", "100000000,true", "123456000,true", "123456700,false",
            "123456001,false", "123456789,false", "999999000,true", "999999001,false"})
    void construtorProtegeReferenciaEObtencaoSemPerderInstante(int nanos, boolean aceito) {
        OffsetDateTime instante = EXATO.withNano(nanos).withOffsetSameInstant(ZoneOffset.ofHours(-3));
        for (boolean referencia : new boolean[]{true, false}) {
            if (aceito) {
                var candidata = criar(referencia ? instante : EXATO.minusSeconds(1), referencia ? EXATO.plusSeconds(1) : instante);
                OffsetDateTime valor = referencia ? candidata.referenciaInstante() : candidata.cotacaoObtidaEm();
                assertEquals(instante.toInstant(), valor.toInstant());
                assertEquals(nanos, valor.getNano());
                assertEquals(ZoneOffset.UTC, valor.getOffset());
            } else {
                ApiException erro = assertThrows(ApiException.class,
                        () -> criar(referencia ? instante : EXATO.minusSeconds(1), referencia ? EXATO.plusSeconds(1) : instante));
                assertEquals(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA, erro.getCode());
                assertEquals(422, erro.getStatus().value());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ReferenciaTipoCotacao.class)
    void clockNaoRepresentavelEhRejeitadoInclusiveEmDataEDesconhecida(ReferenciaTipoCotacao tipo) {
        var dados = new CotacaoData("AAPL", "Apple", "USD", BigDecimal.TEN,
                tipo == ReferenciaTipoCotacao.INSTANTE ? EXATO : null, false,
                tipo == ReferenciaTipoCotacao.DATA ? LocalDate.of(2026, 10, 2) : null, false);
        Clock clock = Clock.fixed(EXATO.withNano(123456789).toInstant(), ZoneOffset.UTC);
        for (boolean cadastro : new boolean[]{true, false}) {
            if (!cadastro && tipo == ReferenciaTipoCotacao.DESCONHECIDA) continue;
            ApiException erro = assertThrows(ApiException.class,
                    () -> CotacaoCandidata.obtidaDe(dados, "AAPL", Mercado.EUA, cadastro, clock));
            assertEquals(ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA, erro.getCode());
        }
    }

    private CotacaoCandidata criar(OffsetDateTime referencia, OffsetDateTime obtencao) {
        return new CotacaoCandidata("AAPL", "Apple", Moeda.USD, BigDecimal.TEN,
                ReferenciaTipoCotacao.INSTANTE, referencia, null, obtencao);
    }
}
