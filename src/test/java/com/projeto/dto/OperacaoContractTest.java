package com.projeto.dto;

import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.mappers.OperacaoMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperacaoContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsOnlyApprovedOperationTypes() {
        assertArrayEquals(
                new TipoOperacao[]{TipoOperacao.COMPRA, TipoOperacao.VENDA},
                TipoOperacao.values()
        );
    }

    @Test
    void validatesRequiredPositiveAndDecimalFields() {
        OperacaoCreateRequest request = new OperacaoCreateRequest();

        Set<ConstraintViolation<OperacaoCreateRequest>> violations = validator.validate(request);

        assertEquals(
                Set.of(
                        "carteiraId",
                        "ticker",
                        "mercado",
                        "tipo",
                        "quantidade",
                        "precoUnitario",
                        "dataOperacao",
                        "ordemNoDia"
                ),
                violations.stream().map(violation -> violation.getPropertyPath().toString()).collect(
                        java.util.stream.Collectors.toSet()
                )
        );

        OperacaoCreateRequest invalid = request(
                new BigDecimal("0"),
                new BigDecimal("-1"),
                0
        );
        Set<String> invalidFields = validator.validate(invalid).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("quantidade", "precoUnitario", "ordemNoDia"), invalidFields);
    }

    @Test
    void rejectsUnknownAndApplicationControlledProperties() {
        OperacaoCreateRequest request = request(new BigDecimal("1"), new BigDecimal("10"), 1);

        assertThrows(IllegalArgumentException.class, () -> request.rejectUnknownProperty("id", 1L));
        assertThrows(IllegalArgumentException.class, () -> request.rejectUnknownProperty("acaoId", 2L));
        assertThrows(IllegalArgumentException.class, () -> request.rejectUnknownProperty("valorTotal", 10));
        assertThrows(IllegalArgumentException.class, () -> request.rejectUnknownProperty("cotacaoAtual", 11));
        assertThrows(IllegalArgumentException.class, () -> request.rejectUnknownProperty("desconhecido", true));
    }

    @Test
    void mapperProjectsCompleteResponseWithoutQuotesOrCalculations() {
        Carteira carteira = new Carteira("Carteira", OffsetDateTime.parse("2026-08-01T10:00:00Z"));
        ReflectionTestUtils.setField(carteira, "id", 1L);
        Acao acao = new Acao(
                "PETR4",
                "Petrobras",
                Mercado.BRASIL,
                Moeda.BRL,
                new BigDecimal("99.000000"),
                OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        ReflectionTestUtils.setField(acao, "id", 2L);
        Operacao operacao = new Operacao(
                carteira,
                acao,
                null,
                TipoOperacao.COMPRA,
                new BigDecimal("100.000000"),
                new BigDecimal("32.470000"),
                LocalDate.of(2026, 8, 10),
                1,
                new BigDecimal("3247.000000000000")
        );
        ReflectionTestUtils.setField(operacao, "id", 3L);

        OperacaoResponse response = new OperacaoMapper().toResponse(operacao);

        assertEquals(3L, response.id());
        assertEquals(1L, response.carteiraId());
        assertEquals("PETR4", response.ticker());
        assertEquals(Mercado.BRASIL, response.mercado());
        assertEquals(null, response.corretoraId());
        assertEquals(TipoOperacao.COMPRA, response.tipo());
        assertEquals(new BigDecimal("100.000000"), response.quantidade());
        assertEquals(new BigDecimal("32.470000"), response.precoUnitario());
        assertEquals(new BigDecimal("3247.000000000000"), response.valorTotal());
    }

    private OperacaoCreateRequest request(BigDecimal quantity, BigDecimal price, Integer order) {
        return new OperacaoCreateRequest(
                1L,
                "PETR4",
                Mercado.BRASIL,
                null,
                TipoOperacao.COMPRA,
                quantity,
                price,
                LocalDate.of(2026, 8, 10),
                order
        );
    }
}
