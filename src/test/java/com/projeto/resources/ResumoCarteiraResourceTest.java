package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Acao;
import com.projeto.entities.Carteira;
import com.projeto.entities.Mercado;
import com.projeto.entities.Moeda;
import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.CorretoraRepository;
import com.projeto.repositories.OperacaoRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResumoCarteiraResourceTest {

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

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void cleanDatabase() {
        operacaoRepository.deleteAll();
        carteiraRepository.deleteAll();
        acaoRepository.deleteAll();
        corretoraRepository.deleteAll();
    }

    @Test
    void returnsEmptySummaryWithoutLocationForExistingEmptyPortfolio() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira vazia"));

        mockMvc.perform(get("/carteiras/{id}/resumo", carteira.getId()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$", Matchers.aMapWithSize(2)))
                .andExpect(jsonPath("$.carteiraId").value(carteira.getId()))
                .andExpect(jsonPath("$.resumos", Matchers.hasSize(0)));
    }

    @Test
    void returnsNotFoundFromCentralizedHandler() throws Exception {
        mockMvc.perform(get("/carteiras/{id}/resumo", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(
                        "/carteiras/" + Long.MAX_VALUE + "/resumo"
                ));
    }

    @Test
    void consolidatesBrlAndFractionalUsdWithoutNPlusOneOrSideEffects() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira multimoeda"));
        Acao petr4 = acaoRepository.saveAndFlush(action(
                "PETR4", Mercado.BRASIL, Moeda.BRL, "35.500000"
        ));
        Acao vale3 = acaoRepository.saveAndFlush(action(
                "VALE3", Mercado.BRASIL, Moeda.BRL, "70.000000"
        ));
        Acao aapl = acaoRepository.saveAndFlush(action(
                "AAPL", Mercado.EUA, Moeda.USD, "224.410000"
        ));
        save(carteira, petr4, TipoOperacao.COMPRA, "100", "32", "2026-08-01", 1);
        save(carteira, vale3, TipoOperacao.COMPRA, "10", "60", "2026-08-01", 1);
        save(carteira, aapl, TipoOperacao.COMPRA, "0.500000", "200", "2026-08-01", 1);
        long operations = operacaoRepository.count();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        mockMvc.perform(get("/carteiras/{id}/resumo", carteira.getId()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.resumos", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.resumos[0]", Matchers.aMapWithSize(4)))
                .andExpect(jsonPath("$.resumos[0].moeda").value("BRL"))
                .andExpect(content().string(Matchers.containsString(
                        "\"custoTotalPosicoes\":3800.000000000000"
                )))
                .andExpect(content().string(Matchers.containsString(
                        "\"patrimonioAtual\":4250.000000000000"
                )))
                .andExpect(content().string(Matchers.containsString(
                        "\"resultadoNaoRealizadoTotal\":450.000000000000"
                )))
                .andExpect(jsonPath("$.resumos[1].moeda").value("USD"))
                .andExpect(content().string(Matchers.containsString(
                        "\"custoTotalPosicoes\":100.000000000000"
                )))
                .andExpect(content().string(Matchers.containsString(
                        "\"patrimonioAtual\":112.205000000000"
                )))
                .andExpect(content().string(Matchers.containsString(
                        "\"resultadoNaoRealizadoTotal\":12.205000000000"
                )))
                .andExpect(jsonPath("$.resumos[0].rentabilidadePercentual").doesNotExist())
                .andExpect(jsonPath("$.resumos[0].resultadoRealizado").doesNotExist());

        assertEquals(2, statistics.getPrepareStatementCount());
        assertEquals(operations, operacaoRepository.count());
        assertEquals(new BigDecimal("35.500000"),
                acaoRepository.findById(petr4.getId()).orElseThrow().getCotacaoAtual());
    }

    @Test
    void matchesPatrimonyExactlyForOneAndMultipleCurrencies() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira equivalência"));
        Acao petr4 = acaoRepository.saveAndFlush(action(
                "PETR4", Mercado.BRASIL, Moeda.BRL, "35.500000"
        ));
        Acao aapl = acaoRepository.saveAndFlush(action(
                "AAPL", Mercado.EUA, Moeda.USD, "224.410000"
        ));
        save(carteira, petr4, TipoOperacao.COMPRA, "100", "32", "2026-08-01", 1);
        save(carteira, aapl, TipoOperacao.COMPRA, "0.500000", "200", "2026-08-01", 1);

        MvcResult patrimonio = mockMvc.perform(get(
                        "/carteiras/{id}/patrimonio", carteira.getId()
                ))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult resumo = mockMvc.perform(get("/carteiras/{id}/resumo", carteira.getId()))
                .andExpect(status().isOk())
                .andReturn();

        String patrimonioJson = patrimonio.getResponse().getContentAsString();
        String resumoJson = resumo.getResponse().getContentAsString();
        assertEquals(true, patrimonioJson.contains("3550.000000000000"));
        assertEquals(true, resumoJson.contains("3550.000000000000"));
        assertEquals(true, patrimonioJson.contains("112.205000000000"));
        assertEquals(true, resumoJson.contains("112.205000000000"));
    }

    @Test
    void reflectsPartialSaleOmitsTotalSaleAndUsesOnlyNewOpenCycle() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira ciclos"));
        Acao partial = acaoRepository.saveAndFlush(action(
                "PART3", Mercado.BRASIL, Moeda.BRL, "15.000000"
        ));
        Acao closed = acaoRepository.saveAndFlush(action(
                "CLOS3", Mercado.BRASIL, Moeda.BRL, "99.000000"
        ));
        Acao cycle = acaoRepository.saveAndFlush(action(
                "CYCL3", Mercado.BRASIL, Moeda.BRL, "30.000000"
        ));
        save(carteira, partial, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1);
        save(carteira, partial, TipoOperacao.VENDA, "40", "15", "2026-08-02", 1);
        save(carteira, closed, TipoOperacao.COMPRA, "10", "10", "2026-08-01", 1);
        save(carteira, closed, TipoOperacao.VENDA, "10", "20", "2026-08-02", 1);
        save(carteira, cycle, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1);
        save(carteira, cycle, TipoOperacao.VENDA, "100", "15", "2026-08-02", 1);
        save(carteira, cycle, TipoOperacao.COMPRA, "2", "25", "2026-08-03", 1);

        mockMvc.perform(get("/carteiras/{id}/resumo", carteira.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumos", Matchers.hasSize(1)))
                .andExpect(content().string(Matchers.containsString(
                        "\"custoTotalPosicoes\":650.000000000000"
                )))
                .andExpect(content().string(Matchers.containsString(
                        "\"patrimonioAtual\":960.000000000000"
                )))
                .andExpect(content().string(Matchers.containsString(
                        "\"resultadoNaoRealizadoTotal\":310.000000000000"
                )));
    }

    private Operacao save(
            Carteira carteira,
            Acao acao,
            TipoOperacao type,
            String quantity,
            String price,
            String date,
            int order
    ) {
        BigDecimal normalizedQuantity = new BigDecimal(quantity).setScale(6);
        BigDecimal normalizedPrice = new BigDecimal(price).setScale(6);
        return operacaoRepository.saveAndFlush(new Operacao(
                carteira, acao, null, type, normalizedQuantity, normalizedPrice,
                LocalDate.parse(date), order,
                normalizedQuantity.multiply(normalizedPrice).setScale(12)
        ));
    }

    private Carteira portfolio(String name) {
        return new Carteira(name, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private Acao action(String ticker, Mercado market, Moeda currency, String quote) {
        return new Acao(
                ticker, "Empresa " + ticker, market, currency, new BigDecimal(quote),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
    }
}
