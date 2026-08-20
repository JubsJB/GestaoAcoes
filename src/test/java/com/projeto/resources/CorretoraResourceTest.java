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
import static org.mockito.Mockito.when;
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
}
