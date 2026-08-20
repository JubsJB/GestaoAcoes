package com.projeto.integrations.cep;

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
public class ViaCepAdapter implements CepProvider {

    private static final String PROVIDER = "ViaCEP";

    private final RestClient restClient;

    public ViaCepAdapter(@Qualifier("viaCepRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CepData consultar(String cepNormalizado) {
        try {
            ViaCepResponse response = restClient.get()
                    .uri("/ws/{cep}/json/", cepNormalizado)
                    .retrieve()
                    .body(ViaCepResponse.class);

            if (response == null) {
                throw invalidResponse();
            }
            if (Boolean.TRUE.equals(response.erro())) {
                throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.CEP_INEXISTENTE, "CEP inexistente");
            }

            return new CepData(
                    response.cep(),
                    response.logradouro(),
                    response.bairro(),
                    response.cidade(),
                    response.uf()
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.CEP_INEXISTENTE, "CEP inexistente");
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

    private record ViaCepResponse(
            String cep,
            String logradouro,
            String bairro,
            @JsonProperty("localidade") String cidade,
            String uf,
            Boolean erro
    ) {
    }
}
