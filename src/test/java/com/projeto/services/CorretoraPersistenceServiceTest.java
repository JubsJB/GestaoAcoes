package com.projeto.services;

import com.projeto.entities.Corretora;
import com.projeto.repositories.CorretoraRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ConstraintNameExtractor;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorretoraPersistenceServiceTest {

    @Mock
    private CorretoraRepository repository;

    @Test
    void translatesLateUniqueConstraintViolationFromConcurrentRace() {
        Corretora broker = new Corretora(
                "11222333000181", "Corretora Exemplo S.A.", null, null, null,
                "01001000", "Praca da Se", null, null, "Se", "Sao Paulo", "SP",
                "ATIVA", OffsetDateTime.of(2026, 8, 20, 12, 30, 0, 0, ZoneOffset.UTC)
        );
        when(repository.existsByCnpj("11222333000181")).thenReturn(false);
        when(repository.saveAndFlush(broker)).thenThrow(integrity("uk_corretora_cnpj"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service().saveUnique(broker)
        );

        assertEquals(ErrorCodes.CORRETORA_DUPLICADA, exception.getCode());
        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void propagatesUnknownOrUnnamedIntegrityViolation() {
        Corretora broker = new Corretora(
                "11222333000181", "Corretora Exemplo S.A.", null, null, null,
                "01001000", "Praca da Se", null, null, "Se", "Sao Paulo", "SP",
                "ATIVA", OffsetDateTime.of(2026, 8, 20, 12, 30, 0, 0, ZoneOffset.UTC)
        );
        when(repository.saveAndFlush(broker))
                .thenThrow(integrity("uk_other"))
                .thenThrow(new DataIntegrityViolationException("uk_corretora_cnpj only in message"));

        assertThrows(DataIntegrityViolationException.class, () -> service().saveUnique(broker));
        assertThrows(DataIntegrityViolationException.class, () -> service().saveUnique(broker));
    }

    private CorretoraPersistenceService service() {
        return new CorretoraPersistenceService(repository, new ConstraintNameExtractor());
    }

    private DataIntegrityViolationException integrity(String name) {
        return new DataIntegrityViolationException("integrity",
                new ConstraintViolationException("native", new SQLException("sql"), name));
    }
}
