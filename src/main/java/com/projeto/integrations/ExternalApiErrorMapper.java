package com.projeto.integrations;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;

public final class ExternalApiErrorMapper {

    private ExternalApiErrorMapper() {
    }

    public static ApiException rateLimit(String provider, HttpClientErrorException.TooManyRequests cause) {
        return new ApiException(
                HttpStatus.TOO_MANY_REQUESTS,
                ErrorCodes.LIMITE_REQUISICOES_EXCEDIDO,
                "Limite de requisições excedido no serviço " + provider
        );
    }

    public static ApiException unavailable(String provider, HttpServerErrorException cause) {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,
                "Serviço externo indisponível: " + provider
        );
    }

    public static ApiException accessFailure(String provider, ResourceAccessException cause) {
        if (hasTimeoutCause(cause)) {
            return new ApiException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    ErrorCodes.SERVICO_EXTERNO_TIMEOUT,
                    "Tempo limite excedido no serviço " + provider
            );
        }

        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL,
                "Serviço externo indisponível: " + provider
        );
    }

    public static ApiException invalidResponse(String provider, RestClientException cause) {
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                ErrorCodes.RESPOSTA_EXTERNA_INVALIDA,
                "Resposta inválida do serviço " + provider
        );
    }

    private static boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpTimeoutException || current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
