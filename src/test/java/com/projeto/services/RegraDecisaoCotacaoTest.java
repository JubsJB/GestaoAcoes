package com.projeto.services;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.ReferenciaTipoCotacao;
import com.projeto.integrations.cotacao.CotacaoCandidata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.projeto.entities.ReferenciaTipoCotacao.*;
import static com.projeto.services.RegraDecisaoCotacao.Efeito.*;
import static com.projeto.services.RegraDecisaoCotacao.Motivo.*;
import static com.projeto.services.RegraDecisaoCotacao.Resultado.*;
import static org.junit.jupiter.api.Assertions.*;

class RegraDecisaoCotacaoTest {
    private static final BigDecimal PRECO = new BigDecimal("100.000000");
    private static final OffsetDateTime OBTENCAO = OffsetDateTime.parse("2026-10-03T15:05:00Z");
    private static final OffsetDateTime INSTANTE_MERCADO = OffsetDateTime.parse("2026-10-02T15:00:00Z");
    private static final LocalDate DIA = LocalDate.of(2026, 10, 2);
    private static final OffsetDateTime LEGADO = OffsetDateTime.parse("2099-01-01T00:00:00Z");

    static Stream<Arguments> matriz() {
        List<Arguments> casos = new ArrayList<>();
        for (ReferenciaTipoCotacao tipo : List.of(DATA, INSTANTE))
            for (int referencia : new int[]{-1, 0, 1})
                for (boolean mesmoPreco : new boolean[]{true, false})
                    for (int obtencao : new int[]{-1, 0, 1})
                        casos.add(Arguments.of(tipo, referencia, mesmoPreco, obtencao));
        return casos.stream();
    }

    @ParameterizedTest
    @MethodSource("matriz")
    void matrizDeReferenciaPrecoEObtencao(ReferenciaTipoCotacao tipo, int referencia,
                                        boolean mesmoPreco, int obtencao) {
        Acao atual = estado(tipo, PRECO, OBTENCAO);
        CotacaoCandidata candidata = candidata(tipo, referencia,
                mesmoPreco ? PRECO : new BigDecimal("102"), OBTENCAO.plusSeconds(obtencao));
        var decisao = decidirSemMutar(atual, candidata);
        if (referencia > 0) {
            assertDecisao(decisao, ATUALIZADA, REFERENCIA_POSTERIOR, SUBSTITUIR_OBSERVACAO,
                    candidata.cotacaoObtidaEm());
        } else if (referencia < 0) {
            assertDecisao(decisao, SEM_NOVA_COTACAO, REFERENCIA_ANTERIOR, PRESERVAR_OBSERVACAO, OBTENCAO);
        } else if (!mesmoPreco) {
            assertDecisao(decisao, FALHA, CONFLITO_NA_MESMA_REFERENCIA, PRESERVAR_OBSERVACAO, OBTENCAO);
        } else {
            assertDecisao(decisao, SEM_NOVA_COTACAO, MESMA_OBSERVACAO,
                    obtencao > 0 ? AVANCAR_SOMENTE_OBTENCAO : PRESERVAR_OBSERVACAO,
                    obtencao > 0 ? candidata.cotacaoObtidaEm() : OBTENCAO);
        }
    }

    static Stream<Arguments> baseline() {
        List<Arguments> casos = new ArrayList<>();
        for (ReferenciaTipoCotacao tipo : List.of(DATA, INSTANTE))
            for (boolean mesmoPreco : new boolean[]{true, false})
                for (int obtencao : new int[]{-1, 0, 1})
                    casos.add(Arguments.of(tipo, mesmoPreco, obtencao));
        return casos.stream();
    }

    @ParameterizedTest
    @MethodSource("baseline")
    void desconhecidaEstabeleceBaselineComObtencaoPropria(ReferenciaTipoCotacao tipo,
                                                        boolean mesmoPreco, int obtencao) {
        Acao atual = estado(DESCONHECIDA, PRECO, OBTENCAO);
        CotacaoCandidata candidata = candidata(tipo, 0,
                mesmoPreco ? PRECO : new BigDecimal("102"), OBTENCAO.plusSeconds(obtencao));
        assertDecisao(decidirSemMutar(atual, candidata), ATUALIZADA, REFERENCIA_INICIAL_ESTABELECIDA,
                SUBSTITUIR_OBSERVACAO, candidata.cotacaoObtidaEm());
    }

    @ParameterizedTest
    @EnumSource(value = ReferenciaTipoCotacao.class, names = {"DATA", "INSTANTE"})
    void tiposIncomparaveisNaoUsamObtencaoComoDesempate(ReferenciaTipoCotacao atualTipo) {
        Acao atual = estado(atualTipo, PRECO, OBTENCAO);
        var candidata = candidata(atualTipo == DATA ? INSTANTE : DATA, 100,
                new BigDecimal("102"), OBTENCAO.plusYears(1));
        assertDecisao(decidirSemMutar(atual, candidata), FALHA, TIPO_REFERENCIA_INCOMPATIVEL,
                PRESERVAR_OBSERVACAO, OBTENCAO);
    }

    @ParameterizedTest
    @EnumSource(ReferenciaTipoCotacao.class)
    void candidataDesconhecidaNaoAtualizaNemBaseline(ReferenciaTipoCotacao atualTipo) {
        var atual = estado(atualTipo, PRECO, OBTENCAO);
        var candidata = candidata(DESCONHECIDA, 0, new BigDecimal("102"), OBTENCAO.plusDays(1));
        assertDecisao(decidirSemMutar(atual, candidata), FALHA, REFERENCIA_TEMPORAL_INVALIDA,
                PRESERVAR_OBSERVACAO, OBTENCAO);
    }

    @ParameterizedTest
    @EnumSource(value = ReferenciaTipoCotacao.class, names = {"DATA", "INSTANTE"})
    void mesmaObservacaoPreencheObtencaoNula(ReferenciaTipoCotacao tipo) {
        var atual = estado(tipo, PRECO, null);
        assertDecisao(decidirSemMutar(atual, candidata(tipo, 0, PRECO, OBTENCAO)),
                SEM_NOVA_COTACAO, MESMA_OBSERVACAO, AVANCAR_SOMENTE_OBTENCAO, OBTENCAO);
    }

    @Test
    void offsetsDiferentesRepresentamMesmaReferenciaEMesmaObtencao() {
        var atual = estado(INSTANTE, PRECO, OBTENCAO);
        var candidata = new CotacaoCandidata("AAPL", "Apple", Moeda.USD, PRECO, INSTANTE,
                OffsetDateTime.parse("2026-10-02T12:00:00-03:00"), null,
                OffsetDateTime.parse("2026-10-03T12:05:00-03:00"));
        assertDecisao(decidirSemMutar(atual, candidata), SEM_NOVA_COTACAO, MESMA_OBSERVACAO,
                PRESERVAR_OBSERVACAO, OBTENCAO);
    }

    @ParameterizedTest
    @EnumSource(value = ReferenciaTipoCotacao.class, names = {"DATA", "INSTANTE"})
    void igualdadeMonetariaUsaValorNaoEscala(ReferenciaTipoCotacao tipo) {
        var atual = estado(tipo, new BigDecimal("100.0"), OBTENCAO);
        var candidata = candidata(tipo, 0, new BigDecimal("100.000000"), OBTENCAO);
        assertDecisao(decidirSemMutar(atual, candidata), SEM_NOVA_COTACAO, MESMA_OBSERVACAO,
                PRESERVAR_OBSERVACAO, OBTENCAO);
    }

    @Test
    void exemploAprovadoSubstituiObservacaoSemMaxDeObtencoes() {
        Acao atual = new Acao("AAPL", "Apple", Mercado.EUA, Moeda.USD, PRECO, LEGADO,
                DATA, null, LocalDate.of(2026, 10, 1), OBTENCAO);
        var candidata = candidata(DATA, 0, new BigDecimal("102"), OBTENCAO.minusMinutes(1));
        assertDecisao(decidirSemMutar(atual, candidata), ATUALIZADA, REFERENCIA_POSTERIOR,
                SUBSTITUIR_OBSERVACAO, OffsetDateTime.parse("2026-10-03T15:04:00Z"));
    }

    @Test
    void legadoNaoEReinterpretadoParaEstabelecerBaseline() {
        Acao atual = new Acao("AAPL", "Apple", Mercado.EUA, Moeda.USD, PRECO, LEGADO);
        assertDecisao(decidirSemMutar(atual, candidata(DATA, 0, PRECO, OBTENCAO)),
                ATUALIZADA, REFERENCIA_INICIAL_ESTABELECIDA, SUBSTITUIR_OBSERVACAO, OBTENCAO);
    }

    @Test
    void rejeitaUsoComIdentidadeDiferenteSemDecisaoPublicaInventada() {
        Acao atual = estado(DATA, PRECO, OBTENCAO);
        var outroTicker = new CotacaoCandidata("MSFT", "Microsoft", Moeda.USD, PRECO,
                DATA, null, DIA, OBTENCAO);
        var outraMoeda = new CotacaoCandidata("AAPL", "Apple", Moeda.BRL, PRECO,
                DATA, null, DIA, OBTENCAO);
        assertThrows(IllegalArgumentException.class, () -> RegraDecisaoCotacao.decidir(atual, outroTicker));
        assertThrows(IllegalArgumentException.class, () -> RegraDecisaoCotacao.decidir(atual, outraMoeda));
        assertThrows(NullPointerException.class, () -> RegraDecisaoCotacao.decidir(null, outroTicker));
        assertThrows(NullPointerException.class, () -> RegraDecisaoCotacao.decidir(atual, null));
    }

    static Stream<Arguments> combinacoesDeDecisao() {
        Set<String> validas = Set.of(
                "ATUALIZADA/REFERENCIA_INICIAL_ESTABELECIDA/SUBSTITUIR_OBSERVACAO",
                "ATUALIZADA/REFERENCIA_POSTERIOR/SUBSTITUIR_OBSERVACAO",
                "SEM_NOVA_COTACAO/REFERENCIA_ANTERIOR/PRESERVAR_OBSERVACAO",
                "SEM_NOVA_COTACAO/MESMA_OBSERVACAO/PRESERVAR_OBSERVACAO",
                "SEM_NOVA_COTACAO/MESMA_OBSERVACAO/AVANCAR_SOMENTE_OBTENCAO",
                "FALHA/CONFLITO_NA_MESMA_REFERENCIA/PRESERVAR_OBSERVACAO",
                "FALHA/TIPO_REFERENCIA_INCOMPATIVEL/PRESERVAR_OBSERVACAO",
                "FALHA/REFERENCIA_TEMPORAL_INVALIDA/PRESERVAR_OBSERVACAO");
        List<Arguments> casos = new ArrayList<>();
        for (var resultado : RegraDecisaoCotacao.Resultado.values())
            for (var motivo : RegraDecisaoCotacao.Motivo.values())
                for (var efeito : RegraDecisaoCotacao.Efeito.values())
                    casos.add(Arguments.of(resultado, motivo, efeito,
                            validas.contains(resultado + "/" + motivo + "/" + efeito)));
        return casos.stream();
    }

    @ParameterizedTest
    @MethodSource("combinacoesDeDecisao")
    void construtorAceitaSomenteOitoCombinacoesAprovadas(RegraDecisaoCotacao.Resultado resultado,
            RegraDecisaoCotacao.Motivo motivo, RegraDecisaoCotacao.Efeito efeito, boolean valida) {
        var dados = efeito == SUBSTITUIR_OBSERVACAO ? candidata(DATA, 1, PRECO, OBTENCAO) : null;
        if (valida) {
            var decisao = assertDoesNotThrow(() -> new RegraDecisaoCotacao.Decisao(
                    resultado, motivo, efeito, OBTENCAO, dados));
            assertDecisao(decisao, resultado, motivo, efeito, OBTENCAO);
        } else {
            assertThrows(IllegalArgumentException.class, () -> new RegraDecisaoCotacao.Decisao(
                    resultado, motivo, efeito, OBTENCAO, dados));
        }
    }

    @Test
    void construtorRejeitaEnumsNulos() {
        assertThrows(IllegalArgumentException.class, () -> new RegraDecisaoCotacao.Decisao(
                null, MESMA_OBSERVACAO, PRESERVAR_OBSERVACAO, null, null));
        assertThrows(IllegalArgumentException.class, () -> new RegraDecisaoCotacao.Decisao(
                SEM_NOVA_COTACAO, null, PRESERVAR_OBSERVACAO, null, null));
        assertThrows(IllegalArgumentException.class, () -> new RegraDecisaoCotacao.Decisao(
                SEM_NOVA_COTACAO, MESMA_OBSERVACAO, null, null, null));
    }

    @Test
    void construtorRejeitaDadosObrigatoriosAusentesEObtencaoDeOutraObservacao() {
        var dados = candidata(DATA, 1, PRECO, OBTENCAO);
        assertThrows(IllegalArgumentException.class, () -> new RegraDecisaoCotacao.Decisao(
                ATUALIZADA, REFERENCIA_POSTERIOR, SUBSTITUIR_OBSERVACAO, OBTENCAO, null));
        assertThrows(IllegalArgumentException.class, () -> new RegraDecisaoCotacao.Decisao(
                ATUALIZADA, REFERENCIA_POSTERIOR, SUBSTITUIR_OBSERVACAO, null, dados));
        assertThrows(IllegalArgumentException.class, () -> new RegraDecisaoCotacao.Decisao(
                ATUALIZADA, REFERENCIA_POSTERIOR, SUBSTITUIR_OBSERVACAO, OBTENCAO.plusSeconds(1), dados));
        assertThrows(IllegalArgumentException.class, () -> new RegraDecisaoCotacao.Decisao(
                SEM_NOVA_COTACAO, MESMA_OBSERVACAO, AVANCAR_SOMENTE_OBTENCAO, null, null));
    }

    @ParameterizedTest
    @EnumSource(value = RegraDecisaoCotacao.Motivo.class,
            names = {"REFERENCIA_INICIAL_ESTABELECIDA", "REFERENCIA_POSTERIOR"})
    void substituicaoRejeitaCandidataDesconhecida(RegraDecisaoCotacao.Motivo motivo) {
        var dados = candidata(DESCONHECIDA, 0, PRECO, OBTENCAO);
        assertThrows(IllegalArgumentException.class, () -> new RegraDecisaoCotacao.Decisao(
                ATUALIZADA, motivo, SUBSTITUIR_OBSERVACAO, OBTENCAO, dados));
    }

    @ParameterizedTest
    @EnumSource(value = RegraDecisaoCotacao.Efeito.class,
            names = {"PRESERVAR_OBSERVACAO", "AVANCAR_SOMENTE_OBTENCAO"})
    void efeitosSemSubstituicaoRejeitamCandidata(RegraDecisaoCotacao.Efeito efeito) {
        var dados = candidata(DATA, 0, PRECO, OBTENCAO);
        assertThrows(IllegalArgumentException.class, () -> new RegraDecisaoCotacao.Decisao(
                SEM_NOVA_COTACAO, MESMA_OBSERVACAO, efeito, OBTENCAO, dados));
    }

    @Test
    void preservacaoPermiteObtencaoLegadaNula() {
        var decisao = new RegraDecisaoCotacao.Decisao(
                FALHA, REFERENCIA_TEMPORAL_INVALIDA, PRESERVAR_OBSERVACAO, null, null);
        assertNull(decisao.cotacaoObtidaEmResultante());
        assertNull(decisao.candidataParaSubstituicao());
    }

    @ParameterizedTest
    @EnumSource(value = ReferenciaTipoCotacao.class, names = {"DATA", "INSTANTE"})
    void substituicaoVinculaTodosOsDadosDaCandidataDecidida(ReferenciaTipoCotacao tipo) {
        var dados = candidata(tipo, 1, new BigDecimal("102.123456"), OBTENCAO.minusMinutes(1));
        var decisao = decidirSemMutar(estado(tipo, PRECO, OBTENCAO), dados);
        var outra = candidata(tipo, 2, new BigDecimal("103"), OBTENCAO.plusMinutes(1));
        assertSame(dados, decisao.candidataParaSubstituicao());
        assertNotEquals(outra, decisao.candidataParaSubstituicao());
        assertEquals(dados.cotacao(), decisao.candidataParaSubstituicao().cotacao());
        assertEquals(tipo, decisao.candidataParaSubstituicao().referenciaTipo());
        assertEquals(dados.referenciaData(), decisao.candidataParaSubstituicao().referenciaData());
        assertEquals(dados.referenciaInstante(), decisao.candidataParaSubstituicao().referenciaInstante());
        assertEquals(dados.cotacaoObtidaEm(), decisao.cotacaoObtidaEmResultante());
        assertEquals(dados.cotacaoObtidaEm(), decisao.candidataParaSubstituicao().cotacaoObtidaEm());
    }

    private static Acao estado(ReferenciaTipoCotacao tipo, BigDecimal preco, OffsetDateTime obtencao) {
        return new Acao("AAPL", "Apple", Mercado.EUA, Moeda.USD, preco, LEGADO, tipo,
                tipo == INSTANTE ? INSTANTE_MERCADO : null, tipo == DATA ? DIA : null, obtencao);
    }

    private static CotacaoCandidata candidata(ReferenciaTipoCotacao tipo, int ordem,
                                            BigDecimal preco, OffsetDateTime obtencao) {
        return new CotacaoCandidata("AAPL", "Apple", Moeda.USD, preco, tipo,
                tipo == INSTANTE ? INSTANTE_MERCADO.plusSeconds(ordem) : null,
                tipo == DATA ? DIA.plusDays(ordem) : null, obtencao);
    }

    private static RegraDecisaoCotacao.Decisao decidirSemMutar(Acao atual, CotacaoCandidata candidata) {
        List<Object> antes = snapshot(atual);
        var primeira = RegraDecisaoCotacao.decidir(atual, candidata);
        if (primeira.efeito() == SUBSTITUIR_OBSERVACAO) {
            assertSame(candidata, primeira.candidataParaSubstituicao());
        } else {
            assertNull(primeira.candidataParaSubstituicao());
        }
        assertEquals(antes, snapshot(atual));
        assertEquals(primeira, RegraDecisaoCotacao.decidir(atual, candidata));
        assertEquals(antes, snapshot(atual));
        return primeira;
    }

    private static List<Object> snapshot(Acao a) {
        return java.util.Arrays.asList(a.getId(), a.getTicker(), a.getNomeEmpresa(), a.getMercado(),
                a.getMoeda(), a.getCotacaoAtual(), a.getDataHoraCotacao(), a.getReferenciaTipo(),
                a.getReferenciaData(), a.getReferenciaInstante(), a.getCotacaoObtidaEm());
    }

    private static void assertDecisao(RegraDecisaoCotacao.Decisao d,
                                     RegraDecisaoCotacao.Resultado resultado,
                                     RegraDecisaoCotacao.Motivo motivo,
                                     RegraDecisaoCotacao.Efeito efeito, OffsetDateTime obtencao) {
        assertEquals(resultado, d.resultado());
        assertEquals(motivo, d.motivo());
        assertEquals(efeito, d.efeito());
        assertEquals(obtencao, d.cotacaoObtidaEmResultante());
    }
}
