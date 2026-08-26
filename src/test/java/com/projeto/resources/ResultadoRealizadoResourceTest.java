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
import com.projeto.services.exceptions.ErrorCodes;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
class ResultadoRealizadoResourceTest {

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

    @BeforeEach
    void cleanDatabase() {
        operacaoRepository.deleteAll();
        carteiraRepository.deleteAll();
        acaoRepository.deleteAll();
        corretoraRepository.deleteAll();
    }

    @Test
    void returnsEmptyWithoutLocationForPortfolioWithoutSalesAndNotFoundForMissingPortfolio() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Sem vendas"));
        Acao acao = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        save(carteira, acao, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1);
        long operationCount = operacaoRepository.count();

        mockMvc.perform(get("/carteiras/{id}/resultados-realizados", carteira.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andExpect(header().doesNotExist("Location"));

        mockMvc.perform(get("/carteiras/{id}/resultados-realizados", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(
                        "/carteiras/" + Long.MAX_VALUE + "/resultados-realizados"
                ));

        assertEquals(operationCount, operacaoRepository.count());
    }

    @Test
    void returnsAccumulatedResultsPerActionIncludingZeroClosedCyclesAndFractionalUsSales() throws Exception {
        Carteira selected = carteiraRepository.saveAndFlush(portfolio("Selecionada"));
        Carteira other = carteiraRepository.saveAndFlush(portfolio("Outra"));
        Acao petr4 = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        Acao vale3 = acaoRepository.saveAndFlush(action("VALE3", Mercado.BRASIL, Moeda.BRL));
        Acao aapl = acaoRepository.saveAndFlush(action("AAPL", Mercado.EUA, Moeda.USD));
        Acao noSale = acaoRepository.saveAndFlush(action("WEGE3", Mercado.BRASIL, Moeda.BRL));

        save(selected, petr4, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1);
        save(selected, petr4, TipoOperacao.VENDA, "20", "15", "2026-08-02", 1);
        save(selected, petr4, TipoOperacao.VENDA, "30", "8", "2026-08-03", 1);
        save(selected, petr4, TipoOperacao.VENDA, "50", "10", "2026-08-04", 1);
        save(selected, petr4, TipoOperacao.COMPRA, "50", "20", "2026-08-05", 1);
        save(selected, petr4, TipoOperacao.VENDA, "10", "22", "2026-08-06", 1);
        save(selected, vale3, TipoOperacao.COMPRA, "10", "60", "2026-08-01", 1);
        save(selected, vale3, TipoOperacao.VENDA, "5", "60", "2026-08-02", 1);
        save(selected, aapl, TipoOperacao.COMPRA, "0.500000", "200", "2026-08-01", 1);
        save(selected, aapl, TipoOperacao.VENDA, "0.250000", "220", "2026-08-02", 1);
        save(selected, noSale, TipoOperacao.COMPRA, "1", "1", "2026-08-01", 1);
        save(other, petr4, TipoOperacao.COMPRA, "10", "1", "2026-08-01", 1);
        save(other, petr4, TipoOperacao.VENDA, "10", "101", "2026-08-02", 1);
        long operationCount = operacaoRepository.count();

        mockMvc.perform(get("/carteiras/{id}/resultados-realizados", selected.getId()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$", Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0]", Matchers.aMapWithSize(6)))
                .andExpect(jsonPath("$[0].acaoId").value(petr4.getId()))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[0].nomeEmpresa").value("PETR4 Empresa"))
                .andExpect(jsonPath("$[0].mercado").value("BRASIL"))
                .andExpect(jsonPath("$[0].moeda").value("BRL"))
                .andExpect(content().string(Matchers.containsString(
                        "\"resultadoRealizado\":60.000000000000"
                )))
                .andExpect(jsonPath("$[1].ticker").value("VALE3"))
                .andExpect(jsonPath("$[1].resultadoRealizado").value(0.0))
                .andExpect(jsonPath("$[2].ticker").value("AAPL"))
                .andExpect(jsonPath("$[2].moeda").value("USD"))
                .andExpect(content().string(Matchers.containsString(
                        "\"resultadoRealizado\":5.000000000000"
                )))
                .andExpect(jsonPath("$[0].quantidadeAtual").doesNotExist())
                .andExpect(jsonPath("$[0].resultadoNaoRealizado").doesNotExist())
                .andExpect(jsonPath("$[0].rentabilidadePercentual").doesNotExist());

        assertEquals(operationCount, operacaoRepository.count());
    }

    @Test
    void rejectsPersistedNegativeBalanceAsInconsistentHistoryWithoutPartialResponse() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Inconsistente"));
        Acao valid = acaoRepository.saveAndFlush(action("PETR4", Mercado.BRASIL, Moeda.BRL));
        Acao invalid = acaoRepository.saveAndFlush(action("VALE3", Mercado.BRASIL, Moeda.BRL));
        save(carteira, valid, TipoOperacao.COMPRA, "10", "10", "2026-08-01", 1);
        save(carteira, valid, TipoOperacao.VENDA, "1", "11", "2026-08-02", 1);
        save(carteira, invalid, TipoOperacao.VENDA, "1", "60", "2026-08-03", 1);

        mockMvc.perform(get("/carteiras/{id}/resultados-realizados", carteira.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    private Carteira portfolio(String name) {
        return new Carteira(name, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private Acao action(String ticker, Mercado mercado, Moeda moeda) {
        return new Acao(
                ticker,
                ticker + " Empresa",
                mercado,
                moeda,
                new BigDecimal("99.123456"),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
    }

    private void save(
            Carteira carteira,
            Acao acao,
            TipoOperacao tipo,
            String quantidade,
            String preco,
            String data,
            int ordem
    ) {
        BigDecimal quantity = new BigDecimal(quantidade).setScale(6);
        BigDecimal price = new BigDecimal(preco).setScale(6);
        operacaoRepository.saveAndFlush(new Operacao(
                carteira,
                acao,
                null,
                tipo,
                quantity,
                price,
                LocalDate.parse(data),
                ordem,
                quantity.multiply(price).setScale(12)
        ));
    }
}
