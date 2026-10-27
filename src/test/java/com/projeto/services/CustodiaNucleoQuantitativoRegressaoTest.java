package com.projeto.services;

import com.projeto.entities.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static com.projeto.services.EventoQuantitativo.*;
import static com.projeto.services.NormalizadorEventosQuantitativos.*;
import static com.projeto.services.ValidadorHistoricoQuantitativo.Intencao;
import static com.projeto.services.ValidadorHistoricoQuantitativo.TipoIntencao;
import static org.junit.jupiter.api.Assertions.*;

/** Integração lógica em memória: nenhuma fixture é persistida e nenhum contexto é inicializado. */
class CustodiaNucleoQuantitativoRegressaoTest {
    private static final LocalDate DATA = LocalDate.of(2026, 10, 1);
    private static final Grupo A = Grupo.corretora(1), B = Grupo.corretora(2), C = Grupo.corretora(3), DGR = Grupo.corretora(4);
    private static final Grupo SEM = Grupo.semCorretora();
    private static final Identidade CANDIDATA = Identidade.temporaria(Dominio.CUSTODIA,
            UUID.fromString("00000000-0000-0000-0000-000000000005"));
    private final NormalizadorEventosQuantitativos normalizador = new NormalizadorEventosQuantitativos();
    private final MotorReplayQuantitativo motor = new MotorReplayQuantitativo();
    private final ValidadorHistoricoQuantitativo validador = new ValidadorHistoricoQuantitativo();
    private final BuscadorPosicaoCustodia buscador = new BuscadorPosicaoCustodia();

    private Financeiro f(long id, int dia, int ordem, TipoOperacao tipo, Grupo grupo, String q) {
        return new Financeiro(Identidade.persistida(Dominio.FINANCEIRO, id), 1, 2, Mercado.EUA,
                tipo, new BigDecimal(q), DATA.plusDays(dia), ordem, grupo);
    }
    private Custodia c(long id, int dia, int ancora, String n, String d, Grupo origem, Grupo destino, String q) {
        var identidade = Identidade.persistida(Dominio.CUSTODIA, id);
        return new Custodia(identidade, 1, 2, Mercado.EUA,
                origem.equals(SEM) ? NaturezaMovimentacaoCustodia.ATRIBUICAO : NaturezaMovimentacaoCustodia.TRANSFERENCIA,
                new BigDecimal(q), DATA.plusDays(dia), ancora, PosicaoRacionalCustodia.deTexto(n, d), origem, destino,
                VinculoEstorno.ausenciaVerificada(identidade));
    }
    private List<EventoQuantitativo> eventos(List<Financeiro> fs, List<Custodia> cs) {
        return normalizador.normalizar(1, 2, fs, cs);
    }
    private MotorReplayQuantitativo.Resultado replay(List<Financeiro> fs, List<Custodia> cs) {
        var r = motor.reproduzir(eventos(fs, cs));
        for (var p : r.checkpoints()) {
            assertTrue(p.estado().reconciliada()); decimal("0", p.estado().diferencaReconciliacao());
        }
        return r;
    }
    private BuscadorPosicaoCustodia.ResultadoBusca localizar(List<Financeiro> fs, List<Custodia> cs,
            int dia, Grupo origem, Grupo destino, String q) {
        return buscador.buscar(eventos(fs, cs), new Intencao(origem.equals(SEM) ? TipoIntencao.ATRIBUICAO : TipoIntencao.TRANSFERENCIA, CANDIDATA),
                new BuscadorPosicaoCustodia.DadosCandidata(1, 2, Mercado.EUA, DATA.plusDays(dia), new BigDecimal(q), origem, destino));
    }
    /** Reconstrução escalar de uma identidade sintética; não representa flush/commit nem usa JPA. */
    private Custodia registroSintetico(BuscadorPosicaoCustodia.ResultadoBusca busca, long id) {
        assertTrue(busca.encontrada());
        var e = busca.candidata(); var identidade = Identidade.persistida(Dominio.CUSTODIA, id);
        return new Custodia(identidade, e.carteiraId(), e.acaoId(), e.mercado(),
                e.natureza() == Natureza.TRANSFERENCIA ? NaturezaMovimentacaoCustodia.TRANSFERENCIA : NaturezaMovimentacaoCustodia.ATRIBUICAO,
                e.quantidade(), e.posicao().dataEfetiva(), e.posicao().ancoraFinanceira(), busca.posicaoPersistivel(),
                e.grupoOrigem(), e.grupoDestino(), VinculoEstorno.ausenciaVerificada(identidade));
    }
    private Custodia comEstorno(Custodia original, long id) {
        return new Custodia(original.identidade(), original.carteiraId(), original.acaoId(), original.mercado(), original.natureza(),
                original.quantidade(), original.dataEfetiva(), original.ancoraFinanceira(), original.fracao(), original.origem(), original.destino(),
                VinculoEstorno.presente(original.identidade(), id));
    }
    private static void decimal(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual), () -> esperado + " != " + atual);
    }

    @Test void compraBuscaTransferenciaReconstrucaoEVendaComIdsIguaisEntreDominios() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"), f(2, 2, 1, TipoOperacao.VENDA, B, "40"));
        var antes = replay(fs, List.of()); assertNotNull(antes.estadoFinal().primeirosNegativos().get(B));
        var busca = localizar(fs, List.of(), 1, A, B, "60"); var original = registroSintetico(busca, 1);
        var depois = replay(fs, List.of(original));
        decimal("60", depois.estadoFinal().quantidadeConsolidada()); decimal("40", depois.estadoFinal().saldo(A)); decimal("20", depois.estadoFinal().saldo(B));
        assertTrue(depois.estadoFinal().primeirosNegativos().isEmpty());
        assertNotEquals(fs.get(0).identidade(), original.identidade());
        assertEquals(busca.candidata().posicao(), depois.checkpoints().get(1).evento().posicao());
        assertEquals(3, depois.checkpoints().size());
    }
    @Test void atribuicaoAnteriorAVendaPreservaCompraSemCorretora() {
        var compra = f(1, 0, 1, TipoOperacao.COMPRA, SEM, "100"); var fs = List.of(compra, f(2, 2, 1, TipoOperacao.VENDA, A, "40"));
        var original = registroSintetico(localizar(fs, List.of(), 1, SEM, A, "60"), 1);
        var r = replay(fs, List.of(original));
        decimal("40", r.estadoFinal().saldo(SEM)); decimal("20", r.estadoFinal().saldo(A));
        assertEquals(SEM, compra.grupo()); assertEquals(NaturezaMovimentacaoCustodia.ATRIBUICAO, original.natureza());
    }
    @Test void novaVendaSoPassaSaldoLocalQuandoConsideraCustodiaNormalizada() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"));
        var original = registroSintetico(localizar(fs, List.of(), 1, A, B, "60"), 1);
        var id = Identidade.temporaria(Dominio.FINANCEIRO, UUID.fromString("00000000-0000-0000-0000-000000000007"));
        var nova = new Financeiro(id, 1, 2, Mercado.EUA, TipoOperacao.VENDA, new BigDecimal("40"), DATA.plusDays(2), 1, B);
        var depoisF = List.of(fs.get(0), nova); var intencao = new Intencao(TipoIntencao.VENDA, id);
        assertTrue(validador.validar(replay(fs, List.of(original)), replay(depoisF, List.of(original)), intencao).valido());
        assertEquals(ValidadorHistoricoQuantitativo.Codigo.SALDO_CORRETORA_INSUFICIENTE,
                validador.validar(replay(fs, List.of()), replay(depoisF, List.of()), intencao).conflito().codigo());
    }
    @Test void cadeiaECicloReconstroemNormalizacaoPreservandoTodasAsPosicoes() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"));
        var t1 = registroSintetico(localizar(fs, List.of(), 1, A, B, "60"), 1);
        var t2 = registroSintetico(localizar(fs, List.of(t1), 1, B, C, "20"), 2);
        var t3 = registroSintetico(localizar(fs, List.of(t1, t2), 1, B, DGR, "40"), 3);
        assertEquals("1/2", t1.fracao().toString()); assertEquals("2/3", t2.fracao().toString()); assertEquals("3/5", t3.fracao().toString());
        var cadeia = replay(fs, List.of(t3, t2, t1));
        decimal("40", cadeia.estadoFinal().saldo(A)); decimal("0", cadeia.estadoFinal().saldo(B));
        decimal("20", cadeia.estadoFinal().saldo(C)); decimal("40", cadeia.estadoFinal().saldo(DGR));
        var t4 = registroSintetico(localizar(fs, List.of(t1, t2, t3), 1, C, A, "20"), 4);
        var t5 = registroSintetico(localizar(fs, List.of(t4, t3, t2, t1), 1, DGR, A, "40"), 5);
        var ciclo = replay(fs, List.of(t5, t4, t3, t2, t1));
        decimal("100", ciclo.estadoFinal().saldo(A));
        for (var grupo : List.of(B, C, DGR)) decimal("0", ciclo.estadoFinal().saldo(grupo));
        assertTrue(ciclo.estadoFinal().primeirosNegativos().isEmpty()); assertEquals(6, ciclo.checkpoints().size());
        assertEquals("1/2", t1.fracao().toString()); assertEquals("2/3", t2.fracao().toString());
    }
    @ParameterizedTest @CsvSource({"false", "true"})
    void estornoComCoberturaIndependenteEOperacoesPosteriores(boolean atribuicao) {
        Grupo origem = atribuicao ? SEM : A;
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, origem, "100"), f(2, 0, 2, TipoOperacao.COMPRA, B, "50"),
                f(3, 2, 1, TipoOperacao.VENDA, B, "20"));
        var original = registroSintetico(localizar(fs, List.of(), 1, origem, B, "40"), 1);
        var antes = replay(fs, List.of(original)); var intencao = new Intencao(TipoIntencao.ESTORNO, original.identidade());
        assertTrue(validador.verificarPrecondicoes(antes, intencao).valido());
        var estornada = comEstorno(original, 10); var depois = replay(fs, List.of(estornada));
        assertTrue(validador.validar(antes, depois, intencao).valido());
        decimal("100", depois.estadoFinal().saldo(origem)); decimal("30", depois.estadoFinal().saldo(B));
        assertEquals(antes.checkpoints().get(2).evento().posicao(), depois.checkpoints().get(2).evento().posicao());
        assertTrue(depois.checkpoints().get(2).evento().neutralizado()); assertEquals(4, depois.checkpoints().size());
        assertEquals(ValidadorHistoricoQuantitativo.Codigo.ESTORNO_DUPLICADO,
                validador.validar(depois, null, intencao).conflito().codigo());
    }
    @ParameterizedTest @CsvSource({"novo,40,30,0,30", "agravado,10,20,10,20"})
    void estornoReconstruidoDetectaDeficitNovoOuAgravado(String caso, String transferida, String vendida, String dAntes, String dDepois) {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"), f(2, 2, 1, TipoOperacao.VENDA, B, vendida),
                f(3, 3, 1, TipoOperacao.COMPRA, B, "40"));
        var original = c(1, 1, 0, "1", "2", A, B, transferida);
        var antes = replay(fs, List.of(original)); var depois = replay(fs, List.of(comEstorno(original, 10)));
        var erro = validador.validar(antes, depois, new Intencao(TipoIntencao.ESTORNO, original.identidade())).conflito();
        assertEquals(ValidadorHistoricoQuantitativo.Codigo.CONFLITO_HISTORICO_QUANTITATIVO, erro.codigo(), caso);
        assertEquals(fs.get(1).identidade(), erro.identidade()); decimal(dAntes, erro.evidencia().deficitAntes()); decimal(dDepois, erro.evidencia().deficitDepois());
        assertTrue(depois.estadoFinal().saldo(B).signum() > 0);
    }
    @Test void estornoProlongaDeficitNoCheckpointDaOriginalPreservada() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"), f(2, 1, 1, TipoOperacao.VENDA, B, "10"), f(3, 3, 1, TipoOperacao.COMPRA, B, "20"));
        var original = c(1, 2, 0, "1", "2", A, B, "10");
        var antes = replay(fs, List.of(original)); var depois = replay(fs, List.of(comEstorno(original, 10)));
        var erro = validador.validar(antes, depois, new Intencao(TipoIntencao.ESTORNO, original.identidade())).conflito();
        assertEquals(original.identidade(), erro.identidade()); decimal("0", erro.evidencia().deficitAntes()); decimal("10", erro.evidencia().deficitDepois());
        assertEquals(4, depois.checkpoints().size()); decimal("10", depois.estadoFinal().saldo(B));
    }
    @Test void primeiraOcorrenciaLegadaSobreviveRegularizacaoMasSomeComCompraRetroativaValida() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"), f(2, 2, 1, TipoOperacao.VENDA, B, "10"), f(3, 3, 1, TipoOperacao.COMPRA, B, "20"));
        var original = registroSintetico(localizar(fs, List.of(), 1, A, C, "5"), 1);
        var antes = replay(fs, List.of(original));
        assertEquals(fs.get(1).identidade(), antes.estadoFinal().primeirosNegativos().get(B).identidade());
        decimal("-10", antes.estadoFinal().primeirosNegativos().get(B).saldoAposEvento()); decimal("10", antes.estadoFinal().saldo(B));
        var id = Identidade.temporaria(Dominio.FINANCEIRO, UUID.fromString("00000000-0000-0000-0000-000000000006"));
        var nova = new Financeiro(id, 1, 2, Mercado.EUA, TipoOperacao.COMPRA, new BigDecimal("10"), DATA.minusDays(1), 1, B);
        var posteriores = new ArrayList<>(fs); posteriores.add(nova); var depois = replay(posteriores, List.of(original));
        assertTrue(validador.validar(antes, depois, new Intencao(TipoIntencao.COMPRA, id)).valido());
        assertFalse(depois.estadoFinal().primeirosNegativos().containsKey(B));
    }
    @Test void deficitLegadoReduzidoContinuaConsultavelSemRevalidarVendaAntiga() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"), f(2, 2, 1, TipoOperacao.VENDA, B, "10"));
        var original = registroSintetico(localizar(fs, List.of(), 1, A, B, "5"), 1);
        var depois = replay(fs, List.of(original));
        decimal("-5", depois.estadoFinal().saldo(B)); decimal("-5", depois.estadoFinal().primeirosNegativos().get(B).saldoAposEvento());
        decimal("90", depois.estadoFinal().quantidadeConsolidada());
    }
    @Test void posicaoEncerradaPreservaGruposNegativosECheckpointEstornado() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"), f(2, 2, 1, TipoOperacao.VENDA, B, "100"));
        var original = registroSintetico(localizar(fs, List.of(), 1, A, B, "10"), 1);
        var ativa = replay(fs, List.of(original)); decimal("0", ativa.estadoFinal().quantidadeConsolidada());
        decimal("90", ativa.estadoFinal().saldo(A)); decimal("-90", ativa.estadoFinal().saldo(B));
        var neutralizada = replay(fs, List.of(comEstorno(original, 10)));
        assertEquals(3, neutralizada.checkpoints().size()); assertTrue(neutralizada.checkpoints().get(1).evento().neutralizado());
        decimal("0", neutralizada.estadoFinal().quantidadeConsolidada());
        assertEquals(ValidadorHistoricoQuantitativo.Codigo.CONFLITO_HISTORICO_QUANTITATIVO,
                validador.validar(ativa, neutralizada, new Intencao(TipoIntencao.ESTORNO, original.identidade())).conflito().codigo());
    }
    @Test void novaVendaInvalidaCustodiaFuturaAntesDoDiagnosticoDiferencial() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100")); var original = c(1, 2, 0, "1", "2", A, B, "80");
        var id = Identidade.temporaria(Dominio.FINANCEIRO, UUID.randomUUID());
        var nova = new Financeiro(id, 1, 2, Mercado.EUA, TipoOperacao.VENDA, new BigDecimal("30"), DATA.plusDays(1), 1, A);
        var antes = replay(fs, List.of(original)); var depois = replay(List.of(fs.get(0), nova), List.of(original));
        var erro = validador.validar(antes, depois, new Intencao(TipoIntencao.VENDA, id)).conflito();
        assertEquals(ValidadorHistoricoQuantitativo.Codigo.MOVIMENTACAO_CUSTODIA_INVALIDADA, erro.codigo()); assertEquals(original.identidade(), erro.identidade());
    }
    @ParameterizedTest @CsvSource({"20,SALDO_CORRETORA_INSUFICIENTE", "120,POSICAO_INSUFICIENTE"})
    void precedenciaConsolidadoLocalCustodiaEDeficitNaComposicao(String q, ValidadorHistoricoQuantitativo.Codigo esperado) {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"), f(2, 0, 2, TipoOperacao.COMPRA, B, "10"));
        var original = c(2, 2, 0, "1", "2", B, C, "10"); var id = Identidade.temporaria(Dominio.FINANCEIRO, UUID.randomUUID());
        var nova = new Financeiro(id, 1, 2, Mercado.EUA, TipoOperacao.VENDA, new BigDecimal(q), DATA.plusDays(1), 1, B);
        var erro = validador.validar(replay(fs, List.of(original)), replay(List.of(fs.get(0), fs.get(1), nova), List.of(original)),
                new Intencao(TipoIntencao.VENDA, id)).conflito();
        assertEquals(esperado, erro.codigo());
    }
    @Test void fontesEmbaralhadasProduzemMesmaBuscaEReplayNormalizado() {
        var fs = new ArrayList<>(List.of(f(99, 0, 1, TipoOperacao.COMPRA, A, "100"), f(1, 3, 1, TipoOperacao.COMPRA, B, "1")));
        var cs = new ArrayList<>(List.of(c(99, 1, 0, "2", "3", A, B, "60"), c(1, 1, 0, "10", "11", B, C, "20")));
        var esperado = localizar(fs, cs, 1, B, DGR, "40"); assertEquals("6/7", esperado.posicaoPersistivel().toString());
        var replayEsperado = replay(fs, cs); var copiaF = List.copyOf(fs); var copiaC = List.copyOf(cs);
        Collections.reverse(fs); Collections.reverse(cs);
        assertEquals(esperado, localizar(fs, cs, 1, B, DGR, "40")); assertEquals(replayEsperado, replay(fs, cs));
        var reversa = new ArrayList<>(copiaF); Collections.reverse(reversa); assertEquals(reversa, fs);
        assertEquals(copiaC.get(0).fracao().toString(), "2/3");
    }
    @Test void chaveDeAncoraZeroReconstruidaPermaneceAntesDeNovaOperacaoNoDia() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"));
        var original = registroSintetico(localizar(fs, List.of(), 1, A, B, "10"), 1);
        var posterior = f(2, 1, 1, TipoOperacao.COMPRA, C, "1");
        var eventos = eventos(List.of(posterior, fs.get(0)), List.of(original));
        assertEquals(original.identidade(), eventos.get(1).identidade()); assertEquals(posterior.identidade(), eventos.get(2).identidade());
        assertEquals(0, original.ancoraFinanceira()); assertEquals("1/2", original.fracao().toString());
    }
    @Test void normalizacaoNaoPermiteVinculoDesconhecidoVirarAusenciaDeEstorno() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100")); var original = c(1, 1, 0, "1", "2", A, B, "10");
        var incompleta = new Custodia(original.identidade(), 1, 2, Mercado.EUA, original.natureza(), original.quantidade(),
                original.dataEfetiva(), 0, original.fracao(), A, B, null);
        assertThrows(IllegalArgumentException.class, () -> localizar(fs, List.of(incompleta), 1, A, C, "1"));
        assertThrows(IllegalArgumentException.class, () -> localizar(List.of(fs.get(0), fs.get(0)), List.of(), 1, A, B, "1"));
    }
    @Test void primeiroEspaco513ValidoNormalizadoRetornaLimiteSemFallback() {
        var d = BigInteger.TEN.pow(512).subtract(BigInteger.ONE);
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100")); var original = c(1, 1, 0, "1", d.toString(), A, B, "50");
        var resultado = localizar(fs, List.of(original), 1, A, C, "10");
        assertEquals(BuscadorPosicaoCustodia.Codigo.LIMITE_POSICAO_CUSTODIA, resultado.diagnostico().codigo()); assertEquals(1, resultado.espacosAvaliados());
        assertEquals(513, resultado.diagnostico().posicao().fracao().getDenominador().toString().length());
        assertThrows(LimitePosicaoCustodiaException.class, () -> resultado.diagnostico().posicao().fracao().paraPersistivel());
        assertEquals(d, original.fracao().getDenominador());
    }
    @Test void espaco513InvalidoPorFuturoNaoBloqueiaBuscaNormalizadaPosterior() {
        var d = BigInteger.TEN.pow(512).subtract(BigInteger.ONE);
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"));
        var t1 = c(1, 1, 0, "1", d.toString(), A, B, "80"); var t2 = c(2, 1, 0, "1", "2", B, A, "60");
        var resultado = localizar(fs, List.of(t2, t1), 1, A, C, "50");
        assertTrue(resultado.encontrada()); assertEquals(3, resultado.espacosAvaliados()); assertEquals("2/3", resultado.posicaoPersistivel().toString());
        assertEquals(4, replay(fs, List.of(t1, t2, registroSintetico(resultado, 3))).checkpoints().size());
    }
    @Test void limite512EhPreservadoNoRoundtripEscalarAposBusca() {
        var d = BigInteger.TEN.pow(512).subtract(BigInteger.ONE);
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100")); var t1 = c(1, 1, 0, "1", d.subtract(BigInteger.ONE).toString(), A, B, "50");
        var original = registroSintetico(localizar(fs, List.of(t1), 1, A, C, "10"), 2);
        assertEquals(d, original.fracao().getDenominador()); assertEquals(512, original.fracao().getDenominadorTexto().length());
        var r = replay(fs, List.of(t1, original)); assertEquals(original.identidade(), r.checkpoints().get(1).evento().identidade());
    }
    @Test void ausenciaDeEspacoValidoRetornaCausaDoPrimeiroComSaldoAposNormalizacao() {
        var fs = List.of(f(1, 0, 1, TipoOperacao.COMPRA, A, "100"), f(2, 0, 2, TipoOperacao.VENDA, A, "80"));
        var resultado = localizar(fs, List.of(), 0, A, B, "50");
        assertEquals(BuscadorPosicaoCustodia.Codigo.POSICAO_CUSTODIA_INDISPONIVEL, resultado.diagnostico().codigo());
        assertEquals(1, resultado.diagnostico().posicao().ancoraFinanceira());
        assertEquals(ValidadorHistoricoQuantitativo.Codigo.CONFLITO_HISTORICO_QUANTITATIVO, resultado.diagnostico().causa().codigo());
    }

    private record FinanceiroObservado(CalculadoraPosicao.ResultadoReplay replay,
            BigDecimal valorAtual, BigDecimal naoRealizado, BigDecimal rentabilidade) { }
    private FinanceiroObservado observar(List<Operacao> ops, BigDecimal cotacao) {
        var calculadora = new CalculadoraPosicao(); var r = calculadora.reproduzir(ops); assertTrue(r.valido());
        var atual = calculadora.calcularValorAtual(r.posicao().quantidadeAtual(), cotacao);
        var naoRealizado = calculadora.calcularResultadoNaoRealizado(atual, r.posicao().custoPosicao());
        return new FinanceiroObservado(r, atual, naoRealizado,
                new CalculadoraRentabilidade().calcularPercentual(naoRealizado, r.posicao().custoPosicao()));
    }
    private List<Financeiro> fontes(List<Operacao> ops) {
        return ops.stream().map(o -> new Financeiro(Identidade.persistida(Dominio.FINANCEIRO, o.getId()), o.getCarteira().getId(),
                o.getAcao().getId(), o.getAcao().getMercado(), o.getTipo(), o.getQuantidade(), o.getDataOperacao(), o.getOrdemNoDia(),
                o.getCorretora() == null ? SEM : Grupo.corretora(o.getCorretora().getId()))).toList();
    }
    private List<Operacao> operacoes(Mercado mercado, String precoVenda, boolean grandes) {
        var instante = OffsetDateTime.parse("2026-10-01T00:00:00Z");
        var carteira = new Carteira("Sintética", instante); ReflectionTestUtils.setField(carteira, "id", 1L);
        var acao = new Acao("ATIVO", "Sintética", mercado, mercado == Mercado.BRASIL ? Moeda.BRL : Moeda.USD,
                new BigDecimal("17.000000"), instante); ReflectionTestUtils.setField(acao, "id", 2L);
        var a = corretora(1); var b = corretora(2);
        String compraQ = grandes ? "9999999999999.999999" : mercado == Mercado.BRASIL ? "100.000000" : "0.100000";
        String vendaQ = mercado == Mercado.BRASIL ? "40.000000" : "0.040000";
        var ops = new ArrayList<Operacao>();
        ops.add(op(1, carteira, acao, a, TipoOperacao.COMPRA, compraQ, grandes ? "1" : "10", 0));
        ops.add(op(2, carteira, acao, a, TipoOperacao.COMPRA, compraQ, grandes ? "1" : "20", 1));
        if (!grandes) ops.add(op(3, carteira, acao, b, TipoOperacao.VENDA, vendaQ, precoVenda, 3));
        return List.copyOf(ops);
    }
    private Corretora corretora(long id) {
        var c = new Corretora("00000000000000", "Sintética", "C" + id, null, null, "00000000", "Rua", null, null,
                "Bairro", "Cidade", "SP", "ATIVA", OffsetDateTime.parse("2026-10-01T00:00:00Z"));
        ReflectionTestUtils.setField(c, "id", id); return c;
    }
    private Operacao op(long id, Carteira carteira, Acao acao, Corretora corretora, TipoOperacao tipo, String q, String p, int dia) {
        var quantidade = new BigDecimal(q); var preco = new BigDecimal(p);
        var o = new Operacao(carteira, acao, corretora, tipo, quantidade, preco, DATA.plusDays(dia), 1, quantidade.multiply(preco));
        ReflectionTestUtils.setField(o, "id", id); return o;
    }
    @ParameterizedTest @CsvSource({"BRASIL,20", "BRASIL,5", "BRASIL,15", "EUA,20", "EUA,5", "EUA,15"})
    void custodiaPreservaTodasAsMetricasFinanceirasComLucroPrejuizoOuResultadoZero(Mercado mercado, String precoVenda) {
        var ops = operacoes(mercado, precoVenda, false); var cotacao = ops.get(0).getAcao().getCotacaoAtual();
        var antes = observar(ops, cotacao); var fontesAntes = fontes(ops);
        var distribuicao = new CalculadoraDistribuicaoCorretora(new CalculadoraPosicao()); var distribuicaoAntes = distribuicao.calcular(1L, ops);
        var normalizadosAntes = eventos(fontesAntes, List.of()); var q = mercado == Mercado.BRASIL ? "60.000000" : "0.060000";
        var busca = buscador.buscar(normalizadosAntes, new Intencao(TipoIntencao.TRANSFERENCIA, CANDIDATA),
                new BuscadorPosicaoCustodia.DadosCandidata(1, 2, mercado, DATA.plusDays(2), new BigDecimal(q), A, B));
        var original = registroSintetico(busca, 1); var combinado = replay(fontes(ops), List.of(original));
        assertEquals(antes, observar(ops, cotacao)); assertEquals(fontesAntes, fontes(ops));
        assertEquals(distribuicaoAntes, distribuicao.calcular(1L, ops));
        decimal(antes.replay().posicao().quantidadeAtual().toPlainString(), combinado.estadoFinal().quantidadeConsolidada());
        decimal(distribuicaoAntes.posicoes().get(0).quantidadeConsolidada().toPlainString(), combinado.estadoFinal().somaSaldos());
        assertEquals(normalizadosAntes, eventos(fontes(ops), List.of()));
        assertEquals(List.of(TipoOperacao.COMPRA, TipoOperacao.VENDA), List.of(TipoOperacao.values()));
        assertTrue(combinado.estadoFinal().primeirosNegativos().isEmpty());
        decimal(mercado == Mercado.BRASIL ? "140" : "0.140000", combinado.estadoFinal().saldo(A));
        decimal(mercado == Mercado.BRASIL ? "20" : "0.020000", combinado.estadoFinal().saldo(B));
        assertEquals(cotacao, ops.get(0).getAcao().getCotacaoAtual());
    }
    @Test void acumuladoresGrandesReconciliamComValidacaoQuantitativaFinanceiraSemArredondar() {
        var ops = operacoes(Mercado.EUA, "1", true); var fs = fontes(ops);
        var financeiro = new CalculadoraPosicao().validarQuantidade(ops); assertTrue(financeiro.valido());
        var original = registroSintetico(localizar(fs, List.of(), 2, A, B, "9999999999999.999999"), 1);
        var r = replay(fs, List.of(original));
        decimal("19999999999999.999998", r.estadoFinal().quantidadeConsolidada());
        decimal(financeiro.posicao().quantidadeAtual().toPlainString(), r.estadoFinal().somaSaldos());
        decimal("9999999999999.999999", r.estadoFinal().saldo(A)); decimal("9999999999999.999999", r.estadoFinal().saldo(B));
        assertEquals(financeiro, new CalculadoraPosicao().validarQuantidade(ops));
    }
}
