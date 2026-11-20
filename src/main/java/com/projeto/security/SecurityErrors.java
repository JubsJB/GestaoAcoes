package com.projeto.security;

import com.projeto.resources.exceptions.StandardError;
import com.projeto.services.exceptions.ApiException;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

public final class SecurityErrors {
    private final JsonMapper mapper;
    private final Clock clock;
    public SecurityErrors(JsonMapper mapper, Clock clock) { this.mapper=mapper; this.clock=clock; }
    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value()); response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getOutputStream(), new StandardError(clock.millis(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), code, Map.of()));
    }
    public void write(HttpServletRequest request, HttpServletResponse response, ApiException erro) throws IOException {
        write(request, response, erro.getStatus(), erro.getCode(), erro.getMessage());
    }
}
