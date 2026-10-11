package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.*;
import com.projeto.integrations.cotacao.*;
import com.projeto.repositories.*;
import com.projeto.services.AcaoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcaoCadastroTemporalIntegrationTest {
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final OffsetDateTime OBTENCAO = OffsetDateTime.parse("2026-10-04T15:30:17.987654000Z");
    private static final OffsetDateTime MERCADO = OffsetDateTime.parse("2026-10-02T12:15:00.123456000-03:00");
    private static final LocalDate DATA = LocalDate.of(2026, 10, 2);
    private static final BigDecimal PRECO = new BigDecimal("1234567890123.123456");
    @Autowired MockMvc mvc;
    @Autowired AcaoService service;
    @Autowired AcaoRepository acoes;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoSpyBean HistoricoCotacaoRepository historico;
    @MockitoSpyBean(name = "brapiAdapter") CotacaoProvider brapi;
    @MockitoSpyBean(name = "alphaVantageAdapter") CotacaoProvider alpha;
    @MockitoBean(name = "applicationClock") Clock clock;

    @BeforeEach void preparar() {
        when(clock.instant()).thenReturn(OBTENCAO.toInstant());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        assertFalse(mockingDetails(service).isMock());
        clearInvocations(clock, brapi, alpha, historico);
    }

    @AfterEach void limpar() { reset(brapi, alpha, historico); }

    private String ticker() { return "CAD" + IDS.incrementAndGet(); }

    private void responder(String ticker, Mercado mercado, ReferenciaTipoCotacao tipo, boolean invalida) {
        doAnswer(i -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return new CotacaoData(ticker, "Empresa", mercado == Mercado.BRASIL ? "BRL" : "USD", PRECO,
                    tipo == ReferenciaTipoCotacao.INSTANTE ? MERCADO : null, false,
                    tipo == ReferenciaTipoCotacao.DATA ? DATA : null, invalida);
        }).when(mercado == Mercado.BRASIL ? brapi : alpha).consultar(ticker);
    }

    private org.springframework.test.web.servlet.ResultActions cadastrar(String ticker, Mercado mercado) throws Exception {
        return mvc.perform(post("/acoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticker\":\"" + ticker + "\",\"mercado\":\"" + mercado + "\"}"));
    }

    @ParameterizedTest
    @EnumSource(ReferenciaTipoCotacao.class)
    void cadastroPersisteMesmaObservacaoNoHttpAcaoEHistorico(ReferenciaTipoCotacao tipo) throws Exception {
        Mercado mercado = tipo == ReferenciaTipoCotacao.DATA ? Mercado.EUA : Mercado.BRASIL;
        String ticker = ticker();
        responder(ticker, mercado, tipo, false);
        var resposta = cadastrar(ticker, mercado)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value(ticker))
                .andExpect(jsonPath("$.nomeEmpresa").value("Empresa"))
                .andExpect(jsonPath("$.mercado").value(mercado.name()))
                .andExpect(jsonPath("$.moeda").value(mercado == Mercado.BRASIL ? "BRL" : "USD"))
                .andExpect(jsonPath("$.referenciaMercadoCotacao.tipo").value(tipo.name()))
                .andExpect(jsonPath("$.cotacaoObtidaEm").value(OBTENCAO.toString()))
                .andExpect(jsonPath("$.dataHoraCotacao").value(
                        tipo == ReferenciaTipoCotacao.INSTANTE ? "2026-10-02T15:15:00.123456Z" : OBTENCAO.toString()))
                .andReturn().getResponse();
        String json = resposta.getContentAsString();
        // Inspect the decimal token directly: JSON floating-point helpers would lose precision.
        assertTrue(json.contains("\"cotacaoAtual\":" + PRECO.toPlainString()));
        Acao acao = acoes.findByTickerAndMercado(ticker, mercado).orElseThrow();
        mvc.perform(get("/acoes/{id}", acao.getId())).andExpect(status().isOk());
        assertEquals(PRECO, acao.getCotacaoAtual());
        assertEquals(tipo, acao.getReferenciaTipo());
        assertEquals(OBTENCAO, acao.getCotacaoObtidaEm());
        assertEquals(tipo == ReferenciaTipoCotacao.INSTANTE ? MERCADO.withOffsetSameInstant(ZoneOffset.UTC) : null,
                acao.getReferenciaInstante());
        assertEquals(tipo == ReferenciaTipoCotacao.DATA ? DATA : null, acao.getReferenciaData());
        assertEquals(tipo == ReferenciaTipoCotacao.INSTANTE ? acao.getReferenciaInstante() : OBTENCAO,
                acao.getDataHoraCotacao());
        var registros = historico.findByAcaoIdOrderByDataHoraCotacaoAsc(acao.getId());
        assertEquals(1, registros.size());
        HistoricoCotacao h = registros.get(0);
        assertEquals(acao.getCotacaoAtual(), h.getCotacao());
        assertEquals(acao.getDataHoraCotacao(), h.getDataHoraCotacao());
        assertEquals(tipo, h.getReferenciaTipo());
        assertEquals(acao.getReferenciaInstante(), h.getReferenciaInstante());
        assertEquals(acao.getReferenciaData(), h.getReferenciaData());
        assertEquals(OBTENCAO, h.getCotacaoObtidaEm());
        assertEquals("/acoes/" + acao.getId(), java.net.URI.create(resposta.getHeader("Location")).getPath());
        assertTrue(json.contains("\"id\":" + acao.getId()));
        if (tipo == ReferenciaTipoCotacao.DATA) assertTrue(json.contains("\"data\":\"2026-10-02\""));
        else assertTrue(json.contains("\"data\":null"));
        if (tipo == ReferenciaTipoCotacao.INSTANTE) {
            assertTrue(json.contains("\"instante\":\"" + MERCADO.withOffsetSameInstant(ZoneOffset.UTC) + "\""));
        }
        if (tipo != ReferenciaTipoCotacao.INSTANTE) assertTrue(json.contains("\"instante\":null"));
        verify(mercado == Mercado.BRASIL ? brapi : alpha).consultar(ticker);
        verifyNoInteractions(mercado == Mercado.BRASIL ? alpha : brapi);
        verify(clock, times(1)).instant();
    }

    @Test void cadastroAmericanoSemReferenciaMantemDesconhecidaComObtencao() throws Exception {
        String ticker = ticker();
        responder(ticker, Mercado.EUA, ReferenciaTipoCotacao.DESCONHECIDA, false);
        cadastrar(ticker, Mercado.EUA).andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenciaMercadoCotacao.tipo").value("DESCONHECIDA"))
                .andExpect(jsonPath("$.referenciaMercadoCotacao.instante").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.referenciaMercadoCotacao.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.dataHoraCotacao").value(OBTENCAO.toString()))
                .andExpect(jsonPath("$.cotacaoObtidaEm").value(OBTENCAO.toString()));
        Acao acao = acoes.findByTickerAndMercado(ticker, Mercado.EUA).orElseThrow();
        assertEquals(ReferenciaTipoCotacao.DESCONHECIDA, acao.getReferenciaTipo());
        assertNull(acao.getReferenciaData());
        assertNull(acao.getReferenciaInstante());
        assertEquals(OBTENCAO, acao.getCotacaoObtidaEm());
        assertEquals(OBTENCAO, acao.getDataHoraCotacao());
        var registros = historico.findByAcaoIdOrderByDataHoraCotacaoAsc(acao.getId());
        assertEquals(1, registros.size());
        var h = registros.get(0);
        assertEquals(ReferenciaTipoCotacao.DESCONHECIDA, h.getReferenciaTipo());
        assertNull(h.getReferenciaData());
        assertNull(h.getReferenciaInstante());
        assertEquals(OBTENCAO, h.getCotacaoObtidaEm());
        assertEquals(OBTENCAO, h.getDataHoraCotacao());
        assertEquals(acao.getCotacaoAtual(), h.getCotacao());
        verify(alpha).consultar(ticker);
        verifyNoInteractions(brapi);
    }

    @ParameterizedTest
    @EnumSource(Mercado.class)
    void referenciaPresenteInvalidaNaoPersisteNemCapturaObtencao(Mercado mercado) throws Exception {
        String ticker = ticker();
        responder(ticker, mercado, ReferenciaTipoCotacao.DESCONHECIDA, true);
        long total = historico.count();
        cadastrar(ticker, mercado).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REFERENCIA_TEMPORAL_INVALIDA"));
        assertFalse(acoes.existsByTickerAndMercado(ticker, mercado));
        assertEquals(total, historico.count());
        verify(historico, never()).saveAndFlush(any());
        verify(clock, never()).instant();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"1969-12-31T23:59:59.999999Z", "2026-10-04T15:30:17.987655Z"})
    void dominioFinanceiroInvalidoNaoCriaAcaoOuHistorico(String value) throws Exception {
        String ticker = ticker();
        doReturn(new CotacaoData(ticker, "Empresa", "BRL", PRECO,
                OffsetDateTime.parse(value), false)).when(brapi).consultar(ticker);
        long total = historico.count();
        cadastrar(ticker, Mercado.BRASIL).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REFERENCIA_TEMPORAL_INVALIDA"));
        assertFalse(acoes.existsByTickerAndMercado(ticker, Mercado.BRASIL));
        assertEquals(total, historico.count());
        verify(historico, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"0001-01-01", "9999-12-31", "2000-02-29"})
    void dominioCivilPersisteSemComparacaoFutura(String value) throws Exception {
        String ticker = ticker();
        doReturn(new CotacaoData(ticker, "Empresa", "USD", PRECO, null, false,
                LocalDate.parse(value), false)).when(alpha).consultar(ticker);
        cadastrar(ticker, Mercado.EUA).andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenciaMercadoCotacao.data").value(value));
        var acao = acoes.findAll().stream().filter(a -> a.getTicker().equals(ticker)).findFirst().orElseThrow();
        assertEquals(value, jdbc.queryForObject("select cast(referencia_data as varchar) from acao where id = ?",
                String.class, acao.getId()), "DATA civil deve permanecer exata no banco");
        assertEquals(LocalDate.parse(value), acao.getReferenciaData());
        assertEquals(LocalDate.parse(value), historico.findByAcaoIdOrderByDataHoraCotacaoAsc(acao.getId()).get(0).getReferenciaData());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"1960-01-01T00:00:00Z", "2027-01-01T00:00:00Z"})
    void leituraPersistidaNaoRevalidaLimitesDeAquisicao(String value) throws Exception {
        var referencia = OffsetDateTime.parse(value);
        var acao = acoes.saveAndFlush(new Acao(ticker(), "Anterior", Mercado.BRASIL, Moeda.BRL,
                PRECO, referencia, ReferenciaTipoCotacao.INSTANTE, referencia, null, OBTENCAO));
        mvc.perform(get("/acoes/{id}", acao.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.referenciaMercadoCotacao.instante").value(value))
                .andExpect(jsonPath("$.cotacaoObtidaEm").value(OBTENCAO.toString()));
        verifyNoInteractions(brapi, alpha);
        verify(clock, never()).instant();
    }

    @Test void falhaDepoisDoInsertHistoricoDesfazAcaoEHistorico() {
        String ticker = ticker();
        responder(ticker, Mercado.BRASIL, ReferenciaTipoCotacao.INSTANTE, false);
        long total = historico.count();
        doAnswer(i -> {
            entityManager.persist(i.getArgument(0, HistoricoCotacao.class));
            entityManager.flush();
            throw new DataIntegrityViolationException("falha depois do insert");
        }).when(historico).saveAndFlush(any(HistoricoCotacao.class));
        assertThrows(DataIntegrityViolationException.class,
                () -> service.cadastrar(new com.projeto.dto.AcaoCreateRequest(ticker, Mercado.BRASIL)));
        assertFalse(acoes.existsByTickerAndMercado(ticker, Mercado.BRASIL));
        assertEquals(total, historico.count());
        verify(brapi).consultar(ticker);
    }

    @Test void leituraLegadaPreservaObtencaoNula() throws Exception {
        Acao legado = acoes.saveAndFlush(new Acao(ticker(), "Legado", Mercado.BRASIL, Moeda.BRL,
                PRECO, MERCADO));
        mvc.perform(get("/acoes/{id}", legado.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.referenciaMercadoCotacao.tipo").value("DESCONHECIDA"))
                .andExpect(jsonPath("$.cotacaoObtidaEm").value(org.hamcrest.Matchers.nullValue()));
        verifyNoInteractions(brapi, alpha);
    }

    @Test
    void nanosNaoPersistiveisDoProviderNaoCriamEstado() throws Exception {
        String ticker = ticker();
        responder(ticker, Mercado.BRASIL, ReferenciaTipoCotacao.INSTANTE, false);
        doReturn(new CotacaoData(ticker, "Empresa", "BRL", PRECO,
                OffsetDateTime.parse("2026-10-02T15:15:00.123456789Z"), false)).when(brapi).consultar(ticker);
        long total = historico.count();
        cadastrar(ticker, Mercado.BRASIL).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REFERENCIA_TEMPORAL_INVALIDA"));
        assertFalse(acoes.existsByTickerAndMercado(ticker, Mercado.BRASIL));
        assertEquals(total, historico.count());
        verify(historico, never()).saveAndFlush(any());
    }

    @Test
    void clockDeAquisicaoConfiguradoPermiteCadastroComFonteBrutaDeNanos() throws Exception {
        String ticker = ticker();
        responder(ticker, Mercado.BRASIL, ReferenciaTipoCotacao.INSTANTE, false);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-04T15:30:17.123456789Z"));
        long total = historico.count();
        cadastrar(ticker, Mercado.BRASIL).andExpect(status().isCreated())
                .andExpect(jsonPath("$.cotacaoObtidaEm").value("2026-10-04T15:30:17.123456Z"));
        var acao = acoes.findByTickerAndMercado(ticker, Mercado.BRASIL).orElseThrow();
        var expected = OffsetDateTime.parse("2026-10-04T15:30:17.123456Z");
        assertEquals(expected, acao.getCotacaoObtidaEm());
        assertEquals(total + 1, historico.count());
        assertEquals(expected, historico.findByAcaoIdOrderByDataHoraCotacaoAsc(acao.getId()).get(0).getCotacaoObtidaEm());
    }
}
