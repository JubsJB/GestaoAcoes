package com.projeto.services;

import com.projeto.resources.CarteiraResource;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotCarteiraArchitectureTest {

    @Test
    void exposesOnlyApprovedSnapshotPostAndNoHistoricalPublicApi() {
        List<Method> snapshotMethods = Arrays.stream(CarteiraResource.class.getDeclaredMethods())
                .filter(method -> method.getName().toLowerCase().contains("snapshot"))
                .toList();

        assertEquals(1, snapshotMethods.size());
        Method method = snapshotMethods.get(0);
        assertTrue(method.isAnnotationPresent(PostMapping.class));
        assertEquals(List.of("/{carteiraId}/snapshots"),
                List.of(method.getAnnotation(PostMapping.class).value()));
        assertFalse(method.isAnnotationPresent(GetMapping.class));
        assertFalse(method.isAnnotationPresent(PatchMapping.class));
        assertFalse(method.isAnnotationPresent(PutMapping.class));
        assertFalse(method.isAnnotationPresent(DeleteMapping.class));
        assertEquals(1, method.getParameterCount());
    }

    @Test
    void serviceHasNoProviderHistoryPatrimonySummaryOrLockDependencies() {
        List<String> dependencyTypes = Arrays.stream(SnapshotCarteiraService.class.getDeclaredFields())
                .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .map(field -> field.getType().getSimpleName())
                .toList();

        assertFalse(dependencyTypes.stream().anyMatch(type -> type.contains("Provider")));
        assertFalse(dependencyTypes.contains("HistoricoCotacaoRepository"));
        assertFalse(dependencyTypes.contains("PatrimonioService"));
        assertFalse(dependencyTypes.contains("ResumoCarteiraService"));
        assertFalse(dependencyTypes.contains("OperacaoService"));
        assertFalse(Arrays.stream(SnapshotCarteiraService.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().toLowerCase().contains("lock")));
    }
}
