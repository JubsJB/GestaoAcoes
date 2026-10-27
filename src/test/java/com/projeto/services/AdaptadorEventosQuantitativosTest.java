package com.projeto.services;

import com.projeto.entities.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static com.projeto.services.EventoQuantitativo.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Fixtures representam entidades já hidratadas; nenhum contexto, sessão ou banco é criado. */
class AdaptadorEventosQuantitativosTest {
    private static final LocalDate DATA = LocalDate.of(2025, 1, 2);
    private static final OffsetDateTime REGISTRO = OffsetDateTime.parse("2025-03-01T10:11:12.123456Z");
    private final AdaptadorEventosQuantitativos adaptador = new AdaptadorEventosQuantitativos();
    private final Carteira carteira = carteira(1);
    private final Acao acao = acao(2, Mercado.EUA);
    private final Corretora a = corretora(3);
    private final Corretora b = corretora(4);

    @ParameterizedTest @ValueSource(strings = {"COMPRA", "VENDA"})
    void converteFinanceiroSemMudarCamposOuFabricarIdentidade(TipoOperacao tipo) {
        var op = operacao(7, tipo, a, "12.345678", 9);
        var fonte = adaptar(List.of(op), List.of(), List.of()).financeiros().get(0);
        assertEquals(Identidade.persistida(Dominio.FINANCEIRO, 7), fonte.identidade());
        assertEquals(1, fonte.carteiraId()); assertEquals(2, fonte.acaoId());
        assertEquals(Mercado.EUA, fonte.mercado()); assertEquals(tipo, fonte.tipo());
        assertEquals(op.getQuantidade(), fonte.quantidade()); assertEquals(DATA, fonte.dataOperacao());
        assertEquals(9, fonte.ordemNoDia()); assertEquals(Grupo.corretora(3), fonte.grupo());
        assertSame(a, op.getCorretora()); assertEquals(9, op.getOrdemNoDia());
    }

    @ParameterizedTest @ValueSource(strings = {"TRANSFERENCIA", "ATRIBUICAO"})
    void converteCustodiaERepresentaExplicitamenteGrupoSemCorretora(NaturezaMovimentacaoCustodia natureza) {
        var m = movimento(7, natureza, natureza == NaturezaMovimentacaoCustodia.ATRIBUICAO ? null : a, "2", "3", 0);
        var fonte = adaptar(List.of(), List.of(m), List.of(ausencia(m))).custodias().get(0);
        assertEquals(Identidade.persistida(Dominio.CUSTODIA, 7), fonte.identidade());
        assertEquals(natureza, fonte.natureza()); assertEquals(m.getQuantidade(), fonte.quantidade());
        assertEquals(DATA, fonte.dataEfetiva()); assertEquals(0, fonte.ancoraFinanceira());
        assertEquals("2/3", fonte.fracao().toString()); assertEquals(Grupo.corretora(4), fonte.destino());
        assertEquals(natureza == NaturezaMovimentacaoCustodia.ATRIBUICAO ? Grupo.semCorretora() : Grupo.corretora(3), fonte.origem());
        assertNull(fonte.vinculo().estornoId()); assertFalse(eventos(List.of(), List.of(m), List.of(ausencia(m))).get(0).neutralizado());
    }

    @Test void financeiraSemCorretoraNaoGeraIdSentinela() {
        var op = operacao(1, TipoOperacao.COMPRA, null, "1", 1);
        assertEquals(Grupo.semCorretora(), adaptar(List.of(op), List.of(), List.of()).financeiros().get(0).grupo());
    }

    @Test void estornoNeutralizaOriginalNaPosicaoEfetivaEPreservaCheckpoint() {
        var original = transferencia(7); var estorno = estorno(8, original);
        var vinculo = VinculoEstornoConsultado.vinculoPresente(7L, estorno);
        var ativo = eventos(List.of(), List.of(original), List.of(ausencia(original))).get(0);
        var neutralizado = eventos(List.of(), List.of(original), List.of(vinculo)).get(0);
        assertTrue(neutralizado.neutralizado()); assertEquals(ativo.identidade(), neutralizado.identidade());
        assertEquals(ativo.posicao(), neutralizado.posicao()); assertEquals(ativo.quantidade(), neutralizado.quantidade());
        var replay = new MotorReplayQuantitativo().reproduzir(List.of(neutralizado));
        assertEquals(1, replay.checkpoints().size()); assertEquals(0, replay.estadoFinal().somaSaldos().signum());
        assertEquals(REGISTRO, original.getDataRegistro()); assertSame(original, estorno.getMovimentacaoOriginal());
    }

    @Test void aceitaOutraInstanciaCarregadaDaMesmaOriginalCoerente() {
        var original = transferencia(7); var copia = transferencia(7);
        assertTrue(eventos(List.of(), List.of(original), List.of(VinculoEstornoConsultado.vinculoPresente(7L, estorno(8, copia)))).get(0).neutralizado());
    }

    @Test void rejeitaDuasVersoesDivergentesDaMesmaOriginal() {
        var original = transferencia(7); var copia = transferencia(7); campo(copia, "quantidade", new BigDecimal("2.000000"));
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(original),
                List.of(VinculoEstornoConsultado.vinculoPresente(7L, estorno(8, copia)))));
    }

    @Test void rejeitaVinculoDuplicadoInclusiveAusenciaEPresenca() {
        var m = transferencia(7);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(m), List.of(ausencia(m), ausencia(m))));
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(m),
                List.of(ausencia(m), VinculoEstornoConsultado.vinculoPresente(7L, estorno(8, m)))));
    }

    @Test void rejeitaIdDeEstornoReutilizadoEmOriginaisDistintas() {
        var m = transferencia(7); var n = movimento(9, NaturezaMovimentacaoCustodia.TRANSFERENCIA, a, "2", "3", 0);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(m, n), List.of(
                VinculoEstornoConsultado.vinculoPresente(7L, estorno(8, m)), VinculoEstornoConsultado.vinculoPresente(9L, estorno(8, n)))));
    }

    @Test void rejeitaEstornoDeOutraOriginalEVinculoExtra() {
        var m = transferencia(7); var n = transferencia(9);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(m),
                List.of(VinculoEstornoConsultado.vinculoPresente(7L, estorno(8, n)))));
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(m), List.of(ausencia(m), ausencia(n))));
    }

    @Test void ausenciaNaoConsultadaNaoEquivaleAOriginalAtiva() {
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(transferencia(7)), List.of()));
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(transferencia(7)), null));
    }

    @ParameterizedTest @CsvSource({"financeiro,carteira", "financeiro,acao", "custodia,carteira", "custodia,acao", "estorno,carteira", "estorno,acao"})
    void rejeitaReferenciaForaDoContexto(String tipo, String referencia) {
        var op = operacao(1, TipoOperacao.COMPRA, a, "1", 1); var m = transferencia(7); var outra = transferencia(7);
        Object alvo = tipo.equals("financeiro") ? op : tipo.equals("custodia") ? m : outra;
        campo(alvo, referencia, referencia.equals("carteira") ? carteira(99) : acao(99, Mercado.EUA));
        var vinculo = tipo.equals("estorno") ? VinculoEstornoConsultado.vinculoPresente(7L, estorno(8, outra)) : ausencia(m);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(op), List.of(m), List.of(vinculo)));
    }

    @Test void rejeitaOrigemObrigatoriaAusenteEDestinoInvalido() {
        var m = transferencia(7); campo(m, "corretoraOrigem", null);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(m), List.of(ausencia(m))));
        final var invalida = transferencia(7); campo(invalida, "corretoraDestino", corretora(0));
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(invalida), List.of(ausencia(invalida))));
    }

    @ParameterizedTest @CsvSource({"2,4", "01,2", "0,1", "1,1", "1,0", "١,2"})
    void rejeitaFracaoPersistidaSemCorrigir(String n, String d) {
        var m = transferencia(7); campo(m, "posicaoNumerador", n); campo(m, "posicaoDenominador", d);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(m), List.of(ausencia(m))));
        assertEquals(n, ReflectionTestUtils.getField(m, "posicaoNumerador"));
    }

    @Test void preservaFracaoDe512DigitosERejeita513() {
        var d = java.math.BigInteger.TEN.pow(512).subtract(java.math.BigInteger.ONE).toString();
        var m = movimento(7, NaturezaMovimentacaoCustodia.TRANSFERENCIA, a, "1", d, 17);
        var c = adaptar(List.of(), List.of(m), List.of(ausencia(m))).custodias().get(0);
        assertEquals(d, c.fracao().getDenominadorTexto()); assertEquals(17, c.ancoraFinanceira());
        campo(m, "posicaoDenominador", "1" + "0".repeat(512));
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(m), List.of(ausencia(m))));
    }

    @Test void mesmosIdsEmDominiosDiferentesNaoColidem() {
        var m = movimento(7, NaturezaMovimentacaoCustodia.TRANSFERENCIA, a, "1", "2", 1);
        var es = eventos(List.of(operacao(7, TipoOperacao.COMPRA, a, "10", 1)), List.of(m), List.of(ausencia(m)));
        assertEquals(2, es.size()); assertNotEquals(es.get(0).identidade(), es.get(1).identidade());
        assertEquals(Dominio.FINANCEIRO, es.get(0).identidade().dominio());
    }

    @ParameterizedTest @CsvSource({"BRASIL,12.000000", "EUA,0.000001", "EUA,9999999999999.999999"})
    void preservaPrecisaoDeQuantidade(Mercado mercado, String quantidade) {
        campo(acao, "mercado", mercado); var m = transferencia(7); campo(m, "quantidade", new BigDecimal(quantidade));
        var op = operacao(1, TipoOperacao.COMPRA, a, quantidade, 1);
        var fontes = adaptar(List.of(op), List.of(m), List.of(ausencia(m)));
        assertEquals(new BigDecimal(quantidade), fontes.financeiros().get(0).quantidade());
        assertEquals(new BigDecimal(quantidade), fontes.custodias().get(0).quantidade());
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "0.1234567", "10000000000000"})
    void quantidadeEstruturalInvalidaFalhaSemArredondar(String quantidade) {
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(operacao(1, TipoOperacao.COMPRA, a, quantidade, 1)), List.of(), List.of()));
    }

    @Test void preservaDeficitLegadoSemRedistribuirOuValidarVendaComoNova() {
        var es = eventos(List.of(operacao(2, TipoOperacao.VENDA, b, "4", 2), operacao(1, TipoOperacao.COMPRA, a, "10", 1)), List.of(), List.of());
        var estado = new MotorReplayQuantitativo().reproduzir(es).estadoFinal();
        assertEquals(0, new BigDecimal("-4").compareTo(estado.saldo(Grupo.corretora(4))));
        assertEquals(0, new BigDecimal("6").compareTo(estado.quantidadeConsolidada()));
        assertEquals(Identidade.persistida(Dominio.FINANCEIRO, 2), estado.primeirosNegativos().get(Grupo.corretora(4)).identidade());
    }

    @Test void ordemMatematicaIndependeDaEntradaEDoRegistro() {
        var m = movimento(7, NaturezaMovimentacaoCustodia.TRANSFERENCIA, a, "2", "3", 0);
        var n = movimento(8, NaturezaMovimentacaoCustodia.TRANSFERENCIA, a, "10", "11", 0);
        campo(m, "dataRegistro", REGISTRO.plusYears(10));
        var ops = new ArrayList<>(List.of(operacao(1, TipoOperacao.COMPRA, a, "10", 1), operacao(2, TipoOperacao.VENDA, a, "1", 2)));
        var ms = new ArrayList<>(List.of(n, m)); var vs = new ArrayList<>(List.of(ausencia(n), ausencia(m)));
        var copiaOps = List.copyOf(ops); var copiaMs = List.copyOf(ms); var copiaVs = List.copyOf(vs);
        var esperado = eventos(ops, ms, vs); assertEquals(copiaOps, ops); assertEquals(copiaMs, ms); assertEquals(copiaVs, vs);
        Collections.reverse(ops); Collections.reverse(ms); Collections.reverse(vs);
        assertEquals(esperado, eventos(ops, ms, vs));
        assertEquals(List.of(Dominio.CUSTODIA, Dominio.CUSTODIA, Dominio.FINANCEIRO, Dominio.FINANCEIRO), esperado.stream().map(e -> e.identidade().dominio()).toList());
        assertEquals("2/3", esperado.get(0).posicao().fracao().toString());
    }

    @Test void saidaImutavelNaoMantemEntidadesOuListasMutaveis() {
        var m = transferencia(7); var ops = new ArrayList<>(List.of(operacao(1, TipoOperacao.COMPRA, a, "10", 1)));
        var ms = new ArrayList<>(List.of(m)); var vs = new ArrayList<>(List.of(ausencia(m)));
        var fontes = adaptar(ops, ms, vs); ops.clear(); ms.clear(); vs.clear(); campo(m, "quantidade", new BigDecimal("999"));
        assertEquals(1, fontes.financeiros().size()); assertEquals(new BigDecimal("1.000000"), fontes.custodias().get(0).quantidade());
        assertThrows(UnsupportedOperationException.class, () -> fontes.financeiros().clear());
        assertThrows(UnsupportedOperationException.class, () -> fontes.custodias().clear());
    }

    @Test void rejeitaIdsAusentesSemFabricarIdOuToken() {
        var op = operacao(1, TipoOperacao.COMPRA, a, "1", 1); campo(op, "id", null);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(op), List.of(), List.of())); assertNull(op.getId());
        var m = transferencia(7); campo(m, "id", null);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(m), List.of()));
        var n = transferencia(7); var e = estorno(8, n); campo(e, "id", null);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(n), List.of(VinculoEstornoConsultado.vinculoPresente(7L, e))));
    }

    @Test void rejeitaIdentidadesEPosicoesDuplicadasSemFiltrar() {
        var op = operacao(1, TipoOperacao.COMPRA, a, "1", 1);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(op, op), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(op, operacao(2, TipoOperacao.COMPRA, a, "1", 1)), List.of(), List.of()));
        var m = transferencia(7); var n = transferencia(8);
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), List.of(m, n), List.of(ausencia(m), ausencia(n))));
    }

    @ParameterizedTest @ValueSource(strings = {"operacao", "carteiraFinanceira", "acaoFinanceira", "corretoraFinanceira", "movimento", "carteiraCustodia", "acaoCustodia", "origem", "destino", "estorno", "originalEstorno"})
    void rejeitaLazyAntesDeLerGettersSemInicializacao(String caso) {
        var op = operacao(1, TipoOperacao.COMPRA, a, "1", 1); var m = transferencia(7); var e = estorno(8, m);
        Object proxy;
        switch (caso) {
            case "operacao" -> { op = proxy(Operacao.class); proxy = op; }
            case "carteiraFinanceira", "carteiraCustodia" -> { proxy = proxy(Carteira.class); campo(caso.endsWith("Financeira") ? op : m, "carteira", proxy); }
            case "acaoFinanceira", "acaoCustodia" -> { proxy = proxy(Acao.class); campo(caso.endsWith("Financeira") ? op : m, "acao", proxy); }
            case "corretoraFinanceira", "origem", "destino" -> {
                proxy = proxy(Corretora.class);
                campo(caso.equals("corretoraFinanceira") ? op : m, caso.equals("origem") ? "corretoraOrigem" : caso.equals("destino") ? "corretoraDestino" : "corretora", proxy);
            }
            case "movimento" -> { m = proxy(MovimentacaoCustodia.class); proxy = m; }
            case "estorno" -> { e = proxy(EstornoMovimentacaoCustodia.class); proxy = e; }
            case "originalEstorno" -> { proxy = proxy(MovimentacaoCustodia.class); campo(e, "movimentacaoOriginal", proxy); }
            default -> throw new AssertionError(caso);
        }
        final var financeiro = op; final var original = m;
        var vinculo = VinculoEstornoConsultado.vinculoPresente(7L, e);
        var falha = assertThrows(IllegalStateException.class, () -> adaptar(List.of(financeiro), List.of(original), List.of(vinculo)));
        assertTrue(falha.getMessage().contains("carregada"));
        var inicializador = ((HibernateProxy) proxy).getHibernateLazyInitializer(); verify(inicializador, never()).initialize();
        assertTrue(mockingDetails(proxy).getInvocations().stream().noneMatch(i -> i.getMethod().getName().startsWith("get") && !i.getMethod().getName().equals("getHibernateLazyInitializer")));
    }

    @Test void contextoVazioEhValidoMasFontesNulasOuContextoInvalidoFalham() {
        assertTrue(adaptar(List.of(), List.of(), List.of()).financeiros().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> adaptador.adaptar(0, 2, List.of(), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> adaptar(null, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> adaptar(List.of(), null, List.of()));
    }

    @Test void rejeitaColecoesHibernateNaoCarregadasAntesDeIterar() {
        var ops = new org.hibernate.collection.spi.PersistentBag<Operacao>();
        var ms = new org.hibernate.collection.spi.PersistentBag<MovimentacaoCustodia>();
        assertThrows(IllegalStateException.class, () -> adaptar(ops, List.of(), List.of()));
        assertThrows(IllegalStateException.class, () -> adaptar(List.of(), ms, List.of()));
        assertFalse(ops.wasInitialized()); assertFalse(ms.wasInitialized());
    }

    private AdaptadorEventosQuantitativos.Fontes adaptar(List<Operacao> ops, List<MovimentacaoCustodia> ms, List<VinculoEstornoConsultado> vs) {
        return adaptador.adaptar(1, 2, ops, ms, vs);
    }
    private List<EventoQuantitativo> eventos(List<Operacao> ops, List<MovimentacaoCustodia> ms, List<VinculoEstornoConsultado> vs) {
        var fontes = adaptar(ops, ms, vs); return new NormalizadorEventosQuantitativos().normalizar(1, 2, fontes.financeiros(), fontes.custodias());
    }
    private VinculoEstornoConsultado ausencia(MovimentacaoCustodia m) { return VinculoEstornoConsultado.ausenciaVerificada(m.getId()); }
    private Operacao operacao(long id, TipoOperacao tipo, Corretora corretora, String quantidade, int ordem) {
        var q = new BigDecimal(quantidade); var op = new Operacao(carteira, acao, corretora, tipo, q, BigDecimal.ONE, DATA, ordem, q);
        campo(op, "id", id); return op;
    }
    private MovimentacaoCustodia transferencia(long id) { return movimento(id, NaturezaMovimentacaoCustodia.TRANSFERENCIA, a, "1", "2", 0); }
    private MovimentacaoCustodia movimento(long id, NaturezaMovimentacaoCustodia natureza, Corretora origem, String n, String d, int ancora) {
        var m = hidratada(MovimentacaoCustodia.class); campo(m, "id", id); campo(m, "carteira", carteira); campo(m, "acao", acao);
        campo(m, "natureza", natureza); campo(m, "corretoraOrigem", origem); campo(m, "corretoraDestino", b);
        campo(m, "quantidade", new BigDecimal("1.000000")); campo(m, "dataEfetiva", DATA); campo(m, "dataRegistro", REGISTRO);
        campo(m, "ordemFinanceiraAncora", ancora); campo(m, "posicaoNumerador", n); campo(m, "posicaoDenominador", d); return m;
    }
    private EstornoMovimentacaoCustodia estorno(long id, MovimentacaoCustodia original) {
        var e = hidratada(EstornoMovimentacaoCustodia.class); campo(e, "id", id); campo(e, "movimentacaoOriginal", original);
        campo(e, "dataRegistro", REGISTRO.plusDays(1)); campo(e, "motivo", "Sintético"); return e;
    }
    private Carteira carteira(long id) { var c = new Carteira("Sintética", REGISTRO); campo(c, "id", id); return c; }
    private Acao acao(long id, Mercado mercado) {
        var c = new Acao("SINT", "Sintética", mercado, mercado == Mercado.EUA ? Moeda.USD : Moeda.BRL, BigDecimal.ONE, REGISTRO);
        campo(c, "id", id); return c;
    }
    private Corretora corretora(long id) { var c = hidratada(Corretora.class); campo(c, "id", id); return c; }
    private static void campo(Object alvo, String nome, Object valor) { ReflectionTestUtils.setField(alvo, nome, valor); }
    private static <T> T hidratada(Class<T> tipo) {
        try { var construtor = tipo.getDeclaredConstructor(); construtor.setAccessible(true); return construtor.newInstance(); }
        catch (ReflectiveOperationException falha) { throw new AssertionError(falha); }
    }
    private static <T> T proxy(Class<T> tipo) {
        var proxy = mock(tipo, withSettings().extraInterfaces(HibernateProxy.class));
        var hibernateProxy = (HibernateProxy) proxy; when(hibernateProxy.asHibernateProxy()).thenReturn(hibernateProxy);
        var inicializador = mock(LazyInitializer.class); when(hibernateProxy.getHibernateLazyInitializer()).thenReturn(inicializador);
        when(inicializador.isUninitialized()).thenReturn(true); return proxy;
    }
}
