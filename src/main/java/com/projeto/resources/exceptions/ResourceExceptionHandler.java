package com.projeto.resources.exceptions;

import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class ResourceExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<StandardError> handleApiException(ApiException ex, HttpServletRequest request) {
        StandardError error = createError(
                ex.getStatus(),
                ex.getMessage(),
                request.getRequestURI(),
                ex.getCode(),
                ex.getDetails()
        );
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );

        StandardError error = createError(
                HttpStatus.BAD_REQUEST,
                "Dados da requisição inválidos",
                request.getRequestURI(),
                ErrorCodes.REQUEST_INVALIDO,
                details
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        StandardError error = createError(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido ou contém campos não permitidos",
                request.getRequestURI(),
                ErrorCodes.REQUEST_INVALIDO,
                Map.of()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        StandardError error = createError(
                HttpStatus.CONFLICT,
                "Já existe uma corretora cadastrada com este CNPJ",
                request.getRequestURI(),
                ErrorCodes.CORRETORA_DUPLICADA,
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<StandardError> handleObjectNotFound(
            ObjectNotFoundException ex,
            HttpServletRequest request
    ) {
        StandardError error = createError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                null,
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        StandardError error = createError(
                HttpStatus.BAD_REQUEST,
                "Parâmetro da requisição inválido",
                request.getRequestURI(),
                ErrorCodes.REQUEST_INVALIDO,
                Map.of()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        StandardError error = createError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                ErrorCodes.REQUEST_INVALIDO,
                Map.of()
        );
        return ResponseEntity.badRequest().body(error);
    }

    private StandardError createError(
            HttpStatus status,
            String message,
            String path,
            String code,
            Map<String, Object> details
    ) {
        return new StandardError(
                System.currentTimeMillis(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                code,
                details
        );
    }
}
