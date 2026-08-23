package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Corretora;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.integrations.cep.ViaCepAdapter;
import com.projeto.integrations.cnpj.BrasilApiAdapter;
import com.projeto.integrations.cotacao.AlphaVantageAdapter;
import com.projeto.integrations.cotacao.BrapiAdapter;
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
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
                .andExpect(jsonPath("$.precoUnitario").value(32.47))
                .andExpect(jsonPath("$.dataOperacao").value("2026-08-10"))
                .andExpect(jsonPath("$.ordemNoDia").value(1))
                .andExpect(jsonPath("$.valorTotal").value(3247.0))
                .andExpect(jsonPath("$.acaoId").doesNotExist())
                .andExpect(jsonPath("$.cotacaoAtual").doesNotExist())
                .andExpect(jsonPath("$.cotacaoHistorica").doesNotExist());

        var saved = operacaoRepository.findAll().get(0);
        assertNull(saved.getCorretora());
        assertEquals(new BigDecimal("32.470000"), saved.getPrecoUnitario());
        assertEquals(new BigDecimal("3247.000000000000"), saved.getValorTotal());
        assertEquals(new BigDecimal("88.000000"), acaoRepository.findById(acao.getId()).orElseThrow().getCotacaoAtual());
        assertEquals("Carteira BR", carteiraRepository.findById(carteira.getId()).orElseThrow().getNome());
        assertNoExternalCalls();
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
                .andExpect(jsonPath("$.precoUnitario").value(200.123456))
                .andExpect(jsonPath("$.valorTotal").value(300.185184));

        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(carteira.getId(), "AAPL", "EUA", broker.getId(), "VENDA", "0.500000", "210.000000", 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("VENDA"))
                .andExpect(jsonPath("$.valorTotal").value(105.0));

        assertEquals(2, operacaoRepository.count());
        assertNoExternalCalls();
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
        assertNoExternalCalls();
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
        assertNoExternalCalls();
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDEM_OPERACAO_DUPLICADA"));

        mockMvc.perform(post("/operacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(carteira.getId(), "PETR4", "BRASIL", null, "VENDA", "2", "11", 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POSICAO_INSUFICIENTE"))
                .andExpect(jsonPath("$.details.quantidadeDisponivel").value(1.0));

        assertEquals(1, operacaoRepository.count());
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
    void exposesNoAdditionalOperationRoutes() throws Exception {
        mockMvc.perform(get("/operacoes"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/operacoes/{id}", 1L))
                .andExpect(status().isNotFound());
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
        return "{" +
                "\"carteiraId\":" + portfolioId +
                ",\"ticker\":\"" + ticker + "\"" +
                ",\"mercado\":\"" + market + "\"" +
                broker +
                ",\"tipo\":\"" + type + "\"" +
                ",\"quantidade\":" + quantity +
                ",\"precoUnitario\":" + price +
                ",\"dataOperacao\":\"2026-08-10\"" +
                ",\"ordemNoDia\":" + order +
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

    private void assertNoExternalCalls() {
        verify(brapiAdapter, never()).consultar(anyString());
        verify(alphaVantageAdapter, never()).consultar(anyString());
        verify(brasilApiAdapter, never()).consultar(anyString());
        verify(viaCepAdapter, never()).consultar(anyString());
    }
}
