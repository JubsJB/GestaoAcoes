package com.projeto.resources.exceptions;

import com.projeto.resources.AtualizacaoCotacoesCarteiraResource;
import com.projeto.services.exceptions.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

/** Conteúdo sem conversor também é body inválido, somente neste endpoint. */
@RestControllerAdvice(assignableTypes = AtualizacaoCotacoesCarteiraResource.class)
public class AtualizacaoCotacoesCarteiraExceptionHandler {
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<StandardError> bodyInvalido(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new StandardError(System.currentTimeMillis(),
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "A atualização de cotações da Carteira não aceita corpo de requisição",
                request.getRequestURI(), ErrorCodes.REQUEST_INVALIDO, Map.of()));
    }
}
