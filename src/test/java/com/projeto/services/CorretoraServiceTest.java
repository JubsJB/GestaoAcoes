package com.projeto.services;

import com.projeto.dto.CorretoraCreateRequest;
import com.projeto.dto.CorretoraResponse;
import com.projeto.entities.Corretora;
import com.projeto.integrations.cep.CepData;
import com.projeto.integrations.cep.CepProvider;
import com.projeto.integrations.cnpj.CnpjData;
import com.projeto.integrations.cnpj.CnpjProvider;
import com.projeto.mappers.CorretoraMapper;
import com.projeto.repositories.CorretoraRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.validation.CepValidator;
import com.projeto.validation.CnpjValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private CorretoraRepository repository;

    private CorretoraService service;

    @BeforeEach
    void setUp() {
        service = new CorretoraService(
                new CnpjValidator(),
                new CepValidator(),
                cnpjProvider,
                cepProvider,
                persistenceService,
                repository,
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

    @Test
    void listsPersistedBrokersUsingAscendingIdSortAndMapsEveryItem() {
        Corretora first = broker(CNPJ, "Primeira Corretora S.A.", true);
        Corretora second = broker("04252011000110", "Segunda Corretora S.A.", false);
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(first, second));

        List<CorretoraResponse> response = service.listar();

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(sortCaptor.capture());
        Sort.Order idOrder = sortCaptor.getValue().getOrderFor("id");
        assertEquals(Sort.Direction.ASC, idOrder.getDirection());
        assertEquals(List.of(CNPJ, "04252011000110"),
                response.stream().map(CorretoraResponse::cnpj).toList());
        assertEquals("Primeira Corretora S.A.", response.get(0).razaoSocial());
        assertNull(response.get(1).nomeFantasia());
        verifyNoInteractions(cnpjProvider, cepProvider, persistenceService);
    }

    @Test
    void returnsEmptyListWhenNoBrokerIsPersisted() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of());

        List<CorretoraResponse> response = service.listar();

        assertTrue(response.isEmpty());
        verifyNoInteractions(cnpjProvider, cepProvider, persistenceService);
    }

    @Test
    void findsPersistedBrokerByIdWithoutCallingExternalProviders() {
        Corretora corretora = broker(CNPJ, "Corretora Consultada S.A.", true);
        when(repository.findById(7L)).thenReturn(Optional.of(corretora));

        CorretoraResponse response = service.buscarPorId(7L);

        assertEquals(CNPJ, response.cnpj());
        assertEquals("Corretora Consultada S.A.", response.razaoSocial());
        verify(repository).findById(7L);
        verifyNoInteractions(cnpjProvider, cepProvider, persistenceService);
    }

    @Test
    void throwsObjectNotFoundWhenBrokerIdDoesNotExistWithoutCallingExternalProviders() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> service.buscarPorId(99L)
        );

        assertEquals("Corretora não encontrada para o id: 99", exception.getMessage());
        verify(repository).findById(99L);
        verifyNoInteractions(cnpjProvider, cepProvider, persistenceService);
    }

    @Test
    void findsBrokerByMaskedOrUnmaskedCnpjUsingOneNormalizedRepositoryQuery() {
        Corretora corretora = broker(CNPJ, "Corretora Consultada S.A.", true);
        when(repository.findByCnpj(CNPJ)).thenReturn(Optional.of(corretora));

        CorretoraResponse masked = service.buscarPorCnpj("11.222.333/0001-81");
        CorretoraResponse unmasked = service.buscarPorCnpj(CNPJ);

        assertEquals(masked, unmasked);
        assertEquals(CNPJ, masked.cnpj());
        assertEquals("Corretora Consultada S.A.", masked.razaoSocial());
        assertEquals("Nome Fantasia", masked.nomeFantasia());
        assertEquals(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC), masked.dataCadastro());
        verify(repository, times(2)).findByCnpj(CNPJ);
        verifyNoInteractions(cnpjProvider, cepProvider, persistenceService);
    }

    @Test
    void rejectsEveryInvalidCnpjBeforeRepositoryOrExternalDependencies() {
        List<String> invalidValues = java.util.Arrays.asList(
                null,
                "",
                "   ",
                "11.222.333/0001-8A",
                "1122233300018",
                "11111111111111",
                "11.222.333/0001-82"
        );

        for (String invalidValue : invalidValues) {
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> service.buscarPorCnpj(invalidValue)
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertEquals(ErrorCodes.CNPJ_INVALIDO, exception.getCode());
        }

        verifyNoInteractions(repository, cnpjProvider, cepProvider, persistenceService);
    }

    @Test
    void throwsObjectNotFoundForValidMissingCnpjWithoutExternalCallsOrWrites() {
        when(repository.findByCnpj(CNPJ)).thenReturn(Optional.empty());

        ObjectNotFoundException exception = assertThrows(
                ObjectNotFoundException.class,
                () -> service.buscarPorCnpj("11.222.333/0001-81")
        );

        assertEquals("Corretora não encontrada para o CNPJ: " + CNPJ, exception.getMessage());
        verify(repository).findByCnpj(CNPJ);
        verifyNoInteractions(cnpjProvider, cepProvider, persistenceService);
    }

    @Test
    void cnpjLookupIsReadOnlyWithDefaultIsolationAndNoLockAnnotation() throws Exception {
        Method method = CorretoraService.class.getMethod("buscarPorCnpj", String.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertTrue(transactional.readOnly());
        assertEquals(Isolation.DEFAULT, transactional.isolation());
        assertNull(method.getAnnotation(Lock.class));
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

    private Corretora broker(String cnpj, String razaoSocial, boolean withOptionalFields) {
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
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)
        );
    }
}
