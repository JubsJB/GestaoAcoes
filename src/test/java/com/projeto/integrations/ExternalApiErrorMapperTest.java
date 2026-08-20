package com.projeto.integrations;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExternalApiErrorMapperTest {

    @Test
    void mapsTimeoutAndOtherAccessFailuresDistinctly() {
        ApiException timeout = ExternalApiErrorMapper.accessFailure(
                "provider",
                new ResourceAccessException("timeout", new HttpTimeoutException("timeout"))
        );
        ApiException unavailable = ExternalApiErrorMapper.accessFailure(
                "provider",
                new ResourceAccessException("connection refused")
        );

        assertEquals(ErrorCodes.SERVICO_EXTERNO_TIMEOUT, timeout.getCode());
        assertEquals(504, timeout.getStatus().value());
        assertEquals(ErrorCodes.SERVICO_EXTERNO_INDISPONIVEL, unavailable.getCode());
        assertEquals(503, unavailable.getStatus().value());
    }
}
