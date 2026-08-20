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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CorretoraService {

    private final CnpjValidator cnpjValidator;
    private final CepValidator cepValidator;
    private final CnpjProvider cnpjProvider;
    private final CepProvider cepProvider;
    private final CorretoraPersistenceService persistenceService;
    private final CorretoraMapper mapper;
    private final Clock clock;

    public CorretoraService(
            CnpjValidator cnpjValidator,
            CepValidator cepValidator,
            CnpjProvider cnpjProvider,
            CepProvider cepProvider,
            CorretoraPersistenceService persistenceService,
            CorretoraMapper mapper,
            Clock clock
    ) {
        this.cnpjValidator = cnpjValidator;
        this.cepValidator = cepValidator;
        this.cnpjProvider = cnpjProvider;
        this.cepProvider = cepProvider;
        this.persistenceService = persistenceService;
        this.mapper = mapper;
        this.clock = clock;
    }

    public CorretoraResponse cadastrar(CorretoraCreateRequest request) {
        String cnpj = cnpjValidator.normalizeAndValidate(request.getCnpj());
        persistenceService.ensureCnpjAvailable(cnpj);

        CnpjData cnpjData = cnpjProvider.consultar(cnpj);
        validateCnpjData(cnpjData, cnpj);

        String cep = cepValidator.normalizeAndValidate(cnpjData.cep());
        CepData cepData = cepProvider.consultar(cep);
        validateCepData(cepData, cep);

        if (!"ATIVA".equals(cnpjData.situacaoCadastral())
                && !request.isConfirmacaoSituacaoCadastralNaoAtiva()) {
            throw confirmationRequired(cnpjData.situacaoCadastral());
        }

        OffsetDateTime dataCadastro = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        Corretora corretora = new Corretora(
                cnpj,
                normalizedRequired(cnpjData.razaoSocial()),
                normalizedOptional(cnpjData.nomeFantasia()),
                normalizedOptional(cnpjData.email()),
                normalizedOptional(cnpjData.telefone()),
                cep,
                normalizedRequired(cepData.logradouro()),
                normalizedOptional(cnpjData.numero()),
                normalizedOptional(cnpjData.complemento()),
                normalizedRequired(cepData.bairro()),
                normalizedRequired(cepData.cidade()),
                normalizedRequired(cepData.uf()),
                cnpjData.situacaoCadastral(),
                dataCadastro
        );

        return mapper.toResponse(persistenceService.saveUnique(corretora));
    }

    private void validateCnpjData(CnpjData data, String requestedCnpj) {
        if (data == null
                || isBlank(data.cnpj())
                || isBlank(data.razaoSocial())
                || isBlank(data.cep())
                || isBlank(data.situacaoCadastral())) {
            throw incompleteExternalData("BrasilAPI");
        }

        String returnedCnpj = data.cnpj().replaceAll("\\D", "");
        if (!requestedCnpj.equals(returnedCnpj)) {
            throw invalidExternalResponse("BrasilAPI");
        }
    }

    private void validateCepData(CepData data, String requestedCep) {
        if (data == null
                || isBlank(data.cep())
                || isBlank(data.logradouro())
                || isBlank(data.bairro())
                || isBlank(data.cidade())
                || isBlank(data.uf())) {
            throw incompleteExternalData("ViaCEP");
        }

        String returnedCep = data.cep().replaceAll("\\D", "");
        if (!requestedCep.equals(returnedCep) || data.uf().trim().length() != 2) {
            throw invalidExternalResponse("ViaCEP");
        }
    }

    private String normalizedRequired(String value) {
        if (isBlank(value)) {
            throw incompleteExternalData("fonte externa");
        }
        return value.trim();
    }

    private String normalizedOptional(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ApiException confirmationRequired(String situacaoCadastral) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("situacaoCadastral", situacaoCadastral);
        details.put("confirmacaoNecessaria", true);

        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCodes.SITUACAO_CADASTRAL_NAO_ATIVA,
                "A situação cadastral não está ativa e exige confirmação",
                details
        );
    }

    private ApiException incompleteExternalData(String provider) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCodes.DADOS_EXTERNOS_INCOMPLETOS,
                "Dados obrigatórios ausentes na resposta de " + provider
        );
    }

    private ApiException invalidExternalResponse(String provider) {
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,
                "Resposta inválida do serviço " + provider
        );
    }
}
