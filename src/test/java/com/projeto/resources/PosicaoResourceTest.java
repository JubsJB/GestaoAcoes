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
class PosicaoResourceTest {

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
    void returnsEmptyArrayWithoutLocationForPortfolioWithoutOperations() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira vazia"));
        long portfolioCount = carteiraRepository.count();

        mockMvc.perform(get("/carteiras/{id}/posicoes", carteira.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andExpect(header().doesNotExist("Location"));

        assertEquals(portfolioCount, carteiraRepository.count());
        assertEquals(0, operacaoRepository.count());
    }

    @Test
    void omitsPositionsClosedByTotalSaleAndPreservesHistory() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira encerrada"));
        Acao acao = acaoRepository.saveAndFlush(action(
                "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL, "999.999999"
        ));
        save(carteira, acao, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1);
        save(carteira, acao, TipoOperacao.VENDA, "100", "12", "2026-08-02", 1);

        mockMvc.perform(get("/carteiras/{id}/posicoes", carteira.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        assertEquals(2, operacaoRepository.count());
    }

    @Test
    void returnsCompleteOpenPositionsOrderedByMarketTickerAndActionId() throws Exception {
        Carteira selected = carteiraRepository.saveAndFlush(portfolio("Carteira selecionada"));
        Carteira other = carteiraRepository.saveAndFlush(portfolio("Outra carteira"));
        Acao aapl = acaoRepository.saveAndFlush(action(
                "AAPL", "Apple Inc.", Mercado.EUA, Moeda.USD, "224.410000"
        ));
        Acao petr4 = acaoRepository.saveAndFlush(action(
                "PETR4", "Petróleo Brasileiro S.A.", Mercado.BRASIL, Moeda.BRL, "99.123456"
        ));
        Acao vale3 = acaoRepository.saveAndFlush(action(
                "VALE3", "Vale S.A.", Mercado.BRASIL, Moeda.BRL, "70.000000"
        ));

        save(selected, aapl, TipoOperacao.COMPRA, "0.500000", "200", "2026-08-01", 1);
        save(selected, vale3, TipoOperacao.COMPRA, "10", "60", "2026-08-01", 1);
        save(selected, petr4, TipoOperacao.COMPRA, "100", "10", "2026-08-01", 1);
        save(selected, petr4, TipoOperacao.VENDA, "40", "99", "2026-08-02", 1);
        save(selected, petr4, TipoOperacao.COMPRA, "40", "20", "2026-08-03", 1);
        save(other, petr4, TipoOperacao.COMPRA, "999", "1", "2026-08-01", 1);
        long operationCount = operacaoRepository.count();

        mockMvc.perform(get("/carteiras/{id}/posicoes", selected.getId()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().string(Matchers.containsString("\"precoMedio\":14.000000000000")))
                .andExpect(content().string(Matchers.containsString("\"custoPosicao\":1400.000000000000")))
                .andExpect(jsonPath("$", Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0]", Matchers.aMapWithSize(11)))
                .andExpect(jsonPath("$[0].acaoId").value(petr4.getId()))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[0].nomeEmpresa").value("Petróleo Brasileiro S.A."))
                .andExpect(jsonPath("$[0].mercado").value("BRASIL"))
                .andExpect(jsonPath("$[0].moeda").value("BRL"))
                .andExpect(jsonPath("$[0].quantidadeAtual").value(100.0))
                .andExpect(jsonPath("$[0].precoMedio").value(14.0))
                .andExpect(jsonPath("$[0].custoPosicao").value(1400.0))
                .andExpect(jsonPath("$[0].cotacaoAtual").value(99.123456))
                .andExpect(jsonPath("$[0].dataHoraCotacao").value("2026-08-01T10:00:00Z"))
                .andExpect(jsonPath("$[0].valorAtualPosicao").value(9912.3456))
                .andExpect(jsonPath("$[1].ticker").value("VALE3"))
                .andExpect(jsonPath("$[1].valorAtualPosicao").value(700.0))
                .andExpect(jsonPath("$[2].ticker").value("AAPL"))
                .andExpect(jsonPath("$[2].quantidadeAtual").value(0.5))
                .andExpect(jsonPath("$[2].moeda").value("USD"))
                .andExpect(jsonPath("$[2].valorAtualPosicao").value(112.205))
                .andExpect(jsonPath("$[0].resultadoRealizado").doesNotExist())
                .andExpect(jsonPath("$[0].resultadoNaoRealizado").doesNotExist())
                .andExpect(jsonPath("$[0].rentabilidade").doesNotExist())
                .andExpect(jsonPath("$[0].patrimonio").doesNotExist())
                .andExpect(jsonPath("$[0].snapshot").doesNotExist());

        assertEquals(operationCount, operacaoRepository.count());
        assertEquals(new BigDecimal("99.123456"),
                acaoRepository.findById(petr4.getId()).orElseThrow().getCotacaoAtual());
        assertEquals("Carteira selecionada",
                carteiraRepository.findById(selected.getId()).orElseThrow().getNome());
    }

    @Test
    void returnsStandardNotFoundAndDoesNotExposeAdditionalPositionRoute() throws Exception {
        mockMvc.perform(get("/carteiras/{id}/posicoes", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Carteira não encontrada para o id: " + Long.MAX_VALUE
                ))
                .andExpect(jsonPath("$.path").value("/carteiras/" + Long.MAX_VALUE + "/posicoes"));

        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira"));
        mockMvc.perform(get("/carteiras/{id}/posicoes/{acaoId}", carteira.getId(), 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsConflictForInconsistentHistoryWithoutPartialResponse() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira inconsistente"));
        Acao valid = acaoRepository.saveAndFlush(action(
                "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL, "99.000000"
        ));
        Acao invalid = acaoRepository.saveAndFlush(action(
                "VALE3", "Vale", Mercado.BRASIL, Moeda.BRL, "70.000000"
        ));
        save(carteira, valid, TipoOperacao.COMPRA, "10", "5", "2026-08-01", 1);
        save(carteira, invalid, TipoOperacao.VENDA, "1", "60", "2026-08-02", 1);

        mockMvc.perform(get("/carteiras/{id}/posicoes", carteira.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE))
                .andExpect(jsonPath("$.details.carteiraId").value(carteira.getId()))
                .andExpect(jsonPath("$.details.ticker").value("VALE3"));

        assertEquals(2, operacaoRepository.count());
    }

    @Test
    void returnsUnprocessableEntityWhenPositionExceedsApprovedPrecision() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(portfolio("Carteira grande"));
        Acao acao = acaoRepository.saveAndFlush(action(
                "AAPL", "Apple Inc.", Mercado.EUA, Moeda.USD, "224.410000"
        ));
        String maximum = "9999999999999.999999";
        save(carteira, acao, TipoOperacao.COMPRA, maximum, maximum, "2026-08-01", 1);
        save(carteira, acao, TipoOperacao.COMPRA, maximum, maximum, "2026-08-02", 1);

        mockMvc.perform(get("/carteiras/{id}/posicoes", carteira.getId()))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO));

        assertEquals(2, operacaoRepository.count());
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
                carteira,
                acao,
                null,
                type,
                normalizedQuantity,
                normalizedPrice,
                LocalDate.parse(date),
                order,
                normalizedQuantity.multiply(normalizedPrice).setScale(12)
        ));
    }

    private Carteira portfolio(String name) {
        return new Carteira(name, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private Acao action(
            String ticker,
            String companyName,
            Mercado market,
            Moeda currency,
            String currentQuote
    ) {
        return new Acao(
                ticker,
                companyName,
                market,
                currency,
                new BigDecimal(currentQuote),
                OffsetDateTime.parse("2026-08-01T10:00:00Z")
        );
    }
}
