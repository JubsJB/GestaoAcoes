package com.projeto.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApresentacaoHttpTransportTest {
    private static final String PROXY = "172.30.180.10";

    @Test
    void aceitaSomenteProxyEOrigemCanonicos() throws Exception {
        assertEquals(true, aceito(PROXY, "localhost:8081", "http://localhost:8081", PROXY));
        assertEquals(true, aceito(PROXY, "localhost:8081", null, PROXY));
        assertEquals(false, aceito("127.0.0.1", "localhost:8081", null, PROXY));
        assertEquals(false, aceito(PROXY, "localhost", null, PROXY));
        assertEquals(false, aceito(PROXY, "localhost:8081", "http://localhost:9999", PROXY));
        assertEquals(false, aceito(PROXY, "localhost:8081", "null", PROXY));
        assertEquals(false, aceito(PROXY, "localhost:8081", null, ""));
    }

    private boolean aceito(String remote, String host, String origin, String proxy) throws Exception {
        var config = mock(AuthRuntimeConfig.class);
        var errors = mock(SecurityErrors.class);
        var request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr(remote);
        request.addHeader("Host", host);
        request.addHeader("X-Forwarded-Proto", "http");
        request.addHeader("X-Forwarded-Host", "localhost");
        request.addHeader("X-Forwarded-Port", "8081");
        if (origin != null) request.addHeader("Origin", origin);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        new TransportSecurityFilter(config, errors, proxy).doFilter(request, response, chain);
        if (chain.getRequest() != null) return true;
        verify(errors).write(eq(request), eq(response), eq(HttpStatus.BAD_REQUEST), eq("HTTPS_OBRIGATORIO"), any());
        return false;
    }
}
