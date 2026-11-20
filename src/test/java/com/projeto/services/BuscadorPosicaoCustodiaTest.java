package com.projeto.services;

import com.projeto.entities.Mercado;
import com.projeto.entities.PosicaoRacionalCustodia;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static com.projeto.services.EventoQuantitativo.*;
import static com.projeto.services.BuscadorPosicaoCustodia.*;
import static com.projeto.services.ValidadorHistoricoQuantitativo.Intencao;
import static com.projeto.services.ValidadorHistoricoQuantitativo.TipoIntencao;
import static org.junit.jupiter.api.Assertions.*;

class BuscadorPosicaoCustodiaTest {
    private static final LocalDate DATA = LocalDate.of(2026, 10, 1);
    private static final Grupo A = Grupo.corretora(1), B = Grupo.corretora(2), C = Grupo.corretora(3), DGR = Grupo.corretora(4);
    private static final Grupo SEM = Grupo.semCorretora();
    private static final BigInteger D = BigInteger.TEN.pow(512).subtract(BigInteger.ONE);
    private static final Identidade ID = Identidade.temporaria(Dominio.CUSTODIA,
            UUID.fromString("00000000-0000-0000-0000-000000000041"));
    private final BuscadorPosicaoCustodia buscador = new BuscadorPosicaoCustodia();
    private EventoQuantitativo f(long id, int dia, int ordem, Natureza natureza, Grupo grupo, String q) {
        return new EventoQuantitativo(Identidade.persistida(Dominio.FINANCEIRO, id), 1, 2, Mercado.EUA,
                natureza, new BigDecimal(q), PosicaoCronologicaQuantitativa.financeira(DATA.plusDays(dia), ordem), grupo, null, false);
    }
    private EventoQuantitativo compra(long id, int dia, int ordem, Grupo grupo, String q) { return f(id, dia, ordem, Natureza.COMPRA, grupo, q); }
    private EventoQuantitativo t(long id, int ancora, String n, String d, Grupo origem, Grupo destino, String q, boolean neutralizada) {
        return new EventoQuantitativo(Identidade.persistida(Dominio.CUSTODIA, id), 1, 2, Mercado.EUA,
                origem.equals(SEM) ? Natureza.ATRIBUICAO : Natureza.TRANSFERENCIA, new BigDecimal(q),
                PosicaoCronologicaQuantitativa.custodia(DATA, ancora, PosicaoRacionalCustodia.deTexto(n, d)), origem, destino, neutralizada);
    }
    private EventoQuantitativo fundos() { return compra(1, -1, 1, A, "100"); }
    private DadosCandidata dados(Grupo origem, Grupo destino, String quantidade) {
        return new DadosCandidata(1, 2, Mercado.EUA, DATA, new BigDecimal(quantidade), origem, destino);
    }
    private Intencao intencao(Grupo origem) {
        return new Intencao(origem.equals(SEM) ? TipoIntencao.ATRIBUICAO : TipoIntencao.TRANSFERENCIA, ID);
    }
    private ResultadoBusca buscar(List<EventoQuantitativo> eventos, Grupo origem, Grupo destino, String q) {
        return buscador.buscar(eventos, intencao(origem), dados(origem, destino, q));
    }
    private void posicao(ResultadoBusca r, int ancora, String fracao) {
        assertTrue(r.encontrada()); assertNull(r.diagnostico());
        assertEquals(ancora, r.candidata().posicao().ancoraFinanceira());
        assertEquals(fracao, r.posicaoPersistivel().toString());
        assertEquals(PosicaoRacionalTransitoria.dePersistivel(r.posicaoPersistivel()), r.candidata().posicao().fracao());
        assertEquals(DATA, r.candidata().posicao().dataEfetiva());
        assertEquals(ID, r.candidata().identidade()); assertNull(r.candidata().identidade().idPersistido());
    }
    @Test void primeiroEspacoValidoEmDataSemOperacoesUsaAncoraZero() {
        var r = buscar(List.of(fundos()), A, B, "10"); posicao(r, 0, "1/2"); assertEquals(1, r.espacosAvaliados());
    }
    @Test void primeiroEspacoSemSaldoESegundoDepoisDaCompraValido() {
        var r = buscar(List.of(compra(99, 0, 1, A, "100")), A, B, "60");
        posicao(r, 1, "1/2"); assertEquals(2, r.espacosAvaliados());
    }
    @Test void insereAntesDaPrimeiraCustodiaSemReutilizarChave() {
        var existente = t(1, 0, "1", "2", A, B, "50", false);
        var r = buscar(List.of(fundos(), existente), A, C, "10"); posicao(r, 0, "1/3");
        assertEquals("1/2", existente.posicao().fracao().toString());
    }
    @Test void insereEntreCustodiasEmCadeiaPreservandoChaves() {
        var t1 = t(99, 0, "1", "2", A, B, "60", false); var t2 = t(1, 0, "2", "3", B, C, "20", false);
        var r = buscar(List.of(fundos(), t1, t2), B, DGR, "40"); posicao(r, 0, "3/5"); assertEquals(2, r.espacosAvaliados());
        assertEquals("1/2", t1.posicao().fracao().toString()); assertEquals("2/3", t2.posicao().fracao().toString());
    }
    @Test void insereDepoisDaUltimaCustodia() {
        var t1 = t(1, 0, "1", "2", A, B, "60", false);
        var r = buscar(List.of(fundos(), t1), B, C, "40"); posicao(r, 0, "2/3"); assertEquals(2, r.espacosAvaliados());
    }
    @Test void atribuicaoConsomeSomenteGrupoSemCorretora() {
        var r = buscar(List.of(compra(1, -1, 1, SEM, "100")), SEM, B, "40");
        posicao(r, 0, "1/2"); assertEquals(Natureza.ATRIBUICAO, r.candidata().natureza()); assertEquals(SEM, r.candidata().grupoOrigem());
    }
    @Test void novaOperacaoNoDiaPermaneceDepoisDaCustodiaDeAncoraZero() {
        var fonte = fundos(); var r = buscar(List.of(fonte), A, B, "10");
        var posterior = compra(2, 0, 1, C, "1");
        assertTrue(r.candidata().posicao().compareTo(posterior.posicao()) < 0);
        var replay = new MotorReplayQuantitativo().reproduzir(List.of(fonte, r.candidata(), posterior));
        assertSame(r.candidata(), replay.checkpoints().get(1).evento()); posicao(r, 0, "1/2");
    }
    @Test void operacaoAnexadaAposUltimaOrdemNaoReposicionaCustodia() {
        var financeira = compra(9, 0, 7, A, "100"); var r = buscar(List.of(financeira), A, B, "10");
        posicao(r, 7, "1/2"); assertTrue(r.candidata().posicao().compareTo(compra(1, 0, 8, C, "1").posicao()) < 0);
        assertEquals(7, financeira.posicao().ancoraFinanceira());
    }
    @Test void originalEstornadaAindaDivideEspacos() {
        var estornada = t(1, 0, "1", "2", A, B, "50", true);
        var r = buscar(List.of(fundos(), estornada), A, C, "10"); posicao(r, 0, "1/3"); assertTrue(estornada.neutralizado());
    }
    @Test void primeiroEspacoComSaldoInvalidaFuturoMasOutroDepoisEhValido() {
        var t1 = t(1, 0, "1", "3", A, B, "80", false); var t2 = t(2, 0, "2", "3", B, A, "60", false);
        var r = buscar(List.of(fundos(), t1, t2), A, C, "50"); posicao(r, 0, "3/4"); assertEquals(3, r.espacosAvaliados());
    }
    @Test void preservaVendaLegadaComDeficitInalterado() {
        var f1 = compra(101, 0, 1, A, "100"); var f2 = compra(102, 0, 2, B, "10");
        var f3 = f(103, 0, 3, Natureza.VENDA, B, "20"); var f4 = compra(104, 0, 4, B, "10");
        var r = buscar(List.of(f1, f2, f3, f4), A, C, "5"); posicao(r, 1, "1/2"); assertEquals(2, r.espacosAvaliados());
    }
    @Test void saldosFinaisPositivosNaoOcultamNovoDeficitFuturo() {
        var f1 = compra(201, 0, 1, A, "100"); var f2 = compra(202, 1, 1, B, "100");
        var f3 = f(203, 2, 1, Natureza.VENDA, A, "90");
        var t4 = new EventoQuantitativo(Identidade.persistida(Dominio.CUSTODIA, 204), 1, 2, Mercado.EUA,
                Natureza.TRANSFERENCIA, new BigDecimal("20"), PosicaoCronologicaQuantitativa.custodia(DATA.plusDays(3), 0,
                PosicaoRacionalCustodia.deTexto("1", "2")), B, A, false);
        var r = buscar(List.of(f1, f2, f3, t4), A, B, "20");
        assertFalse(r.encontrada()); assertEquals(Codigo.POSICAO_CUSTODIA_INDISPONIVEL, r.diagnostico().codigo());
        assertEquals(ValidadorHistoricoQuantitativo.Codigo.CONFLITO_HISTORICO_QUANTITATIVO, r.diagnostico().causa().codigo());
        assertEquals(f3.identidade(), r.diagnostico().causa().identidade());
    }
    @Test void agravamentoLegadoEhCausaDaIndisponibilidade() {
        var f1 = compra(2, 0, 1, B, "10"); var f2 = f(3, 0, 2, Natureza.VENDA, B, "20"); var f3 = compra(4, 0, 3, B, "10");
        var r = buscar(List.of(fundos(), f1, f2, f3), B, C, "5");
        assertEquals(Codigo.POSICAO_CUSTODIA_INDISPONIVEL, r.diagnostico().codigo());
        var evidencia = r.diagnostico().causa().evidencia();
        assertEquals(0, new BigDecimal("10").compareTo(evidencia.deficitAntes()));
        assertEquals(0, new BigDecimal("15").compareTo(evidencia.deficitDepois()));
    }
    @Test void semSaldoEmTodosEspacosRetornaDiagnosticoLocal() {
        var r = buscar(List.of(compra(1, 0, 1, A, "10")), B, C, "1");
        assertFalse(r.encontrada()); assertEquals(Codigo.SALDO_ORIGEM_INSUFICIENTE, r.diagnostico().codigo());
        assertEquals(2, r.espacosAvaliados()); assertEquals(0, r.diagnostico().posicao().ancoraFinanceira());
        assertNull(r.candidata()); assertNull(r.posicaoPersistivel());
    }
    @Test void nenhumEspacoValidoPreservaCausaDoPrimeiroComSaldo() {
        var f1 = compra(1, 0, 1, A, "100"); var venda = f(2, 0, 2, Natureza.VENDA, A, "80");
        var r = buscar(List.of(f1, venda), A, B, "50");
        assertEquals(Codigo.POSICAO_CUSTODIA_INDISPONIVEL, r.diagnostico().codigo());
        assertEquals(1, r.diagnostico().posicao().ancoraFinanceira());
        assertEquals(venda.identidade(), r.diagnostico().causa().identidade()); assertEquals(3, r.espacosAvaliados());
    }
    @Test void custodiaInvalidadaEhCausaSemSerSubstituidaPorPrimeiroEspacoSemSaldo() {
        var f1 = compra(1, 0, 1, A, "100"); var original = t(1, 1, "1", "2", A, B, "80", false);
        var r = buscar(List.of(f1, original), A, C, "50");
        assertEquals(Codigo.POSICAO_CUSTODIA_INDISPONIVEL, r.diagnostico().codigo());
        assertEquals(ValidadorHistoricoQuantitativo.Codigo.MOVIMENTACAO_CUSTODIA_INVALIDADA, r.diagnostico().causa().codigo());
        assertEquals(original.identidade(), r.diagnostico().causa().identidade());
        assertEquals("1/3", r.diagnostico().posicao().fracao().toString());
    }
    @Test void ordenaRacionaisAdversariaisMatematicamente() {
        var t1 = t(99, 0, "2", "3", A, B, "60", false); var t2 = t(1, 0, "10", "11", B, C, "20", false);
        var r = buscar(List.of(t2, fundos(), t1), B, DGR, "40"); posicao(r, 0, "6/7");
    }
    @Test void primeiroEspaco513SemSaldoNaoImpedeSegundoValido() {
        var original = t(1, 0, "1", D.toString(), A, B, "60", false);
        var r = buscar(List.of(fundos(), original), B, C, "40");
        assertTrue(r.encontrada()); assertEquals(2, r.espacosAvaliados());
        assertEquals("1/" + BigInteger.TEN.pow(512).divide(BigInteger.TWO), r.posicaoPersistivel().toString());
        assertTrue(original.posicao().compareTo(r.candidata().posicao()) < 0);
    }
    @Test void primeiroEspaco513ComSaldoMasFuturoInvalidoNaoRetornaLimite() {
        var t1 = t(1, 0, "1", D.toString(), A, B, "80", false); var t2 = t(2, 0, "1", "2", B, A, "60", false);
        var r = buscar(List.of(fundos(), t1, t2), A, C, "50");
        posicao(r, 0, "2/3"); assertEquals(3, r.espacosAvaliados());
    }
    @Test void primeiroEspacoQuantitativamenteValido513RetornaLimiteSemFallback() {
        var fonte = fundos(); var original = t(1, 0, "1", D.toString(), A, B, "50", false);
        var r = buscar(List.of(fonte, original), A, C, "10");
        assertFalse(r.encontrada()); assertEquals(Codigo.LIMITE_POSICAO_CUSTODIA, r.diagnostico().codigo());
        assertEquals(1, r.espacosAvaliados()); assertNull(r.diagnostico().causa());
        assertEquals(513, r.diagnostico().posicao().fracao().getDenominador().toString().length());
        // Há espaço posterior válido e persistível, mas a busca não pode escolhê-lo.
        var outraFracao = original.posicao().fracao().entre(PosicaoRacionalTransitoria.FIM);
        outraFracao.paraPersistivel();
        var alternativa = new EventoQuantitativo(ID, 1, 2, Mercado.EUA, Natureza.TRANSFERENCIA, new BigDecimal("10"),
                PosicaoCronologicaQuantitativa.custodiaTransitoria(DATA, 0, outraFracao), A, C, false);
        var motor = new MotorReplayQuantitativo();
        assertTrue(new ValidadorHistoricoQuantitativo().validar(motor.reproduzir(List.of(fonte, original)),
                motor.reproduzir(List.of(fonte, original, alternativa)), intencao(A)).valido());
    }
    @Test void denominadorDe512DigitosEhConvertidoCorretamente() {
        var original = t(1, 0, "1", D.subtract(BigInteger.ONE).toString(), A, B, "50", false);
        var r = buscar(List.of(fundos(), original), A, C, "10");
        posicao(r, 0, "1/" + D); assertEquals(512, r.posicaoPersistivel().getDenominadorTexto().length());
    }
    @Test void independenciaDaOrdemDeEntradaEDeterminismo() {
        var eventos = new ArrayList<>(List.of(fundos(), t(99, 0, "1", "2", A, B, "60", false),
                t(1, 0, "2", "3", B, C, "20", false)));
        var esperado = buscar(eventos, B, DGR, "40");
        for (int i = 0; i < eventos.size(); i++) {
            Collections.rotate(eventos, 1); assertEquals(esperado, buscar(eventos, B, DGR, "40"));
        }
        Collections.reverse(eventos); assertEquals(esperado, buscar(eventos, B, DGR, "40"));
    }
    @Test void preservaEntradasIdentidadesEPosicoesEForneceResultadoImutavel() {
        var t1 = t(1, 0, "1", "2", A, B, "60", false); var fonte = fundos();
        var eventos = new ArrayList<>(List.of(t1, fonte)); var antes = List.copyOf(eventos);
        var r = buscar(eventos, B, C, "40");
        assertEquals(antes, eventos); assertSame(t1, eventos.get(0));
        assertEquals("1/2", t1.posicao().fracao().toString()); assertEquals(1L, t1.identidade().idPersistido());
        eventos.clear(); posicao(r, 0, "2/3");
        for (Class<?> tipo : new Class<?>[]{DadosCandidata.class, Diagnostico.class, ResultadoBusca.class}) {
            assertTrue(tipo.isRecord()); assertEquals(0, tipo.getAnnotations().length);
        }
    }
    @Test void usaSomenteAncoraZeroEOrdensFinanceirasExistentesSemRenumero() {
        var f1 = compra(99, 0, 3, A, "10"); var f2 = compra(1, 0, 7, A, "1");
        var r = buscar(List.of(f2, f1), A, B, "5"); posicao(r, 3, "1/2");
        assertEquals(3, f1.posicao().ancoraFinanceira()); assertEquals(7, f2.posicao().ancoraFinanceira());
    }
    @Test void ausenciaTotalDeEventosRetornaSaldoInsuficiente() {
        var r = buscar(List.of(), A, B, "1"); assertEquals(Codigo.SALDO_ORIGEM_INSUFICIENTE, r.diagnostico().codigo());
        assertEquals(1, r.espacosAvaliados());
    }
    @Test void historicoConsolidadoInvalidoNaoEhTratadoComoEspacosDescartaveis() {
        var venda = f(1, -1, 1, Natureza.VENDA, A, "1");
        var r = buscar(List.of(venda), A, B, "1");
        assertEquals(Codigo.POSICAO_INSUFICIENTE, r.diagnostico().codigo()); assertEquals(1, r.espacosAvaliados());
    }
    @Test void diagnosticoRepresentativoEhDeterministicoEntreChamadas() {
        var eventos = List.of(compra(1, 0, 1, A, "100"), f(2, 0, 2, Natureza.VENDA, A, "80"));
        assertEquals(buscar(eventos, A, B, "50"), buscar(List.of(eventos.get(1), eventos.get(0)), A, B, "50"));
    }
    @Test void falhasEstruturaisDeEntradaNaoVirAMDiagnosticosQuantitativos() {
        assertThrows(IllegalArgumentException.class, () -> buscar(null, A, B, "1"));
        assertThrows(IllegalArgumentException.class, () -> buscar(java.util.Arrays.asList((EventoQuantitativo) null), A, B, "1"));
        assertThrows(IllegalArgumentException.class, () -> buscar(List.of(fundos(), fundos()), A, B, "1"));
        assertThrows(IllegalArgumentException.class, () -> buscar(List.of(fundos()), A, B, "0"));
        assertThrows(IllegalArgumentException.class, () -> buscar(List.of(fundos()), A, A, "1"));
    }
    @Test void rejeitaIntencaoNaoRelacionadaACustodia() {
        var financeira = new Intencao(TipoIntencao.COMPRA, Identidade.temporaria(Dominio.FINANCEIRO, UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> buscador.buscar(List.of(fundos()), financeira, dados(A, B, "1")));
        assertThrows(IllegalArgumentException.class, () -> buscador.buscar(List.of(fundos()), null, dados(A, B, "1")));
    }
    @Test void identidadeJaExistenteEhFalhaDeContrato() {
        var existente = t(1, 0, "1", "2", A, B, "10", false);
        var repetida = new Intencao(TipoIntencao.TRANSFERENCIA, existente.identidade());
        assertThrows(IllegalArgumentException.class, () -> buscador.buscar(List.of(fundos(), existente), repetida, dados(A, B, "1")));
    }
    @Test void rejeitaContextoDiferenteSemFiltrarEventos() {
        var fora = new DadosCandidata(9, 2, Mercado.EUA, DATA, BigDecimal.ONE, A, B);
        assertThrows(IllegalArgumentException.class, () -> buscador.buscar(List.of(fundos()), intencao(A), fora));
    }
    @Test void rejeitaAncoraSemOperacaoFinanceiraDaData() {
        var orfa = t(1, 4, "1", "2", A, B, "10", false);
        assertThrows(IllegalArgumentException.class, () -> buscar(List.of(fundos(), orfa), A, C, "1"));
    }
    @Test void preservaUltimaCasaDecimalNaQuantidadeDaCandidata() {
        var r = buscar(List.of(fundos()), A, B, "0.000001"); posicao(r, 0, "1/2");
        assertEquals(new BigDecimal("0.000001"), r.candidata().quantidade());
    }
}
