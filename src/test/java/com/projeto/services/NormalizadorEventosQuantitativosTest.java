package com.projeto.services;

import com.projeto.entities.Mercado;
import com.projeto.entities.NaturezaMovimentacaoCustodia;
import com.projeto.entities.PosicaoRacionalCustodia;
import com.projeto.entities.TipoOperacao;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import static com.projeto.services.EventoQuantitativo.*;
import static com.projeto.services.NormalizadorEventosQuantitativos.*;
import static org.junit.jupiter.api.Assertions.*;

class NormalizadorEventosQuantitativosTest {
    private static final LocalDate DATA = LocalDate.of(2026, 1, 1);
    private final NormalizadorEventosQuantitativos normalizador = new NormalizadorEventosQuantitativos();
    private Identidade id(Dominio dominio, long id) { return Identidade.persistida(dominio, id); }
    private Financeiro financeiro(long id, int ordem) {
        return new Financeiro(id(Dominio.FINANCEIRO, id), 1, 2, Mercado.EUA, TipoOperacao.COMPRA,
                new BigDecimal("10.123456"), DATA, ordem, Grupo.semCorretora());
    }
    private Custodia custodia(long id, int ancora, long n, long d) {
        return custodia(id(Dominio.CUSTODIA, id), ancora, n, d, null);
    }
    private Custodia custodia(Identidade identidade, int ancora, long n, long d, Long estorno) {
        return new Custodia(identidade, 1, 2, Mercado.EUA, NaturezaMovimentacaoCustodia.TRANSFERENCIA,
                new BigDecimal("2.000001"), DATA, ancora,
                PosicaoRacionalCustodia.criar(BigInteger.valueOf(n), BigInteger.valueOf(d)),
                Grupo.corretora(3), Grupo.corretora(4), new VinculoEstorno(identidade, estorno));
    }
    private List<EventoQuantitativo> normalizar(List<Financeiro> fs, List<Custodia> cs) {
        return normalizador.normalizar(1, 2, fs, cs);
    }
    @Test void entradasEmbaralhadasProduzemCronologiaSemOrdenarPorId() {
        var f1 = financeiro(99, 1);
        var f2 = financeiro(1, 2);
        var c0 = custodia(30, 0, 1, 2);
        var c1 = custodia(20, 1, 2, 3);
        var c2 = custodia(10, 1, 10, 11);
        assertEquals(List.of(c0.identidade(), f1.identidade(), c1.identidade(), c2.identidade(), f2.identidade()),
                normalizar(List.of(f2, f1), List.of(c2, c0, c1)).stream().map(EventoQuantitativo::identidade).toList());
    }
    @Test void todasAsPermutacoesDasFontesProduzemMesmoResultado() {
        var fs = List.of(financeiro(9, 1), financeiro(8, 2), financeiro(7, 3));
        var cs = List.of(custodia(9, 0, 1, 2), custodia(8, 1, 2, 3), custodia(7, 1, 10, 11));
        var esperado = normalizar(fs, cs);
        for (var fp : permutacoes(fs)) for (var cp : permutacoes(cs)) assertEquals(esperado, normalizar(fp, cp));
    }
    private static <T> List<List<T>> permutacoes(List<T> fonte) {
        if (fonte.isEmpty()) return List.of(List.of());
        List<List<T>> resultado = new ArrayList<>();
        for (int i = 0; i < fonte.size(); i++) {
            var resto = new ArrayList<>(fonte);
            T atual = resto.remove(i);
            for (var p : permutacoes(resto)) {
                var completa = new ArrayList<T>();
                completa.add(atual);
                completa.addAll(p);
                resultado.add(completa);
            }
        }
        return resultado;
    }
    @Test void idsIguaisEntreDominiosSaoAceitos() {
        var eventos = normalizar(List.of(financeiro(1, 1)), List.of(custodia(1, 1, 1, 2)));
        assertEquals(2, eventos.size());
        assertNotEquals(eventos.get(0).identidade(), eventos.get(1).identidade());
    }
    @Test void candidatasTemporariasFinanceiraECustodiaSaoPreservadas() {
        var token = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var fi = Identidade.temporaria(Dominio.FINANCEIRO, token);
        var ci = Identidade.temporaria(Dominio.CUSTODIA, token);
        var f = new Financeiro(fi, 1, 2, Mercado.EUA, TipoOperacao.VENDA, BigDecimal.ONE, DATA, 2, Grupo.corretora(3));
        var eventos = normalizar(List.of(f), List.of(custodia(ci, 1, 1, 2, null)));
        assertEquals(ci, eventos.get(0).identidade());
        assertEquals(fi, eventos.get(1).identidade());
        assertTrue(eventos.stream().allMatch(e -> e.identidade().idPersistido() == null));
    }
    @Test void atribuicaoPreservaGrupoSemCorretoraSemAlterarFinanceiro() {
        var i = id(Dominio.CUSTODIA, 1);
        var c = new Custodia(i, 1, 2, Mercado.EUA, NaturezaMovimentacaoCustodia.ATRIBUICAO,
                BigDecimal.ONE, DATA, 1, PosicaoRacionalCustodia.deTexto("1", "2"),
                Grupo.semCorretora(), Grupo.corretora(4), VinculoEstorno.ausenciaVerificada(i));
        var eventos = normalizar(List.of(financeiro(1, 1)), List.of(c));
        assertEquals(Grupo.semCorretora(), eventos.get(0).grupoOrigem());
        assertEquals(Grupo.semCorretora(), eventos.get(1).grupoOrigem());
        assertEquals(Grupo.corretora(4), eventos.get(1).grupoDestino());
        assertEquals(Natureza.ATRIBUICAO, eventos.get(1).natureza());
    }
    @Test void originalEstornadaPermaneceCheckpointComQuantidadeEPosicaoOriginais() {
        var c = custodia(id(Dominio.CUSTODIA, 20), 1, 2, 3, 70L);
        var eventos = normalizar(List.of(financeiro(1, 1)), List.of(c));
        assertEquals(2, eventos.size());
        var original = eventos.get(1);
        assertTrue(original.neutralizado());
        assertEquals(c.identidade(), original.identidade());
        assertEquals(c.quantidade(), original.quantidade());
        assertEquals(c.dataEfetiva(), original.posicao().dataEfetiva());
        assertEquals(c.fracao(), original.posicao().fracao().paraPersistivel());
        assertEquals(Grupo.corretora(3), original.grupoOrigem());
        assertEquals(Grupo.corretora(4), original.grupoDestino());
    }
    @Test void ausenciaVerificadaMantemOriginalAtiva() {
        assertFalse(normalizar(List.of(), List.of(custodia(1, 0, 1, 2))).get(0).neutralizado());
    }
    @Test void originalEstornadaAindaReservaPosicao() {
        assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(), List.of(
                custodia(id(Dominio.CUSTODIA, 1), 0, 1, 2, 10L), custodia(2, 0, 1, 2))));
    }
    @Test void rejeitaIdentidadeFinanceiraDuplicadaMesmoEmPosicoesDiferentes() {
        assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(financeiro(1, 1), financeiro(1, 2)), List.of()));
    }
    @Test void rejeitaIdentidadeCustodiaDuplicadaMesmoEmPosicoesDiferentes() {
        assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(), List.of(custodia(1, 0, 1, 2), custodia(1, 0, 2, 3))));
    }
    @Test void rejeitaColisaoFinanceiraSemMascararComId() {
        assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(financeiro(1, 1), financeiro(2, 1)), List.of()));
    }
    @Test void rejeitaColisaoRacionalMatematicamenteEquivalente() {
        assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(), List.of(custodia(1, 0, 1, 2), custodia(2, 0, 2, 4))));
    }
    @Test void rejeitaIdentidadeTemporariaDuplicada() {
        var i = Identidade.temporaria(Dominio.CUSTODIA, UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(), List.of(custodia(i, 0, 1, 2, null), custodia(i, 0, 2, 3, null))));
    }
    @Test void rejeitaVinculoAusenteOuDeOutraOriginal() {
        var c = custodia(1, 0, 1, 2);
        for (VinculoEstorno v : new VinculoEstorno[]{null, VinculoEstorno.ausenciaVerificada(id(Dominio.CUSTODIA, 2))}) {
            var invalida = new Custodia(c.identidade(), 1, 2, c.mercado(), c.natureza(), c.quantidade(), DATA,
                    0, c.fracao(), c.origem(), c.destino(), v);
            assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(), List.of(invalida)));
        }
    }
    @Test void rejeitaMesmoEstornoEmDuasOriginais() {
        assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(), List.of(
                custodia(id(Dominio.CUSTODIA, 1), 0, 1, 2, 10L), custodia(id(Dominio.CUSTODIA, 2), 0, 2, 3, 10L))));
    }
    @Test void candidataNaoPodeAfirmarEstornoPersistido() {
        assertThrows(IllegalArgumentException.class, () -> VinculoEstorno.presente(
                Identidade.temporaria(Dominio.CUSTODIA, UUID.randomUUID()), 1));
        assertThrows(IllegalArgumentException.class, () -> VinculoEstorno.presente(id(Dominio.CUSTODIA, 1), 0));
    }
    @Test void rejeitaMisturaDeCarteiraAcaoOuMercado() {
        var base = financeiro(1, 1);
        for (var f : List.of(
                new Financeiro(base.identidade(), 2, 2, Mercado.EUA, base.tipo(), base.quantidade(), DATA, 1, base.grupo()),
                new Financeiro(base.identidade(), 1, 3, Mercado.EUA, base.tipo(), base.quantidade(), DATA, 1, base.grupo()),
                new Financeiro(id(Dominio.FINANCEIRO, 2), 1, 2, Mercado.BRASIL, base.tipo(), BigDecimal.ONE, DATA, 2, base.grupo()))) {
            assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(f), List.of(custodia(1, 0, 1, 2))));
        }
    }
    @Test void rejeitaDirecoesInvalidasSemInferirCorretora() {
        var c = custodia(1, 0, 1, 2);
        for (var origem : List.of(Grupo.semCorretora(), c.destino())) {
            var invalida = new Custodia(c.identidade(), 1, 2, c.mercado(), c.natureza(), c.quantidade(), DATA,
                    0, c.fracao(), origem, c.destino(), c.vinculo());
            assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(), List.of(invalida)));
        }
    }
    @Test void naoCalculaSaldoNemRejeitaVendaIsoladaComoDeficit() {
        var f = new Financeiro(id(Dominio.FINANCEIRO, 1), 1, 2, Mercado.EUA, TipoOperacao.VENDA,
                BigDecimal.ONE, DATA, 1, Grupo.semCorretora());
        assertEquals(Natureza.VENDA, normalizar(List.of(f), List.of()).get(0).natureza());
    }
    @Test void preservaEntradasEDevolveListaImutavel() {
        var fs = new ArrayList<>(List.of(financeiro(2, 2), financeiro(1, 1)));
        var cs = new ArrayList<>(List.of(custodia(2, 1, 10, 11), custodia(1, 1, 2, 3)));
        var antesF = List.copyOf(fs);
        var antesC = List.copyOf(cs);
        var saida = normalizar(fs, cs);
        assertEquals(antesF, fs);
        assertEquals(antesC, cs);
        assertThrows(UnsupportedOperationException.class, () -> saida.clear());
        Collections.shuffle(fs, new Random(1));
        cs.clear();
        assertEquals(4, saida.size());
        assertEquals(antesF.get(1).quantidade(), saida.get(0).quantidade());
        assertEquals(1, antesF.get(1).ordemNoDia());
    }
    @Test void fontesVaziasSaoValidasMasFontesDesconhecidasNao() {
        assertEquals(List.of(), normalizar(List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> normalizar(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> normalizar(java.util.Arrays.asList((Financeiro) null), List.of()));
        assertThrows(IllegalArgumentException.class, () -> normalizar(List.of(), java.util.Arrays.asList((Custodia) null)));
    }
}
