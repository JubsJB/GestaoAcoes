package com.projeto.resources.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class StandardErrorTest {
    @Test
    void construtorPreservaValoresNulosComCopiaDefensivaEImutavel() {
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("evento", null);
        entrada.put("quantidadeDisponivel", 2);
        var erro = new StandardError(1L, 409, "Conflict", "Posição insuficiente",
                "/operacoes", "POSICAO_INSUFICIENTE", entrada);

        entrada.remove("evento");
        entrada.put("quantidadeDisponivel", 99);
        assertTrue(erro.getDetails().containsKey("evento"));
        assertNull(erro.getDetails().get("evento"));
        assertEquals(2, erro.getDetails().get("quantidadeDisponivel"));
        assertThrows(UnsupportedOperationException.class, () -> erro.getDetails().put("outro", 1));
    }

    @Test
    void setterPreservaValoresNulosComCopiaDefensivaEImutavel() {
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("evento", null);
        entrada.put("quantidadeDisponivel", 2);
        var erro = new StandardError();
        erro.setDetails(entrada);

        entrada.clear();
        assertTrue(erro.getDetails().containsKey("evento"));
        assertNull(erro.getDetails().get("evento"));
        assertEquals(2, erro.getDetails().get("quantidadeDisponivel"));
        assertThrows(UnsupportedOperationException.class, () -> erro.getDetails().clear());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mapaNuloProduzMapaVazioImutavel(boolean setter) {
        StandardError erro;
        if (setter) {
            erro = new StandardError(1L, 409, "Conflict", "Erro", "/operacoes",
                    "POSICAO_INSUFICIENTE", Map.of("anterior", 1));
            erro.setDetails(null);
        } else {
            erro = new StandardError(1L, 409, "Conflict", "Erro", "/operacoes",
                    "POSICAO_INSUFICIENTE", null);
        }
        assertNotNull(erro.getDetails());
        assertTrue(erro.getDetails().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> erro.getDetails().put("outro", 1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void chaveNulaContinuaRejeitada(boolean setter) {
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put(null, "inválido");
        if (setter) {
            var erro = new StandardError(1L, 409, "Conflict", "Erro", "/operacoes",
                    "POSICAO_INSUFICIENTE", Map.of("anterior", 1));
            assertThrows(NullPointerException.class, () -> erro.setDetails(entrada));
            assertEquals(Map.of("anterior", 1), erro.getDetails());
        } else {
            assertThrows(NullPointerException.class, () -> new StandardError(
                    1L, 409, "Conflict", "Erro", "/operacoes", "POSICAO_INSUFICIENTE", entrada));
        }
    }
}
