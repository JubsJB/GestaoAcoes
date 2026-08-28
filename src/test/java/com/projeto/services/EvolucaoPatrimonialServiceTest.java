package com.projeto.services;

import com.projeto.dto.EvolucaoPatrimonialResponse;
import com.projeto.entities.Moeda;
import com.projeto.repositories.SnapshotCarteiraEvolucaoProjection;
import com.projeto.repositories.SnapshotCarteiraRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvolucaoPatrimonialServiceTest {

    @Mock
    private SnapshotCarteiraRepository repository;

    private EvolucaoPatrimonialService service;

    @BeforeEach
    void setUp() {
        service = new EvolucaoPatrimonialService(repository);
    }

    @Test
    void groupsOrderedRowsAndPreservesEmptySnapshotAndExactValues() {
        OffsetDateTime primeiro = OffsetDateTime.parse("2026-08-28T10:00:00Z");
        OffsetDateTime segundo = OffsetDateTime.parse("2026-08-28T14:00:00Z");
        BigDecimal brl = new BigDecimal("1000.123456789012");
        BigDecimal usd = new BigDecimal("50.000000000000");
        List<SnapshotCarteiraEvolucaoProjection> linhas = List.of(
                linha(10L, primeiro, Moeda.BRL, brl),
                linha(10L, primeiro, Moeda.USD, usd),
                linha(20L, segundo, null, null)
        );
        when(repository.consultarEvolucaoPatrimonial(1L)).thenReturn(linhas);

        EvolucaoPatrimonialResponse response = service.consultar(1L);

        assertEquals(1L, response.carteiraId());
        assertEquals(List.of(10L, 20L), response.pontos().stream().map(p -> p.snapshotId()).toList());
        assertEquals(List.of(Moeda.BRL, Moeda.USD), response.pontos().get(0).patrimonios().stream()
                .map(p -> p.moeda()).toList());
        assertEquals(brl, response.pontos().get(0).patrimonios().get(0).patrimonioAtual());
        assertEquals(12, response.pontos().get(0).patrimonios().get(0).patrimonioAtual().scale());
        assertTrue(response.pontos().get(1).patrimonios().isEmpty());
        verify(repository, times(1)).consultarEvolucaoPatrimonial(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void returnsEmptySeriesFromPortfolioMarker() {
        SnapshotCarteiraEvolucaoProjection marcador = linha(null, null, null, null);
        when(repository.consultarEvolucaoPatrimonial(1L)).thenReturn(List.of(marcador));

        EvolucaoPatrimonialResponse response = service.consultar(1L);

        assertTrue(response.pontos().isEmpty());
    }

    @Test
    void reportsMissingPortfolioWhenQueryReturnsNoRows() {
        when(repository.consultarEvolucaoPatrimonial(404L)).thenReturn(List.of());

        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> service.consultar(404L)
        );

        assertEquals("Carteira não encontrada para o id: 404", exception.getMessage());
        verify(repository, times(1)).consultarEvolucaoPatrimonial(404L);
    }

    @Test
    void usesReadOnlyDefaultIsolation() throws Exception {
        Transactional transaction = EvolucaoPatrimonialService.class
                .getMethod("consultar", Long.class)
                .getAnnotation(Transactional.class);

        assertTrue(transaction.readOnly());
        assertEquals(Isolation.DEFAULT, transaction.isolation());
    }

    private SnapshotCarteiraEvolucaoProjection linha(
            Long snapshotId,
            OffsetDateTime dataHora,
            Moeda moeda,
            BigDecimal patrimonio
    ) {
        return new Linha(snapshotId, dataHora, moeda, patrimonio);
    }

    private record Linha(
            Long snapshotId,
            OffsetDateTime dataHoraSnapshot,
            Moeda moeda,
            BigDecimal patrimonioAtual
    ) implements SnapshotCarteiraEvolucaoProjection {

        @Override public Long getSnapshotId() { return snapshotId; }
        @Override public OffsetDateTime getDataHoraSnapshot() { return dataHoraSnapshot; }
        @Override public Moeda getMoeda() { return moeda; }
        @Override public BigDecimal getPatrimonioAtual() { return patrimonioAtual; }
    }
}
