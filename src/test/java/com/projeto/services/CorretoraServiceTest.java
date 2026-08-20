package com.projeto.services;

import com.projeto.dto.CorretoraCreateRequest;
import com.projeto.dto.CorretoraResponse;
import com.projeto.entities.Corretora;
import com.projeto.integrations.cep.CepData;
import com.projeto.integrations.cep.CepProvider;
import com.projeto.integrations.cnpj.CnpjData;
import com.projeto.integrations.cnpj.CnpjProvider;
import com.projeto.mappers.CorretoraMapper;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.validation.CepValidator;
import com.projeto.validation.CnpjValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorretoraServiceTest {

    private static final String CNPJ = "11222333000181";
    private static final String CEP = "01001000";
    private static final Instant NOW = Instant.parse("2026-08-20T12:30:00Z");

    @Mock
    private CnpjProvider cnpjProvider;

    @Mock
    private CepProvider cepProvider;

    @Mock
    private CorretoraPersistenceService persistenceService;

    private CorretoraService service;

    @BeforeEach
    void setUp() {
        service = new CorretoraService(
                new CnpjValidator(),
                new CepValidator(),
                cnpjProvider,
                cepProvider,
                persistenceService,
                new CorretoraMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void registersActiveBrokerUsingOnlyExternalDataAndUtcClock() {
        stubSuccessfulPersistence();
        when(cnpjProvider.consultar(CNPJ)).thenReturn(completeCnpjData("ATIVA"));
        when(cepProvider.consultar(CEP)).thenReturn(completeCepData());

        CorretoraResponse response = service.cadastrar(request("11.222.333/0001-81", false));

        ArgumentCaptor<Corretora> captor = ArgumentCaptor.forClass(Corretora.class);
        verify(persistenceService).saveUnique(captor.capture());
        Corretora persisted = captor.getValue();
        assertEquals(CNPJ, persisted.getCnpj());
        assertEquals("Corretora Externa S.A.", persisted.getRazaoSocial());
        assertEquals("Praca da Se", persisted.getLogradouro());
        assertEquals(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC), persisted.getDataCadastro());
        assertEquals(ZoneOffset.UTC, persisted.getDataCadastro().getOffset());
        assertFalse(persisted.isValidadaMercadoFinanceiro(),
                "false representa que a validacao no mercado financeiro ainda nao foi realizada");
        assertFalse(response.validadaMercadoFinanceiro());
    }

    @Test
    void allowsMissingOptionalExternalFields() {
        stubSuccessfulPersistence();
        when(cnpjProvider.consultar(CNPJ)).thenReturn(new CnpjData(
                CNPJ, "Corretora Externa S.A.", null, null, null,
                CEP, null, null, "ATIVA"
        ));
        when(cepProvider.consultar(CEP)).thenReturn(completeCepData());

        CorretoraResponse response = service.cadastrar(request(CNPJ, false));

        assertNull(response.nomeFantasia());
        assertNull(response.email());
        assertNull(response.telefone());
        assertNull(response.numero());
        assertNull(response.complemento());
    }

    @Test
    void rejectsMissingRequiredDataFromEitherProvider() {
        when(cnpjProvider.consultar(CNPJ)).thenReturn(new CnpjData(
                CNPJ, null, null, null, null, CEP, null, null, "ATIVA"
        ));
        ApiException brasilApiFailure = assertThrows(
                ApiException.class,
                () -> service.cadastrar(request(CNPJ, false))
        );
        assertEquals(ErrorCodes.DADOS_EXTERNOS_INCOMPLETOS, brasilApiFailure.getCode());
        verify(cepProvider, never()).consultar(any());

        when(cnpjProvider.consultar(CNPJ)).thenReturn(completeCnpjData("ATIVA"));
        when(cepProvider.consultar(CEP)).thenReturn(new CepData(CEP, null, "Se", "Sao Paulo", "SP"));
        ApiException viaCepFailure = assertThrows(
                ApiException.class,
                () -> service.cadastrar(request(CNPJ, false))
        );
        assertEquals(ErrorCodes.DADOS_EXTERNOS_INCOMPLETOS, viaCepFailure.getCode());
        verify(persistenceService, never()).saveUnique(any());
    }

    @Test
    void invalidCnpjDoesNotCallBrasilApi() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cadastrar(request("11.222.333/0001-82", false))
        );

        assertEquals(ErrorCodes.CNPJ_INVALIDO, exception.getCode());
        verify(cnpjProvider, never()).consultar(any());
    }

    @Test
    void nonActiveStatusRequiresConfirmationWithoutPersistenceAndPreservesStatus() {
        when(cnpjProvider.consultar(CNPJ)).thenReturn(completeCnpjData("BAIXADA"));
        when(cepProvider.consultar(CEP)).thenReturn(completeCepData());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cadastrar(request(CNPJ, false))
        );

        assertEquals(ErrorCodes.SITUACAO_CADASTRAL_NAO_ATIVA, exception.getCode());
        assertEquals(409, exception.getStatus().value());
        assertEquals("BAIXADA", exception.getDetails().get("situacaoCadastral"));
        assertEquals(true, exception.getDetails().get("confirmacaoNecessaria"));
        verify(persistenceService, never()).saveUnique(any());
    }

    @Test
    void confirmedSecondRequestRepeatsProvidersAndPersistsCurrentNonActiveStatus() {
        stubSuccessfulPersistence();
        when(cnpjProvider.consultar(CNPJ)).thenReturn(completeCnpjData("SUSPENSA"));
        when(cepProvider.consultar(CEP)).thenReturn(completeCepData());

        assertThrows(ApiException.class, () -> service.cadastrar(request(CNPJ, false)));
        CorretoraResponse response = service.cadastrar(request(CNPJ, true));

        assertEquals("SUSPENSA", response.situacaoCadastral());
        verify(cnpjProvider, times(2)).consultar(CNPJ);
        verify(cepProvider, times(2)).consultar(CEP);
        verify(persistenceService).saveUnique(any());
    }

    @Test
    void confirmedSecondRequestUsesStatusThatBecameActive() {
        stubSuccessfulPersistence();
        when(cnpjProvider.consultar(CNPJ))
                .thenReturn(completeCnpjData("SUSPENSA"))
                .thenReturn(completeCnpjData("ATIVA"));
        when(cepProvider.consultar(CEP)).thenReturn(completeCepData());

        assertThrows(ApiException.class, () -> service.cadastrar(request(CNPJ, false)));
        CorretoraResponse response = service.cadastrar(request(CNPJ, true));

        assertEquals("ATIVA", response.situacaoCadastral());
        verify(cnpjProvider, times(2)).consultar(CNPJ);
        verify(cepProvider, times(2)).consultar(CEP);
    }

    @Test
    void confirmedSecondRequestDoesNotPersistIfRepeatedLookupFails() {
        when(cnpjProvider.consultar(CNPJ))
                .thenReturn(completeCnpjData("SUSPENSA"))
                .thenThrow(new ApiException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,
                        "indisponivel"
                ));
        when(cepProvider.consultar(CEP)).thenReturn(completeCepData());

        assertThrows(ApiException.class, () -> service.cadastrar(request(CNPJ, false)));
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.cadastrar(request(CNPJ, true))
        );

        assertEquals(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL, exception.getCode());
        verify(cnpjProvider, times(2)).consultar(CNPJ);
        verify(cepProvider).consultar(CEP);
        verify(persistenceService, never()).saveUnique(any());
    }

    private CorretoraCreateRequest request(String cnpj, boolean confirmation) {
        CorretoraCreateRequest request = new CorretoraCreateRequest();
        request.setCnpj(cnpj);
        request.setConfirmarSituacaoCadastralNaoAtiva(confirmation);
        return request;
    }

    private void stubSuccessfulPersistence() {
        doAnswer(invocation -> invocation.getArgument(0))
                .when(persistenceService).saveUnique(any(Corretora.class));
    }

    private CnpjData completeCnpjData(String status) {
        return new CnpjData(
                "11.222.333/0001-81",
                "Corretora Externa S.A.",
                "Corretora Externa",
                "contato@externa.test",
                "1130000000",
                "01001-000",
                "100",
                "10 andar",
                status
        );
    }

    private CepData completeCepData() {
        return new CepData("01001-000", "Praca da Se", "Se", "Sao Paulo", "SP");
    }
}
