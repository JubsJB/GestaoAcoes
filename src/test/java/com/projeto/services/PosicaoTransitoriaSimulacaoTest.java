package com.projeto.services;

import com.projeto.entities.LimitePosicaoCustodiaException;
import com.projeto.entities.Mercado;
import com.projeto.entities.PosicaoRacionalCustodia;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static com.projeto.services.EventoQuantitativo.*;
import static com.projeto.services.MotorReplayQuantitativo.*;
import static com.projeto.services.ValidadorHistoricoQuantitativo.*;
import static org.junit.jupiter.api.Assertions.*;

class PosicaoTransitoriaSimulacaoTest {
    private static final LocalDate DATA = LocalDate.of(2026, 1, 1);
    private static final Grupo A = Grupo.corretora(1), B = Grupo.corretora(2), C = Grupo.corretora(3);
    private static final BigInteger D = BigInteger.TEN.pow(512).subtract(BigInteger.ONE);
    private final MotorReplayQuantitativo motor = new MotorReplayQuantitativo();
    private final ValidadorHistoricoQuantitativo validador = new ValidadorHistoricoQuantitativo();
    private EventoQuantitativo compra() {
        return new EventoQuantitativo(Identidade.persistida(Dominio.FINANCEIRO, 1), 1, 2, Mercado.EUA,
                Natureza.COMPRA, new BigDecimal("100"), PosicaoCronologicaQuantitativa.financeira(DATA.minusDays(1), 1), A, null, false);
    }
    private EventoQuantitativo original(String quantidade, boolean neutralizada) {
        return new EventoQuantitativo(Identidade.persistida(Dominio.CUSTODIA, 1), 1, 2, Mercado.EUA,
                Natureza.TRANSFERENCIA, new BigDecimal(quantidade), PosicaoCronologicaQuantitativa.custodia(DATA, 0,
                PosicaoRacionalCustodia.criar(BigInteger.ONE, D)), A, B, neutralizada);
    }
    private EventoQuantitativo candidata(EventoQuantitativo original) {
        var provisoria = PosicaoRacionalTransitoria.INICIO.entre(original.posicao().fracao());
        return new EventoQuantitativo(Identidade.temporaria(Dominio.CUSTODIA,
                UUID.fromString("00000000-0000-0000-0000-000000000004")), 1, 2, Mercado.EUA,
                Natureza.TRANSFERENCIA, new BigDecimal("10"),
                PosicaoCronologicaQuantitativa.custodiaTransitoria(DATA, 0, provisoria), A, C, false);
    }
    private ResultadoValidacao validar(Resultado antes, Resultado depois, EventoQuantitativo candidata) {
        return validador.validar(antes, depois, new Intencao(TipoIntencao.TRANSFERENCIA, candidata.identidade()));
    }
    private static void decimal(String esperado, BigDecimal valor) {
        assertEquals(0, new BigDecimal(esperado).compareTo(valor));
    }
    @Test void simulaCandidata513DigitosPreservandoIdentidadesCheckpointsEOrdem() {
        var compra = compra(); var original = original("50", false); var candidata = candidata(original);
        var entrada = List.of(compra, candidata, original);
        var antes = motor.reproduzir(List.of(compra, original)); var depois = motor.reproduzir(entrada);
        assertEquals(513, candidata.posicao().fracao().getDenominador().toString().length());
        assertEquals(entrada, depois.checkpoints().stream().map(Checkpoint::evento).toList());
        assertSame(compra, depois.checkpoints().get(0).evento());
        assertSame(original, depois.checkpoints().get(2).evento());
        assertNull(candidata.identidade().idPersistido());
        decimal("100", depois.estadoFinal().quantidadeConsolidada());
        decimal("40", depois.estadoFinal().saldo(A)); decimal("50", depois.estadoFinal().saldo(B)); decimal("10", depois.estadoFinal().saldo(C));
        assertTrue(validar(antes, depois, candidata).valido());
        assertThrows(LimitePosicaoCustodiaException.class, () -> candidata.posicao().fracao().paraPersistivel());
        assertEquals(D, original.posicao().fracao().paraPersistivel().getDenominador());
        assertEquals(0, original.posicao().ancoraFinanceira());
        assertEquals(1, compra.posicao().ancoraFinanceira());
        for (var checkpoint : depois.checkpoints()) assertTrue(checkpoint.estado().reconciliada());
    }
    @Test void fracaoForaDoLimiteNaoImpedeDiagnosticoDeCustodiaFuturaInvalidada() {
        var compra = compra(); var original = original("100", false); var candidata = candidata(original);
        var antes = motor.reproduzir(List.of(compra, original)); var depois = motor.reproduzir(List.of(compra, candidata, original));
        var avaliacao = validar(antes, depois, candidata);
        assertFalse(avaliacao.valido());
        assertEquals(Codigo.MOVIMENTACAO_CUSTODIA_INVALIDADA, avaliacao.conflito().codigo());
        assertEquals(original.identidade(), avaliacao.conflito().identidade());
        decimal("90", avaliacao.conflito().evidencia().saldoDisponivel());
        assertThrows(LimitePosicaoCustodiaException.class, () -> candidata.posicao().fracao().paraPersistivel());
    }
    @Test void validacaoDiferencialComCandidataTransitoriaDetectaDeficitFuturo() {
        var compra = compra(); var original = original("50", false); var candidata = candidata(original);
        var venda = new EventoQuantitativo(Identidade.persistida(Dominio.FINANCEIRO, 2), 1, 2, Mercado.EUA,
                Natureza.VENDA, new BigDecimal("45"), PosicaoCronologicaQuantitativa.financeira(DATA.plusDays(1), 1), A, null, false);
        var antes = motor.reproduzir(List.of(compra, original, venda));
        var depois = motor.reproduzir(List.of(compra, candidata, original, venda));
        var avaliacao = validar(antes, depois, candidata);
        assertEquals(Codigo.CONFLITO_HISTORICO_QUANTITATIVO, avaliacao.conflito().codigo());
        assertEquals(venda.identidade(), avaliacao.conflito().identidade());
        decimal("5", avaliacao.conflito().evidencia().saldoAntes());
        decimal("-5", avaliacao.conflito().evidencia().saldoDepois());
        var pontos = validador.alinhar(antes, depois);
        assertEquals(candidata.identidade(), pontos.get(1).evento().identidade());
        decimal("100", pontos.get(1).antes().saldo(A)); decimal("90", pontos.get(1).depois().saldo(A));
    }
    @Test void checkpointEstornadoContinuaPreservadoComCandidataTransitoria() {
        var compra = compra(); var original = original("50", true); var candidata = candidata(original);
        var antes = motor.reproduzir(List.of(compra, original)); var depois = motor.reproduzir(List.of(compra, candidata, original));
        assertTrue(validar(antes, depois, candidata).valido());
        assertEquals(3, depois.checkpoints().size());
        assertSame(original, depois.checkpoints().get(2).evento());
        assertTrue(depois.checkpoints().get(2).evento().neutralizado());
        decimal("90", depois.estadoFinal().saldo(A)); decimal("0", depois.estadoFinal().saldo(B));
        assertThrows(UnsupportedOperationException.class, () -> depois.checkpoints().clear());
    }
    @Test void igualdadeEntreFabricasNaoPermiteReutilizarPosicaoExistente() {
        var original = original("50", false);
        var candidata = new EventoQuantitativo(Identidade.temporaria(Dominio.CUSTODIA, UUID.randomUUID()), 1, 2, Mercado.EUA,
                Natureza.TRANSFERENCIA, BigDecimal.ONE,
                PosicaoCronologicaQuantitativa.custodiaTransitoria(DATA, 0,
                        PosicaoRacionalTransitoria.dePersistivel(original.posicao().fracao().paraPersistivel())), A, C, false);
        assertEquals(original.posicao(), candidata.posicao());
        assertThrows(IllegalArgumentException.class, () -> motor.reproduzir(List.of(compra(), original, candidata)));
    }
}
