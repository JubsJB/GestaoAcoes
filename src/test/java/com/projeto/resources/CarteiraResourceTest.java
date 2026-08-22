package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Carteira;
import com.projeto.repositories.CarteiraRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarteiraResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarteiraRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsPortfolioWithNormalizedNameCompleteResponseUtcDateAndLocation() throws Exception {
        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  Carteira  Ágil Principal  \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.matchesPattern(".*/carteiras/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Carteira  Ágil Principal"))
                .andExpect(jsonPath("$.dataCriacao").exists());

        Carteira saved = repository.findAll().get(0);
        assertEquals("Carteira  Ágil Principal", saved.getNome());
        assertEquals(ZoneOffset.UTC, saved.getDataCriacao().getOffset());
    }

    @Test
    void allowsDuplicateNamesThroughPost() throws Exception {
        String request = "{\"nome\":\"Carteira Principal\"}";

        mockMvc.perform(post("/carteiras").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/carteiras").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());

        List<Carteira> saved = repository.findAll();
        assertEquals(2, saved.size());
        assertEquals("Carteira Principal", saved.get(0).getNome());
        assertEquals("Carteira Principal", saved.get(1).getNome());
        assertNotEquals(saved.get(0).getId(), saved.get(1).getId());
    }

    @Test
    void rejectsMissingNullEmptyBlankAndOversizedNamesWithStandardError() throws Exception {
        assertInvalidName("{}");
        assertInvalidName("{\"nome\":null}");
        assertInvalidName("{\"nome\":\"\"}");
        assertInvalidName("{\"nome\":\"   \"}");
        assertInvalidName("{\"nome\":\" " + "a".repeat(256) + " \"}");

        assertEquals(0, repository.count());
    }

    @Test
    void rejectsIdCreationDateAndUnknownProperties() throws Exception {
        assertInvalidContract("{\"nome\":\"Carteira\",\"id\":10}");
        assertInvalidContract("{\"nome\":\"Carteira\",\"dataCriacao\":\"2026-08-21T14:30:00Z\"}");
        assertInvalidContract("{\"nome\":\"Carteira\",\"propriedadeDesconhecida\":true}");

        assertEquals(0, repository.count());
    }

    @Test
    void listsPersistedPortfoliosByAscendingIdWithCompleteUnmodifiedValues() throws Exception {
        OffsetDateTime firstDate = OffsetDateTime.parse("2026-08-18T08:15:00Z");
        OffsetDateTime secondDate = OffsetDateTime.parse("2026-08-19T09:30:00Z");
        Carteira first = repository.saveAndFlush(new Carteira("  Carteira Ágil  ", firstDate));
        Carteira second = repository.saveAndFlush(new Carteira("carteira Principal", secondDate));

        mockMvc.perform(get("/carteiras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(first.getId()))
                .andExpect(jsonPath("$[0].nome").value("  Carteira Ágil  "))
                .andExpect(jsonPath("$[0].dataCriacao").value("2026-08-18T08:15:00Z"))
                .andExpect(jsonPath("$[1].id").value(second.getId()))
                .andExpect(jsonPath("$[1].nome").value("carteira Principal"))
                .andExpect(jsonPath("$[1].dataCriacao").value("2026-08-19T09:30:00Z"));
    }

    @Test
    void returnsEmptyArrayWhenNoPortfolioExists() throws Exception {
        mockMvc.perform(get("/carteiras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    @Test
    void findsPortfolioByIdWithCompleteUnmodifiedValues() throws Exception {
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-08-17T07:45:00Z");
        Carteira persisted = repository.saveAndFlush(new Carteira(
                "  Carteira Ágil sem normalização  ",
                creationDate
        ));

        mockMvc.perform(get("/carteiras/{id}", persisted.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(persisted.getId()))
                .andExpect(jsonPath("$.nome").value("  Carteira Ágil sem normalização  "))
                .andExpect(jsonPath("$.dataCriacao").value("2026-08-17T07:45:00Z"));
    }

    @Test
    void returnsStandardNotFoundErrorWhenPortfolioIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/carteiras/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Carteira não encontrada para o id: " + Long.MAX_VALUE
                ))
                .andExpect(jsonPath("$.path").value("/carteiras/" + Long.MAX_VALUE));
    }

    @Test
    void queriesDoNotMutatePersistedPortfolio() throws Exception {
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-08-16T06:25:00Z");
        Carteira persisted = repository.saveAndFlush(new Carteira("  Nome Preservado  ", creationDate));
        long countBeforeQueries = repository.count();

        mockMvc.perform(get("/carteiras"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/carteiras/{id}", persisted.getId()))
                .andExpect(status().isOk());

        Carteira unchanged = repository.findById(persisted.getId()).orElseThrow();
        assertEquals(countBeforeQueries, repository.count());
        assertEquals("  Nome Preservado  ", unchanged.getNome());
        assertEquals(creationDate, unchanged.getDataCriacao());
    }

    private void assertInvalidName(String content) throws Exception {
        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"))
                .andExpect(jsonPath("$.details.nome").exists());
    }

    private void assertInvalidContract(String content) throws Exception {
        mockMvc.perform(post("/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));
    }
}
