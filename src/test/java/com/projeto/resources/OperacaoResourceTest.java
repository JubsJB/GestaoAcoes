package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Corretora;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.integrations.cep.ViaCepAdapter;
import com.projeto.integrations.cnpj.BrasilApiAdapter;
import com.projeto.integrations.cotacao.AlphaVantageAdapter;
import com.projeto.integrations.cotacao.BrapiAdapter;
import com.projeto.integrations.cotacao.CotacaoHistoricaProvider;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.CorretoraRepository;
import com.projeto.repositories.OperacaoRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperacaoResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OperacaoRepository operacaoRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private AcaoRepository acaoRepository;

    @Autowired
    private CorretoraRepository corretoraRepository;

    @MockitoSpyBean
    private BrapiAdapter brapiAdapter;

    @MockitoSpyBean
    private AlphaVantageAdapter alphaVantageAdapter;

    @MockitoSpyBean
    private BrasilApiAdapter brasilApiAdapter;

    @MockitoSpyBean
    private ViaCepAdapter viaCepAdapter;

    @MockitoSpyBean(name = "brapiHistoricoStub")
    private CotacaoHistoricaProvider brapiHistorico;

    @MockitoSpyBean(name = "alphaHistoricoStub")
    private CotacaoHistoricaProvider alphaHistorico;

    @BeforeEach
    void cleanDatabase() {
        operacaoRepository.deleteAll();
        carteiraRepository.deleteAll();
        acaoRepository.deleteAll();
        corretoraRepository.deleteAll();
    }

    @Test
    void createsBrazilianPurchaseWithoutBrokerWithCompleteDtoAndLocation() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira BR"));
        Acao acao = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL, "88.000000"));

        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(carteira.getId(), "  petr4  ", "BRASIL", null, "COMPRA", "100", "32.47", 1)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.matchesPattern(".*/operacoes/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.carteiraId").value(carteira.getId()))
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.mercado").value("BRASIL"))
                .andExpect(jsonPath("$.corretoraId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.tipo").value("COMPRA"))
                .andExpect(jsonPath("$.quantidade").value(100.0))
                .andExpect(jsonPath("$.precoUnitario").value(32.0))
                .andExpect(jsonPath("$.dataOperacao").value("2026-08-10"))
                .andExpect(jsonPath("$.ordemNoDia").value(1))
                .andExpect(jsonPath("$.valorTotal").value(3200.0))
                .andExpect(jsonPath("$.acaoId").doesNotExist())
                .andExpect(jsonPath("$.cotacaoAtual").doesNotExist())
                .andExpect(jsonPath("$.cotacaoHistorica").doesNotExist());

        var saved = operacaoRepository.findAll().get(0);
        assertNull(saved.getCorretora());
        assertEquals(new BigDecimal("32.000000"), saved.getPrecoUnitario());
        assertEquals(new BigDecimal("3200.000000000000"), saved.getValorTotal());
        assertEquals(new BigDecimal("88.000000"), acaoRepository.findById(acao.getId()).orElseThrow().getCotacaoAtual());
        assertEquals("Carteira BR", carteiraRepository.findById(carteira.getId()).orElseThrow().getNome());
        verify(brapiHistorico).consultarFechamento("PETR4", LocalDate.of(2026, 8, 10));
        verify(alphaHistorico, never()).consultarFechamento(anyString(), org.mockito.ArgumentMatchers.any());
        assertNoCurrentProviderCalls();
    }

    @Test
    void createsAmericanFractionalSaleWithExistingBroker() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira EUA"));
        acaoRepository.saveAndFlush(action("AAPL", Mercado.EUA, Moeda.USD, "224.410000"));
        Corretora broker = corretoraRepository.saveAndFlush(broker());

        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(carteira.getId(), "AAPL", "EUA", broker.getId(), "COMPRA", "1.500000", "200.123456", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.corretoraId").value(broker.getId()))
                .andExpect(jsonPath("$.quantidade").value(1.5))
                .andExpect(jsonPath("$.precoUnitario").value(32.0))
                .andExpect(jsonPath("$.valorTotal").value(48.0));

        verify(alphaHistorico).consultarFechamento("AAPL", LocalDate.of(2026, 8, 10));
        clearInvocations(brapiHistorico, alphaHistorico);

        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(carteira.getId(), "AAPL", "EUA", broker.getId(), "VENDA", "0.500000", "210.000000", 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("VENDA"))
                .andExpect(jsonPath("$.valorTotal").value(105.0));

        assertEquals(2, operacaoRepository.count());
        assertNoHistoricalProviderCalls();
        assertNoCurrentProviderCalls();
    }

    @Test
    void rejectsUnknownControlledFieldsAndInvalidType() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL, "32.000000"));
        String base = validRequest(carteira.getId(), "PETR4", "BRASIL", null, "COMPRA", "1", "10", 1);

        for (String field : new String[]{"id", "acaoId", "valorTotal", "cotacaoAtual", "cotacaoHistorica", "desconhecido"}) {
            String invalid = base.substring(0, base.length() - 1) + ",\"" + field + "\":1}";
            assertInvalidContract(invalid);
        }
        assertInvalidContract(base.replace("\"COMPRA\"", "\"DIVIDENDO\""));

        assertEquals(0, operacaoRepository.count());
        assertNoHistoricalProviderCalls();
        assertNoCurrentProviderCalls();
    }

    @Test
    void rejectsCompleteDiscriminatorAndVariantMatrixThroughMockMvc() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Contrato HTTP completo"));
        acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL, "32.000000"));
        String purchase = validRequest(carteira.getId(), "PETR4", "BRASIL", null,
                "COMPRA", "1", "10", 1);
        String sale = validRequest(carteira.getId(), "PETR4", "BRASIL", null,
                "VENDA", "1", "10", 1);

        for (String invalid : new String[]{
                purchase.replace(",\"tipo\":\"COMPRA\"", ""),
                purchase.replace("\"tipo\":\"COMPRA\"", "\"tipo\":null"),
                purchase.replace("\"COMPRA\"", "\"DIVIDENDO\""),
                purchase.replace("\"COMPRA\"", "\"compra\""),
                addField(purchase, "\"precoUnitario\":10"),
                addField(purchase, "\"precoUnitario\":null"),
                addField(purchase, "\"ordemNoDia\":1"),
                addField(sale, "\"ordemNoDia\":1"),
                sale.replace(",\"precoUnitario\":10", ""),
                addField(purchase, "\"campoDesconhecido\":true"),
                addField(sale, "\"campoDesconhecido\":true")
        }) {
            assertInvalidContract(invalid);
        }

        assertEquals(0, operacaoRepository.count());
        assertNoHistoricalProviderCalls();
        assertNoCurrentProviderCalls();
    }

    @Test
    void returnsNotFoundForMissingPortfolioActionOrBrokerWithoutAutoCreation() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL, "32.000000"));

        assertNotFound(validRequest(Long.MAX_VALUE, "PETR4", "BRASIL", null, "COMPRA", "1", "10", 1));
        assertNotFound(validRequest(carteira.getId(), "VALE3", "BRASIL", null, "COMPRA", "1", "10", 1));
        assertNotFound(validRequest(carteira.getId(), "PETR4", "BRASIL", Long.MAX_VALUE, "COMPRA", "1", "10", 1));

        assertEquals(1, acaoRepository.count());
        assertEquals(0, operacaoRepository.count());
        assertNoCurrentProviderCalls();
    }

    @Test
    void returnsStandardizedValidationAndBusinessErrorsWithoutPartialPersistence() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL, "32.000000"));

        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(carteira.getId(), "PETR4", "BRASIL", null, "COMPRA", "0.5", "10", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"))
                .andExpect(jsonPath("$.details.quantidade").exists());

        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(carteira.getId(), "PETR4", "BRASIL", null, "COMPRA", "1", "10", 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(carteira.getId(), "PETR4", "BRASIL", null, "COMPRA", "1", "10", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ordemNoDia").value(2));

        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(carteira.getId(), "PETR4", "BRASIL", null, "VENDA", "3", "11", 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POSICAO_INSUFICIENTE"))
                .andExpect(jsonPath("$.details.quantidadeDisponivel").value(2.0));

        assertEquals(2, operacaoRepository.count());
    }

    @Test
    void protectsPortfolioDeletionAndPreservesDeletionContractForEligiblePortfolio() throws Exception {
        Carteira withHistory = carteiraRepository.saveAndFlush(portfolio("Com histórico"));
        Carteira empty = carteiraRepository.saveAndFlush(portfolio("Sem histórico"));
        acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL, "32.000000"));
        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(withHistory.getId(), "PETR4", "BRASIL", null, "COMPRA", "1", "10", 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/carteiras/{id}", withHistory.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARTEIRA_POSSUI_OPERACOES"))
                .andExpect(jsonPath("$.details.carteiraId").value(withHistory.getId()));

        assertTrue(carteiraRepository.existsById(withHistory.getId()));
        assertTrue(operacaoRepository.existsByCarteiraId(withHistory.getId()));

        mockMvc.perform(delete("/carteiras/{id}", empty.getId()))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Location"));
        assertFalse(carteiraRepository.existsById(empty.getId()));
        assertEquals(1, operacaoRepository.count());
    }

    @Test
    void listsAllOperationsWithApprovedOrderCompleteDtoAndNoSideEffects() throws Exception {
        Carteira brazilianPortfolio = carteiraRepository.saveAndFlush(portfolio("Carteira BR"));
        Carteira americanPortfolio = carteiraRepository.saveAndFlush(portfolio("Carteira EUA"));
        Acao petr4 = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL, "88.000000"));
        Acao aapl = acaoRepository.saveAndFlush(action("AAPL", Mercado.EUA, Moeda.USD, "224.410000"));
        Corretora broker = corretoraRepository.saveAndFlush(broker());

        Operacao later = operacaoRepository.saveAndFlush(operation(
                brazilianPortfolio, petr4, null, TipoOperacao.VENDA,
                "10", "35", "350", LocalDate.of(2026, 8, 10), 2
        ));
        Operacao earlier = operacaoRepository.saveAndFlush(operation(
                brazilianPortfolio, petr4, broker, TipoOperacao.COMPRA,
                "100", "32.47", "3247", LocalDate.of(2026, 8, 1), 5
        ));
        Operacao firstTie = operacaoRepository.saveAndFlush(operation(
                brazilianPortfolio, petr4, null, TipoOperacao.COMPRA,
                "1", "33", "33", LocalDate.of(2026, 8, 10), 1
        ));
        Operacao secondTie = operacaoRepository.saveAndFlush(operation(
                americanPortfolio, aapl, null, TipoOperacao.COMPRA,
                "0.5", "200", "100", LocalDate.of(2026, 8, 10), 1
        ));
        long countBefore = operacaoRepository.count();

        mockMvc.perform(get("/operacoes"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$", Matchers.hasSize(4)))
                .andExpect(jsonPath("$[0].id").value(earlier.getId()))
                .andExpect(jsonPath("$[0].carteiraId").value(brazilianPortfolio.getId()))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[0].mercado").value("BRASIL"))
                .andExpect(jsonPath("$[0].corretoraId").value(broker.getId()))
                .andExpect(jsonPath("$[0].tipo").value("COMPRA"))
                .andExpect(jsonPath("$[0].quantidade").value(100.0))
                .andExpect(jsonPath("$[0].precoUnitario").value(32.47))
                .andExpect(jsonPath("$[0].dataOperacao").value("2026-08-01"))
                .andExpect(jsonPath("$[0].ordemNoDia").value(5))
                .andExpect(jsonPath("$[0].valorTotal").value(3247.0))
                .andExpect(jsonPath("$[0].cotacaoAtual").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(firstTie.getId()))
                .andExpect(jsonPath("$[1].ordemNoDia").value(1))
                .andExpect(jsonPath("$[1].corretoraId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[2].id").value(secondTie.getId()))
                .andExpect(jsonPath("$[2].ordemNoDia").value(1))
                .andExpect(jsonPath("$[3].id").value(later.getId()))
                .andExpect(jsonPath("$[3].tipo").value("VENDA"))
                .andExpect(jsonPath("$[3].ordemNoDia").value(2));

        assertEquals(countBefore, operacaoRepository.count());
        assertEquals(new BigDecimal("35.000000"),
                operacaoRepository.findById(later.getId()).orElseThrow().getPrecoUnitario());
        assertEquals(new BigDecimal("350.000000000000"),
                operacaoRepository.findById(later.getId()).orElseThrow().getValorTotal());
        assertNoCurrentProviderCalls();
    }

    @Test
    void returnsEmptyArrayWhenNoOperationExists() throws Exception {
        mockMvc.perform(get("/operacoes"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$", Matchers.hasSize(0)));

        assertEquals(0, operacaoRepository.count());
        assertNoCurrentProviderCalls();
    }

    @Test
    void findsOperationByIdWithCompletePersistedResponse() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        Acao acao = acaoRepository.saveAndFlush(action("AAPL", Mercado.EUA, Moeda.USD, "224.410000"));
        Operacao operation = operacaoRepository.saveAndFlush(operation(
                carteira, acao, null, TipoOperacao.COMPRA,
                "0.123456", "32.123456", "3.965833383936",
                LocalDate.of(2026, 8, 10), 1
        ));

        mockMvc.perform(get("/operacoes/{id}", operation.getId()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(operation.getId()))
                .andExpect(jsonPath("$.carteiraId").value(carteira.getId()))
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.mercado").value("EUA"))
                .andExpect(jsonPath("$.corretoraId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.tipo").value("COMPRA"))
                .andExpect(jsonPath("$.quantidade").value(0.123456))
                .andExpect(jsonPath("$.precoUnitario").value(32.123456))
                .andExpect(jsonPath("$.dataOperacao").value("2026-08-10"))
                .andExpect(jsonPath("$.ordemNoDia").value(1))
                .andExpect(jsonPath("$.valorTotal").value(3.965833383936));

        assertEquals(1, operacaoRepository.count());
        assertNoCurrentProviderCalls();
    }

    @Test
    void returnsStandardNotFoundErrorForMissingOperationId() throws Exception {
        mockMvc.perform(get("/operacoes/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Operação não encontrada para o id: " + Long.MAX_VALUE
                ))
                .andExpect(jsonPath("$.path").value("/operacoes/" + Long.MAX_VALUE));

        assertEquals(0, operacaoRepository.count());
        assertNoCurrentProviderCalls();
    }

    @Test
    void listsIsolatedPortfolioHistoryOrEmptyAndRejectsMissingPortfolio() throws Exception {
        Carteira selected = carteiraRepository.saveAndFlush(portfolio("Carteira selecionada"));
        Carteira other = carteiraRepository.saveAndFlush(portfolio("Outra carteira"));
        Carteira empty = carteiraRepository.saveAndFlush(portfolio("Carteira vazia"));
        Acao petr4 = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL, "88.000000"));
        Acao aapl = acaoRepository.saveAndFlush(action("AAPL", Mercado.EUA, Moeda.USD, "224.410000"));
        Operacao later = operacaoRepository.saveAndFlush(operation(
                selected, petr4, null, TipoOperacao.VENDA,
                "10", "35", "350", LocalDate.of(2026, 8, 10), 2
        ));
        Operacao first = operacaoRepository.saveAndFlush(operation(
                selected, petr4, null, TipoOperacao.COMPRA,
                "100", "32", "3200", LocalDate.of(2026, 8, 1), 1
        ));
        Operacao second = operacaoRepository.saveAndFlush(operation(
                selected, aapl, null, TipoOperacao.COMPRA,
                "0.5", "200", "100", LocalDate.of(2026, 8, 1), 1
        ));
        operacaoRepository.saveAndFlush(operation(
                other, petr4, null, TipoOperacao.COMPRA,
                "999", "1", "999", LocalDate.of(2026, 7, 1), 1
        ));
        long countBefore = operacaoRepository.count();

        mockMvc.perform(get("/carteiras/{carteiraId}/operacoes", selected.getId()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$", Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(first.getId()))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[1].id").value(second.getId()))
                .andExpect(jsonPath("$[1].ticker").value("AAPL"))
                .andExpect(jsonPath("$[1].corretoraId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[2].id").value(later.getId()))
                .andExpect(jsonPath("$[*].carteiraId", Matchers.everyItem(Matchers.is(selected.getId().intValue()))));

        mockMvc.perform(get("/carteiras/{carteiraId}/operacoes", empty.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));

        mockMvc.perform(get("/carteiras/{carteiraId}/operacoes", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(
                        "Carteira não encontrada para o id: " + Long.MAX_VALUE
                ))
                .andExpect(jsonPath("$.path").value("/carteiras/" + Long.MAX_VALUE + "/operacoes"));

        assertEquals(countBefore, operacaoRepository.count());
        assertEquals("Carteira selecionada",
                carteiraRepository.findById(selected.getId()).orElseThrow().getNome());
        assertNoCurrentProviderCalls();
    }

    @Test
    void doesNotExposeOperationDeletion() throws Exception {
        mockMvc.perform(delete("/operacoes/{id}", 1L))
                .andExpect(status().isMethodNotAllowed());
    }

    private void assertInvalidContract(String content) throws Exception {
        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));
    }

    private void assertNotFound(String content) throws Exception {
        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/operacoes"));
    }

    private String validRequest(
            Long portfolioId,
            String ticker,
            String market,
            Long brokerId,
            String type,
            String quantity,
            String price,
            int order
    ) {
        String broker = brokerId == null ? "" : ",\"corretoraId\":" + brokerId;
        String unitPrice = "VENDA".equals(type) ? ",\"precoUnitario\":" + price : "";
        return "{" +
                "\"carteiraId\":" + portfolioId +
                ",\"ticker\":\"" + ticker + "\"" +
                ",\"mercado\":\"" + market + "\"" +
                broker +
                ",\"tipo\":\"" + type + "\"" +
                ",\"quantidade\":" + quantity +
                unitPrice +
                ",\"dataOperacao\":\"2026-08-10\"" +
                "}";
    }

    private Carteira portfolio(String name) {
        return new Carteira(name, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private Acao action(String ticker, Mercado market, Moeda currency, String quote) {
        return new Acao(
                ticker,
                "Empresa",
                market,
                currency,
                new BigDecimal(quote),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
    }

    private Operacao operation(
            Carteira carteira,
            Acao acao,
            Corretora corretora,
            TipoOperacao type,
            String quantity,
            String price,
            String total,
            LocalDate date,
            Integer order
    ) {
        return new Operacao(
                carteira,
                acao,
                corretora,
                type,
                new BigDecimal(quantity).setScale(6),
                new BigDecimal(price).setScale(6),
                date,
                order,
                new BigDecimal(total).setScale(12)
        );
    }

    private Corretora broker() {
        return new Corretora(
                "11222333000181",
                "Corretora S.A.",
                null,
                null,
                null,
                "01001000",
                "Praça da Sé",
                null,
                null,
                "Sé",
                "São Paulo",
                "SP",
                "ATIVA",
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
    }

    private void assertNoCurrentProviderCalls() {
        verify(brapiAdapter, never()).consultar(anyString());
        verify(alphaVantageAdapter, never()).consultar(anyString());
        verify(brasilApiAdapter, never()).consultar(anyString());
        verify(viaCepAdapter, never()).consultar(anyString());
    }

    private String addField(String json, String field) {
        return json.substring(0, json.length() - 1) + "," + field + "}";
    }

    private void assertNoHistoricalProviderCalls() {
        verify(brapiHistorico, never()).consultarFechamento(anyString(), org.mockito.ArgumentMatchers.any());
        verify(alphaHistorico, never()).consultarFechamento(anyString(), org.mockito.ArgumentMatchers.any());
    }
}
