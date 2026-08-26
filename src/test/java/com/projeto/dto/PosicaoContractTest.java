package com.projeto.dto;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.mappers.PosicaoMapper;
import com.projeto.services.CalculadoraPosicao.PosicaoCalculada;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosicaoContractTest {

    @Test
    void exposesExactlyTheElevenApprovedFields() {
        Set<String> fields = Arrays.stream(PosicaoResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "acaoId",
                "ticker",
                "nomeEmpresa",
                "mercado",
                "moeda",
                "quantidadeAtual",
                "precoMedio",
                "custoPosicao",
                "cotacaoAtual",
                "dataHoraCotacao",
                "valorAtualPosicao"
        ), fields);
    }

    @Test
    void mapperOnlyProjectsPersistedActionAndCalculatedState() {
        Acao acao = new Acao(
                "PETR4",
                "Petróleo Brasileiro S.A.",
                Mercado.BRASIL,
                Moeda.BRL,
                new BigDecimal("999.999999"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
        ReflectionTestUtils.setField(acao, "id", 7L);
        PosicaoCalculada calculada = new PosicaoCalculada(
                new BigDecimal("100.000000"),
                new BigDecimal("14.000000000000"),
                new BigDecimal("1400.000000000000")
        );

        BigDecimal valorAtual = new BigDecimal("99999.999900000000");
        PosicaoResponse response = new PosicaoMapper().toResponse(acao, calculada, valorAtual);
        assertEquals(7L, response.acaoId());
        assertEquals("PETR4", response.ticker());
        assertEquals("Petróleo Brasileiro S.A.", response.nomeEmpresa());
        assertEquals(Mercado.BRASIL, response.mercado());
        assertEquals(Moeda.BRL, response.moeda());
        assertEquals(new BigDecimal("100.000000"), response.quantidadeAtual());
        assertEquals(new BigDecimal("14.000000000000"), response.precoMedio());
        assertEquals(new BigDecimal("1400.000000000000"), response.custoPosicao());
        assertEquals(new BigDecimal("999.999999"), response.cotacaoAtual());
        assertEquals(OffsetDateTime.parse("2026-08-01T10:00:00Z"), response.dataHoraCotacao());
        assertEquals(valorAtual, response.valorAtualPosicao());
        assertEquals(12, response.precoMedio().scale());
        assertEquals(12, response.custoPosicao().scale());
    }
}
