package com.projeto.services;

import com.projeto.dto.CarteiraCreateRequest;
import com.projeto.dto.CarteiraResponse;
import com.projeto.entities.Carteira;
import com.projeto.mappers.CarteiraMapper;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarteiraServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T14:30:00Z");

    @Mock
    private CarteiraRepository repository;

    private CarteiraService service;

    @BeforeEach
    void setUp() {
        service = new CarteiraService(
                repository,
                new CarteiraMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsPortfolioWithTrimmedNamePreservingInternalSpacesAccentsCaseAndUtcClock() {
        when(repository.saveAndFlush(any(Carteira.class))).thenAnswer(invocation -> {
            Carteira carteira = invocation.getArgument(0);
            ReflectionTestUtils.setField(carteira, "id", 7L);
            return carteira;
        });

        CarteiraResponse response = service.cadastrar(request("  Carteira  Ágil Principal  "));

        ArgumentCaptor<Carteira> captor = ArgumentCaptor.forClass(Carteira.class);
        verify(repository).saveAndFlush(captor.capture());
        Carteira persisted = captor.getValue();
        OffsetDateTime expectedDate = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
        assertEquals("Carteira  Ágil Principal", persisted.getNome());
        assertEquals(expectedDate, persisted.getDataCriacao());
        assertEquals(ZoneOffset.UTC, persisted.getDataCriacao().getOffset());
        assertEquals(7L, response.id());
        assertEquals("Carteira  Ágil Principal", response.nome());
        assertEquals(expectedDate, response.dataCriacao());
    }

    @Test
    void rejectsNullEmptyBlankAndNameAboveMaximumBeforePersistence() {
        List<String> invalidNames = java.util.Arrays.asList(null, "", "   ", " " + "a".repeat(256) + " ");

        for (String invalidName : invalidNames) {
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> service.cadastrar(request(invalidName))
            );
            assertEquals(400, exception.getStatus().value());
            assertEquals(ErrorCodes.REQUEST_INVALIDO, exception.getCode());
            assertEquals(true, exception.getDetails().containsKey("nome"));
        }

        verifyNoInteractions(repository);
    }

    @Test
    void acceptsExactly255CharactersAfterTrim() {
        String name = "Á".repeat(255);
        when(repository.saveAndFlush(any(Carteira.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarteiraResponse response = service.cadastrar(request("  " + name + "  "));

        assertEquals(name, response.nome());
        assertEquals(255, response.nome().length());
        verify(repository).saveAndFlush(any(Carteira.class));
    }

    @Test
    void allowsDuplicateNamesAndReturnsIndependentIds() {
        AtomicLong sequence = new AtomicLong(1L);
        when(repository.saveAndFlush(any(Carteira.class))).thenAnswer(invocation -> {
            Carteira carteira = invocation.getArgument(0);
            ReflectionTestUtils.setField(carteira, "id", sequence.getAndIncrement());
            return carteira;
        });

        CarteiraResponse first = service.cadastrar(request("Carteira Principal"));
        CarteiraResponse second = service.cadastrar(request("Carteira Principal"));

        assertEquals(first.nome(), second.nome());
        assertNotEquals(first.id(), second.id());
        verify(repository, times(2)).saveAndFlush(any(Carteira.class));
    }

    private CarteiraCreateRequest request(String nome) {
        return new CarteiraCreateRequest(nome);
    }
}
