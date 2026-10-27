package com.projeto.services.exceptions;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;

public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public ApiException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        if (details == null) {
            this.details = Map.of();
        } else {
            Map<String, Object> copy = new LinkedHashMap<>();
            details.forEach((key, value) -> copy.put(Objects.requireNonNull(key), value));
            this.details = Collections.unmodifiableMap(copy);
        }
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
