package com.projeto.services;

import com.projeto.entities.*;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraDistribuicaoCorretoraTest {
    private final CalculadoraPosicao financeiro = new CalculadoraPosicao();
    private final CalculadoraDistribuicaoCorretora calculadora = new CalculadoraDistribuicaoCorretora(financeiro);
    private final Carteira carteira = carteira(1L);
    private final Acao acao = acao(1L, "PETR4", Mercado.BRASIL);
    private final Corretora a = corretora(1L, "A");
    private final Corretora b = corretora(2L, "B");
    private long id;

    @Test void umaCorretoraEEstadoSaudavelImutavel() {
        var p = posicao(List.of(op(a, TipoOperacao.COMPRA, "100"), op(a, TipoOperacao.VENDA, "40")));
        decimal("60", p.quantidadeConsolidada());
        decimal("60", p.corretoras().get(0).quantidadeLiquida());
        assertTrue(p.reconciliada()); assertNull(p.diagnosticoReconciliacao());
        var d = p.diagnosticosCorretoras().get(0);
        assertFalse(d.saldoNegativoAtual()); assertFalse(d.possuiDeficitHistorico());
        assertNull(d.primeiroDeficit());
        assertThrows(UnsupportedOperationException.class, () -> p.corretoras().clear());
        assertThrows(UnsupportedOperationException.class, () -> p.diagnosticosCorretoras().clear());
    }

    @Test void variasCorretorasECategoriaNullSemprePorUltimo() {
        var p = posicao(List.of(op(b, TipoOperacao.COMPRA, "50"), op(a, TipoOperacao.COMPRA, "100"),
                op(a, TipoOperacao.VENDA, "20"), op(null, TipoOperacao.COMPRA, "5")));
        assertEquals(java.util.Arrays.asList(1L, 2L, null), p.corretoras().stream().map(x -> x.corretoraId()).toList());
        assertEquals("Sem corretora informada", p.corretoras().get(2).nome());
        decimal("80", p.corretoras().get(0).quantidadeLiquida());
        decimal("135", p.quantidadeAtribuidaTotal());
    }

    @Test void negativoAtualNaoEhOcultadoNemImpedeReconciliacao() {
        var p = posicao(List.of(op(a, TipoOperacao.COMPRA, "100"), op(b, TipoOperacao.VENDA, "40")));
        decimal("-40", p.corretoras().get(1).quantidadeLiquida());
        decimal("60", p.quantidadeAtribuidaTotal());
        assertTrue(p.reconciliada());
        var d = p.diagnosticosCorretoras().get(1);
        assertTrue(d.saldoNegativoAtual()); assertTrue(d.possuiDeficitHistorico());
        assertEquals(2L, d.primeiroDeficit().operacaoId());
        decimal("-40", d.primeiroDeficit().saldoAposOperacao());
    }

    @Test void primeiraOcorrenciaNaoEhPiorDeficitENaoSomeAoRegularizar() {
        var primeira = op(b, TipoOperacao.VENDA, "10");
        var base = op(a, TipoOperacao.COMPRA, "100");
        ReflectionTestUtils.setField(base, "ordemNoDia", 1);
        ReflectionTestUtils.setField(primeira, "ordemNoDia", 2);
        var p = posicao(List.of(base, primeira, op(b, TipoOperacao.VENDA, "20"),
                op(b, TipoOperacao.COMPRA, "40")));
        var d = p.diagnosticosCorretoras().get(1);
        decimal("10", d.quantidadeFinal()); assertFalse(d.saldoNegativoAtual());
        assertTrue(d.possuiDeficitHistorico());
        assertEquals(primeira.getId(), d.primeiroDeficit().operacaoId());
        assertEquals(primeira.getDataOperacao(), d.primeiroDeficit().dataOperacao());
        assertEquals(2, d.primeiroDeficit().ordemNoDia());
        decimal("-10", d.primeiroDeficit().saldoAposOperacao());
    }

    @Test void zeroComDeficitMantemDiagnosticoSemLinha() {
        var p = posicao(List.of(op(a, TipoOperacao.COMPRA, "100"), op(b, TipoOperacao.VENDA, "10"),
                op(b, TipoOperacao.COMPRA, "10")));
        assertEquals(1, p.corretoras().size()); assertEquals(2, p.diagnosticosCorretoras().size());
        var d = p.diagnosticosCorretoras().get(1);
        decimal("0", d.quantidadeFinal()); assertFalse(d.saldoNegativoAtual());
        assertTrue(d.possuiDeficitHistorico()); assertNotNull(d.primeiroDeficit());
    }

    @Test void zeroSaudavelNaoCriaDeficit() {
        var p = posicao(List.of(op(a, TipoOperacao.COMPRA, "100"), op(b, TipoOperacao.COMPRA, "10"),
                op(b, TipoOperacao.VENDA, "10")));
        assertEquals(1, p.corretoras().size());
        assertFalse(p.diagnosticosCorretoras().get(1).possuiDeficitHistorico());
        assertNull(p.diagnosticosCorretoras().get(1).primeiroDeficit());
    }

    @Test void cicloPosteriorNaoResetaEvidencia() {
        var ops = List.of(op(a, TipoOperacao.COMPRA, "100"), op(b, TipoOperacao.VENDA, "10"),
                op(b, TipoOperacao.COMPRA, "10"), op(a, TipoOperacao.VENDA, "100"),
                op(b, TipoOperacao.COMPRA, "5"));
        var p = posicao(ops);
        var d = p.diagnosticosCorretoras().get(1);
        decimal("5", d.quantidadeFinal()); assertEquals(2L, d.primeiroDeficit().operacaoId());
    }

    @Test void consolidadoZeroNaoRetornaMesmoComDeficits() {
        var r = calculadora.calcular(1L, List.of(op(a, TipoOperacao.COMPRA, "10"), op(b, TipoOperacao.VENDA, "10")));
        assertTrue(r.posicoes().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> r.posicoes().clear());
        assertTrue(calculadora.calcular(1L, List.of()).posicoes().isEmpty());
    }

    @Test void retroativaOrdenaCopiaPorDataEOrdemNaoPorId() {
        var compra = op(a, TipoOperacao.COMPRA, "100");
        var venda = op(b, TipoOperacao.VENDA, "10");
        var regularizacao = op(b, TipoOperacao.COMPRA, "20");
        ReflectionTestUtils.setField(regularizacao, "dataOperacao", LocalDate.of(2026, 10, 3));
        ReflectionTestUtils.setField(compra, "id", 100L);
        var entrada = List.of(regularizacao, venda, compra);
        var p = posicao(entrada);
        assertEquals(venda.getId(), p.diagnosticosCorretoras().get(1).primeiroDeficit().operacaoId());
        assertSame(regularizacao, entrada.get(0)); assertEquals(100L, compra.getId());
    }

    @Test void categoriaNullPodeTerDeficitRegularizado() {
        var p = posicao(List.of(op(a, TipoOperacao.COMPRA, "100"), op(null, TipoOperacao.VENDA, "10"),
                op(null, TipoOperacao.COMPRA, "20")));
        var d = p.diagnosticosCorretoras().get(1);
        assertNull(d.corretoraId()); assertTrue(d.possuiDeficitHistorico());
        decimal("10", d.quantidadeFinal());
    }

    @ParameterizedTest @EnumSource(Mercado.class)
    void precisaoECalculosFinanceirosPreservados(Mercado mercado) {
        Acao ativo = acao(3L, "ATIVO", mercado);
        String q = mercado == Mercado.BRASIL ? "1" : "0.123456";
        var ops = List.of(op(ativo, carteira, a, TipoOperacao.COMPRA, q),
                op(ativo, carteira, b, TipoOperacao.COMPRA, mercado == Mercado.BRASIL ? "2" : "0.000001"));
        var antes = financeiro.reproduzir(ops);
        var p = calculadora.calcular(1L, ops).posicoes().get(0);
        decimal(mercado == Mercado.BRASIL ? "3" : "0.123457", p.quantidadeConsolidada());
        assertEquals(antes, financeiro.reproduzir(ops));
        assertEquals(ativo.getMoeda(), p.moeda());
    }

    @Test void derivadosNaoRecebemLimiteArtificial19Digitos() {
        String max = "9999999999999.999999";
        Acao us = acao(3L, "AAPL", Mercado.EUA);
        var p = calculadora.calcular(1L, List.of(op(us, carteira, a, TipoOperacao.COMPRA, max),
                op(us, carteira, b, TipoOperacao.VENDA, max), op(us, carteira, a, TipoOperacao.COMPRA, max)))
                .posicoes().get(0);
        decimal("19999999999999.999998", p.corretoras().get(0).quantidadeLiquida());
        decimal(max, p.quantidadeConsolidada()); assertTrue(p.reconciliada());
    }

    @Test void reconciliaPorValorIncluindoZeroENegativoESinalizaDivergencia() {
        var p = posicao(List.of(op(a, TipoOperacao.COMPRA, "100"), op(b, TipoOperacao.VENDA, "40"),
                op(null, TipoOperacao.COMPRA, "5"), op(corretora(3L, "C"), TipoOperacao.COMPRA, "2"),
                op(corretora(3L, "C"), TipoOperacao.VENDA, "2")));
        var igual = CalculadoraDistribuicaoCorretora.reconciliar(new BigDecimal("65.0"), p.diagnosticosCorretoras());
        assertTrue(igual.reconciliada()); assertNull(igual.diagnostico());
        var diferente = CalculadoraDistribuicaoCorretora.reconciliar(new BigDecimal("66"), p.diagnosticosCorretoras());
        assertFalse(diferente.reconciliada()); decimal("65", diferente.quantidadeAtribuidaTotal());
        assertEquals("DIVERGENCIA_QUANTIDADES", diferente.diagnostico().codigo());
        decimal("-40", p.diagnosticosCorretoras().get(1).quantidadeFinal());
    }

    @Test void isolamentoEntreCarteirasEAcoes() {
        var ops = List.of(op(a, TipoOperacao.COMPRA, "10"),
                op(acao, carteira(2L), a, TipoOperacao.COMPRA, "500"),
                op(acao(2L, "VALE3", Mercado.BRASIL), carteira, b, TipoOperacao.COMPRA, "20"));
        var r = calculadora.calcular(1L, ops);
        assertEquals(2, r.posicoes().size()); decimal("10", r.posicoes().get(0).quantidadeConsolidada());
        decimal("20", r.posicoes().get(1).quantidadeConsolidada());
        decimal("500", calculadora.calcular(2L, ops).posicoes().get(0).quantidadeConsolidada());
    }

    @Test void ordenacaoOrdinalEmpatesMercadosEIds() {
        var ops = new ArrayList<Operacao>();
        ops.add(op(null, TipoOperacao.COMPRA, "1"));
        for (var c : List.of(corretora(8L, "a"), corretora(7L, "A"), corretora(4L, "A"), corretora(9L, "\u00c1"))) {
            ops.add(op(c, TipoOperacao.COMPRA, "1"));
        }
        ops.add(op(acao(10L, "AAA", Mercado.EUA), carteira, a, TipoOperacao.COMPRA, "1"));
        ops.add(op(acao(2L, "PETR4", Mercado.BRASIL), carteira, a, TipoOperacao.COMPRA, "1"));
        var r = calculadora.calcular(1L, ops);
        assertEquals(List.of(1L, 2L, 10L), r.posicoes().stream().map(x -> x.acaoId()).toList());
        assertEquals(java.util.Arrays.asList(4L, 7L, 8L, 9L, null),
                r.posicoes().get(0).diagnosticosCorretoras().stream().map(x -> x.corretoraId()).toList());
    }

    @Test void nomesUsamTrimRazaoSocialEFallback() {
        var x = corretora(4L, "  Nome  ");
        var y = corretora(5L, " "); ReflectionTestUtils.setField(y, "razaoSocial", " Razao ");
        var z = corretora(6L, null); ReflectionTestUtils.setField(z, "razaoSocial", null);
        var p = posicao(List.of(op(x, TipoOperacao.COMPRA, "1"), op(y, TipoOperacao.COMPRA, "1"),
                op(z, TipoOperacao.COMPRA, "1")));
        assertEquals(List.of("Corretora #6", "Nome", "Razao"), p.corretoras().stream().map(g -> g.nome()).toList());
    }

    @Test void replayConsolidadoInvalidoNaoProduzDistribuicaoParcial() {
        var e = assertThrows(ApiException.class, () -> posicao(List.of(op(b, TipoOperacao.VENDA, "1"))));
        assertEquals(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE, e.getCode());
        var one = op(a, TipoOperacao.COMPRA, "1"); var two = op(b, TipoOperacao.COMPRA, "1");
        ReflectionTestUtils.setField(two, "ordemNoDia", one.getOrdemNoDia());
        assertThrows(ApiException.class, () -> posicao(List.of(one, two)));
        assertThrows(ApiException.class, () -> calculadora.calcular(1L, java.util.Arrays.asList((Operacao) null)));
    }

    @Test void quantidadeFracionariaBrasilContinuaInvalida() {
        assertThrows(ApiException.class, () -> posicao(List.of(op(a, TipoOperacao.COMPRA, "0.1"))));
    }

    @Test void distribuicaoNaoMudaPrecoMedioCustoRealizadoOuEntidades() {
        var compra = op(a, TipoOperacao.COMPRA, "100");
        var venda = op(b, TipoOperacao.VENDA, "40");
        ReflectionTestUtils.setField(venda, "precoUnitario", new BigDecimal("15.000000"));
        ReflectionTestUtils.setField(venda, "valorTotal", new BigDecimal("600.000000000000"));
        var ops = List.of(compra, venda);
        var antes = financeiro.reproduzir(ops);
        decimal("10", antes.posicao().precoMedio()); decimal("600", antes.posicao().custoPosicao());
        decimal("200", antes.resultadoRealizado());
        BigDecimal cotacao = acao.getCotacaoAtual();
        var p = posicao(ops);
        assertEquals(antes, financeiro.reproduzir(ops));
        decimal("60", p.quantidadeConsolidada());
        assertSame(cotacao, acao.getCotacaoAtual());
        assertSame(b, venda.getCorretora()); decimal("15", venda.getPrecoUnitario());
    }

    private CalculadoraDistribuicaoCorretora.Posicao posicao(List<Operacao> ops) {
        return calculadora.calcular(1L, ops).posicoes().get(0);
    }
    private void decimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
    private Operacao op(Corretora c, TipoOperacao tipo, String quantidade) {
        return op(acao, carteira, c, tipo, quantidade);
    }
    private Operacao op(Acao ativo, Carteira portfolio, Corretora c, TipoOperacao tipo, String quantidade) {
        BigDecimal q = new BigDecimal(quantidade).setScale(6, RoundingMode.UNNECESSARY);
        BigDecimal preco = new BigDecimal("10.000000");
        Operacao op = new Operacao(portfolio, ativo, c, tipo, q, preco, LocalDate.of(2026, 10, 2),
                (int) ++id, q.multiply(preco));
        ReflectionTestUtils.setField(op, "id", id); return op;
    }
    private Carteira carteira(Long id) {
        var c = new Carteira("Carteira", OffsetDateTime.parse("2026-10-02T00:00:00Z"));
        ReflectionTestUtils.setField(c, "id", id); return c;
    }
    private Acao acao(Long id, String ticker, Mercado mercado) {
        var a = new Acao(ticker, "Empresa", mercado, mercado == Mercado.BRASIL ? Moeda.BRL : Moeda.USD,
                BigDecimal.TEN, OffsetDateTime.parse("2026-10-02T00:00:00Z"));
        ReflectionTestUtils.setField(a, "id", id); return a;
    }
    private Corretora corretora(Long id, String nome) {
        var c = new Corretora("00000000000000", "Razao", nome, null, null, "00000000", "Rua", null,
                null, "Bairro", "Cidade", "SP", "ATIVA", OffsetDateTime.parse("2026-10-02T00:00:00Z"));
        ReflectionTestUtils.setField(c, "id", id); return c;
    }
}
