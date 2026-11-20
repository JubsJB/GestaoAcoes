package com.projeto.config;

import static org.junit.jupiter.api.Assertions.*;
import com.projeto.entities.Mercado;
import com.projeto.services.exceptions.ApiException;
import org.junit.jupiter.api.Test;

class CatalogoDemonstracaoTest {
    @Test void forneceSomenteIdentidadesConhecidas() {
        var catalogo = new CatalogoDemonstracao();
        assertEquals("24.00", catalogo.cotacao(Mercado.BRASIL, "DEMA3").cotacao().toPlainString());
        assertEquals("USD", catalogo.cotacao(Mercado.EUA, "ZDMO").moeda());
        assertEquals("Corretora Demo Norte", catalogo.cnpj("99999999000191").nomeFantasia());
        assertEquals("São Paulo", catalogo.cep("99999001").cidade());
        assertThrows(ApiException.class, () -> catalogo.cotacao(Mercado.EUA, "DEMA3"));
        assertThrows(ApiException.class, () -> catalogo.cnpj("12345678000190"));
        assertThrows(ApiException.class, () -> catalogo.cep("00000000"));
    }
}
