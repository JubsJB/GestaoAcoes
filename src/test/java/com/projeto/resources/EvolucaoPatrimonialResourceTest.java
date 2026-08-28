package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Carteira;
import com.projeto.entities.Moeda;
import com.projeto.entities.SnapshotCarteira;
import com.projeto.entities.SnapshotCarteiraMoeda;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.SnapshotCarteiraMoedaRepository;
import com.projeto.repositories.SnapshotCarteiraRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvolucaoPatrimonialResourceTest {

    @Autowired MockMvc mockMvc;
    @Autowired CarteiraRepository carteiraRepository;
    @Autowired SnapshotCarteiraRepository snapshotRepository;
    @Autowired SnapshotCarteiraMoedaRepository componenteRepository;

    @BeforeEach
    void cleanDatabase() {
        componenteRepository.deleteAll();
        snapshotRepository.deleteAll();
        carteiraRepository.deleteAll();
    }

    @Test
    void returnsCompleteOrderedContractIncludingSnapshotIdentityAndExactDecimal() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira("Histórico"));
        SnapshotCarteira tarde = snapshotRepository.saveAndFlush(snapshot(carteira, "2026-08-28T14:00:00Z"));
        SnapshotCarteira cedo = snapshotRepository.saveAndFlush(snapshot(carteira, "2026-08-28T10:00:00Z"));
        componenteRepository.saveAndFlush(new SnapshotCarteiraMoeda(
                tarde, Moeda.USD, new BigDecimal("50.000000000000")));
        componenteRepository.saveAndFlush(new SnapshotCarteiraMoeda(
                tarde, Moeda.BRL, new BigDecimal("1000.123456789012")));

        mockMvc.perform(get("/carteiras/{id}/evolucao-patrimonial", carteira.getId()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$", Matchers.aMapWithSize(2)))
                .andExpect(jsonPath("$.carteiraId").value(carteira.getId()))
                .andExpect(jsonPath("$.pontos", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.pontos[0].snapshotId").value(cedo.getId()))
                .andExpect(jsonPath("$.pontos[0].dataHoraSnapshot").value("2026-08-28T10:00:00Z"))
                .andExpect(jsonPath("$.pontos[0].patrimonios", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.pontos[1].snapshotId").value(tarde.getId()))
                .andExpect(jsonPath("$.pontos[1].patrimonios", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.pontos[1].patrimonios[0].moeda").value("BRL"))
                .andExpect(jsonPath("$.pontos[1].patrimonios[1].moeda").value("USD"))
                .andExpect(content().string(Matchers.containsString(
                        "\"patrimonioAtual\":1000.123456789012")));
    }

    @Test
    void returnsEmptySeriesForExistingPortfolioAndCentralized404ForMissingPortfolio() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira("Sem histórico"));

        mockMvc.perform(get("/carteiras/{id}/evolucao-patrimonial", carteira.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pontos", Matchers.hasSize(0)));

        mockMvc.perform(get("/carteiras/{id}/evolucao-patrimonial", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void doesNotExposeAliasOrAcceptSpeculativeQueryParameters() throws Exception {
        Carteira carteira = carteiraRepository.saveAndFlush(carteira("Contrato"));

        mockMvc.perform(get("/carteiras/{id}/evolucao", carteira.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/carteiras/{id}/evolucao-patrimonial", carteira.getId())
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pontos", Matchers.hasSize(0)));
    }

    private Carteira carteira(String nome) {
        return new Carteira(nome, OffsetDateTime.parse("2026-08-01T10:00:00Z"));
    }

    private SnapshotCarteira snapshot(Carteira carteira, String instante) {
        return new SnapshotCarteira(carteira, OffsetDateTime.parse(instante));
    }
}
