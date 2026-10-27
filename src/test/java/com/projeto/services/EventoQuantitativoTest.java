package com.projeto.services;

import com.projeto.entities.Mercado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static com.projeto.services.EventoQuantitativo.*;
import static org.junit.jupiter.api.Assertions.*;

class EventoQuantitativoTest {
    private static final UUID TOKEN = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test void dominioIntegraIdentidade() {
        assertNotEquals(Identidade.persistida(Dominio.FINANCEIRO, 1), Identidade.persistida(Dominio.CUSTODIA, 1));
        assertEquals(Identidade.persistida(Dominio.CUSTODIA, 1), Identidade.persistida(Dominio.CUSTODIA, 1));
    }
    @Test void candidataNaoRecebeIdArtificial() {
        var id = Identidade.temporaria(Dominio.CUSTODIA, TOKEN);
        assertNull(id.idPersistido());
        assertEquals(TOKEN, id.tokenTemporario());
        assertNotEquals(id, Identidade.persistida(Dominio.CUSTODIA, 1));
        assertNotEquals(id, Identidade.temporaria(Dominio.FINANCEIRO, TOKEN));
    }
    @Test void identidadeExigeExatamenteUmaAlternativa() {
        assertThrows(IllegalArgumentException.class, () -> new Identidade(Dominio.CUSTODIA, 1L, TOKEN));
        assertThrows(IllegalArgumentException.class, () -> new Identidade(Dominio.CUSTODIA, null, null));
        assertThrows(IllegalArgumentException.class, () -> Identidade.persistida(null, 1));
        assertThrows(IllegalArgumentException.class, () -> Identidade.persistida(Dominio.CUSTODIA, 0));
    }
    @Test void grupoSemCorretoraEhChaveExplicita() {
        assertEquals(new Grupo(null), Grupo.semCorretora());
        assertNotEquals(Grupo.semCorretora(), Grupo.corretora(1));
        assertThrows(IllegalArgumentException.class, () -> Grupo.corretora(0));
    }
    private EventoQuantitativo financeiro(String quantidade, Mercado mercado) {
        return new EventoQuantitativo(Identidade.persistida(Dominio.FINANCEIRO, 1), 1, 1, mercado,
                Natureza.COMPRA, new BigDecimal(quantidade),
                PosicaoCronologicaQuantitativa.financeira(LocalDate.of(2026, 1, 1), 1),
                Grupo.semCorretora(), null, false);
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "0.0000001", "10000000000000", "1E+2147483647"})
    void rejeitaQuantidadeForaDoContrato(String valor) {
        assertThrows(IllegalArgumentException.class, () -> financeiro(valor, Mercado.EUA));
    }
    @Test void preservaPrecisaoEscalaELimites() {
        assertEquals(new BigDecimal("9999999999999.999999"), financeiro("9999999999999.999999", Mercado.EUA).quantidade());
        assertEquals(new BigDecimal("0.000001"), financeiro("0.000001", Mercado.EUA).quantidade());
        assertEquals(new BigDecimal("2.000000"), financeiro("2.000000", Mercado.BRASIL).quantidade());
        assertThrows(IllegalArgumentException.class, () -> financeiro("1.5", Mercado.BRASIL));
    }
    @Test void rejeitaDominioEPosicaoIncompativeis() {
        assertThrows(IllegalArgumentException.class, () -> new EventoQuantitativo(
                Identidade.persistida(Dominio.CUSTODIA, 1), 1, 1, Mercado.EUA, Natureza.COMPRA,
                BigDecimal.ONE, PosicaoCronologicaQuantitativa.financeira(LocalDate.now(), 1),
                Grupo.semCorretora(), null, false));
    }
    @Test void estruturasSaoImutaveisESemAnotacoesDeInfraestrutura() {
        for (Class<?> tipo : new Class<?>[]{EventoQuantitativo.class, Identidade.class, Grupo.class,
                PosicaoCronologicaQuantitativa.class, NormalizadorEventosQuantitativos.Financeiro.class,
                NormalizadorEventosQuantitativos.Custodia.class, NormalizadorEventosQuantitativos.VinculoEstorno.class}) {
            assertTrue(tipo.isRecord());
            assertEquals(0, tipo.getAnnotations().length);
            for (var campo : tipo.getDeclaredFields()) assertTrue(java.lang.reflect.Modifier.isFinal(campo.getModifiers()));
        }
        assertEquals(0, NormalizadorEventosQuantitativos.class.getDeclaredFields().length);
        assertEquals(0, NormalizadorEventosQuantitativos.class.getAnnotations().length);
    }
}
