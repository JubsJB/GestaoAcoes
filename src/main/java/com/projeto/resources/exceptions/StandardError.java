package com.projeto.resources.exceptions;

import java.io.Serializable;
import java.util.Map;

public class StandardError implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long timeStamp;
    private Integer status;
    private String error;
    private String message;
    private String path;
    private String code;
    private Map<String, Object> details;

    public StandardError() {
        super();
    }

    public StandardError(Long timeStamp, Integer status, String error, String message, String path) {
        this(timeStamp, status, error, message, path, null, Map.of());
    }

    public StandardError(
            Long timeStamp,
            Integer status,
            String error,
            String message,
            String path,
            String code,
            Map<String, Object> details
    ) {
        super();
        this.timeStamp = timeStamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }
}

