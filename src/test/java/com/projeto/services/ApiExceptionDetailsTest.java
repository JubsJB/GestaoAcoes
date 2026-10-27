package com.projeto.services;

import com.projeto.services.exceptions.ApiException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionDetailsTest {
    @Test void detalhesPermitemEventoNuloSemPerderCopiaDefensivaOuImutabilidade() {
        Map<String,Object> entrada=new LinkedHashMap<>();entrada.put("evento",null);entrada.put("carteiraId",1L);
        var erro=new ApiException(HttpStatus.CONFLICT,"SALDO_ORIGEM_INSUFICIENTE","Sem saldo",entrada);
        entrada.put("carteiraId",2L);entrada.remove("evento");
        assertTrue(erro.getDetails().containsKey("evento"));assertNull(erro.getDetails().get("evento"));
        assertEquals(1L,erro.getDetails().get("carteiraId"));
        assertThrows(UnsupportedOperationException.class,()->erro.getDetails().put("outro",3L));
    }
    @Test void chamadasFinanceirasAnterioresMantemStatusCodigoEDetalhes() {
        var erro=new ApiException(HttpStatus.CONFLICT,"POSICAO_INSUFICIENTE","Posição insuficiente",Map.of("carteiraId",1L));
        assertEquals(HttpStatus.CONFLICT,erro.getStatus());assertEquals("POSICAO_INSUFICIENTE",erro.getCode());
        assertEquals("Posição insuficiente",erro.getMessage());assertEquals(Map.of("carteiraId",1L),erro.getDetails());
        assertTrue(new ApiException(HttpStatus.BAD_REQUEST,"REQUEST_INVALIDO","Inválido",null).getDetails().isEmpty());
    }
    @Test void chavesNulasContinuamRecusadas() {
        Map<String,Object> entrada=new HashMap<>();entrada.put(null,"inválido");
        assertThrows(NullPointerException.class,()->new ApiException(HttpStatus.BAD_REQUEST,"REQUEST_INVALIDO","Inválido",entrada));
    }
}
