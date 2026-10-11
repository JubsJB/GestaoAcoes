package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.*;
import com.projeto.integrations.cotacao.*;
import com.projeto.repositories.*;
import com.projeto.services.AcaoService;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static com.projeto.entities.ReferenciaTipoCotacao.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcaoCotacaoHttpIntegrationTest {
    static final OffsetDateTime MARKET=OffsetDateTime.parse("2026-08-20T15:30:00Z");
    static final AtomicInteger IDS=new AtomicInteger();
    @Autowired MockMvc mvc;
    @Autowired AcaoService service;
    @Autowired AcaoRepository acoes;
    @Autowired HistoricoCotacaoRepository historico;
    @Autowired CarteiraRepository carteiras;
    @Autowired OperacaoRepository operacoes;
    @Autowired SnapshotCarteiraRepository snapshots;
    @Autowired SnapshotCarteiraMoedaRepository componentes;
    @MockitoSpyBean(name="brapiAdapter") CotacaoProvider brapi;
    @MockitoSpyBean(name="alphaVantageAdapter") CotacaoProvider alpha;
    @org.springframework.test.context.bean.override.mockito.MockitoBean(name="applicationClock") Clock clock;

    @BeforeEach void preparar() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-04T15:30:17.123456Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        assertFalse(mockingDetails(service).isMock());
        clearInvocations(brapi,alpha);
    }
    @AfterEach void limpar() { reset(brapi,alpha); }
    Acao inicial(ReferenciaTipoCotacao tipo) {
        return acoes.saveAndFlush(new Acao("HTTP"+IDS.incrementAndGet(),"Empresa persistida",Mercado.BRASIL,
                Moeda.BRL,new BigDecimal("30.000000"),MARKET,tipo,
                tipo==INSTANTE?MARKET:null,tipo==DATA?LocalDate.of(2026,8,20):null,
                tipo==DESCONHECIDA?null:MARKET.plusMinutes(1)));
    }
    void responder(Acao a, String preco, OffsetDateTime referencia, boolean invalida) {
        doAnswer(i->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return new CotacaoData(a.getTicker(),"Nome externo","BRL",new BigDecimal(preco),
                    referencia,false,null,invalida);
        }).when(brapi).consultar(a.getTicker());
    }
    List<Object> estado(Acao a) {
        return Arrays.asList(a.getCotacaoAtual(),a.getReferenciaTipo(),a.getReferenciaInstante(),
                a.getReferenciaData(),a.getCotacaoObtidaEm(),a.getDataHoraCotacao());
    }
    void preservado(Acao a) {
        assertEquals(estado(a),estado(acoes.findById(a.getId()).orElseThrow()));
        assertTrue(historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId()).isEmpty());
        verify(brapi,times(1)).consultar(a.getTicker()); verifyNoInteractions(alpha);
    }
    void erro(Acao a,String codigo) throws Exception {
        mvc.perform(patch("/acoes/{id}/cotacao",a.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(codigo))
                .andExpect(jsonPath("$.path").value("/acoes/"+a.getId()+"/cotacao"))
                .andExpect(jsonPath("$.details.acaoId").value(a.getId()))
                .andExpect(jsonPath("$.details.cotacaoPreservada").value(true))
                .andExpect(jsonPath("$.details.ultimaCotacaoValida").value(30.0))
                .andExpect(jsonPath("$.details.dataHoraUltimaCotacao").value("2026-08-20T15:30:00Z"));
        preservado(a);
    }
    @ParameterizedTest
    @ValueSource(strings = {"1969-12-31T23:59:59.999999Z", "2026-10-04T15:30:17.123457Z"})
    void novosLimitesTemporaisPreservamPatch(String value) throws Exception {
        var a = inicial(DESCONHECIDA);
        responder(a, "35", OffsetDateTime.parse(value), false);
        erro(a, ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA);
    }

    @Test void sucessoBrasilInstantePassaPorServicoRealENucleoAtomico() throws Exception {
        Acao a=inicial(DESCONHECIDA);
        var referencia=OffsetDateTime.parse("2026-08-21T12:30:00-03:00");
        responder(a,"35.123456",referencia,false);
        mvc.perform(patch("/acoes/{id}/cotacao",a.getId()))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(a.getId()))
                .andExpect(jsonPath("$.ticker").value(a.getTicker()))
                .andExpect(jsonPath("$.nomeEmpresa").value(a.getNomeEmpresa()))
                .andExpect(jsonPath("$.mercado").value("BRASIL"))
                .andExpect(jsonPath("$.moeda").value("BRL"))
                .andExpect(jsonPath("$.cotacaoAtual").value(35.123456))
                .andExpect(jsonPath("$.dataHoraCotacao").value("2026-08-21T15:30:00Z"))
                .andExpect(jsonPath("$.referenciaMercadoCotacao.tipo").value("INSTANTE"))
                .andExpect(jsonPath("$.referenciaMercadoCotacao.instante").value("2026-08-21T15:30:00Z"))
                .andExpect(jsonPath("$.cotacaoObtidaEm").isNotEmpty());
        var atual=acoes.findById(a.getId()).orElseThrow();
        assertEquals(INSTANTE,atual.getReferenciaTipo());
        assertEquals(referencia.toInstant(),atual.getReferenciaInstante().toInstant());
        assertEquals(ZoneOffset.UTC,atual.getReferenciaInstante().getOffset());
        assertNotNull(atual.getCotacaoObtidaEm()); assertNull(atual.getReferenciaData());
        var hs=historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId());
        assertEquals(1,hs.size()); var h=hs.get(0);
        assertEquals(atual.getCotacaoAtual(),h.getCotacao());
        assertEquals(INSTANTE,h.getReferenciaTipo());
        assertEquals(atual.getReferenciaInstante(),h.getReferenciaInstante());
        assertEquals(atual.getCotacaoObtidaEm(),h.getCotacaoObtidaEm());
        verify(brapi).consultar(a.getTicker()); verifyNoInteractions(alpha);
    }
    @Test void patchAtualizaAvaliacaoGlobalSemAlterarOperacoesSnapshotsOuEscreverEmGets() throws Exception {
        Acao compartilhada = inicial(DESCONHECIDA);
        Acao outra = inicial(DESCONHECIDA);
        Carteira primeira = carteiras.saveAndFlush(new Carteira("Certificacao A", MARKET));
        Carteira segunda = carteiras.saveAndFlush(new Carteira("Certificacao B", MARKET));
        Carteira terceira = carteiras.saveAndFlush(new Carteira("Certificacao isolada", MARKET));
        var compra = operacoes.saveAndFlush(new Operacao(primeira, compartilhada, null, TipoOperacao.COMPRA,
                new BigDecimal("10.000000"), new BigDecimal("20.000000"), LocalDate.of(2026, 8, 1), 1,
                new BigDecimal("200.000000000000")));
        var venda = operacoes.saveAndFlush(new Operacao(primeira, compartilhada, null, TipoOperacao.VENDA,
                new BigDecimal("4.000000"), new BigDecimal("25.000000"), LocalDate.of(2026, 8, 2), 1,
                new BigDecimal("100.000000000000")));
        var compraB = operacoes.saveAndFlush(new Operacao(segunda, compartilhada, null, TipoOperacao.COMPRA,
                new BigDecimal("2.000000"), new BigDecimal("50.000000"), LocalDate.of(2026, 8, 1), 1,
                new BigDecimal("100.000000000000")));
        operacoes.saveAndFlush(new Operacao(terceira, outra, null, TipoOperacao.COMPRA,
                new BigDecimal("1.000000"), new BigDecimal("10.000000"), LocalDate.of(2026, 8, 1), 1,
                new BigDecimal("10.000000000000")));
        var snapshot = snapshots.saveAndFlush(new SnapshotCarteira(primeira, MARKET));
        var componente = componentes.saveAndFlush(new SnapshotCarteiraMoeda(snapshot, Moeda.BRL,
                new BigDecimal("180.000000000000")));
        List<Long> ids = List.of(compra.getId(), venda.getId(), compraB.getId());
        var antes = valoresOperacoes(ids);
        long quantidadeOperacoes = operacoes.count(), quantidadeSnapshots = snapshots.count(), quantidadeComponentes = componentes.count();
        responder(compartilhada, "35", MARKET.plusDays(1), false);

        mvc.perform(patch("/acoes/{id}/cotacao", compartilhada.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.cotacaoAtual").value(35.0));
        verify(brapi, times(1)).consultar(compartilhada.getTicker());
        clearInvocations(brapi, alpha);
        long quantidadeHistorico = historico.count();
        var estadoAtual = estado(acoes.findById(compartilhada.getId()).orElseThrow());

        mvc.perform(get("/carteiras/{id}/posicoes", primeira.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantidadeAtual").value(6.0))
                .andExpect(jsonPath("$[0].precoMedio").value(20.0))
                .andExpect(jsonPath("$[0].custoPosicao").value(120.0))
                .andExpect(jsonPath("$[0].valorAtualPosicao").value(210.0))
                .andExpect(jsonPath("$[0].resultadoNaoRealizado").value(90.0));
        mvc.perform(get("/carteiras/{id}/posicoes", segunda.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].precoMedio").value(50.0))
                .andExpect(jsonPath("$[0].valorAtualPosicao").value(70.0))
                .andExpect(jsonPath("$[0].resultadoNaoRealizado").value(-30.0));
        mvc.perform(get("/carteiras/{id}/posicoes", terceira.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cotacaoAtual").value(30.0))
                .andExpect(jsonPath("$[0].valorAtualPosicao").value(30.0));
        mvc.perform(get("/carteiras/{id}/patrimonio", primeira.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.patrimonios[0].patrimonioAtual").value(210.0));
        mvc.perform(get("/carteiras/{id}/resultados-realizados", primeira.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resultadoRealizado").value(20.0));
        for (String caminho : List.of("resumo", "evolucao-patrimonial")) {
            mvc.perform(get("/carteiras/{id}/" + caminho, primeira.getId())).andExpect(status().isOk());
        }
        mvc.perform(get("/acoes/{id}", compartilhada.getId())).andExpect(status().isOk());
        assertEquals(antes, valoresOperacoes(ids));
        assertEquals(quantidadeOperacoes, operacoes.count());
        assertEquals(quantidadeSnapshots, snapshots.count());
        assertEquals(quantidadeComponentes, componentes.count());
        assertEquals(quantidadeHistorico, historico.count());
        assertEquals(estadoAtual, estado(acoes.findById(compartilhada.getId()).orElseThrow()));
        assertEquals(MARKET, snapshots.findById(snapshot.getId()).orElseThrow().getDataHoraSnapshot());
        assertEquals(new BigDecimal("180.000000000000"), componentes.findById(componente.getId()).orElseThrow().getPatrimonioAtual());
        verifyNoInteractions(brapi, alpha);
    }

    private List<List<Object>> valoresOperacoes(List<Long> ids) {
        return operacoes.findAllById(ids).stream().map(o -> List.<Object>of(o.getId(), o.getTipo(),
                o.getQuantidade(), o.getPrecoUnitario(), o.getValorTotal(), o.getDataOperacao(), o.getOrdemNoDia())).toList();
    }

    @Test void conflitoPrecoNaMesmaReferenciaChegaAoHandlerCom422() throws Exception {
        Acao a=inicial(INSTANTE); responder(a,"31",MARKET,false);
        erro(a,ErrorCodes.CONFLITO_NA_MESMA_REFERENCIA);
    }

    @Test void patchUsaClockDeAquisicaoExatoMesmoComFonteBrutaDeNanos() throws Exception {
        Acao a = inicial(DESCONHECIDA);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-04T15:30:17.123456789Z"));
        responder(a, "35", MARKET.plusDays(1), false);
        mvc.perform(patch("/acoes/{id}/cotacao", a.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.cotacaoObtidaEm").value("2026-10-04T15:30:17.123456Z"));
        var atual = acoes.findById(a.getId()).orElseThrow();
        assertEquals(123456000, atual.getCotacaoObtidaEm().getNano());
        var registros = historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId());
        assertEquals(1, registros.size());
        assertEquals(atual.getCotacaoObtidaEm(), registros.get(0).getCotacaoObtidaEm());
    }

    @Test void patchMantemRejeicaoDeNanosDoProviderMesmoComClockConfigurado() throws Exception {
        Acao a = inicial(DESCONHECIDA);
        responder(a, "35", MARKET.plusDays(1).withNano(123456789), false);
        erro(a, ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA);
    }
    @Test void dataEInstanteIncompativeisChegamAoHandlerCom422() throws Exception {
        Acao a=inicial(DATA); responder(a,"31",MARKET.plusDays(1),false);
        erro(a,ErrorCodes.TIPO_REFERENCIA_INCOMPATIVEL);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void referenciaAusenteOuInvalidaFalhaAntesDeAplicarSemEscrita(boolean invalida) throws Exception {
        Acao a=inicial(DESCONHECIDA); responder(a,"31",null,invalida);
        erro(a,ErrorCodes.REFERENCIA_TEMPORAL_INVALIDA);
    }
}
