package com.projeto.services;

import com.projeto.dto.CarteiraCreateRequest;
import com.projeto.dto.CarteiraResponse;
import com.projeto.entities.Carteira;
import com.projeto.mappers.CarteiraMapper;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @Test
    void listsPersistedPortfoliosUsingAscendingIdSortAndMapsEveryItemWithoutWritingOrUsingClock() {
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        OffsetDateTime firstDate = OffsetDateTime.parse("2026-08-20T10:15:30Z");
        OffsetDateTime secondDate = OffsetDateTime.parse("2026-08-21T11:20:00Z");
        Carteira first = carteira(1L, "  Carteira Ágil  ", firstDate);
        Carteira second = carteira(2L, "carteira Principal", secondDate);
        when(repository.findAll(sort)).thenReturn(List.of(first, second));

        List<CarteiraResponse> responses = readOnlyService().listar();

        assertEquals(List.of(1L, 2L), responses.stream().map(CarteiraResponse::id).toList());
        assertEquals("  Carteira Ágil  ", responses.get(0).nome());
        assertEquals(firstDate, responses.get(0).dataCriacao());
        assertEquals("carteira Principal", responses.get(1).nome());
        assertEquals(secondDate, responses.get(1).dataCriacao());
        verify(repository).findAll(sort);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void returnsEmptyListWithoutWritingOrUsingClockWhenNoPortfolioExists() {
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        when(repository.findAll(sort)).thenReturn(List.of());

        List<CarteiraResponse> responses = readOnlyService().listar();

        assertEquals(List.of(), responses);
        verify(repository).findAll(sort);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findsPortfolioByIdPreservingPersistedValuesWithoutWritingOrUsingClock() {
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-08-19T09:45:00Z");
        Carteira persisted = carteira(41L, "  Carteira Ágil sem normalização  ", creationDate);
        when(repository.findById(41L)).thenReturn(Optional.of(persisted));

        CarteiraResponse response = readOnlyService().buscarPorId(41L);

        assertEquals(41L, response.id());
        assertEquals("  Carteira Ágil sem normalização  ", response.nome());
        assertEquals(creationDate, response.dataCriacao());
        verify(repository).findById(41L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void throwsObjectNotFoundWithoutWritingOrUsingClockWhenPortfolioIdDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> readOnlyService().buscarPorId(404L)
        );

        assertEquals("Carteira não encontrada para o id: 404", exception.getMessage());
        verify(repository).findById(404L);
        verifyNoMoreInteractions(repository);
    }

    private CarteiraCreateRequest request(String nome) {
        return new CarteiraCreateRequest(nome);
    }

    private Carteira carteira(Long id, String nome, OffsetDateTime dataCriacao) {
        Carteira carteira = new Carteira(nome, dataCriacao);
        ReflectionTestUtils.setField(carteira, "id", id);
        return carteira;
    }

    private CarteiraService readOnlyService() {
        Clock clockThatMustNotBeUsed = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                throw new AssertionError("Clock não deve ser utilizado nas consultas de Carteira");
            }
        };

        return new CarteiraService(repository, new CarteiraMapper(), clockThatMustNotBeUsed);
    }
}
