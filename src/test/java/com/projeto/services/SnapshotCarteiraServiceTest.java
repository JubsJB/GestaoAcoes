package com.projeto.services;

import com.projeto.dto.PosicaoResponse;
import com.projeto.dto.SnapshotCarteiraResponse;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.SnapshotCarteira;
import com.projeto.mappers.SnapshotCarteiraMapper;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.SnapshotCarteiraMoedaRepository;
import com.projeto.repositories.SnapshotCarteiraRepository;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ConstraintNameExtractor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SnapshotCarteiraServiceTest {

    @Mock CarteiraRepository carteiraRepository;
    @Mock PosicaoService posicaoService;
    @Mock AgregadorPosicoesPorMoeda agregador;
    @Mock SnapshotCarteiraRepository snapshotRepository;
    @Mock SnapshotCarteiraMoedaRepository componenteRepository;

    private SnapshotCarteiraService service;
    private Carteira carteira;

    @BeforeEach
    void setUp() {
        carteira = new Carteira("Principal", OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(carteira, "id", 1L);
        service = new SnapshotCarteiraService(
                Clock.fixed(Instant.parse("2026-08-27T15:00:00Z"), ZoneOffset.ofHours(-3)),
                carteiraRepository,
                posicaoService,
                agregador,
                snapshotRepository,
                componenteRepository,
                new SnapshotCarteiraMapper(),
                new ConstraintNameExtractor()
        );
    }

    @Test
    void createsEmptySnapshotWithoutArtificialCurrencies() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of());
        when(carteiraRepository.getReferenceById(1L)).thenReturn(carteira);
        when(agregador.agregar(List.of())).thenReturn(List.of());
        when(snapshotRepository.saveAndFlush(any())).thenAnswer(invocation -> identified(invocation.getArgument(0), 10L));

        SnapshotCarteiraResponse response = service.criar(1L);

        assertEquals(10L, response.id());
        assertEquals(1L, response.carteiraId());
        assertEquals(OffsetDateTime.parse("2026-08-27T15:00:00Z"), response.dataHoraSnapshot());
        assertEquals(List.of(), response.patrimonios());
        verify(componenteRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void persistsOfficialBrlAndUsdTotalsOnceAndInCurrencyOrder() {
        List<PosicaoResponse> posicoes = List.of(position(1L, Moeda.BRL), position(2L, Moeda.USD));
        when(posicaoService.listarPorCarteira(1L)).thenReturn(posicoes);
        when(carteiraRepository.getReferenceById(1L)).thenReturn(carteira);
        when(agregador.agregar(posicoes)).thenReturn(List.of(
                new AgregadorPosicoesPorMoeda.TotaisPorMoeda(Moeda.USD, BigDecimal.ONE, new BigDecimal("20.000000000000"), BigDecimal.ZERO),
                new AgregadorPosicoesPorMoeda.TotaisPorMoeda(Moeda.BRL, BigDecimal.ONE, new BigDecimal("10.000000000000"), BigDecimal.ZERO)
        ));
        when(snapshotRepository.saveAndFlush(any())).thenAnswer(invocation -> identified(invocation.getArgument(0), 11L));
        when(componenteRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SnapshotCarteiraResponse response = service.criar(1L);

        assertEquals(List.of(Moeda.BRL, Moeda.USD), response.patrimonios().stream().map(p -> p.moeda()).toList());
        assertEquals(new BigDecimal("10.000000000000"), response.patrimonios().get(0).patrimonioAtual());
        assertEquals(new BigDecimal("20.000000000000"), response.patrimonios().get(1).patrimonioAtual());
        verify(posicaoService).listarPorCarteira(1L);
        verify(agregador).agregar(posicoes);
        verify(snapshotRepository).saveAndFlush(any());
        verify(componenteRepository).saveAllAndFlush(any());
    }

    @Test
    void missingPortfolioStopsBeforeFinancialReadsAndWrites() {
        when(posicaoService.listarPorCarteira(404L)).thenThrow(new ObjectNotFoundException("ausente"));
        assertThrows(ObjectNotFoundException.class, () -> service.criar(404L));
        verify(posicaoService).listarPorCarteira(404L);
        verifyNoInteractions(agregador, snapshotRepository, componenteRepository);
        verify(carteiraRepository, never()).getReferenceById(any());
    }

    @Test
    void mapsOnlyTemporalUniqueViolationToSpecificConflict() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of());
        when(carteiraRepository.getReferenceById(1L)).thenReturn(carteira);
        when(agregador.agregar(List.of())).thenReturn(List.of());
        when(snapshotRepository.saveAndFlush(any())).thenThrow(integrity(
                "uk_snapshot_carteira_carteira_data_hora"
        ));

        ApiException exception = assertThrows(ApiException.class, () -> service.criar(1L));

        assertEquals(409, exception.getStatus().value());
        assertEquals(ErrorCodes.SNAPSHOT_CARTEIRA_DUPLICADO, exception.getCode());
        verifyNoInteractions(componenteRepository);
    }

    @Test
    void doesNotMisclassifyOtherIntegrityViolations() {
        when(posicaoService.listarPorCarteira(1L)).thenReturn(List.of());
        when(carteiraRepository.getReferenceById(1L)).thenReturn(carteira);
        when(agregador.agregar(List.of())).thenReturn(List.of());
        DataIntegrityViolationException failure = integrity("uk_snapshot_carteira_moeda_snapshot_moeda");
        DataIntegrityViolationException unknown = integrity("outra_constraint");
        DataIntegrityViolationException messageOnly = new DataIntegrityViolationException(
                "uk_snapshot_carteira_carteira_data_hora only in message"
        );
        when(snapshotRepository.saveAndFlush(any()))
                .thenThrow(failure)
                .thenThrow(unknown)
                .thenThrow(messageOnly);

        assertEquals(failure, assertThrows(DataIntegrityViolationException.class, () -> service.criar(1L)));
        assertEquals(unknown, assertThrows(DataIntegrityViolationException.class, () -> service.criar(1L)));
        assertEquals(messageOnly, assertThrows(DataIntegrityViolationException.class, () -> service.criar(1L)));
    }

    @Test
    void declaresSingleRepeatableReadWriteTransactionAndOnlyApprovedDependencies() throws Exception {
        Transactional transactional = SnapshotCarteiraService.class
                .getMethod("criar", Long.class)
                .getAnnotation(Transactional.class);

        assertEquals(Isolation.REPEATABLE_READ, transactional.isolation());
        assertTrue(!transactional.readOnly());
        assertEquals(
                List.of("clock", "carteiraRepository", "posicaoService", "agregador", "snapshotRepository",
                        "componenteRepository", "mapper", "constraintNameExtractor"),
                java.util.Arrays.stream(SnapshotCarteiraService.class.getDeclaredFields())
                        .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                        .map(java.lang.reflect.Field::getName)
                        .toList()
        );
    }

    private DataIntegrityViolationException integrity(String name) {
        return new DataIntegrityViolationException("integrity",
                new ConstraintViolationException("native", new SQLException("sql"), name));
    }

    private SnapshotCarteira identified(SnapshotCarteira snapshot, long id) {
        ReflectionTestUtils.setField(snapshot, "id", id);
        return snapshot;
    }

    private PosicaoResponse position(Long id, Moeda moeda) {
        Mercado mercado = moeda == Moeda.BRL ? Mercado.BRASIL : Mercado.EUA;
        return new PosicaoResponse(id, "T" + id, "Empresa", mercado, moeda,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                OffsetDateTime.parse("2026-08-27T10:00:00Z"), BigDecimal.ONE,
                BigDecimal.ZERO.setScale(12), BigDecimal.ZERO.setScale(6));
    }
}
