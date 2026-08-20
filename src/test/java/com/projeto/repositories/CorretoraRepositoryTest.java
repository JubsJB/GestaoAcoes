package com.projeto.repositories;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Corretora;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
@Transactional
class CorretoraRepositoryTest {

    @Autowired
    private CorretoraRepository repository;

    @Test
    void persistsCompleteBrokerAndFindsNormalizedCnpj() {
        Corretora saved = repository.saveAndFlush(completeBroker("11222333000181"));

        Corretora found = repository.findByCnpj("11222333000181").orElseThrow();
        assertEquals(saved.getId(), found.getId());
        assertEquals("Corretora Exemplo S.A.", found.getRazaoSocial());
        assertEquals("SP", found.getUf());
        assertFalse(found.isValidadaMercadoFinanceiro());
        assertTrue(repository.existsByCnpj("11222333000181"));
    }

    @Test
    void persistsOptionalFieldsAsNull() {
        Corretora saved = repository.saveAndFlush(new Corretora(
                "04252011000110", "Corretora Sem Opcionais S.A.", null, null, null,
                "01001000", "Praca da Se", null, null, "Se", "Sao Paulo", "SP",
                "ATIVA", OffsetDateTime.of(2026, 8, 20, 12, 30, 0, 0, ZoneOffset.UTC)
        ));

        assertNull(saved.getNomeFantasia());
        assertNull(saved.getEmail());
        assertNull(saved.getTelefone());
        assertNull(saved.getNumero());
        assertNull(saved.getComplemento());
    }

    @Test
    void uniqueConstraintIsFinalProtectionAgainstConcurrentDuplicateRace() {
        repository.saveAndFlush(completeBroker("11222333000181"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(completeBroker("11222333000181"))
        );
    }

    private Corretora completeBroker(String cnpj) {
        return new Corretora(
                cnpj,
                "Corretora Exemplo S.A.",
                "Corretora Exemplo",
                "contato@exemplo.test",
                "1130000000",
                "01001000",
                "Praca da Se",
                "100",
                "10 andar",
                "Se",
                "Sao Paulo",
                "SP",
                "ATIVA",
                OffsetDateTime.of(2026, 8, 20, 12, 30, 0, 0, ZoneOffset.UTC)
        );
    }
}
