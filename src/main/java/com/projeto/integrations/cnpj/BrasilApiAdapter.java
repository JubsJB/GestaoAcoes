package com.projeto.integrations.cnpj;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projeto.integrations.ExternalApiErrorMapper;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BrasilApiAdapter implements CnpjProvider {

    private static final String PROVIDER = "BrasilAPI";

    private final RestClient restClient;

    public BrasilApiAdapter(@Qualifier("brasilApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CnpjData consultar(String cnpjNormalizado) {
        try {
            BrasilApiCnpjResponse response = restClient.get()
                    .uri("/api/cnpj/v1/{cnpj}", cnpjNormalizado)
                    .retrieve()
                    .body(BrasilApiCnpjResponse.class);

            if (response == null) {
                throw invalidResponse();
            }

            return new CnpjData(
                    response.cnpj(),
                    response.razaoSocial(),
                    response.nomeFantasia(),
                    response.email(),
                    response.telefone(),
                    response.cep(),
                    response.numero(),
                    response.complemento(),
                    response.situacaoCadastral()
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.CNPJ_INEXISTENTE, "CNPJ inexistente");
        } catch (HttpClientErrorException.TooManyRequests exception) {
            throw ExternalApiErrorMapper.rateLimit(PROVIDER, exception);
        } catch (HttpServerErrorException exception) {
            throw ExternalApiErrorMapper.unavailable(PROVIDER, exception);
        } catch (ResourceAccessException exception) {
            throw ExternalApiErrorMapper.accessFailure(PROVIDER, exception);
        } catch (RestClientException exception) {
            throw ExternalApiErrorMapper.invalidResponse(PROVIDER, exception);
        }
    }

    private ApiException invalidResponse() {
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,
                "Resposta inválida do serviço " + PROVIDER
        );
    }

    private record BrasilApiCnpjResponse(
            String cnpj,
            @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("nome_fantasia") String nomeFantasia,
            String email,
            @JsonProperty("ddd_telefone_1") String telefone,
            String cep,
            String numero,
            String complemento,
            @JsonProperty("descricao_situacao_cadastral") String situacaoCadastral
    ) {
    }
}
