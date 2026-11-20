package com.projeto.services;

import com.projeto.entities.Mercado;
import com.projeto.entities.PosicaoRacionalCustodia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static com.projeto.services.EventoQuantitativo.*;
import static com.projeto.services.MotorReplayQuantitativo.*;
import static org.junit.jupiter.api.Assertions.*;

class MotorReplayQuantitativoTest {
    private static final LocalDate DATA = LocalDate.of(2026, 1, 1);
    private static final Grupo A = Grupo.corretora(1);
    private static final Grupo B = Grupo.corretora(2);
    private static final Grupo C = Grupo.corretora(3);
    private static final Grupo SEM = Grupo.semCorretora();
    private final MotorReplayQuantitativo motor = new MotorReplayQuantitativo();

    private EventoQuantitativo financeiro(long id, int ordem, Natureza natureza, Grupo grupo, String q) {
        return financeiro(id, ordem, natureza, grupo, q, Mercado.EUA);
    }
    private EventoQuantitativo financeiro(long id, int ordem, Natureza natureza, Grupo grupo, String q, Mercado mercado) {
        return new EventoQuantitativo(Identidade.persistida(Dominio.FINANCEIRO, id), 1, 2, mercado,
                natureza, new BigDecimal(q), PosicaoCronologicaQuantitativa.financeira(DATA, ordem), grupo, null, false);
    }
    private EventoQuantitativo custodia(long id, int ancora, int n, int d, Grupo origem, Grupo destino, String q, boolean neutralizada) {
        return new EventoQuantitativo(Identidade.persistida(Dominio.CUSTODIA, id), 1, 2, Mercado.EUA,
                origem.equals(SEM) ? Natureza.ATRIBUICAO : Natureza.TRANSFERENCIA, new BigDecimal(q),
                PosicaoCronologicaQuantitativa.custodia(DATA, ancora,
                        PosicaoRacionalCustodia.criar(BigInteger.valueOf(n), BigInteger.valueOf(d))),
                origem, destino, neutralizada);
    }
    private Resultado replay(EventoQuantitativo... eventos) {
        var resultado = motor.reproduzir(List.of(eventos));
        assertEquals(eventos.length, resultado.checkpoints().size());
        for (int i = 0; i < eventos.length; i++) {
            assertSame(eventos[i], resultado.checkpoints().get(i).evento());
            var estado = resultado.checkpoints().get(i).estado();
            decimal(estado.quantidadeConsolidada().toPlainString(), estado.somaSaldos());
            decimal("0", estado.diferencaReconciliacao());
            assertTrue(estado.reconciliada());
        }
        return resultado;
    }
    private static void decimal(String esperado, BigDecimal valor) {
        assertEquals(0, new BigDecimal(esperado).compareTo(valor), () -> esperado + " != " + valor);
    }

    @Test void compraEVendaNaMesmaCorretora() {
        var r = replay(financeiro(9, 1, Natureza.COMPRA, A, "100"), financeiro(1, 2, Natureza.VENDA, A, "40"));
        decimal("100", r.checkpoints().get(0).estado().saldo(A));
        decimal("60", r.estadoFinal().saldo(A));
        decimal("60", r.estadoFinal().quantidadeConsolidada());
        assertTrue(r.estadoFinal().primeirosNegativos().isEmpty());
    }
    @Test void compraEVendaSemCorretora() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, SEM, "100"), financeiro(2, 2, Natureza.VENDA, SEM, "40"));
        decimal("60", r.estadoFinal().saldo(SEM));
        assertEquals(List.of(SEM), new ArrayList<>(r.estadoFinal().saldos().keySet()));
    }
    @Test void transferenciaAplicaAmbosEfeitosEmUmUnicoCheckpoint() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "100"), custodia(1, 1, 1, 2, A, B, "40", false));
        var cp = r.checkpoints().get(1);
        decimal("60", cp.estado().saldo(A));
        decimal("40", cp.estado().saldo(B));
        decimal("100", cp.estado().quantidadeConsolidada());
        assertEquals(2, r.checkpoints().size());
    }
    @Test void atribuicaoDebitaGrupoSemCorretora() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, SEM, "100"), custodia(1, 1, 1, 2, SEM, A, "40", false));
        decimal("60", r.estadoFinal().saldo(SEM));
        decimal("40", r.estadoFinal().saldo(A));
        decimal("100", r.estadoFinal().quantidadeConsolidada());
        assertEquals(SEM, r.checkpoints().get(0).evento().grupoOrigem());
    }
    @Test void cadeiaDeTransferencias() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "100"),
                custodia(1, 1, 1, 3, A, B, "60", false), custodia(2, 1, 2, 3, B, C, "20", false));
        decimal("40", r.estadoFinal().saldo(A));
        decimal("40", r.estadoFinal().saldo(B));
        decimal("20", r.estadoFinal().saldo(C));
    }
    @Test void cicloDeTransferenciasPreservaGruposZerados() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "100"),
                custodia(1, 1, 1, 4, A, B, "100", false), custodia(2, 1, 1, 2, B, C, "100", false),
                custodia(3, 1, 3, 4, C, A, "100", false));
        decimal("100", r.estadoFinal().saldo(A));
        decimal("0", r.estadoFinal().saldo(B));
        decimal("0", r.estadoFinal().saldo(C));
        assertEquals(3, r.estadoFinal().saldos().size());
        assertTrue(r.estadoFinal().primeirosNegativos().isEmpty());
    }
    @Test void movimentaQuantidadeTotalDisponivel() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "0.123456"),
                custodia(1, 1, 1, 2, A, B, "0.123456", false));
        decimal("0", r.estadoFinal().saldo(A));
        decimal("0.123456", r.estadoFinal().saldo(B));
        assertTrue(r.estadoFinal().primeirosNegativos().isEmpty());
    }
    @Test void estornoDeTransferenciaMantemOriginalSemEfeito() {
        var original = custodia(1, 1, 1, 2, A, B, "40", true);
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "100"), original);
        decimal("100", r.estadoFinal().saldo(A));
        decimal("0", r.estadoFinal().saldo(B));
        assertSame(original, r.checkpoints().get(1).evento());
        assertTrue(r.checkpoints().get(1).evento().neutralizado());
        decimal("40", r.checkpoints().get(1).evento().quantidade());
    }
    @Test void estornoDeAtribuicaoNaoCriaInversao() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, SEM, "100"), custodia(1, 1, 1, 2, SEM, A, "40", true));
        decimal("100", r.estadoFinal().saldo(SEM));
        decimal("0", r.estadoFinal().saldo(A));
        assertEquals(2, r.checkpoints().size());
        assertTrue(r.estadoFinal().primeirosNegativos().isEmpty());
    }
    @Test void preservaCheckpointNeutralizadoEmPosicaoEncerrada() {
        var original = custodia(1, 2, 1, 2, A, B, "10", true);
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "10"), financeiro(2, 2, Natureza.VENDA, A, "10"), original);
        assertEquals(3, r.checkpoints().size());
        decimal("0", r.checkpoints().get(1).estado().quantidadeConsolidada());
        decimal("0", r.checkpoints().get(2).estado().quantidadeConsolidada());
        assertEquals(original.posicao(), r.checkpoints().get(2).evento().posicao());
    }
    @Test void negativoIntermediarioPermaneceComoEvidenciaAposSaldoFinalPositivo() {
        var venda = financeiro(2, 2, Natureza.VENDA, B, "10");
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "100"), venda, financeiro(3, 3, Natureza.COMPRA, B, "20"));
        decimal("-10", r.checkpoints().get(1).estado().saldo(B));
        decimal("10", r.estadoFinal().saldo(B));
        var evidencia = r.estadoFinal().primeirosNegativos().get(B);
        assertEquals(venda.identidade(), evidencia.identidade());
        assertEquals(venda.posicao(), evidencia.posicao());
        assertEquals(Natureza.VENDA, evidencia.natureza());
        decimal("-10", evidencia.saldoAposEvento());
        assertFalse(r.estadoFinal().primeirosNegativos().containsKey(A));
    }
    @Test void primeiraOcorrenciaNaoEhSubstituidaPeloPiorSaldoNemPeloNovoCiclo() {
        var primeira = financeiro(2, 2, Natureza.VENDA, B, "1");
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "100"), primeira,
                financeiro(3, 3, Natureza.VENDA, B, "9"), financeiro(4, 4, Natureza.COMPRA, B, "10"),
                financeiro(5, 5, Natureza.VENDA, B, "2"), financeiro(6, 6, Natureza.COMPRA, B, "5"));
        var evidencia = r.estadoFinal().primeirosNegativos().get(B);
        assertEquals(primeira.identidade(), evidencia.identidade());
        decimal("-1", evidencia.saldoAposEvento());
        decimal("3", r.estadoFinal().saldo(B));
        assertFalse(r.checkpoints().get(0).estado().primeirosNegativos().containsKey(B));
    }
    @Test void consolidadoEncerradoNaoOcultaDeficitDeGrupo() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "100"), financeiro(2, 2, Natureza.VENDA, B, "100"));
        decimal("0", r.estadoFinal().quantidadeConsolidada());
        decimal("100", r.estadoFinal().saldo(A));
        decimal("-100", r.estadoFinal().saldo(B));
        assertNotNull(r.estadoFinal().primeirosNegativos().get(B));
    }
    @Test void saldoZeroSaudavelMantemCheckpointSemFabricarNegativo() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "10"), financeiro(2, 2, Natureza.VENDA, A, "10"));
        decimal("0", r.estadoFinal().saldo(A));
        assertTrue(r.estadoFinal().saldos().containsKey(A));
        assertTrue(r.estadoFinal().primeirosNegativos().isEmpty());
    }
    @Test void grupoNuloTambemPodeTerNegativoIntermediario() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "100"), financeiro(2, 2, Natureza.VENDA, SEM, "10"),
                financeiro(3, 3, Natureza.COMPRA, SEM, "20"));
        decimal("10", r.estadoFinal().saldo(SEM));
        decimal("-10", r.estadoFinal().primeirosNegativos().get(SEM).saldoAposEvento());
    }
    @Test void registraNegativoCausadoPorCustodiaSemDecidirAceitacao() {
        var transferencia = custodia(1, 0, 1, 2, A, B, "1", false);
        var r = replay(transferencia, financeiro(1, 1, Natureza.COMPRA, A, "2"));
        decimal("1", r.estadoFinal().saldo(A));
        decimal("1", r.estadoFinal().saldo(B));
        var evidencia = r.estadoFinal().primeirosNegativos().get(A);
        assertEquals(Dominio.CUSTODIA, evidencia.identidade().dominio());
        assertEquals(transferencia.posicao(), evidencia.posicao());
        decimal("-1", evidencia.saldoAposEvento());
    }
    @Test void replayNaoExecutaValidacaoConsolidadaDeNovaVenda() {
        var r = replay(financeiro(1, 1, Natureza.VENDA, A, "3"));
        decimal("-3", r.estadoFinal().quantidadeConsolidada());
        assertNotNull(r.estadoFinal().primeirosNegativos().get(A));
    }
    @Test void tabelaCronologicaComDeficitLegadoECheckpointDeCustodia() {
        var r = replay(financeiro(101, 1, Natureza.COMPRA, A, "100"), custodia(1, 1, 1, 2, A, C, "5", false),
                financeiro(102, 2, Natureza.COMPRA, B, "10"), financeiro(103, 3, Natureza.VENDA, B, "20"),
                financeiro(104, 4, Natureza.COMPRA, B, "10"));
        String[][] esperados = {{"100", "0", "0", "100"}, {"95", "0", "5", "100"},
                {"95", "10", "5", "110"}, {"95", "-10", "5", "90"}, {"95", "0", "5", "100"}};
        for (int i = 0; i < esperados.length; i++) {
            var estado = r.checkpoints().get(i).estado();
            decimal(esperados[i][0], estado.saldo(A));
            decimal(esperados[i][1], estado.saldo(B));
            decimal(esperados[i][2], estado.saldo(C));
            decimal(esperados[i][3], estado.quantidadeConsolidada());
        }
        assertEquals(103L, r.estadoFinal().primeirosNegativos().get(B).identidade().idPersistido());
    }
    @ParameterizedTest @EnumSource(Mercado.class)
    void quantidadeInteiraExataNosDoisMercados(Mercado mercado) {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "9999999999999.000000", mercado),
                financeiro(2, 2, Natureza.VENDA, A, "1.000000", mercado));
        decimal("9999999999998", r.estadoFinal().quantidadeConsolidada());
    }
    @Test void fracionariosEuaPreservamUltimaCasa() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "0.123456"), financeiro(2, 2, Natureza.COMPRA, A, "0.000001"),
                custodia(1, 2, 1, 2, A, B, "0.000001", false));
        decimal("0.123456", r.estadoFinal().saldo(A));
        decimal("0.000001", r.estadoFinal().saldo(B));
        decimal("0.123457", r.estadoFinal().quantidadeConsolidada());
    }
    @Test void acumuladoresDerivadosNaoRecebemLimiteNumeric19() {
        var r = replay(financeiro(1, 1, Natureza.COMPRA, A, "9999999999999.999999"),
                financeiro(2, 2, Natureza.COMPRA, A, "9999999999999.999999"));
        decimal("19999999999999.999998", r.estadoFinal().saldo(A));
        assertTrue(r.estadoFinal().quantidadeConsolidada().precision() > 19);
    }
    @Test void candidatoTemporarioEhPreservadoNaEvidencia() {
        var identidade = Identidade.temporaria(Dominio.FINANCEIRO,
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var candidato = new EventoQuantitativo(identidade, 1, 2, Mercado.EUA, Natureza.VENDA,
                BigDecimal.ONE, PosicaoCronologicaQuantitativa.financeira(DATA, 1), SEM, null, false);
        var r = replay(candidato);
        assertEquals(identidade, r.estadoFinal().primeirosNegativos().get(SEM).identidade());
        assertNull(r.estadoFinal().primeirosNegativos().get(SEM).identidade().idPersistido());
    }
    @Test void entradaVaziaTemEstadoZeroEGrupoDesconhecidoValeZero() {
        var r = replay();
        assertTrue(r.checkpoints().isEmpty());
        assertTrue(r.estadoFinal().saldos().isEmpty());
        assertTrue(r.estadoFinal().primeirosNegativos().isEmpty());
        decimal("0", r.estadoFinal().saldo(SEM));
        decimal("0", r.estadoFinal().quantidadeConsolidada());
        assertTrue(r.estadoFinal().reconciliada());
    }
    @Test void resultadosEEntradasSaoImutaveisSemCompartilharMapasMutaveis() {
        var entrada = new ArrayList<>(List.of(financeiro(1, 1, Natureza.COMPRA, A, "10"),
                custodia(1, 1, 1, 2, A, B, "5", false)));
        var antes = List.copyOf(entrada);
        var r = motor.reproduzir(entrada);
        assertEquals(antes, entrada);
        assertThrows(UnsupportedOperationException.class, () -> r.checkpoints().clear());
        assertThrows(UnsupportedOperationException.class, () -> r.estadoFinal().saldos().put(A, BigDecimal.ZERO));
        assertThrows(UnsupportedOperationException.class, () -> r.estadoFinal().primeirosNegativos().clear());
        decimal("10", r.checkpoints().get(0).estado().saldo(A));
        assertFalse(r.checkpoints().get(0).estado().saldos().containsKey(B));
        entrada.clear();
        assertEquals(2, r.checkpoints().size());
        var mapa = new LinkedHashMap<Grupo, BigDecimal>();
        mapa.put(A, BigDecimal.TEN);
        var estado = new Estado(mapa, BigDecimal.TEN, Map.of());
        mapa.clear();
        decimal("10", estado.saldo(A));
    }
    @Test void divergenciaDeReconciliacaoEhExplicitaESemCorrecaoSilenciosa() {
        var estado = new Estado(Map.of(A, new BigDecimal("4.00"), B, new BigDecimal("-1")),
                new BigDecimal("5"), Map.of());
        decimal("3", estado.somaSaldos());
        decimal("-2", estado.diferencaReconciliacao());
        assertFalse(estado.reconciliada());
        var escalas = new Estado(Map.of(A, new BigDecimal("1.000000")), BigDecimal.ONE, Map.of());
        assertTrue(escalas.reconciliada());
    }
    @Test void chamadasIndependentesNaoReutilizamEvidenciasOuSaldos() {
        var anterior = replay(financeiro(1, 1, Natureza.VENDA, A, "1"));
        var atual = replay(financeiro(1, 1, Natureza.COMPRA, A, "1"));
        assertFalse(anterior.estadoFinal().primeirosNegativos().isEmpty());
        assertTrue(atual.estadoFinal().primeirosNegativos().isEmpty());
        decimal("-1", anterior.estadoFinal().saldo(A));
    }
    @Test void rejeitaCronologiaInvalidaEmVezDeReordenarPorId() {
        var f1 = financeiro(99, 1, Natureza.COMPRA, A, "10");
        var f2 = financeiro(1, 2, Natureza.VENDA, A, "1");
        assertThrows(IllegalArgumentException.class, () -> motor.reproduzir(List.of(f2, f1)));
        assertThrows(IllegalArgumentException.class, () -> motor.reproduzir(List.of(f1, financeiro(3, 1, Natureza.COMPRA, A, "1"))));
        assertThrows(IllegalArgumentException.class, () -> motor.reproduzir(List.of(f1, financeiro(99, 2, Natureza.COMPRA, A, "1"))));
    }
    @Test void rejeitaMisturaDeContextoENulos() {
        var f = financeiro(1, 1, Natureza.COMPRA, A, "1");
        for (long[] contexto : new long[][]{{2, 2}, {1, 3}}) {
            var outro = new EventoQuantitativo(Identidade.persistida(Dominio.FINANCEIRO, 2), contexto[0], contexto[1],
                    Mercado.EUA, Natureza.COMPRA, BigDecimal.ONE, PosicaoCronologicaQuantitativa.financeira(DATA, 2), A, null, false);
            assertThrows(IllegalArgumentException.class, () -> motor.reproduzir(List.of(f, outro)));
        }
        assertThrows(IllegalArgumentException.class, () -> motor.reproduzir(null));
        assertThrows(IllegalArgumentException.class, () -> motor.reproduzir(java.util.Arrays.asList(f, null)));
    }
    @Test void apiNaoRecebePrecoCustoLucroEntidadesOuDependenciasDeInfraestrutura() {
        assertEquals(0, MotorReplayQuantitativo.class.getDeclaredFields().length);
        assertEquals(0, MotorReplayQuantitativo.class.getAnnotations().length);
        for (var componente : EventoQuantitativo.class.getRecordComponents()) {
            assertFalse(List.of("preco", "precoMedio", "custo", "lucro", "rentabilidade").contains(componente.getName()));
        }
        for (Class<?> tipo : new Class<?>[]{PrimeiroNegativo.class, Estado.class, Checkpoint.class, Resultado.class}) {
            assertTrue(tipo.isRecord());
            assertEquals(0, tipo.getAnnotations().length);
        }
    }
}
