package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Carteira;
import com.projeto.entities.SnapshotCarteira;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.SnapshotCarteiraMoedaRepository;
import com.projeto.repositories.SnapshotCarteiraRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarteiraResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarteiraRepository repository;

    @Autowired
    private SnapshotCarteiraRepository snapshotRepository;

    @Autowired
    private SnapshotCarteiraMoedaRepository snapshotMoedaRepository;

    @BeforeEach
    void cleanDatabase() {
        snapshotMoedaRepository.deleteAll();
        snapshotRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    void createsExplicitEmptySnapshotWithoutBodyWithCanonicalLocation() throws Exception {
        Carteira carteira = repository.saveAndFlush(new Carteira(
                "Carteira vazia",
                OffsetDateTime.parse("2026-08-27T10:00:00Z")
        ));

        mockMvc.perform(post("/carteiras/{id}/snapshots", carteira.getId()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        Matchers.matchesPattern(".*/carteiras/" + carteira.getId() + "/snapshots/\\d+")
                ))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.carteiraId").value(carteira.getId()))
                .andExpect(jsonPath("$.dataHoraSnapshot").exists())
                .andExpect(jsonPath("$.patrimonios").isEmpty());

        assertEquals(1, snapshotRepository.count());
        assertEquals(0, snapshotMoedaRepository.count());
    }

    @Test
    void rejectsSnapshotForMissingPortfolioWithoutPartialPersistence() throws Exception {
        mockMvc.perform(post("/carteiras/999999/snapshots"))
                .andExpect(status().isNotFound());
        assertEquals(0, snapshotRepository.count());
        assertEquals(0, snapshotMoedaRepository.count());
    }

    @Test
    void rejectsDeletionOfPortfolioWithEmptySnapshotAndPreservesHistory() throws Exception {
        Carteira carteira = repository.saveAndFlush(new Carteira(
                "Carteira com fotografia",
                OffsetDateTime.parse("2026-08-27T10:00:00Z")
        ));
        SnapshotCarteira snapshot = snapshotRepository.saveAndFlush(new SnapshotCarteira(
                carteira, OffsetDateTime.parse("2026-08-27T15:00:00Z")
        ));

        mockMvc.perform(delete("/carteiras/{id}", carteira.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARTEIRA_POSSUI_SNAPSHOTS"));

        assertEquals(true, repository.existsById(carteira.getId()));
        assertEquals(true, snapshotRepository.existsById(snapshot.getId()));
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
    void updatesPortfolioNameWithCompleteResponseWithoutLocationAndPreservesImmutableFields() throws Exception {
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-08-15T05:15:00Z");
        Carteira persisted = repository.saveAndFlush(new Carteira("Carteira Original", creationDate));
        Long originalId = persisted.getId();
        long countBeforeUpdate = repository.count();

        mockMvc.perform(patch("/carteiras/{id}", originalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  Carteira  Ágil Principal  \"}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(originalId))
                .andExpect(jsonPath("$.nome").value("Carteira  Ágil Principal"))
                .andExpect(jsonPath("$.dataCriacao").value("2026-08-15T05:15:00Z"));

        Carteira updated = repository.findById(originalId).orElseThrow();
        assertEquals(countBeforeUpdate, repository.count());
        assertEquals(originalId, updated.getId());
        assertEquals("Carteira  Ágil Principal", updated.getNome());
        assertEquals(creationDate, updated.getDataCriacao());
    }

    @Test
    void rejectsInvalidUpdateNamesWithStandardErrorAndPreservesPortfolio() throws Exception {
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-08-14T04:05:00Z");
        Carteira persisted = repository.saveAndFlush(new Carteira("Carteira Preservada", creationDate));

        assertInvalidPatchName(persisted.getId(), "{}");
        assertInvalidPatchName(persisted.getId(), "{\"nome\":null}");
        assertInvalidPatchName(persisted.getId(), "{\"nome\":\"\"}");
        assertInvalidPatchName(persisted.getId(), "{\"nome\":\"   \"}");
        assertInvalidPatchName(persisted.getId(), "{\"nome\":\" " + "a".repeat(256) + " \"}");

        Carteira unchanged = repository.findById(persisted.getId()).orElseThrow();
        assertEquals("Carteira Preservada", unchanged.getNome());
        assertEquals(creationDate, unchanged.getDataCriacao());
        assertEquals(1, repository.count());
    }

    @Test
    void rejectsIdCreationDateAndUnknownPropertiesDuringUpdate() throws Exception {
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-08-13T03:30:00Z");
        Carteira persisted = repository.saveAndFlush(new Carteira("Carteira Preservada", creationDate));

        assertInvalidPatchContract(persisted.getId(), "{\"nome\":\"Carteira\",\"id\":10}");
        assertInvalidPatchContract(
                persisted.getId(),
                "{\"nome\":\"Carteira\",\"dataCriacao\":\"2026-08-21T14:30:00Z\"}"
        );
        assertInvalidPatchContract(
                persisted.getId(),
                "{\"nome\":\"Carteira\",\"propriedadeDesconhecida\":true}"
        );

        Carteira unchanged = repository.findById(persisted.getId()).orElseThrow();
        assertEquals("Carteira Preservada", unchanged.getNome());
        assertEquals(creationDate, unchanged.getDataCriacao());
        assertEquals(1, repository.count());
    }

    @Test
    void returnsStandardNotFoundErrorWhenUpdatingMissingPortfolio() throws Exception {
        mockMvc.perform(patch("/carteiras/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Novo nome\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Carteira não encontrada para o id: " + Long.MAX_VALUE
                ))
                .andExpect(jsonPath("$.path").value("/carteiras/" + Long.MAX_VALUE));

        assertEquals(0, repository.count());
    }

    @Test
    void allowsDuplicateNamesAndTreatsSameNormalizedNameAsIdempotentUpdate() throws Exception {
        OffsetDateTime firstDate = OffsetDateTime.parse("2026-08-12T02:15:00Z");
        OffsetDateTime secondDate = OffsetDateTime.parse("2026-08-11T01:10:00Z");
        Carteira first = repository.saveAndFlush(new Carteira("Carteira Principal", firstDate));
        Carteira second = repository.saveAndFlush(new Carteira("Carteira Secundária", secondDate));

        mockMvc.perform(patch("/carteiras/{id}", second.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carteira Principal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(second.getId()))
                .andExpect(jsonPath("$.nome").value("Carteira Principal"))
                .andExpect(jsonPath("$.dataCriacao").value("2026-08-11T01:10:00Z"));

        mockMvc.perform(patch("/carteiras/{id}", first.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  Carteira Principal  \"}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(first.getId()))
                .andExpect(jsonPath("$.nome").value("Carteira Principal"))
                .andExpect(jsonPath("$.dataCriacao").value("2026-08-12T02:15:00Z"));

        List<Carteira> unchangedIdentityAndDates = repository.findAll();
        assertEquals(2, unchangedIdentityAndDates.size());
        assertEquals(List.of("Carteira Principal", "Carteira Principal"),
                unchangedIdentityAndDates.stream().map(Carteira::getNome).toList());
        assertEquals(firstDate, repository.findById(first.getId()).orElseThrow().getDataCriacao());
        assertEquals(secondDate, repository.findById(second.getId()).orElseThrow().getDataCriacao());
    }

    @Test
    void deletesPortfolioWithNoContentAndPreservesOtherPortfolios() throws Exception {
        OffsetDateTime deletedDate = OffsetDateTime.parse("2026-08-10T10:15:00Z");
        OffsetDateTime preservedDate = OffsetDateTime.parse("2026-08-11T11:20:00Z");
        Carteira deleted = repository.saveAndFlush(new Carteira("Carteira Excluída", deletedDate));
        Carteira preserved = repository.saveAndFlush(new Carteira("Carteira Preservada", preservedDate));

        mockMvc.perform(delete("/carteiras/{id}", deleted.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist("Location"));

        assertEquals(false, repository.existsById(deleted.getId()));
        Carteira unchanged = repository.findById(preserved.getId()).orElseThrow();
        assertEquals("Carteira Preservada", unchanged.getNome());
        assertEquals(preservedDate, unchanged.getDataCriacao());
        assertEquals(1, repository.count());
    }

    @Test
    void returnsStandardNotFoundErrorWhenDeletingMissingPortfolio() throws Exception {
        mockMvc.perform(delete("/carteiras/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Carteira não encontrada para o id: " + Long.MAX_VALUE
                ))
                .andExpect(jsonPath("$.path").value("/carteiras/" + Long.MAX_VALUE));

        assertEquals(0, repository.count());
    }

    @Test
    void returnsNotFoundOnSecondSequentialDeletionWithoutRecreatingState() throws Exception {
        Carteira persisted = repository.saveAndFlush(new Carteira(
                "Carteira para exclusão sequencial",
                OffsetDateTime.parse("2026-08-09T09:10:00Z")
        ));

        mockMvc.perform(delete("/carteiras/{id}", persisted.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(delete("/carteiras/{id}", persisted.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Carteira não encontrada para o id: " + persisted.getId()
                ))
                .andExpect(jsonPath("$.path").value("/carteiras/" + persisted.getId()));

        assertEquals(false, repository.existsById(persisted.getId()));
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

    private void assertInvalidPatchName(Long id, String content) throws Exception {
        mockMvc.perform(patch("/carteiras/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"))
                .andExpect(jsonPath("$.details.nome").exists());
    }

    private void assertInvalidPatchContract(Long id, String content) throws Exception {
        mockMvc.perform(patch("/carteiras/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));
    }
}
