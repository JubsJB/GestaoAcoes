package com.projeto.services.exceptions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrityHandlingArchitectureTest {

    private static final Path MAIN = Path.of("src", "main", "java", "com", "projeto");

    @Test
    void affectedServicesUseStructuredExtractorWithoutNativeMessageParsing() throws IOException {
        for (String service : List.of(
                "CorretoraPersistenceService.java",
                "AcaoPersistenceService.java",
                "OperacaoPersistenceService.java",
                "SnapshotCarteiraService.java"
        )) {
            String source = Files.readString(MAIN.resolve("services").resolve(service));
            assertTrue(source.contains("ConstraintNameExtractor"), service);
            assertTrue(source.contains("equalsIgnoreCase"), service);
            assertFalse(source.contains("toUpperCase(Locale.ROOT)"), service);
            assertFalse(source.contains("toLowerCase(Locale.ROOT)"), service);
            assertFalse(source.contains("PSQLException"), service);
            assertFalse(source.contains("SQLState"), service);
        }
    }

    @Test
    void extractorHasSingleTechnicalResponsibilityAndHandlerUsesNeutralFallback() throws IOException {
        String extractor = Files.readString(MAIN.resolve("services/exceptions/ConstraintNameExtractor.java"));
        assertTrue(extractor.contains("ConstraintViolationException"));
        assertTrue(extractor.contains("getConstraintName()"));
        for (String forbidden : List.of(
                "ResourceExceptionHandler", "StandardError", "ResponseEntity", "Repository",
                "PSQLException", "SQLState", "getMessage()", "contains("
        )) {
            assertFalse(extractor.contains(forbidden), forbidden);
        }

        String handler = Files.readString(MAIN.resolve("resources/exceptions/ResourceExceptionHandler.java"));
        assertTrue(handler.contains("INTEGRIDADE_DADOS_VIOLADA"));
        assertFalse(handler.contains("uk_corretora_cnpj"));
        assertFalse(handler.contains("uk_acao_ticker_mercado"));
        assertFalse(handler.contains("uk_operacao_carteira_acao_data_ordem"));
        assertFalse(handler.contains("uk_snapshot_carteira_carteira_data_hora"));
    }
}
