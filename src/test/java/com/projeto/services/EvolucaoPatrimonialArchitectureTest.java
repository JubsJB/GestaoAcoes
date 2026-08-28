package com.projeto.services;

import com.projeto.repositories.SnapshotCarteiraRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvolucaoPatrimonialArchitectureTest {

    @Test
    void serviceDependsOnlyOnSnapshotRepositoryAndHasNoLocksOrFinancialFlows() {
        List<String> dependencies = Arrays.stream(EvolucaoPatrimonialService.class.getDeclaredFields())
                .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .map(Field::getType)
                .map(Class::getSimpleName)
                .toList();

        assertEquals(List.of("SnapshotCarteiraRepository"), dependencies);
        assertFalse(dependencies.stream().anyMatch(name -> name.contains("Operacao")
                || name.contains("Posicao") || name.contains("PatrimonioService")
                || name.contains("Resumo") || name.contains("ResultadoRealizado")
                || name.contains("HistoricoCotacao") || name.contains("Provider")));
        assertFalse(Arrays.stream(EvolucaoPatrimonialService.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().toLowerCase().contains("lock")));
    }

    @Test
    void repositoryContractUsesSingleLeftJoinProjectionWithoutPaginationOrLocks() throws Exception {
        var method = SnapshotCarteiraRepository.class
                .getMethod("consultarEvolucaoPatrimonial", Long.class);
        String query = method.getAnnotation(Query.class).value().toLowerCase();

        assertTrue(query.contains("from carteira carteira"));
        assertEquals(2, query.split("left join", -1).length - 1);
        assertTrue(query.contains("order by snapshot.datahorasnapshot asc, snapshot.id asc, componente.moeda asc"));
        assertFalse(query.contains("fetch"));
        assertFalse(query.contains("lock"));
        assertEquals(List.class, method.getReturnType());
        assertEquals(1, method.getParameterCount());
    }
}
