package com.projeto.config;

import com.projeto.entities.Moeda;
import com.projeto.entities.ReferenciaTipoCotacao;
import com.projeto.integrations.cotacao.CotacaoCandidata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class TimeConfigTest {
    @Test
    void defaultClockForOtherServicesRemainsTheOriginalPrimaryBean() {
        try (var context = new AnnotationConfigApplicationContext(TimeConfig.class)) {
            Clock original = context.getBean("applicationClock", Clock.class);
            assertSame(original, context.getBean(Clock.class));
            assertNotSame(original, context.getBean("cotacaoAcquisitionClock", Clock.class));
        }
    }

    @Test
    void configuredAcquisitionClockIsUtcAndExactlyRepresentable() {
        var config = new TimeConfig();
        Clock base = config.applicationClock();
        Clock acquisition = config.cotacaoAcquisitionClock(base);
        assertEquals(ZoneOffset.UTC, acquisition.getZone());
        for (int i = 0; i < 128; i++) {
            assertEquals(0, acquisition.instant().getNano() % 1000);
        }
        assertDoesNotThrow(() -> new CotacaoCandidata("PETR4", "Petrobras", Moeda.BRL, BigDecimal.TEN,
                ReferenciaTipoCotacao.INSTANTE, OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), null,
                OffsetDateTime.now(acquisition)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 123456000, 123456001, 123456789, 999999999})
    void explicitSourceResolutionPreservesFixedClockInjectionAndDoesNotMutateBase(int nanos) {
        Instant original = Instant.parse("2026-10-11T12:00:00Z").plusNanos(nanos);
        Clock fixed = Clock.fixed(original, ZoneOffset.UTC);
        Clock acquisition = new TimeConfig().cotacaoAcquisitionClock(fixed);
        assertEquals(original, fixed.instant());
        assertEquals(original.minusNanos(nanos % 1000), acquisition.instant());
        assertEquals(0, acquisition.instant().getNano() % 1000);
    }
}
