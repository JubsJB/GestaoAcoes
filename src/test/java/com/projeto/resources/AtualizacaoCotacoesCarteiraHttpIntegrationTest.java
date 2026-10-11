package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.*;
import com.projeto.integrations.cotacao.*;
import com.projeto.repositories.*;
import com.projeto.services.*;
import com.projeto.services.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import static com.projeto.entities.ReferenciaTipoCotacao.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AtualizacaoCotacoesCarteiraHttpIntegrationTest {
    static final AtomicInteger IDS=new AtomicInteger();
    static final OffsetDateTime OLD=OffsetDateTime.parse("2026-08-20T15:30:00Z");
    @Autowired MockMvc mvc;
    @Autowired AtualizacaoCotacoesCarteiraService lote;
    @Autowired AcaoRepository acoes;
    @Autowired CarteiraRepository carteiras;
    @Autowired OperacaoRepository operacoes;
    @MockitoSpyBean HistoricoCotacaoRepository historico;
    @MockitoSpyBean AplicacaoCotacaoService aplicacao;
    @MockitoSpyBean(name="brapiAdapter") CotacaoProvider br;
    @MockitoSpyBean(name="alphaVantageAdapter") CotacaoProvider us;
    @org.springframework.test.context.bean.override.mockito.MockitoBean(name="applicationClock") Clock clock;
    Carteira carteira;

    @BeforeEach void preparar() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-04T15:30:17.123456Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        assertFalse(mockingDetails(lote).isMock());
        carteira=carteiras.saveAndFlush(new Carteira("HTTP lote "+IDS.incrementAndGet(),OLD));
        clearInvocations(br,us,historico,aplicacao);
    }
    @AfterEach void limpar() { reset(br,us,historico,aplicacao); }
    Acao criar(Mercado mercado, ReferenciaTipoCotacao tipo) {
        return acoes.saveAndFlush(new Acao("BH"+IDS.incrementAndGet(),"Empresa",mercado,
                mercado==Mercado.BRASIL?Moeda.BRL:Moeda.USD,new BigDecimal("100.000000"),OLD,
                tipo,tipo==INSTANTE?OLD:null,tipo==DATA?OLD.toLocalDate():null,
                tipo==DESCONHECIDA?null:OLD.plusMinutes(1)));
    }
    void comprar(Acao a,int ordem) {
        operacoes.saveAndFlush(new Operacao(carteira,a,null,TipoOperacao.COMPRA,BigDecimal.ONE,
                BigDecimal.TEN,OLD.toLocalDate(),ordem,BigDecimal.TEN));
    }
    String path() { return "/carteiras/"+carteira.getId()+"/atualizacoes-cotacoes"; }
    CotacaoProvider provider(Acao a) { return a.getMercado()==Mercado.BRASIL?br:us; }
    void responder(Acao a, boolean posterior) {
        doAnswer(i->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            var tipo=a.getMercado()==Mercado.BRASIL?INSTANTE:DATA;
            return new CotacaoCandidata(a.getTicker(),a.getNomeEmpresa(),a.getMoeda(),
                    posterior?new BigDecimal("101.123456"):a.getCotacaoAtual(),tipo,
                    tipo==INSTANTE?OLD.plusDays(posterior?1:0):null,
                    tipo==DATA?OLD.toLocalDate().plusDays(posterior?1:0):null,OffsetDateTime.now(clock));
        }).when(provider(a)).consultarCandidataAtualizacao(eq(a.getTicker()),any(),any(),any());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"1969-12-31T23:59:59.999999Z", "2026-10-04T15:30:17.123457Z"})
    void candidataInvalidaNoLotePreservaEstado(String value) throws Exception {
        var a = criar(Mercado.BRASIL, DESCONHECIDA); comprar(a, 1);
        doReturn(new CotacaoData(a.getTicker(), "Empresa", "BRL", BigDecimal.TEN,
                OffsetDateTime.parse(value), false)).when(br).consultar(a.getTicker());
        mvc.perform(post(path())).andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.falhas").value(1))
                .andExpect(jsonPath("$.resumo.atualizadas").value(0));
        assertEquals(a.getCotacaoAtual(), acoes.findById(a.getId()).orElseThrow().getCotacaoAtual());
        assertTrue(historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId()).isEmpty());
        verify(aplicacao, never()).aplicar(any(), any());
    }

    @Test void vaziaEInexistenteSemProviderAplicacaoHistorico() throws Exception {
        mvc.perform(post(path())).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultados",hasSize(0))).andExpect(jsonPath("$.resumo.falhas").value(0));
        mvc.perform(post("/carteiras/-1/atualizacoes-cotacoes")).andExpect(status().isNotFound());
        verifyNoInteractions(br,us,historico,aplicacao);
    }

    @Test void parcialRealOrdenaDeduplicaEPreservaPersistencia() throws Exception {
        var a=criar(Mercado.BRASIL,INSTANTE); comprar(a,1); comprar(a,2); responder(a,true);
        var b=criar(Mercado.EUA,DATA); comprar(b,1); responder(b,false);
        var c=criar(Mercado.BRASIL,DESCONHECIDA); comprar(c,1);
        doThrow(new ApiException(HttpStatus.GATEWAY_TIMEOUT,"SERVICO_EXTERNO_TIMEOUT","Timeout público"))
                .when(br).consultarCandidataAtualizacao(eq(c.getTicker()),any(),any(),any());
        mvc.perform(post(path())).andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.atualizadas").value(1))
                .andExpect(jsonPath("$.resumo.semNovaCotacao").value(1))
                .andExpect(jsonPath("$.resumo.falhas").value(1))
                .andExpect(jsonPath("$.resultados[0].acaoId").value(a.getId()))
                .andExpect(jsonPath("$.resultados[1].acaoId").value(b.getId()))
                .andExpect(jsonPath("$.resultados[2].acaoId").value(c.getId()))
                .andExpect(jsonPath("$.resultados[*].resultado",contains("ATUALIZADA","SEM_NOVA_COTACAO","FALHA")))
                .andExpect(jsonPath("$.resultados[2].cotacao.referenciaMercadoCotacao.tipo").value("DESCONHECIDA"))
                .andExpect(jsonPath("$.resultados[2].cotacao.cotacaoObtidaEm").value(nullValue()));
        assertEquals(new BigDecimal("101.123456"),acoes.findById(a.getId()).orElseThrow().getCotacaoAtual());
        assertEquals(1,historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId()).size());
        assertTrue(historico.findByAcaoIdOrderByDataHoraCotacaoAsc(b.getId()).isEmpty());
        assertTrue(historico.findByAcaoIdOrderByDataHoraCotacaoAsc(c.getId()).isEmpty());
        verify(br,times(2)).consultarCandidataAtualizacao(any(),any(),any(),any());
        verify(us,times(1)).consultarCandidataAtualizacao(any(),any(),any(),any());
    }

    @Test void falhaGlobalTardia500PreservaSucessoAnterior() throws Exception {
        var a=criar(Mercado.BRASIL,INSTANTE); comprar(a,1); responder(a,true);
        var b=criar(Mercado.BRASIL,INSTANTE); comprar(b,1);
        doThrow(new IllegalStateException("SQL segredo")).when(br)
                .consultarCandidataAtualizacao(eq(b.getTicker()),any(),any(),any());
        mvc.perform(post(path())).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("FALHA_ATUALIZACAO_COTACOES"))
                .andExpect(jsonPath("$.message",containsString("já confirmadas podem ter sido preservadas")))
                .andExpect(content().string(not(containsString("segredo"))));
        assertEquals(new BigDecimal("101.123456"),acoes.findById(a.getId()).orElseThrow().getCotacaoAtual());
        assertEquals(1,historico.findByAcaoIdOrderByDataHoraCotacaoAsc(a.getId()).size());
        assertTrue(historico.findByAcaoIdOrderByDataHoraCotacaoAsc(b.getId()).isEmpty());
        verify(br,times(2)).consultarCandidataAtualizacao(any(),any(),any(),any());
    }

    @Test void cooldownApresentaSnapshotDaTentativaMesmoAposNovaObservacao() throws Exception {
        var a=criar(Mercado.EUA,DATA); comprar(a,1); responder(a,true);
        mvc.perform(post(path())).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultados[0].resultado").value("ATUALIZADA"));
        aplicacao.aplicar(a.getId(),new CotacaoCandidata(a.getTicker(),a.getNomeEmpresa(),a.getMoeda(),
                new BigDecimal("102.000000"),DATA,null,OLD.toLocalDate().plusDays(2),OLD.plusMinutes(2)));
        clearInvocations(br,us,historico,aplicacao);
        mvc.perform(post(path())).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultados[0].resultado").value("SEM_NOVA_COTACAO"))
                .andExpect(jsonPath("$.resultados[0].motivo").value("COOLDOWN_SUCESSO"))
                .andExpect(content().string(containsString("\"cotacaoAtual\":101.123456")))
                .andExpect(jsonPath("$.resultados[0].cotacao.referenciaMercadoCotacao.data").value("2026-08-21"));
        verifyNoInteractions(br,us,historico,aplicacao);
        assertEquals(new BigDecimal("102.000000"),acoes.findById(a.getId()).orElseThrow().getCotacaoAtual());
    }

    @ParameterizedTest @EnumSource(ReferenciaTipoCotacao.class)
    void getsAcaoEPosicaoPreservamCamposAntigosEAdicionamTemporalidade(ReferenciaTipoCotacao tipo) throws Exception {
        var a=criar(Mercado.BRASIL,tipo); comprar(a,1);
        mvc.perform(get("/acoes/{id}",a.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$",aMapWithSize(9)))
                .andExpect(jsonPath("$.id").value(a.getId()))
                .andExpect(jsonPath("$.ticker").value(a.getTicker()))
                .andExpect(jsonPath("$.nomeEmpresa").value("Empresa"))
                .andExpect(jsonPath("$.mercado").value("BRASIL"))
                .andExpect(jsonPath("$.moeda").value("BRL"))
                .andExpect(jsonPath("$.cotacaoAtual").value(100))
                .andExpect(jsonPath("$.dataHoraCotacao").value("2026-08-20T15:30:00Z"))
                .andExpect(jsonPath("$.referenciaMercadoCotacao.tipo").value(tipo.name()))
                .andExpect(jsonPath("$.cotacaoObtidaEm").value(tipo==DESCONHECIDA?null:"2026-08-20T15:31:00Z"));
        mvc.perform(get("/carteiras/{id}/posicoes",carteira.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0]",aMapWithSize(15)))
                .andExpect(jsonPath("$[0].acaoId").value(a.getId()))
                .andExpect(jsonPath("$[0].ticker").value(a.getTicker()))
                .andExpect(jsonPath("$[0].nomeEmpresa").value("Empresa"))
                .andExpect(jsonPath("$[0].mercado").value("BRASIL"))
                .andExpect(jsonPath("$[0].moeda").value("BRL"))
                .andExpect(jsonPath("$[0].quantidadeAtual").value(1))
                .andExpect(jsonPath("$[0].precoMedio").value(10))
                .andExpect(jsonPath("$[0].custoPosicao").value(10))
                .andExpect(jsonPath("$[0].cotacaoAtual").value(100))
                .andExpect(jsonPath("$[0].dataHoraCotacao").value("2026-08-20T15:30:00Z"))
                .andExpect(jsonPath("$[0].valorAtualPosicao").value(100))
                .andExpect(jsonPath("$[0].resultadoNaoRealizado").value(90))
                .andExpect(jsonPath("$[0].rentabilidadePercentual").value(900))
                .andExpect(jsonPath("$[0].referenciaMercadoCotacao.tipo").value(tipo.name()))
                .andExpect(jsonPath("$[0].cotacaoObtidaEm").value(tipo==DESCONHECIDA?null:"2026-08-20T15:31:00Z"));
        verifyNoInteractions(br,us,historico,aplicacao);
    }
}
