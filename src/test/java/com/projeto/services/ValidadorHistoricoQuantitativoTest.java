package com.projeto.services;

import com.projeto.entities.Mercado;
import com.projeto.entities.PosicaoRacionalCustodia;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import static com.projeto.services.EventoQuantitativo.*;
import static com.projeto.services.MotorReplayQuantitativo.*;
import static com.projeto.services.ValidadorHistoricoQuantitativo.*;
import static org.junit.jupiter.api.Assertions.*;

class ValidadorHistoricoQuantitativoTest {
    private static final LocalDate DATA = LocalDate.of(2026, 10, 1);
    private static final Grupo A = Grupo.corretora(1), B = Grupo.corretora(2), C = Grupo.corretora(3);
    private static final Grupo SEM = Grupo.semCorretora();
    private final ValidadorHistoricoQuantitativo validador = new ValidadorHistoricoQuantitativo();
    private final MotorReplayQuantitativo motor = new MotorReplayQuantitativo();

    private EventoQuantitativo f(long id, int dia, int ordem, Natureza natureza, Grupo grupo, String q) {
        return new EventoQuantitativo(Identidade.persistida(Dominio.FINANCEIRO, id), 1, 2, Mercado.EUA,
                natureza, new BigDecimal(q), PosicaoCronologicaQuantitativa.financeira(DATA.plusDays(dia), ordem),
                grupo, null, false);
    }
    private EventoQuantitativo compra(long id, int ordem, Grupo grupo, String q) { return f(id, 0, ordem, Natureza.COMPRA, grupo, q); }
    private EventoQuantitativo venda(long id, int ordem, Grupo grupo, String q) { return f(id, 0, ordem, Natureza.VENDA, grupo, q); }
    private EventoQuantitativo c(long id, int dia, int ancora, String n, String d, Grupo origem, Grupo destino, String q) {
        return new EventoQuantitativo(Identidade.persistida(Dominio.CUSTODIA, id), 1, 2, Mercado.EUA,
                origem.equals(SEM) ? Natureza.ATRIBUICAO : Natureza.TRANSFERENCIA, new BigDecimal(q),
                PosicaoCronologicaQuantitativa.custodia(DATA.plusDays(dia), ancora, PosicaoRacionalCustodia.deTexto(n, d)),
                origem, destino, false);
    }
    private EventoQuantitativo t(long id, int ancora, Grupo origem, Grupo destino, String q) {
        return c(id, 0, ancora, "1", "2", origem, destino, q);
    }
    private EventoQuantitativo neutralizar(EventoQuantitativo e) {
        return new EventoQuantitativo(e.identidade(), e.carteiraId(), e.acaoId(), e.mercado(), e.natureza(),
                e.quantidade(), e.posicao(), e.grupoOrigem(), e.grupoDestino(), true);
    }
    private Resultado r(EventoQuantitativo... eventos) {
        // Fixtures representam a saída ordenada do normalizador, sem alterar qualquer chave.
        return motor.reproduzir(java.util.Arrays.stream(eventos).sorted(Comparator.comparing(EventoQuantitativo::posicao)).toList());
    }
    private Intencao inserir(EventoQuantitativo e) { return new Intencao(TipoIntencao.valueOf(e.natureza().name()), e.identidade()); }
    private Intencao estornar(EventoQuantitativo e) { return new Intencao(TipoIntencao.ESTORNO, e.identidade()); }
    private ResultadoValidacao validar(Resultado antes, Resultado depois, EventoQuantitativo candidata) {
        return validador.validar(antes, depois, inserir(candidata));
    }
    private Conflito conflito(Codigo esperado, ResultadoValidacao resultado) {
        assertFalse(resultado.valido());
        assertNotNull(resultado.conflito());
        assertEquals(esperado, resultado.conflito().codigo());
        return resultado.conflito();
    }
    private static void decimal(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual), () -> esperado + " != " + atual);
    }

    @Test void exemploDesignDeficitLegadoInalteradoComCandidataTemporaria() {
        var f1 = compra(101, 1, A, "100"); var f2 = compra(102, 2, B, "10");
        var f3 = venda(103, 3, B, "20"); var f4 = compra(104, 4, B, "10");
        var base = t(1, 1, A, C, "5");
        var temporaria = new EventoQuantitativo(Identidade.temporaria(Dominio.CUSTODIA,
                UUID.fromString("00000000-0000-0000-0000-000000000001")), 1, 2, Mercado.EUA,
                base.natureza(), base.quantidade(), base.posicao(), A, C, false);
        var antes = r(f1, f2, f3, f4); var depois = r(f1, temporaria, f2, f3, f4);
        assertTrue(validar(antes, depois, temporaria).valido());
        var pontos = validador.alinhar(antes, depois);
        assertEquals(List.of(f1.identidade(), temporaria.identidade(), f2.identidade(), f3.identidade(), f4.identidade()),
                pontos.stream().map(p -> p.evento().identidade()).toList());
        String[][] esperado = {{"100","0","0","100","0","0"}, {"100","0","0","95","0","5"},
                {"100","10","0","95","10","5"}, {"100","-10","0","95","-10","5"},
                {"100","0","0","95","0","5"}};
        for (int i = 0; i < esperado.length; i++) {
            var p = pontos.get(i);
            decimal(esperado[i][0], p.antes().saldo(A)); decimal(esperado[i][1], p.antes().saldo(B)); decimal(esperado[i][2], p.antes().saldo(C));
            decimal(esperado[i][3], p.depois().saldo(A)); decimal(esperado[i][4], p.depois().saldo(B)); decimal(esperado[i][5], p.depois().saldo(C));
        }
    }
    @Test void aceitaReducaoDeDeficitLegadoPorCompraRetroativa() {
        var f1 = compra(1, 1, A, "100"); var f2 = venda(2, 2, B, "10"); var f3 = compra(3, 3, B, "20");
        var nova = f(4, -1, 1, Natureza.COMPRA, B, "5");
        var antes = r(f1, f2, f3); var depois = r(nova, f1, f2, f3);
        assertTrue(validar(antes, depois, nova).valido());
        decimal("-5", depois.checkpoints().get(2).estado().saldo(B));
    }
    @Test void exemploDesignRejeitaNovoDeficitIntermediarioMesmoComFinalPositivo() {
        var f1 = compra(201, 1, A, "100"); var f2 = f(202, 1, 1, Natureza.COMPRA, B, "100");
        var f3 = f(203, 2, 1, Natureza.VENDA, A, "90");
        var t4 = c(204, 3, 0, "1", "2", B, A, "20"); var nova = t(1, 1, A, B, "20");
        var antes = r(f1, f2, f3, t4); var depois = r(f1, nova, f2, f3, t4);
        var erro = conflito(Codigo.CONFLITO_HISTORICO_QUANTITATIVO, validar(antes, depois, nova));
        assertEquals(f3.identidade(), erro.identidade()); assertEquals(A, erro.grupo());
        decimal("10", erro.evidencia().saldoAntes()); decimal("-10", erro.evidencia().saldoDepois());
        decimal("0", erro.evidencia().deficitAntes()); decimal("10", erro.evidencia().deficitDepois());
        decimal("10", depois.estadoFinal().saldo(A)); decimal("100", depois.estadoFinal().saldo(B));
        var pontos = validador.alinhar(antes, depois);
        String[][] tabela = {{"100","0","100","0"},{"100","0","80","20"},
                {"100","100","80","120"},{"10","100","-10","120"},{"30","80","10","100"}};
        for (int i = 0; i < tabela.length; i++) {
            decimal(tabela[i][0], pontos.get(i).antes().saldo(A)); decimal(tabela[i][1], pontos.get(i).antes().saldo(B));
            decimal(tabela[i][2], pontos.get(i).depois().saldo(A)); decimal(tabela[i][3], pontos.get(i).depois().saldo(B));
        }
    }
    @Test void rejeitaDeficitAgravadoPorEstorno() {
        var f1 = compra(1, 1, A, "100"); var original = t(1, 1, A, B, "10"); var f2 = venda(2, 2, B, "20");
        var erro = conflito(Codigo.CONFLITO_HISTORICO_QUANTITATIVO,
                validador.validar(r(f1, original, f2), r(f1, neutralizar(original), f2), estornar(original)));
        decimal("10", erro.evidencia().deficitAntes()); decimal("20", erro.evidencia().deficitDepois());
        assertEquals(f2.identidade(), erro.identidade());
    }
    @Test void rejeitaDeficitProlongadoNoCheckpointAntesRegularizado() {
        var f1 = compra(1, 1, A, "100"); var f2 = venda(2, 2, B, "10");
        var original = t(1, 2, A, B, "10"); var f3 = compra(3, 3, B, "20");
        var antes = r(f1, f2, original, f3); var depois = r(f1, f2, neutralizar(original), f3);
        var erro = conflito(Codigo.CONFLITO_HISTORICO_QUANTITATIVO, validador.validar(antes, depois, estornar(original)));
        assertEquals(original.identidade(), erro.identidade());
        decimal("0", erro.evidencia().deficitAntes()); decimal("10", erro.evidencia().deficitDepois());
        decimal("10", depois.estadoFinal().saldo(B));
    }
    @Test void pontoSomenteAnteriorTransportaSaldoPosteriorSemCompararIndices() {
        var f1 = compra(1, 1, A, "100"); var f2 = venda(2, 2, B, "10"); var f3 = compra(3, 3, B, "10");
        var antes = r(f1, f2, f3); var depois = r(f1, f2);
        var erro = conflito(Codigo.CONFLITO_HISTORICO_QUANTITATIVO, validador.compararDeficits(antes, depois));
        assertEquals(f3.identidade(), erro.identidade());
        decimal("0", erro.evidencia().saldoAntes()); decimal("-10", erro.evidencia().saldoDepois());
        assertEquals(3, validador.alinhar(antes, depois).size());
    }
    @Test void pontoSomentePosteriorParteDeSaldoZeroQuandoAnteriorVazio() {
        var nova = compra(1, 1, A, "1"); var antes = r(); var depois = r(nova);
        assertTrue(validar(antes, depois, nova).valido());
        decimal("0", validador.alinhar(antes, depois).get(0).antes().saldo(A));
    }
    @Test void estornoAtivoPreservaCheckpointEQuantidadeSemExigirSaldoAtualNoDestino() {
        var f1 = compra(1, 1, A, "100"); var original = t(1, 1, A, B, "40");
        var venda = venda(2, 2, B, "30"); var cobertura = f(3, -1, 1, Natureza.COMPRA, B, "30");
        var antes = r(cobertura, f1, original, venda); var depois = r(cobertura, f1, neutralizar(original), venda);
        assertTrue(validador.verificarPrecondicoes(antes, estornar(original)).valido());
        assertTrue(validador.validar(antes, depois, estornar(original)).valido());
        assertEquals(4, validador.alinhar(antes, depois).size());
        decimal("0", depois.estadoFinal().saldo(B));
    }
    @Test void duplicidadeEhDetectadaSemReceberOuProduzirReplayPosterior() {
        var original = neutralizar(t(1, 0, A, B, "10")); var antes = r(original);
        conflito(Codigo.ESTORNO_DUPLICADO, validador.verificarPrecondicoes(antes, estornar(original)));
        conflito(Codigo.ESTORNO_DUPLICADO, validador.validar(antes, null, estornar(original)));
    }
    @Test void duplicidadeTemPrioridadeSobreConsolidadoNegativo() {
        var original = neutralizar(t(1, 0, A, B, "1")); var f = venda(1, 1, A, "2");
        conflito(Codigo.ESTORNO_DUPLICADO, validador.validar(r(original, f), null, estornar(original)));
    }
    @Test void mesmosHistoricosComIntencoesDiferentesNaoInferemOperacao() {
        var original = neutralizar(t(1, 0, A, B, "10")); var historico = r(original);
        conflito(Codigo.ESTORNO_DUPLICADO, validador.validar(historico, historico, estornar(original)));
        assertThrows(IllegalArgumentException.class, () -> validador.validar(historico, historico, inserir(original)));
        assertTrue(validador.compararDeficits(historico, historico).valido());
    }
    @Test void originalInexistenteEhFalhaDeContratoSemFabricar404OuDuplicidade() {
        var original = t(1, 0, A, B, "1");
        assertThrows(IllegalArgumentException.class, () -> validador.verificarPrecondicoes(r(), estornar(original)));
    }
    @Test void novaVendaSemSaldoLocalEhDistintaDeVendaLegada() {
        var f1 = compra(1, 1, A, "100"); var nova = venda(2, 2, B, "10");
        var erro = conflito(Codigo.SALDO_CORRETORA_INSUFICIENTE, validar(r(f1), r(f1, nova), nova));
        decimal("0", erro.evidencia().saldoDisponivel()); decimal("10", erro.evidencia().quantidadeNecessaria());
        var compra = compra(3, 3, A, "1");
        assertTrue(validar(r(f1, nova), r(f1, nova, compra), compra).valido());
    }
    @Test void novaVendaSemCorretoraExigeSaldoDoGrupoNulo() {
        var f1 = compra(1, 1, A, "100"); var nova = venda(2, 2, SEM, "1");
        assertEquals(SEM, conflito(Codigo.SALDO_CORRETORA_INSUFICIENTE, validar(r(f1), r(f1, nova), nova)).grupo());
    }
    @Test void transferenciaSemSaldoLocalPrevaleceSobreDeficit() {
        var f1 = compra(1, 1, A, "50"); var nova = t(1, 1, A, B, "60");
        var erro = conflito(Codigo.SALDO_ORIGEM_INSUFICIENTE, validar(r(f1), r(f1, nova), nova));
        decimal("50", erro.evidencia().saldoDisponivel());
    }
    @Test void atribuicaoExigeSaldoNoGrupoSemCorretora() {
        var f1 = compra(1, 1, A, "100"); var nova = t(1, 1, SEM, B, "1");
        assertEquals(SEM, conflito(Codigo.SALDO_ORIGEM_INSUFICIENTE, validar(r(f1), r(f1, nova), nova)).grupo());
    }
    @Test void atribuicaoComSaldoExatoEhAceita() {
        var f1 = compra(1, 1, SEM, "1.000001"); var nova = t(1, 1, SEM, B, "1.000001");
        assertTrue(validar(r(f1), r(f1, nova), nova).valido());
    }
    @Test void consolidadoTemPrioridadeSobreSaldoLocal() {
        var f1 = compra(1, 1, A, "10"); var nova = venda(2, 2, B, "20");
        var erro = conflito(Codigo.POSICAO_INSUFICIENTE, validar(r(f1), r(f1, nova), nova));
        assertNull(erro.grupo()); decimal("-10", erro.evidencia().saldoDepois());
    }
    @Test void consolidadoFuturoPrevaleceSobreSaldoLocalInsuficienteMaisCedo() {
        var f1 = compra(1, 1, A, "100"); var legada = venda(3, 3, A, "90"); var nova = venda(2, 2, B, "20");
        var erro = conflito(Codigo.POSICAO_INSUFICIENTE, validar(r(f1, legada), r(f1, nova, legada), nova));
        assertEquals(legada.identidade(), erro.identidade());
    }
    @Test void novaVendaComSaldoExatoEncerraPosicaoSemConflito() {
        var f1 = compra(1, 1, A, "0.123456"); var nova = venda(2, 2, A, "0.123456");
        assertTrue(validar(r(f1), r(f1, nova), nova).valido());
    }
    @Test void consolidadoLegadoNegativoContinuaErroMesmoSeCandidataRegulariza() {
        var legado = venda(1, 1, A, "10"); var nova = f(2, -1, 1, Natureza.COMPRA, A, "20");
        conflito(Codigo.POSICAO_INSUFICIENTE, validar(r(legado), r(nova, legado), nova));
    }
    @Test void custodiaAtivaInvalidadaPrevaleceSobreDeficitDiferencial() {
        var f1 = compra(1, 1, A, "100"); var original = t(1, 2, A, B, "80"); var nova = venda(2, 2, A, "30");
        var erro = conflito(Codigo.MOVIMENTACAO_CUSTODIA_INVALIDADA, validar(r(f1, original), r(f1, nova, original), nova));
        assertEquals(original.identidade(), erro.identidade());
        decimal("70", erro.evidencia().saldoDisponivel()); decimal("80", erro.evidencia().quantidadeNecessaria());
    }
    @Test void estornoQueInvalidaTransferenciaDependenteEhRejeitado() {
        var f1 = compra(1, 1, A, "100"); var original = t(1, 1, A, B, "40"); var t2 = t(2, 2, B, C, "30");
        var erro = conflito(Codigo.MOVIMENTACAO_CUSTODIA_INVALIDADA,
                validador.validar(r(f1, original, t2), r(f1, neutralizar(original), t2), estornar(original)));
        assertEquals(t2.identidade(), erro.identidade());
    }
    @Test void originaisEstornadasNaoSaoRevalidadasComoDebitos() {
        var original = neutralizar(t(1, 0, A, B, "100")); var nova = compra(1, 1, C, "1");
        assertTrue(validar(r(original), r(original, nova), nova).valido());
    }
    @Test void saldoLocalTemPrioridadeSobreCustodiaAtivaInvalidada() {
        var f1 = compra(1, 1, B, "100"); var f2 = compra(2, 2, A, "50");
        var original = t(1, 3, A, C, "50"); var nova = venda(3, 3, A, "60");
        conflito(Codigo.SALDO_CORRETORA_INSUFICIENTE, validar(r(f1, f2, original), r(f1, f2, nova, original), nova));
    }
    @Test void prioridadeDeClasseSuperaConflitoDiferencialMaisCedo() {
        var f1 = compra(1, 1, A, "100"); var vendaLegada = venda(2, 2, B, "10");
        var original = t(1, 1, A, B, "20"); var t2 = t(2, 3, B, C, "5");
        var erro = conflito(Codigo.MOVIMENTACAO_CUSTODIA_INVALIDADA,
                validador.validar(r(f1, original, vendaLegada, t2), r(f1, neutralizar(original), vendaLegada, t2), estornar(original)));
        assertEquals(t2.identidade(), erro.identidade());
    }
    @Test void escolhePrimeiraCustodiaInvalidadaEmOrdemExataNaoPorId() {
        var f1 = compra(1, 1, A, "100"); var nova = venda(2, 2, A, "90");
        var primeira = c(99, 0, 2, "2", "3", A, B, "20");
        var segunda = c(1, 0, 2, "10", "11", A, C, "20");
        var erro = conflito(Codigo.MOVIMENTACAO_CUSTODIA_INVALIDADA, validar(r(f1, primeira, segunda), r(f1, nova, primeira, segunda), nova));
        assertEquals(primeira.identidade(), erro.identidade());
    }
    @Test void deficitEmBJamaisEhCompensadoPorSaldoEmA() {
        var f1 = compra(1, 1, A, "1000"); var f2 = venda(2, 2, B, "1");
        var erro = conflito(Codigo.CONFLITO_HISTORICO_QUANTITATIVO, validador.compararDeficits(r(f1), r(f1, f2)));
        assertEquals(B, erro.grupo()); decimal("1", erro.evidencia().deficitDepois());
    }
    @Test void diagnosticoIndependeDaOrdemDosMapas() {
        var evento = compra(1, 1, A, "100");
        var antes = r(evento);
        var original = r(evento, venda(2, 2, B, "10"), venda(3, 3, SEM, "10"));
        for (List<Grupo> ordem : List.of(List.of(SEM, B, A), List.of(A, SEM, B), List.of(B, A, SEM))) {
            var checkpoints = new ArrayList<Checkpoint>();
            for (var checkpoint : original.checkpoints()) {
                var saldos = new java.util.LinkedHashMap<Grupo, BigDecimal>();
                for (var grupo : ordem) {
                    if (checkpoint.estado().saldos().containsKey(grupo)) saldos.put(grupo, checkpoint.estado().saldo(grupo));
                }
                var estado = new Estado(saldos, checkpoint.estado().quantidadeConsolidada(), checkpoint.estado().primeirosNegativos());
                checkpoints.add(new Checkpoint(checkpoint.evento(), estado));
            }
            var depois = new Resultado(checkpoints, checkpoints.get(checkpoints.size() - 1).estado());
            assertEquals(B, conflito(Codigo.CONFLITO_HISTORICO_QUANTITATIVO, validador.compararDeficits(antes, depois)).grupo());
        }
    }
    @Test void colecoesDeCheckpointsEmOrdemDiferenteNaoMudamDiagnostico() {
        var f1 = compra(1, 1, A, "100"); var f2 = venda(2, 2, B, "10"); var nova = t(1, 1, A, C, "5");
        var antes = r(f1, f2); var depois = r(f1, nova, f2);
        var ca = new ArrayList<>(antes.checkpoints()); var cd = new ArrayList<>(depois.checkpoints());
        Collections.reverse(ca); Collections.reverse(cd);
        var esperado = validar(antes, depois, nova);
        assertEquals(esperado, validar(new Resultado(ca, antes.estadoFinal()), new Resultado(cd, depois.estadoFinal()), nova));
    }
    @Test void decimaisExatosNaoSaoArredondadosNaComparacao() {
        var f1 = compra(1, 1, A, "1.000001"); var nova = t(1, 1, A, B, "1.000002");
        var erro = conflito(Codigo.SALDO_ORIGEM_INSUFICIENTE, validar(r(f1), r(f1, nova), nova));
        assertEquals(new BigDecimal("1.000001"), erro.evidencia().saldoDisponivel());
        assertEquals(new BigDecimal("1.000002"), erro.evidencia().quantidadeNecessaria());
        decimal("0.000001", erro.evidencia().deficitDepois());
    }
    @Test void intencaoDeveTerDominioCorretoENaoFabricaId() {
        assertThrows(IllegalArgumentException.class, () -> new Intencao(TipoIntencao.VENDA, Identidade.persistida(Dominio.CUSTODIA, 1)));
        assertThrows(IllegalArgumentException.class, () -> new Intencao(TipoIntencao.ESTORNO, Identidade.temporaria(Dominio.CUSTODIA, UUID.randomUUID())));
        assertThrows(IllegalArgumentException.class, () -> new Intencao(null, null));
    }
    @Test void rejeitaIntencaoDiferenteDaNaturezaSimulada() {
        var f1 = compra(1, 1, A, "100"); var nova = venda(2, 2, A, "1");
        assertThrows(IllegalArgumentException.class, () -> validador.validar(r(f1), r(f1, nova), new Intencao(TipoIntencao.COMPRA, nova.identidade())));
    }
    @Test void rejeitaCandidataAusenteOuJaExistente() {
        var f1 = compra(1, 1, A, "100"); var nova = venda(2, 2, A, "1");
        assertThrows(IllegalArgumentException.class, () -> validar(r(f1), r(f1), nova));
        assertThrows(IllegalArgumentException.class, () -> validar(r(f1), r(f1), f1));
    }
    @Test void rejeitaAlteracaoDeEventoHistoricoENaoNeutralizacaoDoAlvo() {
        var f1 = compra(1, 1, A, "100"); var original = t(1, 1, A, B, "40");
        assertThrows(IllegalArgumentException.class, () -> validador.validar(r(f1, original), r(f1, original), estornar(original)));
        var adulterada = compra(1, 1, A, "101"); var nova = compra(2, 2, C, "1");
        assertThrows(IllegalArgumentException.class, () -> validar(r(f1), r(adulterada, nova), nova));
    }
    @Test void rejeitaRemocaoOuInclusaoExtraNaSimulacao() {
        var f1 = compra(1, 1, A, "100"); var f2 = compra(2, 2, B, "1"); var nova = compra(3, 3, C, "1");
        assertThrows(IllegalArgumentException.class, () -> validar(r(f1, f2), r(f1, nova), nova));
        assertThrows(IllegalArgumentException.class, () -> validar(r(f1), r(f1, f2, nova), nova));
    }
    @Test void rejeitaOriginalReposicionadaNoEstorno() {
        var f1 = compra(1, 1, A, "100"); var original = t(1, 1, A, B, "40");
        var movida = neutralizar(c(1, 0, 1, "2", "3", A, B, "40"));
        assertThrows(IllegalArgumentException.class, () -> validador.validar(r(f1, original), r(f1, movida), estornar(original)));
        assertThrows(IllegalArgumentException.class, () -> validador.alinhar(r(f1, original), r(f1, movida)));
    }
    @Test void rejeitaColisaoDeIdentidadesNaUniao() {
        var a = compra(1, 1, A, "1"); var b = compra(2, 1, A, "1");
        assertThrows(IllegalArgumentException.class, () -> validador.alinhar(r(a), r(b)));
    }
    @Test void rejeitaMisturaDeContextosEntreHistoricos() {
        var a = compra(1, 1, A, "1");
        var outro = new EventoQuantitativo(a.identidade(), 9, 2, Mercado.EUA, a.natureza(), a.quantidade(), a.posicao(), A, null, false);
        assertThrows(IllegalArgumentException.class, () -> validador.alinhar(r(a), r(outro)));
    }
    @Test void posteriorObrigatorioParaIntencaoAdmissivel() {
        var original = t(1, 0, A, B, "1");
        assertThrows(IllegalArgumentException.class, () -> validador.validar(r(original), null, estornar(original)));
        assertThrows(IllegalArgumentException.class, () -> validador.validar(r(), r(), null));
    }
    @Test void imutabilidadeDosResultadosEntradasEEvidencias() {
        var f1 = compra(1, 1, A, "100"); var nova = venda(2, 2, B, "1");
        var antes = r(f1); var depois = r(f1, nova); var copiaA = List.copyOf(antes.checkpoints()); var copiaD = List.copyOf(depois.checkpoints());
        var erro = conflito(Codigo.SALDO_CORRETORA_INSUFICIENTE, validar(antes, depois, nova));
        var pontos = validador.alinhar(antes, depois);
        assertThrows(UnsupportedOperationException.class, pontos::clear);
        assertThrows(UnsupportedOperationException.class, () -> pontos.get(0).antes().saldos().clear());
        assertEquals(copiaA, antes.checkpoints()); assertEquals(copiaD, depois.checkpoints());
        assertEquals(erro, validar(antes, depois, nova).conflito());
        for (Class<?> tipo : new Class<?>[]{Intencao.class, Evidencia.class, Conflito.class, ResultadoValidacao.class, PontoAlinhado.class}) {
            assertTrue(tipo.isRecord()); assertEquals(0, tipo.getAnnotations().length);
        }
    }
}
