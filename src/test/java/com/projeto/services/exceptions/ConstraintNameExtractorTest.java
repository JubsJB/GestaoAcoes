package com.projeto.services.exceptions;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstraintNameExtractorTest {

    private final ConstraintNameExtractor extractor = new ConstraintNameExtractor();

    @Test
    void extractsConstraintFromDirectAndNestedCauses() {
        ConstraintViolationException direct = constraint("uk_direct");
        assertEquals("uk_direct", extractor.extractConstraintName(direct).orElseThrow());

        RuntimeException nested = new RuntimeException(
                "outer",
                new IllegalStateException("middle", constraint("uk_nested"))
        );
        assertEquals("uk_nested", extractor.extractConstraintName(nested).orElseThrow());
    }

    @Test
    void returnsEmptyWithoutStructuredConstraintOrWithNullOrEmptyName() {
        assertTrue(extractor.extractConstraintName(new DataIntegrityViolationException("uk_known_in_message_only"))
                .isEmpty());
        assertTrue(extractor.extractConstraintName(constraint(null)).isEmpty());
        assertTrue(extractor.extractConstraintName(constraint("")).isEmpty());
        assertTrue(extractor.extractConstraintName(null).isEmpty());
    }

    @Test
    void stopsOnAnomalousCauseCycle() {
        Throwable cyclic = new RuntimeException("cycle") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        assertTrue(extractor.extractConstraintName(cyclic).isEmpty());
    }

    private ConstraintViolationException constraint(String name) {
        return new ConstraintViolationException("native database message", new SQLException("sql"), name);
    }
}
