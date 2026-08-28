package com.projeto.resources;

import com.projeto.GestaoacoesApplication;
import com.projeto.entities.Corretora;
import com.projeto.integrations.cep.CepData;
import com.projeto.integrations.cep.CepProvider;
import com.projeto.integrations.cnpj.CnpjData;
import com.projeto.integrations.cnpj.CnpjProvider;
import com.projeto.repositories.CorretoraRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GestaoacoesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorretoraResourceTest {

    private static final String CNPJ = "11222333000181";
    private static final String CEP = "01001000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorretoraRepository repository;

    @MockitoBean
    private CnpjProvider cnpjProvider;

    @MockitoBean
    private CepProvider cepProvider;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsActiveBrokerWithCompleteDtoAndLocation() throws Exception {
        stubProviders("ATIVA");

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"11.222.333/0001-81\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.matchesPattern(".*/corretoras/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.cnpj").value(CNPJ))
                .andExpect(jsonPath("$.razaoSocial").value("Corretora Externa S.A."))
                .andExpect(jsonPath("$.nomeFantasia").value("Corretora Externa"))
                .andExpect(jsonPath("$.email").value("contato@externa.test"))
                .andExpect(jsonPath("$.telefone").value("1130000000"))
                .andExpect(jsonPath("$.cep").value(CEP))
                .andExpect(jsonPath("$.logradouro").value("Praca da Se"))
                .andExpect(jsonPath("$.numero").value("100"))
                .andExpect(jsonPath("$.complemento").value("10 andar"))
                .andExpect(jsonPath("$.bairro").value("Se"))
                .andExpect(jsonPath("$.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$.uf").value("SP"))
                .andExpect(jsonPath("$.situacaoCadastral").value("ATIVA"))
                .andExpect(jsonPath("$.validadaMercadoFinanceiro").value(false))
                .andExpect(jsonPath("$.dataCadastro").exists());
    }

    @Test
    void rejectsBrokerDataSuppliedByClientAndInvalidCnpjBeforeExternalCall() throws Exception {
        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"11.222.333/0001-81\",\"razaoSocial\":\"Nao permitido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"11.222.333/0001-82\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CNPJ_INVALIDO"));

        verify(cnpjProvider, never()).consultar(any());
    }

    @Test
    void returnsConfirmationConflictThenCreatesAfterExplicitConfirmedRequest() throws Exception {
        stubProviders("SUSPENSA");

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"11222333000181\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SITUACAO_CADASTRAL_NAO_ATIVA"))
                .andExpect(jsonPath("$.details.situacaoCadastral").value("SUSPENSA"))
                .andExpect(jsonPath("$.details.confirmacaoNecessaria").value(true));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"11222333000181\","
                                + "\"confirmarSituacaoCadastralNaoAtiva\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacaoCadastral").value("SUSPENSA"));

        verify(cnpjProvider, times(2)).consultar(CNPJ);
        verify(cepProvider, times(2)).consultar(CEP);
    }

    @Test
    void returnsStandardizedDuplicateErrorBeforeExternalCalls() throws Exception {
        repository.saveAndFlush(new Corretora(
                CNPJ, "Ja existente S.A.", null, null, null,
                CEP, "Praca da Se", null, null, "Se", "Sao Paulo", "SP",
                "ATIVA", OffsetDateTime.of(2026, 8, 20, 12, 30, 0, 0, ZoneOffset.UTC)
        ));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"11222333000181\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CORRETORA_DUPLICADA"));

        verify(cnpjProvider, never()).consultar(any());
    }

    @Test
    void listsPersistedBrokersByAscendingIdWithCompleteDtoAndNullOptionalFields() throws Exception {
        Corretora first = repository.saveAndFlush(completeBroker(
                CNPJ,
                "Primeira Corretora S.A.",
                true
        ));
        Corretora second = repository.saveAndFlush(completeBroker(
                "04252011000110",
                "Segunda Corretora S.A.",
                false
        ));

        mockMvc.perform(get("/corretoras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(first.getId()))
                .andExpect(jsonPath("$[0].cnpj").value(CNPJ))
                .andExpect(jsonPath("$[0].razaoSocial").value("Primeira Corretora S.A."))
                .andExpect(jsonPath("$[0].nomeFantasia").value("Nome Fantasia"))
                .andExpect(jsonPath("$[0].email").value("contato@corretora.test"))
                .andExpect(jsonPath("$[0].telefone").value("1130000000"))
                .andExpect(jsonPath("$[0].cep").value(CEP))
                .andExpect(jsonPath("$[0].logradouro").value("Praca da Se"))
                .andExpect(jsonPath("$[0].numero").value("100"))
                .andExpect(jsonPath("$[0].complemento").value("10 andar"))
                .andExpect(jsonPath("$[0].bairro").value("Se"))
                .andExpect(jsonPath("$[0].cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$[0].uf").value("SP"))
                .andExpect(jsonPath("$[0].situacaoCadastral").value("ATIVA"))
                .andExpect(jsonPath("$[0].validadaMercadoFinanceiro").value(false))
                .andExpect(jsonPath("$[0].dataCadastro").exists())
                .andExpect(jsonPath("$[1].id").value(second.getId()))
                .andExpect(jsonPath("$[1].cnpj").value("04252011000110"))
                .andExpect(jsonPath("$[1].razaoSocial").value("Segunda Corretora S.A."))
                .andExpect(jsonPath("$[1].nomeFantasia").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[1].email").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[1].telefone").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[1].numero").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[1].complemento").value(Matchers.nullValue()));

        verifyNoInteractions(cnpjProvider, cepProvider);
    }

    @Test
    void returnsEmptyArrayWhenNoBrokerIsPersisted() throws Exception {
        mockMvc.perform(get("/corretoras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verifyNoInteractions(cnpjProvider, cepProvider);
    }

    @Test
    void returnsPersistedBrokerByIdWithoutExternalCalls() throws Exception {
        Corretora saved = repository.saveAndFlush(completeBroker(
                CNPJ,
                "Corretora Consultada S.A.",
                false
        ));

        mockMvc.perform(get("/corretoras/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.cnpj").value(CNPJ))
                .andExpect(jsonPath("$.razaoSocial").value("Corretora Consultada S.A."))
                .andExpect(jsonPath("$.nomeFantasia").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.email").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.telefone").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.cep").value(CEP))
                .andExpect(jsonPath("$.logradouro").value("Praca da Se"))
                .andExpect(jsonPath("$.numero").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.complemento").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.bairro").value("Se"))
                .andExpect(jsonPath("$.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$.uf").value("SP"))
                .andExpect(jsonPath("$.situacaoCadastral").value("ATIVA"))
                .andExpect(jsonPath("$.validadaMercadoFinanceiro").value(false))
                .andExpect(jsonPath("$.dataCadastro").exists());

        verifyNoInteractions(cnpjProvider, cepProvider);
    }

    @Test
    void returnsStandardNotFoundErrorForMissingBrokerIdWithoutExternalCalls() throws Exception {
        mockMvc.perform(get("/corretoras/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Corretora não encontrada para o id: 999999"))
                .andExpect(jsonPath("$.path").value("/corretoras/999999"))
                .andExpect(jsonPath("$.code").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.details").isEmpty());

        verifyNoInteractions(cnpjProvider, cepProvider);
    }

    @Test
    void findsBrokerByUnmaskedCnpjWithCompleteResponseAndNoExternalCalls() throws Exception {
        Corretora saved = repository.saveAndFlush(completeBroker(
                CNPJ,
                "Corretora Consultada S.A.",
                true
        ));

        mockMvc.perform(get("/corretoras/por-cnpj").param("cnpj", CNPJ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.cnpj").value(CNPJ))
                .andExpect(jsonPath("$.razaoSocial").value("Corretora Consultada S.A."))
                .andExpect(jsonPath("$.nomeFantasia").value("Nome Fantasia"))
                .andExpect(jsonPath("$.email").value("contato@corretora.test"))
                .andExpect(jsonPath("$.telefone").value("1130000000"))
                .andExpect(jsonPath("$.cep").value(CEP))
                .andExpect(jsonPath("$.logradouro").value("Praca da Se"))
                .andExpect(jsonPath("$.numero").value("100"))
                .andExpect(jsonPath("$.complemento").value("10 andar"))
                .andExpect(jsonPath("$.bairro").value("Se"))
                .andExpect(jsonPath("$.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$.uf").value("SP"))
                .andExpect(jsonPath("$.situacaoCadastral").value("ATIVA"))
                .andExpect(jsonPath("$.validadaMercadoFinanceiro").value(false))
                .andExpect(jsonPath("$.dataCadastro").exists());

        verifyNoInteractions(cnpjProvider, cepProvider);
    }

    @Test
    void findsSameBrokerByMaskedCnpjAndPreservesNullOptionalFields() throws Exception {
        Corretora saved = repository.saveAndFlush(completeBroker(
                CNPJ,
                "Corretora Consultada S.A.",
                false
        ));

        mockMvc.perform(get("/corretoras/por-cnpj")
                        .param("cnpj", "11.222.333/0001-81"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.cnpj").value(CNPJ))
                .andExpect(jsonPath("$.nomeFantasia").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.email").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.telefone").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.numero").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.complemento").value(Matchers.nullValue()));

        verifyNoInteractions(cnpjProvider, cepProvider);
    }

    @Test
    void rejectsMissingEmptyAndInvalidCnpjWithApprovedStandardError() throws Exception {
        mockMvc.perform(get("/corretoras/por-cnpj"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CNPJ_INVALIDO"));

        mockMvc.perform(get("/corretoras/por-cnpj").param("cnpj", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CNPJ_INVALIDO"));

        mockMvc.perform(get("/corretoras/por-cnpj").param("cnpj", "11.222.333/0001-82"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CNPJ_INVALIDO"));

        verifyNoInteractions(cnpjProvider, cepProvider);
    }

    @Test
    void returnsCentralizedNotFoundForValidMissingCnpj() throws Exception {
        mockMvc.perform(get("/corretoras/por-cnpj")
                        .param("cnpj", "11.222.333/0001-81"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Corretora não encontrada para o CNPJ: " + CNPJ))
                .andExpect(jsonPath("$.path").value("/corretoras/por-cnpj"))
                .andExpect(jsonPath("$.code").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.details").isEmpty());

        verifyNoInteractions(cnpjProvider, cepProvider);
    }

    @Test
    void doesNotExposeAliasOrTurnCollectionQueryIntoSingularLookup() throws Exception {
        repository.saveAndFlush(completeBroker(CNPJ, "Corretora Listada S.A.", false));

        mockMvc.perform(get("/corretoras/cnpj/{cnpj}", CNPJ))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/corretoras").param("cnpj", CNPJ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verifyNoInteractions(cnpjProvider, cepProvider);
    }

    private void stubProviders(String status) {
        when(cnpjProvider.consultar(CNPJ)).thenReturn(new CnpjData(
                "11.222.333/0001-81",
                "Corretora Externa S.A.",
                "Corretora Externa",
                "contato@externa.test",
                "1130000000",
                "01001-000",
                "100",
                "10 andar",
                status
        ));
        when(cepProvider.consultar(CEP)).thenReturn(
                new CepData("01001-000", "Praca da Se", "Se", "Sao Paulo", "SP")
        );
    }

    private Corretora completeBroker(String cnpj, String razaoSocial, boolean withOptionalFields) {
        return new Corretora(
                cnpj,
                razaoSocial,
                withOptionalFields ? "Nome Fantasia" : null,
                withOptionalFields ? "contato@corretora.test" : null,
                withOptionalFields ? "1130000000" : null,
                CEP,
                "Praca da Se",
                withOptionalFields ? "100" : null,
                withOptionalFields ? "10 andar" : null,
                "Se",
                "Sao Paulo",
                "SP",
                "ATIVA",
                OffsetDateTime.of(2026, 8, 20, 12, 30, 0, 0, ZoneOffset.UTC)
        );
    }
}
