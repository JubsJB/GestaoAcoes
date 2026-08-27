package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.HistoricoCotacao;
import com.projeto.repositories.HistoricoCotacaoRepository;
import com.projeto.resources.AcaoResource;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoricoCotacaoArchitectureTest {

    @Test
    void somenteFronteirasDePersistenciaDependemDoRepositoryHistorico() {
        assertTrue(dependeDeHistorico(AcaoPersistenceService.class));
        assertTrue(dependeDeHistorico(AcaoCotacaoPersistenceService.class));

        for (Class<?> tipo : List.of(
                AcaoService.class,
                PosicaoService.class,
                PatrimonioService.class,
                ResumoCarteiraService.class,
                ResultadoRealizadoService.class
        )) {
            assertFalse(dependeDeHistorico(tipo), tipo.getSimpleName());
        }
    }

    @Test
    void modeloPermaneceUnidirecionalESemEndpointPublico() {
        assertFalse(Arrays.stream(Acao.class.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(tipo -> tipo == HistoricoCotacao.class || Iterable.class.isAssignableFrom(tipo)));
        assertFalse(Arrays.stream(AcaoResource.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .map(Method::getName)
                .anyMatch(nome -> nome.toLowerCase().contains("historico") || nome.toLowerCase().contains("cotacoes")));
    }

    private boolean dependeDeHistorico(Class<?> tipo) {
        return Arrays.stream(tipo.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(HistoricoCotacaoRepository.class::equals);
    }
}
