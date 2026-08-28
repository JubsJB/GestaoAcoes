package com.projeto.resources.exceptions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.projeto.services.exceptions.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceExceptionHandlerTest {

    @Test
    void returnsNeutralConflictWithoutExposingPersistenceDetails() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/acoes");
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "SQL constraint uk_historico_cotacao_acao_data_hora native stack trace"
        );

        Logger logger = (Logger) LoggerFactory.getLogger(ResourceExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        ResponseEntity<StandardError> response;
        try {
            response = new ResourceExceptionHandler().handleDataIntegrity(exception, request);
        } finally {
            logger.detachAppender(appender);
        }
        StandardError error = response.getBody();

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(error);
        assertEquals(409, error.getStatus());
        assertEquals("Conflict", error.getError());
        assertEquals("A operação viola uma regra de integridade dos dados.", error.getMessage());
        assertEquals("/acoes", error.getPath());
        assertEquals(ErrorCodes.INTEGRIDADE_DADOS_VIOLADA, error.getCode());
        assertEquals(java.util.Map.of(), error.getDetails());
        String publicPayload = error.getMessage() + error.getCode() + error.getDetails();
        assertFalse(publicPayload.contains("SQL"));
        assertFalse(publicPayload.contains("constraint"));
        assertFalse(publicPayload.contains("uk_historico"));
        assertFalse(publicPayload.contains("stack trace"));
        assertEquals(1, appender.list.size());
        assertEquals(Level.ERROR, appender.list.get(0).getLevel());
        assertEquals("/acoes", appender.list.get(0).getArgumentArray()[0]);
        assertNotNull(appender.list.get(0).getThrowableProxy());
    }
}
